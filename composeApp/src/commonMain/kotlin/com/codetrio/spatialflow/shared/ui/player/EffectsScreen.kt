package com.codetrio.spatialflow.shared.ui.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.shared.player.PlaybackController

/** Desktop equivalent of Android's effects controls; unsupported Android-only
 * effects are not shown as working controls. */
@Composable
fun EffectsScreen(controller: PlaybackController) {
    var normalization by remember { mutableStateOf(false) }
    var bass by remember { mutableStateOf(false) }; var bassDb by remember { mutableStateOf(0f) }
    var loudness by remember { mutableStateOf(false) }; var loudnessDb by remember { mutableStateOf(0f) }
    var crossfade by remember { mutableStateOf(0f) }; var speed by remember { mutableStateOf(1f) }
    var bands by remember { mutableStateOf(List(5) { 0f }) }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Effects", style = MaterialTheme.typography.headlineMedium)
        Toggle("Volume normalization", normalization) { normalization = it; controller.setVolumeNormalization(it) }
        Toggle("Bass boost", bass) { bass = it; controller.setBassBoost(bassDb, it) }; EffectSlider("Bass", bassDb, -12f..12f) { bassDb = it; controller.setBassBoost(it, bass) }
        Toggle("Loudness enhancer", loudness) { loudness = it; controller.setLoudnessGain(loudnessDb, it) }; EffectSlider("Loudness", loudnessDb, -12f..12f) { loudnessDb = it; controller.setLoudnessGain(it, loudness) }
        Text("Equalizer", style = MaterialTheme.typography.titleLarge)
        bands.forEachIndexed { index, value -> EffectSlider("Band ${index + 1}", value, -12f..12f) { gain -> bands = bands.toMutableList().apply { set(index, gain) }; controller.setEqualizer(bands) } }
        EffectSlider("Crossfade", crossfade, 0f..12f) { crossfade = it; controller.setCrossfadeDuration((it * 1_000).toLong()) }
        EffectSlider("Speed", speed, .5f..2f) { speed = it; controller.setPlaybackSpeed(it) }
    }
}
@Composable private fun Toggle(title: String, checked: Boolean, update: (Boolean) -> Unit) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.fillMaxWidth()); Switch(checked, update) }
@Composable private fun EffectSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, update: (Float) -> Unit) { Text("$title: ${"%.1f".format(value)}"); Slider(value, update, valueRange = range) }
