package com.codetrio.spatialflow.shared.data.lyrics

/**
 * Common LRC parser for the formats used by the desktop karaoke renderer.
 * It handles repeated line timestamps and enhanced `<mm:ss.xxx>` word timings;
 * Android can retain its Media3/Gramophone parser until that dependency moves.
 */
object SharedLrcParser {
    private const val interludeThresholdMs = 5_000L
    private val lineTimestamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val wordTimestamp = Regex("<(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?>")

    fun parse(content: String?): List<LyricLine> {
        if (content.isNullOrBlank()) return emptyList()
        val lines = buildList {
            content.lineSequence().forEach { source ->
                val timestamps = lineTimestamp.findAll(source).toList()
                if (timestamps.isEmpty()) {
                    source.trim().takeIf(String::isNotEmpty)?.let { add(LyricLine(0, it)) }
                    return@forEach
                }
                val lyric = source.substring(timestamps.last().range.last + 1).trim()
                val words = parseWords(lyric)
                timestamps.forEach { timestamp ->
                    add(LyricLine(
                        startTimeMs = timestampToMs(timestamp),
                        content = if (words.isEmpty()) lyric else words.joinToString("") { it.text },
                        isWordByWord = words.isNotEmpty(),
                        words = words,
                    ))
                }
            }
        }.sorted()
        return insertInterludes(lines)
    }

    private fun parseWords(lyric: String): List<LyricWord> {
        val markers = wordTimestamp.findAll(lyric).toList()
        if (markers.isEmpty()) return emptyList()
        var contentOffset = 0
        return markers.mapIndexedNotNull { index, marker ->
            val start = marker.range.last + 1
            val end = markers.getOrNull(index + 1)?.range?.first ?: lyric.length
            val text = lyric.substring(start, end)
            if (text.isEmpty()) null else {
                val charStart = contentOffset
                contentOffset += text.length
                LyricWord(
                    text = text,
                    absoluteStartTimeMs = timestampToMs(marker),
                    durationMs = ((markers.getOrNull(index + 1)?.let(::timestampToMs) ?: timestampToMs(marker)) - timestampToMs(marker)).coerceAtLeast(0),
                    charRange = charStart until contentOffset,
                )
            }
        }
    }

    private fun timestampToMs(match: MatchResult): Long {
        val minutes = match.groupValues[1].toLong()
        val seconds = match.groupValues[2].toLong()
        val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        return minutes * 60_000 + seconds * 1_000 + fraction
    }

    private fun insertInterludes(lines: List<LyricLine>): List<LyricLine> = buildList {
        lines.forEachIndexed { index, current ->
            add(current)
            val next = lines.getOrNull(index + 1) ?: return@forEachIndexed
            if (next.startTimeMs - current.startTimeMs > interludeThresholdMs && next.content.isNotEmpty()) {
                add(LyricLine(current.startTimeMs + ((next.startTimeMs - current.startTimeMs) / 3).coerceAtMost(3_500), "♪", isInterlude = true))
            }
        }
    }
}
