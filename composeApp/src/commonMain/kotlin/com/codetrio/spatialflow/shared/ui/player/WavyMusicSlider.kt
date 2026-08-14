package com.codetrio.spatialflow.shared.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** Shared desktop-safe port of Android's animated wavy seek track. */
@Composable
fun WavyMusicSlider(value: Float, onValueChange: (Float) -> Unit, valueRange: ClosedFloatingPointRange<Float>, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val normalized = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start).coerceAtLeast(.0001f)).coerceIn(0f, 1f)
    val progress by animateFloatAsState(normalized, tween(180), label = "wavyProgress")
    val transition = rememberInfiniteTransition("wave")
    val phase = transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "wavePhase").value
    val primary = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.height(28.dp)) {
        Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            val y = size.height / 2f
            val start = 10.dp.toPx(); val end = size.width - start
            val current = start + (end - start) * progress
            drawLine(inactive, Offset(current, y), Offset(end, y), 4.dp.toPx(), StrokeCap.Round)
            if (current > start) {
                val path = Path(); path.moveTo(start, y)
                var x = start
                while (x < current) {
                    val wave = if (isPlaying) sin(x / 48.dp.toPx() * (2 * PI).toFloat() + phase) * 2.5.dp.toPx() else 0f
                    path.lineTo(x, y + wave); x += 3f
                }
                path.lineTo(current, y)
                drawPath(path, Brush.horizontalGradient(listOf(primary.copy(alpha = .35f), primary), startX = start, endX = current), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
            }
            drawCircle(primary, 8.dp.toPx(), Offset(current, y))
        }
        Slider(value, onValueChange, Modifier.fillMaxWidth().height(28.dp), valueRange = valueRange, interactionSource = interaction, colors = SliderDefaults.colors(thumbColor = androidx.compose.ui.graphics.Color.Transparent, activeTrackColor = androidx.compose.ui.graphics.Color.Transparent, inactiveTrackColor = androidx.compose.ui.graphics.Color.Transparent))
    }
}
