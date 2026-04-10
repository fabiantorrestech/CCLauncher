package app.cclauncher.ui.screens

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import app.cclauncher.ui.components.ColorPickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    viewModel: MainViewModel,
    folderId: String,
    onNavigateBack: () -> Unit,
) {
    val homeLayout by viewModel.homeLayoutState.collectAsState()
    val folder = homeLayout.items.filterIsInstance<HomeItem.Folder>().find { it.id == folderId }

    if (folder == null) {
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
    var showColorPicker by remember { mutableStateOf(false) }

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
                Text("Grid Rows: ${gridRows.toInt()}", style = MaterialTheme.typography.labelLarge)
                Slider(
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
                Slider(
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
                    "App Text Size (Default): ${"%.1f".format(appTextSize)}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
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
                    TextButton(onClick = { showColorPicker = true }) {
                        Text(if (folder.titleTextColor != 0) "Change color" else "Set custom color")
                    }
                    if (folder.titleTextColor != 0) {
                        TextButton(onClick = { viewModel.updateFolderTitleColor(folderId, 0) }) {
                            Text("Reset")
                        }
                    }
                }
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

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            title = "Folder Text Color",
            currentColor = folder.titleTextColor,
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                viewModel.updateFolderTitleColor(folderId, color)
                showColorPicker = false
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
