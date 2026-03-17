import 'dart:async';

import 'package:avdibook/features/player/data/services/chromecast_service.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class CastSessionState {
  const CastSessionState({
    this.devices = const [],
    this.connectedDevice,
    this.isDiscovering = false,
    this.error,
  });

  final List<CastDevice> devices;
  final CastDevice? connectedDevice;
  final bool isDiscovering;
  final String? error;

  CastSessionState copyWith({
    List<CastDevice>? devices,
    CastDevice? connectedDevice,
    bool? isDiscovering,
    String? error,
    bool clearConnectedDevice = false,
    bool clearError = false,
  }) {
    return CastSessionState(
      devices: devices ?? this.devices,
      connectedDevice: clearConnectedDevice
          ? null
          : (connectedDevice ?? this.connectedDevice),
      isDiscovering: isDiscovering ?? this.isDiscovering,
      error: clearError ? null : (error ?? this.error),
    );
  }
}

class CastSessionNotifier extends Notifier<CastSessionState> {
  late final ChromecastService _service;

  @override
  CastSessionState build() {
    _service = ChromecastService();
    unawaited(refreshDevices());
    return const CastSessionState();
  }

  Future<void> refreshDevices() async {
    state = state.copyWith(isDiscovering: true, clearError: true);
    try {
      final devices = await _service.discoverDevices();
      final currentId = state.connectedDevice?.id;
      CastDevice? connected;
      if (currentId != null) {
        for (final device in devices) {
          if (device.id == currentId) {
            connected = device;
            break;
          }
        }
      }
      state = state.copyWith(
        devices: devices,
        connectedDevice: connected,
        isDiscovering: false,
      );
    } catch (_) {
      state = state.copyWith(
        isDiscovering: false,
        error: 'Chromecast discovery unavailable on this device.',
      );
    }
  }

  Future<void> connect(CastDevice device) async {
    final ok = await _service.connect(device.id);
    state = ok
        ? state.copyWith(connectedDevice: device, clearError: true)
        : state.copyWith(error: 'Unable to connect to cast device.');
  }

  Future<void> disconnect() async {
    await _service.disconnect();
    state = state.copyWith(clearConnectedDevice: true);
  }
}

final castSessionProvider =
    NotifierProvider<CastSessionNotifier, CastSessionState>(
  CastSessionNotifier.new,
);
