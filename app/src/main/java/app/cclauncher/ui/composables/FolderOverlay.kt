package app.cclauncher.ui.composables

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.cclauncher.data.FolderApp
import app.cclauncher.data.HomeItem
import app.cclauncher.helper.showToast
import app.cclauncher.settings.AppSettings
import app.cclauncher.ui.BackHandler

@Composable
fun FolderOverlay(
    folder: HomeItem.Folder,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onLaunchApp: (FolderApp) -> Unit,
    onMoveApp: (FolderApp, Int, Int) -> Unit,
    onRemoveApp: (FolderApp) -> Unit,
    onResizeApp: (FolderApp, Int, Int) -> Unit,
    onAppTextSizeChange: (FolderApp, Float) -> Unit = { _, _ -> },
    onAppLabelAlignmentChange: (FolderApp, Int) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    var movingApp by remember { mutableStateOf<FolderApp?>(null) }
    var appContextMenu by remember { mutableStateOf<FolderApp?>(null) }
    var resizingApp by remember { mutableStateOf<FolderApp?>(null) }
    var textSizingApp by remember { mutableStateOf<FolderApp?>(null) }
    var alignmentApp by remember { mutableStateOf<FolderApp?>(null) }

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = settings.folderBackgroundOpacity))
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp)),
            color = Color.Transparent,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            ) {
                // Folder title + close button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = folder.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        color = when {
                            folder.titleTextColor != 0 -> Color(folder.titleTextColor)
                            settings.useCustomTextColor && settings.textColor != 0 -> Color(settings.textColor)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp),
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close folder",
                            tint = Color.White,
                        )
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

        // Context menu for app inside folder
        appContextMenu?.let { app ->
            AlertDialog(
                onDismissRequest = { appContextMenu = null },
                title = { Text(app.appLabel) },
                text = {
                    Column {
                        DropdownMenuItem(
                            text = { Text("Open") },
                            onClick = { onLaunchApp(app); appContextMenu = null }
                        )
                        DropdownMenuItem(
                            text = { Text("Move") },
                            onClick = {
                                movingApp = app
                                appContextMenu = null
                                context.showToast("Tap where you want to move the app", Toast.LENGTH_SHORT)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Resize") },
                            onClick = { resizingApp = app; appContextMenu = null }
                        )
                        DropdownMenuItem(
                            text = { Text("Text Size") },
                            onClick = { textSizingApp = app; appContextMenu = null }
                        )
                        DropdownMenuItem(
                            text = { Text("Label Alignment") },
                            onClick = { alignmentApp = app; appContextMenu = null }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove from Folder") },
                            onClick = { onRemoveApp(app); appContextMenu = null }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { appContextMenu = null }) { Text("Close") }
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
                            Slider(
                                value = colSpan.toFloat(),
                                onValueChange = { colSpan = it.roundToInt().coerceIn(1, maxColSpan) },
                                valueRange = 1f..maxColSpan.toFloat(),
                                steps = (maxColSpan - 2).coerceAtLeast(0),
                            )
                        }
                        Text("Height (rows): $rowSpan")
                        if (maxRowSpan > 1) {
                            Slider(
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
                        Text("Size: ${"%.1f".format(textSize)}")
                        Slider(
                            value = textSize,
                            onValueChange = { textSize = it },
                            valueRange = 0.5f..2.0f,
                            steps = 29,
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
                    modifier = itemMod,
                )
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

    Text(
        text = folderApp.appLabel,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = effectiveFontSize,
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
