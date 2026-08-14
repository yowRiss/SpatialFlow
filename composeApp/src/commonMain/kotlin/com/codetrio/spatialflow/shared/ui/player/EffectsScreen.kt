package com.codetrio.spatialflow.shared.ui.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.shared.player.PlaybackController
import com.codetrio.spatialflow.shared.settings.SettingsStore

/** Desktop equivalent of Android's effects controls; unsupported Android-only
 * effects are not shown as working controls. */
@Composable
fun EffectsScreen(controller: PlaybackController, settings: SettingsStore) {
    val appSettings by settings.settings.collectAsState()
    val effects = appSettings.effects
    LaunchedEffect(appSettings) {
        controller.setVolumeNormalization(appSettings.normalizeVolume)
        controller.setBassBoost(effects.bassDb, effects.bassEnabled); controller.setLoudnessGain(effects.loudnessDb, effects.loudnessEnabled)
        controller.setEqualizer(effects.equalizerBands); controller.setCrossfadeDuration(effects.crossfadeMs); controller.setPlaybackSpeed(effects.playbackSpeed)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("Effects", style = MaterialTheme.typography.headlineMedium)
        Toggle("Volume normalization", appSettings.normalizeVolume) { settings.update { copy(normalizeVolume = it) } }
        Toggle("Bass boost", effects.bassEnabled) { settings.update { copy(effects = effects.copy(bassEnabled = it)) } }; EffectSlider("Bass", effects.bassDb, -12f..12f) { settings.update { copy(effects = effects.copy(bassDb = it)) } }
        Toggle("Loudness enhancer", effects.loudnessEnabled) { settings.update { copy(effects = effects.copy(loudnessEnabled = it)) } }; EffectSlider("Loudness", effects.loudnessDb, -12f..12f) { settings.update { copy(effects = effects.copy(loudnessDb = it)) } }
        Text("Equalizer", style = MaterialTheme.typography.titleLarge)
        effects.equalizerBands.forEachIndexed { index, value -> EffectSlider("Band ${index + 1}", value, -12f..12f) { gain -> settings.update { copy(effects = effects.copy(equalizerBands = effects.equalizerBands.toMutableList().apply { set(index, gain) })) } } }
        EffectSlider("Crossfade", effects.crossfadeMs / 1_000f, 0f..12f) { settings.update { copy(effects = effects.copy(crossfadeMs = (it * 1_000).toLong())) } }
        EffectSlider("Speed", effects.playbackSpeed, .5f..2f) { settings.update { copy(effects = effects.copy(playbackSpeed = it)) } }
    }
}
@Composable private fun Toggle(title: String, checked: Boolean, update: (Boolean) -> Unit) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.fillMaxWidth()); Switch(checked, update) }
@Composable private fun EffectSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, update: (Float) -> Unit) { Text("$title: ${"%.1f".format(value)}"); Slider(value, update, valueRange = range) }
