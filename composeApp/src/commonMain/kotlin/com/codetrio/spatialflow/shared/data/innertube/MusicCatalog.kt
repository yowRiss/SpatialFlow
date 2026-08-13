package com.codetrio.spatialflow.shared.data.innertube

/**
 * Shared boundary consumed by explore UI and view models. The first Ktor-based
 * InnerTube implementation will satisfy this contract on both platforms.
 */
interface MusicCatalog {
    suspend fun search(query: String, filter: SearchFilter? = null): Result<SearchResult>
    suspend fun searchContinuation(continuation: String): Result<SearchResult>
    suspend fun searchSuggestions(query: String): Result<List<String>>
    suspend fun home(): Result<HomePage>
    suspend fun explore(): Result<HomePage>
    suspend fun album(browseId: String): Result<AlbumPage>
    suspend fun artist(browseId: String): Result<ArtistPage>
    suspend fun playlist(playlistId: String): Result<PlaylistPage>
    suspend fun player(videoId: String): Result<PlayerResult>
}
