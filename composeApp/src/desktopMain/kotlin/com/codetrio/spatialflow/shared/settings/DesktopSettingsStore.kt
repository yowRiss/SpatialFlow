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
        mutableSettings.value = next
    }

    private fun read() = SpatialFlowSettings(
        amoledBlack = preferences.getBoolean("amoled_black", false),
        dynamicAlbumTheme = preferences.getBoolean("dynamic_album_theme", true),
        normalizeVolume = preferences.getBoolean("normalize_volume", true),
        scanOnLaunch = preferences.getBoolean("scan_on_launch", true),
    )
}
