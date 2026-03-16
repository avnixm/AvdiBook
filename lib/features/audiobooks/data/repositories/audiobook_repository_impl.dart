import '../../../setup/data/services/startup_storage_service.dart';
import '../../domain/models/audiobook.dart';
import '../../domain/repositories/audiobook_repository.dart';

class AudiobookRepositoryImpl implements AudiobookRepository {
  AudiobookRepositoryImpl(this._storage);

  final StartupStorageService _storage;

  @override
  Future<void> addBooks(List<Audiobook> books) async {
    final existing = await _storage.getLibraryItems();
    final seen = existing.map((e) => e.sourcePaths.join('|')).toSet();

    final merged = [
      ...books.where((book) => !seen.contains(book.sourcePaths.join('|'))),
      ...existing,
    ]..sort((a, b) => b.importedAt.compareTo(a.importedAt));

    await _storage.setLibraryItems(merged);
  }

  @override
  Future<void> clearLibrary() async {
    await _storage.setLibraryItems(const []);
  }

  @override
  Future<List<Audiobook>> getLibrary() {
    return _storage.getLibraryItems();
  }

  @override
  Future<void> saveLibrary(List<Audiobook> books) {
    return _storage.setLibraryItems(books);
  }
}
