package com.codetrio.spatialflow.shared.platform

import com.codetrio.spatialflow.shared.model.SongItem

interface HapticFeedback { fun performTick() }

/** Lower-priority OS media-session boundary. Desktop begins as a safe no-op. */
interface MediaControls {
    fun publishNowPlaying(song: SongItem?, isPlaying: Boolean)
    fun clear()
}
