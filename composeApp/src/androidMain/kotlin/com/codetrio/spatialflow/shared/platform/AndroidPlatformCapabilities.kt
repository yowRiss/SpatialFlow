package com.codetrio.spatialflow.shared.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.codetrio.spatialflow.shared.model.SongItem

class AndroidHapticFeedback(context: Context) : HapticFeedback {
    private val vibrator = context.getSystemService(Vibrator::class.java)
    override fun performTick() {
        if (vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") vibrator.vibrate(25)
    }
}

/** Android Media3 service remains the active source of OS media controls during migration. */
object AndroidMediaControls : MediaControls {
    override fun publishNowPlaying(song: SongItem?, isPlaying: Boolean) = Unit
    override fun clear() = Unit
}
