package com.codetrio.spatialflow.shared.update

/** Platform hand-off for an available release. Desktop opens the signed release
 * page; replacing the running binary is intentionally delegated to the package
 * manager/installer rather than attempted in-process. */
interface UpdateInstaller {
    fun openRelease(release: AppRelease): Result<Unit>
}

expect fun createUpdateInstaller(): UpdateInstaller
