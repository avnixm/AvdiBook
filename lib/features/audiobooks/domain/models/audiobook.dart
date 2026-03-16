import 'package:equatable/equatable.dart';

import 'audiobook_author.dart';
import 'audiobook_chapter.dart';

class Audiobook extends Equatable {
  const Audiobook({
    required this.id,
    required this.title,
    required this.chapters,
    required this.sourcePaths,
    required this.primaryFormat,
    required this.importedAt,
    this.author,
    this.narrator,
    this.description,
    this.coverPath,
    this.series,
    this.genre,
  });

  final String id;
  final String title;
  final AudiobookAuthor? author;
  final String? narrator;
  final String? description;
  final String? coverPath;
  final String? series;
  final String? genre;
  final List<AudiobookChapter> chapters;
  final List<String> sourcePaths;
  final String primaryFormat;
  final DateTime importedAt;

  bool get isSingleFile => sourcePaths.length == 1;
  int get chapterCount => chapters.length;

  Audiobook copyWith({
    String? id,
    String? title,
    AudiobookAuthor? author,
    bool clearAuthor = false,
    String? narrator,
    bool clearNarrator = false,
    String? description,
    bool clearDescription = false,
    String? coverPath,
    bool clearCoverPath = false,
    String? series,
    bool clearSeries = false,
    String? genre,
    bool clearGenre = false,
    List<AudiobookChapter>? chapters,
    List<String>? sourcePaths,
    String? primaryFormat,
    DateTime? importedAt,
  }) {
    return Audiobook(
      id: id ?? this.id,
      title: title ?? this.title,
      author: clearAuthor ? null : (author ?? this.author),
      narrator: clearNarrator ? null : (narrator ?? this.narrator),
      description:
          clearDescription ? null : (description ?? this.description),
      coverPath: clearCoverPath ? null : (coverPath ?? this.coverPath),
      series: clearSeries ? null : (series ?? this.series),
      genre: clearGenre ? null : (genre ?? this.genre),
      chapters: chapters ?? this.chapters,
      sourcePaths: sourcePaths ?? this.sourcePaths,
      primaryFormat: primaryFormat ?? this.primaryFormat,
      importedAt: importedAt ?? this.importedAt,
    );
  }

  @override
  List<Object?> get props => [
        id,
        title,
        author,
        narrator,
        description,
        coverPath,
        series,
        genre,
        chapters,
        sourcePaths,
        primaryFormat,
        importedAt,
      ];
}
