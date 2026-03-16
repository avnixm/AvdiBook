import 'package:equatable/equatable.dart';

class AudiobookChapter extends Equatable {
  const AudiobookChapter({
    required this.id,
    required this.title,
    required this.filePath,
    required this.index,
    this.startOffset,
    this.duration,
  });

  final String id;
  final String title;
  final String filePath;
  final int index;
  final Duration? startOffset;
  final Duration? duration;

  @override
  List<Object?> get props => [
        id,
        title,
        filePath,
        index,
        startOffset,
        duration,
      ];
}
