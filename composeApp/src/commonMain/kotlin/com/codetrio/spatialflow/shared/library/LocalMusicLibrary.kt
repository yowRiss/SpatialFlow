package com.codetrio.spatialflow.shared.library

import com.codetrio.spatialflow.shared.model.SongItem

/** Platform capability for discovering local music and reading its display metadata. */
interface LocalMusicLibrary {
    suspend fun scan(rootPath: String): Result<List<SongItem>>
}
