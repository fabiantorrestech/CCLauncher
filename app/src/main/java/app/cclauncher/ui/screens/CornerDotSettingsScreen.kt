package app.cclauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.cclauncher.MainViewModel
import app.cclauncher.data.Constants
import app.cclauncher.data.HomeItem
import app.cclauncher.settings.AppSettings
import app.cclauncher.settings.CornerDotConfig
import app.cclauncher.ui.components.ColorPickerDialog
import app.cclauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val CORNER_LABELS = listOf("Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right")
private val ACTION_LABELS = listOf("None", "Search", "Notifications", "App", "Next Page", "Previous Page", "Open Folder")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CornerDotSettingsScreen(
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {
    val settings by settingsViewModel.settingsState.collectAsState()
    val homeLayout by mainViewModel.homeLayoutState.collectAsState()
    val allFolders = remember(homeLayout.items) { homeLayout.items.filterIsInstance<HomeItem.Folder>() }
    val coroutineScope = rememberCoroutineScope()

    val cornerConfigs = listOf(
        settings.cornerDotTopLeft,
        settings.cornerDotTopRight,
        settings.cornerDotBottomLeft,
        settings.cornerDotBottomRight,
    )

    fun updateCornerConfig(corner: Int, config: CornerDotConfig) {
        coroutineScope.launch {
            mainViewModel.settingsRepository.updateCornerDotConfig(corner, config)
        }
    }

    fun updateUniversalConfig(config: CornerDotConfig) {
        coroutineScope.launch {
            mainViewModel.settingsRepository.updateUniversalCornerDotConfig(config)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Corner Dot Shortcuts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Apply to All toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Apply Appearance to All", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "Control all dot appearances from one place",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                        Switch(
                            checked = settings.applyToAllCornerDots,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    settingsViewModel.updateSetting("applyToAllCornerDots", it)
                                }
                            }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(4.dp))
                }

                if (settings.applyToAllCornerDots) {
                    // Universal appearance card
                    item {
                        Text(
                            "Universal Appearance",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Text(
                            "These appearance settings apply to all enabled corner dots",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        CornerDotAppearanceCard(
                            config = settings.cornerDotUniversal,
                            onUpdate = { updated -> updateUniversalConfig(updated) },
                        )
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Per-Corner Settings",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Text(
                            "Enable/disable each corner and set its action independently",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }

                    // Minimal per-corner cards (enabled + action only)
                    items(
                        (Constants.CornerPosition.TOP_LEFT..Constants.CornerPosition.BOTTOM_RIGHT).toList(),
                        key = { it },
                    ) { corner ->
                        CornerDotMinimalCard(
                            label = CORNER_LABELS[corner],
                            config = cornerConfigs[corner],
                            allFolders = allFolders,
                            onUpdate = { updated -> updateCornerConfig(corner, updated) },
                        )
                    }
                } else {
                    // Full per-corner cards with all appearance controls
                    items(
                        (Constants.CornerPosition.TOP_LEFT..Constants.CornerPosition.BOTTOM_RIGHT).toList(),
                        key = { it },
                    ) { corner ->
                        CornerDotCard(
                            label = CORNER_LABELS[corner],
                            config = cornerConfigs[corner],
                            allFolders = allFolders,
                            onUpdate = { updated -> updateCornerConfig(corner, updated) },
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }

            // Live preview — rendered in a Popup window at the true screen origin
            CornerDotLivePreview(settings = settings)
        }
    }
}

/**
 * Renders non-interactive preview dots using a Popup anchored to the root window,
 * giving them the exact same coordinate space as the home screen corner dots.
 */
@Composable
private fun CornerDotLivePreview(settings: AppSettings) {
    val view = LocalView.current
    val density = LocalDensity.current

    val corners = listOf(
        Alignment.TopStart to settings.cornerDotTopLeft,
        Alignment.TopEnd to settings.cornerDotTopRight,
        Alignment.BottomStart to settings.cornerDotBottomLeft,
        Alignment.BottomEnd to settings.cornerDotBottomRight,
    )

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset.Zero,
        properties = PopupProperties(focusable = false, clippingEnabled = false),
    ) {
        // Size the popup to the full display so alignment corners match the home screen exactly.
        val screenWidth = with(density) { view.rootView.width.toDp() }
        val screenHeight = with(density) { view.rootView.height.toDp() }
        if (screenWidth == 0.dp || screenHeight == 0.dp) return@Popup

        Box(modifier = Modifier.size(screenWidth, screenHeight)) {
            corners.forEach { (alignment, rawConfig) ->
                if (!rawConfig.enabled) return@forEach
                val config = if (settings.applyToAllCornerDots) {
                    val u = settings.cornerDotUniversal
                    rawConfig.copy(
                        size = u.size, color = u.color, opacity = u.opacity, visible = u.visible,
                        borderEnabled = u.borderEnabled, borderColor = u.borderColor,
                        borderWidth = u.borderWidth, inset = u.inset,
                    )
                } else rawConfig

                val dotColor = if (config.visible) Color(config.color).copy(alpha = config.opacity)
                else Color.Transparent

                Box(
                    modifier = Modifier
                        .align(alignment)
                        .padding(config.inset.dp)
                        .size(config.size.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .then(
                            if (config.borderEnabled && config.visible)
                                Modifier.border(
                                    config.borderWidth.dp,
                                    Color(config.borderColor).copy(alpha = config.opacity),
                                    CircleShape,
                                )
                            else Modifier
                        )
                )
            }
        }
    }
}

// Full card — used when "Apply to All" is OFF
@Composable
private fun CornerDotCard(
    label: String,
    config: CornerDotConfig,
    allFolders: List<HomeItem.Folder>,
    onUpdate: (CornerDotConfig) -> Unit,
) {
    var showActionPicker by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showBorderColorPicker by remember { mutableStateOf(false) }
    var folderSearch by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                DotPreviewBadge(config = config)
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Enabled", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { onUpdate(config.copy(enabled = it)) },
                )
            }

            if (!config.enabled) return@Column

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Action", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { showActionPicker = true }) {
                    Text(ACTION_LABELS.getOrElse(config.action) { "Unknown" })
                }
            }

            if (config.action == Constants.SwipeAction.OPEN_FOLDER) {
                val folderTitle = allFolders.find { it.id == config.folderId }?.title ?: "Not set"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Folder", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { folderSearch = ""; showFolderPicker = true }) {
                        Text(folderTitle)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            AppearanceControls(
                config = config,
                onUpdate = onUpdate,
                onShowColorPicker = { showColorPicker = true },
                onShowBorderColorPicker = { showBorderColorPicker = true },
            )
        }
    }

    CornerDotDialogs(
        config = config,
        allFolders = allFolders,
        showActionPicker = showActionPicker,
        showFolderPicker = showFolderPicker,
        showColorPicker = showColorPicker,
        showBorderColorPicker = showBorderColorPicker,
        folderSearch = folderSearch,
        onFolderSearchChange = { folderSearch = it },
        onDismissActionPicker = { showActionPicker = false },
        onDismissFolderPicker = { showFolderPicker = false },
        onDismissColorPicker = { showColorPicker = false },
        onDismissBorderColorPicker = { showBorderColorPicker = false },
        onUpdate = onUpdate,
    )
}

// Minimal card — used per-corner when "Apply to All" is ON
@Composable
private fun CornerDotMinimalCard(
    label: String,
    config: CornerDotConfig,
    allFolders: List<HomeItem.Folder>,
    onUpdate: (CornerDotConfig) -> Unit,
) {
    var showActionPicker by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var folderSearch by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall)

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Enabled", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { onUpdate(config.copy(enabled = it)) },
                )
            }

            if (!config.enabled) return@Column

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Action", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { showActionPicker = true }) {
                    Text(ACTION_LABELS.getOrElse(config.action) { "Unknown" })
                }
            }

            if (config.action == Constants.SwipeAction.OPEN_FOLDER) {
                val folderTitle = allFolders.find { it.id == config.folderId }?.title ?: "Not set"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Folder", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { folderSearch = ""; showFolderPicker = true }) {
                        Text(folderTitle)
                    }
                }
            }
        }
    }

    if (showActionPicker) {
        AlertDialog(
            onDismissRequest = { showActionPicker = false },
            title = { Text("Select Action") },
            text = {
                Column {
                    ACTION_LABELS.forEachIndexed { index, actionLabel ->
                        ListItem(
                            headlineContent = { Text(actionLabel) },
                            modifier = Modifier.fillMaxWidth()
                                .then(
                                    if (index == config.action)
                                        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                    else Modifier
                                )
                                .clickable {
                                    onUpdate(config.copy(action = index))
                                    showActionPicker = false
                                }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showActionPicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showFolderPicker) {
        val filtered = remember(folderSearch, allFolders) {
            if (folderSearch.isBlank()) allFolders
            else allFolders.filter { it.title.contains(folderSearch, ignoreCase = true) }
        }
        AlertDialog(
            onDismissRequest = { showFolderPicker = false },
            title = { Text("Select Folder") },
            text = {
                Column {
                    OutlinedTextField(
                        value = folderSearch,
                        onValueChange = { folderSearch = it },
                        placeholder = { Text("Search…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(280.dp)) {
                        items(filtered, key = { it.id }) { folder ->
                            ListItem(
                                headlineContent = { Text(folder.title) },
                                modifier = Modifier.fillMaxWidth()
                                    .then(
                                        if (folder.id == config.folderId)
                                            Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                        else Modifier
                                    )
                                    .clickable {
                                        onUpdate(config.copy(folderId = folder.id))
                                        showFolderPicker = false
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderPicker = false }) { Text("Cancel") }
            }
        )
    }
}

// Appearance-only card — used as the universal control when "Apply to All" is ON
@Composable
private fun CornerDotAppearanceCard(
    config: CornerDotConfig,
    onUpdate: (CornerDotConfig) -> Unit,
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var showBorderColorPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Preview", style = MaterialTheme.typography.titleSmall)
                DotPreviewBadge(config = config)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            AppearanceControls(
                config = config,
                onUpdate = onUpdate,
                onShowColorPicker = { showColorPicker = true },
                onShowBorderColorPicker = { showBorderColorPicker = true },
            )
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            title = "Dot Color",
            currentColor = config.color,
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                onUpdate(config.copy(color = color))
                showColorPicker = false
            }
        )
    }

    if (showBorderColorPicker) {
        ColorPickerDialog(
            title = "Border Color",
            currentColor = config.borderColor,
            onDismiss = { showBorderColorPicker = false },
            onColorSelected = { color ->
                onUpdate(config.copy(borderColor = color))
                showBorderColorPicker = false
            }
        )
    }
}

/** Small dot rendered in card headers to preview the current config. */
@Composable
private fun DotPreviewBadge(config: CornerDotConfig) {
    val previewColor = if (config.visible) Color(config.color).copy(alpha = config.opacity)
    else Color.Transparent
    Box(
        modifier = Modifier
            .size(config.size.coerceIn(8f, 32f).dp)
            .clip(CircleShape)
            .background(previewColor)
            .then(
                if (config.borderEnabled && config.visible)
                    Modifier.border(
                        config.borderWidth.dp,
                        Color(config.borderColor).copy(alpha = config.opacity),
                        CircleShape,
                    )
                else Modifier
            )
    )
}

// Shared appearance controls (visible, size, opacity, inset, color, border)
@Composable
private fun AppearanceControls(
    config: CornerDotConfig,
    onUpdate: (CornerDotConfig) -> Unit,
    onShowColorPicker: () -> Unit,
    onShowBorderColorPicker: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Visible", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Invisible dots are still tappable",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Switch(
            checked = config.visible,
            onCheckedChange = { onUpdate(config.copy(visible = it)) },
        )
    }

    Spacer(Modifier.height(8.dp))

    Text("Size: ${"%.0f".format(config.size)} dp", style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = config.size,
        onValueChange = { onUpdate(config.copy(size = it)) },
        valueRange = 4f..48f,
        modifier = Modifier.fillMaxWidth(),
    )

    Text("Opacity: ${"%.0f".format(config.opacity * 100)}%", style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = config.opacity,
        onValueChange = { onUpdate(config.copy(opacity = it)) },
        valueRange = 0f..1f,
        modifier = Modifier.fillMaxWidth(),
    )

    Text("Inset: ${"%.0f".format(config.inset)} dp", style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = config.inset,
        onValueChange = { onUpdate(config.copy(inset = it)) },
        valueRange = 0f..64f,
        modifier = Modifier.fillMaxWidth(),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Color", style = MaterialTheme.typography.bodyMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(config.color))
            )
            TextButton(onClick = onShowColorPicker) { Text("Change") }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Border", style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = config.borderEnabled,
            onCheckedChange = { onUpdate(config.copy(borderEnabled = it)) },
        )
    }

    if (config.borderEnabled) {
        Text("Border Width: ${"%.1f".format(config.borderWidth)} dp", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = config.borderWidth,
            onValueChange = { onUpdate(config.copy(borderWidth = it)) },
            valueRange = 0.5f..8f,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Border Color", style = MaterialTheme.typography.bodyMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(config.borderColor))
                )
                TextButton(onClick = onShowBorderColorPicker) { Text("Change") }
            }
        }
    }
}

// Dialogs for CornerDotCard
@Composable
private fun CornerDotDialogs(
    config: CornerDotConfig,
    allFolders: List<HomeItem.Folder>,
    showActionPicker: Boolean,
    showFolderPicker: Boolean,
    showColorPicker: Boolean,
    showBorderColorPicker: Boolean,
    folderSearch: String,
    onFolderSearchChange: (String) -> Unit,
    onDismissActionPicker: () -> Unit,
    onDismissFolderPicker: () -> Unit,
    onDismissColorPicker: () -> Unit,
    onDismissBorderColorPicker: () -> Unit,
    onUpdate: (CornerDotConfig) -> Unit,
) {
    if (showActionPicker) {
        AlertDialog(
            onDismissRequest = onDismissActionPicker,
            title = { Text("Select Action") },
            text = {
                Column {
                    ACTION_LABELS.forEachIndexed { index, actionLabel ->
                        ListItem(
                            headlineContent = { Text(actionLabel) },
                            modifier = Modifier.fillMaxWidth()
                                .then(
                                    if (index == config.action)
                                        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                    else Modifier
                                )
                                .clickable {
                                    onUpdate(config.copy(action = index))
                                    onDismissActionPicker()
                                }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissActionPicker) { Text("Cancel") }
            }
        )
    }

    if (showFolderPicker) {
        val filtered = remember(folderSearch, allFolders) {
            if (folderSearch.isBlank()) allFolders
            else allFolders.filter { it.title.contains(folderSearch, ignoreCase = true) }
        }
        AlertDialog(
            onDismissRequest = onDismissFolderPicker,
            title = { Text("Select Folder") },
            text = {
                Column {
                    OutlinedTextField(
                        value = folderSearch,
                        onValueChange = onFolderSearchChange,
                        placeholder = { Text("Search…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(280.dp)) {
                        items(filtered, key = { it.id }) { folder ->
                            ListItem(
                                headlineContent = { Text(folder.title) },
                                modifier = Modifier.fillMaxWidth()
                                    .then(
                                        if (folder.id == config.folderId)
                                            Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                        else Modifier
                                    )
                                    .clickable {
                                        onUpdate(config.copy(folderId = folder.id))
                                        onDismissFolderPicker()
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissFolderPicker) { Text("Cancel") }
            }
        )
    }

    if (showColorPicker) {
        ColorPickerDialog(
            title = "Dot Color",
            currentColor = config.color,
            onDismiss = onDismissColorPicker,
            onColorSelected = { color ->
                onUpdate(config.copy(color = color))
                onDismissColorPicker()
            }
        )
    }

    if (showBorderColorPicker) {
        ColorPickerDialog(
            title = "Border Color",
            currentColor = config.borderColor,
            onDismiss = onDismissBorderColorPicker,
            onColorSelected = { color ->
                onUpdate(config.copy(borderColor = color))
                onDismissBorderColorPicker()
            }
        )
    }
}
