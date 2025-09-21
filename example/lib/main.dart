import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:smdt/smdt.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  String _currentNetType = 'Unknown';
  String _currentModel = 'Unknown';
  int _currentVolume = -1;
  final _smdtPlugin = Smdt();

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    String currentNetType;
    String currentModel;
    int currentVolume;

    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      platformVersion = await _smdtPlugin.getPlatformVersion() ?? 'Unknown platform version';
    } on PlatformException catch (e) {
      platformVersion = 'Failed to get platform version. $e';
    }

    try {
      currentNetType = await _smdtPlugin.smdtGetCurrentNetType() ?? 'Unknown current net type';
    } on PlatformException catch (e) {
      currentNetType = 'Failed to get current net type. $e';
    }
    try {
      currentModel = await _smdtPlugin.smdtGetAndroidBoardType() ?? 'Unknown current model';
    } on PlatformException catch (e) {
      currentModel = 'Failed to get current model. $e';
    }
    try {
      currentVolume = await _smdtPlugin.smdtGetVolume() ?? -1;
    } on PlatformException {
      currentVolume = -1;
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    await _smdtPlugin.smdtSetStatusBar(false);
    await _smdtPlugin.smdtSetNavigationBar(false);

    setState(() {
      _platformVersion = platformVersion;
      _currentNetType = currentNetType;
      _currentModel = currentModel;
      _currentVolume = currentVolume;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Plugin example app')),
        body: Column(
          children: [
            Center(child: Text('Running on: $_platformVersion\n')),
            Center(child: Text('Running on: $_currentNetType\n$_currentModel')),
            Center(child: Text('Current volume: $_currentVolume')),
            ElevatedButton(onPressed: onPressed, child: Text('Reboot')),
          ],
        ),
      ),
    );
  }

  void onPressed() async {
    await _smdtPlugin.smdtReboot();
  }
}
