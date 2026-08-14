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
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlaylistPlay
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.shared.ui.SpatialFlowTheme
import com.codetrio.spatialflow.shared.library.LibraryRepository
import com.codetrio.spatialflow.shared.library.LocalMusicLibrary
import com.codetrio.spatialflow.shared.model.SongItem
import com.codetrio.spatialflow.shared.player.PlaybackController
import com.codetrio.spatialflow.shared.player.PlayerCommand
import com.codetrio.spatialflow.shared.player.PlayerUiState
import com.codetrio.spatialflow.shared.data.innertube.MusicCatalog
import com.codetrio.spatialflow.shared.ui.explore.ExploreScreen
import com.codetrio.spatialflow.shared.ui.player.EffectsScreen
import com.codetrio.spatialflow.shared.ui.SpatialFlowApp
import com.codetrio.spatialflow.shared.ui.custom.AnimatedMeshGradient
import com.codetrio.spatialflow.shared.ui.library.PlaylistScreen
import com.codetrio.spatialflow.shared.ui.library.HistoryScreen
import com.codetrio.spatialflow.shared.ui.explore.AccountScreen
import com.codetrio.spatialflow.shared.account.GoogleAuthClient
import com.codetrio.spatialflow.shared.viewmodel.SettingsViewModel
import com.codetrio.spatialflow.shared.ui.player.FullPlayer
import com.codetrio.spatialflow.shared.ui.player.MiniPlayer
import com.codetrio.spatialflow.shared.ui.player.QueueDrawer
import com.codetrio.spatialflow.shared.ui.player.SyncedLyrics
import com.codetrio.spatialflow.shared.ui.player.SongActionsDialog
import com.codetrio.spatialflow.shared.data.lyrics.LyricsCatalog
import com.codetrio.spatialflow.shared.data.lyrics.LyricLine
import com.codetrio.spatialflow.shared.data.lyrics.SharedLrcParser
import com.codetrio.spatialflow.shared.data.lyrics.TrackMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import org.koin.core.context.GlobalContext
import java.io.File
import java.util.prefs.Preferences
import javax.sound.sampled.AudioSystem
import javax.swing.JFileChooser
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey

private enum class DesktopDestination(val label: String) {
    Home("Home"), Explore("Explore"), Account("Account"), Library("Library"), History("History"), Playlists("Playlists"), Favourites("Favourites"), Queue("Queue"), Effects("Effects"), Tags("Tags"), Settings("Settings")
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
    val favourites: Set<Long> = emptySet(),
    val history: List<DesktopTrack> = emptyList(),
    val notice: String? = null,
)

/** Desktop-only app state and playback bridge. File-system and libVLC details
 * remain outside commonMain, so Android can retain its Media3 implementation. */
private class DesktopPlayerViewModel {
    private val libraryRepository: LibraryRepository = GlobalContext.get().get()
    private val localMusicLibrary: LocalMusicLibrary = GlobalContext.get().get()
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferences = Preferences.userRoot().node("com/codetrio/spatialflow")
    private val playbackController: PlaybackController = GlobalContext.get().get()
    private val musicCatalog: MusicCatalog = GlobalContext.get().get()
    private val lyricsCatalog: LyricsCatalog = GlobalContext.get().get()
    private val authClient: GoogleAuthClient = GlobalContext.get().get()
    private val settingsViewModel: SettingsViewModel = GlobalContext.get().get()
    private val songDownloader = DesktopSongDownloader()
    private var streamingQueue: List<SongItem> = emptyList()
    private var selectedIndex = -1
    var state by mutableStateOf(loadInitialState())
        private set

    init {
        persistenceScope.launch {
            val legacyFavourites = preferences.get("favourites", "")
                .lineSequence().filter(String::isNotBlank).map { it.hashCode().toLong() }.toSet()
            legacyFavourites.forEach { legacyId ->
                if (legacyId !in libraryRepository.favouriteSongIds.value) libraryRepository.toggleFavourite(legacyId)
            }
            if (legacyFavourites.isNotEmpty()) preferences.remove("favourites")
            libraryRepository.favouriteSongIds.collect { favourites ->
                state = state.copy(favourites = favourites)
            }
        }
    }

    fun chooseLibrary() {
        val chooser = JFileChooser().apply {
            dialogTitle = "Choose your music folder"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) loadLibrary(chooser.selectedFile)
    }

    fun loadLibrary(root: File) {
        preferences.put("libraryRoot", root.absolutePath)
        state = state.copy(destination = DesktopDestination.Library, libraryRoot = root, notice = "Scanning ${root.name}…")
        persistenceScope.launch {
            localMusicLibrary.scan(root.absolutePath).onSuccess { songs ->
                val tracks = songs.mapNotNull { song -> song.path?.let(::File)?.takeIf(File::isFile)?.let { file ->
                    DesktopTrack(file, song.title, song.artist, null, (song.duration / 1_000).toInt())
                } }
                state = state.copy(
                    library = tracks,
                    queue = if (state.queue.isEmpty()) tracks else state.queue,
                    notice = if (tracks.isEmpty()) "No supported audio files found in ${root.name}." else "Found ${tracks.size} tracks in ${root.name}.",
                )
            }.onFailure { error -> state = state.copy(notice = "Could not scan ${root.name}: ${error.message}") }
        }
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
        if (playbackController.state.value.currentSong == null) play(current) else playbackController.dispatch(PlayerCommand.TogglePlayback)
        syncPlaybackState(current)
    }

    fun next() = moveBy(1)
    fun previous() = moveBy(-1)

    fun seekTo(seconds: Float) {
        playbackController.dispatch(PlayerCommand.SeekTo((seconds * 1_000L).toLong()))
        state = state.copy(positionSeconds = seconds)
    }

    fun refreshPosition() {
        syncPlaybackState(state.currentTrack)
    }

    fun toggleFavourite(track: DesktopTrack) {
        persistenceScope.launch { libraryRepository.toggleFavourite(track.id.hashCode().toLong()) }
    }

    fun addToQueue(track: DesktopTrack) {
        if (state.queue.none { it.id == track.id }) state = state.copy(queue = state.queue + track, notice = "Added ${track.title} to the queue.")
    }

    fun close() { playbackController.release() }
    fun playerState(): PlayerUiState = playbackController.state.value
    fun playerQueue(): List<SongItem> = streamingQueue.ifEmpty { state.queue.map(::asSong) }
    fun controller(): PlaybackController = playbackController
    fun repository(): LibraryRepository = libraryRepository
    fun auth(): GoogleAuthClient = authClient
    fun settings(): SettingsViewModel = settingsViewModel
    fun catalog(): MusicCatalog = musicCatalog
    fun playOnline(song: SongItem) {
        val videoId = song.videoId ?: run {
            state = state.copy(notice = "This stream is missing its YouTube Music video id.")
            return
        }
        state = state.copy(notice = "Resolving ${song.title}…")
        persistenceScope.launch {
            val player = musicCatalog.player(videoId).getOrElse { error ->
                state = state.copy(notice = "Could not resolve ${song.title}: ${error.message ?: "network error"}")
                return@launch
            }
            val streamUrl = player.playbackUrl
                ?: player.streams.maxByOrNull { it.bitrate }?.url
                ?: run {
                    state = state.copy(notice = "No playable audio stream was returned for ${song.title}.")
                    return@launch
                }
            val playable = song.copy(
                path = streamUrl,
                data = streamUrl,
                duration = player.durationMs.takeIf { it > 0 } ?: song.duration,
                thumbnailUrl = player.thumbnailUrl ?: song.thumbnailUrl,
            )
            streamingQueue = listOf(playable)
            playbackController.setQueue(streamingQueue)
            playbackController.dispatch(PlayerCommand.PlayAt(0))
            state = state.copy(notice = null)
        }
    }
    fun playShared(song: SongItem) {
        if (song.path?.startsWith("http") == true) {
            playOnline(song)
            return
        }
        val file = song.path?.let(::File)?.takeIf(File::isFile) ?: run {
            state = state.copy(notice = "The saved audio file for ${song.title} is no longer available.")
            return
        }
        playTrack(DesktopTrack(file, song.title, song.artist, null, (song.duration / 1_000).toInt()), state.queue.ifEmpty { state.library })
    }
    fun download(song: SongItem) {
        state = state.copy(notice = "Downloading ${song.title}…")
        persistenceScope.launch {
            songDownloader.download(song).onSuccess { file ->
                state = state.copy(notice = "Downloaded ${song.title} to ${file.absolutePath}")
            }.onFailure { error ->
                state = state.copy(notice = "Could not download ${song.title}: ${error.message ?: "network error"}")
            }
        }
    }
    suspend fun lyricsForCurrentSong(): List<LyricLine> {
        val song = playbackController.state.value.currentSong ?: return emptyList()
        DesktopEmbeddedLyrics.read(song).takeIf { it.isNotEmpty() }?.let { return it }
        val metadata = TrackMetadata(song.title, song.artist, song.title, song.artist, durationMs = song.duration, filePath = song.path.orEmpty(), videoId = song.videoId)
        return lyricsCatalog.fetch(metadata).getOrNull()?.let { result ->
            SharedLrcParser.parse(result.syncedLyrics).ifEmpty { result.plainLyrics?.lines()?.filter(String::isNotBlank)?.mapIndexed { index, value -> LyricLine(index * 4_000L, value) }.orEmpty() }
        }.orEmpty()
    }

    private fun moveBy(offset: Int) {
        val queue = state.queue.ifEmpty { state.library }
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.id == state.currentTrack?.id }.takeIf { it >= 0 } ?: selectedIndex.coerceAtLeast(0)
        selectedIndex = (currentIndex + offset).floorMod(queue.size)
        playbackController.dispatch(if (offset > 0) PlayerCommand.Next else PlayerCommand.Previous)
        syncPlaybackState(queue[selectedIndex])
    }

    private fun playTrack(track: DesktopTrack, queue: List<DesktopTrack>) {
        val songs = queue.map(::asSong)
        val index = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playbackController.setQueue(songs, index)
        playbackController.dispatch(PlayerCommand.PlayAt(index))
        state = state.copy(
            queue = queue,
            currentTrack = track,
            isPlaying = true,
            positionSeconds = 0f,
            history = (listOf(track) + state.history.filterNot { it.id == track.id }).take(50),
            notice = null,
        )
        persistenceScope.launch {
            libraryRepository.recordHistory(
                SongItem.local(track.id.hashCode().toLong(), track.title, track.artist, -1, track.file.absolutePath, (track.durationSeconds ?: 0) * 1_000L, track.file.lastModified()),
                System.currentTimeMillis(),
            )
        }
    }

    private fun syncPlaybackState(fallback: DesktopTrack?) {
        val playback = playbackController.state.value
        val matchingTrack = playback.currentSong?.path?.let { path -> state.queue.firstOrNull { it.file.absolutePath == path } } ?: fallback
        state = state.copy(
            currentTrack = matchingTrack,
            isPlaying = playback.isPlaying,
            positionSeconds = playback.currentSong?.let { playback.positionMs / 1_000f } ?: state.positionSeconds,
            notice = if (playback.isProcessing && playback.currentSong != null) "Loading ${playback.currentSong.title}…" else state.notice,
        )
    }

    private fun asSong(track: DesktopTrack) = SongItem.local(
        id = track.id.hashCode().toLong(), rawTitle = track.title, rawArtist = track.artist,
        albumId = -1, path = track.file.absolutePath, duration = (track.durationSeconds ?: 0) * 1_000L,
        dateAdded = track.file.lastModified(),
    )

    private fun loadInitialState(): DesktopUiState {
        val root = preferences.get("libraryRoot", null)?.let(::File)?.takeIf(File::isDirectory)
        val favourites = preferences.get("favourites", "").lineSequence()
            .filter(String::isNotBlank).map { it.hashCode().toLong() }.toSet()
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
fun DesktopSpatialFlowApp() {
    val viewModel = remember { DesktopPlayerViewModel() }
    val settings by viewModel.settings().uiState.collectAsState()
    SpatialFlowTheme(
        amoledBlack = settings.amoledBlack,
        usePlatformDynamicColor = settings.dynamicAlbumTheme,
    ) {
    val state = viewModel.state
    var playerSurface by remember { mutableStateOf(DesktopPlayerSurface.None) }
    var lyricLines by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var showSongActions by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) { onDispose(viewModel::close) }
    LaunchedEffect(state.isPlaying) {
        while (state.isPlaying) {
            kotlinx.coroutines.delay(500)
            viewModel.refreshPosition()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedMeshGradient()
        Row(Modifier.fillMaxSize()) {
            DesktopNavigationRail(state.destination, viewModel::selectDestination)
            Scaffold(
            modifier = Modifier.weight(1f),
            bottomBar = { MiniPlayer(viewModel.playerState(), viewModel.controller()) { playerSurface = DesktopPlayerSurface.Player } },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                state.notice?.let { Notice(it) }
                when (state.destination) {
                    DesktopDestination.Home -> HomeScreen(state, viewModel)
                    DesktopDestination.Explore -> ExploreScreen(viewModel.catalog(), viewModel::playOnline)
                    DesktopDestination.Account -> AccountScreen(viewModel.auth())
                    DesktopDestination.Library -> LibraryScreen(state, viewModel)
                    DesktopDestination.History -> HistoryScreen(viewModel.repository(), viewModel::playShared)
                    DesktopDestination.Playlists -> PlaylistScreen(viewModel.repository())
                    DesktopDestination.Favourites -> FavouritesScreen(state, viewModel)
                    DesktopDestination.Queue -> QueueScreen(state, viewModel)
                    DesktopDestination.Effects -> EffectsScreen(viewModel.controller())
                    DesktopDestination.Tags -> DesktopTagEditor(viewModel.playerState().currentSong) { message -> viewModel.selectDestination(DesktopDestination.Tags) }
                    DesktopDestination.Settings -> SpatialFlowApp(viewModel.settings())
                }
                }
            }
        }
    }
    when (playerSurface) {
        DesktopPlayerSurface.Player -> FullPlayer(viewModel.playerState(), viewModel.playerQueue(), viewModel.controller(), lyricLines, { playerSurface = DesktopPlayerSurface.None }, { scope.launch { lyricLines = viewModel.lyricsForCurrentSong(); playerSurface = DesktopPlayerSurface.Lyrics } }, { playerSurface = DesktopPlayerSurface.Queue }, { showSongActions = true })
        DesktopPlayerSurface.Queue -> QueueDrawer(viewModel.playerQueue(), viewModel.playerState(), viewModel.controller()) { playerSurface = DesktopPlayerSurface.Player }
        DesktopPlayerSurface.Lyrics -> SyncedLyrics(lyricLines, viewModel.playerState().positionMs) { playerSurface = DesktopPlayerSurface.Player }
        DesktopPlayerSurface.None -> Unit
    }
    viewModel.playerState().currentSong?.takeIf { showSongActions }?.let { song -> SongActionsDialog(song, viewModel.repository(), { viewModel.controller().dispatch(PlayerCommand.Next) }, { showSongActions = false }, onDownload = if (song.path?.startsWith("http") == true) ({ viewModel.download(song) }) else null) }
    }
}

private enum class DesktopPlayerSurface { None, Player, Queue, Lyrics }

@Composable
private fun DesktopNavigationRail(selected: DesktopDestination, onSelect: (DesktopDestination) -> Unit) = NavigationRail {
    Spacer(Modifier.height(16.dp))
    DesktopDestination.entries.forEach { destination ->
        val icon = when (destination) {
            DesktopDestination.Home -> Icons.Outlined.Home
            DesktopDestination.Explore -> Icons.Outlined.Search
            DesktopDestination.Account -> Icons.Outlined.Person
            DesktopDestination.Library -> Icons.Outlined.LibraryMusic
            DesktopDestination.History -> Icons.Outlined.History
            DesktopDestination.Playlists -> Icons.Outlined.PlaylistPlay
            DesktopDestination.Favourites -> Icons.Outlined.Favorite
            DesktopDestination.Queue -> Icons.Outlined.QueueMusic
            DesktopDestination.Effects -> Icons.Outlined.Tune
            DesktopDestination.Tags -> Icons.Outlined.Edit
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
    val favourites = state.library.filter { it.id.hashCode().toLong() in state.favourites }
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
    Text("Playback uses libVLC for local and stream formats. Install or bundle libVLC for in-app playback controls.", style = MaterialTheme.typography.bodyMedium)
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
    val favouriteId = track.id.hashCode().toLong()
    IconToggleButton(favouriteId in state.favourites, { viewModel.toggleFavourite(track) }) { Icon(if (favouriteId in state.favourites) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, "Favourite") }
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
