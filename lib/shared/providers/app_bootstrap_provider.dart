import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:avdibook/core/constants/app_constants.dart';
import 'package:avdibook/shared/providers/library_provider.dart';
import 'package:avdibook/shared/providers/storage_providers.dart';

final appBootstrapProvider = FutureProvider<String>((ref) async {
  final storage = ref.read(startupStorageServiceProvider);
  await storage.ensureDriftMigration();

  final onboardingComplete = await storage.getOnboardingComplete();
  final setupComplete = await storage.getSetupComplete();
  final libraryItems = await storage.getLibraryItems();

  ref.read(libraryProvider.notifier).setLibrary(libraryItems);

  if (!onboardingComplete) {
    return AppRoutes.onboarding;
  }

  if (!setupComplete) {
    return AppRoutes.setup;
  }

  return AppRoutes.home;
});
