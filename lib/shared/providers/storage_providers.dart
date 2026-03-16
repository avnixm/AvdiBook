import 'package:avdibook/features/setup/data/local/app_database.dart';
import 'package:avdibook/features/setup/data/services/startup_storage_service.dart';
import 'package:avdibook/shared/providers/preferences_provider.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final appDatabaseProvider = Provider<AppDatabase>((ref) {
  final database = AppDatabase();
  ref.onDispose(database.close);
  return database;
});

final startupStorageServiceProvider = Provider<StartupStorageService>((ref) {
  return StartupStorageService(
    ref.read(sharedPreferencesProvider),
    ref.read(appDatabaseProvider),
  );
});
