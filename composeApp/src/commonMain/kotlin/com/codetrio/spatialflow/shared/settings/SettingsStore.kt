package com.codetrio.spatialflow.shared.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SpatialFlowSettings(
    val amoledBlack: Boolean = false,
    val dynamicAlbumTheme: Boolean = true,
    val normalizeVolume: Boolean = true,
    val scanOnLaunch: Boolean = true,
)

/** Platform adapters persist this state with SharedPreferences or JVM Preferences. */
interface SettingsStore {
    val settings: StateFlow<SpatialFlowSettings>
    fun update(transform: SpatialFlowSettings.() -> SpatialFlowSettings)
}

class InMemorySettingsStore(initial: SpatialFlowSettings = SpatialFlowSettings()) : SettingsStore {
    private val mutableSettings = MutableStateFlow(initial)
    override val settings: StateFlow<SpatialFlowSettings> = mutableSettings
    override fun update(transform: SpatialFlowSettings.() -> SpatialFlowSettings) {
        mutableSettings.value = mutableSettings.value.transform()
    }
}
