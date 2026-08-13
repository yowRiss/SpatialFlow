package com.codetrio.spatialflow.shared.library

import com.codetrio.spatialflow.shared.model.SongItem
import java.io.File
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey

/** JVM scanner equivalent to Android's MediaStore-backed local-library source. */
class DesktopLocalMusicLibrary : LocalMusicLibrary {
    override suspend fun scan(rootPath: String): Result<List<SongItem>> = runCatching {
        val root = File(rootPath)
        require(root.isDirectory) { "Music directory does not exist: $rootPath" }
        root.walkTopDown().onEnter { !it.isHidden }
            .filter { it.isFile && it.extension.lowercase() in supportedExtensions }
            .map(::readSong).sortedBy { it.title.lowercase() }.toList()
    }

    private fun readSong(file: File): SongItem {
        val tag = runCatching { AudioFileIO.read(file).tag }.getOrNull()
        val fallback = file.nameWithoutExtension
        val split = fallback.split(" - ", limit = 2)
        val title = tag?.getFirst(FieldKey.TITLE).orEmpty().ifBlank { split.last() }
        val artist = tag?.getFirst(FieldKey.ARTIST).orEmpty().ifBlank { split.getOrNull(0)?.takeIf { split.size > 1 } ?: "Unknown Artist" }
        val duration = runCatching { AudioFileIO.read(file).audioHeader.trackLength.toLong() * 1_000 }.getOrDefault(0)
        return SongItem.local(file.absolutePath.hashCode().toLong(), title, artist, -1, file.absolutePath, duration, file.lastModified())
    }

    private companion object {
        val supportedExtensions = setOf("mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "aiff", "aif")
    }
}
