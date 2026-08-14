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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
fun PlaylistScreen(repository: LibraryRepository, onPlayQueue: (List<SongItem>, Int) -> Unit) {
    val playlists by repository.playlists.collectAsState()
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Playlist?>(null) }
    selected?.let { playlist ->
        PlaylistDetailScreen(playlist, repository, onBack = { selected = null }, onPlayQueue = onPlayQueue)
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
private fun PlaylistDetailScreen(playlist: Playlist, repository: LibraryRepository, onBack: () -> Unit, onPlayQueue: (List<SongItem>, Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var songs by remember(playlist.id) { mutableStateOf<List<SongItem>>(emptyList()) }
    var renameDraft by remember(playlist.id) { mutableStateOf(playlist.name) }
    var showRename by remember(playlist.id) { mutableStateOf(false) }
    LaunchedEffect(playlist.id) { songs = repository.songs(playlist.id) }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowBack, "Back", Modifier.clickable(onClick = onBack))
            Text(playlist.name, Modifier.padding(start = 16.dp).weight(1f), style = MaterialTheme.typography.headlineMedium)
            IconButton({ showRename = true }) { Icon(Icons.Default.Edit, "Rename playlist") }
        }
        if (songs.isEmpty()) Text("No songs yet. Add one from the player actions menu.", Modifier.padding(top = 24.dp))
        songs.forEachIndexed { index, song ->
            Card(Modifier.fillMaxWidth().padding(top = 8.dp).clickable { onPlayQueue(songs, index) }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.fillMaxWidth()) { Text(song.title); Text(song.artist, style = MaterialTheme.typography.bodySmall) }
                    IconButton({ scope.launch { repository.removeSong(playlist.id, song.id); songs = songs.filterNot { it.id == song.id } } }) { Icon(Icons.Default.Delete, "Remove from playlist") }
                }
            }
        }
    }
    if (showRename) AlertDialog(
        onDismissRequest = { showRename = false },
        title = { Text("Rename playlist") },
        text = { OutlinedTextField(renameDraft, { renameDraft = it }, label = { Text("Playlist name") }) },
        confirmButton = { Button(onClick = {
            scope.launch { repository.renamePlaylist(playlist.id, renameDraft.trim()) }
            showRename = false
        }, enabled = renameDraft.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton({ showRename = false }) { Text("Cancel") } },
    )
}
