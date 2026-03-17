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

class _SearchScreenState extends ConsumerState<SearchScreen> {
  final _queryController = TextEditingController();
  SearchSortMode _sortMode = SearchSortMode.relevance;
  String? _selectedBookId;

  @override
  void dispose() {
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
    final useSplitLayout =
        MediaQuery.sizeOf(context).width >= AppSpacing.mediumMaxWidth;

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

        final aScore = _score(a, query);
        final bScore = _score(b, query);
        if (aScore != bScore) return bScore.compareTo(aScore);
        return b.importedAt.compareTo(a.importedAt);
      });

    final filtered = query.isEmpty
        ? results
        : results.where((book) => _score(book, query) > 0).toList();

    final selectedBook = filtered.firstWhere(
      (book) => book.id == _selectedBookId,
      orElse: () => filtered.isNotEmpty ? filtered.first : results.first,
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
                SearchBar(
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
                    cs.surfaceContainerHighest,
                  ),
                  shape: WidgetStatePropertyAll(
                    RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Text(
                      '${filtered.length} result${filtered.length == 1 ? '' : 's'}',
                      style: tt.labelMedium?.copyWith(
                        color: cs.onSurfaceVariant,
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
                          SearchSortMode.relevance => 'Relevance',
                          SearchSortMode.recentPlayed => 'Recently played',
                          SearchSortMode.recentAdded => 'Recently added',
                          SearchSortMode.title => 'Title',
                          SearchSortMode.author => 'Author',
                        }),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                if (filtered.isEmpty)
                  Padding(
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
                else if (useSplitLayout)
                  SizedBox(
                    height: MediaQuery.sizeOf(context).height * 0.62,
                    child: Row(
                      children: [
                        Expanded(
                          flex: 6,
                          child: ListView.separated(
                            itemCount: filtered.length,
                            separatorBuilder: (_, _) =>
                                const SizedBox(height: 10),
                            itemBuilder: (context, index) {
                              final book = filtered[index];
                              final isSelected = book.id == selectedBook.id;
                              return _SearchResultTile(
                                book: book,
                                selected: isSelected,
                                onTap: () =>
                                    setState(() => _selectedBookId = book.id),
                                onOpen: () =>
                                    context.push(AppRoutes.playerPath(book.id)),
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
                else
                  ...filtered.map(
                    (book) => Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: _SearchResultTile(
                        book: book,
                        selected: false,
                        onTap: () =>
                            context.push(AppRoutes.playerPath(book.id)),
                        onOpen: () =>
                            context.push(AppRoutes.playerPath(book.id)),
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

    return Material(
      color: selected ? cs.secondaryContainer : cs.surfaceContainerLow,
      borderRadius: BorderRadius.circular(16),
      child: ListTile(
        onTap: onTap,
        leading: Icon(
          book.isFavorite ? Icons.star_rounded : Icons.menu_book_rounded,
        ),
        title: Text(book.title),
        subtitle: Text(
          [
            book.author?.name ?? 'Unknown author',
            '${book.chapterCount} chapters',
          ].join(' • '),
        ),
        trailing: IconButton(
          onPressed: onOpen,
          icon: const Icon(Icons.play_arrow_rounded),
          tooltip: 'Open player',
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
      color: cs.surfaceContainerLow,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Selected Result', style: tt.labelMedium),
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
            FilledButton.icon(
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
