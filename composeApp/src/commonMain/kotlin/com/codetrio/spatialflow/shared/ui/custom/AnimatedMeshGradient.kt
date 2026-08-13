package com.codetrio.spatialflow.shared.ui.custom

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import kotlin.math.sin

/** Compose Canvas replacement for Android's AnimatedMeshGradientView. */
@Composable
fun AnimatedMeshGradient(modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    val transition = rememberInfiniteTransition("mesh")
    val phase = transition.animateFloat(0f, (Math.PI * 2).toFloat(), infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart), "phase")
    val surface = MaterialTheme.colorScheme.surface
    val tertiary = MaterialTheme.colorScheme.tertiary
    val secondary = MaterialTheme.colorScheme.secondary
    Canvas(modifier.fillMaxSize()) {
        drawRect(surface)
        val largest = maxOf(size.width, size.height)
        fun orb(x: Float, y: Float, color: Color) = drawCircle(Brush.radialGradient(listOf(color.copy(alpha = .25f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(x, y), radius = largest * .65f), radius = largest * .65f, center = androidx.compose.ui.geometry.Offset(x, y))
        orb(size.width * (.15f + sin(phase.value).toFloat() * .08f), size.height * .18f, accent)
        orb(size.width * (.82f + sin(phase.value + 2f).toFloat() * .07f), size.height * .35f, tertiary)
        orb(size.width * .48f, size.height * (.84f + sin(phase.value + 4f).toFloat() * .06f), secondary)
    }
}
