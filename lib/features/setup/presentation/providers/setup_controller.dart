import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../features/audiobooks/data/repositories/audiobook_repository_impl.dart';
import '../../../../features/audiobooks/data/services/audio_metadata_service.dart';
import '../../../../features/audiobooks/data/services/audiobook_mapper_service.dart';
import '../../../../features/audiobooks/data/services/audiobook_parser_service.dart';
import '../../../../features/audiobooks/domain/models/audiobook.dart';
import '../../../../features/audiobooks/domain/repositories/audiobook_repository.dart';
import '../../../../shared/providers/app_bootstrap_provider.dart';
import '../../../../shared/providers/app_state_provider.dart';
import '../../../../shared/providers/library_provider.dart';
import '../../data/services/library_import_service.dart';
import '../../domain/models/imported_audiobook.dart';

class SetupState {
  const SetupState({
    this.isBusy = false,
    this.errorMessage,
    this.lastImported = const [],
  });

  final bool isBusy;
  final String? errorMessage;
  final List<Audiobook> lastImported;

  SetupState copyWith({
    bool? isBusy,
    String? errorMessage,
    bool clearError = false,
    List<Audiobook>? lastImported,
  }) {
    return SetupState(
      isBusy: isBusy ?? this.isBusy,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      lastImported: lastImported ?? this.lastImported,
    );
  }
}

final libraryImportServiceProvider = Provider<LibraryImportService>((ref) {
  return LibraryImportService();
});

final audiobookParserServiceProvider = Provider<AudiobookParserService>((ref) {
  return AudiobookParserService(ref.read(audioMetadataServiceProvider));
});

final audioMetadataServiceProvider = Provider<AudioMetadataService>((ref) {
  return AudioMetadataService();
});

final audiobookMapperServiceProvider = Provider<AudiobookMapperService>((ref) {
  return AudiobookMapperService(ref.read(audiobookParserServiceProvider));
});

final audiobookRepositoryProvider = Provider<AudiobookRepository>((ref) {
  return AudiobookRepositoryImpl(ref.read(startupStorageServiceProvider));
});

final setupControllerProvider =
    NotifierProvider<SetupController, SetupState>(SetupController.new);

class SetupController extends Notifier<SetupState> {
  @override
  SetupState build() => const SetupState();

  Future<bool> importFiles() async {
    state = state.copyWith(isBusy: true, clearError: true);

    try {
      final importer = ref.read(libraryImportServiceProvider);
      final imported = await importer.importFiles();

      // Allow empty file selection to proceed (user may have cancelled)
      if (imported.isEmpty) {
        state = state.copyWith(isBusy: false, clearError: true);
        return false; // User cancelled, nothing to import
      }

      await _normalizeMergeAndPersist(imported);
      return true;
    } catch (_) {
      state = state.copyWith(
        isBusy: false,
        errorMessage: 'Could not import audiobook files.',
      );
      return false;
    }
  }

  Future<bool> importDirectory() async {
    state = state.copyWith(isBusy: true, clearError: true);

    try {
      final importer = ref.read(libraryImportServiceProvider);
      final imported = await importer.importDirectory();

      // Allow empty folders to be imported successfully
      await _normalizeMergeAndPersist(imported);

      // Save the folder path for future rescanning
      if (importer.lastSelectedDirectory != null) {
        await ref
            .read(scanFolderPathProvider.notifier)
            .set(importer.lastSelectedDirectory!);
      }

      return true;
    } on Exception catch (e) {
      final errorMsg = e.toString();
      String userMessage = 'Could not scan the selected folder.';

      if (errorMsg.contains('Permission denied by user')) {
        userMessage =
            'You need to grant file access permission. Please try again and tap "Allow" when prompted.';
      } else if (errorMsg.contains('permanently denied')) {
        userMessage =
            'File access permission is disabled. Please enable it in: Settings > Apps > AvdiBook > Permissions > Storage.';
      } else if (errorMsg.contains('Permission denied')) {
        userMessage = 'Permission denied. Please grant file access permission.';
      } else if (errorMsg.contains('does not exist')) {
        userMessage = 'Directory does not exist or is no longer accessible.';
      } else if (errorMsg.contains('Could not read files')) {
        userMessage =
            'Could not read files from directory. Check permissions and try again.';
      }

      state = state.copyWith(
        isBusy: false,
        errorMessage: userMessage,
      );
      return false;
    } catch (_) {
      state = state.copyWith(
        isBusy: false,
        errorMessage:
            'Unexpected error. Please check your storage access permissions.',
      );
      return false;
    }
  }

  Future<void> completeOnboarding() async {
    final storage = ref.read(startupStorageServiceProvider);
    await storage.setOnboardingComplete(true);
  }

  /// Rescans the previously selected folder. If no folder was saved, returns false.
  Future<bool> rescanLibrary() async {
    final savedPath = ref.read(scanFolderPathProvider);
    if (savedPath == null || savedPath.isEmpty) {
      return false;
    }

    state = state.copyWith(isBusy: true, clearError: true);

    try {
      final importer = ref.read(libraryImportServiceProvider);
      final imported = await importer.scanFromPath(savedPath);
      await _normalizeMergeAndPersist(imported);
      return true;
    } on Exception catch (e) {
      final errorMsg = e.toString();
      String userMessage = 'Could not rescan the library folder.';

      if (errorMsg.contains('Permission denied')) {
        userMessage = 'File access permission denied. Please grant permission and try again.';
      } else if (errorMsg.contains('does not exist')) {
        userMessage = 'The saved folder no longer exists. Please choose a new folder.';
      } else if (errorMsg.contains('Could not read files')) {
        userMessage = 'Could not read files from the folder. Check permissions and try again.';
      }

      state = state.copyWith(isBusy: false, errorMessage: userMessage);
      return false;
    } catch (_) {
      state = state.copyWith(
        isBusy: false,
        errorMessage: 'Unexpected error while rescanning.',
      );
      return false;
    }
  }

  Future<void> _normalizeMergeAndPersist(List<ImportedAudiobook> imported) async {
    final mapper = ref.read(audiobookMapperServiceProvider);
    final repository = ref.read(audiobookRepositoryProvider);

    final normalized = await mapper.mapImported(imported);
    await repository.addBooks(normalized);

    final merged = await repository.getLibrary();
    ref.read(libraryProvider.notifier).setLibrary(merged);

    final storage = ref.read(startupStorageServiceProvider);
    await storage.setSetupComplete(true);
    await storage.setOnboardingComplete(true);

    state = state.copyWith(
      isBusy: false,
      lastImported: normalized,
    );
  }
}
