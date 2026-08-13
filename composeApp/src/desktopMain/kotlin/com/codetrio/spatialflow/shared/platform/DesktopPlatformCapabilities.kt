package com.codetrio.spatialflow.shared.platform

import com.codetrio.spatialflow.shared.model.SongItem

object DesktopHapticFeedback : HapticFeedback { override fun performTick() = Unit }
object DesktopMediaControls : MediaControls {
    override fun publishNowPlaying(song: SongItem?, isPlaying: Boolean) = Unit
    override fun clear() = Unit
}
