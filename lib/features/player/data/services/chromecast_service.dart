import 'dart:io';

import 'package:flutter/services.dart';

class CastDevice {
  const CastDevice({
    required this.id,
    required this.name,
  });

  final String id;
  final String name;

  factory CastDevice.fromMap(Map<dynamic, dynamic> map) {
    return CastDevice(
      id: map['id']?.toString() ?? '',
      name: map['name']?.toString() ?? 'Chromecast device',
    );
  }
}

class ChromecastService {
  static const _channel = MethodChannel('avdibook/chromecast');

  Future<List<CastDevice>> discoverDevices() async {
    if (!Platform.isAndroid) return const [];
    final raw = await _channel.invokeMethod<List<dynamic>>('discoverDevices');
    if (raw == null) return const [];

    return raw
        .whereType<Map<dynamic, dynamic>>()
        .map(CastDevice.fromMap)
        .where((d) => d.id.isNotEmpty)
        .toList();
  }

  Future<bool> connect(String deviceId) async {
    if (!Platform.isAndroid) return false;
    final connected = await _channel.invokeMethod<bool>('connect', {
      'deviceId': deviceId,
    });
    return connected ?? false;
  }

  Future<void> disconnect() async {
    if (!Platform.isAndroid) return;
    await _channel.invokeMethod<void>('disconnect');
  }
}
