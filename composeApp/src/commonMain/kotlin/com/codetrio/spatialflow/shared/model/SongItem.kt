package com.codetrio.spatialflow.shared.model

/**
 * Shared song identity and display metadata. Platform code owns URI conversion,
 * MediaStore access, embedded artwork, and file descriptors.
 */
data class SongItem(
    val id: Long,
    val title: String,
    val artist: String,
    val albumId: Long = -1,
    val path: String? = null,
    val duration: Long = 0,
    val dateAdded: Long = 0,
    val data: String? = path,
    var thumbnailUrl: String? = null,
    var videoId: String? = null,
    var artistId: String? = null,
    var lufs: Float? = null,
) {
    val artworkLocation: String? get() = thumbnailUrl?.let(::enhanceThumbnailUrl) ?: path

    companion object {
        fun local(
            id: Long, rawTitle: String?, rawArtist: String?, albumId: Long,
            path: String?, duration: Long, dateAdded: Long,
        ) = SongItem(id, cleanTitle(rawTitle, path), cleanArtist(rawArtist, path), albumId, path, duration, dateAdded)

        fun online(
            videoId: String?, title: String?, artist: String?, streamUrl: String?,
            durationMs: Long, thumbnailUrl: String?, artistId: String? = null,
        ): SongItem = SongItem(
            id = videoId?.hashCode()?.toLong() ?: streamUrl?.hashCode()?.toLong() ?: 0L,
            title = title ?: "Unknown Title", artist = artist ?: "Unknown Artist", path = streamUrl,
            duration = durationMs, thumbnailUrl = thumbnailUrl?.let(::enhanceThumbnailUrl),
            videoId = videoId, artistId = artistId,
        )

        fun enhanceThumbnailUrl(url: String): String = when {
            url.isEmpty() -> url
            url.contains("googleusercontent.com") || url.contains("ggpht.com") -> {
                val sizeParameter = Regex("(=[ws]\\d+.*)$")
                if (sizeParameter.containsMatchIn(url)) url.replace(sizeParameter, "=w1000-h1000-l90-rj") else "$url=w1000-h1000-l90-rj"
            }
            url.contains("ytimg.com") || url.contains("youtube.com/vi/") -> url.replace(Regex("/(default|mqdefault)\\.jpg$"), "/hqdefault.jpg")
            else -> url
        }

        private fun cleanTitle(raw: String?, path: String?): String = clean(raw, "Unknown Title", path, 1)
        private fun cleanArtist(raw: String?, path: String?): String = clean(raw, "Unknown Artist", path, 0)
        private fun clean(raw: String?, fallback: String, path: String?, fromSplit: Int): String {
            val value = raw?.takeUnless { it.isBlank() || it.equals("<unknown>", true) } ?: fallback
            if (value != fallback || path == null) return value
            val name = path.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
            val split = name.split(" - ", limit = 2)
            return split.getOrNull(fromSplit)?.trim().takeUnless { it.isNullOrEmpty() } ?: name.trim().ifEmpty { fallback }
        }
    }
}
