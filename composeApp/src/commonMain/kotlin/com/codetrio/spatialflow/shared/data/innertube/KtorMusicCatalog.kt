package com.codetrio.spatialflow.shared.data.innertube

/** Ktor-backed implementation consumed by shared Explore UI/ViewModels. */
class KtorMusicCatalog(private val client: InnerTubeClient) : MusicCatalog {
    override suspend fun search(query: String, filter: SearchFilter?): Result<SearchResult> = client.search(query, filter).map(InnerTubeParser::parseSearchResponse)
    override suspend fun searchContinuation(continuation: String): Result<SearchResult> = client.search("", continuation = continuation).map(InnerTubeParser::parseSearchResponse)
    override suspend fun searchSuggestions(query: String): Result<List<String>> = client.searchSuggestions(query).map(InnerTubeParser::parseSuggestions)
    override suspend fun home(): Result<HomePage> = client.browse("FEmusic_home").map(InnerTubeParser::parseHomeResponse)
    override suspend fun explore(): Result<HomePage> = client.browse("FEmusic_explore").map(InnerTubeParser::parseHomeResponse)
    override suspend fun album(browseId: String): Result<AlbumPage> = client.browse(browseId).map(InnerTubeParser::parseAlbumResponse)
    override suspend fun artist(browseId: String): Result<ArtistPage> = client.browse(browseId).map(InnerTubeParser::parseArtistResponse)
    override suspend fun playlist(playlistId: String): Result<PlaylistPage> = client.browse("VL$playlistId").map(InnerTubeParser::parsePlaylistResponse)
    override suspend fun player(videoId: String): Result<PlayerResult> = client.player(videoId).mapCatching { InnerTubeParser.parsePlayerResponse(it) ?: error("No playable stream") }
}
