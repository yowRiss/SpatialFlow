package com.codetrio.spatialflow.shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** Supplies platform-provided dynamic colours when the platform supports them. */
@Composable
expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?
