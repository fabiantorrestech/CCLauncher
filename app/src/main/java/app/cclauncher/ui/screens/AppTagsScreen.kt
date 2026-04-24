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
import app.cclauncher.helper.AppTagKeyUtils
import app.cclauncher.ui.BackHandler
import app.cclauncher.ui.components.AppTagsEditorDialog
import app.cclauncher.ui.components.ScrollbarIndicator

private data class TagEntryUiModel(
    val tag: String,
    val apps: List<AppEntryUiModel>,
)

private data class AppEntryUiModel(
    val appKey: String,
    val app: AppModel? = null,
    val displayLabel: String,
    val supportingText: String? = null,
    val isDeletedApp: Boolean = false,
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
    var editingAppKey by remember { mutableStateOf<String?>(null) }
    var tagToRename by remember { mutableStateOf<String?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val searchFocusRequester = remember { FocusRequester() }
    val normalizedQuery = remember(searchQuery) { searchQuery.trim() }

    val taggedApps = remember(allApps, appTags) {
        val appsByKey = allApps.associateBy { it.getKey() }

        val activeEntries = allApps
            .mapNotNull { app ->
                val sortedTags = appTags[app.getKey()].orEmpty()
                    .sortedWith(String.CASE_INSENSITIVE_ORDER)
                if (sortedTags.isEmpty()) null else AppEntryUiModel(
                    appKey = app.getKey(),
                    app = app,
                    displayLabel = app.appLabel,
                    tags = sortedTags,
                )
            }

        val retainedEntries = appTags.entries
            .asSequence()
            .filter { (appKey, tags) -> tags.isNotEmpty() && !appsByKey.containsKey(appKey) }
            .mapNotNull { (appKey, tags) ->
                val retained = AppTagKeyUtils.retainedRecordForKey(appKey) ?: return@mapNotNull null
                AppEntryUiModel(
                    appKey = retained.appKey,
                    displayLabel = retained.displayLabel,
                    supportingText = "Deleted app",
                    isDeletedApp = retained.isDeletedApp,
                    tags = tags.sortedWith(String.CASE_INSENSITIVE_ORDER),
                )
            }

        (activeEntries + retainedEntries)
            .sortedBy { it.displayLabel.lowercase() }
    }

    val tagEntries = remember(taggedApps) {
        buildMap<String, MutableList<AppEntryUiModel>> {
            taggedApps.forEach { entry ->
                entry.tags.forEach { tag ->
                    getOrPut(tag) { mutableListOf() }.add(entry)
                }
            }
        }
            .map { (tag, apps) ->
                TagEntryUiModel(
                    tag = tag,
                    apps = apps.sortedBy { it.displayLabel.lowercase() }
                )
            }
            .sortedBy { it.tag.lowercase() }
    }

    val tagEntriesWithDisplayNames = remember(tagEntries) {
        tagEntries.map { entry ->
            entry to entry.apps.map { appEntry ->
                buildString {
                    append(appEntry.displayLabel)
                    appEntry.supportingText?.let {
                        append(" (")
                        append(it)
                        append(')')
                    }
                }
            }.sortedBy { it.lowercase() }
        }
    }

    val filteredTagEntries = remember(tagEntriesWithDisplayNames, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            tagEntriesWithDisplayNames
        } else {
            tagEntriesWithDisplayNames.filter { (entry, displayNames) ->
                entry.tag.contains(normalizedQuery, ignoreCase = true) ||
                    displayNames.any { it.contains(normalizedQuery, ignoreCase = true) }
            }
        }
    }

    val filteredAppEntries = remember(taggedApps, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            taggedApps
        } else {
            taggedApps.filter { entry ->
                entry.displayLabel.contains(normalizedQuery, ignoreCase = true) ||
                    entry.supportingText.orEmpty().contains(normalizedQuery, ignoreCase = true) ||
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
                        items(filteredTagEntries, key = { it.first.tag }) { (entry, displayNames) ->
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
                                    text = displayNames.joinToString(", "),
                                    modifier = Modifier.padding(start = 20.dp, end = 12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        items(filteredAppEntries, key = { it.appKey }) { entry ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = entry.displayLabel,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    IconButton(onClick = {
                                        if (entry.app != null) {
                                            editingApp = entry.app
                                        } else {
                                            editingAppKey = entry.appKey
                                        }
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit app tags")
                                    }
                                }
                                entry.supportingText?.let { supportingText ->
                                    Text(
                                        text = supportingText,
                                        modifier = Modifier.padding(start = 20.dp, end = 12.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
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

    editingAppKey?.let { appKey ->
        val entry = taggedApps.firstOrNull { it.appKey == appKey }
        if (entry != null) {
            AppTagsEditorDialog(
                title = "Tags for ${entry.displayLabel}",
                initialTags = viewModel.getTagsForAppKey(appKey),
                onSave = { viewModel.saveTagsForAppKey(appKey, it) },
                onBack = { editingAppKey = null },
                onDone = { editingAppKey = null }
            )
        }
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
