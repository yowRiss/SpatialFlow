package com.codetrio.spatialflow.desktop

import com.codetrio.spatialflow.shared.model.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** JVM counterpart of Android's SongDownloader. Direct InnerTube audio URLs are
 * copied to the user's Music directory without invoking a shell or ffmpeg. */
class DesktopSongDownloader(
    private val downloadDirectory: File = File(System.getProperty("user.home"), "Music/SpatialFlow"),
) {
    suspend fun download(song: SongItem): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val source = song.path?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
                ?: error("Only streamed tracks can be downloaded.")
            check(downloadDirectory.exists() || downloadDirectory.mkdirs()) { "Could not create ${downloadDirectory.absolutePath}" }
            val baseName = "${song.artist} - ${song.title}".replace(Regex("[\\\\/:*?\"<>|]"), "_").take(160)
            val destination = File(downloadDirectory, "$baseName.m4a")
            val temporary = File(downloadDirectory, ".$baseName.download")
            URL(source).openConnection().apply { connectTimeout = 20_000; readTimeout = 60_000 }.getInputStream().use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            }
            runCatching {
                Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            }.getOrElse {
                Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            destination
        }
    }
}
