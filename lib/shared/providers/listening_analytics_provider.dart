import 'dart:convert';
import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:avdibook/core/constants/app_constants.dart';
import 'package:avdibook/shared/providers/preferences_provider.dart';
import 'package:avdibook/shared/providers/storage_providers.dart';


class BookListeningStats {
  const BookListeningStats({
    required this.bookId,
    required this.totalMs,
    required this.sessions,
    this.lastPlayedAt,
  });

  final String bookId;
  final int totalMs;
  final int sessions;
  final DateTime? lastPlayedAt;

  Duration get totalDuration => Duration(milliseconds: totalMs);

  BookListeningStats copyWith({
    int? totalMs,
    int? sessions,
    DateTime? lastPlayedAt,
  }) {
    return BookListeningStats(
      bookId: bookId,
      totalMs: totalMs ?? this.totalMs,
      sessions: sessions ?? this.sessions,
      lastPlayedAt: lastPlayedAt ?? this.lastPlayedAt,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'bookId': bookId,
      'totalMs': totalMs,
      'sessions': sessions,
      'lastPlayedAt': lastPlayedAt?.toIso8601String(),
    };
  }

  factory BookListeningStats.fromMap(Map<String, dynamic> map) {
    return BookListeningStats(
      bookId: map['bookId'] as String,
      totalMs: map['totalMs'] as int? ?? 0,
      sessions: map['sessions'] as int? ?? 0,
      lastPlayedAt: map['lastPlayedAt'] == null
          ? null
          : DateTime.tryParse(map['lastPlayedAt'] as String),
    );
  }
}

class ListeningAnalyticsState {
  const ListeningAnalyticsState({
    required this.byBook,
  });

  final Map<String, BookListeningStats> byBook;

  Duration get totalListeningDuration {
    final totalMs = byBook.values.fold<int>(
      0,
      (sum, item) => sum + item.totalMs,
    );
    return Duration(milliseconds: totalMs);
  }

  int get totalSessions {
    return byBook.values.fold<int>(
      0,
      (sum, item) => sum + item.sessions,
    );
  }

  Duration get averageSessionDuration {
    if (totalSessions == 0) return Duration.zero;
    return Duration(
      milliseconds: (totalListeningDuration.inMilliseconds / totalSessions)
          .round(),
    );
  }

  String? get topBookId {
    if (byBook.isEmpty) return null;
    final sorted = byBook.values.toList()
      ..sort((a, b) => b.totalMs.compareTo(a.totalMs));
    return sorted.first.bookId;
  }

  Map<String, dynamic> toMap() {
    return {
      'books': byBook.map(
        (key, value) => MapEntry(key, value.toMap()),
      ),
    };
  }

  factory ListeningAnalyticsState.fromMap(Map<String, dynamic> map) {
    final rawBooks = map['books'] as Map<String, dynamic>? ?? {};
    final parsed = <String, BookListeningStats>{};

    for (final entry in rawBooks.entries) {
      final valueMap = entry.value as Map<String, dynamic>;
      parsed[entry.key] = BookListeningStats.fromMap(valueMap);
    }

    return ListeningAnalyticsState(byBook: parsed);
  }

  static const empty = ListeningAnalyticsState(byBook: {});
}

class ListeningAnalyticsNotifier extends Notifier<ListeningAnalyticsState> {
  @override
  ListeningAnalyticsState build() {
    final prefs = ref.watch(sharedPreferencesProvider);
    final raw = prefs.getString(StorageKeys.listeningAnalytics);
    if (raw == null || raw.isEmpty) {
      unawaited(_hydrateFromDriftIfNeeded());
      return ListeningAnalyticsState.empty;
    }

    try {
      final map = jsonDecode(raw) as Map<String, dynamic>;
      return ListeningAnalyticsState.fromMap(map);
    } catch (_) {
      unawaited(_hydrateFromDriftIfNeeded());
      return ListeningAnalyticsState.empty;
    }
  }

  Future<void> _hydrateFromDriftIfNeeded() async {
    final storage = ref.read(startupStorageServiceProvider);
    final raw = await storage.loadListeningAnalyticsSnapshot();
    if (raw == null || raw.isEmpty || !ref.mounted) return;

    try {
      final map = jsonDecode(raw) as Map<String, dynamic>;
      state = ListeningAnalyticsState.fromMap(map);

      final prefs = ref.read(sharedPreferencesProvider);
      await prefs.setString(StorageKeys.listeningAnalytics, raw);
    } catch (_) {
      // Ignore malformed fallback payloads.
    }
  }

  Future<void> startSession(String bookId) async {
    final current = state.byBook[bookId] ??
        BookListeningStats(bookId: bookId, totalMs: 0, sessions: 0);

    final next = current.copyWith(
      sessions: current.sessions + 1,
      lastPlayedAt: DateTime.now(),
    );

    final updated = {...state.byBook, bookId: next};
    state = ListeningAnalyticsState(byBook: updated);
    await _persist();
  }

  Future<void> recordListening({
    required String bookId,
    required Duration delta,
  }) async {
    if (delta <= Duration.zero) return;

    final current = state.byBook[bookId] ??
        BookListeningStats(bookId: bookId, totalMs: 0, sessions: 0);

    final next = current.copyWith(
      totalMs: current.totalMs + delta.inMilliseconds,
      lastPlayedAt: DateTime.now(),
    );

    final updated = {...state.byBook, bookId: next};
    state = ListeningAnalyticsState(byBook: updated);
    await _persist();
  }

  Future<void> reset() async {
    state = ListeningAnalyticsState.empty;
    final prefs = ref.read(sharedPreferencesProvider);
    await prefs.remove(StorageKeys.listeningAnalytics);
  }

  Future<void> _persist() async {
    final prefs = ref.read(sharedPreferencesProvider);
    final encoded = jsonEncode(state.toMap());

    await prefs.setString(
      StorageKeys.listeningAnalytics,
      encoded,
    );

    await ref
        .read(startupStorageServiceProvider)
        .saveListeningAnalyticsSnapshot(encoded);
  }
}

final listeningAnalyticsProvider = NotifierProvider<
    ListeningAnalyticsNotifier,
    ListeningAnalyticsState>(ListeningAnalyticsNotifier.new);
