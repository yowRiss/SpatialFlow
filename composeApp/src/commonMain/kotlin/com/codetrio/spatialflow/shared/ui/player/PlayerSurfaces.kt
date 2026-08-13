package com.codetrio.spatialflow.shared.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
fun FullPlayer(state: PlayerUiState, queue: List<SongItem>, controller: PlaybackController, lyrics: List<LyricLine>, onDismiss: () -> Unit, onLyrics: () -> Unit, onQueue: () -> Unit) {
    val song = state.currentSong ?: return
    val duration = state.duration.coerceAtLeast(song.duration.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()).coerceAtLeast(1)
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))).padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onDismiss) { Icon(Icons.Default.KeyboardArrowDown, "Close player") }
            Text("NOW PLAYING", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            IconButton({}) { Icon(Icons.Default.MoreVert, "More") }
        }
        Spacer(Modifier.height(28.dp)); Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Artwork(song, 300.dp) }
        Spacer(Modifier.height(28.dp)); Row(verticalAlignment = Alignment.CenterVertically) { SongText(song, Modifier.fillMaxWidth()); IconButton({}) { Icon(Icons.Default.FavoriteBorder, "Favourite") } }
        Spacer(Modifier.height(16.dp)); Slider(state.positionMs.coerceIn(0, duration.toLong()).toFloat(), { controller.dispatch(PlayerCommand.SeekTo(it.toLong())) }, valueRange = 0f..duration.toFloat())
        Row(Modifier.fillMaxWidth()) { Text(time(state.positionMs), Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall); Text(time(duration.toLong()), style = MaterialTheme.typography.labelSmall) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton({ controller.dispatch(PlayerCommand.SetRepeatMode(nextRepeat(state.repeatMode))) }) { Icon(Icons.Default.Repeat, "Repeat") }
            IconButton({ controller.dispatch(PlayerCommand.Previous) }) { Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(36.dp)) }
            Surface(Modifier.size(70.dp), CircleShape, color = MaterialTheme.colorScheme.primary) { IconButton({ controller.dispatch(PlayerCommand.TogglePlayback) }) { Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause", Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onPrimary) } }
            IconButton({ controller.dispatch(PlayerCommand.Next) }) { Icon(Icons.Default.SkipNext, "Next", Modifier.size(36.dp)) }
            IconButton(onQueue) { Icon(Icons.Default.QueueMusic, "Queue") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { IconButton(onLyrics) { Icon(Icons.Default.Lyrics, "Lyrics") }; Text("${queue.size} tracks queued", style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
fun QueueDrawer(queue: List<SongItem>, state: PlayerUiState, controller: PlaybackController, onDismiss: () -> Unit) = Column(Modifier.fillMaxSize().padding(24.dp)) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onDismiss) { Icon(Icons.Default.ArrowBack, "Back") }; Text("Queue", style = MaterialTheme.typography.headlineMedium) }
    LazyColumn { itemsIndexed(queue) { index, song -> Row(Modifier.fillMaxWidth().clickable { controller.dispatch(PlayerCommand.PlayAt(index)) }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Artwork(song, 40.dp); Spacer(Modifier.width(12.dp)); SongText(song, Modifier.fillMaxWidth()); if (index == state.currentSongIndex) Icon(Icons.Default.PlayArrow, "Playing", tint = MaterialTheme.colorScheme.primary) } } }
}

@Composable
fun SyncedLyrics(lines: List<LyricLine>, positionMs: Long, onDismiss: () -> Unit) = Column(Modifier.fillMaxSize().padding(24.dp)) {
    Row(Modifier.fillMaxWidth()) { IconButton(onDismiss) { Icon(Icons.Default.ArrowBack, "Back") }; Text("Lyrics", style = MaterialTheme.typography.headlineMedium) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(18.dp)) { itemsIndexed(lines) { index, line ->
        val active = line.startTimeMs <= positionMs && (lines.getOrNull(index + 1)?.startTimeMs ?: Long.MAX_VALUE) > positionMs
        Text(line.content, style = MaterialTheme.typography.headlineSmall, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = .45f), fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    } }
}

@Composable private fun Artwork(song: SongItem, size: androidx.compose.ui.unit.Dp) = Surface(Modifier.size(size).clip(RoundedCornerShape(size / 10)), color = MaterialTheme.colorScheme.secondaryContainer) { Box(contentAlignment = Alignment.Center) { Text(song.title.take(1).uppercase(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSecondaryContainer) } }
@Composable private fun SongText(song: SongItem, modifier: Modifier = Modifier) = Column(modifier) { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold); Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
private fun time(ms: Long): String = "%d:%02d".format(ms / 60_000, (ms / 1_000) % 60)
private fun nextRepeat(mode: RepeatMode) = when (mode) { RepeatMode.OFF -> RepeatMode.ALL; RepeatMode.ALL -> RepeatMode.ONE; RepeatMode.ONE -> RepeatMode.OFF }
