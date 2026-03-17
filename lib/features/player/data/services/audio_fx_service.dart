import 'dart:io';

import 'package:flutter/services.dart';

class AudioFxCapabilities {
  const AudioFxCapabilities({
    required this.equalizerSupported,
    required this.loudnessSupported,
    required this.stereoBalanceSupported,
  });

  final bool equalizerSupported;
  final bool loudnessSupported;
  final bool stereoBalanceSupported;

  factory AudioFxCapabilities.fromMap(Map<dynamic, dynamic> map) {
    return AudioFxCapabilities(
      equalizerSupported: map['equalizerSupported'] == true,
      loudnessSupported: map['loudnessSupported'] == true,
      stereoBalanceSupported: map['stereoBalanceSupported'] == true,
    );
  }

  static const unsupported = AudioFxCapabilities(
    equalizerSupported: false,
    loudnessSupported: false,
    stereoBalanceSupported: false,
  );
}

class AudioFxService {
  static const _channel = MethodChannel('avdibook/audio_fx');

  Future<void> setAudioSessionId(int? sessionId) async {
    if (!Platform.isAndroid) return;
    await _channel.invokeMethod<void>('setAudioSessionId', {
      'id': sessionId,
    });
  }

  Future<void> setEqualizerEnabled(bool enabled) async {
    if (!Platform.isAndroid) return;
    await _channel.invokeMethod<void>('setEqualizerEnabled', {
      'enabled': enabled,
    });
  }

  Future<void> setEqualizerPreset(int preset) async {
    if (!Platform.isAndroid) return;
    await _channel.invokeMethod<void>('setEqualizerPreset', {
      'preset': preset,
    });
  }

  Future<List<String>> getEqualizerPresets() async {
    if (!Platform.isAndroid) return const [];
    final raw = await _channel.invokeMethod<List<dynamic>>('getEqualizerPresets');
    return raw?.whereType<String>().toList() ?? const [];
  }

  Future<void> setLoudnessBoost(double normalizedBoost) async {
    if (!Platform.isAndroid) return;
    final gainMb = (normalizedBoost.clamp(0.0, 1.0) * 1500).round();
    await _channel.invokeMethod<void>('setLoudnessBoost', {
      'gainMb': gainMb,
    });
  }

  Future<bool> setStereoBalance(double balance) async {
    if (!Platform.isAndroid) return false;
    final result = await _channel.invokeMethod<bool>('setStereoBalance', {
      'balance': balance.clamp(-1.0, 1.0),
    });
    return result ?? false;
  }

  Future<AudioFxCapabilities> getCapabilities() async {
    if (!Platform.isAndroid) return AudioFxCapabilities.unsupported;
    final raw = await _channel.invokeMethod<Map<dynamic, dynamic>>(
      'getCapabilities',
    );
    if (raw == null) return AudioFxCapabilities.unsupported;
    return AudioFxCapabilities.fromMap(raw);
  }
}
