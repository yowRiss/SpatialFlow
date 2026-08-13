package com.codetrio.spatialflow.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SpatialFlow",
        state = WindowState(width = 1280.dp, height = 800.dp),
    ) {
        DesktopSpatialFlowApp()
    }
}
