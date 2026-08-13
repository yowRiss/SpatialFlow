package com.codetrio.spatialflow.shared.settings

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidSettingsStore(private val preferences: SharedPreferences) : SettingsStore {
    private val mutableSettings = MutableStateFlow(read())
    override val settings: StateFlow<SpatialFlowSettings> = mutableSettings

    override fun update(transform: SpatialFlowSettings.() -> SpatialFlowSettings) {
        val next = mutableSettings.value.transform()
        preferences.edit().putBoolean("amoled_black", next.amoledBlack)
            .putBoolean("dynamic_album_theme", next.dynamicAlbumTheme)
            .putBoolean("normalize_volume", next.normalizeVolume)
            .putBoolean("scan_on_launch", next.scanOnLaunch).apply()
        mutableSettings.value = next
    }

    private fun read() = SpatialFlowSettings(
        amoledBlack = preferences.getBoolean("amoled_black", false),
        dynamicAlbumTheme = preferences.getBoolean("dynamic_album_theme", true),
        normalizeVolume = preferences.getBoolean("normalize_volume", true),
        scanOnLaunch = preferences.getBoolean("scan_on_launch", true),
    )
}
