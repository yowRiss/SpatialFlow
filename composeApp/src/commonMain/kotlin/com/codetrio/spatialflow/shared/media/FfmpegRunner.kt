package com.codetrio.spatialflow.shared.media

data class FfmpegResult(val exitCode: Int, val output: String) {
    val succeeded: Boolean get() = exitCode == 0
}

interface FfmpegRunner {
    suspend fun run(arguments: List<String>): Result<FfmpegResult>
}
