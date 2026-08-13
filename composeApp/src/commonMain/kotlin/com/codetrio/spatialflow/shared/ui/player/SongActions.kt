package com.codetrio.spatialflow.shared.ui.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.shared.library.LibraryRepository
import com.codetrio.spatialflow.shared.model.SongItem
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/** Compose replacement for Android's SongActionsBottomSheet, retaining queue,
 * favourite, and persistent playlist actions. */
@Composable
fun SongActionsDialog(song: SongItem, repository: LibraryRepository, onPlayNext: () -> Unit, onDismiss: () -> Unit) {
    val playlists by repository.playlists.collectAsState()
    val favourites by repository.favouriteSongIds.collectAsState()
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(song.title) },
        text = { Column {
            TextButton({ onPlayNext(); onDismiss() }) { Text("Play next") }
            TextButton({ scope.launch { repository.toggleFavourite(song.id) } }) { Text(if (song.id in favourites) "Remove from favourites" else "Add to favourites") }
            Text("Add to playlist")
            playlists.forEach { playlist -> TextButton({ scope.launch { repository.addSong(playlist.id, song) } }) { Text(playlist.name) } }
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("New playlist") })
        } },
        confirmButton = { Button(onClick = { scope.launch { val playlist = repository.createPlaylist(name); repository.addSong(playlist.id, song); name = "" } }) { Text("Create") } },
        dismissButton = { TextButton(onDismiss) { Text("Done") } },
    )
}
