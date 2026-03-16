import 'package:equatable/equatable.dart';

class AudiobookAuthor extends Equatable {
  const AudiobookAuthor({
    required this.name,
  });

  final String name;

  @override
  List<Object?> get props => [name];
}
