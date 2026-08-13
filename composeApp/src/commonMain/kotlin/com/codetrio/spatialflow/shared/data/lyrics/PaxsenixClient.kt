package com.codetrio.spatialflow.shared.data.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

/** Shared Ktor transport for the Android app's Paxsenix provider endpoints. */
class PaxsenixClient(private val http: HttpClient, private val baseUrl: String) {
    suspend fun searchSpotify(query: String): Result<String> = get("spotify/search") { parameter("q", query) }
    suspend fun spotifyLyrics(id: String): Result<String> = get("spotify/lyrics") { parameter("id", id) }
    suspend fun searchYouTube(query: String): Result<String> = get("youtube/search") { parameter("q", query) }
    suspend fun youtubeLyrics(id: String): Result<String> = get("youtube/lyrics") { parameter("id", id) }

    suspend fun musixmatchLyrics(query: String, title: String? = null, artist: String? = null, duration: String? = null, type: String = "word"): Result<String> = get("musixmatch/lyrics") {
        parameter("q", query); title?.let { parameter("t", it) }; artist?.let { parameter("a", it) }
        duration?.let { parameter("duration", it) }; parameter("type", type)
    }

    suspend fun appleMusicLyrics(id: String, ttml: Boolean = false): Result<String> = get("apple-music/lyrics") {
        parameter("id", id); parameter("ttml", ttml)
    }

    suspend fun searchAppleMusic(url: String, authorization: String, storefront: String, term: String, limit: Int = 10): Result<String> = runCatching {
        val response = http.get("$url/v1/catalog/$storefront/search") {
            header("Authorization", authorization); header("Origin", "https://music.apple.com")
            header("Referer", "https://music.apple.com/"); header("User-Agent", "SpatialFlow")
            parameter("term", term); parameter("types", "songs"); parameter("limit", limit)
        }
        check(response.status.isSuccess()) { "Apple Music request failed (${response.status.value})" }
        response.bodyAsText()
    }

    private suspend fun get(path: String, configure: io.ktor.client.request.HttpRequestBuilder.() -> Unit): Result<String> = runCatching {
        val response = http.get("${baseUrl.trimEnd('/')}/$path", configure)
        check(response.status.isSuccess()) { "Paxsenix request failed (${response.status.value})" }
        response.bodyAsText()
    }
}
