import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'package:avdibook/core/constants/app_constants.dart';
import 'package:avdibook/core/utils/duration_formatter.dart';
import 'package:avdibook/core/widgets/expressive_bounce.dart';
import 'package:avdibook/features/audiobooks/domain/models/audiobook.dart';
import 'package:avdibook/shared/providers/app_bootstrap_provider.dart';
import 'package:avdibook/shared/providers/library_provider.dart';
import 'package:avdibook/shared/providers/listening_analytics_provider.dart';

class LibraryScreen extends ConsumerStatefulWidget {
  const LibraryScreen({super.key});

  @override
  ConsumerState<LibraryScreen> createState() => _LibraryScreenState();
}

class _LibraryScreenState extends ConsumerState<LibraryScreen> {
  BookStatus? _statusFilter;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final library = ref.watch(libraryProvider);
    final analytics = ref.watch(listeningAnalyticsProvider);
    final analyticsByBook = analytics.byBook;

    final sorted = [...library]
      ..sort((a, b) => b.importedAt.compareTo(a.importedAt));

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

    final filtered = sorted.where((book) {
      if (_statusFilter == null) return true;
      final listened = analyticsByBook[book.id]?.totalDuration ?? Duration.zero;
      return _resolveStatus(book, listened) == _statusFilter;
    }).toList();

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      body: CustomScrollView(
        slivers: [
          SliverAppBar.large(
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
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        _StatusCounterChip(
                          icon: Icons.fiber_new_rounded,
                          label: 'New',
                          count: statusCounts[BookStatus.newBook] ?? 0,
                          active: _statusFilter == BookStatus.newBook,
                          onTap: () => setState(() {
                            _statusFilter = _statusFilter == BookStatus.newBook
                                ? null
                                : BookStatus.newBook;
                          }),
                        ),
                        _StatusCounterChip(
                          icon: Icons.auto_stories_rounded,
                          label: 'Started',
                          count: statusCounts[BookStatus.started] ?? 0,
                          active: _statusFilter == BookStatus.started,
                          onTap: () => setState(() {
                            _statusFilter = _statusFilter == BookStatus.started
                                ? null
                                : BookStatus.started;
                          }),
                        ),
                        _StatusCounterChip(
                          icon: Icons.task_alt_rounded,
                          label: 'Finished',
                          count: statusCounts[BookStatus.finished] ?? 0,
                          active: _statusFilter == BookStatus.finished,
                          onTap: () => setState(() {
                            _statusFilter = _statusFilter == BookStatus.finished
                                ? null
                                : BookStatus.finished;
                          }),
                        ),
                      ],
                    ),
                    if (_statusFilter != null) ...[
                      const SizedBox(height: 10),
                      TextButton.icon(
                        onPressed: () => setState(() => _statusFilter = null),
                        icon: const Icon(Icons.filter_alt_off_rounded),
                        label: const Text('Clear filter'),
                      ),
                    ],
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
                  : SliverList.separated(
                      itemBuilder: (context, index) {
                        final book = filtered[index];
                        final stats = analytics.byBook[book.id];
                        final listened = stats?.totalDuration ?? Duration.zero;
                        return _LibraryBookTile(
                          book: book,
                          listened: listened,
                          status: _resolveStatus(book, listened),
                          onOpen: () =>
                              context.push(AppRoutes.playerPath(book.id)),
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
}

class _LibraryBookTile extends StatelessWidget {
  const _LibraryBookTile({
    required this.book,
    required this.listened,
    required this.status,
    required this.onOpen,
    required this.onRemove,
  });

  final Audiobook book;
  final Duration listened;
  final BookStatus status;
  final VoidCallback onOpen;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final progressPercent = (book.progress.clamp(0.0, 1.0) * 100).round();

    return Material(
      color: cs.surfaceContainerLow,
      borderRadius: BorderRadius.circular(20),
      child: ExpressiveBounce(
        child: InkWell(
          borderRadius: BorderRadius.circular(20),
          onTap: onOpen,
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
                        ],
                      ),
                    ],
                  ),
                ),
                PopupMenuButton<String>(
                  onSelected: (value) {
                    if (value == 'open') onOpen();
                    if (value == 'remove') onRemove();
                  },
                  itemBuilder: (context) => const [
                    PopupMenuItem(
                      value: 'open',
                      child: Text('Open player'),
                    ),
                    PopupMenuItem(
                      value: 'remove',
                      child: Text('Remove from library'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _StatusCounterChip extends StatelessWidget {
  const _StatusCounterChip({
    required this.icon,
    required this.label,
    required this.count,
    required this.active,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final int count;
  final bool active;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;

    return ExpressiveBounce(
      child: ChoiceChip(
        selected: active,
        onSelected: (_) => onTap(),
        avatar: Icon(icon, size: 16),
        label: Text('$label ($count)'),
        selectedColor: cs.secondaryContainer,
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
                color: cs.primaryContainer.withValues(alpha: 0.35),
                child: Icon(
                  Icons.auto_stories_rounded,
                  color: cs.primary,
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
            style: tt.labelSmall?.copyWith(
              color: cs.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}
