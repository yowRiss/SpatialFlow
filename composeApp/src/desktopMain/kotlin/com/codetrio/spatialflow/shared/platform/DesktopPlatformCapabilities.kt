package com.codetrio.spatialflow.shared.platform

import com.codetrio.spatialflow.shared.model.SongItem
import java.awt.Color
import java.awt.Image
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

object DesktopHapticFeedback : HapticFeedback { override fun performTick() = Unit }
object DesktopMediaControls : MediaControls {
    private var trayIcon: TrayIcon? = null
    private var lastMessage: String? = null

    override fun publishNowPlaying(song: SongItem?, isPlaying: Boolean) {
        val current = song ?: run { clear(); return }
        val message = buildString {
            append(current.title)
            if (current.artist.isNotBlank()) append(" — ${current.artist}")
            append(if (isPlaying) " is playing" else " is paused")
        }
        if (message == lastMessage) return
        lastMessage = message
        val icon = ensureTrayIcon() ?: return
        icon.toolTip = "SpatialFlow: $message"
        icon.displayMessage("SpatialFlow", message, TrayIcon.MessageType.INFO)
    }

    override fun clear() {
        lastMessage = null
        trayIcon?.let { icon -> runCatching { SystemTray.getSystemTray().remove(icon) } }
        trayIcon = null
    }

    private fun ensureTrayIcon(): TrayIcon? {
        trayIcon?.let { return it }
        if (!SystemTray.isSupported()) return null
        return runCatching {
            TrayIcon(appIcon(), "SpatialFlow").also { icon ->
                icon.isImageAutoSize = true
                SystemTray.getSystemTray().add(icon)
                trayIcon = icon
            }
        }.getOrNull()
    }

    private fun appIcon(): Image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB).apply {
        val graphics = createGraphics()
        try {
            graphics.color = Color(0x67, 0x5D, 0xFF)
            graphics.fillRoundRect(2, 2, 28, 28, 10, 10)
            graphics.color = Color.WHITE
            graphics.fillOval(9, 8, 7, 7)
            graphics.fillOval(16, 16, 7, 7)
            graphics.drawLine(15, 11, 19, 19)
        } finally {
            graphics.dispose()
        }
    }
}
