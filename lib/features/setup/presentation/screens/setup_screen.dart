import 'package:avdibook/app/theme/app_spacing.dart';
import 'package:avdibook/core/constants/app_constants.dart';
import 'package:avdibook/core/widgets/app_scaffold.dart';
import 'package:avdibook/core/widgets/section_header.dart';
import 'package:avdibook/core/widgets/soft_pill_button.dart';
import 'package:avdibook/features/setup/presentation/providers/setup_controller.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

class SetupScreen extends ConsumerWidget {
  const SetupScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;
    final state = ref.watch(setupControllerProvider);

    ref.listen<SetupState>(setupControllerProvider, (previous, next) {
      if (next.errorMessage != null &&
          next.errorMessage != previous?.errorMessage) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(next.errorMessage!)),
        );
      }
    });

    return AppScaffold(
      showAppBar: false,
      body: ListView(
        children: [
          const SectionHeader(title: 'Set up your library'),
          const SizedBox(height: AppSpacing.xxl),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: scheme.surfaceContainerHighest,
              borderRadius: BorderRadius.circular(28),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(
                  Icons.folder_copy_rounded,
                  size: 34,
                  color: scheme.primary,
                ),
                const SizedBox(height: 16),
                Text(
                  'Choose a folder for your audiobooks',
                  style: text.titleLarge,
                ),
                const SizedBox(height: 8),
                Text(
                  'AvdiBook will scan supported audiobook files like MP3, M4B, M4A, AAC, WAV, and OGG where supported.',
                  style: text.bodyMedium,
                ),
                const SizedBox(height: 18),
                FilledButton(
                  onPressed: state.isBusy
                      ? null
                      : () async {
                          final controller =
                              ref.read(setupControllerProvider.notifier);
                          final ok = await controller.importDirectory();
                          if (!context.mounted) return;
                          if (ok) {
                            context.go(AppRoutes.home);
                            return;
                          }

                          final shouldFallback = await showDialog<bool>(
                                context: context,
                                builder: (dialogContext) => AlertDialog(
                                  title: const Text('Folder scan unavailable'),
                                  content: const Text(
                                    'This folder could not be scanned on your device. Import files manually now?',
                                  ),
                                  actions: [
                                    TextButton(
                                      onPressed: () =>
                                          Navigator.of(dialogContext).pop(false),
                                      child: const Text('Cancel'),
                                    ),
                                    FilledButton(
                                      onPressed: () =>
                                          Navigator.of(dialogContext).pop(true),
                                      child: const Text('Import files'),
                                    ),
                                  ],
                                ),
                              ) ??
                              false;

                          if (!shouldFallback) {
                            return;
                          }

                          final fallbackOk = await controller.importFiles();
                          if (!context.mounted) return;
                          if (fallbackOk) {
                            context.go(AppRoutes.home);
                          }
                        },
                  child: Text(state.isBusy ? 'Scanning...' : 'Choose folder'),
                ),
              ],
            ),
          ),
          const SizedBox(height: AppSpacing.lg),
          Center(
            child: SoftPillButton(
              label: state.isBusy ? 'Importing...' : 'Import files manually',
              icon: Icons.file_open_rounded,
              onPressed: state.isBusy
                  ? () {}
                  : () async {
                      final ok = await ref
                          .read(setupControllerProvider.notifier)
                          .importFiles();
                      if (!context.mounted) return;
                      if (ok) {
                        context.go(AppRoutes.home);
                      }
                    },
            ),
          ),
          const SizedBox(height: AppSpacing.xxl),
          Text(
            'Supported formats',
            style: text.titleMedium,
          ),
          const SizedBox(height: 12),
          const Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              Chip(label: Text('MP3')),
              Chip(label: Text('M4B')),
              Chip(label: Text('M4A')),
              Chip(label: Text('AAC')),
              Chip(label: Text('WAV')),
              Chip(label: Text('OGG')),
            ],
          ),
          const SizedBox(height: AppSpacing.xxl),
          if (state.lastImported.isNotEmpty) ...[
            Text(
              'Last import',
              style: text.titleMedium,
            ),
            const SizedBox(height: 12),
            ...state.lastImported.take(5).map(
                  (book) => Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: scheme.surfaceContainerHighest,
                        borderRadius: BorderRadius.circular(24),
                      ),
                      child: Row(
                        children: [
                          Container(
                            width: 52,
                            height: 72,
                            decoration: BoxDecoration(
                              color: scheme.primary.withValues(alpha: 0.12),
                              borderRadius: BorderRadius.circular(16),
                            ),
                            child: Icon(
                              Icons.menu_book_rounded,
                              color: scheme.primary,
                            ),
                          ),
                          const SizedBox(width: 14),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(book.title, style: text.titleMedium),
                                const SizedBox(height: 4),
                                Text(
                                  '${book.chapterCount} chapter(s) • ${book.primaryFormat.toUpperCase()}',
                                  style: text.bodySmall,
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
          ],
        ],
      ),
    );
  }
}
