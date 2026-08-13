package com.codetrio.spatialflow.shared.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.codetrio.spatialflow.shared.settings.InMemorySettingsStore
import com.codetrio.spatialflow.shared.settings.SettingsStore

enum class SettingsSection(val label: String) {
    Appearance("Appearance"),
    Playback("Playback"),
    Library("Library"),
}

data class SettingsUiState(
    val section: SettingsSection = SettingsSection.Appearance,
    val amoledBlack: Boolean = false,
    val dynamicAlbumTheme: Boolean = true,
    val normalizeVolume: Boolean = true,
    val scanOnLaunch: Boolean = true,
)

/** Shared lifecycle-aware presentation logic; no Android APIs are used here. */
class SettingsViewModel(private val store: SettingsStore = InMemorySettingsStore()) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            amoledBlack = store.settings.value.amoledBlack,
            dynamicAlbumTheme = store.settings.value.dynamicAlbumTheme,
            normalizeVolume = store.settings.value.normalizeVolume,
            scanOnLaunch = store.settings.value.scanOnLaunch,
        ),
    )
    val uiState = _uiState.asStateFlow()

    fun selectSection(section: SettingsSection) = update { copy(section = section) }
    fun toggleAmoledBlack() {
        update { copy(amoledBlack = !amoledBlack) }
        store.update { copy(amoledBlack = _uiState.value.amoledBlack) }
    }
    fun toggleDynamicAlbumTheme() {
        update { copy(dynamicAlbumTheme = !dynamicAlbumTheme) }
        store.update { copy(dynamicAlbumTheme = _uiState.value.dynamicAlbumTheme) }
    }
    fun toggleNormalizeVolume() {
        update { copy(normalizeVolume = !normalizeVolume) }
        store.update { copy(normalizeVolume = _uiState.value.normalizeVolume) }
    }
    fun toggleScanOnLaunch() {
        update { copy(scanOnLaunch = !scanOnLaunch) }
        store.update { copy(scanOnLaunch = _uiState.value.scanOnLaunch) }
    }

    private inline fun update(transform: SettingsUiState.() -> SettingsUiState) {
        _uiState.update(transform)
    }
}
