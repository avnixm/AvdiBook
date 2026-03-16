import 'package:drift/drift.dart';
import 'package:drift_flutter/drift_flutter.dart';

part 'app_database.g.dart';

class LibraryBooks extends Table {
  TextColumn get id => text()();
  TextColumn get payload => text()();
  IntColumn get updatedAtMs => integer()();

  @override
  Set<Column<Object>> get primaryKey => {id};
}

class KeyValueEntries extends Table {
  TextColumn get key => text()();
  TextColumn get value => text()();
  IntColumn get updatedAtMs => integer()();

  @override
  Set<Column<Object>> get primaryKey => {key};
}

@DriftDatabase(tables: [LibraryBooks, KeyValueEntries])
class AppDatabase extends _$AppDatabase {
  AppDatabase([QueryExecutor? executor])
      : super(executor ?? driftDatabase(name: 'avdibook_db'));

  @override
  int get schemaVersion => 1;

  Future<List<String>> getLibraryPayloads() async {
    final rows = await select(libraryBooks).get();
    rows.sort((a, b) => b.updatedAtMs.compareTo(a.updatedAtMs));
    return rows.map((row) => row.payload).toList();
  }

  Future<void> replaceLibraryPayloads(List<({String id, String payload})> items) {
    final nowMs = DateTime.now().millisecondsSinceEpoch;

    return transaction(() async {
      await delete(libraryBooks).go();
      if (items.isEmpty) return;

      await batch((batch) {
        batch.insertAll(
          libraryBooks,
          items
              .map(
                (item) => LibraryBooksCompanion.insert(
                  id: item.id,
                  payload: item.payload,
                  updatedAtMs: nowMs,
                ),
              )
              .toList(),
        );
      });
    });
  }

  Future<void> putKeyValue({required String key, required String value}) {
    return into(keyValueEntries).insertOnConflictUpdate(
      KeyValueEntriesCompanion.insert(
        key: key,
        value: value,
        updatedAtMs: DateTime.now().millisecondsSinceEpoch,
      ),
    );
  }

  Future<String?> getKeyValue(String key) async {
    final row = await (select(keyValueEntries)..where((tbl) => tbl.key.equals(key)))
        .getSingleOrNull();
    return row?.value;
  }
}
