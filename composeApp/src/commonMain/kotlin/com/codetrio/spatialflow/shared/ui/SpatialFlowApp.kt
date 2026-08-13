package com.codetrio.spatialflow.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.shared.viewmodel.SettingsSection
import com.codetrio.spatialflow.shared.viewmodel.SettingsViewModel

private val CompactWidth = 600.dp

@Composable
fun SpatialFlowApp(providedViewModel: SettingsViewModel? = null) = SharedTheme {
    val viewModel = providedViewModel ?: remember { SettingsViewModel() }
    val state by viewModel.uiState.collectAsState()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < CompactWidth) {
            CompactSettings(
                selected = state.section,
                onSectionSelected = viewModel::selectSection,
                content = { padding -> SettingsContent(state, viewModel, padding) },
            )
        } else {
            ExpandedSettings(
                selected = state.section,
                onSectionSelected = viewModel::selectSection,
                content = { padding -> SettingsContent(state, viewModel, padding) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactSettings(
    selected: SettingsSection,
    onSectionSelected: (SettingsSection) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) = Scaffold(
    topBar = { CenterAlignedTopAppBar(title = { Text("Settings") }) },
    bottomBar = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            settingsDestinations.forEach { destination ->
                NavigationBarItem(
                    selected = selected == destination.section,
                    onClick = { onSectionSelected(destination.section) },
                    icon = { Icon(destination.icon, destination.section.label) },
                    label = { Text(destination.section.label) },
                )
            }
        }
    },
    content = content,
)

@Composable
private fun ExpandedSettings(
    selected: SettingsSection,
    onSectionSelected: (SettingsSection) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) = Row(Modifier.fillMaxSize()) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Spacer(Modifier.height(24.dp))
        settingsDestinations.forEach { destination ->
            NavigationRailItem(
                selected = selected == destination.section,
                onClick = { onSectionSelected(destination.section) },
                icon = { Icon(destination.icon, destination.section.label) },
                label = { Text(destination.section.label) },
            )
        }
    }
    // Content is intentionally the same composable as the compact branch.
    content(PaddingValues(horizontal = 40.dp, vertical = 24.dp))
}

@Composable
private fun SettingsContent(
    state: com.codetrio.spatialflow.shared.viewmodel.SettingsUiState,
    viewModel: SettingsViewModel,
    contentPadding: PaddingValues,
) {
    val rows = when (state.section) {
        SettingsSection.Appearance -> listOf(
            SettingRow("Dynamic album theme", "Use the active artwork as the colour source", state.dynamicAlbumTheme, viewModel::toggleDynamicAlbumTheme),
            SettingRow("AMOLED black", "Use pure black surfaces in dark mode", state.amoledBlack, viewModel::toggleAmoledBlack),
        )
        SettingsSection.Playback -> listOf(
            SettingRow("Volume normalization", "Keep playback loudness consistent", state.normalizeVolume, viewModel::toggleNormalizeVolume),
        )
        SettingsSection.Library -> listOf(
            SettingRow("Scan on launch", "Refresh local music when SpatialFlow opens", state.scanOnLaunch, viewModel::toggleScanOnLaunch),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(state.section.label, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        rows.forEach { row -> SharedSettingItem(row) }
    }
}

private data class SettingRow(
    val title: String,
    val summary: String,
    val checked: Boolean,
    val onCheckedChange: () -> Unit,
)

@Composable
private fun SharedSettingItem(row: SettingRow) {
    ListItem(
        headlineContent = { Text(row.title) },
        supportingContent = { Text(row.summary) },
        trailingContent = { Switch(checked = row.checked, onCheckedChange = { row.onCheckedChange() }) },
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
}

private data class SettingsDestination(val section: SettingsSection, val icon: ImageVector)

private val settingsDestinations = listOf(
    SettingsDestination(SettingsSection.Appearance, Icons.Outlined.Palette),
    SettingsDestination(SettingsSection.Playback, Icons.Outlined.Tune),
    SettingsDestination(SettingsSection.Library, Icons.Outlined.Folder),
)
