package com.codetrio.spatialflow.shared.update

/** The Android application retains its existing UpdateManager during migration. */
private object AndroidUpdateInstaller : UpdateInstaller {
    override fun openRelease(release: AppRelease): Result<Unit> = Result.failure(
        UnsupportedOperationException("Android updates are handled by the existing UpdateManager."),
    )
}

actual fun createUpdateInstaller(): UpdateInstaller = AndroidUpdateInstaller
