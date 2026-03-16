import 'package:collection/collection.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:avdibook/core/utils/duration_formatter.dart';
import 'package:avdibook/features/player/presentation/providers/player_provider.dart';
import 'package:avdibook/shared/providers/library_provider.dart';

class ChapterListScreen extends ConsumerWidget {
  const ChapterListScreen({super.key, required this.bookId});

  final String bookId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final library = ref.watch(libraryProvider);
    final playerState = ref.watch(playerProvider);
    final book = library.where((b) => b.id == bookId).firstOrNull;

    if (book == null) {
      return const Scaffold(
        body: Center(child: Text('Book not found.')),
      );
    }

    final chapters = book.chapters;

    return Scaffold(
      appBar: AppBar(title: const Text('Chapters')),
      body: ListView.separated(
        padding: const EdgeInsets.fromLTRB(12, 12, 12, 24),
        itemCount: chapters.length,
        separatorBuilder: (_, _) => const SizedBox(height: 8),
        itemBuilder: (context, index) {
          final chapter = chapters[index];
          final active = index == playerState.currentChapterIndex;

          return Material(
            color: active
                ? Theme.of(context)
                    .colorScheme
                    .primaryContainer
                    .withValues(alpha: 0.45)
                : Theme.of(context).colorScheme.surfaceContainerLow,
            borderRadius: BorderRadius.circular(16),
            child: ListTile(
              onTap: () async {
                await ref
                    .read(playerProvider.notifier)
                    .seekToChapterIndex(index);
                if (context.mounted) Navigator.of(context).pop();
              },
              leading: CircleAvatar(
                radius: 14,
                backgroundColor: active
                    ? Theme.of(context).colorScheme.primary
                    : Theme.of(context).colorScheme.surfaceContainerHighest,
                child: Text(
                  '${index + 1}',
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    color: active
                        ? Theme.of(context).colorScheme.onPrimary
                        : Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
              ),
              title: Text(
                chapter.title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              subtitle: chapter.duration == null
                  ? null
                  : Text(
                      DurationFormatter.formatHuman(chapter.duration!),
                    ),
              trailing: active
                  ? Icon(
                      Icons.graphic_eq_rounded,
                      color: Theme.of(context).colorScheme.primary,
                    )
                  : const Icon(Icons.chevron_right_rounded),
            ),
          );
        },
      ),
    );
  }
}
