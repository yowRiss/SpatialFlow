package com.codetrio.spatialflow.shared.library

import java.io.File
import java.nio.file.Files
import org.jaudiotagger.tag.Tag

/** Extracts embedded covers once, then exposes a stable file URI for Coil. */
object DesktopArtworkCache {
    private val directory = File(System.getProperty("user.home"), ".cache/SpatialFlow/artwork")

    fun extract(tag: Tag?, audioFile: File): String? = runCatching {
        val artwork = tag?.firstArtwork ?: return null
        val bytes = artwork.binaryData.takeIf { it.isNotEmpty() } ?: return null
        val extension = when (artwork.mimeType?.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        if (!directory.exists()) check(directory.mkdirs()) { "Could not create artwork cache" }
        val cacheKey = "${audioFile.absolutePath.hashCode().toUInt().toString(16)}-${audioFile.lastModified()}.$extension"
        val cached = File(directory, cacheKey)
        if (!cached.exists() || cached.length() == 0L) {
            val temporary = File(directory, ".$cacheKey.tmp")
            Files.write(temporary.toPath(), bytes)
            check(temporary.renameTo(cached)) { "Could not cache embedded artwork" }
        }
        cached.toURI().toString()
    }.getOrNull()
}
