package com.codetrio.spatialflow.shared.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun selectSection(section: SettingsSection) = update { copy(section = section) }
    fun toggleAmoledBlack() = update { copy(amoledBlack = !amoledBlack) }
    fun toggleDynamicAlbumTheme() = update { copy(dynamicAlbumTheme = !dynamicAlbumTheme) }
    fun toggleNormalizeVolume() = update { copy(normalizeVolume = !normalizeVolume) }
    fun toggleScanOnLaunch() = update { copy(scanOnLaunch = !scanOnLaunch) }

    private inline fun update(transform: SettingsUiState.() -> SettingsUiState) {
        _uiState.update(transform)
    }
}
