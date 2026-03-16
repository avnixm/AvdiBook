import 'dart:convert';

import 'package:equatable/equatable.dart';

class ImportedAudiobook extends Equatable {
  const ImportedAudiobook({
    required this.id,
    required this.title,
    required this.filePaths,
    required this.primaryFormat,
    required this.importedAt,
    this.author,
    this.coverPath,
  });

  final String id;
  final String title;
  final List<String> filePaths;
  final String primaryFormat;
  final DateTime importedAt;
  final String? author;
  final String? coverPath;

  ImportedAudiobook copyWith({
    String? id,
    String? title,
    List<String>? filePaths,
    String? primaryFormat,
    DateTime? importedAt,
    String? author,
    String? coverPath,
  }) {
    return ImportedAudiobook(
      id: id ?? this.id,
      title: title ?? this.title,
      filePaths: filePaths ?? this.filePaths,
      primaryFormat: primaryFormat ?? this.primaryFormat,
      importedAt: importedAt ?? this.importedAt,
      author: author ?? this.author,
      coverPath: coverPath ?? this.coverPath,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'title': title,
      'filePaths': filePaths,
      'primaryFormat': primaryFormat,
      'importedAt': importedAt.toIso8601String(),
      'author': author,
      'coverPath': coverPath,
    };
  }

  factory ImportedAudiobook.fromMap(Map<String, dynamic> map) {
    return ImportedAudiobook(
      id: map['id'] as String,
      title: map['title'] as String,
      filePaths: List<String>.from(map['filePaths'] as List),
      primaryFormat: map['primaryFormat'] as String,
      importedAt: DateTime.parse(map['importedAt'] as String),
      author: map['author'] as String?,
      coverPath: map['coverPath'] as String?,
    );
  }

  String toJson() => jsonEncode(toMap());

  factory ImportedAudiobook.fromJson(String source) {
    return ImportedAudiobook.fromMap(
      jsonDecode(source) as Map<String, dynamic>,
    );
  }

  static List<String> encodeList(List<ImportedAudiobook> items) {
    return items.map((item) => item.toJson()).toList();
  }

  static List<ImportedAudiobook> decodeList(List<String> items) {
    return items.map(ImportedAudiobook.fromJson).toList();
  }

  @override
  List<Object?> get props => [
        id,
        title,
        filePaths,
        primaryFormat,
        importedAt,
        author,
        coverPath,
      ];
}
