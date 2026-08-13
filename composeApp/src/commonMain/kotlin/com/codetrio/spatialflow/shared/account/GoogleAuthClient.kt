package com.codetrio.spatialflow.shared.account

/** OAuth result deliberately contains no persisted token; token exchange and
 * account storage are owned by the account repository. */
sealed interface GoogleAuthResult {
    data class AuthorizationCode(val code: String, val redirectUri: String, val codeVerifier: String) : GoogleAuthResult
    data class Failure(val reason: String) : GoogleAuthResult
    data object Cancelled : GoogleAuthResult
}

interface GoogleAuthClient {
    suspend fun requestAuthorization(clientId: String, scopes: List<String> = defaultGoogleScopes): GoogleAuthResult
}

val defaultGoogleScopes = listOf("openid", "email", "profile")

/** Each platform supplies its browser/activity hand-off implementation. */
expect fun createGoogleAuthClient(): GoogleAuthClient
