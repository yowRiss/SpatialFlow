package com.codetrio.spatialflow.shared.data.lyrics

/** A platform-neutral word timing range used by shared karaoke UI. */
data class LyricWord(
    val text: String,
    val absoluteStartTimeMs: Long,
    val durationMs: Long,
    val charRange: IntRange = 0..0,
)

data class LyricLine(
    val startTimeMs: Long,
    val content: String,
    val isInterlude: Boolean = false,
    val isWordByWord: Boolean = false,
    val words: List<LyricWord> = emptyList(),
) : Comparable<LyricLine> {
    override fun compareTo(other: LyricLine): Int = startTimeMs.compareTo(other.startTimeMs)
}

data class TrackMetadata(
    val rawTitle: String = "",
    val rawArtist: String = "",
    val cleanedTitle: String = "",
    val cleanedArtist: String = "",
    val album: String = "",
    val durationMs: Long = 0,
    val filePath: String = "",
    val videoId: String? = null,
    val version: String = "original",
    val detectedLanguage: String = "unknown",
) {
    fun cacheKey(): String {
        val raw = "${cleanedArtist.lowercase().trim()}|${cleanedTitle.lowercase().trim()}|${durationMs / 10_000}"
        return "lyr_" + raw.replace("[^a-z0-9|]".toRegex(), "")
    }
}

data class LyricsResult(
    var syncedLyrics: String? = null,
    var plainLyrics: String? = null,
    var providerName: String? = null,
    var confidence: Float = 0f,
    var isSynced: Boolean = false,
    var isWordByWord: Boolean = false,
    var isInstrumental: Boolean = false,
    var matchedTitle: String? = null,
    var matchedArtist: String? = null,
    var matchedAlbum: String? = null,
    var matchedDuration: Float = 0f,
    var fetchTimestamp: Long = 0L,
) {
    fun hasLyrics(): Boolean = !syncedLyrics.isNullOrEmpty() || !plainLyrics.isNullOrEmpty()
}

enum class LyricsState {
    IDLE, FETCHING, SUCCESS, FAILED, REFETCHING;

    val isLoading: Boolean get() = this == FETCHING

    fun canTransitionTo(target: LyricsState): Boolean = when (this) {
        IDLE -> target == FETCHING
        FETCHING -> target == SUCCESS || target == FAILED
        SUCCESS -> target == REFETCHING || target == IDLE
        FAILED -> target == FETCHING || target == IDLE
        REFETCHING -> target == SUCCESS || target == IDLE
    }
}
