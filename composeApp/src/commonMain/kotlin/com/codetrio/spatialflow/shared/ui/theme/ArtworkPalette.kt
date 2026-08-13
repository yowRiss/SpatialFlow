package com.codetrio.spatialflow.shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

/** Platform-neutral equivalent of the Android album-art theme transformation. */
fun ColorScheme.withArtworkSeed(seed: Color, darkTheme: Boolean, amoledBlack: Boolean): ColorScheme {
    val hsl = seed.toHsl()
    val monochrome = hsl.saturation < .06f
    fun tone(hue: Float = hsl.hue, saturation: Float, lightness: Float) = Color.fromHsl(hue, if (monochrome) 0f else saturation, lightness)
    val baseSaturation = if (monochrome) 0f else hsl.saturation
    val backgroundSaturation = min(baseSaturation * .15f, if (darkTheme) .1f else .15f)
    val primarySaturation = max(baseSaturation, .5f)
    val secondarySaturation = (baseSaturation * .5f).coerceIn(.2f, .35f)
    val tertiarySaturation = (baseSaturation * .6f).coerceIn(.3f, .5f)
    val themed = if (darkTheme) copy(
        background = tone(saturation = backgroundSaturation, lightness = .04f), onBackground = tone(saturation = backgroundSaturation, lightness = .9f),
        surface = tone(saturation = backgroundSaturation, lightness = .06f), onSurface = tone(saturation = backgroundSaturation, lightness = .9f),
        surfaceContainerLow = tone(saturation = backgroundSaturation, lightness = .08f), surfaceContainer = tone(saturation = backgroundSaturation, lightness = .12f), surfaceContainerHigh = tone(saturation = backgroundSaturation, lightness = .16f), surfaceContainerHighest = tone(saturation = backgroundSaturation, lightness = .22f),
        surfaceVariant = tone(saturation = backgroundSaturation, lightness = .28f), onSurfaceVariant = tone(saturation = backgroundSaturation, lightness = .8f),
        primary = tone(saturation = primarySaturation, lightness = .75f), onPrimary = tone(saturation = primarySaturation, lightness = .2f), primaryContainer = tone(saturation = max(baseSaturation, .4f), lightness = .25f), onPrimaryContainer = tone(saturation = max(baseSaturation, .4f), lightness = .9f),
        secondary = tone(saturation = secondarySaturation, lightness = .7f), tertiary = tone((hsl.hue + 60f) % 360f, tertiarySaturation, .7f),
    ) else copy(
        background = tone(saturation = backgroundSaturation, lightness = .98f), onBackground = tone(saturation = backgroundSaturation, lightness = .1f),
        surface = tone(saturation = backgroundSaturation, lightness = .98f), onSurface = tone(saturation = backgroundSaturation, lightness = .1f),
        surfaceContainerLow = tone(saturation = backgroundSaturation, lightness = .96f), surfaceContainer = tone(saturation = backgroundSaturation, lightness = .94f), surfaceContainerHigh = tone(saturation = backgroundSaturation, lightness = .9f), surfaceContainerHighest = tone(saturation = backgroundSaturation, lightness = .86f),
        primary = tone(saturation = primarySaturation, lightness = .4f), onPrimary = tone(saturation = primarySaturation, lightness = .95f), primaryContainer = tone(saturation = max(baseSaturation, .4f), lightness = .85f), onPrimaryContainer = tone(saturation = max(baseSaturation, .4f), lightness = .1f),
        secondary = tone(saturation = secondarySaturation, lightness = .45f), tertiary = tone((hsl.hue + 60f) % 360f, tertiarySaturation, .45f),
    )
    return if (darkTheme && amoledBlack) themed.copy(background = Color.Black, surface = Color.Black, surfaceContainerLowest = Color.Black, surfaceContainerLow = Color.Black, surfaceContainer = Color.Black) else themed
}

private data class Hsl(val hue: Float, val saturation: Float, val lightness: Float)
private fun Color.toHsl(): Hsl {
    val max = max(red, max(green, blue)); val min = min(red, min(green, blue)); val delta = max - min; val lightness = (max + min) / 2
    val saturation = if (delta == 0f) 0f else delta / (1 - kotlin.math.abs(2 * lightness - 1))
    val hue = when { delta == 0f -> 0f; max == red -> 60 * (((green - blue) / delta) % 6); max == green -> 60 * ((blue - red) / delta + 2); else -> 60 * ((red - green) / delta + 4) }
    return Hsl(if (hue < 0) hue + 360 else hue, saturation, lightness)
}
private fun Color.Companion.fromHsl(hue: Float, saturation: Float, lightness: Float): Color {
    val chroma = (1 - kotlin.math.abs(2 * lightness - 1)) * saturation; val x = chroma * (1 - kotlin.math.abs((hue / 60) % 2 - 1)); val m = lightness - chroma / 2
    val (r, g, b) = when (hue) { in 0f..<60f -> Triple(chroma, x, 0f); in 60f..<120f -> Triple(x, chroma, 0f); in 120f..<180f -> Triple(0f, chroma, x); in 180f..<240f -> Triple(0f, x, chroma); in 240f..<300f -> Triple(x, 0f, chroma); else -> Triple(chroma, 0f, x) }
    return Color(r + m, g + m, b + m)
}
