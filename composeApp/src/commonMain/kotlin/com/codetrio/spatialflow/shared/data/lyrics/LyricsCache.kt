package com.codetrio.spatialflow.shared.data.lyrics

/** Persistence-neutral lyrics cache; platform persistence adapters can wrap this API. */
interface LyricsCache {
    fun get(track: TrackMetadata): LyricsResult?
    fun put(track: TrackMetadata, result: LyricsResult)
    fun remove(track: TrackMetadata)
}

class InMemoryLyricsCache : LyricsCache {
    private val values = mutableMapOf<String, LyricsResult>()
    override fun get(track: TrackMetadata): LyricsResult? = values[track.cacheKey()]
    override fun put(track: TrackMetadata, result: LyricsResult) { values[track.cacheKey()] = result }
    override fun remove(track: TrackMetadata) { values.remove(track.cacheKey()) }
}
