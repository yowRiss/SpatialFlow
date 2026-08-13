package com.codetrio.spatialflow.shared.account

/** The existing Android app owns Google Sign-In during the incremental port. */
private object AndroidGoogleAuthClient : GoogleAuthClient {
    override suspend fun requestAuthorization(clientId: String, scopes: List<String>): GoogleAuthResult =
        GoogleAuthResult.Failure("Google Sign-In is handled by the Android application during migration.")
}

actual fun createGoogleAuthClient(): GoogleAuthClient = AndroidGoogleAuthClient
