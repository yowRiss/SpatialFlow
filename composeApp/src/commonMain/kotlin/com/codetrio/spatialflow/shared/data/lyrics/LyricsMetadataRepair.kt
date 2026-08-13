package com.codetrio.spatialflow.shared.data.lyrics

/** Shared metadata repair logic used before querying every lyrics provider. */
class LyricsMetadataRepair {
    fun repair(
        rawTitle: String?, rawArtist: String?, album: String?, durationMs: Long,
        filePath: String?, videoId: String? = null,
    ): TrackMetadata {
        var title = rawTitle.orEmpty().trim()
        var artist = rawArtist.orEmpty().trim()
        if (isMissing(title) || isMissing(artist)) {
            val (fromArtist, fromTitle) = extractFromPath(filePath)
            if (isMissing(title)) title = fromTitle
            if (isMissing(artist)) artist = fromArtist
        }
        return TrackMetadata(
            rawTitle = rawTitle.orEmpty(), rawArtist = rawArtist.orEmpty(), album = album.orEmpty(),
            cleanedTitle = cleanTitle(title), cleanedArtist = cleanArtist(artist),
            durationMs = durationMs, filePath = filePath.orEmpty(), videoId = videoId,
            version = detectVersion(title), detectedLanguage = detectLanguage(title, artist),
        )
    }

    private fun extractFromPath(path: String?): Pair<String, String> {
        val fileName = path.orEmpty().substringAfterLast('/').substringAfterLast('\\')
        val name = fileName.substringBeforeLast('.').replace('_', ' ').trim()
        val parts = name.split(Regex("\\s*[-–—]\\s*"), limit = 2)
        return if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to name
    }

    private fun cleanTitle(value: String): String = value
        .replace(emoji, "")
        .replace(noiseParen, "")
        .replace(featParen, "")
        .replace(trailingNoise, "")
        .replace(versionParen, "")
        .replace(Regex("\\s+"), " ").trim().replace(Regex("\\s*[-–—]\\s*$"), "").trim()
        .ifEmpty { value.trim() }

    private fun cleanArtist(value: String): String = value
        .replace(emoji, "")
        .replace(Regex("(?i)\\s*(?:feat\\.?|ft\\.?|featuring|&|,|\\bx\\b|\\bvs\\.?)\\s+.*$"), "")
        .replace(Regex("\\s+"), " ").trim().ifEmpty { value.trim() }

    private fun detectVersion(title: String): String = when {
        Regex("(?:^|\\W)(remix|rmx)(?:\\W|$)", RegexOption.IGNORE_CASE).containsMatchIn(title) -> "remix"
        Regex("(?:^|\\W)(slowed|slowed\\s*\\+\\s*reverb)(?:\\W|$)", RegexOption.IGNORE_CASE).containsMatchIn(title) -> "slowed"
        Regex("(?:^|\\W)(live|concert|unplugged)(?:\\W|$)", RegexOption.IGNORE_CASE).containsMatchIn(title) -> "live"
        Regex("(?:^|\\W)(cover|acoustic\\s*cover)(?:\\W|$)", RegexOption.IGNORE_CASE).containsMatchIn(title) -> "cover"
        Regex("(?:^|\\W)(extended|extended\\s*mix|extended\\s*version)(?:\\W|$)", RegexOption.IGNORE_CASE).containsMatchIn(title) -> "extended"
        Regex("(?:^|\\W)(edit|radio\\s*edit)(?:\\W|$)", RegexOption.IGNORE_CASE).containsMatchIn(title) -> "edit"
        else -> "original"
    }

    private fun detectLanguage(title: String, artist: String): String {
        val value = "$title $artist"
        return when {
            Regex("[\\x{0900}-\\x{097F}]").containsMatchIn(value) -> "hi"
            Regex("[\\x{0B80}-\\x{0BFF}]").containsMatchIn(value) -> "ta"
            Regex("[\\x{0C00}-\\x{0C7F}]").containsMatchIn(value) -> "te"
            Regex("[\\x{0A00}-\\x{0A7F}]").containsMatchIn(value) -> "pa"
            else -> "en"
        }
    }

    private fun isMissing(value: String) = value.isBlank() || value.equals("Unknown Title", true) ||
        value.equals("Unknown Artist", true) || value.equals("<unknown>", true)

    private companion object {
        val noiseParen = Regex("\\s*[(\\[]\\s*(?:official\\s*(?:music\\s*)?video|official\\s*audio|lyrics?\\s*video|lyric|audio|hd|hq|4k|1080p|720p|full\\s*video|visuali[sz]er|animated|amv|mv|m/v|8d\\s*audio|8d|16d|bass\\s*boosted|slowed\\s*(?:\\+\\s*reverb)?|slowed|reverb|nightcore|daycore|instrumental|karaoke|clean|explicit)\\s*[)\\]]", RegexOption.IGNORE_CASE)
        val featParen = Regex("\\s*[(\\[]\\s*(?:feat\\.?|ft\\.?|featuring)\\s+[^)\\]]+[)\\]]", RegexOption.IGNORE_CASE)
        val trailingNoise = Regex("\\s*[-–—|]\\s*(?:official\\s*(?:music\\s*)?video|lyrics?|audio|hd|hq)\\s*$", RegexOption.IGNORE_CASE)
        val versionParen = Regex("(?i)\\s*[(\\[]\\s*(?:remix|rmx|slowed|reverb|live|cover|acoustic|extended|edit|radio\\s*edit|nightcore|daycore|bass\\s*boosted|8d|16d)\\s*[)\\]]")
        val emoji = Regex("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{1F1E0}-\\x{1F1FF}\\x{2600}-\\x{27BF}\\x{FE00}-\\x{FE0F}\\x{1F900}-\\x{1F9FF}\\x{200D}]+")
    }
}
