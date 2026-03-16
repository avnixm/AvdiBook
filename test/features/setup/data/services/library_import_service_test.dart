import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

import 'package:avdibook/features/setup/data/services/library_import_service.dart';

void main() {
  late Directory tempDir;
  late LibraryImportService service;

  setUp(() async {
    tempDir = await Directory.systemTemp.createTemp('avdibook_import_test_');
    service = LibraryImportService();
  });

  tearDown(() async {
    if (await tempDir.exists()) {
      await tempDir.delete(recursive: true);
    }
  });

  test('treats root files as individual books and subfolders as chaptered books',
      () async {
    await _createFile('${tempDir.path}/File One.mp3');
    await _createFile('${tempDir.path}/File Two.m4b');
    await _createFile('${tempDir.path}/Book C/ch1.mp3');
    await _createFile('${tempDir.path}/Book C/Disc 2/ch2.mp3');
    await _createFile('${tempDir.path}/Book D/01.mp3');
    await _createFile('${tempDir.path}/ignore.txt');

    final imported = await service.scanFromPath(tempDir.path);

    expect(imported.length, 4);
    expect(imported.map((b) => b.title), containsAll(['File One', 'File Two', 'Book C', 'Book D']));

    final fileOne = imported.firstWhere((b) => b.title == 'File One');
    expect(fileOne.filePaths.length, 1);
    expect(fileOne.filePaths.single.endsWith('File One.mp3'), isTrue);

    final fileTwo = imported.firstWhere((b) => b.title == 'File Two');
    expect(fileTwo.filePaths.length, 1);
    expect(fileTwo.filePaths.single.endsWith('File Two.m4b'), isTrue);

    final bookC = imported.firstWhere((b) => b.title == 'Book C');
    expect(bookC.filePaths.length, 2);
    expect(bookC.filePaths.any((p) => p.endsWith('Book C/ch1.mp3')), isTrue);
    expect(bookC.filePaths.any((p) => p.endsWith('Book C/Disc 2/ch2.mp3')), isTrue);

    final bookD = imported.firstWhere((b) => b.title == 'Book D');
    expect(bookD.filePaths.length, 1);
    expect(bookD.filePaths.single.endsWith('Book D/01.mp3'), isTrue);
  });

  test('keeps root files separate even when base names match', () async {
    await _createFile('${tempDir.path}/Same.mp3');
    await _createFile('${tempDir.path}/Same.m4a');

    final imported = await service.scanFromPath(tempDir.path);

    expect(imported.length, 2);
    expect(imported.every((book) => book.title == 'Same'), isTrue);
    expect(imported.every((book) => book.filePaths.length == 1), isTrue);
  });
}

Future<void> _createFile(String path) async {
  final file = File(path);
  await file.parent.create(recursive: true);
  await file.writeAsString('dummy');
}
