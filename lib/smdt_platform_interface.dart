import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'smdt_method_channel.dart';

abstract class SmdtPlatform extends PlatformInterface {
  /// Constructs a SmdtPlatform.
  SmdtPlatform() : super(token: _token);

  static final Object _token = Object();

  static SmdtPlatform _instance = MethodChannelSmdt();

  /// The default instance of [SmdtPlatform] to use.
  ///
  /// Defaults to [MethodChannelSmdt].
  static SmdtPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [SmdtPlatform] when
  /// they register themselves.
  static set instance(SmdtPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<List<int>> smdtGetVolumeMaxMin() {
    throw UnimplementedError('smdtGetVolumeMaxMin() has not been implemented.');
  }

  Future<String?> smdtGetSDCardPath() {
    throw UnimplementedError('smdtGetSDCardPath() has not been implemented.');
  }

  Future<void> smdtSetDaemonsActivity(String packageName, int timeout, bool enable) {
    throw UnimplementedError('smdtSetDaemonsActivity() has not been implemented.');
  }

  Future<void> smdtSetSystemBootApp(String packageName) {
    throw UnimplementedError('smdtSetSystemBootApp() has not been implemented.');
  }

  Future<String?> smdtGetCurrentNetType() {
    throw UnimplementedError('smdtGetCurrentNetType() has not been implemented.');
  }

  Future<String?> smdtGetAndroidBoardType() {
    throw UnimplementedError('smdtGetAndroidBoardType() has not been implemented.');
  }

  Future<void> smdtSetStatusBar(bool enable) {
    throw UnimplementedError('smdtSetStatusBar() has not been implemented.');
  }

  Future<void> smdtSetNavigationBar(bool enable) {
    throw UnimplementedError('smdtSetNavigationBar() has not been implemented.');
  }

  Future<int?> smdtGetVolume() {
    throw UnimplementedError('smdtGetVolume() has not been implemented.');
  }

  Future<void> smdtSetVolume(int volume) {
    throw UnimplementedError('smdtSetVolume() has not been implemented.');
  }

  Future<void> smdtReboot() {
    throw UnimplementedError('smdtReboot() has not been implemented.');
  }

  Future<void> smdtShutdown() {
    throw UnimplementedError('smdtShutdown() has not been implemented.');
  }

  Future<String?> getLogCat() {
    throw UnimplementedError('getLogCat() has not been implemented.');
  }

  Future<void> smdtInstall(String apkPath) {
    throw UnimplementedError('smdtInstall() has not been implemented.');
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
  Future<String?> smdtGetAPIVersion() {
    throw UnimplementedError('smdtGetAPIVersion() has not been implemented.');
  }
}
