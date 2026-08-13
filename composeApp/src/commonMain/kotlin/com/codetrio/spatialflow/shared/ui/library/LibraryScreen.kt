package com.codetrio.spatialflow.shared.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.shared.library.HistoryEntry
import com.codetrio.spatialflow.shared.library.LibraryRepository
import com.codetrio.spatialflow.shared.model.SongItem
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun LibraryScreen(songs: List<SongItem>, repository: LibraryRepository, onPlay: (SongItem) -> Unit) {
    val favourites by repository.favouriteSongIds.collectAsState()
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Library", style = MaterialTheme.typography.headlineMedium)
        Text("${songs.size} songs", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(songs, key = { it.id }) { song -> SongRow(song, song.id in favourites, onPlay) { scope.launch { repository.toggleFavourite(song.id) } } }
        }
    }
}

@Composable
fun HistoryScreen(repository: LibraryRepository, onPlay: (SongItem) -> Unit) {
    val history by repository.history.collectAsState()
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("History", style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.weight(1f))
            Text("Clear", modifier = Modifier.clickable { scope.launch { repository.clearHistory() } }, color = MaterialTheme.colorScheme.primary)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(history, key = { it.playedAt }) { SongRow(it.song, false, onPlay, null) } }
    }
}

@Composable private fun SongRow(song: SongItem, favourite: Boolean, onPlay: (SongItem) -> Unit, onFavourite: (() -> Unit)?) = Card(Modifier.fillMaxWidth().clickable { onPlay(song) }) {
    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        onFavourite?.let { IconButton(it) { Icon(if (favourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favourite") } }
    }
}
