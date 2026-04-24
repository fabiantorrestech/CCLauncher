package app.cclauncher.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cclauncher.MainViewModel
import app.cclauncher.data.AppModel
import app.cclauncher.ui.BackHandler
import app.cclauncher.ui.components.AppTagsEditorDialog
import app.cclauncher.ui.components.ScrollbarIndicator

private data class TagEntryUiModel(
    val tag: String,
    val apps: List<AppModel>,
)

private data class AppEntryUiModel(
    val app: AppModel,
    val tags: List<String>,
)

@Composable
fun AppTagsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
) {
    val appTags by viewModel.appTags.collectAsState()
    val allApps by viewModel.appListAll.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingApp by remember { mutableStateOf<AppModel?>(null) }
    var tagToRename by remember { mutableStateOf<String?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val searchFocusRequester = remember { FocusRequester() }
    val normalizedQuery = remember(searchQuery) { searchQuery.trim() }

    val taggedApps = remember(allApps, appTags) {
        allApps
            .mapNotNull { app ->
                val sortedTags = appTags[app.getKey()].orEmpty()
                    .sortedWith(String.CASE_INSENSITIVE_ORDER)
                if (sortedTags.isEmpty()) null else AppEntryUiModel(app = app, tags = sortedTags)
            }
            .sortedBy { it.app.appLabel.lowercase() }
    }

    val tagEntries = remember(taggedApps) {
        buildMap<String, MutableList<AppModel>> {
            taggedApps.forEach { entry ->
                entry.tags.forEach { tag ->
                    getOrPut(tag) { mutableListOf() }.add(entry.app)
                }
            }
        }
            .map { (tag, apps) ->
                TagEntryUiModel(
                    tag = tag,
                    apps = apps.sortedBy { it.appLabel.lowercase() }
                )
            }
            .sortedBy { it.tag.lowercase() }
    }

    val filteredTagEntries = remember(tagEntries, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            tagEntries
        } else {
            tagEntries.filter { entry ->
                entry.tag.contains(normalizedQuery, ignoreCase = true) ||
                    entry.apps.any { it.appLabel.contains(normalizedQuery, ignoreCase = true) }
            }
        }
    }

    val filteredAppEntries = remember(taggedApps, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            taggedApps
        } else {
            taggedApps.filter { entry ->
                entry.app.appLabel.contains(normalizedQuery, ignoreCase = true) ||
                    entry.tags.any { it.contains(normalizedQuery, ignoreCase = true) }
            }
        }
    }

    val showingTagsTab = selectedTab == 0
    val resultCount = if (showingTagsTab) filteredTagEntries.size else filteredAppEntries.size
    val showScrollbar = resultCount > 0

    BackHandler {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(selectedTab, normalizedQuery) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(if (showingTagsTab) "Search tags..." else "Search apps...")
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            )
                        )
                    } else {
                        Text("App Tags")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            searchQuery = ""
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSearchActive) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search app tags")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = showingTagsTab,
                    onClick = { selectedTab = 0 },
                    text = { Text("Tags") }
                )
                Tab(
                    selected = !showingTagsTab,
                    onClick = { selectedTab = 1 },
                    text = { Text("Apps") }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val scrollbarPadding = if (showScrollbar) Modifier.padding(end = 12.dp) else Modifier

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(scrollbarPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (showingTagsTab) {
                        items(filteredTagEntries, key = { it.tag }) { entry ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = entry.tag,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    IconButton(onClick = {
                                        tagToRename = entry.tag
                                        renameValue = entry.tag
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit tag")
                                    }
                                    IconButton(onClick = { viewModel.removeTagAcrossApps(entry.tag) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete tag")
                                    }
                                }
                                Text(
                                    text = entry.apps.joinToString(", ") { it.appLabel },
                                    modifier = Modifier.padding(start = 20.dp, end = 12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        items(filteredAppEntries, key = { it.app.getKey() }) { entry ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = entry.app.appLabel,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    IconButton(onClick = { editingApp = entry.app }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit app tags")
                                    }
                                }
                                Text(
                                    text = entry.tags.joinToString(", "),
                                    modifier = Modifier.padding(start = 20.dp, end = 12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                if (showScrollbar) {
                    ScrollbarIndicator(
                        listState = listState,
                        totalItems = resultCount,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }
        }
    }

    editingApp?.let { app ->
        AppTagsEditorDialog(
            title = "Tags for ${app.appLabel}",
            initialTags = appTags[app.getKey()].orEmpty(),
            onSave = { viewModel.saveTagsForApp(app, it) },
            onBack = { editingApp = null },
            onDone = { editingApp = null }
        )
    }

    tagToRename?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToRename = null },
            title = { Text("Edit tag") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    singleLine = true,
                    label = { Text("Tag") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameTagAcrossApps(tag, renameValue)
                    tagToRename = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
