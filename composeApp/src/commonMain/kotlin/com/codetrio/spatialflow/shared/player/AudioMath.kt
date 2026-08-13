package com.codetrio.spatialflow.shared.player

import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/** Platform-independent gain and PCM loudness calculations. */
object AudioMath {
    const val targetLufs = -14f

    fun calculateGain(measuredLufs: Float, target: Float = targetLufs): Float = (target - measuredLufs).coerceIn(-15f, 15f)
    fun dbToLinear(db: Float): Float = 10.0.pow(db / 20.0).toFloat()

    /** Fast RMS LUFS estimate for a decoded PCM16 sample window. */
    fun estimateLufs(samples: ShortArray): Float? {
        if (samples.isEmpty()) return null
        var sumSquares = 0.0
        samples.forEach { sample ->
            val normalized = sample.toDouble() / Short.MAX_VALUE
            sumSquares += normalized * normalized
        }
        val rms = sqrt(sumSquares / samples.size)
        return if (rms == 0.0) null else (20 * log10(rms) + 3.0).toFloat()
    }
}
