package com.codetrio.spatialflow.shared.data.lyrics

/** Shared provider query normalization and result confidence policy. */
object LyricsMatching {
    fun generateQueries(track: TrackMetadata?): List<Pair<String, String>> {
        if (track == null) return emptyList()
        val artist = track.cleanedArtist
        val title = track.cleanedTitle
        return buildList {
            fun addDistinct(value: Pair<String, String>) { if (value.second.isNotBlank() && value !in this) add(value) }
            addDistinct(artist to title)
            addDistinct("" to title)
            val rawTitle = foldDiacritics(track.rawTitle)
            if (rawTitle != title) addDistinct(artist to rawTitle)
            if (track.version != "original") addDistinct(artist to title.replace(Regex("(?i)\\s*(?:remix|rmx|slowed|reverb|live|cover|acoustic|extended|edit|radio\\s*edit)\\s*"), "").trim())
            val foldedTitle = foldDiacritics(title)
            if (foldedTitle != title) addDistinct(foldDiacritics(artist) to foldedTitle)
            if (artist.isNotBlank()) addDistinct("" to "$artist $title")
            if (track.rawArtist.isNotBlank() && track.rawArtist != artist) addDistinct(track.rawArtist to title)
        }
    }

    fun similarity(first: String?, second: String?): Float {
        val a = normalize(first)
        val b = normalize(second)
        if (a.isEmpty() && b.isEmpty()) return 1f
        if (a.isEmpty() || b.isEmpty()) return 0f
        if (a == b) return 1f
        if (a.contains(b) || b.contains(a)) return maxOf(0.7f, minOf(a.length, b.length).toFloat() / maxOf(a.length, b.length))
        return 1f - levenshtein(a, b).toFloat() / maxOf(a.length, b.length)
    }

    private fun normalize(value: String?): String = foldDiacritics(value.orEmpty().lowercase().trim())
        .replace(Regex("[^a-z0-9\\s]"), "").replace(Regex("\\s+"), " ").trim()

    private fun levenshtein(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in a.indices) {
            current[0] = i + 1
            for (j in b.indices) current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + if (a[i] == b[j]) 0 else 1)
            val swap = previous; previous = current; current = swap
        }
        return previous[b.length]
    }
}

class LyricsConfidenceScorer {
    fun score(result: LyricsResult?, track: TrackMetadata?): Float {
        if (result == null || track == null) return 0f
        val title = result.matchedTitle?.let { LyricsMatching.similarity(track.cleanedTitle, it) } ?: if (result.hasLyrics()) .5f else 0f
        val artist = result.matchedArtist?.let { LyricsMatching.similarity(track.cleanedArtist, it) } ?: if (result.hasLyrics()) .5f else 0f
        val duration = when {
            result.matchedDuration <= 0 || track.durationMs <= 0 -> .5f
            kotlin.math.abs(track.durationMs / 1000f - result.matchedDuration) <= 2 -> 1f
            kotlin.math.abs(track.durationMs / 1000f - result.matchedDuration) <= 5 -> .9f
            kotlin.math.abs(track.durationMs / 1000f - result.matchedDuration) <= 10 -> .7f
            kotlin.math.abs(track.durationMs / 1000f - result.matchedDuration) <= 30 -> .4f
            else -> .1f
        }
        val sync = syncQuality(result)
        var total = title * .25f + artist * .25f + duration * .2f + sync * .2f + trust(result.providerName) * .1f
        if (result.isSynced && sync > .8f) total = (total + .05f).coerceAtMost(1f)
        return if (result.isInstrumental) total.coerceAtMost(.4f) else total
    }

    private fun syncQuality(result: LyricsResult): Float = when {
        result.isSynced && result.syncedLyrics != null -> when (result.syncedLyrics!!.lines().count { Regex("^\\[\\d{2}:\\d{2}\\.\\d{2,3}].*").matches(it) }) {
            in 20..Int.MAX_VALUE -> 1f; in 10..19 -> .8f; in 5..9 -> .6f; in 1..4 -> .4f; else -> 0f
        }
        !result.plainLyrics.isNullOrEmpty() -> when (result.plainLyrics!!.lines().size) { in 10..Int.MAX_VALUE -> .5f; in 5..9 -> .3f; else -> .2f }
        else -> 0f
    }

    private fun trust(provider: String?) = when (provider) {
        "LRCLIB", "SyncLRC" -> .95f; "EmbeddedID3", "LocalLrc" -> .9f; "YouTube Music" -> .85f; "OVH" -> .7f; "SomeRandom" -> .6f; else -> .5f
    }
}
