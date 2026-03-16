import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/audiobooks/domain/models/audiobook.dart';

class LibraryNotifier extends Notifier<List<Audiobook>> {
  @override
  List<Audiobook> build() => const [];

  void setLibrary(List<Audiobook> items) {
    state = items;
  }
}

final libraryProvider = NotifierProvider<LibraryNotifier, List<Audiobook>>(
  LibraryNotifier.new,
);
