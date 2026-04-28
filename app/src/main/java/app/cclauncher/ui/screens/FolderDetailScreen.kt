package app.cclauncher.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cclauncher.MainViewModel
import app.cclauncher.data.Constants
import app.cclauncher.data.FolderApp
import app.cclauncher.data.HomeItem
import app.cclauncher.data.HomeOrientation
import app.cclauncher.ui.components.AppSlider
import app.cclauncher.ui.components.ColorPickerDialog
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    viewModel: MainViewModel,
    folderId: String,
    onNavigateBack: () -> Unit,
) {
    val homeLayout by viewModel.homeLayoutState.collectAsState()
    val settingsSnapshot by viewModel.settingsSnapshot.collectAsState()
    val folder = remember(homeLayout.items, folderId) { viewModel.getAllFolders().find { it.id == folderId } }
    val portraitFolder = remember(settingsSnapshot, folderId) {
        viewModel.getFolder(folderId, HomeOrientation.PORTRAIT) ?: folder?.copy(showOnHome = false)
    }
    val landscapeFolder = remember(settingsSnapshot, folderId) {
        viewModel.getFolder(folderId, HomeOrientation.LANDSCAPE) ?: folder?.copy(showOnHome = false)
    }
    val portraitHasSpace = remember(settingsSnapshot, folderId) {
        viewModel.hasFreeSpaceForFolder(folderId, HomeOrientation.PORTRAIT)
    }
    val landscapeHasSpace = remember(settingsSnapshot, folderId) {
        viewModel.hasFreeSpaceForFolder(folderId, HomeOrientation.LANDSCAPE)
    }

    if (folder == null || portraitFolder == null || landscapeFolder == null) {
        // Folder was deleted while viewing — go back
        onNavigateBack()
        return
    }

    val appDrawerState by viewModel.appDrawerState.collectAsState()
    val allApps = appDrawerState.apps

    var folderTitle by remember(folder.title) { mutableStateOf(folder.title) }
    var gridRows by remember(folder.gridRows) { mutableFloatStateOf(folder.gridRows.toFloat()) }
    var gridColumns by remember(folder.gridColumns) { mutableFloatStateOf(folder.gridColumns.toFloat()) }
    var appTextSize by remember(folder.appTextSize) { mutableFloatStateOf(folder.appTextSize) }
    var showAppPicker by remember { mutableStateOf(false) }
    var appPickerSearch by remember { mutableStateOf("") }
    var colorPickerOrientation by remember { mutableStateOf<HomeOrientation?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var defaultFontOrientation by remember { mutableStateOf<HomeOrientation?>(null) }
    var portraitExpanded by remember { mutableStateOf(true) }
    var landscapeExpanded by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    val pickDefaultFontLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch {
                val newPath = viewModel.settingsRepository.importFontFile(uri, "folder_default_font")
                if (newPath != null) {
                    val orientation = defaultFontOrientation ?: HomeOrientation.PORTRAIT
                    viewModel.updateFolderDefaultAppFont(folderId, newPath, orientation)
                }
                defaultFontOrientation = null
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folder.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Folder Name", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = folderTitle,
                    onValueChange = { folderTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (folderTitle != folder.title) {
                            TextButton(onClick = {
                                viewModel.updateFolderTitle(folderId, folderTitle)
                            }) { Text("Save") }
                        }
                    }
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text(
                    "Contents stay shared. The sections below control each layout's own folder appearance and home visibility.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                FolderOrientationSection(
                    title = "Portrait Settings",
                    folder = portraitFolder,
                    expanded = portraitExpanded,
                    hasSpace = portraitHasSpace,
                    onExpandedChange = { portraitExpanded = it },
                    onShowOnHomeChange = { viewModel.setFolderShowOnHome(folderId, it, HomeOrientation.PORTRAIT) },
                    onHideTitleChange = { viewModel.setFolderHideTitle(folderId, it, HomeOrientation.PORTRAIT) },
                    onHideCloseButtonChange = { viewModel.setFolderHideCloseButton(folderId, it, HomeOrientation.PORTRAIT) },
                    onHideOutlineChange = { viewModel.setFolderHideOutline(folderId, it, HomeOrientation.PORTRAIT) },
                    onTapOutsideToCloseChange = { viewModel.setFolderTapOutsideToClose(folderId, it, HomeOrientation.PORTRAIT) },
                    onPickDefaultFont = {
                        defaultFontOrientation = HomeOrientation.PORTRAIT
                        pickDefaultFontLauncher.launch("font/*")
                    },
                    onResetDefaultFont = {
                        viewModel.updateFolderDefaultAppFont(folderId, "", HomeOrientation.PORTRAIT)
                    },
                    onPickColor = { colorPickerOrientation = HomeOrientation.PORTRAIT },
                    onResetColor = { viewModel.updateFolderTitleColor(folderId, 0, HomeOrientation.PORTRAIT) },
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                FolderOrientationSection(
                    title = "Landscape Settings",
                    folder = landscapeFolder,
                    expanded = landscapeExpanded,
                    hasSpace = landscapeHasSpace,
                    onExpandedChange = { landscapeExpanded = it },
                    onShowOnHomeChange = { viewModel.setFolderShowOnHome(folderId, it, HomeOrientation.LANDSCAPE) },
                    onHideTitleChange = { viewModel.setFolderHideTitle(folderId, it, HomeOrientation.LANDSCAPE) },
                    onHideCloseButtonChange = { viewModel.setFolderHideCloseButton(folderId, it, HomeOrientation.LANDSCAPE) },
                    onHideOutlineChange = { viewModel.setFolderHideOutline(folderId, it, HomeOrientation.LANDSCAPE) },
                    onTapOutsideToCloseChange = { viewModel.setFolderTapOutsideToClose(folderId, it, HomeOrientation.LANDSCAPE) },
                    onPickDefaultFont = {
                        defaultFontOrientation = HomeOrientation.LANDSCAPE
                        pickDefaultFontLauncher.launch("font/*")
                    },
                    onResetDefaultFont = {
                        viewModel.updateFolderDefaultAppFont(folderId, "", HomeOrientation.LANDSCAPE)
                    },
                    onPickColor = { colorPickerOrientation = HomeOrientation.LANDSCAPE },
                    onResetColor = { viewModel.updateFolderTitleColor(folderId, 0, HomeOrientation.LANDSCAPE) },
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text("Grid Rows: ${gridRows.toInt()}", style = MaterialTheme.typography.labelLarge)
                AppSlider(
                    value = gridRows,
                    onValueChange = { gridRows = it },
                    onValueChangeFinished = {
                        viewModel.updateFolderGridSize(folderId, gridRows.toInt(), gridColumns.toInt())
                    },
                    valueRange = Constants.GridSize.MIN_ROWS.toFloat()..Constants.GridSize.MAX_ROWS.toFloat(),
                    steps = Constants.GridSize.MAX_ROWS - Constants.GridSize.MIN_ROWS - 1,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                Text("Grid Columns: ${gridColumns.toInt()}", style = MaterialTheme.typography.labelLarge)
                AppSlider(
                    value = gridColumns,
                    onValueChange = { gridColumns = it },
                    onValueChangeFinished = {
                        viewModel.updateFolderGridSize(folderId, gridRows.toInt(), gridColumns.toInt())
                    },
                    valueRange = Constants.GridSize.MIN_COLUMNS.toFloat()..Constants.GridSize.MAX_COLUMNS.toFloat(),
                    steps = Constants.GridSize.MAX_COLUMNS - Constants.GridSize.MIN_COLUMNS - 1,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text(
                    "App Text Size (Default): ${"%.2f".format(appTextSize)}",
                    style = MaterialTheme.typography.labelLarge,
                )
                AppSlider(
                    value = appTextSize,
                    onValueChange = { appTextSize = it },
                    onValueChangeFinished = {
                        viewModel.updateFolderAppTextSize(folderId, appTextSize)
                    },
                    valueRange = 0.5f..2.0f,
                    steps = 29,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text("Folder App Font (Default)", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Each orientation can choose its own default app font above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Apps in this folder (${folder.apps.size})",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    IconButton(onClick = { showAppPicker = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add app")
                    }
                }
                HorizontalDivider()
            }

            if (folder.apps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No apps yet. Tap + to add one.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            } else {
                items(folder.apps, key = { "${it.appPackage}_${it.row}_${it.column}" }) { app ->
                    FolderAppListItem(
                        app = app,
                        onRemove = { viewModel.removeAppFromFolder(folderId, app) }
                    )
                    HorizontalDivider()
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Delete Folder") }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    colorPickerOrientation?.let { orientation ->
        val targetFolder = if (orientation == HomeOrientation.PORTRAIT) portraitFolder else landscapeFolder
        ColorPickerDialog(
            title = if (orientation == HomeOrientation.PORTRAIT) "Portrait Folder Text Color" else "Landscape Folder Text Color",
            currentColor = targetFolder.titleTextColor,
            onDismiss = { colorPickerOrientation = null },
            onColorSelected = { color ->
                viewModel.updateFolderTitleColor(folderId, color, orientation)
                colorPickerOrientation = null
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Folder") },
            text = { Text("Permanently delete \"${folder.title}\"? Apps inside will not be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeFolder(folder)
                        showDeleteConfirm = false
                        onNavigateBack()
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Inline app picker dialog
    if (showAppPicker) {
        val filteredApps = remember(appPickerSearch, allApps) {
            if (appPickerSearch.isBlank()) allApps
            else allApps.filter { it.appLabel.contains(appPickerSearch, ignoreCase = true) }
        }

        AlertDialog(
            onDismissRequest = { showAppPicker = false; appPickerSearch = "" },
            title = { Text("Add App to Folder") },
            text = {
                Column {
                    OutlinedTextField(
                        value = appPickerSearch,
                        onValueChange = { appPickerSearch = it },
                        placeholder = { Text("Search apps...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(filteredApps, key = { it.getKey() }) { app ->
                            ListItem(
                                headlineContent = { Text(app.appLabel) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addAppToFolder(folderId, app)
                                        showAppPicker = false
                                        appPickerSearch = ""
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppPicker = false; appPickerSearch = "" }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun FolderOrientationSection(
    title: String,
    folder: HomeItem.Folder,
    expanded: Boolean,
    hasSpace: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onShowOnHomeChange: (Boolean) -> Unit,
    onHideTitleChange: (Boolean) -> Unit,
    onHideCloseButtonChange: (Boolean) -> Unit,
    onHideOutlineChange: (Boolean) -> Unit,
    onTapOutsideToCloseChange: (Boolean) -> Unit,
    onPickDefaultFont: () -> Unit,
    onResetDefaultFont: () -> Unit,
    onPickColor: () -> Unit,
    onResetColor: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .animateContentSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (folder.showOnHome) "Visible on this home layout" else "Hidden from this home layout",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
            )
        }

        AnimatedVisibility(expanded) {
            Column {
                Spacer(Modifier.height(12.dp))
                val canEnable = folder.showOnHome || hasSpace
                FolderOrientationToggleRow(
                    title = "Show on Home Screen",
                    description = if (!folder.showOnHome && !hasSpace)
                        "No available grid space"
                    else
                        "Hide the tile on this home grid without deleting the folder.",
                    checked = folder.showOnHome,
                    onCheckedChange = { if (it && !canEnable) Unit else onShowOnHomeChange(it) },
                    enabled = canEnable,
                )
                FolderOrientationToggleRow(
                    title = "Hide Folder Name",
                    description = "Don't show the folder title in this layout's overlay header.",
                    checked = folder.hideTitle,
                    onCheckedChange = onHideTitleChange,
                )
                FolderOrientationToggleRow(
                    title = "Hide Close Button",
                    description = "Don't show the × button for this layout's overlay.",
                    checked = folder.hideCloseButton,
                    onCheckedChange = onHideCloseButtonChange,
                )
                FolderOrientationToggleRow(
                    title = "Hide Folder Outline",
                    description = "Remove the rounded border around this layout's folder overlay.",
                    checked = folder.hideOutline,
                    onCheckedChange = onHideOutlineChange,
                )
                FolderOrientationToggleRow(
                    title = "Tap Empty Space to Close",
                    description = "Dismiss the folder by tapping the backdrop or any empty grid cell.",
                    checked = folder.tapOutsideToClose,
                    onCheckedChange = onTapOutsideToCloseChange,
                )
                Text("Folder App Font (Default)", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (folder.defaultAppFontPath.isBlank()) {
                        "Uses folder/home/global fallback"
                    } else {
                        File(folder.defaultAppFontPath).name
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onPickDefaultFont) { Text("Select Font") }
                    if (folder.defaultAppFontPath.isNotBlank()) {
                        TextButton(onClick = onResetDefaultFont) { Text("Use Fallback") }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Folder Text Color", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (folder.titleTextColor != 0) Color(folder.titleTextColor)
                                else MaterialTheme.colorScheme.onSurface
                            )
                    )
                    TextButton(onClick = onPickColor) {
                        Text(if (folder.titleTextColor != 0) "Change color" else "Set custom color")
                    }
                    if (folder.titleTextColor != 0) {
                        TextButton(onClick = onResetColor) { Text("Reset") }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderOrientationToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun FolderAppListItem(
    app: FolderApp,
    onRemove: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(app.appLabel) },
        supportingContent = { Text(app.appPackage, style = MaterialTheme.typography.bodySmall) },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove from folder")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
