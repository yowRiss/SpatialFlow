package com.codetrio.spatialflow.shared.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.shared.data.lyrics.LyricLine
import com.codetrio.spatialflow.shared.model.SongItem
import com.codetrio.spatialflow.shared.player.PlaybackController
import com.codetrio.spatialflow.shared.player.PlayerCommand
import com.codetrio.spatialflow.shared.player.PlayerUiState
import com.codetrio.spatialflow.shared.player.RepeatMode
import com.codetrio.spatialflow.shared.player.SleepTimerMode
import com.codetrio.spatialflow.shared.ui.components.ArtworkImage

@Composable
fun MiniPlayer(state: PlayerUiState, controller: PlaybackController, onExpand: () -> Unit) {
    val song = state.currentSong ?: return
    Surface(Modifier.fillMaxWidth().clickable(onClick = onExpand), tonalElevation = 10.dp) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            val duration = state.duration.coerceAtLeast(song.duration.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()).coerceAtLeast(1)
            Slider(state.positionMs.coerceIn(0, duration.toLong()).toFloat(), { controller.dispatch(PlayerCommand.SeekTo(it.toLong())) }, valueRange = 0f..duration.toFloat())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(song, 44.dp); Spacer(Modifier.width(12.dp))
                SongText(song, Modifier.fillMaxWidth())
                IconButton({ controller.dispatch(PlayerCommand.Previous) }) { Icon(Icons.Default.SkipPrevious, "Previous") }
                IconButton({ controller.dispatch(PlayerCommand.TogglePlayback) }) { Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause") }
                IconButton({ controller.dispatch(PlayerCommand.Next) }) { Icon(Icons.Default.SkipNext, "Next") }
            }
        }
    }
}

@Composable
fun FullPlayer(state: PlayerUiState, queue: List<SongItem>, controller: PlaybackController, lyrics: List<LyricLine>, isFavourite: Boolean, onToggleFavourite: () -> Unit, onDismiss: () -> Unit, onLyrics: () -> Unit, onQueue: () -> Unit, onActions: () -> Unit, onSleepTimer: () -> Unit) {
    val song = state.currentSong ?: return
    val duration = state.duration.coerceAtLeast(song.duration.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()).coerceAtLeast(1)
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))).padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onDismiss) { Icon(Icons.Default.KeyboardArrowDown, "Close player") }
            Text("NOW PLAYING", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            IconButton(onActions) { Icon(Icons.Default.MoreVert, "More") }
        }
        Spacer(Modifier.height(28.dp)); Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Artwork(song, 300.dp) }
        Spacer(Modifier.height(28.dp)); Row(verticalAlignment = Alignment.CenterVertically) {
            SongText(song, Modifier.fillMaxWidth())
            IconButton(onToggleFavourite) {
                Icon(if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (isFavourite) "Remove from favourites" else "Add to favourites", tint = if (isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(16.dp)); Slider(state.positionMs.coerceIn(0, duration.toLong()).toFloat(), { controller.dispatch(PlayerCommand.SeekTo(it.toLong())) }, valueRange = 0f..duration.toFloat())
        Row(Modifier.fillMaxWidth()) { Text(time(state.positionMs), Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall); Text(time(duration.toLong()), style = MaterialTheme.typography.labelSmall) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton({ controller.dispatch(PlayerCommand.SetShuffle(!state.isShuffleEnabled)) }) {
                Icon(Icons.Default.Shuffle, "Shuffle", tint = if (state.isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
            IconButton({ controller.dispatch(PlayerCommand.SetRepeatMode(nextRepeat(state.repeatMode))) }) { Icon(Icons.Default.Repeat, "Repeat") }
            IconButton({ controller.dispatch(PlayerCommand.Previous) }) { Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(36.dp)) }
            Surface(Modifier.size(70.dp), CircleShape, color = MaterialTheme.colorScheme.primary) { IconButton({ controller.dispatch(PlayerCommand.TogglePlayback) }) { Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause", Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onPrimary) } }
            IconButton({ controller.dispatch(PlayerCommand.Next) }) { Icon(Icons.Default.SkipNext, "Next", Modifier.size(36.dp)) }
            IconButton(onQueue) { Icon(Icons.Default.QueueMusic, "Queue") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onLyrics) { Icon(Icons.Default.Lyrics, "Lyrics") }
            Text("${queue.size} tracks queued", style = MaterialTheme.typography.labelMedium)
            IconButton(onSleepTimer) { Icon(Icons.Default.Timer, "Sleep timer") }
        }
    }
}

/** Desktop Compose counterpart of Android's SleepTimerBottomSheet. The engine
 * owns countdown and end-of-playback behavior; this surface only dispatches
 * the same shared commands. */
@Composable
fun SleepTimerDialog(controller: PlaybackController, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Stop playback after a selected time or when playback reaches its end.")
                listOf(15, 30, 45, 60, 90, 120).forEach { minutes ->
                    TextButton(onClick = {
                        controller.dispatch(PlayerCommand.SetSleepTimer(SleepTimerMode.CUSTOM, minutes))
                        onDismiss()
                    }) { Text("Stop in $minutes minutes") }
                }
                TextButton(onClick = {
                    controller.dispatch(PlayerCommand.SetSleepTimer(SleepTimerMode.END_OF_SONG))
                    onDismiss()
                }) { Text("Stop at end of song") }
                TextButton(onClick = {
                    controller.dispatch(PlayerCommand.SetSleepTimer(SleepTimerMode.END_OF_QUEUE))
                    onDismiss()
                }) { Text("Stop at end of queue") }
            }
        },
        confirmButton = {
            Button(onClick = {
                controller.dispatch(PlayerCommand.SetSleepTimer(SleepTimerMode.OFF))
                onDismiss()
            }) { Text("Cancel timer") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
fun QueueDrawer(queue: List<SongItem>, state: PlayerUiState, controller: PlaybackController, onReorder: (Int, Int) -> Unit, onDismiss: () -> Unit) = Column(Modifier.fillMaxSize().padding(24.dp)) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onDismiss) { Icon(Icons.Default.ArrowBack, "Back") }; Text("Queue", style = MaterialTheme.typography.headlineMedium) }
    LazyColumn { itemsIndexed(queue) { index, song -> Row(Modifier.fillMaxWidth().clickable { controller.dispatch(PlayerCommand.PlayAt(index)) }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(song, 40.dp); Spacer(Modifier.width(12.dp)); SongText(song, Modifier.weight(1f))
        if (index == state.currentSongIndex) Icon(Icons.Default.PlayArrow, "Playing", tint = MaterialTheme.colorScheme.primary)
        IconButton(enabled = index > 0, onClick = { onReorder(index, index - 1) }) { Icon(Icons.Default.ArrowUpward, "Move up") }
        IconButton(enabled = index < queue.lastIndex, onClick = { onReorder(index, index + 1) }) { Icon(Icons.Default.ArrowDownward, "Move down") }
    } } }
}

@Composable
fun SyncedLyrics(lines: List<LyricLine>, positionMs: Long, onSeekTo: (Long) -> Unit, onDismiss: () -> Unit) {
    val listState = rememberLazyListState()
    val activeIndex = lines.indexOfLast { it.startTimeMs <= positionMs }.coerceAtLeast(0)
    LaunchedEffect(activeIndex, lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(activeIndex.coerceIn(lines.indices))
    }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth()) { IconButton(onDismiss) { Icon(Icons.Default.ArrowBack, "Back") }; Text("Lyrics", style = MaterialTheme.typography.headlineMedium) }
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(18.dp)) { itemsIndexed(lines) { index, line ->
        val active = line.startTimeMs <= positionMs && (lines.getOrNull(index + 1)?.startTimeMs ?: Long.MAX_VALUE) > positionMs
        Box(Modifier.fillMaxWidth().clickable { onSeekTo(line.startTimeMs) }) { KaraokeLine(line, positionMs, active) }
        } }
    }
}

/** Uses enhanced-LRC word timestamps when present, while retaining the
 * Android player’s line-level fallback for ordinary synced lyrics. */
@Composable
private fun KaraokeLine(line: LyricLine, positionMs: Long, active: Boolean) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f)
    if (!line.isWordByWord || line.words.isEmpty()) {
        Text(line.content, style = MaterialTheme.typography.headlineSmall, color = if (active) activeColor else inactiveColor, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
        return
    }
    // This is the same two-layer text technique as Android's
    // SyncedLyricsCompose: dim text remains underneath while an off-screen,
    // glowing copy is erased character-by-character ahead of the word sweep.
    val smoothPosition by animateFloatAsState(positionMs.toFloat(), tween(180, easing = LinearEasing), label = "karaokePosition")
    var layout: TextLayoutResult? = null
    val style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
    Box {
        Text(line.content, style = style, color = inactiveColor)
        Text(
            line.content,
            style = style.copy(shadow = Shadow(activeColor.copy(alpha = .42f), Offset.Zero, 16f)),
            color = activeColor,
            onTextLayout = { layout = it },
            modifier = Modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        layout?.let { eraseFutureKaraokeText(it, line.words, smoothPosition.toLong(), this) }
                    }
                },
        )
    }
}

private fun eraseFutureKaraokeText(layout: TextLayoutResult, words: List<com.codetrio.spatialflow.shared.data.lyrics.LyricWord>, positionMs: Long, scope: DrawScope) {
    for (charIndex in 0 until layout.layoutInput.text.length) {
        val word = words.firstOrNull { charIndex in it.charRange }
            ?: words.lastOrNull { it.charRange.last < charIndex }
            ?: words.firstOrNull()
            ?: continue
        val wordStart = word.absoluteStartTimeMs
        val wordEnd = wordStart + word.durationMs.coerceAtLeast(120L)
        val wordProgress = ((positionMs - wordStart).toFloat() / (wordEnd - wordStart).coerceAtLeast(1L)).coerceIn(0f, 1f)
        val eased = 1f - (1f - wordProgress) * (1f - wordProgress) * (1f - wordProgress)
        val range = word.charRange
        val relative = (charIndex - range.first).toFloat() / range.count().coerceAtLeast(1)
        val sweepWidth = .35f
        val charProgress = ((eased * (1f + sweepWidth) - relative) / sweepWidth).coerceIn(0f, 1f)
        if (charProgress < .99f) {
            val path = layout.getPathForRange(charIndex, charIndex + 1)
            if (charProgress < .01f) scope.drawPath(path, Color.Black, blendMode = BlendMode.DstOut)
            else {
                val bounds = layout.getBoundingBox(charIndex)
                val centre = bounds.left + bounds.width * charProgress
                scope.drawPath(
                    path,
                    Brush.horizontalGradient(0f to Color.Transparent, 1f to Color.Black, startX = centre - bounds.width, endX = centre + bounds.width),
                    blendMode = BlendMode.DstOut,
                )
            }
        }
    }
}

@Composable private fun Artwork(song: SongItem, size: androidx.compose.ui.unit.Dp) = ArtworkImage(song.artworkLocation, song.title, Modifier.size(size))
@Composable private fun SongText(song: SongItem, modifier: Modifier = Modifier) = Column(modifier) { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold); Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
private fun time(ms: Long): String = "%d:%02d".format(ms / 60_000, (ms / 1_000) % 60)
private fun nextRepeat(mode: RepeatMode) = when (mode) { RepeatMode.OFF -> RepeatMode.ALL; RepeatMode.ALL -> RepeatMode.ONE; RepeatMode.ONE -> RepeatMode.OFF }
