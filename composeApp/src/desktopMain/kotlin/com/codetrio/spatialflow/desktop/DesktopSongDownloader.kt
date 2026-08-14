package com.codetrio.spatialflow.desktop

import com.codetrio.spatialflow.shared.model.SongItem
import com.codetrio.spatialflow.shared.media.FfmpegRunner
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** JVM counterpart of Android's SongDownloader. It produces a valid tagged
 * M4A file regardless of the source stream's native container/codec. */
class DesktopSongDownloader(
    private val ffmpeg: FfmpegRunner,
    private val downloadDirectory: File = File(System.getProperty("user.home"), "Music/SpatialFlow"),
) {
    suspend fun download(song: SongItem): Result<File> = try {
        val source = song.path?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
            ?: error("Only streamed tracks can be downloaded.")
        check(downloadDirectory.exists() || downloadDirectory.mkdirs()) { "Could not create ${downloadDirectory.absolutePath}" }
        val baseName = "${song.artist} - ${song.title}".replace(Regex("[\\\\/:*?\"<>|]"), "_").take(160)
        val destination = File(downloadDirectory, "$baseName.m4a")
        val temporary = File(downloadDirectory, ".$baseName.partial.m4a")
        temporary.delete()
        val result = ffmpeg.run(
            listOf(
                "-y", "-i", source, "-vn", "-c:a", "aac", "-b:a", "192k",
                "-metadata", "title=${song.title}", "-metadata", "artist=${song.artist}",
                "-movflags", "+faststart", temporary.absolutePath,
            ),
        ).getOrElse { throw it }
        check(result.succeeded && temporary.isFile && temporary.length() > 0) {
            "FFmpeg could not create the offline track: ${result.output.takeLast(400)}"
        }
        runCatching {
            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }.getOrElse {
            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        Result.success(destination)
    } catch (error: Throwable) {
        Result.failure(error)
    }
}
