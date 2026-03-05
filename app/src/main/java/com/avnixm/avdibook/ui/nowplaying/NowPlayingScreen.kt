package com.avnixm.avdibook.ui.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avnixm.avdibook.AvdiBookApplication

@Composable
fun NowPlayingRoute(
    bookId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as AvdiBookApplication
    val viewModel: NowPlayingViewModel = viewModel(
        key = "now_playing_$bookId",
        factory = NowPlayingViewModel.factory(
            application = application,
            appContainer = application.appContainer,
            bookId = bookId
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NowPlayingScreen(
        modifier = modifier,
        uiState = uiState,
        onBack = onBack,
        onPlayPause = viewModel::onPlayPauseTapped,
        onSeekBy = viewModel::seekBy,
        onSeekTo = viewModel::seekTo,
        onSetSpeed = viewModel::setSpeed
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    uiState: NowPlayingUiState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onSetSpeed: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSpeedDialogVisible by remember { mutableStateOf(false) }
    val maxDuration = uiState.durationMs.coerceAtLeast(1L)
    var sliderValue by remember(uiState.positionMs, uiState.durationMs) {
        mutableFloatStateOf(uiState.positionMs.coerceIn(0L, maxDuration).toFloat())
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = uiState.bookTitle.ifBlank { "Unknown Book" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = uiState.trackTitle.ifBlank { "No track loaded" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onSeekTo(sliderValue.toLong()) },
                valueRange = 0f..maxDuration.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatClock(uiState.positionMs), style = MaterialTheme.typography.bodyMedium)
                Text(formatClock(uiState.durationMs), style = MaterialTheme.typography.bodyMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onSeekBy(-10_000) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("-10s")
                }
                Button(
                    onClick = onPlayPause,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (uiState.isPlaying) "Pause" else "Play")
                }
                OutlinedButton(
                    onClick = { onSeekBy(30_000) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+30s")
                }
            }

            AssistChip(
                onClick = { isSpeedDialogVisible = true },
                label = { Text("Speed ${uiState.speed}x") }
            )
        }
    }

    if (isSpeedDialogVisible) {
        AlertDialog(
            onDismissRequest = { isSpeedDialogVisible = false },
            title = { Text("Playback Speed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SPEED_OPTIONS.forEach { speed ->
                        OutlinedButton(
                            onClick = {
                                onSetSpeed(speed)
                                isSpeedDialogVisible = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${speed}x")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isSpeedDialogVisible = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private val SPEED_OPTIONS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

private fun formatClock(milliseconds: Long): String {
    val safeMillis = milliseconds.coerceAtLeast(0L)
    val totalSeconds = safeMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
