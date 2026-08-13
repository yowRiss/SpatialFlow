package com.codetrio.spatialflow.shared.library

import com.codetrio.spatialflow.shared.model.SongItem
import kotlinx.coroutines.flow.StateFlow

data class Playlist(val id: Long, val name: String, val createdAt: Long, val updatedAt: Long)
data class HistoryEntry(val song: SongItem, val playedAt: Long)

/** Common DAO-shaped contract. Persistent backend selection is platform-level. */
interface LibraryRepository {
    val playlists: StateFlow<List<Playlist>>
    val history: StateFlow<List<HistoryEntry>>
    val favouriteSongIds: StateFlow<Set<Long>>
    suspend fun createPlaylist(name: String): Playlist
    suspend fun renamePlaylist(id: Long, name: String)
    suspend fun deletePlaylist(id: Long)
    suspend fun songs(playlistId: Long): List<SongItem>
    suspend fun addSong(playlistId: Long, song: SongItem, position: Int? = null)
    suspend fun removeSong(playlistId: Long, songId: Long)
    suspend fun recordHistory(song: SongItem, playedAt: Long)
    suspend fun clearHistory()
    suspend fun toggleFavourite(songId: Long)
}
