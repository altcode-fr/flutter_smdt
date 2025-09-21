import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'smdt_platform_interface.dart';

/// An implementation of [SmdtPlatform] that uses method channels.
class MethodChannelSmdt extends SmdtPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('smdt');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<String?> smdtGetAPIVersion() async {
    final version = await methodChannel.invokeMethod<String>('smdtGetAPIVersion');
    return version;
  }

  @override
  Future<List<int>> smdtGetVolumeMaxMin() async {
    final List<int> volumeRange = await methodChannel.invokeMethod<List<int>>('smdtGetVolumeMaxMin') ?? [0, 100];
    return volumeRange;
  }

  @override
  Future<String?> smdtGetSDCardPath() async {
    return await methodChannel.invokeMethod<String>('smdtGetSDCardPath');
  }

  @override
  Future<void> smdtSetDaemonsActivity(String packageName, int timeout, bool enable) async {
    await methodChannel.invokeMethod<void>('smdtSetDaemonsActivity', <String, dynamic>{
      'packageName': packageName,
      'timeout': timeout,
      'enable': enable,
    });
  }

  @override
  Future<void> smdtSetSystemBootApp(String packageName) async {
    await methodChannel.invokeMethod<void>('smdtSetSystemBootApp', <String, dynamic>{'packageName': packageName});
  }

  @override
  Future<String?> smdtGetCurrentNetType() async {
    final netType = await methodChannel.invokeMethod<String>('smdtGetCurrentNetType');
    return netType;
  }

  @override
  Future<String?> smdtGetAndroidBoardType() async {
    final version = await methodChannel.invokeMethod<String>('smdtGetAndroidBoardType');
    return version;
  }

  @override
  Future<void> smdtSetStatusBar(bool enable) async {
    await methodChannel.invokeMethod<void>('smdtSetStatusBar', <String, dynamic>{'enable': enable});
  }

  @override
  Future<void> smdtSetNavigationBar(bool enable) async {
    await methodChannel.invokeMethod<void>('smdtSetNavigationBar', <String, dynamic>{'enable': enable});
  }

  @override
  Future<int?> smdtGetVolume() async {
    final volume = await methodChannel.invokeMethod<int>('smdtGetVolume');
    return volume;
  }

  @override
  Future<void> smdtSetVolume(int volume) async {
    await methodChannel.invokeMethod<void>('smdtSetVolume', <String, dynamic>{'volume': volume});
  }

  @override
  Future<void> smdtReboot() async {
    await methodChannel.invokeMethod<void>('smdtReboot');
  }

  @override
  Future<void> smdtShutdown() async {
    await methodChannel.invokeMethod<void>('smdtShutdown');
  }

  @override
  Future<String?> getLogCat() async {
    final logcat = await methodChannel.invokeMethod<String>('getLogCat');
    return logcat;
  }

  @override
  Future<void> smdtInstall(String apkPath) async {
    await methodChannel.invokeMethod<void>('smdtInstall', <String, dynamic>{'apkPath': apkPath});
  }
}
