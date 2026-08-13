package com.codetrio.spatialflow.shared.player

import androidx.compose.runtime.Immutable
import com.codetrio.spatialflow.shared.model.SongItem

/** Shared player presentation state; platform engines map their state into it. */
@Immutable
data class PlayerUiState(
    val currentSong: SongItem? = null,
    val isPlaying: Boolean = false,
    val duration: Int = 0,
    val positionMs: Long = 0,
    val isProcessing: Boolean = false,
    val currentSongIndex: Int = -1,
    val isHapticsEnabled: Boolean = false,
    val miniPlayerBlendColor: Int = 0,
    val isCurrentSongFavorite: Boolean = false,
    val isCurrentSongDisliked: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffleEnabled: Boolean = false,
    val playerBackgroundColor: Int = 0xFF0F0F0F.toInt(),
    val likesCount: String = "Like",
    val isCurrentSongDownloaded: Boolean = false,
    val currentSongDownloadProgress: Int? = null,
)

enum class RepeatMode { OFF, ALL, ONE }

enum class SleepTimerMode { OFF, CUSTOM, END_OF_SONG, END_OF_QUEUE }

sealed interface PlayerCommand {
    data object TogglePlayback : PlayerCommand
    data object Next : PlayerCommand
    data object Previous : PlayerCommand
    data class SeekTo(val positionMs: Long) : PlayerCommand
    data class PlayAt(val queueIndex: Int) : PlayerCommand
    data class ReorderQueue(val fromIndex: Int, val toIndex: Int) : PlayerCommand
    data class SetRepeatMode(val mode: RepeatMode) : PlayerCommand
    data class SetShuffle(val enabled: Boolean) : PlayerCommand
    data class SetSleepTimer(val mode: SleepTimerMode, val durationMinutes: Int? = null) : PlayerCommand
}

/** Implemented by the platform playback bridge after an engine is chosen. */
interface PlaybackController {
    val state: kotlinx.coroutines.flow.StateFlow<PlayerUiState>
    fun dispatch(command: PlayerCommand)
    fun setQueue(songs: List<SongItem>, startIndex: Int = 0)
    fun setVolumeNormalization(enabled: Boolean)
    fun setEqualizer(bands: List<Float>, preamp: Float = 0f)
    /** Maps Android's bass/loudness controls onto the platform DSP chain. */
    fun setBassBoost(gainDb: Float, enabled: Boolean)
    fun setLoudnessGain(gainDb: Float, enabled: Boolean)
    fun setPlaybackSpeed(speed: Float)
    fun setCrossfadeDuration(durationMs: Long)
    fun release()
}

/** Platform player construction deliberately lives behind an actual so UI and
 * shared view-models never import Media3 or libVLC. */
expect fun createPlaybackController(): PlaybackController
