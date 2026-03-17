import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'package:avdibook/app/theme/app_spacing.dart';
import 'package:avdibook/core/constants/app_constants.dart';
import 'package:avdibook/features/audiobooks/domain/models/audiobook.dart';
import 'package:avdibook/shared/providers/library_provider.dart';
import 'package:avdibook/shared/providers/listening_analytics_provider.dart';

enum SearchSortMode { relevance, recentPlayed, recentAdded, title, author }

class SearchScreen extends ConsumerStatefulWidget {
  const SearchScreen({super.key});

  @override
  ConsumerState<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends ConsumerState<SearchScreen>
  with SingleTickerProviderStateMixin {
  final _queryController = TextEditingController();
  SearchSortMode _sortMode = SearchSortMode.relevance;
  String? _selectedBookId;
  late final AnimationController _entranceController;

  @override
  void initState() {
    super.initState();
    _entranceController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 650),
    )..forward();
  }

  @override
  void dispose() {
    _entranceController.dispose();
    _queryController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final library = ref.watch(libraryProvider);
    final analytics = ref.watch(listeningAnalyticsProvider).byBook;
    final query = _queryController.text.trim().toLowerCase();
    final tokenized = _tokenizeQuery(query);
    final textQuery = tokenized.textQuery;
    final hasFavoriteFilter = tokenized.tokens.contains('is:favorite');
    final hasRecentFilter = tokenized.tokens.contains('is:recent');
    final useSplitLayout =
        MediaQuery.sizeOf(context).width >= AppSpacing.mediumMaxWidth;
    final reduceMotion = MediaQuery.maybeOf(context)?.disableAnimations ?? false;

    final results = [...library]
      ..sort((a, b) {
        if (_sortMode == SearchSortMode.recentAdded) {
          return b.importedAt.compareTo(a.importedAt);
        }
        if (_sortMode == SearchSortMode.recentPlayed) {
          final aPlayed = analytics[a.id]?.lastPlayedAt ?? a.lastPlayedAt;
          final bPlayed = analytics[b.id]?.lastPlayedAt ?? b.lastPlayedAt;
          if (aPlayed == null && bPlayed == null) return 0;
          if (aPlayed == null) return 1;
          if (bPlayed == null) return -1;
          return bPlayed.compareTo(aPlayed);
        }
        if (_sortMode == SearchSortMode.title) {
          return a.title.toLowerCase().compareTo(b.title.toLowerCase());
        }
        if (_sortMode == SearchSortMode.author) {
          final aAuthor = (a.author?.name ?? '').toLowerCase();
          final bAuthor = (b.author?.name ?? '').toLowerCase();
          return aAuthor.compareTo(bAuthor);
        }

        final aScore = _score(a, textQuery);
        final bScore = _score(b, textQuery);
        if (aScore != bScore) return bScore.compareTo(aScore);
        return b.importedAt.compareTo(a.importedAt);
      });

    final filtered = results.where((book) {
      if (hasFavoriteFilter && !book.isFavorite) return false;
      if (hasRecentFilter) {
        final wasPlayedRecently =
            analytics[book.id]?.lastPlayedAt != null || book.lastPlayedAt != null;
        if (!wasPlayedRecently) return false;
      }
      if (textQuery.isEmpty) return true;
      return _score(book, textQuery) > 0;
    }).toList();

    final selectedBook = filtered.firstWhere(
      (book) => book.id == _selectedBookId,
      orElse: () => filtered.isNotEmpty ? filtered.first : results.first,
    );

    final contentBlock = filtered.isEmpty
        ? Padding(
            padding: const EdgeInsets.only(top: 24),
            child: Text(
              query.isEmpty
                  ? 'Start typing to search your library.'
                  : 'No books match your search yet.',
              style: tt.bodyMedium?.copyWith(
                color: cs.onSurfaceVariant,
              ),
            ),
          )
        : useSplitLayout
        ? SizedBox(
            height: MediaQuery.sizeOf(context).height * 0.62,
            child: Row(
              children: [
                Expanded(
                  flex: 6,
                  child: ListView.separated(
                    itemCount: filtered.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 10),
                    itemBuilder: (context, index) {
                      final book = filtered[index];
                      final isSelected = book.id == selectedBook.id;
                      return _SearchResultTile(
                        book: book,
                        selected: isSelected,
                        onTap: () => setState(() => _selectedBookId = book.id),
                        onOpen: () => context.push(AppRoutes.playerPath(book.id)),
                      );
                    },
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  flex: 4,
                  child: _SearchDetailPane(
                    book: selectedBook,
                    onOpen: () => context.push(
                      AppRoutes.playerPath(selectedBook.id),
                    ),
                  ),
                ),
              ],
            ),
          )
        : Column(
            children: [
              for (final book in filtered)
                Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: _SearchResultTile(
                    book: book,
                    selected: false,
                    onTap: () => context.push(AppRoutes.playerPath(book.id)),
                    onOpen: () => context.push(AppRoutes.playerPath(book.id)),
                  ),
                ),
            ],
          );

    return Scaffold(
      appBar: AppBar(
        title: const Text('Search'),
        elevation: 0,
      ),
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      body: CustomScrollView(
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                _reveal(
                  index: 0,
                  reduceMotion: reduceMotion,
                  child: _SearchHero(
                    query: query,
                    onClear: () {
                      _queryController.clear();
                      setState(() {});
                    },
                    hasFavoriteFilter: hasFavoriteFilter,
                    hasRecentFilter: hasRecentFilter,
                    onFavoritesTap: () => _toggleQueryToken('is:favorite'),
                    onRecentTap: () => _toggleQueryToken('is:recent'),
                    onClearFilters: _removeQueryTokens,
                    child: SearchBar(
                      controller: _queryController,
                      onChanged: (_) => setState(() {}),
                      hintText: 'Search books, authors, narrators...',
                      leading: Icon(
                        Icons.search_rounded,
                        color: cs.onSurface.withValues(alpha: 0.5),
                      ),
                      trailing: query.isEmpty
                          ? null
                          : [
                              IconButton(
                                onPressed: () {
                                  _queryController.clear();
                                  setState(() {});
                                },
                                icon: const Icon(Icons.close_rounded),
                              ),
                            ],
                      elevation: const WidgetStatePropertyAll(0),
                      backgroundColor: WidgetStatePropertyAll(
                        cs.surface,
                      ),
                      shape: WidgetStatePropertyAll(
                        RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(16),
                        ),
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 10),
                _reveal(
                  index: 1,
                  reduceMotion: reduceMotion,
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 6,
                        ),
                        decoration: BoxDecoration(
                          color: cs.secondaryContainer,
                          borderRadius: BorderRadius.circular(999),
                        ),
                        child: Text(
                          '${filtered.length} result${filtered.length == 1 ? '' : 's'}',
                          style: tt.labelMedium?.copyWith(
                            color: cs.onSecondaryContainer,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                      const Spacer(),
                      PopupMenuButton<SearchSortMode>(
                        initialValue: _sortMode,
                        onSelected: (value) => setState(() => _sortMode = value),
                        itemBuilder: (context) => const [
                          PopupMenuItem(
                            value: SearchSortMode.relevance,
                            child: Text('Relevance'),
                          ),
                          PopupMenuItem(
                            value: SearchSortMode.recentPlayed,
                            child: Text('Recently played'),
                          ),
                          PopupMenuItem(
                            value: SearchSortMode.recentAdded,
                            child: Text('Recently added'),
                          ),
                          PopupMenuItem(
                            value: SearchSortMode.title,
                            child: Text('Title'),
                          ),
                          PopupMenuItem(
                            value: SearchSortMode.author,
                            child: Text('Author'),
                          ),
                        ],
                        child: Chip(
                          label: Text(switch (_sortMode) {
                            SearchSortMode.relevance => 'Sort: Relevance',
                            SearchSortMode.recentPlayed =>
                              'Sort: Recently played',
                            SearchSortMode.recentAdded => 'Sort: Recently added',
                            SearchSortMode.title => 'Sort: Title',
                            SearchSortMode.author => 'Sort: Author',
                          }),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 10),
                _reveal(
                  index: 2,
                  reduceMotion: reduceMotion,
                  child: AnimatedSwitcher(
                    duration: const Duration(milliseconds: 260),
                    switchInCurve: Curves.easeOutCubic,
                    switchOutCurve: Curves.easeInCubic,
                    child: KeyedSubtree(
                      key: ValueKey<String>(
                        '$query-${_sortMode.name}-${filtered.length}-$useSplitLayout',
                      ),
                      child: contentBlock,
                    ),
                  ),
                ),
              ]),
            ),
          ),
        ],
      ),
    );
  }

  int _score(Audiobook book, String query) {
    if (query.isEmpty) return 1;

    var score = 0;
    final title = book.title.toLowerCase();
    final author = (book.author?.name ?? '').toLowerCase();
    final narrator = (book.narrator ?? '').toLowerCase();
    final series = (book.series ?? '').toLowerCase();
    final genre = (book.genre ?? '').toLowerCase();

    if (title == query) score += 100;
    if (title.startsWith(query)) score += 70;
    if (title.contains(query)) score += 40;
    if (author.startsWith(query)) score += 30;
    if (author.contains(query)) score += 20;
    if (narrator.contains(query)) score += 12;
    if (series.contains(query)) score += 10;
    if (genre.contains(query)) score += 8;

    return score;
  }

  _TokenizedQuery _tokenizeQuery(String rawQuery) {
    final parts = rawQuery
        .split(RegExp(r'\s+'))
        .map((part) => part.trim())
        .where((part) => part.isNotEmpty)
        .toList();

    final tokens = <String>{};
    final terms = <String>[];
    for (final part in parts) {
      if (part.startsWith('is:')) {
        tokens.add(part);
      } else {
        terms.add(part);
      }
    }
    return _TokenizedQuery(
      textQuery: terms.join(' ').trim(),
      tokens: tokens,
    );
  }

  void _toggleQueryToken(String token) {
    final current = _tokenizeQuery(_queryController.text.trim().toLowerCase());
    final tokens = <String>{...current.tokens};
    if (tokens.contains(token)) {
      tokens.remove(token);
    } else {
      tokens.add(token);
    }
    _updateQueryFromTokens(tokens, current.textQuery);
  }

  void _removeQueryTokens() {
    final current = _tokenizeQuery(_queryController.text.trim().toLowerCase());
    _updateQueryFromTokens(const <String>{}, current.textQuery);
  }

  void _updateQueryFromTokens(Set<String> tokens, String textQuery) {
    const tokenOrder = ['is:favorite', 'is:recent'];
    final orderedTokens = [
      for (final token in tokenOrder)
        if (tokens.contains(token)) token,
    ];
    final joined = [
      ...orderedTokens,
      if (textQuery.isNotEmpty) textQuery,
    ].join(' ');
    _queryController
      ..text = joined
      ..selection = TextSelection.collapsed(offset: joined.length);
    setState(() {});
  }

  Animation<double> _staggered(int index) {
    final start = math.min(index * 0.12, 0.75);
    final end = math.min(start + 0.32, 1.0);
    return CurvedAnimation(
      parent: _entranceController,
      curve: Interval(start, end, curve: Curves.easeOutCubic),
    );
  }

  Widget _reveal({
    required int index,
    required bool reduceMotion,
    required Widget child,
  }) {
    if (reduceMotion) return child;
    final animation = _staggered(index);
    return FadeTransition(
      opacity: animation,
      child: SlideTransition(
        position: Tween<Offset>(
          begin: const Offset(0, 0.04),
          end: Offset.zero,
        ).animate(animation),
        child: child,
      ),
    );
  }
}

class _SearchResultTile extends StatelessWidget {
  const _SearchResultTile({
    required this.book,
    required this.selected,
    required this.onTap,
    required this.onOpen,
  });

  final Audiobook book;
  final bool selected;
  final VoidCallback onTap;
  final VoidCallback onOpen;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    return AnimatedContainer(
      duration: const Duration(milliseconds: 220),
      curve: Curves.easeOutCubic,
      decoration: BoxDecoration(
        color: selected ? cs.secondaryContainer : cs.surfaceContainerLow,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: selected
              ? cs.primary.withValues(alpha: 0.3)
              : cs.outlineVariant.withValues(alpha: 0.4),
        ),
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(16),
        child: ListTile(
          onTap: onTap,
          leading: AnimatedSwitcher(
            duration: const Duration(milliseconds: 180),
            child: Icon(
              book.isFavorite ? Icons.star_rounded : Icons.menu_book_rounded,
              key: ValueKey<bool>(book.isFavorite),
            ),
          ),
          title: Text(book.title),
          subtitle: Text(
            [
              book.author?.name ?? 'Unknown author',
              '${book.chapterCount} chapters',
            ].join(' • '),
          ),
          trailing: FilledButton.tonalIcon(
            onPressed: onOpen,
            icon: const Icon(Icons.play_arrow_rounded, size: 18),
            label: Text(
              'Play',
              style: tt.labelMedium,
            ),
          ),
        ),
      ),
    );
  }
}

class _SearchDetailPane extends StatelessWidget {
  const _SearchDetailPane({required this.book, required this.onOpen});

  final Audiobook book;
  final VoidCallback onOpen;

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;

    return Card(
      color: cs.surfaceContainer,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
              decoration: BoxDecoration(
                color: cs.tertiaryContainer,
                borderRadius: BorderRadius.circular(999),
              ),
              child: Text(
                'Selected Result',
                style: tt.labelMedium?.copyWith(
                  color: cs.onTertiaryContainer,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
            const SizedBox(height: 12),
            Text(
              book.title,
              maxLines: 3,
              overflow: TextOverflow.ellipsis,
              style: tt.headlineSmall?.copyWith(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 6),
            Text(
              book.author?.name ?? 'Unknown author',
              style: tt.bodyMedium?.copyWith(color: cs.onSurfaceVariant),
            ),
            const SizedBox(height: 10),
            Text(
              '${book.chapterCount} chapters',
              style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
            ),
            const Spacer(),
            FilledButton.tonalIcon(
              onPressed: onOpen,
              icon: const Icon(Icons.play_arrow_rounded),
              label: const Text('Open Player'),
            ),
          ],
        ),
      ),
    );
  }
}

class _SearchHero extends StatelessWidget {
  const _SearchHero({
    required this.child,
    required this.query,
    required this.onClear,
    required this.hasFavoriteFilter,
    required this.hasRecentFilter,
    required this.onFavoritesTap,
    required this.onRecentTap,
    required this.onClearFilters,
  });

  final Widget child;
  final String query;
  final VoidCallback onClear;
  final bool hasFavoriteFilter;
  final bool hasRecentFilter;
  final VoidCallback onFavoritesTap;
  final VoidCallback onRecentTap;
  final VoidCallback onClearFilters;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Card(
      margin: EdgeInsets.zero,
      color: cs.surfaceContainerLow,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            child,
            const SizedBox(height: 10),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _InfoAssistChip(
                  label: 'Favorites',
                  selected: hasFavoriteFilter,
                  onTap: onFavoritesTap,
                ),
                _InfoAssistChip(
                  label: 'Recent',
                  selected: hasRecentFilter,
                  onTap: onRecentTap,
                ),
                if (hasFavoriteFilter || hasRecentFilter)
                  ActionChip(
                    avatar: const Icon(Icons.filter_alt_off_rounded, size: 16),
                    label: const Text('Clear filters'),
                    onPressed: onClearFilters,
                  ),
                if (query.isNotEmpty)
                  ActionChip(
                    avatar: const Icon(Icons.close_rounded, size: 16),
                    label: const Text('Clear'),
                    onPressed: onClear,
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _InfoAssistChip extends StatelessWidget {
  const _InfoAssistChip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return FilterChip(
      label: Text(label),
      avatar: const Icon(Icons.auto_awesome_rounded, size: 15),
      selected: selected,
      onSelected: (_) => onTap(),
    );
  }
}

class _TokenizedQuery {
  const _TokenizedQuery({required this.textQuery, required this.tokens});

  final String textQuery;
  final Set<String> tokens;
}
