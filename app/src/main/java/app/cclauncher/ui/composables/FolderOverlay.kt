package app.cclauncher.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import app.cclauncher.ui.components.AnimatedContextMenuDialog
import app.cclauncher.ui.components.ContextMenuItemRow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.cclauncher.ui.theme.AnimationConfig
import kotlinx.coroutines.delay
import app.cclauncher.LocalLauncherFontSettings
import app.cclauncher.data.Constants
import app.cclauncher.data.FolderApp
import app.cclauncher.data.HomeItem
import app.cclauncher.loadFontFamily
import app.cclauncher.settings.AppSettings
import app.cclauncher.ui.BackHandler
import app.cclauncher.ui.components.AppTagsEditorDialog
import app.cclauncher.ui.components.AppSlider
import app.cclauncher.ui.components.MoveDirection
import app.cclauncher.ui.components.MovePadOverlay

@Composable
fun FolderOverlay(
    folder: HomeItem.Folder,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onLaunchApp: (FolderApp) -> Unit,
    onMoveApp: (FolderApp, Int, Int) -> Unit,
    findNudgeTarget: (FolderApp, MoveDirection) -> Pair<Int, Int>?,
    onRemoveApp: (FolderApp) -> Unit,
    onResizeApp: (FolderApp, Int, Int) -> Unit,
    onAppRename: (FolderApp, String) -> Unit = { _, _ -> },
    appTagsForApp: (FolderApp) -> List<String> = { emptyList() },
    onSaveAppTags: (FolderApp, List<String>) -> Unit = { _, _ -> },
    onAppTextSizeChange: (FolderApp, Float) -> Unit = { _, _ -> },
    onAppLabelAlignmentChange: (FolderApp, Int) -> Unit = { _, _ -> },
    onAppIconPlacementChange: (FolderApp, Int) -> Unit = { _, _ -> },
    onAppSelectFont: (FolderApp) -> Unit = {},
    onAppResetFont: (FolderApp) -> Unit = {},
) {
    val context = LocalContext.current

    val animationsEnabled = settings.folderAnimationsEnabled
    var enterVisible by remember { mutableStateOf(false) }
    var exitRequested by remember { mutableStateOf(false) }
    val animAlpha by animateFloatAsState(
        targetValue = when { exitRequested -> 0f; enterVisible -> 1f; else -> 0f },
        animationSpec = when {
            !animationsEnabled -> androidx.compose.animation.core.snap()
            exitRequested -> AnimationConfig.overlayAlphaExitKeyframe
            else -> AnimationConfig.overlayAlphaEnter
        },
        label = "folderOverlayAlpha"
    )
    val animBlur by animateDpAsState(
        targetValue = when { exitRequested -> 28.dp; enterVisible -> 0.dp; else -> 28.dp },
        animationSpec = when {
            !animationsEnabled -> androidx.compose.animation.core.snap()
            exitRequested -> AnimationConfig.overlayBlurExit
            else -> AnimationConfig.overlayBlurEnter
        },
        label = "folderOverlayBlur"
    )
    LaunchedEffect(Unit) { enterVisible = true }
    LaunchedEffect(exitRequested) {
        if (exitRequested) {
            delay(if (animationsEnabled) AnimationConfig.OVERLAY_EXIT.toLong() else 0L)
            onDismiss()
        }
    }

    var movingApp by remember { mutableStateOf<FolderApp?>(null) }
    var appContextMenu by remember { mutableStateOf<FolderApp?>(null) }
    var appCustomizeMenu by remember { mutableStateOf<FolderApp?>(null) }
    var appTagsMenu by remember { mutableStateOf<FolderApp?>(null) }
    var appFontMenu by remember { mutableStateOf<FolderApp?>(null) }
    var appRenameMenu by remember { mutableStateOf<FolderApp?>(null) }
    var appRenameValue by remember { mutableStateOf("") }
    var resizingApp by remember { mutableStateOf<FolderApp?>(null) }
    var textSizingApp by remember { mutableStateOf<FolderApp?>(null) }
    var alignmentApp by remember { mutableStateOf<FolderApp?>(null) }
    var iconPlacementApp by remember { mutableStateOf<FolderApp?>(null) }
    val launcherFontSettings = LocalLauncherFontSettings.current
    val folderTitleFontFamily = remember(launcherFontSettings, folder.titleFontPath) {
        if (!launcherFontSettings.customFontsEnabled) {
            null
        } else {
            loadFontFamily(folder.titleFontPath)
                ?: loadFontFamily(launcherFontSettings.folderDefaultFontPath)
                ?: loadFontFamily(launcherFontSettings.mainFontPath)
        }
    }

    fun stepPosition(row: Int, column: Int, direction: MoveDirection): Pair<Int, Int> {
        return when (direction) {
            MoveDirection.Up -> row - 1 to column
            MoveDirection.Down -> row + 1 to column
            MoveDirection.Left -> row to column - 1
            MoveDirection.Right -> row to column + 1
        }
    }

    fun nudgeMovingApp(direction: MoveDirection): Boolean {
        val app = movingApp ?: return false
        val (newRow, newColumn) = findNudgeTarget(app, direction) ?: return false
        onMoveApp(app, newRow, newColumn)
        movingApp = app.copy(row = newRow, column = newColumn)
        return true
    }

    BackHandler { exitRequested = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = animAlpha }
            .blur(animBlur)
            .background(Color.Black.copy(alpha = settings.folderBackgroundOpacity))
            // Consume drag events so swipe gestures on the backdrop don't leak through
            // to the home screen gesture handler underneath.
            .pointerInput(Unit) { detectDragGestures(onDrag = { change, _ -> change.consume() }) }
            .then(
                if (folder.tapOutsideToClose)
                    Modifier.pointerInput(Unit) { detectTapGestures { exitRequested = true } }
                else Modifier
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (!folder.hideOutline)
                        Modifier.border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
                    else Modifier
                ),
            color = Color.Transparent,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            ) {
                // Folder title + close button — hidden individually if toggled off
                if (!folder.hideTitle || !folder.hideCloseButton) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!folder.hideTitle) {
                            Text(
                                text = folder.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = folderTitleFontFamily,
                                ),
                                color = when {
                                    folder.titleTextColor != 0 -> Color(folder.titleTextColor)
                                    settings.useCustomTextColor && settings.textColor != 0 -> Color(settings.textColor)
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = if (!folder.hideCloseButton) 40.dp else 0.dp),
                            )
                        }
                        if (!folder.hideCloseButton) {
                            IconButton(
                                onClick = { exitRequested = true },
                                modifier = Modifier.align(Alignment.CenterEnd),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close folder",
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                }

                // Folder grid
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(movingApp, folder.apps) {
                            detectTapGestures(
                                onLongPress = {
                                    // Long press while moving cancels move mode
                                    if (movingApp != null) {
                                        movingApp = null
                                        return@detectTapGestures
                                    }
                                    // Long press on an app — find which one
                                    val app = findFolderAppAtPosition(folder, it, size)
                                    if (app != null) {
                                        appContextMenu = app
                                    }
                                },
                                onTap = { offset ->
                                    if (movingApp != null) {
                                        val pos = calculateFolderGridPosition(offset, folder, size)
                                        if (pos != null) {
                                            onMoveApp(movingApp!!, pos.first, pos.second)
                                        }
                                        movingApp = null
                                    } else {
                                        val app = findFolderAppAtPosition(folder, offset, size)
                                        if (app != null) {
                                            onLaunchApp(app)
                                        } else if (folder.tapOutsideToClose) {
                                            exitRequested = true
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    FolderGridContent(
                        folder = folder,
                        settings = settings,
                        movingApp = movingApp,
                        appTextSizeScale = folder.appTextSize,
                    )
                }
            }
        }

        if (movingApp != null) {
            MovePadOverlay(onMove = ::nudgeMovingApp)
        }

        // Context menu for app inside folder
        val contextMenuApp = appContextMenu
        AnimatedContextMenuDialog(
            visible = contextMenuApp != null,
            onDismissRequest = { appContextMenu = null },
            animationsEnabled = settings.folderAnimationsEnabled,
            title = contextMenuApp?.let { app -> { Text(app.appLabel) } },
            confirmButton = {
                TextButton(onClick = { appContextMenu = null }) { Text("Close") }
            },
        ) {
            if (contextMenuApp != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    ContextMenuItemRow(
                        text = "Open",
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        animationsEnabled = settings.folderAnimationsEnabled,
                        onClick = { onLaunchApp(contextMenuApp); appContextMenu = null },
                    )
                    ContextMenuItemRow(
                        text = "Move",
                        icon = Icons.Default.OpenWith,
                        animationsEnabled = settings.folderAnimationsEnabled,
                        onClick = {
                            movingApp = contextMenuApp
                            appContextMenu = null
                        },
                    )
                    ContextMenuItemRow(
                        text = "Resize",
                        icon = Icons.Default.AspectRatio,
                        animationsEnabled = settings.folderAnimationsEnabled,
                        onClick = { resizingApp = contextMenuApp; appContextMenu = null },
                    )
                    ContextMenuItemRow(
                        text = "Customize...",
                        icon = Icons.Default.Edit,
                        animationsEnabled = settings.folderAnimationsEnabled,
                        onClick = { appCustomizeMenu = contextMenuApp; appContextMenu = null },
                    )
                    ContextMenuItemRow(
                        text = "Rename",
                        icon = Icons.Default.DriveFileRenameOutline,
                        animationsEnabled = settings.folderAnimationsEnabled,
                        onClick = {
                            appRenameValue = contextMenuApp.appLabel
                            appRenameMenu = contextMenuApp
                            appContextMenu = null
                        },
                    )
                    ContextMenuItemRow(
                        text = "Remove from Folder",
                        icon = Icons.Default.Delete,
                        animationsEnabled = settings.folderAnimationsEnabled,
                        onClick = { onRemoveApp(contextMenuApp); appContextMenu = null },
                    )
                }
            }
        }

        val customizeMenuApp = appCustomizeMenu
        AnimatedContextMenuDialog(
            visible = customizeMenuApp != null,
            onDismissRequest = { appCustomizeMenu = null },
            animationsEnabled = settings.folderAnimationsEnabled,
            title = customizeMenuApp?.let { app -> { Text("Customize ${app.appLabel}") } },
            confirmButton = {
                TextButton(onClick = { appCustomizeMenu = null }) { Text("Close") }
            },
        ) {
            if (customizeMenuApp != null) {
                ContextMenuItemRow(
                    text = "Text Size...",
                    icon = Icons.Default.AspectRatio,
                    animationsEnabled = settings.folderAnimationsEnabled,
                    onClick = { textSizingApp = customizeMenuApp; appCustomizeMenu = null },
                )
                ContextMenuItemRow(
                    text = "Label Alignment...",
                    icon = Icons.Default.Edit,
                    animationsEnabled = settings.folderAnimationsEnabled,
                    onClick = { alignmentApp = customizeMenuApp; appCustomizeMenu = null },
                )
                ContextMenuItemRow(
                    text = "Select Font...",
                    icon = Icons.Default.DriveFileRenameOutline,
                    animationsEnabled = settings.folderAnimationsEnabled,
                    onClick = { appFontMenu = customizeMenuApp; appCustomizeMenu = null },
                )
                ContextMenuItemRow(
                    text = "Tags...",
                    icon = Icons.Default.Edit,
                    animationsEnabled = settings.folderAnimationsEnabled,
                    onClick = { appTagsMenu = customizeMenuApp; appCustomizeMenu = null },
                )
                if (customizeMenuApp.isSystemShortcut && settings.showShortcutIcon) {
                    ContextMenuItemRow(
                        text = "Change Icon Placement...",
                        icon = Icons.Default.OpenWith,
                        animationsEnabled = settings.folderAnimationsEnabled,
                        onClick = { iconPlacementApp = customizeMenuApp; appCustomizeMenu = null },
                    )
                }
            }
        }

        appTagsMenu?.let { app ->
            AppTagsEditorDialog(
                title = "Tags for ${app.appLabel}",
                initialTags = appTagsForApp(app),
                onSave = { tags -> onSaveAppTags(app, tags) },
                onBack = {
                    appTagsMenu = null
                    appCustomizeMenu = app
                },
                onDone = {
                    appTagsMenu = null
                    appContextMenu = null
                }
            )
        }

        appFontMenu?.let { app ->
            AlertDialog(
                onDismissRequest = { appFontMenu = null },
                title = { Text("Select Font") },
                text = {
                    Column {
                        DropdownMenuItem(
                            text = { Text("Select Font...") },
                            onClick = {
                                onAppSelectFont(app)
                                appFontMenu = null
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Default Font") },
                            onClick = {
                                onAppResetFont(app)
                                appFontMenu = null
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { appFontMenu = null }) { Text("Close") }
                }
            )
        }

        appRenameMenu?.let { app ->
            AlertDialog(
                onDismissRequest = { appRenameMenu = null },
                title = { Text("Rename ${app.appLabel}") },
                text = {
                    androidx.compose.material3.OutlinedTextField(
                        value = appRenameValue,
                        onValueChange = { appRenameValue = it },
                        singleLine = true,
                        label = { Text("New name") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onAppRename(app, appRenameValue)
                        appRenameMenu = null
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { appRenameMenu = null }) { Text("Cancel") }
                }
            )
        }

        // Resize dialog for app inside folder
        resizingApp?.let { app ->
            val maxColSpan = (folder.gridColumns - app.column).coerceAtLeast(1)
            val maxRowSpan = (folder.gridRows - app.row).coerceAtLeast(1)
            var colSpan by remember(app) { mutableIntStateOf(app.columnSpan) }
            var rowSpan by remember(app) { mutableIntStateOf(app.rowSpan) }

            AlertDialog(
                onDismissRequest = { resizingApp = null },
                title = { Text("Resize ${app.appLabel}") },
                text = {
                    Column {
                        Text("Width (columns): $colSpan")
                        if (maxColSpan > 1) {
                            AppSlider(
                                value = colSpan.toFloat(),
                                onValueChange = { colSpan = it.roundToInt().coerceIn(1, maxColSpan) },
                                valueRange = 1f..maxColSpan.toFloat(),
                                steps = (maxColSpan - 2).coerceAtLeast(0),
                            )
                        }
                        Text("Height (rows): $rowSpan")
                        if (maxRowSpan > 1) {
                            AppSlider(
                                value = rowSpan.toFloat(),
                                onValueChange = { rowSpan = it.roundToInt().coerceIn(1, maxRowSpan) },
                                valueRange = 1f..maxRowSpan.toFloat(),
                                steps = (maxRowSpan - 2).coerceAtLeast(0),
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (colSpan != app.columnSpan || rowSpan != app.rowSpan) {
                            onResizeApp(app, rowSpan, colSpan)
                        }
                        resizingApp = null
                    }) { Text("Apply") }
                },
                dismissButton = {
                    TextButton(onClick = { resizingApp = null }) { Text("Cancel") }
                }
            )
        }

        // Text size dialog for app inside folder
        textSizingApp?.let { app ->
            var textSize by remember(app) { mutableFloatStateOf(app.appTextSize) }
            AlertDialog(
                onDismissRequest = { textSizingApp = null },
                title = { Text("Text Size") },
                text = {
                    Column {
                        Text("Size: ${"%.2f".format(textSize)}")
                        AppSlider(
                            value = textSize,
                            onValueChange = { textSize = it },
                            valueRange = 0.5f..3.0f,
                            steps = 49,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { textSize = 1.0f }
                        ) {
                            Text("Apply default text size (folder)")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onAppTextSizeChange(app, textSize)
                        textSizingApp = null
                    }) { Text("Apply") }
                },
                dismissButton = {
                    TextButton(onClick = { textSizingApp = null }) { Text("Cancel") }
                }
            )
        }

        // Label alignment dialog for app inside folder
        alignmentApp?.let { app ->
            LabelAlignmentDialog(
                currentAlignment = app.appLabelAlignment,
                onDismiss = { alignmentApp = null },
                onAlignmentSelected = { alignment ->
                    onAppLabelAlignmentChange(app, alignment)
                    alignmentApp = null
                }
            )
        }

        iconPlacementApp?.let { app ->
            IconPlacementDialog(
                currentPlacement = app.iconPlacement,
                onDismiss = { iconPlacementApp = null },
                onPlacementSelected = { placement ->
                    onAppIconPlacementChange(app, placement)
                    iconPlacementApp = null
                }
            )
        }
    }
}

@Composable
private fun FolderGridContent(
    folder: HomeItem.Folder,
    settings: AppSettings,
    movingApp: FolderApp?,
    appTextSizeScale: Float = 1.0f,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = 8.dp
        val verticalPadding = 8.dp
        val usableWidth = maxWidth - horizontalPadding * 2
        val usableHeight = maxHeight - verticalPadding * 2
        val cellWidth = usableWidth / folder.gridColumns
        val cellHeight = usableHeight / folder.gridRows

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            folder.apps.forEach { app ->
                val isMoving = movingApp == app

                val itemMod = Modifier
                    .offset(x = cellWidth * app.column, y = cellHeight * app.row)
                    .size(width = cellWidth * app.columnSpan, height = cellHeight * app.rowSpan)
                    .then(
                        if (isMoving)
                            Modifier
                                .alpha(0.6f)
                                .border(2.dp, Color.White, RoundedCornerShape(4.dp))
                        else Modifier
                    )

                HomeFolderAppItem(
                    folderApp = app,
                    settings = settings,
                    appTextSizeScale = appTextSizeScale,
                    folderTextColor = folder.titleTextColor,
                    folderDefaultAppFontPath = folder.defaultAppFontPath,
                    modifier = itemMod,
                )
            }
        }

        if (movingApp != null && settings.showMoveGridOverlay) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val hPadPx = horizontalPadding.toPx()
                val vPadPx = verticalPadding.toPx()
                val usableW = size.width - hPadPx * 2
                val usableH = size.height - vPadPx * 2
                val cellW = usableW / folder.gridColumns
                val cellH = usableH / folder.gridRows
                val lineColor = Color.White.copy(alpha = 0.25f)
                val strokePx = 1.dp.toPx()

                for (col in 0..folder.gridColumns) {
                    val x = hPadPx + col * cellW
                    drawLine(lineColor, Offset(x, vPadPx), Offset(x, vPadPx + usableH), strokePx)
                }
                for (row in 0..folder.gridRows) {
                    val y = vPadPx + row * cellH
                    drawLine(lineColor, Offset(hPadPx, y), Offset(hPadPx + usableW, y), strokePx)
                }
            }
        }
    }
}

@Composable
private fun HomeFolderAppItem(
    folderApp: FolderApp,
    settings: AppSettings,
    appTextSizeScale: Float = 1.0f,
    folderTextColor: Int = 0,
    folderDefaultAppFontPath: String = "",
    modifier: Modifier = Modifier,
) {
    val textColor = when {
        folderTextColor != 0 -> Color(folderTextColor)
        settings.useCustomTextColor && settings.textColor != 0 -> Color(settings.textColor)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val fontWeight = when (settings.fontWeight) {
        0 -> FontWeight.Thin; 1 -> FontWeight.Light; 2 -> FontWeight.Normal
        3 -> FontWeight.Medium; 4 -> FontWeight.Bold; 5 -> FontWeight.Black
        else -> FontWeight.Normal
    }
    val effectiveFontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * appTextSizeScale * folderApp.appTextSize).let {
        androidx.compose.ui.unit.TextUnit(it, androidx.compose.ui.unit.TextUnitType.Sp)
    }
    val effectiveAlignment = if (folderApp.appLabelAlignment >= 0) folderApp.appLabelAlignment else settings.appLabelAlignment
    val textAlign = when (effectiveAlignment) {
        1 -> TextAlign.Center
        2 -> TextAlign.Right
        else -> TextAlign.Left
    }
    val rowArrangement = when (effectiveAlignment) {
        1 -> Arrangement.Center
        2 -> Arrangement.End
        else -> Arrangement.Start
    }
    val showShortcutIcon = folderApp.isSystemShortcut && settings.showShortcutIcon
    val showShortcutIconOnRight = folderApp.iconPlacement == Constants.IconPlacement.RIGHT
    val launcherFontSettings = LocalLauncherFontSettings.current
    val labelFontFamily = remember(launcherFontSettings, folderApp.labelFontPath, folderDefaultAppFontPath) {
        if (!launcherFontSettings.customFontsEnabled) {
            null
        } else {
            loadFontFamily(folderApp.labelFontPath)
                ?: loadFontFamily(folderDefaultAppFontPath)
                ?: loadFontFamily(launcherFontSettings.folderDefaultFontPath)
                ?: loadFontFamily(launcherFontSettings.mainFontPath)
        }
    }

    if (showShortcutIcon) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = rowArrangement,
        ) {
            if (!showShortcutIconOnRight) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.size(4.dp))
            }
            Text(
                text = folderApp.appLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = effectiveFontSize,
                    fontFamily = labelFontFamily,
                    fontWeight = fontWeight,
                ),
                color = textColor,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (showShortcutIconOnRight) {
                Spacer(modifier = Modifier.size(4.dp))
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    } else {
        Text(
            text = folderApp.appLabel,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = effectiveFontSize,
                fontFamily = labelFontFamily,
                fontWeight = fontWeight,
            ),
            color = textColor,
            textAlign = textAlign,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = modifier
                .fillMaxSize()
                .padding(4.dp),
        )
    }
}

@Composable
fun LabelAlignmentDialog(
    currentAlignment: Int,
    onDismiss: () -> Unit,
    onAlignmentSelected: (Int) -> Unit,
) {
    val options = listOf("Default" to -1, "Left" to 0, "Center" to 1, "Right" to 2)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Label Alignment") },
        text = {
            Column {
                options.forEach { (label, value) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (value == currentAlignment) "$label (current)" else label,
                                color = if (value == currentAlignment)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = { if (value != currentAlignment) onAlignmentSelected(value) },
                        enabled = value != currentAlignment,
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
private fun IconPlacementDialog(
    currentPlacement: Int,
    onDismiss: () -> Unit,
    onPlacementSelected: (Int) -> Unit,
) {
    val options = listOf("Left" to Constants.IconPlacement.LEFT, "Right" to Constants.IconPlacement.RIGHT)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Icon Placement") },
        text = {
            Column {
                options.forEach { (label, value) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (value == currentPlacement) "$label (current)" else label,
                                color = if (value == currentPlacement)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        onClick = { if (value != currentPlacement) onPlacementSelected(value) },
                        enabled = value != currentPlacement,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun findFolderAppAtPosition(
    folder: HomeItem.Folder,
    position: Offset,
    size: IntSize,
): FolderApp? {
    val cellWidth = size.width.toFloat() / folder.gridColumns
    val cellHeight = size.height.toFloat() / folder.gridRows
    val column = (position.x / cellWidth).toInt()
    val row = (position.y / cellHeight).toInt()
    return folder.apps.find { app ->
        row >= app.row && row < app.row + app.rowSpan &&
            column >= app.column && column < app.column + app.columnSpan
    }
}

private fun calculateFolderGridPosition(
    position: Offset,
    folder: HomeItem.Folder,
    size: IntSize,
): Pair<Int, Int>? {
    val cellWidth = size.width.toFloat() / folder.gridColumns
    val cellHeight = size.height.toFloat() / folder.gridRows
    val column = (position.x / cellWidth).toInt()
    val row = (position.y / cellHeight).toInt()
    return if (row in 0 until folder.gridRows && column in 0 until folder.gridColumns)
        Pair(row, column) else null
}
