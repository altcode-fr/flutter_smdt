package com.altcode.smdt;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import android.os.Message;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import android.app.Activity;
import android.os.Bundle;

import android.content.Context;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import dalvik.system.DexClassLoader;
import java.lang.reflect.Method;

import java.util.zip.ZipFile;
import java.util.Enumeration;

/** SmdtPlugin */
public class SmdtPlugin implements FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private FlutterPluginBinding flutterPluginBinding;
  private Context applicationContext;
  
  /// ClassLoader global pour garder la classe chargée en mémoire
  private Class<?> clazz;

  /// ClassLoader pour charger le jar dynamiquement
  private ClassLoader loadDynamicJar(Context context) throws Exception {
    String jarFileName;

    /// Choix du jar en fonction de la version Android
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) { // Android 10
      jarFileName = "smdt_old.jar";
    } else {
      /// La librairie smdt.jar (smdt_new.jar) ne compile pas avec le ClassLoader
      /// Utilisation d'une version modifiée smdt_new_dex.jar
      /// J'ai utilisé d8 pour générer un dex à partir du jar :
      /// d8 --release smdt_new.jar --output out/ --classpath ~/android/sdk/platforms/android-28/android.jar
      /// Puis j'ai zippé le dex dans un jar : zip smdt_new_dex.jar ./out/classes.dex
      jarFileName = "smdt_new_dex.jar"; 
    }

    Log.d("SmdtPlugin", "Loading jar file: " + jarFileName);

    File dexDir = context.getDir("dex", Context.MODE_PRIVATE);
    File jarFile = new File(dexDir, jarFileName);
    
    // Copie du jar depuis assets ou libs vers un répertoire exécutable
    if (!jarFile.exists()) {
      try (InputStream is = context.getAssets().open(jarFileName);
      FileOutputStream fos = new FileOutputStream(jarFile)) {
        byte[] buffer = new byte[4096];
        int length;
        while ((length = is.read(buffer)) > 0) {
          fos.write(buffer, 0, length);
        }
        fos.flush();
      } catch (IOException e) {
        Log.e("SmdtPlugin", "Error copying jar file", e);
        throw e;
      }
    }
    
    Log.d("SmdtPlugin", "Jar chargé : " + jarFile.getAbsolutePath());
    
    /// Définition du ClassLoader
    return new dalvik.system.DexClassLoader(
        jarFile.getAbsolutePath(),
        dexDir.getAbsolutePath(),
        null,
        context.getClassLoader());
  }

  /// Construit le ClassLoader et charge la classe SmdtManager ou SmdtManagerNew
  private void cacheClassLoader(Context context) {
    // Charge la classe une seule fois
    if(this.clazz == null) {
      
      ClassLoader cl;
      
      // Charge le jar dynamiquement
      try {
        cl = loadDynamicJar(context);
      } catch (Exception e) {
        Log.e("SmdtPlugin", "Error loading jar", e);
        this.clazz = null;
        return;
      }

      Log.d("SmdtPlugin", "Loading class for the first time");
      try {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) { // Android 10
          
          // Charge la classe SmdtManager
          clazz = cl.loadClass("android.app.smdt.SmdtManager");
        } else {

          // Charge la classe SmdtManagerNew
          clazz = cl.loadClass("android.app.smdt.SmdtManagerNew");
        }
      } catch (ClassNotFoundException e) {
        Log.e("SmdtPlugin", "Class not found in the jar", e);
        this.clazz = null;
        return;
      }
    }
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    this.flutterPluginBinding = binding;

    // Create a MethodChannel that will handle communication between Flutter and
    // Android
    if (binding.getBinaryMessenger() == null) {
      return;
    }

    channel = new MethodChannel(binding.getBinaryMessenger(), "smdt");
    channel.setMethodCallHandler(this);

    applicationContext = binding.getApplicationContext();

    // Charge le ClassLoader et la classe SmdtManager ou SmdtManagerNew
    cacheClassLoader(applicationContext);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    Log.d("SmdtPlugin", "Method called: " + call.method);

    try {
      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) { // Android 10
        onMethodCallOld(call, result);
      } else {
        onMethodCallNew(call, result);
      }
    } catch (Exception e) {
      Log.e("SmdtPlugin", "Error handling method call", e);
      result.error("ERROR", "Error handling method call: " + e.getMessage(), null);
    }
  }

  /// Invocation d'une méthode du jar dynamique  
  public Object invokeLibMethod(Context context, String methodName, Class<?>[] paramTypes, Object[] params) {

    if (this.clazz == null) {
      Log.e("SmdtPlugin", "Class not loaded, cannot invoke method: " + methodName);
      return null;
    }

    try {
      
      // Obtient une instance de la classe SmdtManager ou SmdtManagerNew
      Object instance;
      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) { // Android 10
      
        // Appel de la méthode create dans l'ancienne version
        Method createMethod = clazz.getMethod("create", Context.class);
        instance = createMethod.invoke(null, context);
      } else {
      
        // Appel de la méthode getInstance dans la nouvelle version
        Method createMethod = clazz.getMethod("getInstance", Context.class);
        instance = createMethod.invoke(null, context);
      }

      // Appel de la méthode demandée
      return clazz.getMethod(methodName, paramTypes).invoke(instance, params);

    } catch (Exception e) {
      Log.e("SmdtPlugin", "Error invoking method: " + methodName, e);
      return null;
    }
  }


  public void onMethodCallOld(@NonNull MethodCall call, @NonNull Result result) {
    Log.d("SmdtPluginOld", "Method called: " + call.method);
    Object res;

    switch (call.method) {

      // (string) getPlatformVersion
      case "getPlatformVersion":
        result.success("Android " + android.os.Build.VERSION.RELEASE);
        break;

      // (string) smdtGetAPIVersion
      case "smdtGetAPIVersion":
        res = invokeLibMethod(
            applicationContext,
            "smdtGetAPIVersion",
            new Class[] {},
            new Object[] {});
      
        result.success(res.toString());
        break;

      // (string) smdtGetCurrentNetType
      case "smdtGetCurrentNetType":
        res = invokeLibMethod(
            applicationContext,
            "getCurrentNetType",
            new Class[] {},
            new Object[] {});
      
        result.success(res.toString());
        break;

      // smdtSetStatusBar
      case "smdtSetStatusBar":
        invokeLibMethod(
            applicationContext,
            "smdtSetStatusBar",
            new Class[] {Context.class, boolean.class},
            new Object[] {applicationContext, call.argument("enable")});
      
        result.success(null);
        break;     

      // (string) smdtGetAndroidBoardType
      case "smdtGetAndroidBoardType":
        res = invokeLibMethod(
            applicationContext,
            "getAndroidBoardType",
            new Class[] {},
            new Object[] {});
      
        result.success(res.toString());
        break;

      /// (int) smdtGetVolume
      case "smdtGetVolume":
        res = invokeLibMethod(
            applicationContext,
            "smdtGetVolume",
            new Class[] {Context.class},
            new Object[] {applicationContext});
      
        if (res != null) {
          result.success((Integer) res);
        } else {
          result.error("UNAVAILABLE", "SmdtManagerOld is not available", null);
          Log.d("SmdtPluginOld", "SmdtManager is not available");
        }
        break;

      case "smdtSetDaemonsActivity":
        result.success(null);
        break;
      case "smdtSetSystemBootApp":
        result.success(null);
        break;
      case "smdtSetNavigationBar":
        result.success(null);
        break;

      // smdtGetSDCardPath
      case "smdtGetSDCardPath":
        res = invokeLibMethod(
            applicationContext,
            "smdtGetSDcardPath",
            new Class[] {Context.class},
            new Object[] {applicationContext});

        result.success(res.toString());
        break;

      /// smdtGetVolumeMaxMin
      case "smdtGetVolumeMaxMin":
        res = invokeLibMethod(
            applicationContext,
            "smdtGetVolume",
            new Class[] {Context.class},
            new Object[] {applicationContext});
      
        if (res != null) {
          result.success(new int[] {(Integer) res, 0});
        } else {
          result.error("UNAVAILABLE", "SmdtManagerOld is not available", null);
          Log.d("SmdtPluginOld", "SmdtManager is not available");
        }
        break;

      /// (void) smdtSetVolume(int)
      case "smdtSetVolume":
        int volume = call.argument("volume");
        invokeLibMethod(
            applicationContext,
            "smdtSetVolume",
            new Class[] {Context.class, int.class},
            new Object[] {applicationContext, volume});
      
        result.success(null);
        break;

      /// (void) smdtReboot
      case "smdtReboot":
        invokeLibMethod(
            applicationContext,
            "smdtReboot",
            new Class[] {},
            new Object[] {});
      
        result.success(null);
        break;

      /// (void) smdtShutdown
      case "smdtShutdown":
        invokeLibMethod(
            applicationContext,
            "shutDown",
            new Class[] {},
            new Object[] {});
      
        result.success(null);
        break;

      /// (void) smdtInstall(string)
      case "smdtInstall":
        final String apkPath = call.argument("apkPath");
        /// Démarre un thread pour l'installation silencieuse
        Thread t = new Thread() {
          @Override
          public void run() {
            if (!apkPath.equals("")) {

              invokeLibMethod(
                  applicationContext,
                  "smdtSilentInstall",
                  new Class[] {String.class,
                      Context.class},
                  new Object[] {apkPath, applicationContext});
              // Message msg = new Message();
              // msg.what = 0x123;
            }
          }
        };
        t.start();
        result.success(null);
        break;

      /// (string) getLogCat
      case "getLogCat":
        getLogCat(result);
        break;

      default:
        result.notImplemented();
    }

  }

  private void getLogCat(Result result) {
    try {
      Process process = Runtime.getRuntime().exec("logcat -d -D");

      StringBuilder log = new StringBuilder();
      BufferedReader bufferedReader = new BufferedReader(
          new InputStreamReader(process.getInputStream()));

      String line = "";
      while ((line = bufferedReader.readLine()) != null) {
        log.append(line);
        log.append("\n");
      }
      result.success(log.toString());
    } catch (IOException e) {
      result.error("IO_ERROR", "Error reading logcat", e.getMessage());
      return;
    }
  }

  public void onMethodCallNew(@NonNull MethodCall call, @NonNull Result result) {
    Log.d("onMethodCallNew", "Method called: " + call.method);
    
    switch (call.method) {
      case "getPlatformVersion":
        result.success("Android " + android.os.Build.VERSION.RELEASE);
        break;

      // (string) smdtGetAPIVersion
      case "smdtGetAPIVersion":
        Object res1 = invokeLibMethod(
            applicationContext,
            "info_getApiVersion",
            new Class[] {},
            new Object[] {});

        result.success(res1.toString());
        break;

      // smdtGetVolumeMaxMin
      case "smdtGetVolumeMaxMin":
        int[] volumeMaxMin = new int[2];
        Object res = invokeLibMethod(
            applicationContext,
            "sys_getVolumeMaxMin",
            new Class[] {String.class},
            new Object[] {"max"});

        volumeMaxMin[0] = (res != null) ? (Integer) res : -1;
        res = invokeLibMethod(
            applicationContext,
            "sys_getVolumeMaxMin",
            new Class[] {String.class},
            new Object[] {"min"});

        volumeMaxMin[1] = (res != null) ? (Integer) res : -1;
        result.success(volumeMaxMin);
        break;

      // smdtGetSDCardPath
      case "smdtGetSDCardPath":
        res = invokeLibMethod(
            applicationContext,
            "dev_getSDcardPath",
            new Class[] {},
            new Object[] {});

        result.success(res.toString());
        break;

      // smdtSetDaemonsActivity
      case "smdtSetDaemonsActivity":
        final String packageName = call.argument("packageName");
        final long timeout = ((int)call.argument("timeout")).longValue();
        final boolean enable = call.argument("enable");  
        invokeLibMethod(
            applicationContext,
            "sys_setDaemonsActivity",
            new Class[] {String.class, long.class, boolean.class},
            new Object[] {packageName, timeout, enable});

        result.success(null);
        break;

      // smdtGetSystemBootApp
      case "smdtSetSystemBootApp":
        final String p1 = call.argument("packageName");  
        invokeLibMethod(
            applicationContext,
            "sys_setSystemBootApp",
            new Class[] {String.class},
            new Object[] {p1});

        result.success(null);
        break;

      // (string) smdtGetCurrentNetType
      case "smdtGetCurrentNetType":
        res = invokeLibMethod(
            applicationContext,
            "net_getCurrentNetType",
            new Class[] {},
            new Object[] {});

        result.success(res.toString());
        break;
      
      // smdtSetStatusBar
      case "smdtSetStatusBar":
        invokeLibMethod(
            applicationContext,
            "disp_setStatusBar",
            new Class[] {boolean.class},
            new Object[] {call.argument("enable")});
        invokeLibMethod(
            applicationContext,
            "disp_setStatusBarDrag",
            new Class[] {boolean.class},
            new Object[] {call.argument("enable")});
      
        result.success(null);
        break;
      
      // smdtSetNavigationBar
      case "smdtSetNavigationBar":
        invokeLibMethod(
            applicationContext,
            "disp_setNavigationBar",
            new Class[] {boolean.class},
            new Object[] {call.argument("enable")});
        invokeLibMethod(
            applicationContext,
            "disp_setGestureBar",
            new Class[] {boolean.class},
            new Object[] {call.argument("enable")});
      
        result.success(null);
        break;

      // (string) smdtGetAndroidBoardType
      case "smdtGetAndroidBoardType":
        res = invokeLibMethod(
            applicationContext,
            "info_getBoardType",
            new Class[] {},
            new Object[] {});
      
        result.success(res.toString());
        break;

      /// (int) smdtGetVolume
      case "smdtGetVolume":
        res = invokeLibMethod(
            applicationContext,
            "sys_getVolume",
            new Class[] {},
            new Object[] {});
      
        if (res != null) {
          result.success((Integer) res);
        } else {
          result.error("UNAVAILABLE", "SmdtManagerNew is not available", null);
          Log.d("SmdtPluginNew", "SmdtManager is not available");
        }
        break;

      /// (void) smdtSetVolume(int)
      case "smdtSetVolume":
        final int volume = call.argument("volume");
        invokeLibMethod(
            applicationContext,
            "sys_setVolume",
            new Class[] {int.class},
            new Object[] {volume});
      
        result.success(null);
        break;

      /// (void) smdtReboot
      case "smdtReboot":
        invokeLibMethod(
            applicationContext,
            "sys_setReboot",
            new Class[] {},
            new Object[] {});
      
        result.success(null);
        break;

      /// (void) smdtShutdown
      case "smdtShutdown":
        invokeLibMethod(
            applicationContext,
            "sys_setPowerOff",
            new Class[] {},
            new Object[] {});
      
        result.success(null);
        break;

      /// (void) smdtInstall(string)
      case "smdtInstall":
        final String apkPath = call.argument("apkPath");
        /// Démarre un thread pour l'installation silencieuse
        Thread t = new Thread() {
          @Override
          public void run() {
            if (!apkPath.equals("")) {

              invokeLibMethod(
                  applicationContext,
                  "sys_doSilentInstallApp",
                  new Class[] {String.class, Context.class},
                  new Object[] {apkPath, applicationContext});
              // Message msg = new Message();
              // msg.what = 0x123;
            }
          }
        };
        t.start();
        result.success(null);
        break;

      /// (string) getLogCat
      case "getLogCat":
        getLogCat(result);
        break;

      default:
        result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }
}
