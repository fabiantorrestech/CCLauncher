package app.cclauncher.ui.screens

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import app.cclauncher.ui.components.ScrollbarIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.cclauncher.helper.BitmapUtils.drawableToBitmap

data class WidgetListItem(
    val appName: String,
    val appPackage: String,
    val widgets: List<AppWidgetProviderInfo>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerScreen(
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val widgetManager = AppWidgetManager.getInstance(context)
    var widgetList by remember { mutableStateOf<List<WidgetListItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        isLoading = true
        widgetList = loadInstalledWidgets(context, widgetManager)
        isLoading = false
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) focusRequester.requestFocus()
    }

    val pm = context.packageManager
    val filteredList by remember(widgetList, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) widgetList
            else {
                widgetList.mapNotNull { group ->
                    if (group.appName.contains(searchQuery, ignoreCase = true)) {
                        group
                    } else {
                        val matching = group.widgets.filter {
                            it.loadLabel(pm).contains(searchQuery, ignoreCase = true)
                        }
                        if (matching.isNotEmpty()) group.copy(widgets = matching) else null
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search widgets...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                innerTextField()
                            }
                        )
                    } else {
                        Text("Select Widget")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            searchQuery = ""
                        } else {
                            onDismiss()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                }
            )
        },
//        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (filteredList.isEmpty()) {
                Text(
                    if (searchQuery.isNotBlank()) "No widgets match your search." else "No widgets found.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val listState = rememberLazyListState()
                // 1 header + N widgets + 1 divider per group
                val totalItems = filteredList.sumOf { 2 + it.widgets.size }

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        filteredList.forEach { group ->
                            item {
                                Text(
                                    text = group.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                                )
                            }
                            items(group.widgets, key = { it.provider.flattenToString() }) { widgetInfo ->
                                WidgetInfoItem(
                                    context = context,
                                    widgetInfo = widgetInfo,
                                    onClick = { onWidgetSelected(widgetInfo) }
                                )
                            }
                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    thickness = Dp.Hairline,
                                    color = Color.Transparent
                                )
                            }
                        }
                    }

                    ScrollbarIndicator(
                        listState = listState,
                        totalItems = totalItems,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetInfoItem(
    context: Context,
    widgetInfo: AppWidgetProviderInfo,
    onClick: () -> Unit
) {
    val pm = context.packageManager
    var previewImage by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(widgetInfo) {
        previewImage = loadWidgetPreview(context, widgetInfo)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show preview or app icon
        val imageBitmap = previewImage?.asImageBitmap()
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Widget preview",
                modifier = Modifier
                    .size(64.dp) // Adjust size as needed
                    .padding(end = 16.dp)
            )
        } else {
            // TODO: Fallback: Show App Icon - requires IconCache modification or direct load
            // Placeholder:
            Spacer(modifier = Modifier.size(64.dp).padding(end = 16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = widgetInfo.loadLabel(pm),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            Text(
                text = "${widgetInfo.minWidth}x${widgetInfo.minHeight}dp", // Show min size info
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper to load widgets and group them
private suspend fun loadInstalledWidgets(context: Context, widgetManager: AppWidgetManager): List<WidgetListItem> {
    return withContext(Dispatchers.IO) {
        val pm = context.packageManager
        widgetManager.installedProviders
            .groupBy { it.provider.packageName }
            .mapNotNull { (packageName, widgets) ->
                try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    WidgetListItem(appName, packageName, widgets.sortedBy { it.loadLabel(pm) })
                } catch (_: Exception) {
                    null // Skip if app info not found
                }
            }
            .sortedBy { it.appName }
    }
}

// Helper to load widget preview (can be slow)
private suspend fun loadWidgetPreview(
    context: Context,
    widgetInfo: AppWidgetProviderInfo
): Bitmap? {
    return withContext(Dispatchers.IO) {
        widgetInfo.loadPreviewImage(context, 0)?.let { drawable ->
            drawableToBitmap(drawable, defaultSize = 100)
        }
    }
}