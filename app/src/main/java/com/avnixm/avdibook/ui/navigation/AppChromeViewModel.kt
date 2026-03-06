package com.avnixm.avdibook.ui.navigation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.avnixm.avdibook.AppContainer
import com.avnixm.avdibook.data.model.ListeningSettings
import com.avnixm.avdibook.data.prefs.AppPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MiniPlayerUiState(
    val isVisible: Boolean = false,
    val bookId: Long? = null,
    val title: String = "",
    val subtitle: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val coverArtPath: String? = null
)

data class AppChromeUiState(
    val isInitializing: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val themeMode: AppPreferences.ThemeMode = AppPreferences.ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val pureBlackDarkEnabled: Boolean = false,
    val listeningDefaults: ListeningSettings = ListeningSettings(),
    val textScalePreset: AppPreferences.TextScalePreset = AppPreferences.TextScalePreset.STANDARD,
    val reducedMotionEnabled: Boolean = false,
    val miniPlayer: MiniPlayerUiState = MiniPlayerUiState()
)

class AppChromeViewModel(
    application: Application,
    private val appPreferences: AppPreferences,
    private val appContainer: AppContainer
) : AndroidViewModel(application) {
    private val miniPlayerState = MutableStateFlow(MiniPlayerUiState())

    val uiState: StateFlow<AppChromeUiState> = combine(
        appPreferences.onboardingCompleted,
        appPreferences.themeMode,
        appPreferences.dynamicColorEnabled,
        appPreferences.pureBlackDarkEnabled,
        appPreferences.listeningDefaults,
        appPreferences.accessibilitySettings,
        miniPlayerState
    ) { onboardingCompleted, themeMode, dynamicColorEnabled, pureBlackDarkEnabled, listeningDefaults, accessibilitySettings, miniPlayer ->
        AppChromeUiState(
            isInitializing = false,
            onboardingCompleted = onboardingCompleted,
            themeMode = themeMode,
            dynamicColorEnabled = dynamicColorEnabled,
            pureBlackDarkEnabled = pureBlackDarkEnabled,
            listeningDefaults = listeningDefaults,
            textScalePreset = accessibilitySettings.textScalePreset,
            reducedMotionEnabled = accessibilitySettings.reducedMotionEnabled,
            miniPlayer = miniPlayer
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppChromeUiState()
    )

    init {
        viewModelScope.launch {
            while (isActive) {
                runCatching {
                    val controller = appContainer.playbackControllerFacade.connectController()
                    val currentItem = controller.currentMediaItem
                    if (currentItem == null) {
                        miniPlayerState.update { MiniPlayerUiState() }
                    } else {
                        val bookId = currentItem.mediaMetadata.extras
                            ?.getLong(com.avnixm.avdibook.playback.PlaybackContract.EXTRA_BOOK_ID, -1L)
                            ?.takeIf { it >= 0L }
                        val coverArtPath = bookId?.let {
                            appContainer.database.bookDao().getBookById(it)?.coverArtPath
                        }
                        miniPlayerState.update {
                            MiniPlayerUiState(
                                isVisible = true,
                                bookId = bookId,
                                title = currentItem.mediaMetadata.title?.toString().orEmpty(),
                                subtitle = currentItem.mediaMetadata.albumTitle?.toString().orEmpty(),
                                isPlaying = controller.isPlaying,
                                positionMs = controller.currentPosition,
                                durationMs = controller.duration.coerceAtLeast(0L),
                                coverArtPath = coverArtPath
                            )
                        }
                    }
                }.onFailure { e ->
                    Log.e("AppChromeVM", "mini-player controller poll failed", e)
                }
                delay(1_000)
            }
        }
    }

    fun setOnboardingCompleted(value: Boolean) {
        viewModelScope.launch {
            appPreferences.setOnboardingCompleted(value)
        }
    }

    fun setThemeMode(themeMode: AppPreferences.ThemeMode) {
        viewModelScope.launch {
            appPreferences.setThemeMode(themeMode)
        }
    }

    fun setDynamicColorEnabled(value: Boolean) {
        viewModelScope.launch {
            appPreferences.setDynamicColorEnabled(value)
        }
    }

    fun setPureBlackDarkEnabled(value: Boolean) {
        viewModelScope.launch {
            appPreferences.setPureBlackDarkEnabled(value)
        }
    }

    fun setDefaultSpeed(value: Float) {
        viewModelScope.launch {
            appPreferences.setDefaultSpeed(value)
        }
    }

    fun setDefaultSkipBackSec(value: Int) {
        viewModelScope.launch {
            appPreferences.setDefaultSkipBackSec(value)
        }
    }

    fun setDefaultSkipForwardSec(value: Int) {
        viewModelScope.launch {
            appPreferences.setDefaultSkipForwardSec(value)
        }
    }

    fun setDefaultAutoRewindSec(value: Int) {
        viewModelScope.launch {
            appPreferences.setDefaultAutoRewindSec(value)
        }
    }

    fun setDefaultAutoRewindAfterPauseSec(value: Int) {
        viewModelScope.launch {
            appPreferences.setDefaultAutoRewindAfterPauseSec(value)
        }
    }

    fun setDefaultUseLoudnessBoost(value: Boolean) {
        viewModelScope.launch {
            appPreferences.setDefaultUseLoudnessBoost(value)
        }
    }

    fun setTextScalePreset(value: AppPreferences.TextScalePreset) {
        viewModelScope.launch {
            appPreferences.setTextScalePreset(value)
        }
    }

    fun setReducedMotionEnabled(value: Boolean) {
        viewModelScope.launch {
            appPreferences.setReducedMotionEnabled(value)
        }
    }

    fun toggleMiniPlayerPlayPause() {
        viewModelScope.launch {
            runCatching {
                val controller = appContainer.playbackControllerFacade.connectController()
                if (controller.isPlaying) {
                    appContainer.playbackControllerFacade.pause()
                } else {
                    appContainer.playbackControllerFacade.play()
                }
            }
        }
    }

    fun miniPlayerSkipBack() {
        viewModelScope.launch {
            runCatching {
                appContainer.playbackControllerFacade.seekBy(-10_000L)
            }
        }
    }

    fun miniPlayerSkipForward() {
        viewModelScope.launch {
            runCatching {
                appContainer.playbackControllerFacade.seekBy(30_000L)
            }
        }
    }

    companion object {
        fun factory(application: Application, appContainer: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    AppChromeViewModel(
                        application = application,
                        appPreferences = appContainer.appPreferences,
                        appContainer = appContainer
                    )
                }
            }
    }
}
