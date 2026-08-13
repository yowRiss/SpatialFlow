package com.codetrio.spatialflow.shared.data.innertube

/** Platform-neutral YouTube Music browse, search, and stream domain models. */
data class OnlineSong(val videoId: String, val title: String, val artist: String, val albumName: String? = null, val albumId: String? = null, val artistId: String? = null, val duration: String? = null, val durationMs: Long = 0, val thumbnailUrl: String? = null, val isExplicit: Boolean = false)
data class OnlineAlbum(val browseId: String, val playlistId: String? = null, val title: String, val artists: List<OnlineArtistRef>, val year: Int? = null, val thumbnailUrl: String? = null, val songCount: String? = null)
data class OnlineArtist(val browseId: String, val title: String, val thumbnailUrl: String? = null, val subscriberCount: String? = null, val shuffleEndpoint: String? = null, val radioEndpoint: String? = null, val isSubscribed: Boolean = false)
data class OnlineArtistRef(val name: String, val id: String? = null)
data class OnlinePlaylist(val playlistId: String, val title: String, val author: OnlineArtistRef? = null, val songCount: String? = null, val thumbnailUrl: String? = null)

sealed interface SearchItem {
    data class Song(val song: OnlineSong) : SearchItem
    data class Album(val album: OnlineAlbum) : SearchItem
    data class Artist(val artist: OnlineArtist) : SearchItem
    data class Playlist(val playlist: OnlinePlaylist) : SearchItem
}

data class SearchResult(val items: List<SearchItem>, val continuation: String? = null)
data class StreamData(val url: String, val mimeType: String, val bitrate: Int, val contentLength: Long?, val audioQuality: String?)
data class PlayerResult(val videoId: String, val title: String, val artist: String, val thumbnailUrl: String?, val durationMs: Long, val streams: List<StreamData>, val playbackUrl: String? = null, val watchtimeUrl: String? = null, val likesCount: String? = null)
data class HomeSection(val title: String, val items: List<SearchItem>, val browseEndpoint: String? = null, val params: String? = null)
data class HomePage(val sections: List<HomeSection>, val continuation: String? = null, val moods: List<String> = emptyList())
data class AlbumPage(val album: OnlineAlbum, val songs: List<OnlineSong>, val otherVersions: List<OnlineAlbum> = emptyList())
data class ArtistPage(val artist: OnlineArtist, val sections: List<HomeSection>, val description: String? = null)
data class PlaylistPage(val playlist: OnlinePlaylist, val songs: List<OnlineSong>, val continuation: String? = null)
enum class SearchFilter(val value: String) {
    SONGS("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"), ALBUMS("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D"),
    ARTISTS("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D"), PLAYLISTS("EgeKAQQoAEABagwQDhAKEAMQBRAJEAQ%3D"),
}
data class UserProfile(val name: String, val handle: String? = null, val email: String? = null, val avatarUrl: String? = null)
