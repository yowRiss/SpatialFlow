package com.codetrio.spatialflow.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.codetrio.spatialflow.shared.di.commonModule
import com.codetrio.spatialflow.shared.di.desktopModule
import com.codetrio.spatialflow.shared.onboarding.DesktopOnboardingPlatform
import com.codetrio.spatialflow.shared.onboarding.ThemeMode
import com.codetrio.spatialflow.shared.account.DesktopYouTubeSession
import com.codetrio.spatialflow.shared.ui.SpatialFlowTheme
import com.codetrio.spatialflow.shared.ui.onboarding.OnboardingScreen
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

fun main() {
    startKoin { modules(commonModule, desktopModule) }
    application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SpatialFlow",
        state = WindowState(width = 1280.dp, height = 800.dp),
    ) {
        val platform = remember { GlobalContext.get().get<DesktopOnboardingPlatform>() }
        val youtubeSession = remember { GlobalContext.get().get<DesktopYouTubeSession>() }
        var onboardingComplete by remember { mutableStateOf(platform.hasCompletedOnboarding()) }
        var onboardingTheme by remember { mutableStateOf(platform.savedThemeMode()) }
        var showYouTubeLogin by remember { mutableStateOf(false) }
        val darkTheme = when (onboardingTheme) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
        if (onboardingComplete) {
            DesktopSpatialFlowApp()
        } else {
            SpatialFlowTheme(darkTheme = darkTheme) {
                OnboardingScreen(
                    platform = platform,
                    accountConnected = youtubeSession.isLoggedIn(),
                    onConnectAccount = { showYouTubeLogin = true },
                    onThemeModeChanged = { onboardingTheme = it },
                    onComplete = { onboardingComplete = true },
                )
            }
        }
        if (showYouTubeLogin) DialogWindow(
            onCloseRequest = { showYouTubeLogin = false },
            title = "Connect YouTube Music",
        ) {
            DesktopYouTubeMusicLogin(youtubeSession) { showYouTubeLogin = false }
        }
    }
}
}
