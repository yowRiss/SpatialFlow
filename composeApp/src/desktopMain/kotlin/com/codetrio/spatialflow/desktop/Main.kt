package com.codetrio.spatialflow.desktop

import androidx.compose.ui.unit.dp
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
        var onboardingComplete by remember { mutableStateOf(platform.hasCompletedOnboarding()) }
        if (onboardingComplete) DesktopSpatialFlowApp() else OnboardingScreen(platform) { onboardingComplete = true }
    }
}
}
