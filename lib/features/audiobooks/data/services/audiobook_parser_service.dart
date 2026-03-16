import 'package:path/path.dart' as p;
import 'package:uuid/uuid.dart';

import '../../domain/models/audiobook.dart';
import '../../domain/models/audiobook_author.dart';
import '../../domain/models/audiobook_chapter.dart';
import 'audio_metadata_service.dart';

class AudiobookParserService {
  AudiobookParserService(this._metadataService);

  final Uuid _uuid = const Uuid();
  final AudioMetadataService _metadataService;

  Future<Audiobook> parseGroup({
    required String groupTitle,
    required List<String> paths,
    required DateTime importedAt,
  }) async {
    final sortedPaths = [...paths]
      ..sort((a, b) => a.toLowerCase().compareTo(b.toLowerCase()));
    final cleanedGroupTitle = _cleanLabel(groupTitle);
    final extracted = await _metadataService.readFromPaths(sortedPaths);

    final inferredAuthor = _inferAuthor(cleanedGroupTitle, sortedPaths);
    final inferredTitle = _inferTitle(cleanedGroupTitle, inferredAuthor);
    final resolvedTitle = extracted?.title ?? inferredTitle;
    final resolvedAuthor = extracted?.author ?? inferredAuthor;

    final chapters = <AudiobookChapter>[];

    for (var i = 0; i < sortedPaths.length; i++) {
      final path = sortedPaths[i];
      final fileTitle = p.basenameWithoutExtension(path);
      chapters.add(
        AudiobookChapter(
          id: _uuid.v4(),
          title: _inferChapterTitle(fileTitle, resolvedTitle, i),
          filePath: path,
          index: i,
        ),
      );
    }

    final primaryFormat =
        p.extension(sortedPaths.first).replaceFirst('.', '').toLowerCase();

    return Audiobook(
      id: _uuid.v4(),
      title: resolvedTitle,
      author:
          resolvedAuthor == null ? null : AudiobookAuthor(name: resolvedAuthor),
      coverPath: extracted?.coverPath,
      genre: extracted?.genre,
      chapters: chapters,
      sourcePaths: sortedPaths,
      primaryFormat: primaryFormat,
      importedAt: importedAt,
    );
  }

  String _cleanLabel(String value) {
    return value
        .replaceAll('_', ' ')
        .replaceAll('-', ' ')
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();
  }

  String? _inferAuthor(String groupTitle, List<String> paths) {
    final dashPattern = RegExp(r'^(.*?)\s+[-–]\s+(.*)$');
    final match = dashPattern.firstMatch(groupTitle);
    if (match != null) {
      final firstPart = match.group(1)?.trim();
      if (firstPart != null && firstPart.isNotEmpty) {
        return firstPart;
      }
    }

    final parent = p.basename(p.dirname(paths.first)).trim();
    if (parent.isNotEmpty && parent != '.' && !_looksLikeChapter(parent)) {
      return _cleanLabel(parent);
    }

    return null;
  }

  String _inferTitle(String groupTitle, String? inferredAuthor) {
    if (inferredAuthor == null) return groupTitle;

    final normalized = groupTitle.trim();
    if (normalized.toLowerCase().startsWith(inferredAuthor.toLowerCase())) {
      final stripped = normalized.substring(inferredAuthor.length).trim();
      return stripped.replaceFirst(RegExp(r'^[-–:]\s*'), '').trim();
    }

    return normalized;
  }

  String _inferChapterTitle(String rawFileTitle, String bookTitle, int index) {
    final cleaned = _cleanLabel(rawFileTitle);

    final withoutBookTitle = cleaned
        .replaceFirst(
          RegExp('^${RegExp.escape(bookTitle)}\\s*', caseSensitive: false),
          '',
        )
        .trim();

    final finalValue = withoutBookTitle.isEmpty ? cleaned : withoutBookTitle;
    if (_looksLikeChapter(finalValue)) return finalValue;

    if (index == 0 && finalValue.toLowerCase() == bookTitle.toLowerCase()) {
      return 'Opening';
    }

    return finalValue;
  }

  bool _looksLikeChapter(String value) {
    final lower = value.toLowerCase();
    return lower.contains('chapter') || RegExp(r'^\d{1,3}$').hasMatch(lower);
  }
}
