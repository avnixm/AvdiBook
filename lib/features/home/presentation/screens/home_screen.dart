import 'dart:io';
import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'package:avdibook/app/theme/app_spacing.dart';
import 'package:avdibook/core/constants/app_constants.dart';
import 'package:avdibook/core/utils/duration_formatter.dart';
import 'package:avdibook/features/audiobooks/domain/models/audiobook.dart';
import 'package:avdibook/features/player/presentation/providers/cover_palette_provider.dart';
import 'package:avdibook/features/setup/presentation/providers/setup_controller.dart';
import 'package:avdibook/shared/providers/library_provider.dart';
import 'package:avdibook/shared/providers/listening_analytics_provider.dart';
import 'package:avdibook/shared/providers/playback_history_provider.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final library = ref.watch(libraryProvider);
    final analytics = ref.watch(listeningAnalyticsProvider);
    final setupState = ref.watch(setupControllerProvider);
    final playbackHistory = ref.watch(playbackHistoryProvider);
    final isBusy = setupState.isBusy;
    final statusCounts = _buildStatusCounts(library, analytics.byBook);

    final continueListening = library
        .where((b) => b.progress > 0 && b.progress < 0.98)
        .toList()
      ..sort((a, b) {
        final aPlayed = analytics.byBook[a.id]?.lastPlayedAt ?? a.lastPlayedAt;
        final bPlayed = analytics.byBook[b.id]?.lastPlayedAt ?? b.lastPlayedAt;
        if (aPlayed == null && bPlayed == null) return 0;
        if (aPlayed == null) return 1;
        if (bPlayed == null) return -1;
        return bPlayed.compareTo(aPlayed);
      });

    final historyItems = playbackHistory
        .where((entry) => library.any((book) => book.id == entry.bookId))
        .take(12)
        .toList();

    void importFiles() =>
        ref.read(setupControllerProvider.notifier).importFiles();
    void importDirectory() =>
        ref.read(setupControllerProvider.notifier).importDirectory();

    return Scaffold(
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            pinned: true,
            title: Text(_greeting(library.isEmpty)),
            actions: [
              Padding(
                padding: const EdgeInsets.only(right: 8),
                child: IconButton.filledTonal(
                  onPressed: () => context.go(AppRoutes.settings),
                  icon: const Icon(Icons.settings_outlined),
                ),
              ),
            ],
          ),
          SliverPadding(
            padding:
                const EdgeInsets.symmetric(horizontal: AppSpacing.screenH),
            sliver: SliverToBoxAdapter(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (library.isEmpty)
                    _ExpressiveReveal(
                      index: 0,
                      child: _EmptyState(
                        isBusy: isBusy,
                        errorMessage: setupState.errorMessage,
                        onImportFiles: importFiles,
                        onImportDirectory: importDirectory,
                      ),
                    )
                  else ...[
                    const SizedBox(height: AppSpacing.md),

                    const _ExpressiveReveal(
                      index: 0,
                      child: _SectionTitle(title: 'Continue listening'),
                    ),
                    const SizedBox(height: AppSpacing.sm),
                    _ExpressiveReveal(
                      index: 1,
                      child: AnimatedSwitcher(
                        duration: const Duration(milliseconds: 280),
                        switchInCurve: Curves.easeOutCubic,
                        switchOutCurve: Curves.easeInCubic,
                        child: continueListening.isNotEmpty
                            ? _ContinueListeningCard(
                                key: ValueKey<String>(continueListening.first.id),
                                book: continueListening.first,
                                lastPlayedAt:
                                    analytics.byBook[continueListening.first.id]?.lastPlayedAt ??
                                    continueListening.first.lastPlayedAt,
                                onTap: (book) {
                                  final coverPath = book.coverPath;
                                  if (coverPath != null) {
                                    unawaited(ref.read(coverPaletteProvider(book.id).future));
                                    final imageFile = File(coverPath);
                                    if (imageFile.existsSync()) {
                                      unawaited(precacheImage(FileImage(imageFile), context));
                                    }
                                  }
                                  context.push(AppRoutes.playerPath(book.id));
                                },
                              )
                            : Card(
                                key: const ValueKey<String>('no-continue'),
                                margin: EdgeInsets.zero,
                                child: ListTile(
                                  leading: const Icon(Icons.play_circle_outline_rounded),
                                  title: const Text('No recent listening yet'),
                                  subtitle: const Text('Start a book and it will appear here.'),
                                  onTap: () => context.go(AppRoutes.library),
                                ),
                              ),
                      ),
                    ),

                    const SizedBox(height: AppSpacing.xl),
                    _ExpressiveReveal(
                      index: 2,
                      child: _ListeningAnalyticsCard(
                        totalListening: analytics.totalListeningDuration,
                        averageSession: analytics.averageSessionDuration,
                        sessions: analytics.totalSessions,
                      ),
                    ),

                    if (historyItems.isNotEmpty) ...[
                      const SizedBox(height: AppSpacing.xl),
                      const _ExpressiveReveal(
                        index: 3,
                        child: _SectionTitle(title: 'Playback history'),
                      ),
                      const SizedBox(height: AppSpacing.sm),
                      _ExpressiveReveal(
                        index: 4,
                        child: _PlaybackHistoryList(
                          historyItems: historyItems,
                          library: library,
                          onTap: (book) {
                            final coverPath = book.coverPath;
                            if (coverPath != null) {
                              unawaited(ref.read(coverPaletteProvider(book.id).future));
                              final imageFile = File(coverPath);
                              if (imageFile.existsSync()) {
                                unawaited(precacheImage(FileImage(imageFile), context));
                              }
                            }
                            context.push(AppRoutes.playerPath(book.id));
                          },
                        ),
                      ),
                    ],

                    const SizedBox(height: AppSpacing.xl),
                    _ExpressiveReveal(
                      index: 5,
                      child: _LibraryStatusOverview(counts: statusCounts),
                    ),
                  ],
                  const SizedBox(height: AppSpacing.xxl),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

String _greeting(bool isEmpty) {
  if (isEmpty) return 'Welcome to AvdiBook';
  final hour = DateTime.now().hour;
  if (hour < 12) return 'Good morning';
  if (hour < 17) return 'Good afternoon';
  return 'Good evening';
}

String _relativeTime(DateTime value) {
  final delta = DateTime.now().difference(value);
  if (delta.inMinutes < 1) return 'just now';
  if (delta.inHours < 1) return '${delta.inMinutes}m ago';
  if (delta.inDays < 1) return '${delta.inHours}h ago';
  return '${delta.inDays}d ago';
}

Map<BookStatus, int> _buildStatusCounts(
  List<Audiobook> books,
  Map<String, BookListeningStats> analyticsByBook,
) {
  final counts = {
    BookStatus.newBook: 0,
    BookStatus.started: 0,
    BookStatus.finished: 0,
  };
  for (final book in books) {
    final listened = analyticsByBook[book.id]?.totalDuration ?? Duration.zero;
    final status = _resolveStatus(book, listened);
    counts[status] = (counts[status] ?? 0) + 1;
  }
  return counts;
}

BookStatus _resolveStatus(Audiobook book, Duration listened) {
  if (book.status != BookStatus.newBook) return book.status;
  if (book.progress >= 0.98) return BookStatus.finished;
  if (book.progress > 0.01 || listened > Duration.zero) {
    return BookStatus.started;
  }
  return BookStatus.newBook;
}

// ---------------------------------------------------------------------------
// Widgets
// ---------------------------------------------------------------------------

class _EmptyState extends StatelessWidget {
  const _EmptyState({
    required this.isBusy,
    required this.errorMessage,
    required this.onImportFiles,
    required this.onImportDirectory,
  });

  final bool isBusy;
  final String? errorMessage;
  final VoidCallback onImportFiles;
  final VoidCallback onImportDirectory;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;

    return Card(
      margin: EdgeInsets.zero,
      clipBehavior: Clip.antiAlias,
      color: scheme.surfaceContainerLow,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [scheme.primaryContainer, scheme.tertiaryContainer],
              ),
            ),
            child: Row(
              children: [
                Icon(
                  Icons.library_music_rounded,
                  size: 30,
                  color: scheme.onPrimaryContainer,
                ),
                const SizedBox(width: 10),
                Text(
                  'Start your listening library',
                  style: text.titleMedium?.copyWith(
                    color: scheme.onPrimaryContainer,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('No audiobooks found', style: text.titleLarge),
                const SizedBox(height: 8),
                Text(
                  'Import files or choose a folder to build your library. '
                  'You can always add more later.',
                  style: text.bodyMedium,
                ),
                const SizedBox(height: 18),
                Wrap(
                  spacing: 12,
                  runSpacing: 12,
                  children: [
                    FilledButton.icon(
                      onPressed: isBusy ? null : onImportFiles,
                      icon: const Icon(Icons.upload_file_rounded),
                      label: const Text('Import files'),
                    ),
                    OutlinedButton.icon(
                      onPressed: isBusy ? null : onImportDirectory,
                      icon: const Icon(Icons.folder_open_rounded),
                      label: const Text('Choose folder'),
                    ),
                  ],
                ),
                if (isBusy) ...[
                  const SizedBox(height: 14),
                  const LinearProgressIndicator(),
                ],
                if (errorMessage != null) ...[
                  const SizedBox(height: 12),
                  Text(
                    errorMessage!,
                    style: text.bodySmall?.copyWith(color: scheme.error),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

// Continue listening -----------------------------------------------------------

class _ContinueListeningCard extends StatelessWidget {
  const _ContinueListeningCard({
    super.key,
    required this.book,
    required this.lastPlayedAt,
    required this.onTap,
  });

  final Audiobook book;
  final DateTime? lastPlayedAt;
  final void Function(Audiobook) onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;
    final progress = (book.progress * 100).round();
    final lastPlayedText =
        lastPlayedAt == null ? 'Not played yet' : _relativeTime(lastPlayedAt!);

    return Card(
      margin: EdgeInsets.zero,
      color: scheme.surfaceContainerLow,
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: () => onTap(book),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              _BookCover(
                book: book,
                width: 82,
                borderRadius: BorderRadius.circular(12),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      book.title,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: text.titleMedium,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      book.author?.name ?? 'Unknown author',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: text.bodySmall?.copyWith(
                        color: scheme.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: 10),
                    TweenAnimationBuilder<double>(
                      tween: Tween<double>(end: book.progress.clamp(0, 1)),
                      duration: const Duration(milliseconds: 420),
                      curve: Curves.easeOutCubic,
                      builder: (context, value, _) =>
                          LinearProgressIndicator(value: value),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      '$progress% complete · Last listened $lastPlayedText',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: text.labelSmall?.copyWith(
                        color: scheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              TweenAnimationBuilder<double>(
                tween: Tween<double>(begin: 0.94, end: 1),
                duration: const Duration(milliseconds: 500),
                curve: Curves.easeOutBack,
                builder: (context, scale, child) => Transform.scale(
                  scale: scale,
                  child: child,
                ),
                child: const Icon(Icons.play_circle_fill_rounded),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// Playback history -------------------------------------------------------

class _PlaybackHistoryList extends StatelessWidget {
  const _PlaybackHistoryList({
    required this.historyItems,
    required this.library,
    required this.onTap,
  });

  final List<PlaybackHistoryEntry> historyItems;
  final List<Audiobook> library;
  final void Function(Audiobook) onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;

    return Column(
      children: historyItems.take(6).toList().asMap().entries.map((entry) {
        final index = entry.key;
        final event = entry.value;
        final book = library.firstWhere((b) => b.id == event.bookId);
        return _ExpressiveReveal(
          index: 5 + index,
          child: Card(
            margin: const EdgeInsets.only(bottom: 8),
            color: scheme.surfaceContainerLow,
            child: ListTile(
              onTap: () => onTap(book),
              leading: CircleAvatar(
                backgroundColor: scheme.secondaryContainer,
                foregroundColor: scheme.onSecondaryContainer,
                child: Icon(
                  event.event == 'pause'
                      ? Icons.pause_rounded
                      : Icons.play_arrow_rounded,
                ),
              ),
              title: Text(
                book.title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              subtitle: Text(
                '${event.event == 'pause' ? 'Paused' : 'Resumed'} at '
                '${DurationFormatter.format(Duration(milliseconds: event.positionMs))} · '
                '${_relativeTime(event.playedAt)}',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: text.bodySmall,
              ),
              trailing: const Icon(Icons.chevron_right_rounded),
            ),
          ),
        );
      }).toList(),
    );
  }
}

class _ExpressiveReveal extends StatefulWidget {
  const _ExpressiveReveal({
    required this.index,
    required this.child,
  });

  final int index;
  final Widget child;

  @override
  State<_ExpressiveReveal> createState() => _ExpressiveRevealState();
}

class _ExpressiveRevealState extends State<_ExpressiveReveal> {
  bool _visible = false;

  @override
  void initState() {
    super.initState();
    final delay = Duration(milliseconds: 40 + (widget.index * 55));
    Timer(delay, () {
      if (!mounted) return;
      setState(() => _visible = true);
    });
  }

  @override
  Widget build(BuildContext context) {
    final reduceMotion = MediaQuery.maybeOf(context)?.disableAnimations ?? false;
    if (reduceMotion) return widget.child;

    return AnimatedSlide(
      duration: const Duration(milliseconds: 420),
      curve: Curves.easeOutCubic,
      offset: _visible ? Offset.zero : const Offset(0, 0.03),
      child: AnimatedOpacity(
        duration: const Duration(milliseconds: 360),
        curve: Curves.easeOutCubic,
        opacity: _visible ? 1 : 0,
        child: widget.child,
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle({required this.title});

  final String title;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Text(
      title,
      style: text.titleLarge?.copyWith(fontWeight: FontWeight.w700),
    );
  }
}

class _BookCover extends StatelessWidget {
  const _BookCover({
    required this.book,
    required this.width,
    required this.borderRadius,
  });

  final Audiobook book;
  final double width;
  final BorderRadius borderRadius;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final hasImage =
        book.coverPath != null && File(book.coverPath!).existsSync();

    return ClipRRect(
      borderRadius: borderRadius,
      child: SizedBox(
        width: width,
        height: width * 1.35,
        child: hasImage
            ? Image.file(File(book.coverPath!), fit: BoxFit.cover)
            : DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [scheme.primaryContainer, scheme.tertiaryContainer],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                ),
                child: Icon(
                  Icons.headphones_rounded,
                  size: 32,
                  color: scheme.onPrimaryContainer,
                ),
              ),
      ),
    );
  }
}

// Analytics card --------------------------------------------------------------

class _ListeningAnalyticsCard extends StatelessWidget {
  const _ListeningAnalyticsCard({
    required this.totalListening,
    required this.averageSession,
    required this.sessions,
  });

  final Duration totalListening;
  final Duration averageSession;
  final int sessions;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;

    return Card(
      margin: EdgeInsets.zero,
      clipBehavior: Clip.antiAlias,
      color: scheme.surfaceContainerHigh,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(28),
      ),
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
              decoration: BoxDecoration(
                color: scheme.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                children: [
                  Container(
                    width: 30,
                    height: 30,
                    decoration: BoxDecoration(
                      color: scheme.primaryContainer,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      Icons.insights_rounded,
                      size: 18,
                      color: scheme.onPrimaryContainer,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'Listening analytics',
                      style: text.titleMedium?.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 14),
            Row(
              children: [
                Expanded(
                  child: _AnalyticsStat(
                    label: 'Total time',
                    value: DurationFormatter.formatHuman(totalListening),
                    icon: Icons.graphic_eq_rounded,
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: _AnalyticsStat(
                    label: 'Avg session',
                    value: DurationFormatter.formatHuman(averageSession),
                    icon: Icons.schedule_rounded,
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: _AnalyticsStat(
                    label: 'Sessions',
                    value: '$sessions',
                    icon: Icons.repeat_rounded,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _AnalyticsStat extends StatelessWidget {
  const _AnalyticsStat({
    required this.label,
    required this.value,
    required this.icon,
  });

  final String label;
  final String value;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
      decoration: BoxDecoration(
        color: scheme.surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: scheme.outlineVariant.withValues(alpha: 0.6),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 28,
            height: 28,
            decoration: BoxDecoration(
              color: scheme.secondaryContainer,
              shape: BoxShape.circle,
            ),
            child: Icon(icon, size: 16, color: scheme.onSecondaryContainer),
          ),
          const SizedBox(height: 10),
          Text(
            value,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: text.titleMedium?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 2),
          Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: text.labelSmall?.copyWith(color: scheme.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

// Library status overview -----------------------------------------------------

class _LibraryStatusOverview extends StatelessWidget {
  const _LibraryStatusOverview({required this.counts});

  final Map<BookStatus, int> counts;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        color: scheme.surfaceContainer,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Library status',
            style: text.titleMedium?.copyWith(fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 10),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _StatusSummaryPill(
                icon: Icons.fiber_new_rounded,
                label: 'New',
                count: counts[BookStatus.newBook] ?? 0,
                color: scheme.tertiaryContainer,
                foreground: scheme.onTertiaryContainer,
              ),
              _StatusSummaryPill(
                icon: Icons.auto_stories_rounded,
                label: 'Started',
                count: counts[BookStatus.started] ?? 0,
                color: scheme.primaryContainer,
                foreground: scheme.onPrimaryContainer,
              ),
              _StatusSummaryPill(
                icon: Icons.task_alt_rounded,
                label: 'Finished',
                count: counts[BookStatus.finished] ?? 0,
                color: scheme.secondaryContainer,
                foreground: scheme.onSecondaryContainer,
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _StatusSummaryPill extends StatelessWidget {
  const _StatusSummaryPill({
    required this.icon,
    required this.label,
    required this.count,
    required this.color,
    required this.foreground,
  });

  final IconData icon;
  final String label;
  final int count;
  final Color color;
  final Color foreground;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 15, color: foreground),
          const SizedBox(width: 6),
          Text(
            '$label ($count)',
            style: text.labelMedium?.copyWith(
              color: foreground,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}
