package com.codetrio.spatialflow.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.io.File
import javax.swing.JPanel
import javax.swing.SwingUtilities

@Composable
fun DesktopYouTubeMusicLogin(session: DesktopYouTubeSession, onClose: (() -> Unit)? = null) {
    var status by remember { mutableStateOf(if (session.isLoggedIn()) "Connected to YouTube Music." else "Sign in in the embedded YouTube Music window.") }
    var browserRequested by remember { mutableStateOf(false) }
    val browser = remember { mutableStateOf<CefBrowser?>(null) }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("YouTube Music account", style = MaterialTheme.typography.headlineMedium)
        Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!browserRequested) {
            Text(
                "The first embedded sign-in downloads its Chromium engine once. It is optional and will not block the rest of SpatialFlow.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = {
                browserRequested = true
                status = "Preparing the embedded YouTube Music sign-in…"
            }) { Text(if (session.isLoggedIn()) "Open YouTube Music" else "Open embedded sign-in") }
        } else SwingPanel(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = {
                runCatching {
                val app = CefAppBuilder().apply {
                    setInstallDir(File(System.getProperty("user.home"), ".local/share/SpatialFlow/jcef"))
                }.build()
                val client = app.createClient()
                client.addLoadHandler(object : CefLoadHandlerAdapter() {
                    override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                        if (frame.isMain && browser.url.startsWith("https://music.youtube.com")) {
                            session.captureFromEmbeddedBrowser { captured ->
                                if (captured) SwingUtilities.invokeLater { status = "Connected to YouTube Music." }
                            }
                        }
                    }
                })
                client.createBrowser("https://accounts.google.com/ServiceLogin?service=youtube&passive=true&continue=https://music.youtube.com/", false, false).also { browser.value = it }.getUIComponent()
                }.getOrElse { error ->
                    SwingUtilities.invokeLater { status = "Could not start embedded sign-in: ${error.message ?: "unknown error"}" }
                    JPanel()
                }
            },
        )
        if (browserRequested) Button(onClick = { session.captureFromEmbeddedBrowser { captured -> status = if (captured) "Connected to YouTube Music." else "Complete sign-in in the page, then try again." } }) { Text("Finish sign-in") }
        if (browserRequested) TextButton(onClick = {
            browser.value?.loadURL("https://accounts.google.com/ServiceLogin?service=youtube&passive=true&continue=https://music.youtube.com/")
            status = "Reloaded YouTube Music sign-in."
        }) { Text("Reload sign-in") }
        onClose?.let { close -> TextButton(onClick = close) { Text("Close") } }
        if (session.isLoggedIn()) TextButton(onClick = {
            session.clear()
            browser.value?.loadURL("https://accounts.google.com/ServiceLogin?service=youtube&passive=true&continue=https://music.youtube.com/")
            status = "Signed out of YouTube Music."
        }) { Text("Log out") }
    }
    DisposableEffect(Unit) { onDispose { browser.value?.close(true) } }
}
