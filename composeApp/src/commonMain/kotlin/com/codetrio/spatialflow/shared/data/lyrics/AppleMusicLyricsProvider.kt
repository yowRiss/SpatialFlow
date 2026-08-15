package com.codetrio.spatialflow.shared.data.lyrics

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Optional Apple Music AMP lyrics source. The developer token is deliberately
 * supplied by the desktop host: Apple tokens are short lived and must never be
 * embedded in a release binary.
 */
data class AppleMusicLyricsConfig(
    val developerToken: String,
    val storefront: String = "us",
    val ampBaseUrl: String = "https://amp-api.music.apple.com",
)

class AppleMusicLyricsProvider(
    private val client: PaxsenixClient,
    private val config: AppleMusicLyricsConfig,
) {
    suspend fun fetch(track: TrackMetadata): Result<LyricsResult> = runCatching {
        val search = client.searchAppleMusic(
            url = config.ampBaseUrl,
            authorization = "Bearer ${config.developerToken}",
            storefront = config.storefront,
            term = "${track.cleanedTitle} ${track.cleanedArtist}".trim(),
        ).getOrThrow()
        val best = selectMatch(search, track) ?: error("Apple Music returned no matching song")
        val ttml = unwrapTtml(client.appleMusicLyrics(best.id, ttml = true).getOrThrow())
            ?: error("Apple Music returned no timed lyrics")
        val lrc = AppleMusicTtml.toEnhancedLrc(ttml)
        check(lrc.isNotBlank()) { "Apple Music TTML contained no lyric lines" }
        LyricsResult(
            syncedLyrics = lrc,
            providerName = "Apple Music",
            isSynced = true,
            isWordByWord = lrc.contains('<'),
            matchedTitle = best.title,
            matchedArtist = best.artist,
            matchedDuration = best.durationMs / 1_000f,
        )
    }

    private fun selectMatch(raw: String, track: TrackMetadata): AppleSong? {
        val songs = runCatching {
            (kotlinx.serialization.json.Json.parseToJsonElement(raw) as JsonObject)
                .getValue("results").jsonObject.getValue("songs").jsonObject.getValue("data")
                .let { it as kotlinx.serialization.json.JsonArray }
        }.getOrNull() ?: return null
        return songs.mapNotNull { item ->
            val objectItem = item.jsonObject
            val attributes = objectItem["attributes"]?.jsonObject ?: return@mapNotNull null
            AppleSong(
                id = (objectItem["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null,
                title = (attributes["name"] as? JsonPrimitive)?.content.orEmpty(),
                artist = (attributes["artistName"] as? JsonPrimitive)?.content.orEmpty(),
                durationMs = (attributes["durationInMillis"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L,
            )
        }.maxByOrNull { song ->
            val title = LyricsMatching.similarity(track.cleanedTitle, song.title)
            val artist = LyricsMatching.similarity(track.cleanedArtist, song.artist)
            val duration = if (track.durationMs > 0 && song.durationMs > 0) {
                (1f - (kotlin.math.abs(track.durationMs - song.durationMs) / 10_000f)).coerceIn(0f, 1f)
            } else .5f
            title * .45f + artist * .4f + duration * .15f
        }?.takeIf { LyricsMatching.similarity(track.cleanedTitle, it.title) >= .5f }
    }

    private fun unwrapTtml(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.startsWith("<tt") || trimmed.startsWith("<?xml")) return trimmed
        return runCatching {
            ((kotlinx.serialization.json.Json.parseToJsonElement(trimmed) as? JsonObject)?.get("content") as? JsonPrimitive)?.content
        }.getOrNull()?.takeIf { it.contains("<tt") || it.contains("<p") }
    }

    private data class AppleSong(val id: String, val title: String, val artist: String, val durationMs: Long)
}

/** Minimal TTML-to-enhanced-LRC adapter for AMP's timed <p>/<span> lyric form. */
object AppleMusicTtml {
    private val paragraph = Regex("<p\\b([^>]*)>(.*?)</p>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val span = Regex("<span\\b([^>]*)>(.*?)</span>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val begin = Regex("\\bbegin\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']", RegexOption.IGNORE_CASE)
    private val end = Regex("\\bend\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']", RegexOption.IGNORE_CASE)
    private val tags = Regex("<[^>]+>")

    fun toEnhancedLrc(ttml: String): String = paragraph.findAll(ttml).mapNotNull { match ->
        val lineStart = parseTime(begin.find(match.groupValues[1])?.groupValues?.get(1)) ?: return@mapNotNull null
        val body = match.groupValues[2]
        val words = span.findAll(body).mapNotNull { word ->
            val start = parseTime(begin.find(word.groupValues[1])?.groupValues?.get(1)) ?: return@mapNotNull null
            "<${format(start)}>" + decode(word.groupValues[2].replace(tags, "")).takeIf { it.isNotBlank() }
        }.toList()
        val text = if (words.isEmpty()) decode(body.replace(tags, "")).trim() else words.joinToString("")
        text.takeIf { it.isNotBlank() }?.let { "[${format(lineStart)}]$it" }
    }.joinToString("\n")

    private fun parseTime(value: String?): Long? {
        val source = value?.trim()?.removeSuffix("s") ?: return null
        source.toDoubleOrNull()?.let { return (it * 1_000).toLong() }
        val parts = source.split(':').map { it.toDoubleOrNull() ?: return null }
        return when (parts.size) {
            2 -> ((parts[0] * 60 + parts[1]) * 1_000).toLong()
            3 -> ((parts[0] * 3_600 + parts[1] * 60 + parts[2]) * 1_000).toLong()
            else -> null
        }
    }

    private fun format(milliseconds: Long): String = buildString {
        append((milliseconds / 60_000).toString().padStart(2, '0'))
        append(':')
        append(((milliseconds / 1_000) % 60).toString().padStart(2, '0'))
        append('.')
        append((milliseconds % 1_000).toString().padStart(3, '0'))
    }
    private fun decode(value: String): String = value.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
}
