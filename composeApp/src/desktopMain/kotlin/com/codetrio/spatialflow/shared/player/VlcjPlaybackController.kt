package com.codetrio.spatialflow.shared.player

import com.codetrio.spatialflow.shared.model.SongItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import kotlin.math.roundToInt
import kotlin.math.pow
import kotlin.random.Random

/**
 * libVLC-backed desktop player. libVLC is discovered by vlcj at runtime; if it
 * is not installed, the controller reports a recoverable processing state
 * instead of preventing the desktop application from launching.
 */
class VlcjPlaybackController : PlaybackController {
    private val mutableState = MutableStateFlow(PlayerUiState())
    override val state: StateFlow<PlayerUiState> = mutableState
    private var queue: List<SongItem> = emptyList()
    private var factory: MediaPlayerFactory? = null
    private var player: MediaPlayer? = null
    private var fadingPlayer: MediaPlayer? = null
    private val playbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var crossfadeJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var sleepMode = SleepTimerMode.OFF
    private var isCrossfading = false
    private var normalizationEnabled = false
    private var crossfadeDurationMs = 0L
    private var pendingBands: List<Float> = emptyList()
    private var pendingPreamp = 0f
    private var bassBoostDb = 0f
    private var loudnessGainDb = 0f

    override fun setQueue(songs: List<SongItem>, startIndex: Int) {
        queue = songs
        val index = startIndex.coerceIn(0, (songs.lastIndex).coerceAtLeast(0))
        mutableState.value = mutableState.value.copy(currentSong = songs.getOrNull(index), currentSongIndex = if (songs.isEmpty()) -1 else index)
    }

    override fun dispatch(command: PlayerCommand) {
        synchronized(this) {
        when (command) {
            PlayerCommand.TogglePlayback -> toggle()
            PlayerCommand.Next -> playAt(nextIndex())
            PlayerCommand.Previous -> playAt(previousIndex())
            is PlayerCommand.SeekTo -> player?.controls()?.setTime(command.positionMs.coerceAtLeast(0))
            is PlayerCommand.PlayAt -> playAt(command.queueIndex)
            is PlayerCommand.ReorderQueue -> reorder(command.fromIndex, command.toIndex)
            is PlayerCommand.SetRepeatMode -> mutableState.value = mutableState.value.copy(repeatMode = command.mode)
            is PlayerCommand.SetShuffle -> mutableState.value = mutableState.value.copy(isShuffleEnabled = command.enabled)
            is PlayerCommand.SetSleepTimer -> setSleepTimer(command.mode, command.durationMinutes)
        }
        }
    }

    override fun setVolumeNormalization(enabled: Boolean) {
        normalizationEnabled = enabled
        applyNormalization()
    }

    override fun setEqualizer(bands: List<Float>, preamp: Float) {
        pendingBands = bands
        pendingPreamp = preamp
        applyEqualizer()
    }

    override fun setBassBoost(gainDb: Float, enabled: Boolean) {
        bassBoostDb = if (enabled) gainDb.coerceIn(-20f, 20f) else 0f
        applyEqualizer()
    }

    override fun setLoudnessGain(gainDb: Float, enabled: Boolean) {
        loudnessGainDb = if (enabled) gainDb.coerceIn(-20f, 20f) else 0f
        applyEqualizer()
    }

    override fun setPlaybackSpeed(speed: Float) {
        player?.controls()?.setRate(speed.coerceIn(0.25f, 4f))
        fadingPlayer?.controls()?.setRate(speed.coerceIn(0.25f, 4f))
    }

    override fun setCrossfadeDuration(durationMs: Long) {
        crossfadeDurationMs = durationMs.coerceAtLeast(0)
    }

    private fun toggle() {
        val active = player ?: createPlayer() ?: return
        if (active.status().isPlaying) active.controls().setPause(true) else if (active.media().isValid) active.controls().play() else playAt(mutableState.value.currentSongIndex)
    }

    private fun playAt(rawIndex: Int) {
        if (queue.isEmpty()) return
        val index = rawIndex.coerceIn(0, queue.lastIndex)
        val song = queue[index]
        val location = song.path ?: return
        val active = createPlayer() ?: return
        mutableState.value = mutableState.value.copy(currentSong = song, currentSongIndex = index, isProcessing = true)
        active.media().play(location)
        applyNormalization(); applyEqualizer()
    }

    private fun createPlayer(): MediaPlayer? {
        player?.let { return it }
        return runCatching {
            val newFactory = MediaPlayerFactory("--no-video", "--quiet")
            val newPlayer = newFactory.mediaPlayers().newMediaPlayer()
            newPlayer.events().addMediaPlayerEventListener(listener)
            factory = newFactory
            player = newPlayer
            newPlayer
        }.getOrElse {
            mutableState.value = mutableState.value.copy(isProcessing = false, isPlaying = false)
            null
        }
    }

    private val listener: MediaPlayerEventAdapter = object : MediaPlayerEventAdapter() {
        override fun playing(mediaPlayer: MediaPlayer) { mutableState.value = mutableState.value.copy(isPlaying = true, isProcessing = false) }
        override fun paused(mediaPlayer: MediaPlayer) { mutableState.value = mutableState.value.copy(isPlaying = false) }
        override fun stopped(mediaPlayer: MediaPlayer) { mutableState.value = mutableState.value.copy(isPlaying = false) }
        override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) { mutableState.value = mutableState.value.copy(duration = newLength.coerceAtLeast(0).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) }
        override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
            if (mediaPlayer !== player) return
            mutableState.value = mutableState.value.copy(positionMs = newTime.coerceAtLeast(0))
            startCrossfadeIfNeeded(newTime)
        }
        override fun finished(mediaPlayer: MediaPlayer) { onFinished() }
        override fun error(mediaPlayer: MediaPlayer) { mutableState.value = mutableState.value.copy(isPlaying = false, isProcessing = false) }
    }

    private fun onFinished() = synchronized(this) {
        if (isCrossfading) return
        if (sleepMode == SleepTimerMode.END_OF_SONG) { sleepMode = SleepTimerMode.OFF; mutableState.value = mutableState.value.copy(isPlaying = false, sleepTimerMode = SleepTimerMode.OFF); return }
        if (sleepMode == SleepTimerMode.END_OF_QUEUE && mutableState.value.currentSongIndex == queue.lastIndex) { sleepMode = SleepTimerMode.OFF; mutableState.value = mutableState.value.copy(isPlaying = false, sleepTimerMode = SleepTimerMode.OFF); return }
        val current = mutableState.value.currentSongIndex
        when (mutableState.value.repeatMode) {
            RepeatMode.ONE -> playAt(current)
            RepeatMode.ALL -> playAt(nextIndex())
            RepeatMode.OFF -> if (mutableState.value.isShuffleEnabled || current < queue.lastIndex) playAt(nextIndex()) else mutableState.value = mutableState.value.copy(isPlaying = false)
        }
    }

    private fun nextIndex(): Int = when {
        queue.isEmpty() -> -1
        !mutableState.value.isShuffleEnabled || queue.size == 1 -> (mutableState.value.currentSongIndex + 1).floorMod(queue.size)
        else -> queue.indices.filter { it != mutableState.value.currentSongIndex }.random(Random.Default)
    }
    private fun previousIndex(): Int = if (queue.isEmpty()) -1 else (mutableState.value.currentSongIndex - 1).floorMod(queue.size)
    private fun Int.floorMod(size: Int) = if (size == 0) -1 else ((this % size) + size) % size

    /**
     * libVLC does not offer a one-call crossfade control, so the controller
     * overlaps two independent libVLC players and ramps their volumes over the
     * configured interval. This retains the queue/public controller API while
     * keeping native-player ownership entirely in desktopMain.
     */
    private fun startCrossfadeIfNeeded(positionMs: Long) = synchronized(this) {
        if (crossfadeDurationMs <= 0 || isCrossfading || crossfadeJob?.isActive == true || queue.size < 2) return
        val outgoing = player ?: return
        val length = outgoing.status().length().takeIf { it > 0 } ?: return
        if (positionMs < length - crossfadeDurationMs) return
        val targetIndex = crossfadeTargetIndex() ?: return
        val targetSong = queue[targetIndex]
        val location = targetSong.path ?: return
        val activeFactory = factory ?: return
        val incoming: MediaPlayer = runCatching<MediaPlayer> {
            val newPlayer: MediaPlayer = activeFactory.mediaPlayers().newMediaPlayer()
            newPlayer.events().addMediaPlayerEventListener(listener)
            newPlayer
        }.getOrNull() ?: return

        isCrossfading = true
        fadingPlayer = incoming
        incoming.audio().setVolume(0)
        incoming.media().play(location)
        crossfadeJob = playbackScope.launch {
            val steps = 24
            val outgoingVolume = normalizedVolume(mutableState.value.currentSong)
            val incomingVolume = normalizedVolume(targetSong)
            repeat(steps + 1) { step ->
                val progress = step.toFloat() / steps
                outgoing.audio().setVolume((outgoingVolume * (1f - progress)).roundToInt().coerceIn(0, 200))
                incoming.audio().setVolume((incomingVolume * progress).roundToInt().coerceIn(0, 200))
                delay((crossfadeDurationMs / steps).coerceAtLeast(1))
            }
            synchronized(this@VlcjPlaybackController) {
                if (fadingPlayer === incoming) {
                    outgoing.release()
                    player = incoming
                    fadingPlayer = null
                    isCrossfading = false
                    mutableState.value = mutableState.value.copy(
                        currentSong = targetSong,
                        currentSongIndex = targetIndex,
                        positionMs = 0,
                        duration = 0,
                        isPlaying = true,
                    )
                }
            }
        }
    }

    private fun crossfadeTargetIndex(): Int? {
        val current = mutableState.value.currentSongIndex
        return when {
            current < queue.lastIndex -> current + 1
            mutableState.value.repeatMode == RepeatMode.ALL -> 0
            else -> null
        }
    }

    private fun reorder(from: Int, to: Int) {
        if (from !in queue.indices || to !in queue.indices) return
        queue = queue.toMutableList().apply { add(to, removeAt(from)) }
    }

    private fun setSleepTimer(mode: SleepTimerMode, minutes: Int?) {
        sleepTimerJob?.cancel(); sleepTimerJob = null; sleepMode = mode
        mutableState.value = mutableState.value.copy(sleepTimerMode = mode)
        if (mode == SleepTimerMode.CUSTOM && (minutes ?: 0) > 0) {
            sleepTimerJob = playbackScope.launch {
                delay(minutes!! * 60_000L)
                synchronized(this@VlcjPlaybackController) { player?.controls()?.setPause(true); sleepMode = SleepTimerMode.OFF; mutableState.value = mutableState.value.copy(isPlaying = false, sleepTimerMode = SleepTimerMode.OFF) }
            }
        }
    }

    private fun applyNormalization() {
        val song = mutableState.value.currentSong ?: return
        val volume = normalizedVolume(song)
        player?.audio()?.setVolume(volume)
    }

    private fun normalizedVolume(song: SongItem?): Int {
        val lufs = song?.lufs ?: return 100
        // Target -14 LUFS. libVLC volume allows 0..200, so retain headroom.
        return if (normalizationEnabled) (100f * 10f.pow((-14f - lufs) / 20f)).roundToInt().coerceIn(0, 200) else 100
    }

    private fun applyEqualizer() {
        val activeFactory = factory ?: return
        val equalizer = activeFactory.equalizer().newEqualizer().apply {
            setPreamp((pendingPreamp + loudnessGainDb).coerceIn(-20f, 20f))
            pendingBands.take(10).forEachIndexed { index, gain ->
                val bassGain = if (index <= 1) bassBoostDb else 0f
                setAmp(index, (gain + bassGain).coerceIn(-20f, 20f))
            }
        }
        player?.audio()?.setEqualizer(equalizer)
        fadingPlayer?.audio()?.setEqualizer(equalizer)
    }

    override fun release() = synchronized(this) {
        crossfadeJob?.cancel(); crossfadeJob = null
        sleepTimerJob?.cancel(); sleepTimerJob = null
        player?.release(); player = null
        fadingPlayer?.release(); fadingPlayer = null
        factory?.release(); factory = null
    }
}

actual fun createPlaybackController(): PlaybackController = VlcjPlaybackController()
