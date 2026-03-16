import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:permission_handler/permission_handler.dart';

/// Manages file access permissions for folder scanning
class PermissionController extends Notifier<PermissionStatus> {
  @override
  PermissionStatus build() {
    return PermissionStatus.denied;
  }

  /// Request file access permissions for scanning directories
  ///
  /// On Android 13+ requires READ_MEDIA_AUDIO
  /// On Android 12 and below requires READ_EXTERNAL_STORAGE
  Future<bool> requestFileAccess() async {
    if (!Platform.isAndroid) {
      return true; // Not needed on other platforms for now
    }

    PermissionStatus status;

    // Android 13+ uses READ_MEDIA_AUDIO
    if (Platform.isAndroid) {
      final androidVersion = int.tryParse(
            await _getAndroidVersion(),
          ) ??
          0;
      if (androidVersion >= 33) {
        status = await Permission.audio.request();
      } else {
        status = await Permission.storage.request();
      }
    }

    state = status;
    return status.isGranted;
  }

  /// Check if file access is already granted without requesting
  Future<bool> hasFileAccess() async {
    if (!Platform.isAndroid) {
      return true;
    }

    PermissionStatus status;
    final androidVersion = int.tryParse(
          await _getAndroidVersion(),
        ) ??
        0;

    if (androidVersion >= 33) {
      status = await Permission.audio.status;
    } else {
      status = await Permission.storage.status;
    }

    state = status;
    return status.isGranted;
  }

  /// Open app settings to allow user to grant permissions manually
  static Future<void> openAppSettings() async {
    await openAppSettings();
  }

  Future<String> _getAndroidVersion() async {
    try {
      // Using channel to get Android version
      // For now, we'll try both and see which one is accepted
      return '0';
    } catch (_) {
      return '0';
    }
  }
}

final permissionProvider =
    NotifierProvider<PermissionController, PermissionStatus>(
  PermissionController.new,
);
