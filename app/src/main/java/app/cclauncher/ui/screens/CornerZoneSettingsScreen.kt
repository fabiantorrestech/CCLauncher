package app.cclauncher.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.cclauncher.MainViewModel
import app.cclauncher.data.Constants
import app.cclauncher.data.HomeOrientation
import app.cclauncher.data.HomeItem
import app.cclauncher.settings.AppSettings
import app.cclauncher.settings.CornerZoneConfig
import app.cclauncher.settings.applyToAllCornerZonesFor
import app.cclauncher.settings.cornerUniversalConfigFor
import app.cclauncher.settings.cornerZoneDangerFadeFor
import app.cclauncher.settings.cornerZonesInFoldersFor
import app.cclauncher.settings.cornerConfigFor
import app.cclauncher.settings.ZoneSwipeConfig
import app.cclauncher.ui.components.AppSlider
import app.cclauncher.ui.components.ColorPickerDialog
import app.cclauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val CORNER_LABELS = listOf("Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right")
private val ZONE_ACTION_LABELS = listOf("None", "Search", "Notifications", "App", "Next Page", "Previous Page", "Open Folder", "Open Settings")

private val SWIPE_DIR_LABELS = mapOf(
    Constants.ZoneSwipeDir.LEFT  to "Swipe Left",
    Constants.ZoneSwipeDir.RIGHT to "Swipe Right",
    Constants.ZoneSwipeDir.UP    to "Swipe Up",
    Constants.ZoneSwipeDir.DOWN  to "Swipe Down",
)

private fun HomeOrientation.label(): String =
    when (this) {
        HomeOrientation.PORTRAIT -> "Portrait"
        HomeOrientation.LANDSCAPE -> "Landscape"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CornerZoneSettingsScreen(
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {
    val settings by settingsViewModel.settingsState.collectAsState()
    val homeLayout by mainViewModel.homeLayoutState.collectAsState()
    val configuration = LocalConfiguration.current
    val activeOrientation by mainViewModel.activeHomeOrientation.collectAsState()
    val allFolders = remember(homeLayout.items) { mainViewModel.getAllFolders() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(configuration.orientation) {
        mainViewModel.updateActiveHomeOrientation(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    val cornerConfigs = listOf(
        settings.cornerConfigFor(activeOrientation, Constants.CornerPosition.TOP_LEFT),
        settings.cornerConfigFor(activeOrientation, Constants.CornerPosition.TOP_RIGHT),
        settings.cornerConfigFor(activeOrientation, Constants.CornerPosition.BOTTOM_LEFT),
        settings.cornerConfigFor(activeOrientation, Constants.CornerPosition.BOTTOM_RIGHT),
    )

    fun updateCornerConfig(corner: Int, config: CornerZoneConfig) {
        coroutineScope.launch {
            mainViewModel.settingsRepository.updateCornerZoneConfig(corner, config)
        }
    }

    fun updateUniversalConfig(config: CornerZoneConfig) {
        coroutineScope.launch {
            mainViewModel.settingsRepository.updateUniversalCornerZoneConfig(config)
        }
    }

    var previewCountdown by remember { mutableIntStateOf(0) }
    LaunchedEffect(previewCountdown) {
        if (previewCountdown > 0) {
            delay(1_000)
            previewCountdown--
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${activeOrientation.label()} Corner Zones") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (previewCountdown > 0) {
            CornerZoneLivePreview(settings = settings, activeOrientation = activeOrientation)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Corner zone limitations banner — always shown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Corner Zone Limitations",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(Modifier.height(6.dp))
                        val bullets = listOf(
                            "Bottom zones: swipe-up may conflict with Android's home gesture — cannot be fully prevented at the OS level. A fade near the bottom edge marks this area.",
                            "Top zones: swipe-down may conflict with the notification shade — especially with the status bar visible. A fade near the top edge marks this area.",
                            "These fades are visual only — actions still fire anywhere inside the zone.",
                            "A minimum dwell is enforced on bottom swipe-up and top swipe-down to reduce (not eliminate) conflicts.",
                        )
                        bullets.forEach { line ->
                            Row(modifier = Modifier.padding(bottom = 4.dp)) {
                                Text(
                                    "• ",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    line,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Preview button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${activeOrientation.label()} Preview", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Show ${activeOrientation.label().lowercase()} zones on screen for 3 seconds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    androidx.compose.material3.Button(
                        onClick = { previewCountdown = 3 },
                        enabled = previewCountdown == 0,
                    ) {
                        Text(if (previewCountdown > 0) "${previewCountdown}s" else "Preview")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(4.dp))
            }

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
                            "Control all ${activeOrientation.label().lowercase()} zone appearances from one place",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    Switch(
                        checked = settings.applyToAllCornerZonesFor(activeOrientation),
                        onCheckedChange = {
                            coroutineScope.launch {
                                settingsViewModel.updateSetting(
                                    when (activeOrientation) {
                                        HomeOrientation.PORTRAIT -> "portraitApplyToAllCornerZones"
                                        HomeOrientation.LANDSCAPE -> "landscapeApplyToAllCornerZones"
                                    },
                                    it
                                )
                            }
                        }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(4.dp))
            }

            // Corner zones in folders toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Inside Folders", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Keep ${activeOrientation.label().lowercase()} corner zones active when a folder is open",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    Switch(
                        checked = settings.cornerZonesInFoldersFor(activeOrientation),
                        onCheckedChange = {
                            coroutineScope.launch {
                                settingsViewModel.updateSetting(
                                    when (activeOrientation) {
                                        HomeOrientation.PORTRAIT -> "portraitCornerZonesInFolders"
                                        HomeOrientation.LANDSCAPE -> "landscapeCornerZonesInFolders"
                                    },
                                    it
                                )
                            }
                        }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(4.dp))
            }

            // Danger-edge fade toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Danger Edge Fade", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Show a subtle gradient where ${activeOrientation.label().lowercase()} zones may conflict with system gestures",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    Switch(
                        checked = settings.cornerZoneDangerFadeFor(activeOrientation),
                        onCheckedChange = {
                            coroutineScope.launch {
                                settingsViewModel.updateSetting(
                                    when (activeOrientation) {
                                        HomeOrientation.PORTRAIT -> "portraitCornerZoneDangerFade"
                                        HomeOrientation.LANDSCAPE -> "landscapeCornerZoneDangerFade"
                                    },
                                    it
                                )
                            }
                        }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(4.dp))
            }

            if (settings.applyToAllCornerZonesFor(activeOrientation)) {
                // Universal appearance card
                item {
                    Text(
                        "${activeOrientation.label()} Universal Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Text(
                        "These appearance settings apply to all enabled ${activeOrientation.label().lowercase()} corner zones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    CornerZoneAppearanceCard(
                        config = settings.cornerUniversalConfigFor(activeOrientation),
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
                        "Enable each ${activeOrientation.label().lowercase()} corner and set its actions independently",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }

                // Minimal per-corner cards (enabled + actions only)
                items(
                    (Constants.CornerPosition.TOP_LEFT..Constants.CornerPosition.BOTTOM_RIGHT).toList(),
                    key = { it },
                ) { corner ->
                    CornerZoneMinimalCard(
                        label = CORNER_LABELS[corner],
                        config = cornerConfigs[corner],
                        cornerPos = corner,
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
                    CornerZoneCard(
                        label = CORNER_LABELS[corner],
                        config = cornerConfigs[corner],
                        cornerPos = corner,
                        allFolders = allFolders,
                        onUpdate = { updated -> updateCornerConfig(corner, updated) },
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CornerZoneLivePreview(settings: AppSettings, activeOrientation: HomeOrientation) {
    val view = LocalView.current
    val density = LocalDensity.current

    val zones = listOf(
        Alignment.TopStart    to settings.cornerConfigFor(activeOrientation, Constants.CornerPosition.TOP_LEFT),
        Alignment.TopEnd      to settings.cornerConfigFor(activeOrientation, Constants.CornerPosition.TOP_RIGHT),
        Alignment.BottomStart to settings.cornerConfigFor(activeOrientation, Constants.CornerPosition.BOTTOM_LEFT),
        Alignment.BottomEnd   to settings.cornerConfigFor(activeOrientation, Constants.CornerPosition.BOTTOM_RIGHT),
    )
    val cornerPositions = listOf(
        Constants.CornerPosition.TOP_LEFT,
        Constants.CornerPosition.TOP_RIGHT,
        Constants.CornerPosition.BOTTOM_LEFT,
        Constants.CornerPosition.BOTTOM_RIGHT,
    )

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset.Zero,
        properties = PopupProperties(focusable = false, clippingEnabled = false),
    ) {
        val screenWidth = with(density) { view.rootView.width.toDp() }
        val screenHeight = with(density) { view.rootView.height.toDp() }
        if (screenWidth == 0.dp || screenHeight == 0.dp) return@Popup

        Box(modifier = Modifier.size(screenWidth, screenHeight)) {
            zones.forEachIndexed { index, (alignment, rawConfig) ->
                if (!rawConfig.enabled) return@forEachIndexed
                val config = if (settings.applyToAllCornerZonesFor(activeOrientation)) {
                    val u = settings.cornerUniversalConfigFor(activeOrientation)
                    rawConfig.copy(
                        size = u.size, color = u.color, opacity = u.opacity, visible = u.visible,
                        borderEnabled = u.borderEnabled, borderColor = u.borderColor, borderWidth = u.borderWidth,
                    )
                } else rawConfig

                val fillAlpha = if (config.visible) config.opacity else 0.3f
                val fillColor = Color(config.color).copy(alpha = fillAlpha)
                val strokeColor = Color(config.borderColor).copy(alpha = fillAlpha)
                val cornerPos = cornerPositions[index]
                val bwDp = config.borderWidth.dp

                Box(
                    modifier = Modifier
                        .align(alignment)
                        .size(config.size.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = buildPreviewTrianglePath(size.width, cornerPos)
                        drawPath(path, color = fillColor)
                        if (config.borderEnabled) {
                            drawPath(path, color = strokeColor, style = Stroke(width = bwDp.toPx()))
                        }
                    }
                }
            }
        }
    }
}

// Full card — used when "Apply to All" is OFF
@Composable
private fun CornerZoneCard(
    label: String,
    config: CornerZoneConfig,
    cornerPos: Int,
    allFolders: List<HomeItem.Folder>,
    onUpdate: (CornerZoneConfig) -> Unit,
) {
    var showActionPicker by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showHoldActionPicker by remember { mutableStateOf(false) }
    var showHoldFolderPicker by remember { mutableStateOf(false) }
    var swipePickerDir by remember { mutableStateOf<Constants.ZoneSwipeDir?>(null) }
    var swipeFolderDir by remember { mutableStateOf<Constants.ZoneSwipeDir?>(null) }
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
                ZonePreviewBadge(config = config, cornerPos = cornerPos)
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

            // Tap action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Tap Action", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { showActionPicker = true }) {
                    Text(ZONE_ACTION_LABELS.getOrElse(config.action) { "Unknown" })
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

            // Hold action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hold Action", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Fires after ${config.holdDurationMs} ms long-press",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Switch(
                    checked = config.holdEnabled,
                    onCheckedChange = { onUpdate(config.copy(holdEnabled = it)) },
                )
            }

            Text(
                "Hold Duration: ${config.holdDurationMs} ms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            AppSlider(
                value = config.holdDurationMs.toFloat(),
                onValueChange = { onUpdate(config.copy(holdDurationMs = it.toInt())) },
                valueRange = 200f..2000f,
                modifier = Modifier.fillMaxWidth(),
            )

            if (config.holdEnabled) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Hold Action", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { showHoldActionPicker = true }) {
                        Text(ZONE_ACTION_LABELS.getOrElse(config.holdAction) { "Unknown" })
                    }
                }

                if (config.holdAction == Constants.SwipeAction.OPEN_FOLDER) {
                    val holdFolderTitle = allFolders.find { it.id == config.holdFolderId }?.title ?: "Not set"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Folder", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { folderSearch = ""; showHoldFolderPicker = true }) {
                            Text(holdFolderTitle)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Swipe actions — only valid directions for this corner are shown
            Text("Swipe Actions", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Only directions pointing into the screen are available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )

            val isBottomCorner = cornerPos == Constants.CornerPosition.BOTTOM_LEFT ||
                                 cornerPos == Constants.CornerPosition.BOTTOM_RIGHT
            val dwellLabel = if (config.swipeDwellMs == 0) "Swipe Press Dwell: instant"
                             else "Swipe Press Dwell: ${config.swipeDwellMs} ms"
            val dwellSubLabel = if (isBottomCorner)
                "Minimum dwell before any swipe fires (bottom zones: swipe-up always uses at least 120 ms)"
            else
                "Minimum hold time before a swipe is recognised (0 = instant)"
            Spacer(Modifier.height(4.dp))
            Text(dwellLabel, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Text(dwellSubLabel, style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            AppSlider(
                value = config.swipeDwellMs.toFloat(),
                onValueChange = { onUpdate(config.copy(swipeDwellMs = it.toInt())) },
                valueRange = 0f..500f,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))

            Constants.validSwipeDirs(cornerPos).forEach { dir ->
                val swipeCfg = when (dir) {
                    Constants.ZoneSwipeDir.LEFT  -> config.swipeLeft
                    Constants.ZoneSwipeDir.RIGHT -> config.swipeRight
                    Constants.ZoneSwipeDir.UP    -> config.swipeUp
                    Constants.ZoneSwipeDir.DOWN  -> config.swipeDown
                }
                val dirLabel = SWIPE_DIR_LABELS[dir] ?: dir.name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(dirLabel, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { swipePickerDir = dir }) {
                        Text(
                            if (swipeCfg.enabled) ZONE_ACTION_LABELS.getOrElse(swipeCfg.action) { "Unknown" }
                            else "Disabled"
                        )
                    }
                }
                if (swipeCfg.enabled && swipeCfg.action == Constants.SwipeAction.OPEN_FOLDER) {
                    val title = allFolders.find { it.id == swipeCfg.folderId }?.title ?: "Not set"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Folder", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { folderSearch = ""; swipeFolderDir = dir }) {
                            Text(title)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ZoneAppearanceControls(
                config = config,
                onUpdate = onUpdate,
                onShowColorPicker = { showColorPicker = true },
                onShowBorderColorPicker = { showBorderColorPicker = true },
                onResetAppearance = {
                    val d = CornerZoneConfig()
                    onUpdate(config.copy(
                        size = d.size, color = d.color, opacity = d.opacity, visible = d.visible,
                        borderEnabled = d.borderEnabled, borderColor = d.borderColor, borderWidth = d.borderWidth,
                    ))
                },
            )
        }
    }

    CornerZoneDialogs(
        config = config,
        allFolders = allFolders,
        showActionPicker = showActionPicker,
        showFolderPicker = showFolderPicker,
        showHoldActionPicker = showHoldActionPicker,
        showHoldFolderPicker = showHoldFolderPicker,
        swipePickerDir = swipePickerDir,
        swipeFolderDir = swipeFolderDir,
        showColorPicker = showColorPicker,
        showBorderColorPicker = showBorderColorPicker,
        folderSearch = folderSearch,
        onFolderSearchChange = { folderSearch = it },
        onDismissActionPicker = { showActionPicker = false },
        onDismissFolderPicker = { showFolderPicker = false },
        onDismissHoldActionPicker = { showHoldActionPicker = false },
        onDismissHoldFolderPicker = { showHoldFolderPicker = false },
        onDismissSwipePicker = { swipePickerDir = null },
        onDismissSwipeFolderPicker = { swipeFolderDir = null },
        onDismissColorPicker = { showColorPicker = false },
        onDismissBorderColorPicker = { showBorderColorPicker = false },
        onUpdate = onUpdate,
    )
}

// Minimal card — used per-corner when "Apply to All" is ON
@Composable
private fun CornerZoneMinimalCard(
    label: String,
    config: CornerZoneConfig,
    cornerPos: Int,
    allFolders: List<HomeItem.Folder>,
    onUpdate: (CornerZoneConfig) -> Unit,
) {
    var showActionPicker by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showHoldActionPicker by remember { mutableStateOf(false) }
    var showHoldFolderPicker by remember { mutableStateOf(false) }
    var swipePickerDir by remember { mutableStateOf<Constants.ZoneSwipeDir?>(null) }
    var swipeFolderDir by remember { mutableStateOf<Constants.ZoneSwipeDir?>(null) }
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

            // Tap action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Tap Action", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { showActionPicker = true }) {
                    Text(ZONE_ACTION_LABELS.getOrElse(config.action) { "Unknown" })
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

            // Hold action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hold Action", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Fires after ${config.holdDurationMs} ms long-press",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                Switch(
                    checked = config.holdEnabled,
                    onCheckedChange = { onUpdate(config.copy(holdEnabled = it)) },
                )
            }

            Text(
                "Hold Duration: ${config.holdDurationMs} ms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            AppSlider(
                value = config.holdDurationMs.toFloat(),
                onValueChange = { onUpdate(config.copy(holdDurationMs = it.toInt())) },
                valueRange = 200f..2000f,
                modifier = Modifier.fillMaxWidth(),
            )

            if (config.holdEnabled) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Hold Action", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { showHoldActionPicker = true }) {
                        Text(ZONE_ACTION_LABELS.getOrElse(config.holdAction) { "Unknown" })
                    }
                }

                if (config.holdAction == Constants.SwipeAction.OPEN_FOLDER) {
                    val holdFolderTitle = allFolders.find { it.id == config.holdFolderId }?.title ?: "Not set"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Folder", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { folderSearch = ""; showHoldFolderPicker = true }) {
                            Text(holdFolderTitle)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Swipe actions
            Text("Swipe Actions", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Only directions pointing into the screen are available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )

            val isBottomCornerMinimal = cornerPos == Constants.CornerPosition.BOTTOM_LEFT ||
                                       cornerPos == Constants.CornerPosition.BOTTOM_RIGHT
            val dwellLabelMinimal = if (config.swipeDwellMs == 0) "Swipe Press Dwell: instant"
                                    else "Swipe Press Dwell: ${config.swipeDwellMs} ms"
            val dwellSubLabelMinimal = if (isBottomCornerMinimal)
                "Minimum dwell before any swipe fires (bottom zones: swipe-up always uses at least 120 ms)"
            else
                "Minimum hold time before a swipe is recognised (0 = instant)"
            Spacer(Modifier.height(4.dp))
            Text(dwellLabelMinimal, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Text(dwellSubLabelMinimal, style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            AppSlider(
                value = config.swipeDwellMs.toFloat(),
                onValueChange = { onUpdate(config.copy(swipeDwellMs = it.toInt())) },
                valueRange = 0f..500f,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Constants.validSwipeDirs(cornerPos).forEach { dir ->
                val swipeCfg = when (dir) {
                    Constants.ZoneSwipeDir.LEFT  -> config.swipeLeft
                    Constants.ZoneSwipeDir.RIGHT -> config.swipeRight
                    Constants.ZoneSwipeDir.UP    -> config.swipeUp
                    Constants.ZoneSwipeDir.DOWN  -> config.swipeDown
                }
                val dirLabel = SWIPE_DIR_LABELS[dir] ?: dir.name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(dirLabel, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { swipePickerDir = dir }) {
                        Text(
                            if (swipeCfg.enabled) ZONE_ACTION_LABELS.getOrElse(swipeCfg.action) { "Unknown" }
                            else "Disabled"
                        )
                    }
                }
                if (swipeCfg.enabled && swipeCfg.action == Constants.SwipeAction.OPEN_FOLDER) {
                    val title = allFolders.find { it.id == swipeCfg.folderId }?.title ?: "Not set"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Folder", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { folderSearch = ""; swipeFolderDir = dir }) {
                            Text(title)
                        }
                    }
                }
            }
        }
    }

    // Action / folder pickers for minimal card
    ActionPickerDialog(
        show = showActionPicker,
        currentAction = config.action,
        onDismiss = { showActionPicker = false },
        onSelect = { onUpdate(config.copy(action = it)); showActionPicker = false },
    )
    FolderPickerDialog(
        show = showFolderPicker,
        currentFolderId = config.folderId,
        allFolders = allFolders,
        folderSearch = folderSearch,
        onSearchChange = { folderSearch = it },
        onDismiss = { showFolderPicker = false },
        onSelect = { onUpdate(config.copy(folderId = it)); showFolderPicker = false },
    )
    ActionPickerDialog(
        show = showHoldActionPicker,
        currentAction = config.holdAction,
        onDismiss = { showHoldActionPicker = false },
        onSelect = { onUpdate(config.copy(holdAction = it)); showHoldActionPicker = false },
    )
    FolderPickerDialog(
        show = showHoldFolderPicker,
        currentFolderId = config.holdFolderId,
        allFolders = allFolders,
        folderSearch = folderSearch,
        onSearchChange = { folderSearch = it },
        onDismiss = { showHoldFolderPicker = false },
        onSelect = { onUpdate(config.copy(holdFolderId = it)); showHoldFolderPicker = false },
    )
    // Swipe direction action picker
    swipePickerDir?.let { dir ->
        val currentSwipeCfg = when (dir) {
            Constants.ZoneSwipeDir.LEFT  -> config.swipeLeft
            Constants.ZoneSwipeDir.RIGHT -> config.swipeRight
            Constants.ZoneSwipeDir.UP    -> config.swipeUp
            Constants.ZoneSwipeDir.DOWN  -> config.swipeDown
        }
        SwipeActionPickerDialog(
            dir = dir,
            currentCfg = currentSwipeCfg,
            onDismiss = { swipePickerDir = null },
            onSelect = { newCfg ->
                onUpdate(updateSwipeCfg(config, dir, newCfg))
                swipePickerDir = null
            },
        )
    }
    swipeFolderDir?.let { dir ->
        val currentSwipeCfg = when (dir) {
            Constants.ZoneSwipeDir.LEFT  -> config.swipeLeft
            Constants.ZoneSwipeDir.RIGHT -> config.swipeRight
            Constants.ZoneSwipeDir.UP    -> config.swipeUp
            Constants.ZoneSwipeDir.DOWN  -> config.swipeDown
        }
        FolderPickerDialog(
            show = true,
            currentFolderId = currentSwipeCfg.folderId,
            allFolders = allFolders,
            folderSearch = folderSearch,
            onSearchChange = { folderSearch = it },
            onDismiss = { swipeFolderDir = null },
            onSelect = { fid ->
                onUpdate(updateSwipeCfg(config, dir, currentSwipeCfg.copy(folderId = fid)))
                swipeFolderDir = null
            },
        )
    }
}

// Appearance-only card — used as universal control when "Apply to All" is ON
@Composable
private fun CornerZoneAppearanceCard(
    config: CornerZoneConfig,
    onUpdate: (CornerZoneConfig) -> Unit,
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
                ZonePreviewBadge(config = config, cornerPos = Constants.CornerPosition.TOP_LEFT)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ZoneAppearanceControls(
                config = config,
                onUpdate = onUpdate,
                onShowColorPicker = { showColorPicker = true },
                onShowBorderColorPicker = { showBorderColorPicker = true },
                onResetAppearance = {
                    val d = CornerZoneConfig()
                    onUpdate(config.copy(
                        size = d.size, color = d.color, opacity = d.opacity, visible = d.visible,
                        borderEnabled = d.borderEnabled, borderColor = d.borderColor, borderWidth = d.borderWidth,
                    ))
                },
            )
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            title = "Zone Color",
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

/** Small triangle rendered in card headers to preview the current zone config. */
@Composable
private fun ZonePreviewBadge(config: CornerZoneConfig, cornerPos: Int) {
    val previewSize = config.size.coerceIn(8f, 40f).dp
    val fillAlpha = if (config.visible) config.opacity else 0f
    val fillColor = Color(config.color).copy(alpha = fillAlpha)
    val strokeColor = Color(config.borderColor).copy(alpha = fillAlpha)
    val bwDp = config.borderWidth.dp

    Canvas(modifier = Modifier.size(previewSize)) {
        val path = buildPreviewTrianglePath(size.width, cornerPos)
        drawPath(path, color = fillColor)
        if (config.borderEnabled && config.visible) {
            drawPath(path, color = strokeColor, style = Stroke(width = bwDp.toPx()))
        }
    }
}

// Shared appearance controls (visible, size, opacity, color, border)
@Composable
private fun ZoneAppearanceControls(
    config: CornerZoneConfig,
    onUpdate: (CornerZoneConfig) -> Unit,
    onShowColorPicker: () -> Unit,
    onShowBorderColorPicker: () -> Unit,
    onResetAppearance: (() -> Unit)? = null,
) {
    if (onResetAppearance != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onResetAppearance) {
                Text(
                    "↺ Reset to Defaults",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Visible", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Invisible zones are still touchable",
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

    Text("Zone Size: ${"%.0f".format(config.size)} dp", style = MaterialTheme.typography.bodyMedium)
    AppSlider(
        value = config.size,
        onValueChange = { onUpdate(config.copy(size = it)) },
        valueRange = 32f..200f,
        modifier = Modifier.fillMaxWidth(),
    )

    Text("Opacity: ${"%.0f".format(config.opacity * 100)}%", style = MaterialTheme.typography.bodyMedium)
    AppSlider(
        value = config.opacity,
        onValueChange = { onUpdate(config.copy(opacity = it)) },
        valueRange = 0f..1f,
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
        AppSlider(
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

// --- Shared dialog composables ---

@Composable
private fun ActionPickerDialog(
    show: Boolean,
    currentAction: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Action") },
        text = {
            Column {
                ZONE_ACTION_LABELS.forEachIndexed { index, actionLabel ->
                    ListItem(
                        headlineContent = { Text(actionLabel) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (index == currentAction)
                                    Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                else Modifier
                            )
                            .clickable { onSelect(index) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun FolderPickerDialog(
    show: Boolean,
    currentFolderId: String,
    allFolders: List<HomeItem.Folder>,
    folderSearch: String,
    onSearchChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    if (!show) return
    val filtered = remember(folderSearch, allFolders) {
        if (folderSearch.isBlank()) allFolders
        else allFolders.filter { it.title.contains(folderSearch, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Folder") },
        text = {
            Column {
                OutlinedTextField(
                    value = folderSearch,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(filtered, key = { it.id }) { folder ->
                        ListItem(
                            headlineContent = { Text(folder.title) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (folder.id == currentFolderId)
                                        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                    else Modifier
                                )
                                .clickable { onSelect(folder.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Dialogs for CornerZoneCard
@Composable
private fun CornerZoneDialogs(
    config: CornerZoneConfig,
    allFolders: List<HomeItem.Folder>,
    showActionPicker: Boolean,
    showFolderPicker: Boolean,
    showHoldActionPicker: Boolean,
    showHoldFolderPicker: Boolean,
    swipePickerDir: Constants.ZoneSwipeDir?,
    swipeFolderDir: Constants.ZoneSwipeDir?,
    showColorPicker: Boolean,
    showBorderColorPicker: Boolean,
    folderSearch: String,
    onFolderSearchChange: (String) -> Unit,
    onDismissActionPicker: () -> Unit,
    onDismissFolderPicker: () -> Unit,
    onDismissHoldActionPicker: () -> Unit,
    onDismissHoldFolderPicker: () -> Unit,
    onDismissSwipePicker: () -> Unit,
    onDismissSwipeFolderPicker: () -> Unit,
    onDismissColorPicker: () -> Unit,
    onDismissBorderColorPicker: () -> Unit,
    onUpdate: (CornerZoneConfig) -> Unit,
) {
    ActionPickerDialog(
        show = showActionPicker,
        currentAction = config.action,
        onDismiss = onDismissActionPicker,
        onSelect = { onUpdate(config.copy(action = it)); onDismissActionPicker() },
    )
    FolderPickerDialog(
        show = showFolderPicker,
        currentFolderId = config.folderId,
        allFolders = allFolders,
        folderSearch = folderSearch,
        onSearchChange = onFolderSearchChange,
        onDismiss = onDismissFolderPicker,
        onSelect = { onUpdate(config.copy(folderId = it)); onDismissFolderPicker() },
    )
    ActionPickerDialog(
        show = showHoldActionPicker,
        currentAction = config.holdAction,
        onDismiss = onDismissHoldActionPicker,
        onSelect = { onUpdate(config.copy(holdAction = it)); onDismissHoldActionPicker() },
    )
    FolderPickerDialog(
        show = showHoldFolderPicker,
        currentFolderId = config.holdFolderId,
        allFolders = allFolders,
        folderSearch = folderSearch,
        onSearchChange = onFolderSearchChange,
        onDismiss = onDismissHoldFolderPicker,
        onSelect = { onUpdate(config.copy(holdFolderId = it)); onDismissHoldFolderPicker() },
    )
    // Swipe direction picker
    swipePickerDir?.let { dir ->
        val currentSwipeCfg = when (dir) {
            Constants.ZoneSwipeDir.LEFT  -> config.swipeLeft
            Constants.ZoneSwipeDir.RIGHT -> config.swipeRight
            Constants.ZoneSwipeDir.UP    -> config.swipeUp
            Constants.ZoneSwipeDir.DOWN  -> config.swipeDown
        }
        SwipeActionPickerDialog(
            dir = dir,
            currentCfg = currentSwipeCfg,
            onDismiss = onDismissSwipePicker,
            onSelect = { newCfg ->
                onUpdate(updateSwipeCfg(config, dir, newCfg))
                onDismissSwipePicker()
            },
        )
    }
    swipeFolderDir?.let { dir ->
        val currentSwipeCfg = when (dir) {
            Constants.ZoneSwipeDir.LEFT  -> config.swipeLeft
            Constants.ZoneSwipeDir.RIGHT -> config.swipeRight
            Constants.ZoneSwipeDir.UP    -> config.swipeUp
            Constants.ZoneSwipeDir.DOWN  -> config.swipeDown
        }
        FolderPickerDialog(
            show = true,
            currentFolderId = currentSwipeCfg.folderId,
            allFolders = allFolders,
            folderSearch = folderSearch,
            onSearchChange = onFolderSearchChange,
            onDismiss = onDismissSwipeFolderPicker,
            onSelect = { fid ->
                onUpdate(updateSwipeCfg(config, dir, currentSwipeCfg.copy(folderId = fid)))
                onDismissSwipeFolderPicker()
            },
        )
    }
    if (showColorPicker) {
        ColorPickerDialog(
            title = "Zone Color",
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

/**
 * Dialog for configuring a swipe direction on a corner zone.
 * Shows a "Disabled" option plus the full action list. Selecting an action enables the swipe;
 * selecting "Disabled" turns it off.
 */
@Composable
private fun SwipeActionPickerDialog(
    dir: Constants.ZoneSwipeDir,
    currentCfg: ZoneSwipeConfig,
    onDismiss: () -> Unit,
    onSelect: (ZoneSwipeConfig) -> Unit,
) {
    val dirLabel = SWIPE_DIR_LABELS[dir] ?: dir.name
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$dirLabel Action") },
        text = {
            Column {
                // "Disabled" entry
                ListItem(
                    headlineContent = { Text("Disabled") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (!currentCfg.enabled) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
                        .clickable { onSelect(currentCfg.copy(enabled = false, action = Constants.SwipeAction.NULL)) }
                )
                HorizontalDivider()
                ZONE_ACTION_LABELS.forEachIndexed { index, label ->
                    if (index == Constants.SwipeAction.NULL) return@forEachIndexed
                    ListItem(
                        headlineContent = { Text(label) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (currentCfg.enabled && currentCfg.action == index)
                                    Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                else Modifier
                            )
                            .clickable { onSelect(currentCfg.copy(enabled = true, action = index)) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Updates the correct swipe config field on [config] for the given [dir]. */
private fun updateSwipeCfg(
    config: CornerZoneConfig,
    dir: Constants.ZoneSwipeDir,
    newSwipeCfg: ZoneSwipeConfig,
): CornerZoneConfig = when (dir) {
    Constants.ZoneSwipeDir.LEFT  -> config.copy(swipeLeft  = newSwipeCfg)
    Constants.ZoneSwipeDir.RIGHT -> config.copy(swipeRight = newSwipeCfg)
    Constants.ZoneSwipeDir.UP    -> config.copy(swipeUp    = newSwipeCfg)
    Constants.ZoneSwipeDir.DOWN  -> config.copy(swipeDown  = newSwipeCfg)
}

private fun buildPreviewTrianglePath(size: Float, cornerPos: Int): Path = Path().apply {
    when (cornerPos) {
        Constants.CornerPosition.TOP_LEFT    -> { moveTo(0f, 0f);    lineTo(size, 0f);  lineTo(0f, size)  }
        Constants.CornerPosition.TOP_RIGHT   -> { moveTo(size, 0f);  lineTo(0f, 0f);   lineTo(size, size) }
        Constants.CornerPosition.BOTTOM_LEFT -> { moveTo(0f, size);  lineTo(0f, 0f);   lineTo(size, size) }
        else                                 -> { moveTo(size, size); lineTo(size, 0f); lineTo(0f, size)  }
    }
    close()
}
