package com.codetrio.spatialflow.desktop

import com.codetrio.spatialflow.shared.data.lyrics.LyricLine
import com.codetrio.spatialflow.shared.data.lyrics.SharedLrcParser
import com.codetrio.spatialflow.shared.model.SongItem
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

/** Reads embedded SYLT/USLT-equivalent text exposed by jaudiotagger before the
 * network providers are queried. Plain embedded lyrics receive stable lines. */
object DesktopEmbeddedLyrics {
    fun read(song: SongItem): List<LyricLine> {
        val file = song.path?.let(::File)?.takeIf(File::isFile) ?: return emptyList()
        val raw = runCatching { AudioFileIO.read(file).tag?.getFirst(FieldKey.LYRICS) }
            .getOrNull()?.trim().orEmpty()
        if (raw.isBlank()) return emptyList()
        return SharedLrcParser.parse(raw).ifEmpty {
            raw.lineSequence().filter(String::isNotBlank).mapIndexed { index, line ->
                LyricLine(index * 4_000L, line.trim())
            }.toList()
        }
    }
}
