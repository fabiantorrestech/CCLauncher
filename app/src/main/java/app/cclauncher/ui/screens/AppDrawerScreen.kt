package app.cclauncher.ui.screens

import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import app.cclauncher.ui.components.ScrollbarIndicator
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.unit.Dp

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.math.floor
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.cclauncher.MainViewModel
import app.cclauncher.loadFontFamily
import app.cclauncher.data.AppShortcut
import app.cclauncher.data.AppModel
import app.cclauncher.data.Constants
import app.cclauncher.data.HomeItem
import app.cclauncher.data.HomeOrientation
import app.cclauncher.helper.isSystemApp
import app.cclauncher.helper.openAppInfo
import app.cclauncher.helper.openSearch
import app.cclauncher.helper.rememberAppIcon
import app.cclauncher.helper.uninstall
import app.cclauncher.ui.BackHandler
import app.cclauncher.ui.components.AppListItem
import app.cclauncher.ui.components.AnimatedContextMenuDialog
import app.cclauncher.ui.components.AppTagsEditorDialog
import app.cclauncher.ui.components.ContextMenuItemRow
import app.cclauncher.ui.components.PrivateSpaceIndicator
import app.cclauncher.ui.components.PrivateSpaceToggle
import app.cclauncher.ui.theme.AnimationConfig
import app.cclauncher.data.KeyModifier
import app.cclauncher.ui.composables.KeyboardShortcutDialog
import app.cclauncher.ui.util.detectSwipeGestures
import app.cclauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.yield
import org.koin.androidx.compose.koinViewModel

private data class AppDrawerMenuAction(
    val text: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel = koinViewModel(),
    onAppClick: (AppModel) -> Unit,
    selectionMode: Boolean = false,
    selectionTitle: String = "",
    onNavigateToSettings: () -> Unit = {},
    onSwipeDown: () -> Unit, // This is the primary action to go "home" or navigate back
) {
    BackHandler(onBack = onSwipeDown)

    val context = LocalContext.current
    val uiState by viewModel.appDrawerState.collectAsState()
    val settings by settingsViewModel.settingsState.collectAsState()
    val homeLayoutState by viewModel.homeLayoutState.collectAsState()
    val homeFolders by remember(homeLayoutState.items) { derivedStateOf { viewModel.getAllFolders() } }

    val searchQuery = uiState.searchQuery
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var isSearchFocused by remember { mutableStateOf(false) }
    var hasAutoSelected by remember { mutableStateOf(false) }
    var shortcutsDialogApp by remember { mutableStateOf<AppModel?>(null) }
    var appShortcuts by remember { mutableStateOf<List<AppShortcut>>(emptyList()) }
    var shortcutsLoading by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val supportsShortcuts = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1

    val shouldShowIcons = if (settings.showAppIcons) {
        if (isLandscape) settings.showIconsInLandscape else settings.showIconsInPortrait
    } else { false }

    val itemSpacing = when (if (isLandscape) settings.landscapeItemSpacing else settings.itemSpacing) {
        0 -> 0.dp; 1 -> 4.dp; 2 -> 8.dp; 3 -> 16.dp; else -> 4.dp
    }

    val searchResultsFontSize = when {
        isLandscape -> settings.landscapeSearchResultsFontSize
        settings.searchResultsUseHomeFont -> settings.textSizeScale
        else -> settings.searchResultsFontSize
    }

    val fontWeight = when (settings.fontWeight) {
        0 -> FontWeight.Thin; 1 -> FontWeight.Light; 2 -> FontWeight.Normal
        3 -> FontWeight.Medium; 4 -> FontWeight.Bold; 5 -> FontWeight.Black
        else -> FontWeight.Normal
    }
    val customFontsEnabled = !settings.useSystemFont
    val appDrawerFontFamily = remember(customFontsEnabled, settings.appDrawerLabelFontPath, settings.customFontPath) {
        if (!customFontsEnabled) {
            null
        } else {
            loadFontFamily(settings.appDrawerLabelFontPath) ?: loadFontFamily(settings.customFontPath)
        }
    }

    var selectedApp by remember { mutableStateOf<AppModel?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showTagsEditor by remember { mutableStateOf(false) }
    var showFolderPickerForApp by remember { mutableStateOf<AppModel?>(null) }
    var pendingAddToHomeApp by remember { mutableStateOf<AppModel?>(null) }
    var showShortcutDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    // On pause (e.g. screen lock), explicitly dismiss the keyboard so IME insets are
    // zeroed out before the activity pauses. Without this, stale insets persist on
    // resume and imePadding() reserves blank space where the keyboard was.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Clear search when returning to this screen
    LaunchedEffect(Unit) {
        viewModel.searchApps("")
    }

    val handleAppClick: (AppModel) -> Unit = { app ->
        Log.d("AppDrawer", "handleAppClick called! selectionMode=$selectionMode")
        if (selectionMode) {
            Log.d("AppDrawer", "Calling onAppClick for SELECTION")
            onAppClick(app)
        } else {
            Log.d("AppDrawer", "Calling onAppClick to OPEN app")
            viewModel.searchApps("")
            focusManager.clearFocus()
            keyboardController?.hide()

            if (settings.returnToHomeAfterApp) {
                onSwipeDown()
            }

            onAppClick(app)
        }
    }

    val requestAddToHome: (AppModel) -> Unit = { app ->
        if (viewModel.availableHomeOrientations().size > 1) {
            pendingAddToHomeApp = app
        } else {
            viewModel.addAppToHomeScreen(app)
        }
    }

    pendingAddToHomeApp?.let { app ->
        AlertDialog(
            onDismissRequest = { pendingAddToHomeApp = null },
            title = { Text("Add to Home Screen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose where to place ${app.appLabel}.")
                    Text(
                        "Portrait and landscape homes are separate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        viewModel.addAppToHomeScreen(app, HomeOrientation.PORTRAIT)
                        pendingAddToHomeApp = null
                    }) { Text("Add to Portrait") }
                    TextButton(onClick = {
                        viewModel.addAppToHomeScreen(app, HomeOrientation.LANDSCAPE)
                        pendingAddToHomeApp = null
                    }) { Text("Add to Landscape") }
                    TextButton(onClick = { pendingAddToHomeApp = null }) { Text("Cancel") }
                }
            },
            dismissButton = {}
        )
    }

    LaunchedEffect(Unit) { viewModel.loadApps() }

    val effectiveAutoShowKeyboard = if (isLandscape) settings.landscapeAutoShowKeyboard else settings.autoShowKeyboard
    LaunchedEffect(effectiveAutoShowKeyboard, focusRequester, searchQuery) {
        if (effectiveAutoShowKeyboard && searchQuery.isEmpty()) {
            yield()
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {
                // Focus requester might not be attached yet
            }
        }
    }

    val scrollState = rememberLazyListState()

    val isBottomSearch = settings.searchBarPlacement == Constants.SearchBarPlacement.BOTTOM
    val invertSearchResults = settings.invertSearchResultsOrder
    val reverseAppList = settings.reverseAppListDirection
    val avoidCutout = settings.avoidCameraBottomSearch
    val showScrollbar = if (isLandscape) settings.landscapeShowScrollbar else settings.showScrollbar
    val scrollbarOnLeft = if (isLandscape) settings.landscapeScrollbarOnLeft else settings.scrollbarOnLeft
    val isRightAligned = (if (isLandscape) settings.landscapeAppDrawerAlignment else settings.appDrawerAlignment) == Constants.AppDrawerAlignment.RIGHT

    val appsToShow = if (searchQuery.isEmpty()) uiState.apps else uiState.filteredApps

    val showLabelsInList = if (settings.showAppNamesInSearchAfter > 0) {
        searchQuery.length >= settings.showAppNamesInSearchAfter
    } else {
        settings.showAppNames
    }

    val belowNameThreshold = settings.showAppNamesInSearchAfter > 0 &&
        searchQuery.length < settings.showAppNamesInSearchAfter

    LaunchedEffect(searchQuery) {
        hasAutoSelected = false
    }

    val effectiveAutoOpenFilteredApp = if (isLandscape) settings.landscapeAutoOpenFilteredApp else settings.autoOpenFilteredApp
    LaunchedEffect(appsToShow, effectiveAutoOpenFilteredApp, searchQuery) {
        if (
            searchQuery.isNotEmpty() &&
            appsToShow.size == 1 &&
            effectiveAutoOpenFilteredApp &&
            !hasAutoSelected
        ) {
            handleAppClick(appsToShow[0])
        }
    }

    LaunchedEffect(appsToShow, settings.searchSortOrder) {
        if (settings.searchSortOrder == Constants.SortOrder.RECENT_FIRST) {
            delay(150)
            scrollState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(searchQuery, scrollState) {
        if (searchQuery.isEmpty() && (scrollState.firstVisibleItemIndex != 0 || scrollState.firstVisibleItemScrollOffset != 0)) {
            scrollState.scrollToItem(0)
        }
    }

    // Keyboard and scroll interaction logic
    LaunchedEffect(scrollState, keyboardController, focusManager, focusRequester, isSearchFocused, isBottomSearch) {
        var previousIndex = scrollState.firstVisibleItemIndex
        var previousOffset = scrollState.firstVisibleItemScrollOffset

        snapshotFlow {
            Triple(
                scrollState.firstVisibleItemIndex,
                scrollState.firstVisibleItemScrollOffset,
                scrollState.isScrollInProgress
            )
        }.distinctUntilChanged().collect { (currentIndex, currentOffset, isScrolling) ->
            if (isScrolling) {
                val actualScrollHappened = currentIndex != previousIndex || currentOffset != previousOffset
                if (actualScrollHappened) {
                    val verticalScrollDelta: Int = if (currentIndex > previousIndex) 1
                    else if (currentIndex < previousIndex) -1
                    else currentOffset - previousOffset

                    if (isBottomSearch) {
                        // Bottom search: scrolling away from search bar (up, negative delta) hides keyboard
                        if (verticalScrollDelta < 0) {
                            if (isSearchFocused) { focusManager.clearFocus() }
                            keyboardController?.hide()
                        } else {
                            if (currentIndex == 0 && currentOffset == 0) {
                                if (!isSearchFocused) { focusRequester.requestFocus() }
                            }
                        }
                    } else {
                        // Top search: scrolling away from search bar (down, positive delta) hides keyboard
                        if (verticalScrollDelta > 0) {
                            if (isSearchFocused) { focusManager.clearFocus() }
                            keyboardController?.hide()
                        } else {
                            if (currentIndex == 0 && currentOffset == 0) {
                                if (!isSearchFocused) { focusRequester.requestFocus() }
                            }
                        }
                    }
                }
            }
            previousIndex = currentIndex
            previousOffset = currentOffset
        }
    }

    // Bottom search naturally reverses (A's nearest the search bar).
    // reverseAppList flips that default in both placements via XOR:
    //   bottom=true,  reverseAppList=false → true  (A's at bottom — default)
    //   bottom=true,  reverseAppList=true  → false (A's at top    — toggled)
    //   bottom=false, reverseAppList=false → false (A's at top    — default)
    //   bottom=false, reverseAppList=true  → true  (A's at bottom — toggled)
    // During bottom+inverted search, reverseLayout anchors item 0 (best match) at the
    // bottom near the search bar — no imperative scrolling needed.
    val shouldReverseLayout = if (searchQuery.isEmpty()) {
        isBottomSearch != reverseAppList
    } else {
        isBottomSearch && invertSearchResults
    }

    // When bottom+inverted search is active, reverseLayout handles the visual flip, so
    // we keep the list in its natural order (best match at index 0 = bottom of the view).
    // For top search with invertSearchResults, we still reverse the content manually.
    val displayList = remember(appsToShow, invertSearchResults, searchQuery, isBottomSearch) {
        if (invertSearchResults && searchQuery.isNotEmpty() && !isBottomSearch) appsToShow.reversed() else appsToShow
    }

    // With reverseLayout=true, item 0 is always the bottom anchor. Re-snap after every
    // displayList change (keyed here, not on searchQuery, so it fires after the ViewModel
    // has emitted the updated filtered results). yield() defers past the layout pass so
    // scrollToItem(0) runs after LazyColumn has settled its new item positions.
    LaunchedEffect(displayList) {
        if (isBottomSearch && invertSearchResults && searchQuery.isNotEmpty() && displayList.isNotEmpty()) {
            yield()
            scrollState.scrollToItem(0)
        }
    }

    val privateSpaceState by viewModel.privateSpaceState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .detectSwipeGestures(
                sensitivity = settings.gestureSensitivity,
                onSwipeDown = {
                    onSwipeDown()
                },
                onSwipeUp = {
                    if (scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0) {
                        onSwipeDown()
                    }
                }
            )
            .statusBarsPadding()
            .imePadding()
    ) {
        if (selectionMode) {
            TopAppBar(
                title = { Text(selectionTitle) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppDrawerSearch(
                        searchQuery = searchQuery,
                        onSearchChanged = { query -> viewModel.searchApps(query) },
                        modifier = Modifier.focusRequester(focusRequester).fillMaxWidth(),
                        onEnterPressed = {
                            val appsToOpen = if (searchQuery.isEmpty()) uiState.apps else uiState.filteredApps
                            if (appsToOpen.isNotEmpty()) handleAppClick(appsToOpen[0])
                        },
                        onFocusStateChanged = { focused -> isSearchFocused = focused }
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                )
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when {
                        uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                        uiState.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Error: ${uiState.error}") }
                        uiState.apps.isEmpty() && searchQuery.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No apps found") }
                        uiState.filteredApps.isEmpty() && searchQuery.isNotEmpty() -> {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("No apps found matching \"$searchQuery\"", color = MaterialTheme.colorScheme.onBackground)
                                    if (settings.showWebSearchOption) {
                                        Button(
                                            onClick = {
                                                if (searchQuery.startsWith("!")) {
                                                    context.openSearch(Constants.URL_DUCK_SEARCH + searchQuery.substring(1).replace(" ", "%20"))
                                                } else {
                                                    context.openSearch(searchQuery.trim())
                                                }
                                            },
                                            modifier = Modifier.padding(top = 16.dp)
                                        ) {
                                            Text("Search Web")
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            if (settings.landscapeAppDrawerStyle == Constants.LandscapeDrawerStyle.PAGED_GRID) {
                                val customTextColor = if (settings.useCustomTextColor && settings.textColor != 0) {
                                    Color(settings.textColor)
                                } else {
                                    null
                                }
                                LandscapePagedGrid(
                                    displayList = displayList,
                                    itemSpacing = itemSpacing,
                                    textSizeScale = settings.textSizeScale,
                                    shouldShowIcons = shouldShowIcons,
                                    showLabelsInList = showLabelsInList,
                                    iconCornerRadius = settings.iconCornerRadius.dp,
                                    fontFamily = appDrawerFontFamily,
                                    fontWeight = fontWeight,
                                    textColor = customTextColor,
                                    isRightAligned = isRightAligned,
                                    showShortcutIcon = settings.showShortcutIcon,
                                    iconPackName = settings.selectedIconPack,
                                    onAppClick = { app ->
                                        if (selectionMode || settings.appDrawerTapToOpen) handleAppClick(app)
                                    },
                                    onAppLongClick = { app ->
                                        if (settings.appDrawerLongPressEnabled && !selectionMode) {
                                            selectedApp = app
                                            showContextMenu = true
                                        }
                                    },
                                    isAppInPrivateSpace = { app -> viewModel.isPrivateSpaceSupported && viewModel.isAppInPrivateSpace(app) },
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    val scrollbarPadding = if (showScrollbar && displayList.isNotEmpty()) {
                                        if (scrollbarOnLeft) Modifier.padding(start = 12.dp)
                                        else Modifier.padding(end = 12.dp)
                                    } else Modifier
                                    LazyColumn(
                                        state = scrollState,
                                        modifier = Modifier.fillMaxSize().then(scrollbarPadding),
                                        verticalArrangement = Arrangement.spacedBy(itemSpacing)
                                    ) {
                                        items(
                                            items = displayList,
                                            key = { app -> app.getKey() }
                                        ) { app ->
                                            val customTextColor = if (settings.useCustomTextColor && settings.textColor != 0) {
                                                Color(settings.textColor)
                                            } else {
                                                null
                                            }
                                            AppListItem(
                                                appLabel = app.appLabel,
                                                appIcon = rememberAppIcon(app, settings.selectedIconPack, shouldShowIcons),
                                                showIcon = shouldShowIcons,
                                                showLabel = showLabelsInList,
                                                iconCornerRadius = settings.iconCornerRadius.dp,
                                                fontScale = searchResultsFontSize,
                                                fontFamily = appDrawerFontFamily,
                                                fontWeight = fontWeight,
                                                textColor = customTextColor,
                                                isRightAligned = isRightAligned,
                                                onClick = {
                                                    if (selectionMode || settings.appDrawerTapToOpen) handleAppClick(app)
                                                },
                                                onLongClick = {
                                                    if (settings.appDrawerLongPressEnabled && !selectionMode) {
                                                        selectedApp = app
                                                        showContextMenu = true
                                                    }
                                                },
                                                labelPrefix = if (app.isSystemShortcut && settings.showShortcutIcon) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.Language,
                                                            contentDescription = null,
                                                            tint = customTextColor ?: MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                } else null,
                                                trailing = if (viewModel.isPrivateSpaceSupported && viewModel.isAppInPrivateSpace(app)) {
                                                    { PrivateSpaceIndicator(true) }
                                                } else null
                                            )
                                        }
                                    }
                                    if (showScrollbar && displayList.isNotEmpty()) {
                                        ScrollbarIndicator(
                                            listState = scrollState,
                                            totalItems = displayList.size,
                                            reverseLayout = false,
                                            alignToStart = scrollbarOnLeft,
                                            modifier = Modifier.align(
                                                if (scrollbarOnLeft) Alignment.TopStart else Alignment.TopEnd
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
        if (!isBottomSearch) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppDrawerSearch(
                    searchQuery = searchQuery,
                    onSearchChanged = { query -> viewModel.searchApps(query) },
                    modifier = Modifier.focusRequester(focusRequester).weight(1f),
                    onEnterPressed = {
                        val appsToOpen = if (searchQuery.isEmpty()) uiState.apps else uiState.filteredApps
                        if (appsToOpen.isNotEmpty()) handleAppClick(appsToOpen[0])
                    },
                    onFocusStateChanged = { focused -> isSearchFocused = focused }
                )
                if (viewModel.isPrivateSpaceSupported &&
                    privateSpaceState != MainViewModel.PrivateSpaceState.NotSetUp) {
                    Spacer(modifier = Modifier.width(8.dp))
                    PrivateSpaceToggle(viewModel)
                }
            }
        }

        LaunchedEffect(settings.renamedApps) {
            Log.d("AppRename", "Renamed apps: ${settings.renamedApps}")
        }

        var containerHeightPx by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current
        Box(modifier = Modifier
            .weight(1f)
            .onSizeChanged { containerHeightPx = it.height }
        ) {
            // When avoiding the camera cutout, reduce the ceiling so the list can't
            // grow tall enough to put items behind the punch-hole camera.
            // 48.dp covers virtually all modern phone cutouts.
            val maxListHeight = with(density) {
                val fullHeight = containerHeightPx.toDp()
                if (isBottomSearch && avoidCutout) (fullHeight - 48.dp).coerceAtLeast(0.dp) else fullHeight
            }
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                uiState.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Error: ${uiState.error}") }
                uiState.apps.isEmpty() && searchQuery.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No apps found") }
                uiState.filteredApps.isEmpty() && searchQuery.isNotEmpty() -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No apps found matching \"$searchQuery\"", color = MaterialTheme.colorScheme.onBackground)
                            if (settings.showWebSearchOption) {
                                Button(
                                    onClick = {
                                        if (searchQuery.startsWith("!")) {
                                            context.openSearch(Constants.URL_DUCK_SEARCH + searchQuery.substring(1).replace(" ", "%20"))
                                        } else {
                                            context.openSearch(searchQuery.trim())
                                        }
                                    },
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text("Search Web")
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Outer box always fills the full available space so that Modifier.align()
                    // on the inner box has a proper, fully-sized Box scope to work against.
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = (if (isBottomSearch) {
                                // heightIn on the *box* (not the LazyColumn) ensures fillMaxSize
                                // on the LazyColumn expands the box to exactly the capped height,
                                // so align(BottomStart) pins the list just above the search bar.
                                // When avoidCutout is on, the cap is 48 dp short of the full
                                // height, which leaves a transparent gap at the TOP of the screen
                                // (near the punch-hole camera) — never at the bottom.
                                Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .heightIn(max = maxListHeight)
                            } else {
                                Modifier.fillMaxSize()
                            }).alpha(if (belowNameThreshold) 0f else 1f)
                        ) {
                        // Reserve space on the scrollbar side so content doesn't sit
                        // flush against the scrollbar thumb / touch target.
                        // Only apply when there are apps to show — no apps means no scrollbar.
                        val scrollbarPadding = if (showScrollbar && displayList.isNotEmpty()) {
                            if (scrollbarOnLeft) Modifier.padding(start = 12.dp)
                            else Modifier.padding(end = 12.dp)
                        } else Modifier

                        LazyColumn(
                            state = scrollState,
                            reverseLayout = shouldReverseLayout,
                            // Bottom search: fillMaxWidth only — the LazyColumn sizes to its
                            // content height (short results stay compact near the search bar)
                            // while still growing to fill the Box's heightIn cap for long lists.
                            // fillMaxSize would force all results to the top of a tall container.
                            modifier = (if (isBottomSearch) Modifier.fillMaxWidth() else Modifier.fillMaxSize())
                                .then(scrollbarPadding),
                            verticalArrangement = Arrangement.spacedBy(itemSpacing)
                        ) {
                            items(
                                items = displayList,
                                key = { app -> app.getKey() }
                            ) { app ->
                                val customTextColor = if (settings.useCustomTextColor && settings.textColor != 0) {
                                    Color(settings.textColor)
                                } else {
                                    null
                                }

                                AppListItem(
                                    appLabel = app.appLabel,
                                    appIcon = rememberAppIcon(app, settings.selectedIconPack, shouldShowIcons),
                                    showIcon = shouldShowIcons,
                                    showLabel = showLabelsInList,
                                    iconCornerRadius = settings.iconCornerRadius.dp,
                                    fontScale = searchResultsFontSize,
                                    fontFamily = appDrawerFontFamily,
                                    fontWeight = fontWeight,
                                    textColor = customTextColor,
                                    isRightAligned = isRightAligned,
                                    onClick = {
                                        if (selectionMode || settings.appDrawerTapToOpen) {
                                            handleAppClick(app)
                                        }
                                    },
                                    onLongClick = {
                                        if (settings.appDrawerLongPressEnabled && !selectionMode) {
                                            selectedApp = app
                                            showContextMenu = true
                                        }
                                    },
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = null,
                                        fadeOutSpec = null,
                                        placementSpec = AnimationConfig.listItemAnimationSpec
                                    ),
                                    labelPrefix = if (app.isSystemShortcut && settings.showShortcutIcon) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Language,
                                                contentDescription = null,
                                                tint = customTextColor ?: MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    } else null,
                                    trailing = if (viewModel.isPrivateSpaceSupported && viewModel.isAppInPrivateSpace(app)) {
                                        { PrivateSpaceIndicator(true) }
                                    } else null
                                )
                            }
                        }

                        if (showScrollbar && displayList.size > 0 && !belowNameThreshold) {
                            ScrollbarIndicator(
                                listState = scrollState,
                                totalItems = displayList.size,
                                reverseLayout = shouldReverseLayout,
                                alignToStart = scrollbarOnLeft,
                                modifier = Modifier.align(
                                    if (scrollbarOnLeft) Alignment.TopStart else Alignment.TopEnd
                                )
                            )
                        }
                        } // inner Box

                        // Transparent overlay while below name-reveal threshold.
                        // Intercepts taps so invisible list items can't be accidentally triggered.
                        // Reserved for future content (hints, animations, etc.).
                        if (belowNameThreshold) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures { }
                                    }
                            )
                        }
                    } // outer Box
                }
            }
        }

        if (isBottomSearch) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppDrawerSearch(
                    searchQuery = searchQuery,
                    onSearchChanged = { query -> viewModel.searchApps(query) },
                    modifier = Modifier.focusRequester(focusRequester).weight(1f),
                    onEnterPressed = {
                        val appsToOpen = if (searchQuery.isEmpty()) uiState.apps else uiState.filteredApps
                        if (appsToOpen.isNotEmpty()) handleAppClick(appsToOpen[0])
                    },
                    onFocusStateChanged = { focused -> isSearchFocused = focused }
                )
                if (viewModel.isPrivateSpaceSupported &&
                    privateSpaceState != MainViewModel.PrivateSpaceState.NotSetUp) {
                    Spacer(modifier = Modifier.width(8.dp))
                    PrivateSpaceToggle(viewModel)
                }
            }
        }
        } // end else (portrait)
    }

    if (showContextMenu && selectedApp != null) {
        val app = selectedApp ?: return
        val isSystemShortcut = app.isSystemShortcut
        val canUninstall = !isSystemShortcut && !context.isSystemApp(app.appPackage)
        val hiddenApps by viewModel.hiddenApps.collectAsState()
        val isHidden = hiddenApps.any { it.getKey() == app.getKey() }

        var renameDialogVisible by remember { mutableStateOf(false) }
        var newAppName by remember { mutableStateOf(app.appLabel) }
        val contextMenuListState = rememberLazyListState()

        val dismissMenu = { showContextMenu = false; selectedApp = null; showShortcutDialog = false }
        val privateSpaceUnlocked =
            viewModel.isPrivateSpaceSupported &&
                viewModel.privateSpaceState.collectAsState().value == MainViewModel.PrivateSpaceState.Unlocked
        val menuActions = buildList {
            if (isSystemShortcut) {
                add(AppDrawerMenuAction("Open", Icons.Default.AdsClick) {
                    handleAppClick(app)
                    dismissMenu()
                })
                add(AppDrawerMenuAction("Rename", Icons.Default.DriveFileRenameOutline) {
                    renameDialogVisible = true
                })
                add(AppDrawerMenuAction("Tags...", Icons.Default.Search) {
                    showTagsEditor = true
                })
                add(AppDrawerMenuAction("Add to Home Screen", Icons.Default.Add) {
                    requestAddToHome(app)
                    dismissMenu()
                })
                add(AppDrawerMenuAction("App Info", Icons.Default.Info) {
                    openAppInfo(context, app)
                    dismissMenu()
                })
                if (homeFolders.isNotEmpty()) {
                    add(AppDrawerMenuAction("Add to Folder...", Icons.Default.Folder) {
                        showFolderPickerForApp = app
                        dismissMenu()
                    })
                }
                add(AppDrawerMenuAction("Delete", Icons.Default.Delete) {
                    viewModel.deleteSystemShortcut(app)
                    dismissMenu()
                })
            } else {
                add(AppDrawerMenuAction("Open", Icons.Default.AdsClick) {
                    handleAppClick(app)
                    dismissMenu()
                })
                add(AppDrawerMenuAction(if (isHidden) "Unhide" else "Hide", Icons.Default.Settings) {
                    viewModel.toggleAppHidden(app)
                    dismissMenu()
                })
                add(AppDrawerMenuAction("Rename", Icons.Default.DriveFileRenameOutline) {
                    renameDialogVisible = true
                })
                add(AppDrawerMenuAction("Tags...", Icons.Default.Search) {
                    showTagsEditor = true
                })
                add(AppDrawerMenuAction("App Info", Icons.Default.Info) {
                    openAppInfo(context, app)
                    dismissMenu()
                })
                if (canUninstall) {
                    add(AppDrawerMenuAction("Uninstall", Icons.Default.DeleteOutline) {
                        context.uninstall(app.appPackage)
                        dismissMenu()
                    })
                }
                add(AppDrawerMenuAction("Add to Home Screen", Icons.Default.Add) {
                    requestAddToHome(app)
                    dismissMenu()
                })
                if (homeFolders.isNotEmpty()) {
                    add(AppDrawerMenuAction("Add to Folder...", Icons.Default.Folder) {
                        showFolderPickerForApp = app
                        dismissMenu()
                    })
                }
                add(AppDrawerMenuAction("Assign Shortcut", Icons.Default.Keyboard) {
                    showShortcutDialog = true
                })
                if (supportsShortcuts && !selectionMode) {
                    add(AppDrawerMenuAction("Shortcuts", Icons.Default.SubdirectoryArrowRight) {
                        shortcutsDialogApp = app
                        shortcutsLoading = true
                        appShortcuts = emptyList()
                        viewModel.getAppShortcuts(app) { shortcuts ->
                            if (shortcutsDialogApp?.getKey() == app.getKey()) {
                                appShortcuts = shortcuts
                                shortcutsLoading = false
                            }
                        }
                        dismissMenu()
                    })
                }
                if (privateSpaceUnlocked) {
                    val isInPrivateSpace = viewModel.isAppInPrivateSpace(app)
                    add(
                        AppDrawerMenuAction(
                            if (isInPrivateSpace) "Remove from Private Space" else "Add to Private Space",
                            Icons.Default.Lock
                        ) {
                            viewModel.toggleAppInPrivateSpace(app)
                            dismissMenu()
                        }
                    )
                }
            }
        }

        if (showShortcutDialog) {
            val existingShortcut = viewModel.getShortcutForApp(app.getKey())
            val existingLabel = existingShortcut?.let {
                val modName = if (it.modifier == KeyModifier.CTRL) "Ctrl" else "Alt"
                "$modName+${android.view.KeyEvent.keyCodeToString(it.keyCode).removePrefix("KEYCODE_").replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() }}"
            }
            KeyboardShortcutDialog(
                appName = app.appLabel,
                currentShortcutLabel = existingLabel,
                onAssign = { modifier, keyCode ->
                    viewModel.setShortcutForApp(app, modifier, keyCode)
                },
                onClear = { viewModel.clearShortcutForApp(app) },
                onDismiss = { showShortcutDialog = false },
            )
        } else if (showTagsEditor) {
            AppTagsEditorDialog(
                title = "Tags for ${app.appLabel}",
                initialTags = viewModel.getTagsForApp(app),
                onSave = { tags -> viewModel.saveTagsForApp(app, tags) },
                onBack = { showTagsEditor = false },
                onDone = {
                    showTagsEditor = false
                    dismissMenu()
                }
            )
        } else AnimatedContextMenuDialog(
            visible = true,
            onDismissRequest = dismissMenu,
            animationsEnabled = settings.contextMenuAnimationsEnabled,
            title = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(app.appLabel, modifier = Modifier.align(Alignment.CenterStart))
                    IconButton(
                        onClick = { onNavigateToSettings(); dismissMenu() },
                        modifier = Modifier.align(Alignment.TopEnd).size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(dismissMenu) { Text("Close") } },
        ) {
            ScrollableAppDrawerContextMenu(
                actions = menuActions,
                listState = contextMenuListState,
                scrollbarOnLeft = settings.scrollbarOnLeft,
                animationsEnabled = settings.contextMenuAnimationsEnabled,
            )
        }

        if (renameDialogVisible) {
            AlertDialog(
                onDismissRequest = { renameDialogVisible = false },
                title = { Text("Rename ${app.appLabel}") },
                text = {
                    TextField(
                        value = newAppName,
                        onValueChange = { newAppName = it },
                        label = { Text("New name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.renameApp(app, newAppName)
                        renameDialogVisible = false
                        dismissMenu()
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { renameDialogVisible = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

    }

    if (showFolderPickerForApp != null) {
        val app = showFolderPickerForApp ?: return
        AlertDialog(
            onDismissRequest = { showFolderPickerForApp = null },
            title = { Text("Add to Folder") },
            text = {
                androidx.compose.foundation.lazy.LazyColumn {
                    items(homeFolders, key = { it.id }) { folder ->
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(folder.title) },
                            supportingContent = { Text("${folder.apps.size} apps") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addAppToFolder(folder.id, app)
                                    showFolderPickerForApp = null
                                }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderPickerForApp = null }) { Text("Cancel") }
            }
        )
    }

    if (supportsShortcuts && shortcutsDialogApp != null) {
        val app = shortcutsDialogApp ?: return
        AlertDialog(
            onDismissRequest = { shortcutsDialogApp = null },
            title = { Text("${app.appLabel} Shortcuts") },
            text = {
                Column {
                    when {
                        shortcutsLoading -> {
                            Text("Loading shortcuts...")
                        }
                        appShortcuts.isEmpty() -> {
                            Text("No shortcuts available")
                        }
                        else -> {
                            appShortcuts.forEach { shortcut ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.launchShortcut(app, shortcut.id)
                                            shortcutsDialogApp = null
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (shortcut.icon != null) {
                                        Image(
                                            bitmap = shortcut.icon,
                                            contentDescription = shortcut.label,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .padding(end = 0.dp),
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Text(
                                        text = shortcut.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.pinShortcutToLauncher(app, shortcut.id)
                                            shortcutsDialogApp = null
                                        }
                                    ) {
                                        Icon(Icons.Default.Add, "Add")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { shortcutsDialogApp = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ScrollableAppDrawerContextMenu(
    actions: List<AppDrawerMenuAction>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    scrollbarOnLeft: Boolean,
    animationsEnabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
    ) {
        val showScrollbar = actions.size > 6
        val scrollbarPadding = if (showScrollbar) {
            if (scrollbarOnLeft) Modifier.padding(start = 12.dp) else Modifier.padding(end = 12.dp)
        } else {
            Modifier
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .then(scrollbarPadding),
        ) {
            items(actions, key = { it.text }) { action ->
                ContextMenuItemRow(
                    text = action.text,
                    icon = action.icon,
                    animationsEnabled = animationsEnabled,
                    onClick = action.onClick,
                )
            }
        }
        if (showScrollbar) {
            ScrollbarIndicator(
                listState = listState,
                totalItems = actions.size,
                alignToStart = scrollbarOnLeft,
                modifier = Modifier.align(if (scrollbarOnLeft) Alignment.TopStart else Alignment.TopEnd),
            )
        }
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LandscapePagedGrid(
    displayList: List<AppModel>,
    itemSpacing: Dp,
    textSizeScale: Float,
    shouldShowIcons: Boolean,
    showLabelsInList: Boolean,
    iconCornerRadius: Dp,
    fontFamily: FontFamily?,
    fontWeight: FontWeight,
    textColor: Color?,
    isRightAligned: Boolean,
    showShortcutIcon: Boolean,
    iconPackName: String,
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel) -> Unit,
    isAppInPrivateSpace: (AppModel) -> Boolean,
) {
    val dotRowHeight = 14.dp
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableHeight = maxHeight - dotRowHeight
        val rawItemHeight = ((14f * textSizeScale * 1.4f) + 24f).dp + itemSpacing
        val itemHeight = maxOf(28.dp, rawItemHeight)
        val itemsPerPage = maxOf(2, (floor((availableHeight.value / itemHeight.value).toDouble()) * 2).toInt())
        val pages = displayList.chunked(itemsPerPage)
        val pagerState = rememberPagerState(pageCount = { pages.size })

        LaunchedEffect(pages.size) {
            if (pages.isNotEmpty() && pagerState.currentPage >= pages.size) {
                pagerState.scrollToPage(0)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(itemSpacing),
                ) {
                    gridItems(
                        items = pages.getOrElse(pageIndex) { emptyList() },
                        key = { app -> app.getKey() }
                    ) { app ->
                        AppListItem(
                            appLabel = app.appLabel,
                            appIcon = rememberAppIcon(app, iconPackName, shouldShowIcons),
                            showIcon = shouldShowIcons,
                            showLabel = showLabelsInList,
                            iconCornerRadius = iconCornerRadius,
                            fontScale = textSizeScale,
                            fontFamily = fontFamily,
                            fontWeight = fontWeight,
                            textColor = textColor,
                            isRightAligned = isRightAligned,
                            onClick = { onAppClick(app) },
                            onLongClick = { onAppLongClick(app) },
                            labelPrefix = if (app.isSystemShortcut && showShortcutIcon) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = textColor ?: MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            } else null,
                            trailing = if (isAppInPrivateSpace(app)) {
                                { PrivateSpaceIndicator(true) }
                            } else null
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dotRowHeight),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { index ->
                    val isActive = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(if (isActive) 11.dp else 4.dp)
                            .background(
                                color = if (isActive) Color(0xFF777777) else Color(0xFF2A2A2A),
                                shape = if (isActive) RoundedCornerShape(2.dp) else CircleShape
                            )
                    )
                    if (index < pages.size - 1) Spacer(Modifier.width(5.dp))
                }
            }
        }
    }
}

@Composable
fun AppDrawerSearch(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onEnterPressed: () -> Unit = {},
    onFocusStateChanged: (Boolean) -> Unit // Callback to notify parent of focus state
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    TextField(
        value = searchQuery,
        onValueChange = onSearchChanged,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .onFocusChanged { focusState ->
                val focused = focusState.isFocused
                onFocusStateChanged(focused) // Notify parent of focus change
                if (focused) {
                    keyboardController?.show() // Show keyboard when TextField gains focus
                }
                // Keyboard hiding on focus loss is handled by system, IME actions, or explicit calls elsewhere (e.g., scroll logic)
            },
        placeholder = { Text("Search apps...") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            keyboardController?.hide() // Hide keyboard on IME "Search" action
            onEnterPressed()
        }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        )
    )
}
