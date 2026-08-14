package com.codetrio.spatialflow.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.shared.account.DesktopYouTubeSession
import me.friwi.jcefmaven.CefAppBuilder
import org.cef.browser.CefBrowser
import java.io.File

@Composable
fun DesktopYouTubeMusicLogin(session: DesktopYouTubeSession) {
    var status by remember { mutableStateOf(if (session.isLoggedIn()) "Connected to YouTube Music." else "Sign in in the embedded YouTube Music window.") }
    val browser = remember { mutableStateOf<CefBrowser?>(null) }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("YouTube Music account", style = MaterialTheme.typography.headlineMedium)
        Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SwingPanel(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = {
                val app = CefAppBuilder().apply {
                    setInstallDir(File(System.getProperty("user.home"), ".local/share/SpatialFlow/jcef"))
                }.build()
                val client = app.createClient()
                client.createBrowser("https://accounts.google.com/ServiceLogin?service=youtube&passive=true&continue=https://music.youtube.com/", false, false).also { browser.value = it }.getUIComponent()
            },
        )
        Button(onClick = { session.captureFromEmbeddedBrowser { captured -> status = if (captured) "Connected to YouTube Music." else "Complete sign-in in the page, then try again." } }) { Text("Finish sign-in") }
    }
    DisposableEffect(Unit) { onDispose { browser.value?.close(true) } }
}
