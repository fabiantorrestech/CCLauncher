package app.cclauncher.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cclauncher.MainViewModel
import app.cclauncher.data.HomeItem
import app.cclauncher.data.HomeOrientation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderListScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFolder: (String) -> Unit,
) {
    val homeLayout by viewModel.homeLayoutState.collectAsState()
    val folders = remember(homeLayout.items) { viewModel.getAllFolders() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var pendingCreateOrientationPrompt by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newFolderName = ""
            },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    singleLine = true,
                    label = { Text("Folder name") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (viewModel.availableHomeOrientations().size > 1) {
                        pendingCreateOrientationPrompt = true
                    } else {
                        viewModel.addFolderToHomeScreen(newFolderName)
                        showCreateDialog = false
                        newFolderName = ""
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newFolderName = ""
                }) { Text("Cancel") }
            }
        )
    }

    if (pendingCreateOrientationPrompt) {
        AlertDialog(
            onDismissRequest = { pendingCreateOrientationPrompt = false },
            title = { Text("Place Folder On Home") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose where to place this folder tile.")
                    Text(
                        "The folder contents stay shared, but portrait and landscape positions are separate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        viewModel.addFolderToHomeScreen(newFolderName, HomeOrientation.PORTRAIT)
                        pendingCreateOrientationPrompt = false
                        showCreateDialog = false
                        newFolderName = ""
                    }) { Text("Add to Portrait") }
                    TextButton(onClick = {
                        viewModel.addFolderToHomeScreen(newFolderName, HomeOrientation.LANDSCAPE)
                        pendingCreateOrientationPrompt = false
                        showCreateDialog = false
                        newFolderName = ""
                    }) { Text("Add to Landscape") }
                    TextButton(onClick = { pendingCreateOrientationPrompt = false }) { Text("Cancel") }
                }
            },
            dismissButton = {}
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Folders") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New Folder")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Folder")
            }
        }
    ) { innerPadding ->
        if (folders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Text(
                        text = "No folders yet.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "Tap + to create your first folder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(folders, key = { it.id }) { folder ->
                    ListItem(
                        leadingContent = {
                            Icon(Icons.Default.Folder, contentDescription = null)
                        },
                        headlineContent = { Text(folder.title) },
                        supportingContent = {
                            Text(
                                "${folder.apps.size} app${if (folder.apps.size != 1) "s" else ""} · " +
                                    "Grid: ${folder.gridRows}×${folder.gridColumns}"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToFolder(folder.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
