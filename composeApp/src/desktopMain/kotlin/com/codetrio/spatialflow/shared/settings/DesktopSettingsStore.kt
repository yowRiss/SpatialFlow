package com.codetrio.spatialflow.shared.settings

import java.util.prefs.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DesktopSettingsStore(private val preferences: Preferences = Preferences.userRoot().node("com/codetrio/spatialflow")) : SettingsStore {
    private val mutableSettings = MutableStateFlow(read())
    override val settings: StateFlow<SpatialFlowSettings> = mutableSettings

    override fun update(transform: SpatialFlowSettings.() -> SpatialFlowSettings) {
        val next = mutableSettings.value.transform()
        preferences.putBoolean("amoled_black", next.amoledBlack)
        preferences.putBoolean("dynamic_album_theme", next.dynamicAlbumTheme)
        preferences.putBoolean("normalize_volume", next.normalizeVolume)
        preferences.putBoolean("scan_on_launch", next.scanOnLaunch)
        preferences.putBoolean("effects_bass_enabled", next.effects.bassEnabled)
        preferences.putFloat("effects_bass_db", next.effects.bassDb)
        preferences.putBoolean("effects_loudness_enabled", next.effects.loudnessEnabled)
        preferences.putFloat("effects_loudness_db", next.effects.loudnessDb)
        preferences.put("effects_equalizer", next.effects.equalizerBands.joinToString(","))
        preferences.putLong("effects_crossfade_ms", next.effects.crossfadeMs)
        preferences.putFloat("effects_speed", next.effects.playbackSpeed)
        mutableSettings.value = next
    }

    private fun read() = SpatialFlowSettings(
        amoledBlack = preferences.getBoolean("amoled_black", false),
        dynamicAlbumTheme = preferences.getBoolean("dynamic_album_theme", true),
        normalizeVolume = preferences.getBoolean("normalize_volume", true),
        scanOnLaunch = preferences.getBoolean("scan_on_launch", true),
        effects = PlaybackEffectsSettings(
            bassEnabled = preferences.getBoolean("effects_bass_enabled", false),
            bassDb = preferences.getFloat("effects_bass_db", 0f),
            loudnessEnabled = preferences.getBoolean("effects_loudness_enabled", false),
            loudnessDb = preferences.getFloat("effects_loudness_db", 0f),
            equalizerBands = preferences.get("effects_equalizer", "").split(',').mapNotNull(String::toFloatOrNull).take(10).let { if (it.size == 10) it else List(10) { 0f } },
            crossfadeMs = preferences.getLong("effects_crossfade_ms", 0L),
            playbackSpeed = preferences.getFloat("effects_speed", 1f),
        ),
    )
}
