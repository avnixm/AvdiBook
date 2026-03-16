import '../models/audiobook.dart';

abstract class AudiobookRepository {
  Future<List<Audiobook>> getLibrary();
  Future<void> saveLibrary(List<Audiobook> books);
  Future<void> addBooks(List<Audiobook> books);
  Future<void> clearLibrary();
}
