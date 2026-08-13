package com.codetrio.spatialflow.shared.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Executes a trusted, internally-built FFmpeg command; no shell is invoked. */
class DesktopFfmpegRunner(private val executable: String = "ffmpeg") : FfmpegRunner {
    override suspend fun run(arguments: List<String>): Result<FfmpegResult> = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder(listOf(executable) + arguments).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            FfmpegResult(process.waitFor(), output)
        }
    }
}
