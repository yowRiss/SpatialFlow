package com.codetrio.spatialflow.shared.data.innertube

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class InnerTubeConfig(
    val apiKey: String,
    val locale: String = "en",
    val region: String = "US",
    val cookie: String? = null,
    val visitorData: String? = null,
)

/** Ktor transport for typed repositories and parsers; no Android/NewPipe code leaks into commonMain. */
class InnerTubeClient(private val http: HttpClient, private val config: InnerTubeConfig) {
    suspend fun search(query: String, filter: SearchFilter? = null, continuation: String? = null): Result<JsonObject> = request("search") {
        put("context", webRemixContext())
        if (continuation != null) put("continuation", continuation) else {
            put("query", query); filter?.let { put("params", it.value) }
        }
    }

    suspend fun browse(browseId: String? = null, params: String? = null, continuation: String? = null): Result<JsonObject> = request("browse") {
        put("context", webRemixContext())
        if (continuation != null) put("continuation", continuation) else {
            browseId?.takeIf(String::isNotBlank)?.let { put("browseId", it) }
            params?.let { put("params", it) }
        }
    }

    suspend fun player(videoId: String, playlistId: String? = null): Result<JsonObject> = request("player") {
        put("context", webRemixContext()); put("videoId", videoId); playlistId?.let { put("playlistId", it) }
    }

    suspend fun searchSuggestions(query: String): Result<JsonObject> = request("music/get_search_suggestions") {
        put("context", webRemixContext()); put("input", query)
    }

    private suspend fun request(endpoint: String, body: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): Result<JsonObject> = runCatching {
        val response = http.post("https://music.youtube.com/youtubei/v1/$endpoint?key=${config.apiKey}") {
            contentType(ContentType.Application.Json)
            header("Origin", "https://music.youtube.com")
            header("X-Origin", "https://music.youtube.com")
            header("X-YouTube-Client-Name", "67")
            header("X-YouTube-Client-Version", "1.20260531.05.00")
            config.cookie?.let { header("Cookie", it) }
            config.visitorData?.let { header("X-Goog-Visitor-Id", it) }
            setBody(buildJsonObject(body).toString())
        }
        check(response.status.isSuccess()) { "InnerTube $endpoint failed (${response.status.value})" }
        Json.parseToJsonElement(response.bodyAsText()) as? JsonObject ?: error("InnerTube $endpoint returned non-object JSON")
    }

    private fun webRemixContext() = buildJsonObject {
        put("client", buildJsonObject {
            put("clientName", "WEB_REMIX"); put("clientVersion", "1.20260531.05.00")
            put("hl", config.locale); put("gl", config.region)
            config.visitorData?.let { put("visitorData", it) }
        })
    }
}
