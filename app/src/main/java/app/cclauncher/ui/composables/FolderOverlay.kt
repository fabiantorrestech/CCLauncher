package app.cclauncher.ui.composables

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.constraintlayout.compose.ConstraintLayout
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
) {
    val context = LocalContext.current
    var movingApp by remember { mutableStateOf<FolderApp?>(null) }
    var appContextMenu by remember { mutableStateOf<FolderApp?>(null) }
    var resizingApp by remember { mutableStateOf<FolderApp?>(null) }

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
                // Folder title
                Text(
                    text = folder.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (settings.useCustomTextColor && settings.textColor != 0)
                        Color(settings.textColor) else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )

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

        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            val refs = folder.apps.associate { it.hashCode() to createRef() }

            folder.apps.forEach { app ->
                val isMoving = movingApp == app
                val ref = refs[app.hashCode()] ?: return@forEach

                val itemMod = Modifier
                    .constrainAs(ref) {
                        top.linkTo(parent.top, margin = cellHeight * app.row)
                        start.linkTo(parent.start, margin = cellWidth * app.column)
                        width = androidx.constraintlayout.compose.Dimension.value(cellWidth * app.columnSpan)
                        height = androidx.constraintlayout.compose.Dimension.value(cellHeight * app.rowSpan)
                    }
                    .then(
                        if (isMoving)
                            Modifier
                                .alpha(0.6f)
                                .border(2.dp, Color.White, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                        else Modifier
                    )

                HomeFolderAppItem(
                    folderApp = app,
                    settings = settings,
                    appTextSizeScale = appTextSizeScale,
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
    modifier: Modifier = Modifier,
) {
    val textColor = if (settings.useCustomTextColor && settings.textColor != 0)
        Color(settings.textColor) else MaterialTheme.colorScheme.onSurface
    val fontWeight = when (settings.fontWeight) {
        0 -> FontWeight.Thin; 1 -> FontWeight.Light; 2 -> FontWeight.Normal
        3 -> FontWeight.Medium; 4 -> FontWeight.Bold; 5 -> FontWeight.Black
        else -> FontWeight.Normal
    }
    val effectiveFontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * appTextSizeScale).let {
        androidx.compose.ui.unit.TextUnit(it, androidx.compose.ui.unit.TextUnitType.Sp)
    }

    Text(
        text = folderApp.appLabel,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = effectiveFontSize,
            fontWeight = fontWeight,
        ),
        color = textColor,
        maxLines = 2,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp),
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
