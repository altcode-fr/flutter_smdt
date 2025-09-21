import 'smdt_platform_interface.dart';

class Smdt {
  Future<String?> smdtGetSDCardPath() {
    return SmdtPlatform.instance.smdtGetSDCardPath();
  }

  Future<List<int>> smdtGetVolumeMaxMin() {
    return SmdtPlatform.instance.smdtGetVolumeMaxMin();
  }

  Future<void> smdtSetDaemonsActivity(String packageName, int timeout, bool enable) {
    return SmdtPlatform.instance.smdtSetDaemonsActivity(packageName, timeout, enable);
  }

  Future<void> smdtSetSystemBootApp(String packageName) {
    return SmdtPlatform.instance.smdtSetSystemBootApp(packageName);
  }

  Future<String?> smdtGetCurrentNetType() {
    return SmdtPlatform.instance.smdtGetCurrentNetType();
  }

  Future<String?> smdtGetAndroidBoardType() {
    return SmdtPlatform.instance.smdtGetAndroidBoardType();
  }

  Future<void> smdtSetStatusBar(bool enable) {
    return SmdtPlatform.instance.smdtSetStatusBar(enable);
  }

  Future<void> smdtSetNavigationBar(bool enable) {
    return SmdtPlatform.instance.smdtSetNavigationBar(enable);
  }

  Future<int?> smdtGetVolume() {
    return SmdtPlatform.instance.smdtGetVolume();
  }

  Future<void> smdtSetVolume(int volume) {
    return SmdtPlatform.instance.smdtSetVolume(volume);
  }

  Future<void> smdtReboot() {
    return SmdtPlatform.instance.smdtReboot();
  }

  Future<void> smdtShutdown() {
    return SmdtPlatform.instance.smdtShutdown();
  }

  Future<String?> getLogCat() {
    return SmdtPlatform.instance.getLogCat();
  }

  Future<void> smdtInstall(String apkPath) {
    return SmdtPlatform.instance.smdtInstall(apkPath);
  }

  Future<String?> getPlatformVersion() {
    return SmdtPlatform.instance.getPlatformVersion();
  }
}
