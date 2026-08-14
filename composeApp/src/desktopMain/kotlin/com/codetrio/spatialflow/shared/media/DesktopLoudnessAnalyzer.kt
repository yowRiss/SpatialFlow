package com.codetrio.spatialflow.shared.media

/** Measures integrated EBU R128 loudness using the existing trusted FFmpeg runner. */
class DesktopLoudnessAnalyzer(private val ffmpeg: FfmpegRunner) {
    suspend fun analyze(location: String): Float? {
        val result = ffmpeg.run(
            listOf("-hide_banner", "-nostats", "-i", location, "-filter:a", "ebur128=framelog=verbose", "-f", "null", "-"),
        ).getOrNull()?.takeIf { it.succeeded } ?: return null
        return integratedLufs.findAll(result.output).lastOrNull()?.groupValues?.getOrNull(1)?.toFloatOrNull()
    }

    private companion object {
        val integratedLufs = Regex("""\bI:\s*(-?\d+(?:\.\d+)?)\s*LUFS""")
    }
}
