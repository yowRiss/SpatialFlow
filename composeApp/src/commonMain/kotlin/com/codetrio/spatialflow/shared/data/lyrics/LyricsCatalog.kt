package com.codetrio.spatialflow.shared.data.lyrics

import io.ktor.client.HttpClient

interface LyricsCatalog {
    suspend fun fetch(track: TrackMetadata): Result<LyricsResult>
}

/** Initial shared multi-provider fallback: SyncLRC is preferred when karaoke timing is available. */
class NetworkLyricsCatalog(
    http: HttpClient,
    syncLrcBaseUrl: String,
    private val scorer: LyricsConfidenceScorer = LyricsConfidenceScorer(),
) : LyricsCatalog {
    private val lrcLib = LrcLibClient(http)
    private val syncLrc = SyncLrcClient(http, syncLrcBaseUrl)

    override suspend fun fetch(track: TrackMetadata): Result<LyricsResult> = runCatching {
        val candidates = listOfNotNull(
            syncLrc.getLyrics(track).getOrNull()?.toLyricsResult(),
            lrcLib.getLyrics(track).getOrNull()?.toLyricsResult(),
        ).map { candidate -> candidate.apply { confidence = scorer.score(this, track) } }
        candidates.maxWithOrNull(compareBy<LyricsResult> { it.isWordByWord }.thenBy { it.confidence })
            ?: error("No lyrics found for ${track.cleanedArtist} — ${track.cleanedTitle}")
    }
}
