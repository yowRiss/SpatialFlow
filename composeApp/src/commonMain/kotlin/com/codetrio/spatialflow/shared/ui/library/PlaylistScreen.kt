package com.codetrio.spatialflow.shared.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.shared.library.LibraryRepository
import com.codetrio.spatialflow.shared.library.Playlist
import com.codetrio.spatialflow.shared.model.SongItem
import kotlinx.coroutines.launch

@Composable
fun PlaylistScreen(repository: LibraryRepository, onPlay: (SongItem) -> Unit) {
    val playlists by repository.playlists.collectAsState()
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Playlist?>(null) }
    selected?.let { playlist ->
        PlaylistDetailScreen(playlist, repository, onBack = { selected = null }, onPlay = onPlay)
        return
    }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Playlists", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("New playlist") })
            TextButton({ scope.launch { repository.createPlaylist(name); name = "" } }) { Text("Create") }
        }
        playlists.forEach { playlist ->
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { selected = playlist }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(playlist.name, Modifier.fillMaxWidth())
                    IconButton({ scope.launch { repository.deletePlaylist(playlist.id) } }) { Icon(Icons.Default.Delete, "Delete") }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailScreen(playlist: Playlist, repository: LibraryRepository, onBack: () -> Unit, onPlay: (SongItem) -> Unit) {
    val scope = rememberCoroutineScope()
    var songs by remember(playlist.id) { mutableStateOf<List<SongItem>>(emptyList()) }
    LaunchedEffect(playlist.id) { songs = repository.songs(playlist.id) }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowBack, "Back", Modifier.clickable(onClick = onBack))
            Text(playlist.name, Modifier.padding(start = 16.dp).fillMaxWidth(), style = MaterialTheme.typography.headlineMedium)
        }
        if (songs.isEmpty()) Text("No songs yet. Add one from the player actions menu.", Modifier.padding(top = 24.dp))
        songs.forEach { song ->
            Card(Modifier.fillMaxWidth().padding(top = 8.dp).clickable { onPlay(song) }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.fillMaxWidth()) { Text(song.title); Text(song.artist, style = MaterialTheme.typography.bodySmall) }
                    IconButton({ scope.launch { repository.removeSong(playlist.id, song.id); songs = songs.filterNot { it.id == song.id } } }) { Icon(Icons.Default.Delete, "Remove from playlist") }
                }
            }
        }
    }
}
