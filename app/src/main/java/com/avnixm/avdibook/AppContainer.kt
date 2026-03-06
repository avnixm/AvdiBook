package com.avnixm.avdibook

import android.content.Context
import com.avnixm.avdibook.data.db.AvdiBookDatabase
import com.avnixm.avdibook.data.metadata.ChapterExtractionScheduler
import com.avnixm.avdibook.data.prefs.AppPreferences
import com.avnixm.avdibook.data.repository.BackupRepository
import com.avnixm.avdibook.data.repository.BookDetailsRepository
import com.avnixm.avdibook.data.repository.DefaultBackupRepository
import com.avnixm.avdibook.data.repository.DefaultBookDetailsRepository
import com.avnixm.avdibook.data.repository.DefaultLibraryRepository
import com.avnixm.avdibook.data.repository.DefaultPlaybackRepository
import com.avnixm.avdibook.data.repository.LibraryRepository
import com.avnixm.avdibook.data.repository.PlaybackRepository
import com.avnixm.avdibook.playback.PlaybackControllerFacade

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    val database: AvdiBookDatabase = AvdiBookDatabase.create(applicationContext)
    val appPreferences = AppPreferences(applicationContext)
    private val extractionScheduler = ChapterExtractionScheduler(
        appContext = applicationContext,
        bookDao = database.bookDao(),
        trackDao = database.trackDao(),
        chapterDao = database.chapterDao()
    )

    val libraryRepository: LibraryRepository = DefaultLibraryRepository(
        context = applicationContext,
        database = database,
        extractionScheduler = extractionScheduler
    )

    val playbackRepository: PlaybackRepository = DefaultPlaybackRepository(
        bookDao = database.bookDao(),
        playbackDao = database.playbackDao()
    )

    val bookDetailsRepository: BookDetailsRepository = DefaultBookDetailsRepository(
        bookDao = database.bookDao(),
        trackDao = database.trackDao(),
        playbackDao = database.playbackDao(),
        bookSettingsDao = database.bookSettingsDao(),
        bookmarkDao = database.bookmarkDao(),
        chapterDao = database.chapterDao(),
        appPreferences = appPreferences
    )

    val backupRepository: BackupRepository = DefaultBackupRepository(
        appContext = applicationContext,
        database = database,
        libraryRepository = libraryRepository,
        appPreferences = appPreferences
    )

    val playbackControllerFacade = PlaybackControllerFacade(
        appContext = applicationContext,
        libraryRepository = libraryRepository,
        playbackRepository = playbackRepository,
        bookDetailsRepository = bookDetailsRepository
    )
}
