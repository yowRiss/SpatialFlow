package com.codetrio.spatialflow.shared.data.lyrics

import io.ktor.client.HttpClient

interface LyricsCatalog {
    suspend fun fetch(track: TrackMetadata): Result<LyricsResult>
}

/** Initial shared multi-provider fallback: SyncLRC is preferred when karaoke timing is available. */
class NetworkLyricsCatalog(
    http: HttpClient,
    syncLrcBaseUrl: String,
    paxsenixBaseUrl: String,
    appleMusicConfig: AppleMusicLyricsConfig? = null,
    private val scorer: LyricsConfidenceScorer = LyricsConfidenceScorer(),
) : LyricsCatalog {
    private val lrcLib = LrcLibClient(http)
    private val syncLrc = SyncLrcClient(http, syncLrcBaseUrl)
    private val paxsenix = PaxsenixLyricsProvider(PaxsenixClient(http, paxsenixBaseUrl))
    private val appleMusic = appleMusicConfig?.let { AppleMusicLyricsProvider(PaxsenixClient(http, paxsenixBaseUrl), it) }

    override suspend fun fetch(track: TrackMetadata): Result<LyricsResult> = runCatching {
        val candidates = listOfNotNull(
            appleMusic?.fetch(track)?.getOrNull(),
            syncLrc.getLyrics(track).getOrNull()?.toLyricsResult(),
            lrcLib.getLyrics(track).getOrNull()?.toLyricsResult(),
            paxsenix.fetch(track).getOrNull(),
        ).map { candidate -> candidate.apply { confidence = scorer.score(this, track) } }
        candidates.maxWithOrNull(compareBy<LyricsResult> { it.isWordByWord }.thenBy { it.confidence })
            ?: error("No lyrics found for ${track.cleanedArtist} — ${track.cleanedTitle}")
    }
}

/** Reliable no-configuration lyrics catalog for desktop startup. Additional
 * providers can be layered on top when their endpoint configuration exists. */
class LrcLibLyricsCatalog(private val client: LrcLibClient) : LyricsCatalog {
    private val scorer = LyricsConfidenceScorer()
    override suspend fun fetch(track: TrackMetadata): Result<LyricsResult> =
        client.getLyrics(track).map { result -> result.toLyricsResult().apply { confidence = scorer.score(this, track) } }
}
