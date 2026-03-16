import 'dart:io';

import 'package:audio_metadata_reader/audio_metadata_reader.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

class ExtractedAudioMetadata {
  const ExtractedAudioMetadata({
    this.title,
    this.author,
    this.genre,
    this.coverPath,
  });

  final String? title;
  final String? author;
  final String? genre;
  final String? coverPath;

  bool get hasAnyValue =>
      title != null || author != null || genre != null || coverPath != null;
}

class AudioMetadataService {
  Future<ExtractedAudioMetadata?> readFromPaths(List<String> filePaths) async {
    if (filePaths.isEmpty) return null;

    // Prefer container formats that commonly carry rich tags for audiobooks.
    final prioritized = [...filePaths]..sort(_sourcePriorityCompare);

    for (final path in prioritized) {
      try {
        final metadata = readMetadata(File(path), getImage: true);
        final title = _firstNonEmpty([metadata.title, metadata.album]);
        final performerNames = metadata.performers
            .map((name) => name.trim())
            .where((name) => name.isNotEmpty)
            .join(', ');
        final author = _firstNonEmpty([metadata.artist, performerNames]);
        final genre = metadata.genres.isEmpty ? null : _clean(metadata.genres.first);
        final picture = metadata.pictures.isEmpty ? null : metadata.pictures.first;
        final coverPath = await _storeCoverIfPresent(path, picture?.bytes);

        final extracted = ExtractedAudioMetadata(
          title: title,
          author: author,
          genre: genre,
          coverPath: coverPath,
        );

        if (extracted.hasAnyValue) return extracted;
      } catch (_) {
        // Keep trying other paths if one file cannot be parsed.
      }
    }

    return null;
  }

  Future<String?> _storeCoverIfPresent(
    String sourcePath,
    List<int>? coverBytes,
  ) async {
    if (coverBytes == null || coverBytes.isEmpty) return null;

    try {
      final supportDir = await getApplicationSupportDirectory();
      final coversDir = Directory(p.join(supportDir.path, 'covers'));
      if (!await coversDir.exists()) {
        await coversDir.create(recursive: true);
      }

      final extension = 'jpg';
      final safeBase = p
          .basenameWithoutExtension(sourcePath)
          .replaceAll(RegExp(r'[^a-zA-Z0-9_-]+'), '_');
      final fileName = 'cover_${safeBase}_${DateTime.now().millisecondsSinceEpoch}.$extension';
      final destination = File(p.join(coversDir.path, fileName));

      await destination.writeAsBytes(coverBytes, flush: true);
      return destination.path;
    } catch (_) {
      return null;
    }
  }

  int _sourcePriorityCompare(String a, String b) {
    final pa = _priorityForExtension(p.extension(a).toLowerCase());
    final pb = _priorityForExtension(p.extension(b).toLowerCase());
    if (pa != pb) return pa.compareTo(pb);
    return a.toLowerCase().compareTo(b.toLowerCase());
  }

  int _priorityForExtension(String ext) {
    switch (ext) {
      case '.m4b':
        return 0;
      case '.m4a':
        return 1;
      case '.mp3':
        return 2;
      default:
        return 3;
    }
  }

  String? _firstNonEmpty(List<String?> values) {
    for (final value in values) {
      final cleaned = _clean(value);
      if (cleaned != null) return cleaned;
    }
    return null;
  }

  String? _clean(String? value) {
    final normalized = value?.trim();
    if (normalized == null || normalized.isEmpty) return null;
    return normalized;
  }
}
