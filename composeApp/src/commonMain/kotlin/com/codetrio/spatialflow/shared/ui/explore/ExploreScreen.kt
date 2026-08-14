package com.codetrio.spatialflow.shared.ui.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.codetrio.spatialflow.shared.data.innertube.OnlineSong
import com.codetrio.spatialflow.shared.model.SongItem
import com.codetrio.spatialflow.shared.library.LibraryRepository
import com.codetrio.spatialflow.shared.library.Playlist
import com.codetrio.spatialflow.shared.ui.components.ArtworkImage
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun ExploreScreen(catalog: MusicCatalog, onPlay: (SongItem) -> Unit, repository: LibraryRepository? = null) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchItem>>(emptyList()) }
    var message by remember { mutableStateOf("Search YouTube Music without an account.") }
    var selected by remember { mutableStateOf<SearchItem?>(null) }
    LaunchedEffect(query) {
        if (query.trim().length < 2) return@LaunchedEffect
        message = "Searching…"
        catalog.search(query).fold({ result -> results = result.items; message = if (result.items.isEmpty()) "No results" else "${result.items.size} results" }, { message = it.message ?: "Search failed" })
    }
    val detail = selected
    if (detail != null) {
        ExploreDetailScreen(catalog, detail, repository, onBack = { selected = null }, onPlay = onPlay)
        return
    }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Explore", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, label = { Text("Search songs, albums, artists") })
        Text(message, Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(results) { item -> ExploreItem(item, onPlay) { selected = item } } }
    }
}

@Composable private fun ExploreItem(item: SearchItem, onPlay: (SongItem) -> Unit, onOpen: () -> Unit) {
    val song = (item as? SearchItem.Song)?.song
    val title = when (item) { is SearchItem.Song -> item.song.title; is SearchItem.Album -> item.album.title; is SearchItem.Artist -> item.artist.title; is SearchItem.Playlist -> item.playlist.title }
    val subtitle = when (item) { is SearchItem.Song -> item.song.artist; is SearchItem.Album -> item.album.artists.joinToString { it.name }; is SearchItem.Artist -> item.artist.subscriberCount.orEmpty(); is SearchItem.Playlist -> item.playlist.author?.name.orEmpty() }
    val artwork = when (item) { is SearchItem.Song -> item.song.thumbnailUrl; is SearchItem.Album -> item.album.thumbnailUrl; is SearchItem.Artist -> item.artist.thumbnailUrl; is SearchItem.Playlist -> item.playlist.thumbnailUrl }
    Card(Modifier.fillMaxWidth().clickable { song?.let { onPlay(it.asSongItem()) } ?: onOpen() }) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { ArtworkImage(artwork, title, Modifier.size(48.dp)); Spacer(Modifier.padding(8.dp)); Column(Modifier.fillMaxWidth()) { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
}

@Composable
private fun ExploreDetailScreen(catalog: MusicCatalog, item: SearchItem, repository: LibraryRepository?, onBack: () -> Unit, onPlay: (SongItem) -> Unit) {
    var songs by remember(item) { mutableStateOf<List<OnlineSong>>(emptyList()) }
    var status by remember(item) { mutableStateOf("Loading…") }
    var selectingPlaylist by remember(item) { mutableStateOf(false) }
    val emptyPlaylists = remember { MutableStateFlow(emptyList<Playlist>()) }
    val playlists by (repository?.playlists ?: emptyPlaylists).collectAsState()
    val scope = rememberCoroutineScope()
    val title = when (item) { is SearchItem.Album -> item.album.title; is SearchItem.Artist -> item.artist.title; is SearchItem.Playlist -> item.playlist.title; is SearchItem.Song -> item.song.title }
    val artwork = when (item) { is SearchItem.Album -> item.album.thumbnailUrl; is SearchItem.Artist -> item.artist.thumbnailUrl; is SearchItem.Playlist -> item.playlist.thumbnailUrl; is SearchItem.Song -> item.song.thumbnailUrl }
    LaunchedEffect(item) {
        val result = when (item) {
            is SearchItem.Album -> catalog.album(item.album.browseId).map { it.songs }
            is SearchItem.Artist -> catalog.artist(item.artist.browseId).map { page -> page.sections.flatMap { section -> section.items.mapNotNull { (it as? SearchItem.Song)?.song } } }
            is SearchItem.Playlist -> catalog.playlist(item.playlist.playlistId).map { it.songs }
            is SearchItem.Song -> Result.success(listOf(item.song))
        }
        result.fold({ loaded -> songs = loaded; status = if (loaded.isEmpty()) "No playable tracks found." else "${loaded.size} tracks" }, { status = it.message ?: "Could not load this page." })
    }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ArrowBack, "Back", Modifier.clickable(onClick = onBack)); ArtworkImage(artwork, title, Modifier.padding(start = 12.dp).size(64.dp)); Text(title, Modifier.padding(start = 14.dp), style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        Text(status, Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (repository != null && songs.isNotEmpty()) {
            TextButton({ selectingPlaylist = !selectingPlaylist }) { Text(if (item is SearchItem.Playlist) "Import into playlist" else "Save tracks to playlist") }
            if (selectingPlaylist) {
                if (playlists.isEmpty()) Text("Create a playlist first from the Playlists section.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                playlists.forEach { playlist ->
                    TextButton({
                        scope.launch {
                            songs.forEachIndexed { index, song -> repository.addSong(playlist.id, song.asSongItem(), index) }
                            status = "Saved ${songs.size} tracks to ${playlist.name}."
                            selectingPlaylist = false
                        }
                    }) { Text(playlist.name) }
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(songs, key = OnlineSong::videoId) { song -> ExploreSongRow(song) { onPlay(song.asSongItem()) } } }
    }
}

@Composable
private fun ExploreSongRow(song: OnlineSong, onPlay: () -> Unit) = Card(Modifier.fillMaxWidth().clickable(onClick = onPlay)) {
    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        ArtworkImage(song.thumbnailUrl, song.title, Modifier.size(44.dp))
        Column(Modifier.padding(start = 12.dp).fillMaxWidth()) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun OnlineSong.asSongItem() = SongItem.online(videoId, title, artist, null, durationMs, thumbnailUrl, artistId)
