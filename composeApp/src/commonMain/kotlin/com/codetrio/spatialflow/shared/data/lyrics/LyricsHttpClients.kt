package com.codetrio.spatialflow.shared.data.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class LrcLibLyrics(
    val id: Int,
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationSeconds: Float,
    val instrumental: Boolean,
    val plainLyrics: String?,
    val syncedLyrics: String?,
) {
    fun toLyricsResult(providerName: String = "LRCLIB") = LyricsResult(
        syncedLyrics = syncedLyrics, plainLyrics = plainLyrics, providerName = providerName,
        isSynced = !syncedLyrics.isNullOrBlank(), isInstrumental = instrumental,
        matchedTitle = title, matchedArtist = artist, matchedAlbum = album,
        matchedDuration = durationSeconds,
    )
}

/** Ktor replacement for the Retrofit LRCLIB API, shared by Android and desktop. */
class LrcLibClient(private val http: HttpClient, private val baseUrl: String = "https://lrclib.net") {
    suspend fun getLyrics(track: TrackMetadata): Result<LrcLibLyrics> = request("/api/get") {
        parameter("track_name", track.cleanedTitle)
        parameter("artist_name", track.cleanedArtist)
        track.album.takeIf(String::isNotBlank)?.let { parameter("album_name", it) }
        track.durationMs.takeIf { it > 0 }?.let { parameter("duration", it / 1000f) }
    }.mapCatching(::parse)

    suspend fun search(query: String): Result<List<LrcLibLyrics>> = request("/api/search") {
        parameter("q", query)
    }.mapCatching { payload ->
        Json.parseToJsonElement(payload).let { element ->
            element as? kotlinx.serialization.json.JsonArray ?: error("Unexpected LRCLIB search response")
        }.map { parse(it as JsonObject) }
    }

    private suspend fun request(path: String, configure: io.ktor.client.request.HttpRequestBuilder.() -> Unit): Result<String> = runCatching {
        val response = http.get("$baseUrl$path", configure)
        check(response.status.isSuccess()) { "LRCLIB request failed (${response.status.value})" }
        response.bodyAsText()
    }

    private fun parse(payload: String): LrcLibLyrics = parse(Json.parseToJsonElement(payload) as JsonObject)
    private fun parse(json: JsonObject): LrcLibLyrics = LrcLibLyrics(
        id = json["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
        title = json.stringOrNull("name"), artist = json.stringOrNull("artistName"), album = json.stringOrNull("albumName"),
        durationSeconds = json["duration"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f,
        instrumental = json["instrumental"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
        plainLyrics = json.stringOrNull("plainLyrics"), syncedLyrics = json.stringOrNull("syncedLyrics"),
    )

    private fun JsonObject.stringOrNull(name: String) = this[name]?.jsonPrimitive?.contentOrNull
}

data class SyncLrcLyrics(
    val id: String?, val title: String?, val artist: String?, val album: String?,
    val durationSeconds: Int?, val instrumental: Boolean, val karaoke: String?,
    val syncedLyrics: String?, val plainLyrics: String?,
) {
    fun toLyricsResult() = LyricsResult(
        syncedLyrics = syncedLyrics ?: karaoke, plainLyrics = plainLyrics, providerName = "SyncLRC",
        isSynced = !(syncedLyrics ?: karaoke).isNullOrBlank(), isWordByWord = !karaoke.isNullOrBlank(),
        isInstrumental = instrumental, matchedTitle = title, matchedArtist = artist,
        matchedAlbum = album, matchedDuration = durationSeconds?.toFloat() ?: 0f,
    )
}

/** Ktor replacement for the Retrofit SyncLRC endpoint. */
class SyncLrcClient(private val http: HttpClient, private val baseUrl: String) {
    suspend fun getLyrics(track: TrackMetadata): Result<SyncLrcLyrics> = runCatching {
        val response = http.get("$baseUrl/lyrics") {
            parameter("track", track.cleanedTitle)
            parameter("artist", track.cleanedArtist)
            track.album.takeIf(String::isNotBlank)?.let { parameter("album", it) }
            track.durationMs.takeIf { it > 0 }?.let { parameter("duration", it / 1_000) }
        }
        check(response.status.isSuccess()) { "SyncLRC request failed (${response.status.value})" }
        val json = Json.parseToJsonElement(response.bodyAsText()) as JsonObject
        SyncLrcLyrics(
            id = json.stringValue("id"), title = json.stringValue("track"), artist = json.stringValue("artist"),
            album = json.stringValue("album"), durationSeconds = json["duration"]?.jsonPrimitive?.content?.toIntOrNull(),
            instrumental = json["instrumental"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
            karaoke = json.stringValue("karaoke"), syncedLyrics = json.stringValue("synced"), plainLyrics = json.stringValue("plain"),
        )
    }
}

private fun JsonObject.stringValue(name: String) = this[name]?.jsonPrimitive?.contentOrNull
