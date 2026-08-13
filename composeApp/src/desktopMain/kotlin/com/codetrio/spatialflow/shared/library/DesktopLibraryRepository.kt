package com.codetrio.spatialflow.shared.library

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.codetrio.spatialflow.shared.database.Play_history
import com.codetrio.spatialflow.shared.database.Playlist_song
import com.codetrio.spatialflow.shared.database.SpatialFlowDatabase
import com.codetrio.spatialflow.shared.model.SongItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/** SQLDelight-backed desktop persistence for playlists and play history. */
class DesktopLibraryRepository(
    databaseFile: File = File(System.getProperty("user.home"), ".spatialflow/spatialflow.db"),
) : LibraryRepository {
    private val mutex = Mutex()
    private val database: SpatialFlowDatabase
    private val queries get() = database.spatialFlowQueries

    private val _playlists = MutableStateFlow(emptyList<Playlist>())
    override val playlists: StateFlow<List<Playlist>> = _playlists
    private val _history = MutableStateFlow(emptyList<HistoryEntry>())
    override val history: StateFlow<List<HistoryEntry>> = _history
    private val _favouriteSongIds = MutableStateFlow(emptySet<Long>())
    override val favouriteSongIds: StateFlow<Set<Long>> = _favouriteSongIds

    init {
        databaseFile.parentFile?.mkdirs()
        val isNewDatabase = !databaseFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
        if (isNewDatabase) SpatialFlowDatabase.Schema.create(driver)
        database = SpatialFlowDatabase(driver)
        queries.createFavourites()
        refresh()
    }

    override suspend fun createPlaylist(name: String): Playlist = mutex.withLock {
        val now = System.currentTimeMillis()
        queries.insertPlaylist(name.trim().ifBlank { "Untitled playlist" }, now, now)
        val result = Playlist(queries.lastPlaylistId().executeAsOne(), name.trim().ifBlank { "Untitled playlist" }, now, now)
        refresh()
        result
    }

    override suspend fun renamePlaylist(id: Long, name: String) = mutex.withLock {
        queries.renamePlaylist(name.trim().ifBlank { "Untitled playlist" }, System.currentTimeMillis(), id)
        refresh()
    }

    override suspend fun deletePlaylist(id: Long) = mutex.withLock {
        queries.deletePlaylist(id)
        refresh()
    }

    override suspend fun songs(playlistId: Long): List<SongItem> = mutex.withLock {
        queries.selectPlaylistSongs(playlistId).executeAsList().map { it.toPlaylistSongItem() }
    }

    override suspend fun addSong(playlistId: Long, song: SongItem, position: Int?) = mutex.withLock {
        val ordinal = position?.toLong() ?: queries.selectPlaylistSongs(playlistId).executeAsList().size.toLong()
        queries.upsertPlaylistSong(
            playlistId, ordinal, song.id, song.title, song.artist, song.albumId, song.path, song.duration,
            song.dateAdded, song.data, song.thumbnailUrl, song.videoId, song.artistId, song.lufs?.toDouble(),
        )
        Unit
    }

    override suspend fun removeSong(playlistId: Long, songId: Long) = mutex.withLock {
        queries.removePlaylistSong(playlistId, songId)
        Unit
    }

    override suspend fun recordHistory(song: SongItem, playedAt: Long) = mutex.withLock {
        queries.insertHistory(
            song.id, song.title, song.artist, song.albumId, song.path, song.duration, song.dateAdded,
            song.data, song.thumbnailUrl, song.videoId, song.artistId, song.lufs?.toDouble(), playedAt,
        )
        refresh()
    }

    override suspend fun clearHistory() = mutex.withLock {
        queries.clearHistory()
        refresh()
    }

    override suspend fun toggleFavourite(songId: Long) = mutex.withLock {
        if (songId in _favouriteSongIds.value) queries.removeFavourite(songId) else queries.addFavourite(songId)
        refresh()
    }

    private fun refresh() {
        _playlists.value = queries.selectAllPlaylists().executeAsList().map { Playlist(it.id, it.name, it.created_at, it.updated_at) }
        _history.value = queries.selectHistory(200).executeAsList().map { HistoryEntry(it.toHistorySongItem(), it.played_at) }
        _favouriteSongIds.value = queries.selectFavouriteSongIds().executeAsList().toSet()
    }

    private fun Playlist_song.toPlaylistSongItem() = SongItem(song_id, title, artist, album_id, path, duration, date_added, data_, thumbnail_url, video_id, artist_id, lufs?.toFloat())
    private fun Play_history.toHistorySongItem() = SongItem(song_id, title, artist, album_id, path, duration, date_added, data_, thumbnail_url, video_id, artist_id, lufs?.toFloat())
}
