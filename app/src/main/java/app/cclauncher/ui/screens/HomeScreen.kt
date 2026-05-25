package app.cclauncher.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Configuration
import android.net.Uri
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import app.cclauncher.data.FolderApp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import android.graphics.Rect as AndroidRect
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import app.cclauncher.settings.CornerZoneConfig
import kotlin.math.abs
import kotlinx.coroutines.withTimeout
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import app.cclauncher.MainViewModel
import app.cclauncher.data.Constants
import app.cclauncher.data.Constants.MAX_PAGES
import app.cclauncher.data.HomeItem
import app.cclauncher.data.HomeLayout
import app.cclauncher.helper.expandNotificationDrawer
import app.cclauncher.helper.getScreenDimensions
import app.cclauncher.helper.showToast
import app.cclauncher.helper.withResolvedUser
import app.cclauncher.settings.AppSettings
import app.cclauncher.settings.applyToAllCornerZonesFor
import app.cclauncher.settings.cornerConfigFor
import app.cclauncher.settings.cornerUniversalConfigFor
import app.cclauncher.settings.cornerZoneDangerFadeFor
import app.cclauncher.settings.cornerZonesInFoldersFor
import app.cclauncher.settings.swipeActionFor
import app.cclauncher.settings.swipeFolderIdFor
import app.cclauncher.ui.components.AnimatedContextMenuDialog
import app.cclauncher.ui.components.AppTagsEditorDialog
import app.cclauncher.ui.components.AppSlider
import app.cclauncher.ui.components.ContextMenuItemRow
import app.cclauncher.ui.components.MoveDirection
import app.cclauncher.ui.components.MovePadOverlay
import app.cclauncher.ui.composables.HomeAppItem
import app.cclauncher.ui.composables.WidgetHostViewContainer
import app.cclauncher.ui.composables.WidgetSizeData
import app.cclauncher.ui.dialogs.ResizeDialog
import app.cclauncher.ui.util.detectSwipeGestures
import app.cclauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    appWidgetHost: AppWidgetHost,
    onNavigateToAppDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()
    val homeLayoutState by viewModel.homeLayoutState.collectAsState()
    val homeFolders by remember(homeLayoutState.items) { derivedStateOf { viewModel.getAllFolders() } }
    val settings by settingsViewModel.settingsState.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val activeOrientation by viewModel.activeHomeOrientation.collectAsState()

    LaunchedEffect(configuration.orientation) {
        viewModel.updateActiveHomeOrientation(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    val pagerState = rememberPagerState(
        initialPage = currentPage,
        pageCount = { homeLayoutState.pageCount }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentPage(pagerState.currentPage)
    }

    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage) {
            pagerState.animateScrollToPage(currentPage)
        }
    }

    fun goToNextPage() {
        if (pagerState.currentPage < homeLayoutState.pageCount - 1) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    fun goToPreviousPage() {
        if (pagerState.currentPage > 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        }
    }

    fun handleSwipeAction(action: Int, appLauncher: () -> Unit, folderId: String = "") {
        when (action) {
            Constants.SwipeAction.NOTIFICATIONS -> expandNotificationDrawer(context)
            Constants.SwipeAction.SEARCH -> onNavigateToAppDrawer()
            Constants.SwipeAction.APP -> appLauncher()
            Constants.SwipeAction.NEXT_PAGE -> goToNextPage()
            Constants.SwipeAction.PREVIOUS_PAGE -> goToPreviousPage()
            Constants.SwipeAction.OPEN_FOLDER -> viewModel.openFolderById(folderId)
            Constants.SwipeAction.OPEN_SETTINGS -> onNavigateToSettings()
            Constants.SwipeAction.NULL -> {}
        }
    }

    var showAppContextMenu by remember { mutableStateOf<HomeItem.App?>(null) }
    var showWidgetContextMenu by remember { mutableStateOf<HomeItem.Widget?>(null) }
    var showFolderContextMenu by remember { mutableStateOf<HomeItem.Folder?>(null) }
    var showPlaceholderWidgetActions by remember { mutableStateOf<HomeItem.Widget?>(null) }
    var showPlaceholderWidgetDetails by remember { mutableStateOf<HomeItem.Widget?>(null) }
    var resizeDialogItem by remember { mutableStateOf<HomeItem?>(null) }
    var openFolderId by remember { mutableStateOf<String?>(null) }
    var pendingHomeAppFontItem by remember { mutableStateOf<HomeItem.App?>(null) }
    var pendingFolderTitleFontItem by remember { mutableStateOf<HomeItem.Folder?>(null) }
    var pendingFolderAppFontItem by remember { mutableStateOf<Pair<String, FolderApp>?>(null) }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri == null) {
                pendingHomeAppFontItem = null
                pendingFolderTitleFontItem = null
                pendingFolderAppFontItem = null
                return@rememberLauncherForActivityResult
            }
            coroutineScope.launch {
                val repo = viewModel.settingsRepository
                pendingHomeAppFontItem?.let { appItem ->
                    val newPath = repo.importFontFile(uri, "home_item_font")
                    if (newPath != null) {
                        if (appItem.labelFontPath.isNotBlank()) repo.deleteFontFile(appItem.labelFontPath)
                        viewModel.updateHomeAppLabelFont(appItem, newPath)
                    }
                }
                pendingFolderTitleFontItem?.let { folderItem ->
                    val newPath = repo.importFontFile(uri, "folder_title_font")
                    if (newPath != null) {
                        if (folderItem.titleFontPath.isNotBlank()) repo.deleteFontFile(folderItem.titleFontPath)
                        viewModel.updateFolderTitleFont(folderItem.id, newPath)
                    }
                }
                pendingFolderAppFontItem?.let { (folderId, folderApp) ->
                    val newPath = repo.importFontFile(uri, "folder_item_font")
                    if (newPath != null) {
                        if (folderApp.labelFontPath.isNotBlank()) repo.deleteFontFile(folderApp.labelFontPath)
                        viewModel.updateFolderAppLabelFont(folderId, folderApp, newPath)
                    }
                }
                pendingHomeAppFontItem = null
                pendingFolderTitleFontItem = null
                pendingFolderAppFontItem = null
            }
        }
    )

    val onSwipeUp: () -> Unit = {
        if (openFolderId == null || settings.swipeGesturesInFolders)
            handleSwipeAction(settings.swipeActionFor(activeOrientation, "up"), { viewModel.launchSwipeUpApp() }, settings.swipeFolderIdFor(activeOrientation, "up"))
    }
    val onSwipeDown: () -> Unit = {
        if (openFolderId == null || settings.swipeGesturesInFolders)
            handleSwipeAction(settings.swipeActionFor(activeOrientation, "down"), { viewModel.launchSwipeDownApp() }, settings.swipeFolderIdFor(activeOrientation, "down"))
    }
    val onSwipeLeft: () -> Unit = {
        if (openFolderId == null || settings.swipeGesturesInFolders)
            handleSwipeAction(settings.swipeActionFor(activeOrientation, "left"), { viewModel.launchSwipeLeftApp() }, settings.swipeFolderIdFor(activeOrientation, "left"))
    }
    val onSwipeRight: () -> Unit = {
        if (openFolderId == null || settings.swipeGesturesInFolders)
            handleSwipeAction(settings.swipeActionFor(activeOrientation, "right"), { viewModel.launchSwipeRightApp() }, settings.swipeFolderIdFor(activeOrientation, "right"))
    }

    // Simple movement tracking - no overlay needed
    var widgetBeingMoved by remember { mutableStateOf<HomeItem.Widget?>(null) }
    var appBeingMoved by remember { mutableStateOf<HomeItem.App?>(null) }
    var folderBeingMoved by remember { mutableStateOf<HomeItem.Folder?>(null) }

    fun stepPosition(row: Int, column: Int, direction: MoveDirection): Pair<Int, Int> {
        return when (direction) {
            MoveDirection.Up -> row - 1 to column
            MoveDirection.Down -> row + 1 to column
            MoveDirection.Left -> row to column - 1
            MoveDirection.Right -> row to column + 1
        }
    }

    fun nudgeMovingHomeItem(direction: MoveDirection): Boolean {
        widgetBeingMoved?.let { item ->
            val (newRow, newColumn) = stepPosition(item.row, item.column, direction)
            if (!viewModel.canMoveHomeItem(item, newRow, newColumn)) return false
            viewModel.moveWidget(item, newRow, newColumn)
            widgetBeingMoved = item.copy(row = newRow, column = newColumn)
            return true
        }

        appBeingMoved?.let { item ->
            val (newRow, newColumn) = stepPosition(item.row, item.column, direction)
            if (!viewModel.canMoveHomeItem(item, newRow, newColumn)) return false
            viewModel.moveApp(item, newRow, newColumn)
            appBeingMoved = item.copy(row = newRow, column = newColumn)
            return true
        }

        folderBeingMoved?.let { item ->
            val (newRow, newColumn) = stepPosition(item.row, item.column, direction)
            if (!viewModel.canMoveHomeItem(item, newRow, newColumn)) return false
            viewModel.moveFolder(item, newRow, newColumn)
            folderBeingMoved = item.copy(row = newRow, column = newColumn)
            return true
        }

        return false
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Collect openFolderById events from the ViewModel (used by swipe actions and corner dots)
    LaunchedEffect(viewModel) {
        viewModel.openFolderEvent.collect { folderId ->
            openFolderId = if (openFolderId == folderId) null else folderId
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionUp -> {
                            onSwipeDown()
                            true
                        }
                        Key.DirectionDown -> {
                            onSwipeUp()
                            true
                        }
                        Key.DirectionLeft -> {
                            onSwipeLeft()
                            true
                        }
                        Key.DirectionRight -> {
                            onSwipeRight()
                            true
                        }
                        Key.DirectionCenter, Key.Enter -> {
                            onNavigateToSettings()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = false,
            key = { page -> "page_$page" }
        ) { page ->
            HomeScreenPage(
                homeLayout = homeLayoutState,
                page = page,
                settings = settings,
                appWidgetHost = appWidgetHost,
                widgetBeingMoved = widgetBeingMoved,
                appBeingMoved = appBeingMoved,
                folderBeingMoved = folderBeingMoved,
                onAppClick = { item ->
                    viewModel.launchApp(item.appModel.withResolvedUser(context))
                },
                onAppLongPress = { item -> showAppContextMenu = item },
                onWidgetLongPress = { item -> showWidgetContextMenu = item },
                onPlaceholderWidgetClick = { item -> showPlaceholderWidgetActions = item },
                onFolderClick = { folder -> openFolderId = folder.id },
                onFolderLongPress = { folder -> showFolderContextMenu = folder },
                onEmptyLongPress = { onNavigateToSettings() },
                onDoubleTap = {
                    if (settings.doubleTapToLock) {
                        viewModel.lockScreen()
                    }
                },
                onSwipeUp = onSwipeUp,
                onSwipeDown = onSwipeDown,
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight,
                gestureSensitivity = settings.gestureSensitivity,
                onMoveToPosition = { item, row, col ->
                    when (item) {
                        is HomeItem.Widget -> {
                            viewModel.moveWidget(item, row, col)
                            widgetBeingMoved = null
                        }
                        is HomeItem.App -> {
                            viewModel.moveApp(item, row, col)
                            appBeingMoved = null
                        }
                        is HomeItem.Folder -> {
                            viewModel.moveFolder(item, row, col)
                            folderBeingMoved = null
                        }
                    }
                },
                onCancelMovement = {
                    widgetBeingMoved = null
                    appBeingMoved = null
                    folderBeingMoved = null
                }
            )
        }

        // Page indicators
        if (homeLayoutState.pageCount > 1 && settings.showPageIndicator) {
            PageIndicator(
                pageCount = homeLayoutState.pageCount,
                currentPage = pagerState.currentPage,
                onPageSelected = { page ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }

        if ((widgetBeingMoved != null || appBeingMoved != null || folderBeingMoved != null) && openFolderId == null) {
            MovePadOverlay(
                onMove = ::nudgeMovingHomeItem,
                modifier = Modifier.zIndex(2f),
            )
        }

        // App context menu
        showAppContextMenu?.let { appItem ->
            val currentItem = homeLayoutState.items.find { it.id == appItem.id } as? HomeItem.App
            currentItem?.let {
                HomeAppContextMenu(
                    appItem = it,
                    pageCount = homeLayoutState.pageCount,
                    folders = homeFolders,
                    onDismiss = { showAppContextMenu = null },
                    onRemove = { app ->
                        viewModel.removeAppFromHomeScreen(app)
                        showAppContextMenu = null
                    },
                    onResize = { app ->
                        resizeDialogItem = app
                        showAppContextMenu = null
                    },
                    onMove = { app ->
                        appBeingMoved = app
                        showAppContextMenu = null
                        context.showToast("Tap where you want to move the app")
                    },
                    onMoveToPage = { app, targetPage ->
                        viewModel.moveItemToPage(app, targetPage)
                        showAppContextMenu = null
                    },
                    onRename = { newName ->
                        viewModel.renameApp(it.appModel, newName)
                        showAppContextMenu = null
                    },
                    appTags = viewModel.getTagsForApp(it.appModel),
                    onSaveTags = { tags ->
                        viewModel.saveTagsForApp(it.appModel, tags)
                    },
                    onAddToFolder = { folder ->
                        viewModel.addAppToFolder(folder.id, it.appModel)
                        viewModel.removeAppFromHomeScreen(it)
                        showAppContextMenu = null
                    },
                    onTextSizeChange = { textSize ->
                        viewModel.updateHomeAppTextSize(it, textSize)
                    },
                    onLabelAlignmentChange = { alignment ->
                        viewModel.updateHomeAppLabelAlignment(it, alignment)
                    },
                    showShortcutIconSetting = settings.showShortcutIcon,
                    onIconPlacementChange = { placement ->
                        viewModel.updateHomeAppIconPlacement(it, placement)
                    },
                    onSelectFont = {
                        pendingHomeAppFontItem = it
                        pendingFolderTitleFontItem = null
                        pendingFolderAppFontItem = null
                        fontPickerLauncher.launch("font/*")
                    },
                    onResetFont = {
                        if (it.labelFontPath.isNotBlank()) {
                            viewModel.settingsRepository.deleteFontFile(it.labelFontPath)
                        }
                        viewModel.updateHomeAppLabelFont(it, "")
                    },
                    onNavigateToSettings = onNavigateToSettings,
                    animationsEnabled = settings.contextMenuAnimationsEnabled,
                )
            }
        }

        showWidgetContextMenu?.let { widgetItem ->
            val currentItem = homeLayoutState.items.find { it.id == widgetItem.id } as? HomeItem.Widget
            currentItem?.let {
                WidgetContextMenu(
                    widgetItem = it,
                    pageCount = homeLayoutState.pageCount,
                    onDismiss = { showWidgetContextMenu = null },
                    onRemove = { widget ->
                        viewModel.removeWidget(widget)
                        showWidgetContextMenu = null
                    },
                    onResize = { widget ->
                        resizeDialogItem = widget
                        showWidgetContextMenu = null
                    },
                    onConfigure = { widget ->
                        viewModel.requestWidgetReconfigure(widget)
                        showWidgetContextMenu = null
                    },
                    onCopyDetails = { widget ->
                        copyWidgetDetailsToClipboard(context, widget)
                        context.showToast("Widget details copied")
                        showWidgetContextMenu = null
                    },
                    onMove = { widget ->
                        widgetBeingMoved = widget
                        showWidgetContextMenu = null
                        context.showToast(
                            if (widget.isPlaceholder) "Tap where you want to move the placeholder widget"
                            else "Tap where you want to move the widget",
                            Toast.LENGTH_SHORT
                        )
                    },
                    onMoveToPage = { widget, targetPage ->
                        viewModel.moveItemToPage(widget, targetPage)
                        showWidgetContextMenu = null
                    },
                    onNavigateToSettings = onNavigateToSettings,
                    isBeingMoved = widgetBeingMoved?.id == it.id,
                    onCancelMove = {
                        widgetBeingMoved = null
                        showWidgetContextMenu = null
                    },
                    animationsEnabled = settings.contextMenuAnimationsEnabled,
                )
            }
        }

        showPlaceholderWidgetActions?.let { widgetItem ->
            AlertDialog(
                onDismissRequest = { showPlaceholderWidgetActions = null },
                title = { Text(widgetItem.widgetName.ifBlank { "Widget Placeholder" }) },
                text = {
                    Column {
                        Text(widgetItem.placeholderDisplayText(includeProviderClassName = false))
                        Spacer(modifier = Modifier.height(16.dp))
                        ContextMenuItemRow(
                            text = "See details of widget...",
                            icon = Icons.Default.Info,
                            onClick = {
                                showPlaceholderWidgetDetails = widgetItem
                                showPlaceholderWidgetActions = null
                            }
                        )
                        ContextMenuItemRow(
                            text = "Open widgets",
                            icon = Icons.Default.Search,
                            onClick = {
                                viewModel.startPlaceholderWidgetReplacement(widgetItem)
                                showPlaceholderWidgetActions = null
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPlaceholderWidgetActions = null }) {
                        Text("Close")
                    }
                }
            )
        }

        showPlaceholderWidgetDetails?.let { widgetItem ->
            AlertDialog(
                onDismissRequest = { showPlaceholderWidgetDetails = null },
                title = { Text(widgetItem.widgetName.ifBlank { "Widget Details" }) },
                text = { Text(widgetItem.placeholderDisplayText()) },
                confirmButton = {
                    TextButton(onClick = { showPlaceholderWidgetDetails = null }) {
                        Text("Close")
                    }
                }
            )
        }

        // Folder context menu
        val currentFolderContextMenu = showFolderContextMenu?.let { f ->
            homeLayoutState.items.find { it.id == f.id } as? HomeItem.Folder
        }
        FolderContextMenu(
            folderItem = currentFolderContextMenu,
            pageCount = homeLayoutState.pageCount,
            onDismiss = { showFolderContextMenu = null },
            onRemoveFromHome = { folder ->
                viewModel.setFolderShowOnHome(folder.id, false)
                showFolderContextMenu = null
            },
            onDelete = { folder ->
                viewModel.removeFolder(folder)
                showFolderContextMenu = null
            },
            onResize = { folder ->
                resizeDialogItem = folder
                showFolderContextMenu = null
            },
            onMove = { folder ->
                folderBeingMoved = folder
                showFolderContextMenu = null
                context.showToast("Tap where you want to move the folder")
            },
            onMoveToPage = { folder, targetPage ->
                viewModel.moveItemToPage(folder, targetPage)
                showFolderContextMenu = null
            },
            onRename = { folder, newTitle ->
                viewModel.renameFolder(folder.id, newTitle)
                showFolderContextMenu = null
            },
            onTextSizeChange = { textSize ->
                currentFolderContextMenu?.let { viewModel.updateFolderTitleTextSize(it.id, textSize) }
            },
            onLabelAlignmentChange = { alignment ->
                currentFolderContextMenu?.let { viewModel.updateFolderTitleLabelAlignment(it.id, alignment) }
            },
            showFolderIconSetting = settings.showFolderIcon,
            onIconPlacementChange = { placement ->
                currentFolderContextMenu?.let { viewModel.updateFolderIconPlacement(it.id, placement) }
            },
            onSelectFont = {
                pendingFolderTitleFontItem = currentFolderContextMenu
                pendingHomeAppFontItem = null
                pendingFolderAppFontItem = null
                fontPickerLauncher.launch("font/*")
            },
            onResetFont = {
                currentFolderContextMenu?.let { folder ->
                    if (folder.titleFontPath.isNotBlank()) {
                        viewModel.settingsRepository.deleteFontFile(folder.titleFontPath)
                    }
                    viewModel.updateFolderTitleFont(folder.id, "")
                }
            },
            onNavigateToSettings = onNavigateToSettings,
            animationsEnabled = settings.contextMenuAnimationsEnabled,
        )

        // Resize dialog
        ResizeDialog(
            item = resizeDialogItem,
            currentRows = homeLayoutState.rows,
            currentColumns = homeLayoutState.columns,
            onDismiss = { resizeDialogItem = null },
            onResize = { item, newRowSpan, newColSpan ->
                when (item) {
                    is HomeItem.Widget -> {
                        if (!item.isPlaceholder) {
                            val screenDimensions = getScreenDimensions(context)
                            val cellWidth = screenDimensions.first / homeLayoutState.columns
                            val cellHeight = screenDimensions.second / homeLayoutState.rows
                            val widgetWidthDp = (cellWidth * newColSpan)
                            val widgetHeightDp = (cellHeight * newRowSpan)

                            val options = Bundle().apply {
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widgetWidthDp)
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widgetWidthDp)
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, widgetHeightDp)
                                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, widgetHeightDp)
                            }
                            viewModel.appWidgetManager.updateAppWidgetOptions(item.appWidgetId, options)
                        }
                        viewModel.resizeWidget(item, newRowSpan, newColSpan)
                    }
                    is HomeItem.App -> {
                        viewModel.resizeApp(item, newRowSpan, newColSpan)
                    }
                    is HomeItem.Folder -> {
                        viewModel.resizeFolder(item, newRowSpan, newColSpan)
                    }
                }
            }
        )

        // Folder overlay — shown on top of everything when a folder is open
        openFolderId?.let { fid ->
            val openFolder = homeLayoutState.items.filterIsInstance<HomeItem.Folder>().find { it.id == fid }
            if (openFolder != null) {
                app.cclauncher.ui.composables.FolderOverlay(
                    folder = openFolder,
                    settings = settings,
                    onDismiss = { openFolderId = null },
                    onLaunchApp = { folderApp ->
                        viewModel.launchApp(folderApp.toAppModel())
                        openFolderId = null
                    },
                    onMoveApp = { folderApp, newRow, newCol ->
                        viewModel.moveFolderApp(fid, folderApp, newRow, newCol)
                    },
                    canMoveApp = { folderApp, newRow, newCol ->
                        viewModel.canMoveFolderApp(fid, folderApp, newRow, newCol)
                    },
                    onRemoveApp = { folderApp ->
                        viewModel.removeAppFromFolder(fid, folderApp)
                    },
                    onResizeApp = { folderApp, newRowSpan, newColSpan ->
                        viewModel.resizeFolderApp(fid, folderApp, newRowSpan, newColSpan)
                    },
                    onAppTextSizeChange = { folderApp, textSize ->
                        viewModel.updateFolderAppIndividualTextSize(fid, folderApp, textSize)
                    },
                    onAppLabelAlignmentChange = { folderApp, alignment ->
                        viewModel.updateFolderAppIndividualLabelAlignment(fid, folderApp, alignment)
                    },
                    onAppIconPlacementChange = { folderApp, placement ->
                        viewModel.updateFolderAppIconPlacement(fid, folderApp, placement)
                    },
                    onAppRename = { folderApp, newName ->
                        viewModel.renameApp(folderApp.toAppModel(), newName)
                    },
                    appTagsForApp = { folderApp ->
                        viewModel.getTagsForApp(folderApp.toAppModel())
                    },
                    onSaveAppTags = { folderApp, tags ->
                        viewModel.saveTagsForApp(folderApp.toAppModel(), tags)
                    },
                    onAppSelectFont = { folderApp ->
                        pendingFolderAppFontItem = fid to folderApp
                        pendingHomeAppFontItem = null
                        pendingFolderTitleFontItem = null
                        fontPickerLauncher.launch("font/*")
                    },
                    onAppResetFont = { folderApp ->
                        if (folderApp.labelFontPath.isNotBlank()) {
                            viewModel.settingsRepository.deleteFontFile(folderApp.labelFontPath)
                        }
                        viewModel.updateFolderAppLabelFont(fid, folderApp, "")
                    },
                )
            }
        }

        // Corner shortcut zones — hidden while a folder is open if the setting is off
        if (openFolderId == null || settings.cornerZonesInFoldersFor(activeOrientation)) {
            HomeCornerZones(
                settings = settings,
                activeOrientation = activeOrientation,
                onOpenFolder = { folderId -> openFolderId = if (openFolderId == folderId) null else folderId },
                onAction = { action -> handleSwipeAction(action, {}) },
            )
        }
    }
}

@Composable
private fun HomeScreenPage(
    homeLayout: HomeLayout,
    page: Int,
    settings: AppSettings,
    appWidgetHost: AppWidgetHost,
    widgetBeingMoved: HomeItem.Widget?,
    appBeingMoved: HomeItem.App?,
    folderBeingMoved: HomeItem.Folder?,
    onAppClick: (HomeItem.App) -> Unit,
    onAppLongPress: (HomeItem.App) -> Unit,
    onWidgetLongPress: (HomeItem.Widget) -> Unit,
    onPlaceholderWidgetClick: (HomeItem.Widget) -> Unit,
    onFolderClick: (HomeItem.Folder) -> Unit,
    onFolderLongPress: (HomeItem.Folder) -> Unit,
    onEmptyLongPress: () -> Unit,
    onDoubleTap: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    gestureSensitivity: Float,
    onMoveToPosition: (HomeItem, Int, Int) -> Unit,
    onCancelMovement: () -> Unit
) {
    val pageItems = remember(homeLayout.items, page) {
        homeLayout.itemsForPage(page).filter { item ->
            item !is HomeItem.Folder || item.showOnHome
        }
    }

    val isMoving = widgetBeingMoved != null || appBeingMoved != null || folderBeingMoved != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .detectSwipeGestures(
                sensitivity = gestureSensitivity,
                onSwipeUp = onSwipeUp,
                onSwipeDown = onSwipeDown,
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight
            )
            .pointerInput(widgetBeingMoved, appBeingMoved, folderBeingMoved) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() },
                    onLongPress = { offset ->
                        if (isMoving) {
                            // Widgets in move-mode get their context menu on long-press
                            // instead of a silent cancel (apps/folders cancel via their own handlers)
                            if (widgetBeingMoved != null) {
                                val widget = findWidgetAtPosition(homeLayout, offset, size, page)
                                if (widget != null && widget.id == widgetBeingMoved.id) {
                                    onWidgetLongPress(widget)
                                    return@detectTapGestures
                                }
                            }
                            onCancelMovement()
                            return@detectTapGestures
                        }

                        val widget = findWidgetAtPosition(homeLayout, offset, size, page)
                        if (widget != null) {
                            onWidgetLongPress(widget)
                            return@detectTapGestures
                        }

                        val folder = findFolderAtPosition(homeLayout, offset, size, page)
                        if (folder != null) {
                            onFolderLongPress(folder)
                            return@detectTapGestures
                        }

                        val app = findAppAtPosition(homeLayout, offset, size, page)
                        if (app != null) {
                            onAppLongPress(app)
                            return@detectTapGestures
                        }

                        onEmptyLongPress()
                    },
                    onTap = { offset ->
                        if (isMoving) {
                            val gridPosition = calculateGridPosition(offset, homeLayout, size)
                            if (gridPosition != null) {
                                val item: HomeItem? = widgetBeingMoved ?: appBeingMoved ?: folderBeingMoved
                                item?.let { onMoveToPosition(it, gridPosition.first, gridPosition.second) }
                            }
                        } else {
                            val widget = findWidgetAtPosition(homeLayout, offset, size, page)
                            if (widget != null && widget.isPlaceholder) {
                                onPlaceholderWidgetClick(widget)
                                return@detectTapGestures
                            }
                            val folder = findFolderAtPosition(homeLayout, offset, size, page)
                            if (folder != null) {
                                onFolderClick(folder)
                            }
                        }
                    }
                )
            }
    ) {
        HomeScreenContent(
            homeLayout = homeLayout,
            pageItems = pageItems,
            settings = settings,
            appWidgetHost = appWidgetHost,
            widgetBeingMoved = widgetBeingMoved,
            appBeingMoved = appBeingMoved,
            folderBeingMoved = folderBeingMoved,
            onAppClick = onAppClick,
            onAppLongPress = { app -> if (isMoving) onCancelMovement() else onAppLongPress(app) },
            onWidgetLongPress = onWidgetLongPress,
            onPlaceholderWidgetClick = { widget ->
                if (isMoving) onCancelMovement() else onPlaceholderWidgetClick(widget)
            },
            onFolderClick = onFolderClick,
            onFolderLongPress = { folder -> if (isMoving) onCancelMovement() else onFolderLongPress(folder) },
        )

        if (isMoving && settings.showMoveGridOverlay) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val hPadPx = 16.dp.toPx()
                val vPadPx = 16.dp.toPx()
                val usableW = size.width - hPadPx * 2
                val usableH = size.height - vPadPx * 2
                val cellW = usableW / homeLayout.columns
                val cellH = usableH / homeLayout.rows
                val lineColor = Color.White.copy(alpha = 0.25f)
                val strokePx = 1.dp.toPx()

                // vertical lines
                for (col in 0..homeLayout.columns) {
                    val x = hPadPx + col * cellW
                    drawLine(lineColor, Offset(x, vPadPx), Offset(x, vPadPx + usableH), strokePx)
                }
                // horizontal lines
                for (row in 0..homeLayout.rows) {
                    val y = vPadPx + row * cellH
                    drawLine(lineColor, Offset(hPadPx, y), Offset(hPadPx + usableW, y), strokePx)
                }
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun HomeScreenContent(
    homeLayout: HomeLayout,
    pageItems: List<HomeItem>,
    settings: AppSettings,
    appWidgetHost: AppWidgetHost,
    widgetBeingMoved: HomeItem.Widget?,
    appBeingMoved: HomeItem.App?,
    folderBeingMoved: HomeItem.Folder?,
    onAppClick: (HomeItem.App) -> Unit,
    onAppLongPress: (HomeItem.App) -> Unit,
    onWidgetLongPress: (HomeItem.Widget) -> Unit,
    onPlaceholderWidgetClick: (HomeItem.Widget) -> Unit,
    onFolderClick: (HomeItem.Folder) -> Unit,
    onFolderLongPress: (HomeItem.Folder) -> Unit,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val widgetManager = AppWidgetManager.getInstance(context)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val parentWidthDp = maxWidth
        val parentHeightDp = maxHeight

        val horizontalPadding = 16.dp
        val verticalPadding = 16.dp
        val usableWidth = parentWidthDp - horizontalPadding * 2
        val usableHeight = parentHeightDp - verticalPadding * 2

        val cellWidth = usableWidth / homeLayout.columns
        val cellHeight = usableHeight / homeLayout.rows

        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            val refs = pageItems.associate { it.id to createRef() }

            pageItems.forEach { item ->
                key(item.id) {
                val itemModifier = Modifier.constrainAs(refs.getValue(item.id)) {
                    top.linkTo(parent.top, margin = cellHeight * item.row)
                    start.linkTo(parent.start, margin = cellWidth * item.column)
                    width = androidx.constraintlayout.compose.Dimension.value(cellWidth * item.columnSpan)
                    height = androidx.constraintlayout.compose.Dimension.value(cellHeight * item.rowSpan)
                }

                when (item) {
                    is HomeItem.App -> {
                        val isBeingMoved = appBeingMoved?.id == item.id

                        val appModifier = if (isBeingMoved) {
                            itemModifier
                                .padding(2.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                .alpha(0.7f)
                        } else {
                            itemModifier.padding(2.dp)
                        }

                        HomeAppItem(
                            modifier = appModifier,
                            app = item.appModel,
                            settings = settings,
                            appWidth = cellWidth * item.columnSpan,
                            appHeight = cellHeight * item.rowSpan,
                            onClick = { onAppClick(item) },
                            onLongClick = { onAppLongPress(item) },
                            appTextSize = item.appTextSize,
                            appLabelAlignment = item.appLabelAlignment,
                            shortcutIconPlacement = item.iconPlacement,
                            labelFontPath = item.labelFontPath,
                        )
                    }

                    is HomeItem.Widget -> {
                        val isBeingMoved = widgetBeingMoved?.id == item.id

                        val widgetModifier = if (isBeingMoved) {
                            itemModifier
                                .padding(2.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                .alpha(0.7f)
                        } else {
                            itemModifier.padding(2.dp)
                        }

                        if (item.isPlaceholder) {
                            PlaceholderWidgetCard(
                                modifier = widgetModifier,
                                widgetItem = item,
                                onClick = { onPlaceholderWidgetClick(item) },
                                onLongClick = { onWidgetLongPress(item) }
                            )
                        } else {
                            val providerInfo = remember(item.packageName, item.providerClassName) {
                                widgetManager.installedProviders.find {
                                    it.provider.packageName == item.packageName &&
                                            it.provider.className == item.providerClassName
                                }
                            }

                            if (providerInfo != null) {
                                val sizeData = remember(item.columnSpan, item.rowSpan, cellWidth, cellHeight, density) {
                                    with(density) {
                                        val wDp = cellWidth * item.columnSpan
                                        val hDp = cellHeight * item.rowSpan
                                        WidgetSizeData(
                                            width = wDp.toPx().roundToInt(),
                                            height = hDp.toPx().roundToInt(),
                                            minWidthDp = wDp,
                                            maxWidthDp = wDp,
                                            minHeightDp = hDp,
                                            maxHeightDp = hDp
                                        )
                                    }
                                }

                                WidgetHostViewContainer(
                                    modifier = widgetModifier,
                                    appWidgetId = item.appWidgetId,
                                    providerInfo = providerInfo,
                                    appWidgetHost = appWidgetHost,
                                    widgetSizeData = sizeData,
                                    onLongPress = { onWidgetLongPress(item) }
                                )
                            } else {
                                Box(itemModifier)
                                Log.w("HomeScreen", "Provider not found for widget ID ${item.appWidgetId}")
                            }
                        }
                    }

                    is HomeItem.Folder -> {
                        val isBeingMoved = folderBeingMoved?.id == item.id
                        val folderModifier = if (isBeingMoved) {
                            itemModifier
                                .padding(2.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                .alpha(0.7f)
                        } else {
                            itemModifier.padding(2.dp)
                        }
                        app.cclauncher.ui.composables.HomeFolderItem(
                            folder = item,
                            settings = settings,
                            modifier = folderModifier,
                            onClick = { onFolderClick(item) },
                            onLongClick = { onFolderLongPress(item) },
                        )
                    }
                }
                } // end key(item.id)
            }
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { page ->
            val isSelected = page == currentPage
            Box(
                modifier = Modifier
                    .size(if (isSelected) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onPageSelected(page) }
            )
        }
    }
}

@Composable
private fun ScrollableDialogMenu(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .verticalScroll(rememberScrollState()),
        content = content,
    )
}

@Composable
fun WidgetContextMenu(
    widgetItem: HomeItem.Widget?,
    pageCount: Int = 1,
    onDismiss: () -> Unit,
    onRemove: (HomeItem.Widget) -> Unit,
    onResize: (HomeItem.Widget) -> Unit,
    onConfigure: (HomeItem.Widget) -> Unit,
    onCopyDetails: (HomeItem.Widget) -> Unit,
    onMove: (HomeItem.Widget) -> Unit,
    onMoveToPage: (HomeItem.Widget, Int) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    isBeingMoved: Boolean = false,
    onCancelMove: () -> Unit = {},
    animationsEnabled: Boolean = true,
) {
    if (widgetItem == null) return

    val context = LocalContext.current
    val widgetManager = AppWidgetManager.getInstance(context)
    val providerInfo = remember(widgetItem) {
        widgetManager.installedProviders.find {
            it.provider.packageName == widgetItem.packageName &&
                    it.provider.className == widgetItem.providerClassName
        }
    }
    val canReconfigure = !widgetItem.isPlaceholder && providerInfo?.configure != null

    var showPageSelector by remember { mutableStateOf(false) }

    if (showPageSelector) {
        PageSelectorDialog(
            currentItemPage = widgetItem.page,
            pageCount = pageCount,
            onDismiss = { showPageSelector = false },
            onPageSelected = { targetPage ->
                onMoveToPage(widgetItem, targetPage)
                showPageSelector = false
                onDismiss()
            }
        )
    } else {
        AnimatedContextMenuDialog(
            visible = true,
            onDismissRequest = onDismiss,
            animationsEnabled = animationsEnabled,
            title = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text("Widget Options", modifier = Modifier.align(Alignment.CenterStart))
                    IconButton(
                        onClick = { onNavigateToSettings(); onDismiss() },
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
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Close") }
            },
        ) {
            ScrollableDialogMenu {
                if (isBeingMoved) {
                    ContextMenuItemRow(
                        text = "Cancel Move",
                        icon = Icons.Default.Edit,
                        animationsEnabled = animationsEnabled,
                        onClick = { onCancelMove(); onDismiss() }
                    )
                } else {
                    ContextMenuItemRow(
                        text = "Move",
                        icon = Icons.Default.OpenWith,
                        animationsEnabled = animationsEnabled,
                        onClick = { onMove(widgetItem); onDismiss() }
                    )
                }
                if (!isBeingMoved && (pageCount > 1 || pageCount < MAX_PAGES)) {
                    ContextMenuItemRow(
                        text = "Move to page...",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        animationsEnabled = animationsEnabled,
                        onClick = { showPageSelector = true }
                    )
                }
                ContextMenuItemRow(
                    text = "Resize",
                    icon = Icons.Default.AspectRatio,
                    animationsEnabled = animationsEnabled,
                    onClick = { onResize(widgetItem); onDismiss() }
                )
                if (canReconfigure) {
                    ContextMenuItemRow(
                        text = "Configure",
                        icon = Icons.Default.Settings,
                        animationsEnabled = animationsEnabled,
                        onClick = { onConfigure(widgetItem); onDismiss() }
                    )
                }
                if (widgetItem.isPlaceholder) {
                    ContextMenuItemRow(
                        text = "Copy Details",
                        icon = Icons.Default.ContentCopy,
                        animationsEnabled = animationsEnabled,
                        onClick = { onCopyDetails(widgetItem); onDismiss() }
                    )
                }
                ContextMenuItemRow(
                    text = "Remove",
                    icon = Icons.Default.Delete,
                    animationsEnabled = animationsEnabled,
                    onClick = { onRemove(widgetItem); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun PlaceholderWidgetCard(
    modifier: Modifier,
    widgetItem: HomeItem.Widget,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val lineCount = when {
        widgetItem.rowSpan >= 3 || widgetItem.columnSpan >= 3 -> 3
        widgetItem.rowSpan >= 2 || widgetItem.columnSpan >= 2 -> 2
        else -> 1
    }
    val displayText = if (lineCount <= 1) {
        "..."
    } else {
        widgetItem.placeholderPrimaryLines().joinToString("\n")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(12.dp)
    ) {
        Text(
            text = displayText,
            maxLines = lineCount,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun copyWidgetDetailsToClipboard(context: android.content.Context, widget: HomeItem.Widget) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Widget Details", widget.placeholderDisplayText())
    clipboard.setPrimaryClip(clip)
}

@Composable
fun HomeAppContextMenu(
    appItem: HomeItem.App,
    pageCount: Int = 1,
    folders: List<HomeItem.Folder> = emptyList(),
    onDismiss: () -> Unit,
    onRemove: (HomeItem.App) -> Unit,
    onResize: (HomeItem.App) -> Unit,
    onMove: (HomeItem.App) -> Unit,
    onMoveToPage: (HomeItem.App, Int) -> Unit,
    onRename: (String) -> Unit = {},
    appTags: List<String> = emptyList(),
    onSaveTags: (List<String>) -> Unit = {},
    onAddToFolder: ((HomeItem.Folder) -> Unit)? = null,
    onTextSizeChange: (Float) -> Unit = {},
    onLabelAlignmentChange: (Int) -> Unit = {},
    showShortcutIconSetting: Boolean = true,
    onIconPlacementChange: (Int) -> Unit = {},
    onSelectFont: () -> Unit = {},
    onResetFont: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    animationsEnabled: Boolean = true,
) {
    var showPageSelector by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showCustomizeMenu by remember { mutableStateOf(false) }
    var showTagsEditor by remember { mutableStateOf(false) }
    var showFontMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showTextSizeEditor by remember { mutableStateOf(false) }
    var showLabelAlignmentPicker by remember { mutableStateOf(false) }
    var showIconPlacementPicker by remember { mutableStateOf(false) }
    var renameValue by remember(appItem.id, appItem.appModel.appLabel) { mutableStateOf(appItem.appModel.appLabel) }

    if (showPageSelector) {
        PageSelectorDialog(
            currentItemPage = appItem.page,
            pageCount = pageCount,
            onDismiss = { showPageSelector = false },
            onPageSelected = { targetPage ->
                onMoveToPage(appItem, targetPage)
                showPageSelector = false
                onDismiss()
            }
        )
    } else if (showTextSizeEditor) {
        var textSize by remember { mutableFloatStateOf(appItem.appTextSize) }
        AlertDialog(
            onDismissRequest = { showTextSizeEditor = false },
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
                        Text("Apply default text size (homescreen)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onTextSizeChange(textSize)
                    showTextSizeEditor = false
                    onDismiss()
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showTextSizeEditor = false }) { Text("Cancel") }
            }
        )
    } else if (showLabelAlignmentPicker) {
        app.cclauncher.ui.composables.LabelAlignmentDialog(
            currentAlignment = appItem.appLabelAlignment,
            onDismiss = { showLabelAlignmentPicker = false },
            onAlignmentSelected = { alignment ->
                onLabelAlignmentChange(alignment)
                showLabelAlignmentPicker = false
                onDismiss()
            }
        )
    } else if (showIconPlacementPicker) {
        IconPlacementDialog(
            currentPlacement = appItem.iconPlacement,
            onDismiss = { showIconPlacementPicker = false },
            onPlacementSelected = { placement ->
                onIconPlacementChange(placement)
                showIconPlacementPicker = false
                onDismiss()
            }
        )
    } else if (showFontMenu) {
        AlertDialog(
            onDismissRequest = { showFontMenu = false },
            title = { Text("Select Font") },
            text = {
                ScrollableDialogMenu {
                    ContextMenuItemRow(
                        text = "Select Font...",
                        icon = Icons.Default.TextFields,
                        onClick = { onSelectFont(); onDismiss() }
                    )
                    ContextMenuItemRow(
                        text = "Default Font",
                        icon = Icons.Default.TextFields,
                        onClick = { onResetFont(); onDismiss() }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontMenu = false }) { Text("Back") }
            }
        )
    } else if (showTagsEditor) {
        AppTagsEditorDialog(
            title = "Tags for ${appItem.appModel.appLabel}",
            initialTags = appTags,
            onSave = onSaveTags,
            onBack = { showTagsEditor = false },
            onDone = onDismiss
        )
    } else if (showCustomizeMenu) {
        AlertDialog(
            onDismissRequest = { showCustomizeMenu = false },
            title = { Text("Customize") },
            text = {
                ScrollableDialogMenu {
                    ContextMenuItemRow(
                        text = "Text Size...",
                        icon = Icons.Default.TextFields,
                        onClick = { showTextSizeEditor = true }
                    )
                    ContextMenuItemRow(
                        text = "Label Alignment...",
                        icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                        onClick = { showLabelAlignmentPicker = true }
                    )
                    ContextMenuItemRow(
                        text = "Select Font...",
                        icon = Icons.Default.TextFields,
                        onClick = { showFontMenu = true }
                    )
                    ContextMenuItemRow(
                        text = "Tags...",
                        icon = Icons.Default.Search,
                        onClick = { showTagsEditor = true }
                    )
                    if (appItem.appModel.isSystemShortcut && showShortcutIconSetting) {
                        ContextMenuItemRow(
                            text = "Change Icon Placement...",
                            icon = Icons.Default.SwapHoriz,
                            onClick = { showIconPlacementPicker = true }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomizeMenu = false }) { Text("Back") }
            }
        )
    } else if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename ${appItem.appModel.appLabel}") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    singleLine = true,
                    label = { Text("New name") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(renameValue)
                    showRenameDialog = false
                    onDismiss()
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    } else if (showFolderPicker) {
        AlertDialog(
            onDismissRequest = { showFolderPicker = false },
            title = { Text("Add to Folder") },
            text = {
                ScrollableDialogMenu {
                    folders.forEach { folder ->
                        ContextMenuItemRow(
                            text = "${folder.title} (${folder.apps.size} apps)",
                            icon = Icons.Default.Folder,
                            onClick = {
                                onAddToFolder?.invoke(folder)
                                showFolderPicker = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderPicker = false }) { Text("Cancel") }
            }
        )
    } else {
        AnimatedContextMenuDialog(
            visible = true,
            onDismissRequest = onDismiss,
            animationsEnabled = animationsEnabled,
            title = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text("App Options", modifier = Modifier.align(Alignment.CenterStart))
                    IconButton(
                        onClick = { onNavigateToSettings(); onDismiss() },
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
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Close") }
            },
        ) {
            ScrollableDialogMenu {
                ContextMenuItemRow(
                    text = "Move",
                    icon = Icons.Default.OpenWith,
                    animationsEnabled = animationsEnabled,
                    onClick = { onMove(appItem); onDismiss() }
                )
                if (pageCount > 1 || pageCount < MAX_PAGES) {
                    ContextMenuItemRow(
                        text = "Move to page...",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        animationsEnabled = animationsEnabled,
                        onClick = { showPageSelector = true }
                    )
                }
                ContextMenuItemRow(
                    text = "Resize",
                    icon = Icons.Default.AspectRatio,
                    animationsEnabled = animationsEnabled,
                    onClick = { onResize(appItem); onDismiss() }
                )
                ContextMenuItemRow(
                    text = "Customize...",
                    icon = Icons.Default.Edit,
                    animationsEnabled = animationsEnabled,
                    onClick = { showCustomizeMenu = true }
                )
                ContextMenuItemRow(
                    text = "Rename",
                    icon = Icons.Default.DriveFileRenameOutline,
                    animationsEnabled = animationsEnabled,
                    onClick = { showRenameDialog = true }
                )
                if (folders.isNotEmpty() && onAddToFolder != null) {
                    ContextMenuItemRow(
                        text = "Add to Folder...",
                        icon = Icons.Default.Folder,
                        animationsEnabled = animationsEnabled,
                        onClick = { showFolderPicker = true }
                    )
                }
                ContextMenuItemRow(
                    text = "Remove",
                    icon = Icons.Default.Delete,
                    animationsEnabled = animationsEnabled,
                    onClick = { onRemove(appItem); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun PageSelectorDialog(
    currentItemPage: Int,
    pageCount: Int,
    onDismiss: () -> Unit,
    onPageSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Page") },
        text = {
            Column {
                repeat(pageCount) { page ->
                    val isCurrentPage = page == currentItemPage
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Page ${page + 1}${if (isCurrentPage) " (current)" else ""}",
                                color = if (isCurrentPage) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = { if (!isCurrentPage) onPageSelected(page) },
                        enabled = !isCurrentPage
                    )
                }
                if (pageCount < MAX_PAGES) {
                    DropdownMenuItem(
                        text = { Text("+ New Page") },
                        onClick = { onPageSelected(pageCount) }
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
                                else MaterialTheme.colorScheme.onSurface
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

// Helper functions
private fun findAppAtPosition(
    homeLayout: HomeLayout,
    position: Offset,
    size: IntSize,
    page: Int
): HomeItem.App? {
    val cellWidth = size.width.toFloat() / homeLayout.columns
    val cellHeight = size.height.toFloat() / homeLayout.rows
    val column = (position.x / cellWidth).toInt()
    val row = (position.y / cellHeight).toInt()

    return homeLayout.itemsForPage(page).filterIsInstance<HomeItem.App>().find { app ->
        row >= app.row && row < app.row + app.rowSpan &&
                column >= app.column && column < app.column + app.columnSpan
    }
}

private fun findWidgetAtPosition(
    homeLayout: HomeLayout,
    position: Offset,
    size: IntSize,
    page: Int
): HomeItem.Widget? {
    val cellWidth = size.width.toFloat() / homeLayout.columns
    val cellHeight = size.height.toFloat() / homeLayout.rows
    val column = (position.x / cellWidth).toInt()
    val row = (position.y / cellHeight).toInt()

    return homeLayout.itemsForPage(page).filterIsInstance<HomeItem.Widget>().find { widget ->
        row >= widget.row && row < widget.row + widget.rowSpan &&
                column >= widget.column && column < widget.column + widget.columnSpan
    }
}

private fun findFolderAtPosition(
    homeLayout: HomeLayout,
    position: Offset,
    size: IntSize,
    page: Int
): HomeItem.Folder? {
    val cellWidth = size.width.toFloat() / homeLayout.columns
    val cellHeight = size.height.toFloat() / homeLayout.rows
    val column = (position.x / cellWidth).toInt()
    val row = (position.y / cellHeight).toInt()

    return homeLayout.itemsForPage(page).filterIsInstance<HomeItem.Folder>()
        .filter { it.showOnHome }
        .find { folder ->
            row >= folder.row && row < folder.row + folder.rowSpan &&
                    column >= folder.column && column < folder.column + folder.columnSpan
        }
}

private fun calculateGridPosition(
    position: Offset,
    homeLayout: HomeLayout,
    size: IntSize
): Pair<Int, Int>? {
    val cellWidth = size.width.toFloat() / homeLayout.columns
    val cellHeight = size.height.toFloat() / homeLayout.rows
    val column = (position.x / cellWidth).toInt()
    val row = (position.y / cellHeight).toInt()

    return if (row in 0 until homeLayout.rows && column in 0 until homeLayout.columns) {
        Pair(row, column)
    } else null
}

/** Dwell time (ms) required before a swipe-up is recognised on bottom corner zones.
 *  Bottom zones sit on top of Android's home-gesture strip; requiring a brief press
 *  differentiates an intentional zone swipe from the system's quick-flick home gesture. */
private const val BOTTOM_ZONE_SWIPE_UP_DWELL_MS = 120L

/** Dwell time (ms) required before a swipe-down is recognised on top corner zones.
 *  Top zones sit adjacent to the status bar / notification-shade pull area; the dwell
 *  reduces (but cannot eliminate) races with the system shade gesture. */
private const val TOP_ZONE_SWIPE_DOWN_DWELL_MS = 120L

/**
 * Renders the 4 corner shortcut zones overlaid on the home screen.
 * Each zone is a right triangle anchored at its screen corner with a 45° hypotenuse.
 * Zones with [CornerZoneConfig.visible] == false are transparent but still receive touches.
 * Zones with [CornerZoneConfig.enabled] == false are skipped entirely.
 *
 * Registers system gesture exclusion rects so Android's back-gesture does not fire
 * inside the active zone areas.
 */
@Composable
private fun HomeCornerZones(
    settings: AppSettings,
    activeOrientation: app.cclauncher.data.HomeOrientation,
    onOpenFolder: (String) -> Unit,
    onAction: (Int) -> Unit,
) {
    val view = LocalView.current
    // cornerPos → exclusion rect; updated by each zone via onGloballyPositioned
    val exclusionRects = remember { mutableMapOf<Int, AndroidRect>() }

    fun pushExclusionRects() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.post { view.systemGestureExclusionRects = exclusionRects.values.toList() }
        }
    }

    // Clear all exclusion rects when the composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                view.post { view.systemGestureExclusionRects = emptyList() }
            }
        }
    }

    val zones = listOf(
        settings.cornerConfigFor(activeOrientation, Constants.CornerPosition.TOP_LEFT),
        settings.cornerConfigFor(activeOrientation, Constants.CornerPosition.TOP_RIGHT),
        settings.cornerConfigFor(activeOrientation, Constants.CornerPosition.BOTTOM_LEFT),
        settings.cornerConfigFor(activeOrientation, Constants.CornerPosition.BOTTOM_RIGHT),
    )
    val alignments = listOf(
        Alignment.TopStart,
        Alignment.TopEnd,
        Alignment.BottomStart,
        Alignment.BottomEnd,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        zones.forEachIndexed { cornerPos, rawConfig ->
            if (!rawConfig.enabled) {
                // Zone disabled — remove its rect so the system gesture is no longer blocked
                if (exclusionRects.remove(cornerPos) != null) pushExclusionRects()
                return@forEachIndexed
            }
            val config = if (settings.applyToAllCornerZonesFor(activeOrientation)) {
                val u = settings.cornerUniversalConfigFor(activeOrientation)
                rawConfig.copy(
                    size = u.size, color = u.color, opacity = u.opacity, visible = u.visible,
                    borderEnabled = u.borderEnabled, borderColor = u.borderColor, borderWidth = u.borderWidth,
                )
            } else rawConfig

            CornerZoneElement(
                config = config,
                alignment = alignments[cornerPos],
                cornerPos = cornerPos,
                showDangerFade = settings.cornerZoneDangerFadeFor(activeOrientation),
                onBoundsChanged = { rect ->
                    if (rect != null) exclusionRects[cornerPos] = rect
                    else exclusionRects.remove(cornerPos)
                    pushExclusionRects()
                },
                onTap = {
                    when (config.action) {
                        Constants.SwipeAction.OPEN_FOLDER -> onOpenFolder(config.folderId)
                        else -> onAction(config.action)
                    }
                },
                onHold = {
                    if (config.holdEnabled) {
                        when (config.holdAction) {
                            Constants.SwipeAction.OPEN_FOLDER -> onOpenFolder(config.holdFolderId)
                            else -> onAction(config.holdAction)
                        }
                    }
                },
                onSwipe = { dir ->
                    val swipeCfg = when (dir) {
                        Constants.ZoneSwipeDir.LEFT  -> config.swipeLeft
                        Constants.ZoneSwipeDir.RIGHT -> config.swipeRight
                        Constants.ZoneSwipeDir.UP    -> config.swipeUp
                        Constants.ZoneSwipeDir.DOWN  -> config.swipeDown
                    }
                    if (swipeCfg.enabled) {
                        when (swipeCfg.action) {
                            Constants.SwipeAction.OPEN_FOLDER -> onOpenFolder(swipeCfg.folderId)
                            else -> onAction(swipeCfg.action)
                        }
                    }
                },
            )
        }
    }
}

private sealed class ZoneGestureResult {
    data object None : ZoneGestureResult()
    data object Tap : ZoneGestureResult()
    data object LongPress : ZoneGestureResult()
    /** [elapsedMs] = millis between down and swipe-threshold being crossed. */
    data class Swipe(val delta: Offset, val elapsedMs: Long) : ZoneGestureResult()
}

@Composable
private fun BoxScope.CornerZoneElement(
    config: CornerZoneConfig,
    alignment: Alignment,
    cornerPos: Int,
    showDangerFade: Boolean = true,
    onBoundsChanged: (AndroidRect?) -> Unit,
    onTap: () -> Unit,
    onHold: () -> Unit,
    onSwipe: (Constants.ZoneSwipeDir) -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.08f else 1.0f,
        label = "zone_scale",
    )
    val opacityBoost by animateFloatAsState(
        targetValue = if (isPressed) 0.25f else 0.0f,
        label = "zone_brightness",
    )

    val effectiveOpacity = (config.opacity + opacityBoost).coerceIn(0f, 1f)
    val fillColor = if (config.visible) Color(config.color).copy(alpha = effectiveOpacity) else Color.Transparent
    val strokeColor = if (config.visible && config.borderEnabled) Color(config.borderColor).copy(alpha = effectiveOpacity) else Color.Transparent
    val borderWidthDp = config.borderWidth.dp

    val transformOrigin = when (cornerPos) {
        Constants.CornerPosition.TOP_LEFT    -> TransformOrigin(0f, 0f)
        Constants.CornerPosition.TOP_RIGHT   -> TransformOrigin(1f, 0f)
        Constants.CornerPosition.BOTTOM_LEFT -> TransformOrigin(0f, 1f)
        else                                 -> TransformOrigin(1f, 1f)
    }

    val validDirs = remember(cornerPos) { Constants.validSwipeDirs(cornerPos) }
    val isBottomZone = cornerPos == Constants.CornerPosition.BOTTOM_LEFT ||
                       cornerPos == Constants.CornerPosition.BOTTOM_RIGHT
    val isTopZone = cornerPos == Constants.CornerPosition.TOP_LEFT ||
                    cornerPos == Constants.CornerPosition.TOP_RIGHT

    // Triangle clip shape — restricts BOTH rendering and hit-testing to just the triangle.
    // In Compose 1.7+, Modifier.clip() with a non-rectangular shape also restricts pointer
    // hit-testing, so touches in the rectangular bounding box that are OUTSIDE the triangle
    // fall through to the underlying composables (home screen swipe gestures) instead of
    // being absorbed by this Box.
    val triangleShape = remember(cornerPos) {
        GenericShape { size, _ ->
            when (cornerPos) {
                Constants.CornerPosition.TOP_LEFT    -> { moveTo(0f, 0f);         lineTo(size.width, 0f); lineTo(0f, size.height) }
                Constants.CornerPosition.TOP_RIGHT   -> { moveTo(size.width, 0f); lineTo(0f, 0f);         lineTo(size.width, size.height) }
                Constants.CornerPosition.BOTTOM_LEFT -> { moveTo(0f, size.height); lineTo(0f, 0f);        lineTo(size.width, size.height) }
                else                                  -> { moveTo(size.width, size.height); lineTo(size.width, 0f); lineTo(0f, size.height) }
            }
            close()
        }
    }

    // Danger-strip heights: how many px from the screen edge to treat as the collision zone.
    // Navigation bar inset covers gesture nav area at the bottom; status bar inset covers the
    // notification-shade pull area at the top. Both are coerced to sensible minimums so the
    // indicator still shows when the insets are reported as 0 (gesture nav / hidden status bar).
    val density = LocalDensity.current
    val navDangerPx = with(density) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            .coerceAtLeast(48.dp).toPx()
    }
    val statusDangerPx = with(density) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            .coerceAtLeast(24.dp).toPx()
    }

    // Remove this zone's exclusion rect when it leaves composition
    DisposableEffect(Unit) {
        onDispose { onBoundsChanged(null) }
    }

    Box(
        modifier = Modifier
            .align(alignment)
            .size(config.size.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale, transformOrigin = transformOrigin)
            // Register exclusion rect so Android's back-gesture won't fire over the zone
            .onGloballyPositioned { coords ->
                val b = coords.boundsInWindow()
                onBoundsChanged(
                    AndroidRect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())
                )
            }
            // Clip to the triangle — in Compose 1.7+ this also restricts hit-testing so
            // the rectangular "dead zone" outside the triangle no longer blocks gestures.
            .clip(triangleShape)
            .pointerInput(
                config.action, config.holdEnabled, config.holdAction,
                config.swipeLeft, config.swipeRight, config.swipeUp, config.swipeDown,
                config.holdDurationMs, config.swipeDwellMs,
            ) {
                awaitPointerEventScope {
                  while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!isInsideZoneTriangle(down.position, size.width.toFloat(), cornerPos)) {
                        // Outside the triangle — release immediately so the home screen's sibling
                        // gesture detectors (swipe up/down/left/right) receive this touch
                        // unobstructed. Skipping awaitAllPointersUp is the key: awaitEachGesture
                        // would call it, keeping this pointerInput scope "observing" at
                        // PointerEventPass.Final for the entire gesture and blocking siblings.
                        continue
                    }

                    // Claim the down event so the home screen's swipe handler ignores it
                    down.consume()
                    isPressed = true
                    val startTime = System.currentTimeMillis()
                    val startPos = down.position
                    var result: ZoneGestureResult = ZoneGestureResult.None

                    try {
                        withTimeout(config.holdDurationMs.toLong()) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                // Consume every movement event — prevents launcher swipe handlers
                                // further up the composition tree from also firing
                                change.consume()
                                if (!change.pressed) {
                                    result = ZoneGestureResult.Tap
                                    break
                                }
                                val delta = change.position - startPos
                                if (delta.getDistance() > viewConfiguration.touchSlop * 3) {
                                    result = ZoneGestureResult.Swipe(
                                        delta = delta,
                                        elapsedMs = System.currentTimeMillis() - startTime,
                                    )
                                    break
                                }
                            }
                        }
                    } catch (_: PointerEventTimeoutCancellationException) {
                        result = ZoneGestureResult.LongPress
                    }

                    isPressed = false

                    when (val r = result) {
                        is ZoneGestureResult.Tap -> onTap()
                        is ZoneGestureResult.LongPress -> onHold()
                        is ZoneGestureResult.Swipe -> {
                            val dir = classifyZoneSwipeDir(r.delta)
                            // Compute the required dwell for this direction.
                            // Bottom-zone swipe-up is always clamped to at least
                            // BOTTOM_ZONE_SWIPE_UP_DWELL_MS to resist Android's home gesture.
                            val requiredDwell = when {
                                isBottomZone && dir == Constants.ZoneSwipeDir.UP ->
                                    maxOf(BOTTOM_ZONE_SWIPE_UP_DWELL_MS, config.swipeDwellMs.toLong())
                                isTopZone && dir == Constants.ZoneSwipeDir.DOWN ->
                                    maxOf(TOP_ZONE_SWIPE_DOWN_DWELL_MS, config.swipeDwellMs.toLong())
                                else -> config.swipeDwellMs.toLong()
                            }
                            if (dir != null && dir in validDirs && r.elapsedMs >= requiredDwell) {
                                onSwipe(dir)
                            }
                        }
                        ZoneGestureResult.None -> {}
                    }

                    // Drain remaining events until the finger lifts
                    while (currentEvent.changes.any { it.pressed }) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                  } // end while(true)
                } // end awaitPointerEventScope
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = buildZoneTrianglePath(size.width, cornerPos)
            drawPath(path, color = fillColor)
            if (config.borderEnabled && config.visible) {
                drawPath(path, color = strokeColor, style = Stroke(width = borderWidthDp.toPx()))
            }

            // Danger-edge fade: linear gradient from the hypotenuse midpoint (dark, visible
            // interior) to the outer screen corner (transparent, hidden by rounded bezel).
            // The gradient direction ensures the ENTIRE hypotenuse is uniformly darkest, so
            // the fade is visible along the diagonal AND along both screen-edge sides of the
            // triangle. The drawing is clipped to the actual danger strip (nav-bar / status-bar
            // height) so the indicator only covers the area that conflicts with system gestures.
            // Purely cosmetic; actions still fire everywhere.
            if (config.visible && showDangerFade) {
                val fadeColor = Color.Black
                val fadeAlpha = 0.22f
                val hyp = Offset(size.width / 2, size.height / 2) // hypotenuse midpoint (start = dark)
                val brush = when (cornerPos) {
                    Constants.CornerPosition.BOTTOM_LEFT  -> Brush.linearGradient(
                        colors = listOf(fadeColor.copy(alpha = fadeAlpha), Color.Transparent),
                        start = hyp, end = Offset(0f, size.height),
                    )
                    Constants.CornerPosition.BOTTOM_RIGHT -> Brush.linearGradient(
                        colors = listOf(fadeColor.copy(alpha = fadeAlpha), Color.Transparent),
                        start = hyp, end = Offset(size.width, size.height),
                    )
                    Constants.CornerPosition.TOP_LEFT     -> Brush.linearGradient(
                        colors = listOf(fadeColor.copy(alpha = fadeAlpha), Color.Transparent),
                        start = hyp, end = Offset(0f, 0f),
                    )
                    else                                  -> Brush.linearGradient( // TOP_RIGHT
                        colors = listOf(fadeColor.copy(alpha = fadeAlpha), Color.Transparent),
                        start = hyp, end = Offset(size.width, 0f),
                    )
                }
                // Clip to the danger strip (bottom N px for bottom zones, top N px for top zones)
                if (isBottomZone) {
                    val stripTop = (size.height - navDangerPx).coerceAtLeast(0f)
                    clipRect(left = 0f, top = stripTop, right = size.width, bottom = size.height) {
                        drawPath(path, brush = brush)
                    }
                } else {
                    val stripBottom = statusDangerPx.coerceAtMost(size.height)
                    clipRect(left = 0f, top = 0f, right = size.width, bottom = stripBottom) {
                        drawPath(path, brush = brush)
                    }
                }
            }
        }
    }
}

private fun classifyZoneSwipeDir(delta: Offset): Constants.ZoneSwipeDir? {
    if (delta.getDistance() < 5f) return null
    return if (abs(delta.x) >= abs(delta.y)) {
        if (delta.x > 0) Constants.ZoneSwipeDir.RIGHT else Constants.ZoneSwipeDir.LEFT
    } else {
        if (delta.y > 0) Constants.ZoneSwipeDir.DOWN else Constants.ZoneSwipeDir.UP
    }
}

private fun buildZoneTrianglePath(size: Float, cornerPos: Int): Path = Path().apply {
    when (cornerPos) {
        Constants.CornerPosition.TOP_LEFT    -> { moveTo(0f, 0f);    lineTo(size, 0f);  lineTo(0f, size)  }
        Constants.CornerPosition.TOP_RIGHT   -> { moveTo(size, 0f);  lineTo(0f, 0f);   lineTo(size, size) }
        Constants.CornerPosition.BOTTOM_LEFT -> { moveTo(0f, size);  lineTo(0f, 0f);   lineTo(size, size) }
        else                                 -> { moveTo(size, size); lineTo(size, 0f); lineTo(0f, size)  }
    }
    close()
}

private fun isInsideZoneTriangle(offset: Offset, size: Float, cornerPos: Int): Boolean {
    val x = offset.x; val y = offset.y
    if (x < 0f || y < 0f || x > size || y > size) return false
    return when (cornerPos) {
        Constants.CornerPosition.TOP_LEFT    -> x + y <= size
        Constants.CornerPosition.TOP_RIGHT   -> (size - x) + y <= size
        Constants.CornerPosition.BOTTOM_LEFT -> x + (size - y) <= size
        else                                 -> (size - x) + (size - y) <= size
    }
}

@Composable
fun FolderContextMenu(
    folderItem: HomeItem.Folder?,
    pageCount: Int = 1,
    onDismiss: () -> Unit,
    onRemoveFromHome: (HomeItem.Folder) -> Unit,
    onDelete: (HomeItem.Folder) -> Unit,
    onResize: (HomeItem.Folder) -> Unit,
    onMove: (HomeItem.Folder) -> Unit,
    onMoveToPage: (HomeItem.Folder, Int) -> Unit,
    onRename: (HomeItem.Folder, String) -> Unit,
    onTextSizeChange: (Float) -> Unit = {},
    onLabelAlignmentChange: (Int) -> Unit = {},
    showFolderIconSetting: Boolean = true,
    onIconPlacementChange: (Int) -> Unit = {},
    onSelectFont: () -> Unit = {},
    onResetFont: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    animationsEnabled: Boolean = true,
) {
    var showPageSelector by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCustomizeMenu by remember { mutableStateOf(false) }
    var showFontMenu by remember { mutableStateOf(false) }
    var showTextSizeEditor by remember { mutableStateOf(false) }
    var showLabelAlignmentPicker by remember { mutableStateOf(false) }
    var showIconPlacementPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var renameValue by remember(folderItem?.title) { mutableStateOf(folderItem?.title ?: "") }

    val anySubDialog = showPageSelector || showRenameDialog || showCustomizeMenu ||
            showFontMenu || showTextSizeEditor || showLabelAlignmentPicker ||
            showIconPlacementPicker || showDeleteConfirm

    // Primary animated context menu — always in composition so exit animation can play
    AnimatedContextMenuDialog(
        visible = folderItem != null && !anySubDialog,
        onDismissRequest = onDismiss,
        animationsEnabled = animationsEnabled,
        instantExit = anySubDialog,
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text("Folder Options", modifier = Modifier.align(Alignment.CenterStart))
                IconButton(
                    onClick = { onNavigateToSettings(); onDismiss() },
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
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    ) {
        if (folderItem != null) {
            ScrollableDialogMenu {
                ContextMenuItemRow(
                    text = "Move",
                    icon = Icons.Default.OpenWith,
                    animationsEnabled = animationsEnabled,
                    onClick = { onMove(folderItem); onDismiss() }
                )
                if (pageCount > 1 || pageCount < MAX_PAGES) {
                    ContextMenuItemRow(
                        text = "Move to page...",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        animationsEnabled = animationsEnabled,
                        onClick = { showPageSelector = true }
                    )
                }
                ContextMenuItemRow(
                    text = "Resize",
                    icon = Icons.Default.AspectRatio,
                    animationsEnabled = animationsEnabled,
                    onClick = { onResize(folderItem); onDismiss() }
                )
                ContextMenuItemRow(
                    text = "Customize...",
                    icon = Icons.Default.Edit,
                    animationsEnabled = animationsEnabled,
                    onClick = { showCustomizeMenu = true }
                )
                ContextMenuItemRow(
                    text = "Rename",
                    icon = Icons.Default.DriveFileRenameOutline,
                    animationsEnabled = animationsEnabled,
                    onClick = { showRenameDialog = true }
                )
                ContextMenuItemRow(
                    text = "Remove",
                    icon = Icons.Default.Delete,
                    animationsEnabled = animationsEnabled,
                    onClick = { onRemoveFromHome(folderItem); onDismiss() }
                )
                ContextMenuItemRow(
                    text = "Delete",
                    icon = Icons.Default.Delete,
                    animationsEnabled = animationsEnabled,
                    onClick = { showDeleteConfirm = true }
                )
            }
        }
    }

    // Sub-dialogs — shown alongside the primary menu (not in an else chain)
    if (folderItem != null) {
        if (showPageSelector) {
            PageSelectorDialog(
                currentItemPage = folderItem.page,
                pageCount = pageCount,
                onDismiss = { showPageSelector = false },
                onPageSelected = { targetPage ->
                    onMoveToPage(folderItem, targetPage)
                    showPageSelector = false
                    onDismiss()
                }
            )
        }
        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename Folder") },
                text = {
                    androidx.compose.material3.OutlinedTextField(
                        value = renameValue,
                        onValueChange = { renameValue = it },
                        singleLine = true,
                        label = { Text("Folder name") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onRename(folderItem, renameValue)
                        showRenameDialog = false
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
                }
            )
        }
        if (showTextSizeEditor) {
            var textSize by remember { mutableFloatStateOf(folderItem.titleTextSize) }
            AlertDialog(
                onDismissRequest = { showTextSizeEditor = false },
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
                        TextButton(onClick = { textSize = 1.0f }) {
                            Text("Reset to default")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onTextSizeChange(textSize)
                        onDismiss()
                    }) { Text("Apply") }
                },
                dismissButton = {
                    TextButton(onClick = { showTextSizeEditor = false }) { Text("Cancel") }
                }
            )
        }
        if (showLabelAlignmentPicker) {
            app.cclauncher.ui.composables.LabelAlignmentDialog(
                currentAlignment = folderItem.titleLabelAlignment,
                onDismiss = { showLabelAlignmentPicker = false },
                onAlignmentSelected = { alignment ->
                    onLabelAlignmentChange(alignment)
                    showLabelAlignmentPicker = false
                    onDismiss()
                }
            )
        }
        if (showIconPlacementPicker) {
            IconPlacementDialog(
                currentPlacement = folderItem.iconPlacement,
                onDismiss = { showIconPlacementPicker = false },
                onPlacementSelected = { placement ->
                    onIconPlacementChange(placement)
                    showIconPlacementPicker = false
                    onDismiss()
                }
            )
        }
        if (showFontMenu) {
            AlertDialog(
                onDismissRequest = { showFontMenu = false },
                title = { Text("Select Font") },
                text = {
                    ScrollableDialogMenu {
                        ContextMenuItemRow(
                            text = "Select Font...",
                            icon = Icons.Default.TextFields,
                            onClick = { onSelectFont(); onDismiss() }
                        )
                        ContextMenuItemRow(
                            text = "Default Font",
                            icon = Icons.Default.TextFields,
                            onClick = { onResetFont(); onDismiss() }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFontMenu = false }) { Text("Back") }
                }
            )
        }
        if (showCustomizeMenu) {
            AlertDialog(
                onDismissRequest = { showCustomizeMenu = false },
                title = { Text("Customize") },
                text = {
                    ScrollableDialogMenu {
                        ContextMenuItemRow(
                            text = "Text Size...",
                            icon = Icons.Default.TextFields,
                            onClick = { showTextSizeEditor = true }
                        )
                        ContextMenuItemRow(
                            text = "Label Alignment...",
                            icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                            onClick = { showLabelAlignmentPicker = true }
                        )
                        ContextMenuItemRow(
                            text = "Select Font...",
                            icon = Icons.Default.TextFields,
                            onClick = { showFontMenu = true }
                        )
                        if (showFolderIconSetting) {
                            ContextMenuItemRow(
                                text = "Change Icon Placement...",
                                icon = Icons.Default.SwapHoriz,
                                onClick = { showIconPlacementPicker = true }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCustomizeMenu = false }) { Text("Back") }
                }
            )
        }
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Folder") },
                text = { Text("Are you sure? This will permanently delete the folder and all its contents.") },
                confirmButton = {
                    TextButton(onClick = {
                        onDelete(folderItem)
                        showDeleteConfirm = false
                        onDismiss()
                    }) { Text("YES") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }
}
