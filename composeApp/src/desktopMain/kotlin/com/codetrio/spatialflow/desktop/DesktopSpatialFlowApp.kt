package com.codetrio.spatialflow.desktop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.shared.ui.SharedTheme
import java.awt.Desktop
import java.io.File
import java.util.prefs.Preferences
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.swing.JFileChooser
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey

private enum class DesktopDestination(val label: String) {
    Home("Home"), Library("Library"), Favourites("Favourites"), Queue("Queue"), Settings("Settings")
}

private data class DesktopTrack(
    val file: File,
    val title: String,
    val artist: String,
    val album: String?,
    val durationSeconds: Int?,
) {
    val id: String get() = file.absolutePath
}

private data class DesktopUiState(
    val destination: DesktopDestination = DesktopDestination.Home,
    val libraryRoot: File? = null,
    val library: List<DesktopTrack> = emptyList(),
    val queue: List<DesktopTrack> = emptyList(),
    val currentTrack: DesktopTrack? = null,
    val isPlaying: Boolean = false,
    val positionSeconds: Float = 0f,
    val favourites: Set<String> = emptySet(),
    val history: List<DesktopTrack> = emptyList(),
    val notice: String? = null,
)

/** Desktop-only app state and playback bridge. This deliberately keeps file-system and Java Sound APIs
 * out of commonMain, so Android can keep using its Media3 implementation during the migration. */
private class DesktopPlayerViewModel {
    private val preferences = Preferences.userRoot().node("com/codetrio/spatialflow")
    private var clip: Clip? = null
    private var selectedIndex = -1
    var state by mutableStateOf(loadInitialState())
        private set

    fun chooseLibrary() {
        val chooser = JFileChooser().apply {
            dialogTitle = "Choose your music folder"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) loadLibrary(chooser.selectedFile)
    }

    fun loadLibrary(root: File) {
        val tracks = root.walkTopDown()
            .onEnter { !it.isHidden }
            .filter { it.isFile && it.extension.lowercase() in audioExtensions }
            .map(::readDesktopTrack)
            .sortedBy { it.title.lowercase() }
            .toList()
        preferences.put("libraryRoot", root.absolutePath)
        state = state.copy(
            destination = DesktopDestination.Library,
            libraryRoot = root,
            library = tracks,
            queue = if (state.queue.isEmpty()) tracks else state.queue,
            notice = if (tracks.isEmpty()) "No supported audio files found in ${root.name}." else "Found ${tracks.size} tracks in ${root.name}.",
        )
    }

    fun selectDestination(destination: DesktopDestination) {
        state = state.copy(destination = destination, notice = null)
    }

    fun play(track: DesktopTrack, replaceQueue: Boolean = false) {
        val queue = if (replaceQueue || state.queue.isEmpty()) state.library else state.queue
        selectedIndex = queue.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: 0
        playTrack(track, queue)
    }

    fun togglePlayback() {
        val current = state.currentTrack ?: state.queue.firstOrNull() ?: state.library.firstOrNull() ?: return
        val activeClip = clip
        if (activeClip != null) {
            if (activeClip.isRunning) {
                activeClip.stop()
                state = state.copy(isPlaying = false)
            } else {
                activeClip.start()
                state = state.copy(isPlaying = true)
            }
        } else {
            play(current)
        }
    }

    fun next() = moveBy(1)
    fun previous() = moveBy(-1)

    fun seekTo(seconds: Float) {
        clip?.let {
            it.microsecondPosition = (seconds * 1_000_000).toLong().coerceAtMost(it.microsecondLength)
            state = state.copy(positionSeconds = seconds)
        }
    }

    fun refreshPosition() {
        val activeClip = clip ?: return
        val position = activeClip.microsecondPosition / 1_000_000f
        if (!activeClip.isRunning && state.isPlaying && activeClip.microsecondPosition >= activeClip.microsecondLength) {
            next()
        } else if (position != state.positionSeconds) {
            state = state.copy(positionSeconds = position)
        }
    }

    fun toggleFavourite(track: DesktopTrack) {
        val favourites = state.favourites.toMutableSet()
        if (!favourites.add(track.id)) favourites.remove(track.id)
        preferences.put("favourites", favourites.joinToString("\n"))
        state = state.copy(favourites = favourites)
    }

    fun addToQueue(track: DesktopTrack) {
        if (state.queue.none { it.id == track.id }) state = state.copy(queue = state.queue + track, notice = "Added ${track.title} to the queue.")
    }

    fun close() { clip?.close() }

    private fun moveBy(offset: Int) {
        val queue = state.queue.ifEmpty { state.library }
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.id == state.currentTrack?.id }.takeIf { it >= 0 } ?: selectedIndex.coerceAtLeast(0)
        selectedIndex = (currentIndex + offset).floorMod(queue.size)
        playTrack(queue[selectedIndex], queue)
    }

    private fun playTrack(track: DesktopTrack, queue: List<DesktopTrack>) {
        clip?.stop()
        clip?.close()
        clip = try {
            AudioSystem.getAudioInputStream(track.file).use { stream ->
                AudioSystem.getClip().also { newClip -> newClip.open(stream); newClip.start() }
            }
        } catch (_: Exception) {
            // Java Sound's available codecs vary by operating system. Still let users open the item
            // in their configured desktop audio player instead of making the library unusable.
            runCatching { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(track.file) }
            null
        }
        state = state.copy(
            queue = queue,
            currentTrack = track,
            isPlaying = clip?.isRunning ?: false,
            positionSeconds = 0f,
            history = (listOf(track) + state.history.filterNot { it.id == track.id }).take(50),
            notice = if (clip == null) "Opened in your system audio player; install Java Sound codecs for in-app controls." else null,
        )
    }

    private fun loadInitialState(): DesktopUiState {
        val root = preferences.get("libraryRoot", null)?.let(::File)?.takeIf(File::isDirectory)
        val favourites = preferences.get("favourites", "").lineSequence().filter(String::isNotBlank).toSet()
        val tracks = root?.walkTopDown()
            ?.onEnter { !it.isHidden }
            ?.filter { it.isFile && it.extension.lowercase() in audioExtensions }
            ?.map(::readDesktopTrack)
            ?.sortedBy { it.title.lowercase() }
            ?.toList()
            .orEmpty()
        return DesktopUiState(libraryRoot = root, library = tracks, queue = tracks, favourites = favourites)
    }
}

private val audioExtensions = setOf("mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "aiff", "aif")

private fun readDesktopTrack(file: File): DesktopTrack {
    val fallback = file.nameWithoutExtension
    val fields = runCatching {
        AudioFileIO.read(file).tag?.let { tag ->
            Triple(tag.getFirst(FieldKey.TITLE), tag.getFirst(FieldKey.ARTIST), tag.getFirst(FieldKey.ALBUM))
        }
    }.getOrNull()
    val (title, artist, album) = fields ?: Triple("", "", "")
    val splitName = fallback.split(" - ", limit = 2)
    return DesktopTrack(
        file = file,
        title = title.takeUnless(String::isBlank) ?: splitName.last(),
        artist = artist.takeUnless(String::isBlank) ?: splitName.getOrNull(0)?.takeIf { splitName.size > 1 } ?: "Unknown artist",
        album = album.takeUnless(String::isBlank),
        durationSeconds = audioDurationSeconds(file),
    )
}

private fun audioDurationSeconds(file: File): Int? = runCatching {
    AudioSystem.getAudioFileFormat(file).properties()["duration"]?.let { (it as Long / 1_000_000L).toInt() }
}.getOrNull()

private fun Int.floorMod(modulus: Int) = ((this % modulus) + modulus) % modulus

@Composable
fun DesktopSpatialFlowApp() = SharedTheme {
    val viewModel = remember { DesktopPlayerViewModel() }
    val state = viewModel.state
    DisposableEffect(Unit) { onDispose(viewModel::close) }
    LaunchedEffect(state.isPlaying) {
        while (state.isPlaying) {
            kotlinx.coroutines.delay(500)
            viewModel.refreshPosition()
        }
    }

    Row(Modifier.fillMaxSize()) {
        DesktopNavigationRail(state.destination, viewModel::selectDestination)
        Scaffold(
            modifier = Modifier.weight(1f),
            bottomBar = { NowPlayingBar(state, viewModel) },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                state.notice?.let { Notice(it) }
                when (state.destination) {
                    DesktopDestination.Home -> HomeScreen(state, viewModel)
                    DesktopDestination.Library -> LibraryScreen(state, viewModel)
                    DesktopDestination.Favourites -> FavouritesScreen(state, viewModel)
                    DesktopDestination.Queue -> QueueScreen(state, viewModel)
                    DesktopDestination.Settings -> DesktopSettingsScreen(state, viewModel)
                }
            }
        }
    }
}

@Composable
private fun DesktopNavigationRail(selected: DesktopDestination, onSelect: (DesktopDestination) -> Unit) = NavigationRail {
    Spacer(Modifier.height(16.dp))
    DesktopDestination.entries.forEach { destination ->
        val icon = when (destination) {
            DesktopDestination.Home -> Icons.Outlined.Home
            DesktopDestination.Library -> Icons.Outlined.LibraryMusic
            DesktopDestination.Favourites -> Icons.Outlined.Favorite
            DesktopDestination.Queue -> Icons.Outlined.QueueMusic
            DesktopDestination.Settings -> Icons.Outlined.Settings
        }
        NavigationRailItem(selected == destination, { onSelect(destination) }, { Icon(icon, destination.label) }, label = { Text(destination.label) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(state: DesktopUiState, viewModel: DesktopPlayerViewModel) = Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
    Text("Good listening", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
    Text(if (state.library.isEmpty()) "Choose a folder to build your local library." else "${state.library.size} tracks ready in ${state.libraryRoot?.name}.", style = MaterialTheme.typography.bodyLarge)
    Button(onClick = viewModel::chooseLibrary) { Icon(Icons.Outlined.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Choose music folder") }
    if (state.history.isNotEmpty()) {
        Text("Recently played", style = MaterialTheme.typography.titleLarge)
        TrackList(state.history.take(8), state, viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScreen(state: DesktopUiState, viewModel: DesktopPlayerViewModel) {
    var query by remember { mutableStateOf("") }
    val visibleTracks = remember(state.library, query) { state.library.filter { it.title.contains(query, true) || it.file.parentFile.name.contains(query, true) } }
    Column(Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(title = { Text("Library") }, actions = { TextButton(viewModel::chooseLibrary) { Icon(Icons.Outlined.FolderOpen, null); Text(" Change folder") } })
        Column(Modifier.padding(horizontal = 32.dp).fillMaxSize()) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Outlined.Search, null) }, label = { Text("Search your library") })
            Spacer(Modifier.height(12.dp))
            if (state.library.isEmpty()) EmptyLibrary(viewModel) else TrackList(visibleTracks, state, viewModel)
        }
    }
}

@Composable
private fun FavouritesScreen(state: DesktopUiState, viewModel: DesktopPlayerViewModel) = Column(Modifier.fillMaxSize().padding(32.dp)) {
    Text("Favourites", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(16.dp))
    val favourites = state.library.filter { it.id in state.favourites }
    if (favourites.isEmpty()) Text("Mark tracks with the heart to keep them here.") else TrackList(favourites, state, viewModel)
}

@Composable
private fun QueueScreen(state: DesktopUiState, viewModel: DesktopPlayerViewModel) = Column(Modifier.fillMaxSize().padding(32.dp)) {
    Text("Up next", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(16.dp))
    if (state.queue.isEmpty()) Text("Your queue is empty. Play a track from the library to start one.") else TrackList(state.queue, state, viewModel)
}

@Composable
private fun DesktopSettingsScreen(state: DesktopUiState, viewModel: DesktopPlayerViewModel) = Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text("Desktop settings", style = MaterialTheme.typography.headlineMedium)
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) {
        Text("Music folder", style = MaterialTheme.typography.titleMedium)
        Text(state.libraryRoot?.absolutePath ?: "No folder selected", style = MaterialTheme.typography.bodyMedium)
        TextButton(viewModel::chooseLibrary) { Text("Change music folder") }
    } }
    Text("Playback uses the desktop’s Java Sound codecs. WAV/AIFF work out of the box; unsupported formats open in the configured system player.", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun EmptyLibrary(viewModel: DesktopPlayerViewModel) = Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
    Icon(Icons.Outlined.LibraryMusic, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(12.dp)); Text("No music in your library yet", style = MaterialTheme.typography.titleLarge)
    TextButton(viewModel::chooseLibrary) { Text("Choose a folder") }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackList(tracks: List<DesktopTrack>, state: DesktopUiState, viewModel: DesktopPlayerViewModel) {
    val listState = rememberLazyListState()
    LazyColumn(Modifier.fillMaxSize(), state = listState) {
        items(tracks, key = DesktopTrack::id) { track ->
            TrackRow(track, state, viewModel, Modifier.animateItem())
            HorizontalDivider()
        }
    }
}

@Composable
private fun TrackRow(track: DesktopTrack, state: DesktopUiState, viewModel: DesktopPlayerViewModel, modifier: Modifier = Modifier) = Row(
    modifier.fillMaxWidth().clickable { viewModel.play(track) }.padding(vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Surface(Modifier.size(46.dp), shape = RoundedCornerShape(10.dp), color = if (state.currentTrack?.id == track.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest) {
        Box(contentAlignment = Alignment.Center) { Icon(if (state.currentTrack?.id == track.id && state.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, track.title) }
    }
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)) {
        Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (state.currentTrack?.id == track.id) MaterialTheme.colorScheme.primary else Color.Unspecified)
        Text("${track.artist} · ${track.album ?: track.file.parentFile.name} · ${track.durationSeconds?.let(::formatTime) ?: "Unknown duration"}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    IconButton({ viewModel.addToQueue(track) }) { Icon(Icons.Outlined.QueueMusic, "Add to queue") }
    IconToggleButton(track.id in state.favourites, { viewModel.toggleFavourite(track) }) { Icon(if (track.id in state.favourites) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, "Favourite") }
}

@Composable
private fun NowPlayingBar(state: DesktopUiState, viewModel: DesktopPlayerViewModel) {
    val track = state.currentTrack ?: return
    val duration = (track.durationSeconds ?: 0).toFloat().coerceAtLeast(1f)
    Surface(Modifier.fillMaxWidth(), tonalElevation = 4.dp) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) {
            Slider(value = state.positionSeconds.coerceIn(0f, duration), onValueChange = viewModel::seekTo, valueRange = 0f..duration)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(42.dp), CircleShape, color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.LibraryMusic, null) } }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${formatTime(state.positionSeconds.toInt())} / ${track.durationSeconds?.let(::formatTime) ?: "--:--"}", style = MaterialTheme.typography.bodySmall) }
                IconButton(viewModel::previous) { Icon(Icons.Outlined.SkipPrevious, "Previous") }
                IconButton(viewModel::togglePlayback) { Icon(if (state.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (state.isPlaying) "Pause" else "Play", Modifier.size(30.dp)) }
                IconButton(viewModel::next) { Icon(Icons.Outlined.SkipNext, "Next") }
            }
        }
    }
}

@Composable
private fun Notice(message: String) = Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer) { Text(message, Modifier.padding(horizontal = 24.dp, vertical = 10.dp), color = MaterialTheme.colorScheme.onSecondaryContainer) }

private fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
