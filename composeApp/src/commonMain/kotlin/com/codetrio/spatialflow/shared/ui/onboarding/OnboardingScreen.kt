package com.codetrio.spatialflow.shared.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.shared.onboarding.OnboardingPlatform
import com.codetrio.spatialflow.shared.onboarding.OnboardingPreferences
import com.codetrio.spatialflow.shared.onboarding.OnboardingStep
import com.codetrio.spatialflow.shared.onboarding.OnboardingUiState
import com.codetrio.spatialflow.shared.onboarding.ThemeMode
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/** Shared nine-step onboarding, preserving the Android information architecture. */
@Composable
fun OnboardingScreen(platform: OnboardingPlatform, onComplete: () -> Unit) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(OnboardingUiState()) }
    fun updatePreferences(transform: (OnboardingPreferences) -> OnboardingPreferences) {
        state = state.copy(preferences = transform(state.preferences))
    }
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("SpatialFlow", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${state.step.ordinal + 1} / ${OnboardingStep.entries.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            AnimatedContent(state.step, transitionSpec = { fadeIn() togetherWith fadeOut() }) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> IntroPage("Welcome to SpatialFlow", "Your music, beautifully in motion.", Icons.Default.Home)
                    OnboardingStep.ECOSYSTEM -> IntroPage("One music ecosystem", "Local files, playlists, streaming, and lyrics in one queue.", Icons.Default.LibraryMusic)
                    OnboardingStep.FEATURES -> FeaturesPage()
                    OnboardingStep.ACCOUNT -> AccountPage(state.isSignedIn, { state = state.copy(isSignedIn = true) })
                    OnboardingStep.PERMISSIONS -> PermissionsPage(state) {
                        scope.launch { state = state.copy(permissions = platform.requestPermissions()) }
                    }
                    OnboardingStep.THEME -> ThemePage(state.preferences.themeMode) { mode -> updatePreferences { it.copy(themeMode = mode) } }
                    OnboardingStep.NAVIGATION -> NavigationPage(state.preferences) { updatePreferences(it) }
                    OnboardingStep.PREFERENCES -> PreferencesPage(state.preferences.hapticsStrength, { strength ->
                        updatePreferences { it.copy(hapticsStrength = strength) }
                        if (strength > 0) platform.previewHaptics(strength)
                    })
                    OnboardingStep.FINISH -> IntroPage("You’re ready", "Build your library, explore new music, and make SpatialFlow yours.", Icons.Default.Check)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = state.step != OnboardingStep.WELCOME, onClick = { state = state.copy(step = state.step.previous()) }) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            if (state.step.isLast) Button(onClick = {
                scope.launch { platform.persist(state.preferences); onComplete() }
            }) { Text("Finish"); Icon(Icons.Default.Check, null) }
            else Button(enabled = state.canAdvance, onClick = { state = state.copy(step = state.step.next()) }) {
                Text("Continue"); Icon(Icons.Default.ArrowForward, null)
            }
        }
    }
}

@Composable private fun IntroPage(title: String, body: String, icon: ImageVector) = Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
    Icon(icon, null, Modifier.size(88.dp), MaterialTheme.colorScheme.primary)
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    Text(body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable private fun FeaturesPage() = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Everything in flow", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    listOf("Local and streaming music in one queue", "Synced karaoke lyrics", "Artwork-driven themes and glass panels", "Crossfade, loudness normalization, and EQ").forEach {
        Card { Text(it, Modifier.padding(18.dp), style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable private fun AccountPage(isSignedIn: Boolean, signIn: () -> Unit) = Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(if (isSignedIn) "Account connected" else "Connect your account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text(if (isSignedIn) "You can now use your connected account where available." else "Streaming works without an account. Sign in is optional.", textAlign = TextAlign.Center)
    if (!isSignedIn) Button(onClick = signIn) { Text("Connect Google account") }
}

@Composable private fun PermissionsPage(state: OnboardingUiState, request: () -> Unit) = Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text("Let SpatialFlow in", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    PermissionItem(Icons.Default.LibraryMusic, "Music library", state.permissions.audio)
    PermissionItem(Icons.Default.Notifications, "Now playing notifications", state.permissions.notifications)
    PermissionItem(Icons.Default.Mic, "Microphone", state.permissions.microphone)
    Button(onClick = request) { Text("Grant access") }
}

@Composable private fun PermissionItem(icon: ImageVector, text: String, granted: Boolean) = Card { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
    Icon(icon, null); Spacer(Modifier.size(14.dp)); Text(text, Modifier.weight(1f)); Text(if (granted) "Granted" else "Required", color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
} }

@Composable private fun ThemePage(selected: ThemeMode, select: (ThemeMode) -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Choose a theme", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    ThemeMode.entries.forEach { mode -> OutlinedButton(onClick = { select(mode) }, modifier = Modifier.fillMaxWidth()) {
        Icon(if (mode == ThemeMode.DARK) Icons.Default.DarkMode else Icons.Default.LightMode, null); Spacer(Modifier.size(12.dp)); Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)); if (selected == mode) { Spacer(Modifier.weight(1f)); Icon(Icons.Default.Check, null) }
    } }
}

@Composable private fun NavigationPage(preferences: OnboardingPreferences, update: ((OnboardingPreferences) -> OnboardingPreferences) -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Navigation style", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    ToggleRow("Hide navigation labels", preferences.hideNavigationLabels) { update { it.copy(hideNavigationLabels = it.hideNavigationLabels.not()) } }
    ToggleRow("Dynamic navigation style", preferences.dynamicNavigationStyle) { update { it.copy(dynamicNavigationStyle = it.dynamicNavigationStyle.not()) } }
    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) { listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.LibraryMusic, Icons.Default.Settings).forEach { Icon(it, null, Modifier.size(34.dp)) } }
}

@Composable private fun PreferencesPage(level: Float, update: (Float) -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Fine tune the feel", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text("Haptic strength: ${level.toInt()}%")
    Slider(value = level, onValueChange = update, valueRange = 0f..100f)
}

@Composable private fun ToggleRow(label: String, checked: Boolean, change: () -> Unit) = Card { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(checked, { change() }) } }
