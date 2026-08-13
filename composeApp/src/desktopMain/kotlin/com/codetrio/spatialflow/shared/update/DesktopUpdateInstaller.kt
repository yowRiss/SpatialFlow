package com.codetrio.spatialflow.shared.update

import java.awt.Desktop
import java.net.URI

class DesktopUpdateInstaller : UpdateInstaller {
    override fun openRelease(release: AppRelease): Result<Unit> = runCatching {
        check(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            "No system browser is available to open the update."
        }
        Desktop.getDesktop().browse(URI(release.releaseUrl))
    }
}

actual fun createUpdateInstaller(): UpdateInstaller = DesktopUpdateInstaller()
