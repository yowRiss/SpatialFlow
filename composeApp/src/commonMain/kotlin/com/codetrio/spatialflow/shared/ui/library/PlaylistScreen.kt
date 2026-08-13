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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.shared.library.LibraryRepository
import kotlinx.coroutines.launch

@Composable
fun PlaylistScreen(repository: LibraryRepository) {
    val playlists by repository.playlists.collectAsState()
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Playlists", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("New playlist") })
            TextButton({ scope.launch { repository.createPlaylist(name); name = "" } }) { Text("Create") }
        }
        playlists.forEach { playlist ->
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(playlist.name, Modifier.fillMaxWidth())
                    IconButton({ scope.launch { repository.deletePlaylist(playlist.id) } }) { Icon(Icons.Default.Delete, "Delete") }
                }
            }
        }
    }
}
