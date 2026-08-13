package com.codetrio.spatialflow.shared.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.codetrio.spatialflow.shared.ui.theme.Error
import com.codetrio.spatialflow.shared.ui.theme.ErrorContainer
import com.codetrio.spatialflow.shared.ui.theme.OnError
import com.codetrio.spatialflow.shared.ui.theme.OnErrorContainer
import com.codetrio.spatialflow.shared.ui.theme.OnPrimary
import com.codetrio.spatialflow.shared.ui.theme.OnPrimaryContainer
import com.codetrio.spatialflow.shared.ui.theme.OnSecondary
import com.codetrio.spatialflow.shared.ui.theme.OnSecondaryContainer
import com.codetrio.spatialflow.shared.ui.theme.OnSurface
import com.codetrio.spatialflow.shared.ui.theme.OnSurfaceVariant
import com.codetrio.spatialflow.shared.ui.theme.OnTertiary
import com.codetrio.spatialflow.shared.ui.theme.OnTertiaryContainer
import com.codetrio.spatialflow.shared.ui.theme.Outline
import com.codetrio.spatialflow.shared.ui.theme.OutlineVariant
import com.codetrio.spatialflow.shared.ui.theme.Primary
import com.codetrio.spatialflow.shared.ui.theme.PrimaryContainer
import com.codetrio.spatialflow.shared.ui.theme.Secondary
import com.codetrio.spatialflow.shared.ui.theme.SecondaryContainer
import com.codetrio.spatialflow.shared.ui.theme.Surface
import com.codetrio.spatialflow.shared.ui.theme.SurfaceContainer
import com.codetrio.spatialflow.shared.ui.theme.SurfaceContainerHigh
import com.codetrio.spatialflow.shared.ui.theme.SurfaceContainerHighest
import com.codetrio.spatialflow.shared.ui.theme.SurfaceContainerLow
import com.codetrio.spatialflow.shared.ui.theme.SurfaceVariant
import com.codetrio.spatialflow.shared.ui.theme.Tertiary
import com.codetrio.spatialflow.shared.ui.theme.TertiaryContainer
import com.codetrio.spatialflow.shared.ui.theme.platformDynamicColorScheme
import com.codetrio.spatialflow.shared.ui.theme.spatialFlowTypography
import com.codetrio.spatialflow.shared.ui.theme.withArtworkSeed

private val fallbackDarkScheme = darkColorScheme(
    primary = Primary, onPrimary = OnPrimary, primaryContainer = PrimaryContainer, onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary, onSecondary = OnSecondary, secondaryContainer = SecondaryContainer, onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary, onTertiary = OnTertiary, tertiaryContainer = TertiaryContainer, onTertiaryContainer = OnTertiaryContainer,
    background = Surface, onBackground = OnSurface, surface = Surface, onSurface = OnSurface,
    surfaceVariant = SurfaceVariant, onSurfaceVariant = OnSurfaceVariant, outline = Outline, outlineVariant = OutlineVariant,
    error = Error, onError = OnError, errorContainer = ErrorContainer, onErrorContainer = OnErrorContainer,
    surfaceContainerLow = SurfaceContainerLow, surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh, surfaceContainerHighest = SurfaceContainerHighest,
)

/**
 * Shared counterpart of the Android root theme. Platform dynamic colour is
 * supplied through an expect/actual; all app brand tokens and typography live
 * in commonMain.
 */
@Composable
fun SpatialFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledBlack: Boolean = false,
    usePlatformDynamicColor: Boolean = true,
    artworkSeed: Color? = null,
    content: @Composable () -> Unit,
) {
    val platformScheme = if (usePlatformDynamicColor) platformDynamicColorScheme(darkTheme) else null
    val baseScheme = platformScheme ?: if (darkTheme) fallbackDarkScheme else lightColorScheme()
    val colorScheme = remember(baseScheme, darkTheme, amoledBlack, artworkSeed) {
        val artworkScheme = artworkSeed?.let { baseScheme.withArtworkSeed(it, darkTheme, amoledBlack) }
        artworkScheme ?: if (darkTheme && amoledBlack) baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerHigh = Color(0xFF0D0D0D),
            surfaceContainerHighest = Color(0xFF141414),
        ) else baseScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = spatialFlowTypography(), content = content)
}

/** Compatibility entry point for the existing desktop scaffold. */
@Composable
fun SharedTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    SpatialFlowTheme(darkTheme = darkTheme, content = content)
}
