package com.codetrio.spatialflow.shared.player

import com.codetrio.spatialflow.shared.model.SongItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Transitional Android actual. The existing :app Media3 service remains the
 * production player while the KMP Android entry point is migrated.
 */
private class AndroidMigrationPlaybackController : PlaybackController {
    private val mutableState = MutableStateFlow(PlayerUiState())
    override val state: StateFlow<PlayerUiState> = mutableState
    override fun dispatch(command: PlayerCommand) = Unit
    override fun setQueue(songs: List<SongItem>, startIndex: Int) {
        mutableState.value = mutableState.value.copy(currentSong = songs.getOrNull(startIndex), currentSongIndex = startIndex)
    }
    override fun setVolumeNormalization(enabled: Boolean) = Unit
    override fun setEqualizer(bands: List<Float>, preamp: Float) = Unit
    override fun setBassBoost(gainDb: Float, enabled: Boolean) = Unit
    override fun setLoudnessGain(gainDb: Float, enabled: Boolean) = Unit
    override fun setPlaybackSpeed(speed: Float) = Unit
    override fun setCrossfadeDuration(durationMs: Long) = Unit
    override fun release() = Unit
}

actual fun createPlaybackController(): PlaybackController = AndroidMigrationPlaybackController()
