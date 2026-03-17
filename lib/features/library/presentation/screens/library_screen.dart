import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'package:avdibook/app/theme/app_spacing.dart';
import 'package:avdibook/core/constants/app_constants.dart';
import 'package:avdibook/core/utils/duration_formatter.dart';
import 'package:avdibook/core/widgets/expressive_bounce.dart';
import 'package:avdibook/features/audiobooks/domain/models/audiobook.dart';
import 'package:avdibook/shared/providers/character_notes_provider.dart';
import 'package:avdibook/shared/providers/library_provider.dart';
import 'package:avdibook/shared/providers/listening_analytics_provider.dart';
import 'package:avdibook/shared/providers/storage_providers.dart';

enum LibraryViewFilter {
  all,
  continueListening,
  recentPlayed,
  favorites,
  downloaded,
}

enum LibrarySortMode { recentAdded, recentPlayed, title, author, progress }

class LibraryScreen extends ConsumerStatefulWidget {
  const LibraryScreen({super.key});

  @override
  ConsumerState<LibraryScreen> createState() => _LibraryScreenState();
}

class _LibraryScreenState extends ConsumerState<LibraryScreen> {
  BookStatus? _statusFilter;
  LibraryViewFilter _viewFilter = LibraryViewFilter.all;
  LibrarySortMode _sortMode = LibrarySortMode.recentAdded;
  String? _selectedBookId;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final library = ref.watch(libraryProvider);
    final analytics = ref.watch(listeningAnalyticsProvider);
    final charactersByBook = ref.watch(characterNotesProvider);
    final analyticsByBook = analytics.byBook;
    final useSplitLayout =
        MediaQuery.sizeOf(context).width >= AppSpacing.mediumMaxWidth;

    final sorted = [...library];

    final statusCounts = {
      BookStatus.newBook: 0,
      BookStatus.started: 0,
      BookStatus.finished: 0,
    };
    for (final book in sorted) {
      final listened = analyticsByBook[book.id]?.totalDuration ?? Duration.zero;
      final resolved = _resolveStatus(book, listened);
      statusCounts[resolved] = (statusCounts[resolved] ?? 0) + 1;
    }

    final continueCount = sorted
      .where((book) => book.progress > 0 && book.progress < 0.98)
      .length;
    final recentCount = sorted
      .where(
        (book) =>
          analyticsByBook[book.id]?.lastPlayedAt != null ||
          book.lastPlayedAt != null,
      )
      .length;
    final favoritesCount = sorted.where((book) => book.isFavorite).length;
    final downloadedCount =
      sorted.where((book) => book.sourcePaths.isNotEmpty).length;

    final filtered =
        sorted
            .where((book) {
              if (_statusFilter == null) return true;
              final listened =
                  analyticsByBook[book.id]?.totalDuration ?? Duration.zero;
              return _resolveStatus(book, listened) == _statusFilter;
            })
            .where((book) {
              final listened =
                  analyticsByBook[book.id]?.totalDuration ?? Duration.zero;
              switch (_viewFilter) {
                case LibraryViewFilter.all:
                  return true;
                case LibraryViewFilter.continueListening:
                  return (book.progress > 0 && book.progress < 0.98) ||
                      (listened > Duration.zero && book.progress < 0.98);
                case LibraryViewFilter.recentPlayed:
                  return analyticsByBook[book.id]?.lastPlayedAt != null ||
                      book.lastPlayedAt != null;
                case LibraryViewFilter.favorites:
                  return book.isFavorite;
                case LibraryViewFilter.downloaded:
                  return book.sourcePaths.isNotEmpty;
              }
            })
            .toList()
          ..sort((a, b) {
            switch (_sortMode) {
              case LibrarySortMode.recentAdded:
                return b.importedAt.compareTo(a.importedAt);
              case LibrarySortMode.recentPlayed:
                final aPlayed =
                    analyticsByBook[a.id]?.lastPlayedAt ?? a.lastPlayedAt;
                final bPlayed =
                    analyticsByBook[b.id]?.lastPlayedAt ?? b.lastPlayedAt;
                if (aPlayed == null && bPlayed == null) return 0;
                if (aPlayed == null) return 1;
                if (bPlayed == null) return -1;
                return bPlayed.compareTo(aPlayed);
              case LibrarySortMode.title:
                return a.title.toLowerCase().compareTo(b.title.toLowerCase());
              case LibrarySortMode.author:
                final aAuthor = (a.author?.name ?? '').toLowerCase();
                final bAuthor = (b.author?.name ?? '').toLowerCase();
                return aAuthor.compareTo(bAuthor);
              case LibrarySortMode.progress:
                return b.progress.compareTo(a.progress);
            }
          });

    final selectedBook = filtered.firstWhere(
      (book) => book.id == _selectedBookId,
      orElse: () => filtered.isNotEmpty ? filtered.first : sorted.first,
    );
    final selectedStats = analyticsByBook[selectedBook.id];
    final selectedListened = selectedStats?.totalDuration ?? Duration.zero;
    final selectedStatus = _resolveStatus(selectedBook, selectedListened);

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            pinned: true,
            title: const Text('Library'),
            backgroundColor: Theme.of(context).scaffoldBackgroundColor,
            elevation: 0,
          ),
          if (sorted.isEmpty)
            SliverFillRemaining(
              child: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      Icons.library_music_rounded,
                      size: 56,
                      color: cs.onSurface.withValues(alpha: 0.25),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      'No books yet',
                      style: tt.titleLarge?.copyWith(
                        color: cs.onSurface.withValues(alpha: 0.75),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Import from Home to build your audiobook library.',
                      style: tt.bodyMedium?.copyWith(
                        color: cs.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            )
          else
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
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
                        if (_viewFilter != LibraryViewFilter.all || _statusFilter != null)
                          TextButton.icon(
                            onPressed: () => setState(() {
                              _viewFilter = LibraryViewFilter.all;
                              _statusFilter = null;
                            }),
                            icon: const Icon(Icons.filter_alt_off_rounded),
                            label: const Text('Clear'),
                          ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        PopupMenuButton<LibraryViewFilter>(
                          initialValue: _viewFilter,
                          onSelected: (value) => setState(() => _viewFilter = value),
                          itemBuilder: (context) => [
                            PopupMenuItem(
                              value: LibraryViewFilter.all,
                              child: Text('All (${
                                sorted.length
                              })'),
                            ),
                            PopupMenuItem(
                              value: LibraryViewFilter.continueListening,
                              child: Text('Continue ($continueCount)'),
                            ),
                            PopupMenuItem(
                              value: LibraryViewFilter.recentPlayed,
                              child: Text('Recent ($recentCount)'),
                            ),
                            PopupMenuItem(
                              value: LibraryViewFilter.favorites,
                              child: Text('Favorites ($favoritesCount)'),
                            ),
                            PopupMenuItem(
                              value: LibraryViewFilter.downloaded,
                              child: Text('Downloaded ($downloadedCount)'),
                            ),
                          ],
                          child: Chip(
                            avatar: const Icon(Icons.filter_list_rounded, size: 18),
                            label: Text(switch (_viewFilter) {
                              LibraryViewFilter.all => 'View: All',
                              LibraryViewFilter.continueListening =>
                                'View: Continue',
                              LibraryViewFilter.recentPlayed => 'View: Recent',
                              LibraryViewFilter.favorites => 'View: Favorites',
                              LibraryViewFilter.downloaded => 'View: Downloaded',
                            }),
                          ),
                        ),
                        PopupMenuButton<BookStatus?>(
                          initialValue: _statusFilter,
                          onSelected: (value) => setState(() => _statusFilter = value),
                          itemBuilder: (context) => [
                            PopupMenuItem<BookStatus?>(
                              value: null,
                              child: Text('All statuses (${sorted.length})'),
                            ),
                            PopupMenuItem<BookStatus?>(
                              value: BookStatus.newBook,
                              child: Text(
                                'New (${statusCounts[BookStatus.newBook] ?? 0})',
                              ),
                            ),
                            PopupMenuItem<BookStatus?>(
                              value: BookStatus.started,
                              child: Text(
                                'Started (${statusCounts[BookStatus.started] ?? 0})',
                              ),
                            ),
                            PopupMenuItem<BookStatus?>(
                              value: BookStatus.finished,
                              child: Text(
                                'Finished (${statusCounts[BookStatus.finished] ?? 0})',
                              ),
                            ),
                          ],
                          child: Chip(
                            avatar: const Icon(Icons.tune_rounded, size: 18),
                            label: Text(switch (_statusFilter) {
                              null => 'Status: All',
                              BookStatus.newBook => 'Status: New',
                              BookStatus.started => 'Status: Started',
                              BookStatus.finished => 'Status: Finished',
                            }),
                          ),
                        ),
                        PopupMenuButton<LibrarySortMode>(
                          initialValue: _sortMode,
                          onSelected: (value) => setState(() => _sortMode = value),
                          itemBuilder: (context) => const [
                            PopupMenuItem(
                              value: LibrarySortMode.recentAdded,
                              child: Text('Recently added'),
                            ),
                            PopupMenuItem(
                              value: LibrarySortMode.recentPlayed,
                              child: Text('Recently played'),
                            ),
                            PopupMenuItem(
                              value: LibrarySortMode.title,
                              child: Text('Title'),
                            ),
                            PopupMenuItem(
                              value: LibrarySortMode.author,
                              child: Text('Author'),
                            ),
                            PopupMenuItem(
                              value: LibrarySortMode.progress,
                              child: Text('Progress'),
                            ),
                          ],
                          child: Chip(
                            avatar: const Icon(Icons.swap_vert_rounded, size: 18),
                            label: Text(switch (_sortMode) {
                              LibrarySortMode.recentAdded => 'Sort: Added',
                              LibrarySortMode.recentPlayed => 'Sort: Played',
                              LibrarySortMode.title => 'Sort: Title',
                              LibrarySortMode.author => 'Sort: Author',
                              LibrarySortMode.progress => 'Sort: Progress',
                            }),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          if (sorted.isNotEmpty)
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
              sliver: filtered.isEmpty
                  ? SliverToBoxAdapter(
                      child: Padding(
                        padding: const EdgeInsets.only(top: 16),
                        child: Text(
                          'No books in this category yet.',
                          style: tt.bodyMedium?.copyWith(
                            color: cs.onSurfaceVariant,
                          ),
                        ),
                      ),
                    )
                  : useSplitLayout
                  ? SliverFillRemaining(
                      child: Row(
                        children: [
                          Expanded(
                            flex: 6,
                            child: ListView.separated(
                              itemCount: filtered.length,
                              separatorBuilder: (_, _) =>
                                  const SizedBox(height: 12),
                              itemBuilder: (context, index) {
                                final book = filtered[index];
                                final stats = analytics.byBook[book.id];
                                final listened =
                                    stats?.totalDuration ?? Duration.zero;
                                final isSelected = book.id == selectedBook.id;
                                return _LibraryBookTile(
                                  book: book,
                                  listened: listened,
                                  status: _resolveStatus(book, listened),
                                  characterCount:
                                      charactersByBook[book.id]?.length ?? 0,
                                  selected: isSelected,
                                  onSelect: () =>
                                      setState(() => _selectedBookId = book.id),
                                  onOpen: () => context.push(
                                    AppRoutes.playerPath(book.id),
                                  ),
                                  onManageCharacters: () =>
                                      _showCharactersSheet(context, ref, book),
                                  onToggleFavorite: () =>
                                      _toggleFavorite(context, ref, book),
                                  onRemove: () =>
                                      _removeBook(context, ref, book),
                                );
                              },
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            flex: 4,
                            child: _LibraryDetailPane(
                              book: selectedBook,
                              listened: selectedListened,
                              status: selectedStatus,
                              characterCount:
                                  charactersByBook[selectedBook.id]?.length ??
                                  0,
                              onOpen: () => context.push(
                                AppRoutes.playerPath(selectedBook.id),
                              ),
                              onManageCharacters: () => _showCharactersSheet(
                                context,
                                ref,
                                selectedBook,
                              ),
                              onToggleFavorite: () =>
                                  _toggleFavorite(context, ref, selectedBook),
                              onRemove: () =>
                                  _removeBook(context, ref, selectedBook),
                            ),
                          ),
                        ],
                      ),
                    )
                  : SliverList.separated(
                      itemBuilder: (context, index) {
                        final book = filtered[index];
                        final stats = analytics.byBook[book.id];
                        final listened = stats?.totalDuration ?? Duration.zero;
                        return _LibraryBookTile(
                          book: book,
                          listened: listened,
                          status: _resolveStatus(book, listened),
                          characterCount:
                              charactersByBook[book.id]?.length ?? 0,
                          onOpen: () =>
                              context.push(AppRoutes.playerPath(book.id)),
                          onManageCharacters: () =>
                              _showCharactersSheet(context, ref, book),
                          onToggleFavorite: () =>
                              _toggleFavorite(context, ref, book),
                          onRemove: () => _removeBook(context, ref, book),
                        );
                      },
                      separatorBuilder: (_, _) => const SizedBox(height: 12),
                      itemCount: filtered.length,
                    ),
            ),
        ],
      ),
    );
  }

  Future<void> _showCharactersSheet(
    BuildContext context,
    WidgetRef ref,
    Audiobook book,
  ) async {
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (ctx) {
        return Consumer(
          builder: (context, modalRef, _) {
            final items = modalRef.watch(bookCharactersProvider(book.id));
            final tt = Theme.of(context).textTheme;
            final cs = Theme.of(context).colorScheme;

            return Padding(
              padding: EdgeInsets.fromLTRB(
                16,
                10,
                16,
                16 + MediaQuery.of(ctx).viewInsets.bottom,
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          'Characters',
                          style: tt.titleLarge?.copyWith(
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                      FilledButton.icon(
                        onPressed: () => _showCharacterEditor(
                          context: ctx,
                          ref: modalRef,
                          bookId: book.id,
                        ),
                        icon: const Icon(Icons.add_rounded),
                        label: const Text('Add'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Track who is who in "${book.title}".',
                    style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                  ),
                  const SizedBox(height: 12),
                  if (items.isEmpty)
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 22),
                      child: Text(
                        'No characters yet. Add the first one.',
                        style: tt.bodyMedium?.copyWith(
                          color: cs.onSurfaceVariant,
                        ),
                      ),
                    )
                  else
                    Flexible(
                      child: ListView.separated(
                        shrinkWrap: true,
                        itemCount: items.length,
                        separatorBuilder: (_, _) => const SizedBox(height: 8),
                        itemBuilder: (context, index) {
                          final item = items[index];
                          return Material(
                            color: cs.surfaceContainerLow,
                            borderRadius: BorderRadius.circular(16),
                            child: ListTile(
                              title: Text(item.name),
                              subtitle: Text(
                                [
                                  if (item.role != null) item.role!,
                                  if (item.note != null) item.note!,
                                ].join(' • '),
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                              ),
                              trailing: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  IconButton.filledTonal(
                                    tooltip: 'Edit character',
                                    onPressed: () => _showCharacterEditor(
                                      context: ctx,
                                      ref: modalRef,
                                      bookId: book.id,
                                      existing: item,
                                    ),
                                    icon: const Icon(Icons.edit_outlined),
                                  ),
                                  const SizedBox(width: 6),
                                  IconButton.filledTonal(
                                    tooltip: 'Delete character',
                                    onPressed: () {
                                      modalRef
                                          .read(characterNotesProvider.notifier)
                                          .remove(bookId: book.id, id: item.id);
                                    },
                                    icon: const Icon(Icons.delete_outline_rounded),
                                  ),
                                ],
                              ),
                            ),
                          );
                        },
                      ),
                    ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  Future<void> _showCharacterEditor({
    required BuildContext context,
    required WidgetRef ref,
    required String bookId,
    BookCharacter? existing,
  }) async {
    final nameCtl = TextEditingController(text: existing?.name ?? '');
    final roleCtl = TextEditingController(text: existing?.role ?? '');
    final noteCtl = TextEditingController(text: existing?.note ?? '');

    final saved = await showDialog<bool>(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          title: Text(existing == null ? 'Add character' : 'Edit character'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: nameCtl,
                  decoration: const InputDecoration(labelText: 'Name'),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: roleCtl,
                  decoration: const InputDecoration(
                    labelText: 'Role (optional)',
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: noteCtl,
                  decoration: const InputDecoration(
                    labelText: 'Note (optional)',
                  ),
                  minLines: 2,
                  maxLines: 4,
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(false),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: () => Navigator.of(ctx).pop(true),
              child: const Text('Save'),
            ),
          ],
        );
      },
    );

    if (saved == true) {
      final name = nameCtl.text.trim();
      if (name.isNotEmpty) {
        final notifier = ref.read(characterNotesProvider.notifier);
        if (existing == null) {
          await notifier.add(
            bookId: bookId,
            name: name,
            role: roleCtl.text,
            note: noteCtl.text,
          );
        } else {
          await notifier.update(
            bookId: bookId,
            id: existing.id,
            name: name,
            role: roleCtl.text,
            note: noteCtl.text,
          );
        }
      }
    }

    nameCtl.dispose();
    roleCtl.dispose();
    noteCtl.dispose();
  }

  BookStatus _resolveStatus(Audiobook book, Duration listened) {
    if (book.status != BookStatus.newBook) return book.status;
    if (book.progress >= 0.98) return BookStatus.finished;
    if (book.progress > 0.01 || listened > Duration.zero) {
      return BookStatus.started;
    }
    return BookStatus.newBook;
  }

  Future<void> _removeBook(
    BuildContext context,
    WidgetRef ref,
    Audiobook book,
  ) async {
    final existing = ref.read(libraryProvider);
    final next = existing.where((b) => b.id != book.id).toList();

    ref.read(libraryProvider.notifier).setLibrary(next);
    await ref.read(startupStorageServiceProvider).setLibraryItems(next);

    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Removed "${book.title}" from library.')),
      );
    }
  }

  Future<void> _toggleFavorite(
    BuildContext context,
    WidgetRef ref,
    Audiobook book,
  ) async {
    final existing = ref.read(libraryProvider);
    final index = existing.indexWhere((b) => b.id == book.id);
    if (index < 0) return;

    final next = [...existing];
    next[index] = next[index].copyWith(isFavorite: !next[index].isFavorite);

    ref.read(libraryProvider.notifier).setLibrary(next);
    await ref.read(startupStorageServiceProvider).setLibraryItems(next);
  }
}

class _LibraryBookTile extends StatelessWidget {
  const _LibraryBookTile({
    required this.book,
    required this.listened,
    required this.status,
    required this.characterCount,
    required this.onOpen,
    this.onSelect,
    this.selected = false,
    required this.onManageCharacters,
    required this.onToggleFavorite,
    required this.onRemove,
  });

  final Audiobook book;
  final Duration listened;
  final BookStatus status;
  final int characterCount;
  final VoidCallback onOpen;
  final VoidCallback? onSelect;
  final bool selected;
  final VoidCallback onManageCharacters;
  final VoidCallback onToggleFavorite;
  final VoidCallback onRemove;

  Future<void> _showActions(BuildContext context) {
    return showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (ctx) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(8, 0, 8, 12),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              ListTile(
                leading: const Icon(Icons.play_arrow_rounded),
                title: const Text('Open player'),
                onTap: () {
                  Navigator.of(ctx).pop();
                  onOpen();
                },
              ),
              ListTile(
                leading: const Icon(Icons.groups_rounded),
                title: const Text('Manage characters'),
                onTap: () {
                  Navigator.of(ctx).pop();
                  onManageCharacters();
                },
              ),
              ListTile(
                leading: Icon(
                  book.isFavorite
                      ? Icons.star_border_rounded
                      : Icons.star_rounded,
                ),
                title: Text(
                  book.isFavorite
                      ? 'Remove from favorites'
                      : 'Add to favorites',
                ),
                onTap: () {
                  Navigator.of(ctx).pop();
                  onToggleFavorite();
                },
              ),
              ListTile(
                leading: const Icon(Icons.delete_outline_rounded),
                title: const Text('Remove from library'),
                onTap: () {
                  Navigator.of(ctx).pop();
                  onRemove();
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final progressPercent = (book.progress.clamp(0.0, 1.0) * 100).round();

    return Material(
      color: selected ? cs.secondaryContainer : cs.surfaceContainerLow,
      borderRadius: BorderRadius.circular(20),
      child: ExpressiveBounce(
        child: InkWell(
          borderRadius: BorderRadius.circular(20),
          onTap: onSelect ?? onOpen,
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Row(
              children: [
                _BookCover(coverPath: book.coverPath),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        book.title,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: tt.titleMedium?.copyWith(
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        book.author?.name ?? 'Unknown author',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: tt.bodySmall?.copyWith(
                          color: cs.onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: [
                          _StatusBadge(status: status),
                          _MetaPill(
                            icon: Icons.menu_book_rounded,
                            label: '${book.chapterCount} chapters',
                          ),
                          if (progressPercent > 0)
                            _MetaPill(
                              icon: Icons.timelapse_rounded,
                              label: '$progressPercent% complete',
                            ),
                          _MetaPill(
                            icon: Icons.graphic_eq_rounded,
                            label:
                                'Listened ${DurationFormatter.formatHuman(listened)}',
                          ),
                          if (characterCount > 0)
                            _MetaPill(
                              icon: Icons.groups_rounded,
                              label: '$characterCount characters',
                            ),
                          if (book.isFavorite)
                            const _MetaPill(
                              icon: Icons.star_rounded,
                              label: 'Favorite',
                            ),
                        ],
                      ),
                    ],
                  ),
                ),
                IconButton.filledTonal(
                  onPressed: () => _showActions(context),
                  icon: const Icon(Icons.more_horiz_rounded),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _LibraryDetailPane extends StatelessWidget {
  const _LibraryDetailPane({
    required this.book,
    required this.listened,
    required this.status,
    required this.characterCount,
    required this.onOpen,
    required this.onManageCharacters,
    required this.onToggleFavorite,
    required this.onRemove,
  });

  final Audiobook book;
  final Duration listened;
  final BookStatus status;
  final int characterCount;
  final VoidCallback onOpen;
  final VoidCallback onManageCharacters;
  final VoidCallback onToggleFavorite;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final progressPercent = (book.progress.clamp(0.0, 1.0) * 100).round();

    return Card(
      color: cs.surfaceContainerLow,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Selected Book', style: tt.labelMedium),
            const SizedBox(height: 10),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _BookCover(coverPath: book.coverPath),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        book.title,
                        maxLines: 3,
                        overflow: TextOverflow.ellipsis,
                        style: tt.titleLarge?.copyWith(
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        book.author?.name ?? 'Unknown author',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: tt.bodyMedium?.copyWith(
                          color: cs.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _StatusBadge(status: status),
                _MetaPill(
                  icon: Icons.menu_book_rounded,
                  label: '${book.chapterCount} chapters',
                ),
                _MetaPill(
                  icon: Icons.timelapse_rounded,
                  label: '$progressPercent% complete',
                ),
                _MetaPill(
                  icon: Icons.graphic_eq_rounded,
                  label: 'Listened ${DurationFormatter.formatHuman(listened)}',
                ),
                if (characterCount > 0)
                  _MetaPill(
                    icon: Icons.groups_rounded,
                    label: '$characterCount characters',
                  ),
              ],
            ),
            const Spacer(),
            FilledButton.icon(
              onPressed: onOpen,
              icon: const Icon(Icons.play_arrow_rounded),
              label: const Text('Open Player'),
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: onManageCharacters,
                    icon: const Icon(Icons.groups_rounded),
                    label: const Text('Characters'),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton.filledTonal(
                  onPressed: onToggleFavorite,
                  icon: Icon(
                    book.isFavorite
                        ? Icons.star_rounded
                        : Icons.star_border_rounded,
                  ),
                ),
                const SizedBox(width: 8),
                IconButton.filledTonal(
                  onPressed: onRemove,
                  icon: const Icon(Icons.delete_outline_rounded),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.status});

  final BookStatus status;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    final (icon, bg, fg) = switch (status) {
      BookStatus.newBook => (
        Icons.fiber_new_rounded,
        cs.tertiaryContainer,
        cs.onTertiaryContainer,
      ),
      BookStatus.started => (
        Icons.auto_stories_rounded,
        cs.primaryContainer,
        cs.onPrimaryContainer,
      ),
      BookStatus.finished => (
        Icons.task_alt_rounded,
        cs.secondaryContainer,
        cs.onSecondaryContainer,
      ),
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 6),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: fg),
          const SizedBox(width: 5),
          Text(
            status.label,
            style: tt.labelSmall?.copyWith(
              color: fg,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}

class _BookCover extends StatelessWidget {
  const _BookCover({required this.coverPath});

  final String? coverPath;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final hasImage = coverPath != null && File(coverPath!).existsSync();

    return ClipRRect(
      borderRadius: BorderRadius.circular(14),
      child: SizedBox(
        width: 70,
        height: 96,
        child: hasImage
            ? Image.file(File(coverPath!), fit: BoxFit.cover)
            : Container(
                color: cs.primaryContainer,
                child: Icon(
                  Icons.auto_stories_rounded,
                  color: cs.onPrimaryContainer,
                  size: 26,
                ),
              ),
      ),
    );
  }
}

class _MetaPill extends StatelessWidget {
  const _MetaPill({required this.icon, required this.label});

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: cs.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: cs.primary),
          const SizedBox(width: 5),
          Text(
            label,
            style: tt.labelSmall?.copyWith(color: cs.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}
