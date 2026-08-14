package com.codetrio.spatialflow.desktop

import androidx.compose.ui.graphics.Color
import java.io.File
import java.net.URI
import java.net.URL
import javax.imageio.ImageIO
import kotlin.math.max

/** Small bounded sampler used by the shared artwork-seeded Material palette. */
internal object DesktopArtworkSeedExtractor {
    fun extract(location: String?): Color? = runCatching {
        val image = when {
            location.isNullOrBlank() -> return null
            location.startsWith("file:") -> ImageIO.read(File(URI(location)))
            location.startsWith("http://") || location.startsWith("https://") -> URL(location).openConnection().run {
                connectTimeout = 10_000
                readTimeout = 15_000
                inputStream.use { input -> ImageIO.read(input) }
            }
            else -> ImageIO.read(File(location))
        } ?: return null
        val horizontalStep = max(1, image.width / 48)
        val verticalStep = max(1, image.height / 48)
        var red = 0L
        var green = 0L
        var blue = 0L
        var weight = 0L
        for (y in 0 until image.height step verticalStep) for (x in 0 until image.width step horizontalStep) {
            val pixel = image.getRGB(x, y)
            val alpha = pixel ushr 24 and 0xFF
            if (alpha < 32) continue
            red += pixel shr 16 and 0xFF
            green += pixel shr 8 and 0xFF
            blue += pixel and 0xFF
            weight++
        }
        if (weight == 0L) null else Color(red.toFloat() / weight / 255f, green.toFloat() / weight / 255f, blue.toFloat() / weight / 255f)
    }.getOrNull()
}
