import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:avdibook/core/constants/app_constants.dart';
import 'package:avdibook/features/setup/data/services/startup_storage_service.dart';
import 'package:avdibook/shared/providers/library_provider.dart';

final startupStorageServiceProvider = Provider<StartupStorageService>((ref) {
  return StartupStorageService();
});

final appBootstrapProvider = FutureProvider<String>((ref) async {
  final storage = ref.read(startupStorageServiceProvider);

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
