package com.codetrio.spatialflow.shared.ui.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.shared.data.innertube.MusicCatalog
import com.codetrio.spatialflow.shared.data.innertube.SearchItem
import com.codetrio.spatialflow.shared.model.SongItem

@Composable
fun ExploreScreen(catalog: MusicCatalog, onPlay: (SongItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchItem>>(emptyList()) }
    var message by remember { mutableStateOf("Search YouTube Music without an account.") }
    LaunchedEffect(query) {
        if (query.trim().length < 2) return@LaunchedEffect
        message = "Searching…"
        catalog.search(query).fold({ result -> results = result.items; message = if (result.items.isEmpty()) "No results" else "${result.items.size} results" }, { message = it.message ?: "Search failed" })
    }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Explore", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, label = { Text("Search songs, albums, artists") })
        Text(message, Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(results) { item -> ExploreItem(item, onPlay) } }
    }
}

@Composable private fun ExploreItem(item: SearchItem, onPlay: (SongItem) -> Unit) {
    val song = (item as? SearchItem.Song)?.song
    val title = when (item) { is SearchItem.Song -> item.song.title; is SearchItem.Album -> item.album.title; is SearchItem.Artist -> item.artist.title; is SearchItem.Playlist -> item.playlist.title }
    val subtitle = when (item) { is SearchItem.Song -> item.song.artist; is SearchItem.Album -> item.album.artists.joinToString { it.name }; is SearchItem.Artist -> item.artist.subscriberCount.orEmpty(); is SearchItem.Playlist -> item.playlist.author?.name.orEmpty() }
    Card(Modifier.fillMaxWidth().clickable(enabled = song != null) { song?.let { onPlay(SongItem.online(it.videoId, it.title, it.artist, null, it.durationMs, it.thumbnailUrl, it.artistId)) } }) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.padding(8.dp)); Column { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
}
