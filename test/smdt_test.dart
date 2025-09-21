import 'package:flutter_test/flutter_test.dart';
import 'package:smdt/smdt.dart';
import 'package:smdt/smdt_platform_interface.dart';
import 'package:smdt/smdt_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockSmdtPlatform with MockPlatformInterfaceMixin implements SmdtPlatform {
  @override
  Future<List<int>> smdtGetVolumeMaxMin() => Future.value([0, 100]);
  @override
  Future<String?> smdtGetSDCardPath() => Future.value('/sdcard');
  @override
  Future<void> smdtSetDaemonsActivity(String packageName, int timeout, bool enable) => Future.value();
  @override
  Future<void> smdtSetSystemBootApp(String packageName) => Future.value();
  @override
  Future<String?> smdtGetCurrentNetType() => Future.value('wifi');
  @override
  Future<String?> smdtGetAndroidBoardType() => Future.value('#');
  @override
  Future<String?> getLogCat() => Future.value('Logcat');
  @override
  Future<void> smdtInstall(String apkPath) => Future.value();
  @override
  Future<void> smdtReboot() => Future.value();
  @override
  Future<void> smdtShutdown() => Future.value();
  @override
  Future<void> smdtSetStatusBar(bool enable) => Future.value();
  @override
  Future<void> smdtSetNavigationBar(bool enable) => Future.value();
  @override
  Future<void> smdtSetVolume(int volume) => Future.value();
  @override
  Future<int?> smdtGetVolume() => Future.value(72);
  @override
  Future<String?> getPlatformVersion() {
    return SmdtPlatform.instance.getPlatformVersion();
  }
}

void main() {
  final SmdtPlatform initialPlatform = SmdtPlatform.instance;

  test('$MethodChannelSmdt is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelSmdt>());
  });

  test('getPlatformVersion', () async {
    Smdt smdtPlugin = Smdt();
    MockSmdtPlatform fakePlatform = MockSmdtPlatform();
    SmdtPlatform.instance = fakePlatform;

    expect(await smdtPlugin.getPlatformVersion(), '42');
  });
}
