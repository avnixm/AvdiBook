import 'dart:convert';
import 'dart:async';

import 'package:avdibook/shared/providers/preferences_provider.dart';
import 'package:avdibook/shared/providers/storage_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';

class BookCharacter {
  const BookCharacter({
    required this.id,
    required this.name,
    this.role,
    this.note,
  });

  final String id;
  final String name;
  final String? role;
  final String? note;

  BookCharacter copyWith({
    String? id,
    String? name,
    String? role,
    bool clearRole = false,
    String? note,
    bool clearNote = false,
  }) {
    return BookCharacter(
      id: id ?? this.id,
      name: name ?? this.name,
      role: clearRole ? null : (role ?? this.role),
      note: clearNote ? null : (note ?? this.note),
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'role': role,
        'note': note,
      };

  static BookCharacter fromJson(Map<String, dynamic> json) {
    return BookCharacter(
      id: json['id'] as String,
      name: json['name'] as String,
      role: json['role'] as String?,
      note: json['note'] as String?,
    );
  }
}

class CharacterNotesNotifier extends Notifier<Map<String, List<BookCharacter>>> {
  static const _storageKey = 'book_characters_v1';
  final Uuid _uuid = const Uuid();

  @override
  Map<String, List<BookCharacter>> build() {
    final prefs = ref.watch(sharedPreferencesProvider);
    final raw = prefs.getString(_storageKey);
    if (raw == null || raw.isEmpty) {
      unawaited(_hydrateFromDriftIfNeeded());
      return const {};
    }

    try {
      final decoded = jsonDecode(raw) as Map<String, dynamic>;
      return decoded.map((bookId, list) {
        final entries = (list as List<dynamic>)
            .map((item) => BookCharacter.fromJson(item as Map<String, dynamic>))
            .toList();
        return MapEntry(bookId, entries);
      });
    } catch (_) {
      unawaited(_hydrateFromDriftIfNeeded());
      return const {};
    }
  }

  Future<void> _hydrateFromDriftIfNeeded() async {
    final storage = ref.read(startupStorageServiceProvider);
    final raw = await storage.loadCharacterNotesSnapshot();
    if (raw == null || raw.isEmpty || !ref.mounted) return;

    try {
      final decoded = jsonDecode(raw) as Map<String, dynamic>;
      final next = decoded.map((bookId, list) {
        final entries = (list as List<dynamic>)
            .map((item) => BookCharacter.fromJson(item as Map<String, dynamic>))
            .toList();
        return MapEntry(bookId, entries);
      });

      state = next;

      final prefs = ref.read(sharedPreferencesProvider);
      await prefs.setString(_storageKey, raw);
    } catch (_) {
      // Ignore malformed fallback payloads.
    }
  }

  List<BookCharacter> forBook(String bookId) => state[bookId] ?? const [];

  Future<void> add({
    required String bookId,
    required String name,
    String? role,
    String? note,
  }) async {
    final character = BookCharacter(
      id: _uuid.v4(),
      name: name.trim(),
      role: role?.trim().isEmpty ?? true ? null : role!.trim(),
      note: note?.trim().isEmpty ?? true ? null : note!.trim(),
    );
    final next = [...forBook(bookId), character];
    await _saveBook(bookId, next);
  }

  Future<void> update({
    required String bookId,
    required String id,
    required String name,
    String? role,
    String? note,
  }) async {
    final current = forBook(bookId);
    final index = current.indexWhere((entry) => entry.id == id);
    if (index < 0) return;

    final next = [...current];
    next[index] = next[index].copyWith(
      name: name.trim(),
      role: role?.trim().isEmpty ?? true ? null : role!.trim(),
      clearRole: role == null || role.trim().isEmpty,
      note: note?.trim().isEmpty ?? true ? null : note!.trim(),
      clearNote: note == null || note.trim().isEmpty,
    );
    await _saveBook(bookId, next);
  }

  Future<void> remove({required String bookId, required String id}) async {
    final next = forBook(bookId).where((entry) => entry.id != id).toList();
    await _saveBook(bookId, next);
  }

  Future<void> clearBook(String bookId) async {
    await _saveBook(bookId, const []);
  }

  Future<void> _saveBook(String bookId, List<BookCharacter> characters) async {
    final nextState = {...state};
    if (characters.isEmpty) {
      nextState.remove(bookId);
    } else {
      nextState[bookId] = characters;
    }

    state = nextState;

    final prefs = ref.read(sharedPreferencesProvider);
    final encoded = jsonEncode(
      nextState.map(
        (key, value) => MapEntry(
          key,
          value.map((entry) => entry.toJson()).toList(),
        ),
      ),
    );
    await prefs.setString(_storageKey, encoded);
    await ref
        .read(startupStorageServiceProvider)
        .saveCharacterNotesSnapshot(encoded);
  }
}

final characterNotesProvider =
    NotifierProvider<CharacterNotesNotifier, Map<String, List<BookCharacter>>>(
  CharacterNotesNotifier.new,
);

final bookCharactersProvider = Provider.family<List<BookCharacter>, String>(
  (ref, bookId) => ref.watch(characterNotesProvider)[bookId] ?? const [],
);
