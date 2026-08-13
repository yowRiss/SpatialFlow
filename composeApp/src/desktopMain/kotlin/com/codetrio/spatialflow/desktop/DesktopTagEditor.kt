package com.codetrio.spatialflow.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.shared.model.SongItem
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

/** JVM replacement for Android's TagEditorFragment. Unsupported files report a
 * write error rather than silently claiming success. */
@Composable
fun DesktopTagEditor(song: SongItem?, onSaved: (String) -> Unit) {
    if (song?.path.isNullOrBlank()) { Column(Modifier.fillMaxSize().padding(24.dp)) { Text("Tag editor", style = MaterialTheme.typography.headlineMedium); Text("Play a local track to edit its tags.") }; return }
    var title by remember(song.id) { mutableStateOf(song.title) }
    var artist by remember(song.id) { mutableStateOf(song.artist) }
    var album by remember(song.id) { mutableStateOf("") }
    var message by remember(song.id) { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Edit tags", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title") })
        OutlinedTextField(artist, { artist = it }, Modifier.fillMaxWidth(), label = { Text("Artist") })
        OutlinedTextField(album, { album = it }, Modifier.fillMaxWidth(), label = { Text("Album") })
        Button(onClick = {
            message = runCatching {
                val audioFile = AudioFileIO.read(File(song.path!!))
                val tag = audioFile.tagOrCreateAndSetDefault
                tag.setField(FieldKey.TITLE, title); tag.setField(FieldKey.ARTIST, artist); tag.setField(FieldKey.ALBUM, album)
                AudioFileIO.write(audioFile); "Saved" 
            }.getOrElse { "Could not save tags: ${it.message}" }
            onSaved(message)
        }) { Text("Save tags") }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
