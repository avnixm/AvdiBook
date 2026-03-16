import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:path/path.dart' as p;
import 'package:permission_handler/permission_handler.dart';
import 'package:uuid/uuid.dart';

import 'package:avdibook/features/setup/domain/models/imported_audiobook.dart';

class LibraryImportService {
  static const supportedExtensions = {
    'mp3',
    'm4b',
    'm4a',
    'aac',
    'wav',
    'ogg',
  };

  final _uuid = const Uuid();

  /// The last directory path selected by the user, for rescanning.
  String? lastSelectedDirectory;

  Future<List<ImportedAudiobook>> importFiles() async {
    try {
      final result = await FilePicker.platform.pickFiles(
        allowMultiple: true,
        type: FileType.custom,
        allowedExtensions: supportedExtensions.toList(),
      );

      if (result == null || result.files.isEmpty) {
        return [];
      }

      final paths = result.paths.whereType<String>().toList();
      final inferredRoot = _inferCommonRootDirectory(paths);
      return _groupPathsIntoBooks(paths, rootDirectoryPath: inferredRoot);
    } catch (e) {
      _log('Error importing files: $e');
      rethrow;
    }
  }

  Future<List<ImportedAudiobook>> importDirectory() async {
    try {
      // Request file access permissions first
      final hasPermission = await _requestFileAccess();
      if (!hasPermission) {
        throw Exception('File access permission denied');
      }

      _log('Opening directory picker...');
      final selectedDirectory = await FilePicker.platform.getDirectoryPath();
      _log('Directory picker result: $selectedDirectory');

      if (selectedDirectory == null || selectedDirectory.isEmpty) {
        _log('No directory selected (user cancelled or error)');
        return [];
      }

      lastSelectedDirectory = selectedDirectory;

      final dir = Directory(selectedDirectory);
      if (!await dir.exists()) {
        throw Exception('Directory does not exist: $selectedDirectory');
      }

      _log('Scanning directory: $selectedDirectory');

      // List all files with proper error handling
      final allFiles = <File>[];
      int totalEntities = 0;
      try {
        await for (final entity
            in dir.list(recursive: true, followLinks: false)) {
          totalEntities++;
          if (entity is File) {
            allFiles.add(entity);
          }
        }
      } catch (e) {
        _log('Error listing directory contents after scanning $totalEntities entities: $e');
        if (allFiles.isEmpty) {
          throw Exception('Could not read files from directory: ${e.toString()}');
        }
        // If we found some files before error, continue with what we have
        _log('Continuing with ${allFiles.length} files found before error');
      }

      _log('Found ${allFiles.length} files in directory (from $totalEntities entities)');

      final supportedPaths = allFiles
          .map((file) => file.path)
          .where(_isSupportedAudioPath)
          .toList();

      _log('Found ${supportedPaths.length} supported audio files');

      return _groupPathsIntoBooks(
        supportedPaths,
        rootDirectoryPath: selectedDirectory,
      );
    } catch (e) {
      _log('Error importing directory: $e');
      rethrow;
    }
  }

  /// Scans a specific directory path without opening the file picker.
  /// Used for rescanning a previously selected folder.
  Future<List<ImportedAudiobook>> scanFromPath(String dirPath) async {
    try {
      final hasPermission = await _requestFileAccess();
      if (!hasPermission) {
        throw Exception('File access permission denied');
      }

      final dir = Directory(dirPath);
      if (!await dir.exists()) {
        throw Exception('Directory does not exist: $dirPath');
      }

      _log('Rescanning directory: $dirPath');

      final allFiles = <File>[];
      int totalEntities = 0;
      try {
        await for (final entity
            in dir.list(recursive: true, followLinks: false)) {
          totalEntities++;
          if (entity is File) allFiles.add(entity);
        }
      } catch (e) {
        _log('Error listing directory after $totalEntities entities: $e');
        if (allFiles.isEmpty) {
          throw Exception('Could not read files from directory: ${e.toString()}');
        }
        _log('Continuing with ${allFiles.length} files found before error');
      }

      _log('Found ${allFiles.length} files in directory');

      final supportedPaths = allFiles
          .map((file) => file.path)
          .where(_isSupportedAudioPath)
          .toList();

      _log('Found ${supportedPaths.length} supported audio files');

      return _groupPathsIntoBooks(
        supportedPaths,
        rootDirectoryPath: dirPath,
      );
    } catch (e) {
      _log('Error rescanning directory: $e');
      rethrow;
    }
  }

  Future<bool> _requestFileAccess() async {
    if (!Platform.isAndroid) {
      return true;
    }

    try {
      _log('Requesting file access permissions...');

      // Try audio permission first (Android 13+)
      _log('Attempting to request audio permission (Android 13+)...');
      PermissionStatus status = await Permission.audio.request();
      _log('Audio permission status: $status');

      if (status.isGranted) {
        _log('Audio permission granted!');
        return true;
      }

      // If audio permission is not needed or failed, try storage permission
      _log('Audio permission not granted, attempting storage permission...');
      status = await Permission.storage.request();
      _log('Storage permission status: $status');

      if (status.isGranted) {
        _log('Storage permission granted!');
        return true;
      }

      // If still not granted, check if permission is permanently denied
      if (status.isDenied) {
        _log('Storage permission denied - user rejected');
        throw Exception('File access permission denied by user');
      }

      if (status.isPermanentlyDenied) {
        _log('Storage permission permanently denied - user disabled in settings');
        throw Exception(
            'File access permission permanently denied. Please enable it in app settings.');
      }

      _log('Storage permission status unknown: $status');
      return false;
    } catch (e) {
      _log('Error requesting permissions: $e');
      rethrow;
    }
  }

  bool _isSupportedAudioPath(String path) {
    final extension = p.extension(path).replaceFirst('.', '').toLowerCase();
    return supportedExtensions.contains(extension);
  }

  String? _inferCommonRootDirectory(List<String> paths) {
    if (paths.isEmpty) return null;

    final allDirSegments = paths
        .map((path) => p.split(p.normalize(p.dirname(path))))
        .toList();

    if (allDirSegments.any((segments) => segments.isEmpty)) {
      return null;
    }

    final first = allDirSegments.first;
    var commonLength = first.length;

    for (var i = 0; i < commonLength; i++) {
      final segment = first[i];
      final allMatch = allDirSegments.every((other) =>
          other.length > i && other[i].toLowerCase() == segment.toLowerCase());
      if (!allMatch) {
        commonLength = i;
        break;
      }
    }

    if (commonLength == 0) return null;
    return p.joinAll(first.take(commonLength));
  }

  List<ImportedAudiobook> _groupPathsIntoBooks(
    List<String> paths, {
    String? rootDirectoryPath,
  }) {
    final grouped = <String, List<String>>{};
    final groupTitles = <String, String>{};
    final normalizedRoot =
        rootDirectoryPath == null ? null : p.normalize(rootDirectoryPath);

    for (final path in paths) {
      final normalizedPath = p.normalize(path);
      String groupKey;
      String groupTitle;

      if (normalizedRoot != null) {
        final relativePath = p.relative(normalizedPath, from: normalizedRoot);
        final segments = p.split(relativePath);

        // For folder imports, root-level files are standalone books while
        // any nested path is grouped under its top-level subfolder.
        if (segments.isNotEmpty && segments.first != '..' && relativePath != '.') {
          if (segments.length == 1) {
            groupKey = 'file::$normalizedPath';
            groupTitle = p.basenameWithoutExtension(normalizedPath).trim();
          } else {
            final topLevelFolder = segments.first.trim();
            groupKey = 'folder::${topLevelFolder.toLowerCase()}';
            groupTitle = topLevelFolder;
          }

          grouped.putIfAbsent(groupKey, () => <String>[]).add(normalizedPath);
          groupTitles.putIfAbsent(groupKey, () => groupTitle);
          continue;
        }
      }

      final parentFolderName = p.basename(p.dirname(normalizedPath)).trim();
      final fileTitle = p.basenameWithoutExtension(normalizedPath).trim();
      groupKey =
          parentFolderName.isNotEmpty && parentFolderName != '.'
              ? 'legacy::$parentFolderName'
              : 'legacy-file::$fileTitle';
      groupTitle =
          parentFolderName.isNotEmpty && parentFolderName != '.'
              ? parentFolderName
              : fileTitle;

      grouped.putIfAbsent(groupKey, () => <String>[]).add(normalizedPath);
      groupTitles.putIfAbsent(groupKey, () => groupTitle);
    }

    final books = grouped.entries.map((entry) {
      final sortedPaths = [...entry.value]
        ..sort((a, b) => a.toLowerCase().compareTo(b.toLowerCase()));
      final firstExtension =
          p.extension(sortedPaths.first).replaceFirst('.', '').toLowerCase();
      final rawTitle = groupTitles[entry.key] ?? entry.key;

      return ImportedAudiobook(
        id: _uuid.v4(),
        title: _cleanTitle(rawTitle),
        filePaths: sortedPaths,
        primaryFormat: firstExtension,
        importedAt: DateTime.now(),
      );
    }).toList();

    books.sort((a, b) => b.importedAt.compareTo(a.importedAt));
    return books;
  }

  String _cleanTitle(String raw) {
    return raw
        .replaceAll('_', ' ')
        .replaceAll('-', ' ')
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();
  }

  static void _log(String message) {
    // Print for debugging; in production, use proper logging
    print('[LibraryImportService] $message');
  }
}

