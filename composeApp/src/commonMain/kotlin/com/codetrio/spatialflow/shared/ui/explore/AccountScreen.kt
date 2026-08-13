package com.codetrio.spatialflow.shared.ui.explore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.shared.account.GoogleAuthClient
import com.codetrio.spatialflow.shared.account.GoogleAuthResult
import kotlinx.coroutines.launch

/** Desktop equivalent of Account/GoogleSignIn screens. A Google desktop OAuth
 * client ID is intentionally user-configured, never bundled in source. */
@Composable
fun AccountScreen(auth: GoogleAuthClient) {
    var clientId by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Streaming works without an account.") }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Account", style = MaterialTheme.typography.headlineMedium)
        Text("Connect Google for account-only YouTube Music features. The authorization code stays in memory until an account token exchange is configured.")
        OutlinedTextField(clientId, { clientId = it }, Modifier.fillMaxWidth(), label = { Text("Google desktop OAuth client ID") })
        Button(onClick = { scope.launch { status = when (val result = auth.requestAuthorization(clientId)) {
            is GoogleAuthResult.AuthorizationCode -> "Browser authorization completed. Configure the account token exchange to finish sign-in."
            is GoogleAuthResult.Failure -> result.reason
            GoogleAuthResult.Cancelled -> "Sign-in cancelled"
        } } }) { Text("Connect Google account") }
        Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
