package com.avnixm.avdibook.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.avnixm.avdibook.ui.navigation.MiniPlayerUiState

@Composable
fun MiniPlayerBar(
    state: MiniPlayerUiState,
    onOpenPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible || state.bookId == null) return

    val progress = if (state.durationMs > 0L) {
        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Playback progress: ${(progress * 100).toInt()}%"
                    }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPlayer)
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                        }
                    }
                    AsyncImage(
                        model = state.coverArtPath?.let { java.io.File(it) },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.subtitle.ifBlank { "Now Playing" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = state.title.ifBlank { "Unknown track" },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onSkipBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Skip back 10 seconds",
                        modifier = Modifier.size(20.dp)
                    )
                }

                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play"
                    )
                }

                IconButton(
                    onClick = onSkipForward,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward30,
                        contentDescription = "Skip forward 30 seconds",
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}
