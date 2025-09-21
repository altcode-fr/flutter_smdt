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

  private void listClassesInJar(File jarFile) {
    try (ZipFile zip = new ZipFile(jarFile)) {
      Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
      while (entries.hasMoreElements()) {
        String name = entries.nextElement().getName();
        if (name.endsWith(".class") && name.contains("Smdt")) {
          Log.d("PLUGIN", "Class trouvée: " + name.replace("/", "."));
        }
      }
    } catch (Exception e) {
      Log.e("PLUGIN", "Erreur lecture jar", e);
    }
  }

  private ClassLoader loadDynamicJar(Context context) throws Exception {
    String jarFileName;

    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) { // Android 10
      jarFileName = "smdt_old.jar";
    } else {
      jarFileName = "smdt_new_dex.jar";
    }

    Log.d("SmdtPlugin", "Loading jar file: " + jarFileName);

    // Copie du jar depuis assets ou libs vers un répertoire exécutable
    File dexDir = context.getDir("dex", Context.MODE_PRIVATE);
    File jarFile = new File(dexDir, jarFileName);
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

    listClassesInJar(jarFile); // Liste les classes avant de copier
    Log.d("SmdtPlugin", "Jar chargé : " + jarFile.getAbsolutePath());

    return new dalvik.system.DexClassLoader(
        jarFile.getAbsolutePath(),
        dexDir.getAbsolutePath(),
        null,
        context.getClassLoader());
  }

  /// Invocation d'une méthode du jar dynamique
  private Class<?> clazz;
  public Object invokeLibMethod(Context context, String methodName, Class<?>[] paramTypes, Object[] params) {
        
    // Charge la classe une seule fois
    if(clazz == null) {
      
      ClassLoader cl;
      
      // Charge le jar dynamiquement
      try {
        cl = loadDynamicJar(context);
      } catch (Exception e) {
        Log.e("SmdtPlugin", "Error loading jar", e);
        return null;
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
        return null;
      }
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

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    this.flutterPluginBinding = binding;

    // Create a MethodChannel that will handle communication between Flutter and Android
    if (binding.getBinaryMessenger() == null) {
      return;
    }

    channel = new MethodChannel(binding.getBinaryMessenger(), "smdt");
    channel.setMethodCallHandler(this);    

    applicationContext = binding.getApplicationContext();
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    Log.d("SmdtPlugin", "Method called: " + call.method);
    Log.d("SmdtPlugin", "SDK Version: " + android.os.Build.VERSION.SDK_INT);

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

  public void onMethodCallOld(@NonNull MethodCall call, @NonNull Result result) {
    Log.d("SmdtPluginOld", "Method called: " + call.method);
    Object res;

    switch (call.method) {

      // (string) getPlatformVersion
      case "getPlatformVersion":
        result.success("Android " + android.os.Build.VERSION.RELEASE);
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
        final int timeout = call.argument("timeout");
        final boolean enable = call.argument("enable");  
        invokeLibMethod(
            applicationContext,
            "sys_setDaemonsActivity",
            new Class[] {String.class, int.class, boolean.class},
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
