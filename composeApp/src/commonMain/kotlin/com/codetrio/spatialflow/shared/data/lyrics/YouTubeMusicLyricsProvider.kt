package com.codetrio.spatialflow.shared.data.lyrics

import com.codetrio.spatialflow.shared.data.innertube.InnerTubeClient
import com.codetrio.spatialflow.shared.data.innertube.InnerTubeParser

/** Official plain-lyrics lookup for the active YouTube Music video. */
class YouTubeMusicLyricsProvider(private val innerTube: InnerTubeClient) {
    suspend fun fetch(track: TrackMetadata): Result<LyricsResult> = runCatching {
        val videoId = track.videoId ?: error("No YouTube Music video id")
        val next = innerTube.next(videoId).getOrThrow()
        val lyricsBrowseId = InnerTubeParser.findLyricsBrowseId(next) ?: error("No official lyrics tab")
        val lyrics = InnerTubeParser.parseLyrics(innerTube.browse(lyricsBrowseId).getOrThrow())
            ?: error("No official lyric text")
        LyricsResult(
            plainLyrics = lyrics,
            providerName = "YouTube Music",
            confidence = 1f,
            matchedTitle = track.rawTitle,
            matchedArtist = track.rawArtist,
        )
    }
}
