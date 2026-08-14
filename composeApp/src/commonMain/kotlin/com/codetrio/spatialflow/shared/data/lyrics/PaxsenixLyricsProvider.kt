package com.codetrio.spatialflow.shared.data.lyrics

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Shared Paxsenix parser with word-timed Musixmatch, Spotify, and YouTube
 * fallbacks. Apple Music requires a renewable developer token and remains a
 * configured integration rather than embedding an expiring credential. */
class PaxsenixLyricsProvider(private val client: PaxsenixClient) {
    suspend fun fetch(track: TrackMetadata): Result<LyricsResult> = runCatching {
        fetchMusixmatch(track).getOrNull()
            ?: fetchSpotify(track).getOrNull()
            ?: fetchYouTube(track).getOrNull()
            ?: error("Paxsenix returned no lyrics for ${track.cleanedArtist} — ${track.cleanedTitle}")
    }

    private suspend fun fetchMusixmatch(track: TrackMetadata): Result<LyricsResult?> = runCatching {
        val query = "${track.cleanedTitle} ${track.cleanedArtist}"
        val duration = track.durationMs.takeIf { it > 0 }?.div(1_000)?.toString()
        val wordTimed = client.musixmatchLyrics(query, track.cleanedTitle, track.cleanedArtist, duration, type = "word")
            .getOrNull()?.let(::decodeLyrics)
        val lyrics = wordTimed ?: client.musixmatchLyrics(query, track.cleanedTitle, track.cleanedArtist, duration, type = "default")
            .getOrNull()?.let(::decodeLyrics)
            ?: return@runCatching null
        lyrics.toResult("Musixmatch", track.cleanedTitle, track.cleanedArtist)
    }

    private suspend fun fetchSpotify(track: TrackMetadata): Result<LyricsResult?> = runCatching {
        val search = parse(client.searchSpotify("${track.cleanedTitle} ${track.cleanedArtist}").getOrThrow())
        val item = search.jsonArray.firstOrNull()?.jsonObject ?: return@runCatching null
        val id = item.string("trackId") ?: return@runCatching null
        val lyrics = decodeLyrics(client.spotifyLyrics(id).getOrThrow()) ?: return@runCatching null
        lyrics.toResult("Spotify", item.string("name") ?: track.cleanedTitle, item.string("artistName") ?: track.cleanedArtist)
    }

    private suspend fun fetchYouTube(track: TrackMetadata): Result<LyricsResult?> = runCatching {
        val search = parse(client.searchYouTube("${track.cleanedTitle} ${track.cleanedArtist}").getOrThrow())
        val item = search.jsonArray.firstOrNull()?.jsonObject ?: return@runCatching null
        val id = item.string("id") ?: item.string("videoId") ?: return@runCatching null
        val lyrics = decodeLyrics(client.youtubeLyrics(id).getOrThrow()) ?: return@runCatching null
        lyrics.toResult("YouTube (Paxsenix)", track.cleanedTitle, track.cleanedArtist)
    }

    private fun String.toResult(provider: String, title: String, artist: String): LyricsResult {
        val synced = contains(Regex("\\[\\d{1,2}:\\d{2}(?:[.:]\\d{2,3})?]"))
        return LyricsResult(
            syncedLyrics = takeIf { synced },
            plainLyrics = takeUnless { synced },
            providerName = provider,
            isSynced = synced,
            isWordByWord = contains(Regex("<\\d{1,2}:\\d{2}(?:[.:]\\d{2,3})?>")),
            matchedTitle = title,
            matchedArtist = artist,
            fetchTimestamp = 0L,
        )
    }

    private fun decodeLyrics(raw: String): String? {
        val element = runCatching { parse(raw) }.getOrNull()
        val value = when (element) {
            is JsonPrimitive -> element.content
            is JsonObject -> element.string("lyrics") ?: element.string("lrc") ?: element["lines"]?.let(::linesToLrc)
            is JsonArray -> linesToLrc(element)
            else -> raw
        } ?: raw
        return value.trim().takeUnless { it.isEmpty() || it.contains("\"error\"", ignoreCase = true) || it.contains("isError\":true") }
    }

    private fun linesToLrc(lines: JsonElement): String = lines.jsonArray.joinToString("\n") { line ->
        val objectLine = line.jsonObject
        val time = objectLine.string("timeTag") ?: objectLine.string("startTimeMs")?.toLongOrNull()?.let(::formatTime)
        val words = objectLine.string("words").orEmpty()
        if (time == null) words else "[$time] $words"
    }

    private fun formatTime(milliseconds: Long): String {
        val minutes = milliseconds / 60_000
        val seconds = (milliseconds % 60_000) / 1_000
        val hundredths = (milliseconds % 1_000) / 10
        return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${hundredths.toString().padStart(2, '0')}"
    }

    private fun JsonObject.string(name: String): String? = (this[name] as? JsonPrimitive)?.content
    private fun parse(raw: String): JsonElement = Json.parseToJsonElement(raw)
}
