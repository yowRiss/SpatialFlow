package com.codetrio.spatialflow.shared.media

/** Shared FFmpeg argument construction. Platform actuals only execute the command. */
object FfmpegCommands {
    fun spatial8d(inputPath: String, outputPath: String, rotationSpeed: Float): List<String> {
        val speed = rotationSpeed.coerceIn(.03f, .25f)
        val formattedSpeed = ((speed * 1_000).toInt() / 1_000.0).toString()
        val filter = "apulsator=hz=$formattedSpeed:width=0.75:mode=sine,aecho=0.6:0.4:30|60:0.2|0.15,stereotools=balance_in=0.02,alimiter=limit=0.97"
        return listOf("-y", "-hide_banner", "-loglevel", "error", "-threads", "0", "-i", inputPath, "-vn", "-map", "0:a:0", "-af", filter, "-c:a", "aac", "-b:a", "192k", "-ar", "44100", "-ac", "2", "-movflags", "+faststart", "-map_metadata", "0", outputPath)
    }
}
