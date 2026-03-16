import 'package:avdibook/app/theme/app_spacing.dart';
import 'package:avdibook/core/constants/app_constants.dart';
import 'package:avdibook/core/widgets/app_scaffold.dart';
import 'package:avdibook/features/setup/presentation/providers/setup_controller.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

class OnboardingScreen extends ConsumerWidget {
  const OnboardingScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);

    return AppScaffold(
      showAppBar: false,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Spacer(),
          Text(
            'A calm home for your audiobooks.',
            style: theme.textTheme.headlineLarge,
          ),
          const SizedBox(height: AppSpacing.lg),
          Text(
            'Import your audiobook folder, scan supported files, and continue every book from exactly where you stopped.',
            style: theme.textTheme.bodyLarge,
          ),
          const Spacer(),
          FilledButton(
            onPressed: () async {
              await ref
                  .read(setupControllerProvider.notifier)
                  .completeOnboarding();
              if (!context.mounted) return;
              context.go(AppRoutes.setup);
            },
            child: const Text('Get started'),
          ),
          const SizedBox(height: 12),
          Center(
            child: TextButton(
              onPressed: () async {
                await ref
                    .read(setupControllerProvider.notifier)
                    .completeOnboarding();
                if (!context.mounted) return;
                context.go(AppRoutes.setup);
              },
              child: const Text('Continue to library setup'),
            ),
          ),
          const SizedBox(height: 12),
        ],
      ),
    );
  }
}
