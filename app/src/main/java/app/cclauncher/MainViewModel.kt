package app.cclauncher

import android.app.Activity.RESULT_OK
import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.res.Configuration
import android.os.UserHandle
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cclauncher.data.*
import app.cclauncher.data.Constants.MAX_PAGES
import app.cclauncher.data.repository.AppRepository
import app.cclauncher.helper.AppTagStorage
import app.cclauncher.helper.BitmapUtils
import app.cclauncher.settings.AppSettingsRepository
import app.cclauncher.settings.AppPreference
import app.cclauncher.settings.AppSettings
import app.cclauncher.settings.AppKeyMigration
import app.cclauncher.settings.availableHomeOrientations
import app.cclauncher.settings.homeColumnsFor
import app.cclauncher.settings.homePagesFor
import app.cclauncher.settings.homeRowsFor
import app.cclauncher.settings.isLandscapeHomeAvailable
import app.cclauncher.settings.swipeAppFor
import app.cclauncher.helper.IconCache
import app.cclauncher.helper.MyAccessibilityService
import app.cclauncher.helper.PrivateSpaceHelper
import app.cclauncher.helper.SearchAliasUtils
import app.cclauncher.helper.getScreenDimensions
import app.cclauncher.helper.getUserHandleFromString
import app.cclauncher.ui.UiEvent
import app.cclauncher.ui.AppDrawerUiState
import app.cclauncher.ui.components.snackbar.SnackbarManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID
import kotlin.math.ceil

/**
 * MainViewModel is the primary ViewModel for CCLauncher that manages app state and user interactions.
 */
@OptIn(FlowPreview::class)
class MainViewModel(application: Application, private val appWidgetHost: AppWidgetHost) : AndroidViewModel(application), KoinComponent {
    private val appContext = application.applicationContext
    val settingsRepository: AppSettingsRepository by inject()
    private val appRepository: AppRepository by inject()

    private val REQUEST_CODE_CONFIGURE_WIDGET = WidgetConstants.REQUEST_CONFIGURE_WIDGET
    private var pendingWidgetInfo: PendingWidgetInfo? = null
    private var pendingWidgetReplacementTarget: HomeItem.Widget? = null

    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger = _refreshTrigger.asStateFlow()
    private val _settingsSnapshot = MutableStateFlow(AppSettings())
    val settingsSnapshot: StateFlow<AppSettings> = _settingsSnapshot.asStateFlow()

    private data class AppReloadRequest(
        val reason: String,
        val forceEmit: Boolean = false
    )

    private val appReloadRequests = MutableSharedFlow<AppReloadRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private fun requestAppReload(
        reason: String,
        forceEmit: Boolean = false
    ) {
        appReloadRequests.tryEmit(AppReloadRequest(reason, forceEmit))
    }

    private val privateSpaceHelper = PrivateSpaceHelper(application.applicationContext)

    val isPrivateSpaceSupported = privateSpaceHelper.isPrivateSpaceSupported()

    private val _privateSpaceState = MutableStateFlow<PrivateSpaceState>(PrivateSpaceState.Unsupported)
    val privateSpaceState: StateFlow<PrivateSpaceState> = _privateSpaceState.asStateFlow()

    data class PendingWidgetInfo(val appWidgetId: Int, val providerInfo: android.appwidget.AppWidgetProviderInfo)

    // Events manager for UI events
    private val _eventsFlow = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _eventsFlow.asSharedFlow()

    // UI States
    private val _homeLayoutState = MutableStateFlow(HomeLayout())
    val homeLayoutState: StateFlow<HomeLayout> = _homeLayoutState.asStateFlow()

    private val _appDrawerState = MutableStateFlow(AppDrawerUiState())
    val appDrawerState: StateFlow<AppDrawerUiState> = _appDrawerState.asStateFlow()
    private val appDrawerSearchQuery = MutableStateFlow("")

    // App list state
    private val _appList = MutableStateFlow<List<AppModel>>(emptyList())
    val appList: StateFlow<List<AppModel>> = _appList.asStateFlow()

    private val _appListAll = MutableStateFlow<List<AppModel>>(emptyList())
    val appListAll: StateFlow<List<AppModel>> = _appListAll.asStateFlow()

    private val _hiddenApps = MutableStateFlow<List<AppModel>>(emptyList())
    val hiddenApps: StateFlow<List<AppModel>> = _hiddenApps.asStateFlow()
    private val _appTags = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val appTags: StateFlow<Map<String, List<String>>> = _appTags.asStateFlow()

    private val _keyboardShortcuts = MutableStateFlow<List<KeyboardShortcut>>(emptyList())
    val keyboardShortcuts: StateFlow<List<KeyboardShortcut>> = _keyboardShortcuts.asStateFlow()

    // Reset launcher state
    private val _launcherResetFailed = MutableStateFlow(false)
    val launcherResetFailed: StateFlow<Boolean> = _launcherResetFailed.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()
    private val _activeHomeOrientation = MutableStateFlow(
        if (appContext.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            HomeOrientation.LANDSCAPE
        } else {
            HomeOrientation.PORTRAIT
        }
    )
    val activeHomeOrientation: StateFlow<HomeOrientation> = _activeHomeOrientation.asStateFlow()
    private val _widgetReplacementTarget = MutableStateFlow<HomeItem.Widget?>(null)
    val widgetReplacementTarget: StateFlow<HomeItem.Widget?> = _widgetReplacementTarget.asStateFlow()

    val appWidgetManager: AppWidgetManager =  AppWidgetManager.getInstance(appContext)

    val snackbarManager : SnackbarManager by inject()

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            requestAppReload("packageRemoved:$packageName")
        }

        override fun onPackageAdded(packageName: String, user: UserHandle) {
            requestAppReload("packageAdded:$packageName")
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            requestAppReload("packageChanged:$packageName")
        }

        override fun onPackagesAvailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean
        ) {
            requestAppReload("packagesAvailable:${packageNames.joinToString()}")
        }

        override fun onPackagesUnavailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean
        ) {
            requestAppReload("packagesUnavailable:${packageNames.joinToString()}")
        }

        override fun onShortcutsChanged(
            packageName: String,
            shortcuts: List<android.content.pm.ShortcutInfo>,
            user: UserHandle
        ) {
            Log.d("MainViewModel", "System shortcuts changed for $packageName")
            requestAppReload("shortcutsChanged:$packageName")
        }
    }

    private val appRefreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            Log.d("MainViewModel", "Received broadcast: ${intent?.action}")
            if (intent?.action == "app.cclauncher.ACTION_REFRESH_APPS") {
                Log.d("MainViewModel", "Queueing app refresh after broadcast")
                requestAppReload("customRefreshBroadcast", forceEmit = true)
            }
        }
    }

    // Alias search index: key = app.getKey(), value = set of aliases
    private var searchAliasIndex: Map<String, Set<String>> = emptyMap()

    init {
        viewModelScope.launch {
            settingsRepository.ensureOrientationAwareMigration()
        }

        viewModelScope.launch {
            combine(
                settingsRepository.settings,
                _activeHomeOrientation
            ) { settings, requestedOrientation ->
                val effectiveOrientation = resolveEffectiveHomeOrientation(settings, requestedOrientation)
                settingsRepository.setActiveHomeOrientation(effectiveOrientation)
                val layout = settings.homeLayouts.layoutFor(effectiveOrientation)
                if (settings.homePagesFor(effectiveOrientation) != layout.pageCount) {
                    settingsRepository.updateHomePageSetting(effectiveOrientation, layout.pageCount)
                }
                val updatedLayout = layout.copy(
                    rows = settings.homeRowsFor(effectiveOrientation),
                    columns = settings.homeColumnsFor(effectiveOrientation),
                    pageCount = settings.homePagesFor(effectiveOrientation)
                )
                Triple(loadIconsForHomeLayout(updatedLayout, settings), effectiveOrientation, settings)
            }.collect { (updatedLayout, effectiveOrientation, settings) ->
                _settingsSnapshot.value = settings
                _activeHomeOrientation.value = effectiveOrientation
                _homeLayoutState.value = updatedLayout
                if (_currentPage.value >= updatedLayout.pageCount) {
                    _currentPage.value = (updatedLayout.pageCount - 1).coerceAtLeast(0)
                }
            }
        }

        viewModelScope.launch {
            combine(
                appRepository.appListAll,
                appRepository.appList
            ) { allApps, visibleApps ->
                _appListAll.value = allApps
                _appList.value = visibleApps
                updateAppDrawerState()
                reapplySearchFilter()
            }.collect {}
        }

        // Observe hidden apps changes
        viewModelScope.launch {
            appRepository.hiddenApps.collect { apps ->
                _hiddenApps.value = apps
            }
        }

        viewModelScope.launch {
            settingsRepository.settings
                .map { AppTagStorage.decode(it.appTagsJson) }
                .distinctUntilChanged()
                .collect { tags ->
                    _appTags.value = tags
                }
        }

        viewModelScope.launch {
            settingsRepository.getKeyboardShortcuts().collect { shortcuts ->
                _keyboardShortcuts.value = shortcuts
            }
        }

        viewModelScope.launch {
            settingsRepository.settings
                .map { it.selectedIconPack }
                .distinctUntilChanged()
                .drop(1) // Skip initial value
                .collect { _ ->
                    refreshHomeScreenAppIcons()
                }
        }

        viewModelScope.launch {
            settingsRepository.settings
                .map { it.showPinnedShortcuts }
                .distinctUntilChanged()
                .drop(1) // Skip initial value
                .collect { _ ->
                    requestAppReload("showPinnedShortcutsChanged", forceEmit = true)
                }
        }

        // Rebuild alias index whenever app list or relevant settings change
        viewModelScope.launch {
            combine(
                appRepository.appListAll,
                settingsRepository.settings
                    .map { Triple(it.searchAliasesMode, it.searchIncludePackageNames, AppTagStorage.decode(it.appTagsJson)) }
                    .distinctUntilChanged()
            ) { _, _ -> }
                .collect {
                    rebuildSearchAliasIndex()
                    reapplySearchFilter()
                }
        }

        viewModelScope.launch {
            appRepository.appListAll
                .map { apps -> apps.filter { it.isSystemShortcut }.mapTo(mutableSetOf()) { it.getKey() } }
                .distinctUntilChanged()
                .collect { shortcutKeys ->
                    settingsRepository.removeOrphanedShortcutTags(shortcutKeys)
                }
        }

        viewModelScope.launch {
            settingsRepository.settings
                .map { Triple(it.searchType, it.searchSortOrder, it.showHiddenAppsOnSearch) }
                .distinctUntilChanged()
                .collect { 
                    requestAppReload("searchSettingsChanged", forceEmit = true)
                    reapplySearchFilter() 
                }
        }

        viewModelScope.launch {
            appDrawerSearchQuery
                .collectLatest { query ->
                    _appDrawerState.update {
                        it.copy(
                            searchQuery = query,
                            isLoading = true,
                            error = null
                        )
                    }

                    try {
                        val filtered = filterAndRank(query)
                        _appDrawerState.update { current ->
                            if (current.searchQuery != query) {
                                current
                            } else {
                                current.copy(
                                    filteredApps = filtered,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                    } catch (e: Exception) {
                        snackbarManager.show("Search failed: ${e.message}")
                        _appDrawerState.update { current ->
                            if (current.searchQuery != query) {
                                current
                            } else {
                                current.copy(
                                    isLoading = false,
                                    error = e.message
                                )
                            }
                        }
                    }
                }
        }

        updatePrivateSpaceState()

        val filter = IntentFilter("app.cclauncher.ACTION_REFRESH_APPS")
        ContextCompat.registerReceiver(
            appContext,
            appRefreshReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                val handler = Handler(Looper.getMainLooper())
                launcherApps.registerCallback(launcherAppsCallback, handler)
                Log.e("MainViewModel", "LauncherApps callback registered successfully")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to register LauncherApps callback", e)
            }
        }

        viewModelScope.launch {
            appReloadRequests
                .debounce(250L)
                .collectLatest { request ->
                    Log.d(
                        "MainViewModel",
                        "Reloading apps. reason=${request.reason}, forceEmit=${request.forceEmit}"
                    )
                    appRepository.loadApps(forceEmit = request.forceEmit)
                    updatePrivateSpaceState()
                }
        }

        requestAppReload("initial", forceEmit = true)
    }

    private suspend fun rebuildSearchAliasIndex() {
        val settings = settingsRepository.settings.first()
        val mode = settings.searchAliasesMode
        val includePkg = settings.searchIncludePackageNames
        val tagsByApp = _appTags.value

        if (mode == SearchAliasUtils.Mode.OFF && !includePkg && tagsByApp.isEmpty()) {
            searchAliasIndex = emptyMap()
            return
        }

        val idx = HashMap<String, Set<String>>(_appListAll.value.size)
        for (app in _appListAll.value) {
            val aliases = SearchAliasUtils.buildAppAliases(
                label = app.appLabel,
                packageName = app.appPackage,
                mode = mode,
                includePkg = includePkg
            ).toMutableSet()
            tagsByApp[app.getKey()].orEmpty().forEach { tag ->
                aliases += SearchAliasUtils.buildSearchTerms(tag, mode)
            }
            idx[app.getKey()] = aliases
        }
        searchAliasIndex = idx
    }

    /**
     * Load icons for all apps in the home layout
     */
    private suspend fun loadIconsForHomeLayout(layout: HomeLayout, settings: AppSettings): HomeLayout {
        if (!settings.showHomeScreenIcons) {
            return layout // Don't load icons if they're not shown
        }

        val iconCache = IconCache(appContext)

        val updatedItems = layout.items.map { item ->
            when (item) {
                is HomeItem.App -> {
                    val resolvedUser = getUserHandleFromString(appContext, item.appModel.userString)
                    val icon = if (item.appModel.isSystemShortcut &&
                        item.appModel.systemShortcutId != null &&
                        item.appModel.systemShortcutPackage != null &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
                    ) {
                        val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                        if (!launcherApps.hasShortcutHostPermission()) {
                            null
                        } else {
                            val query = LauncherApps.ShortcutQuery()
                                .setPackage(item.appModel.systemShortcutPackage)
                                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
                            val shortcut = launcherApps.getShortcuts(query, resolvedUser)
                                .orEmpty()
                                .firstOrNull { it.id == item.appModel.systemShortcutId }

                            val iconDrawable = shortcut?.let {
                                launcherApps.getShortcutIconDrawable(it, appContext.resources.displayMetrics.densityDpi)
                            }
                            BitmapUtils.drawableToBitmap(iconDrawable)?.asImageBitmap()
                        }
                    } else {
                        iconCache.getIcon(
                            packageName = item.appModel.appPackage,
                            className = item.appModel.activityClassName,
                            user = resolvedUser,
                            iconPackName = settings.selectedIconPack,
                        )
                    }
                    val updatedAppModel = item.appModel.copy(appIcon = icon)
                    item.copy(appModel = updatedAppModel)
                }
                is HomeItem.Widget -> item
                is HomeItem.Folder -> item
            }
        }

        return layout.copy(items = updatedItems)
    }

    private suspend fun refreshHomeScreenAppIcons() {
        val currentLayout = _homeLayoutState.value
        val settings = settingsRepository.settings.first()
        val iconCache = IconCache(appContext)

        val updatedItems = currentLayout.items.map { item ->
            when (item) {
                is HomeItem.App -> {
                    val updatedIcon = if (settings.showHomeScreenIcons) {
                        if (item.appModel.isSystemShortcut &&
                            item.appModel.systemShortcutId != null &&
                            item.appModel.systemShortcutPackage != null &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
                        ) {
                            val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                            if (!launcherApps.hasShortcutHostPermission()) {
                                null
                            } else {
                                val query = LauncherApps.ShortcutQuery()
                                    .setPackage(item.appModel.systemShortcutPackage)
                                    .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
                                val shortcut = launcherApps.getShortcuts(query, item.appModel.user)
                                    .orEmpty()
                                    .firstOrNull { it.id == item.appModel.systemShortcutId }

                                val iconDrawable = shortcut?.let {
                                    launcherApps.getShortcutIconDrawable(it, appContext.resources.displayMetrics.densityDpi)
                                }
                                BitmapUtils.drawableToBitmap(iconDrawable)?.asImageBitmap()
                            }
                        } else {
                            iconCache.getIcon(
                                packageName = item.appModel.appPackage,
                                className = item.appModel.activityClassName,
                                user = item.appModel.user,
                                iconPackName = settings.selectedIconPack,
                            )
                        }
                    } else {
                        null
                    }
                    val updatedAppModel = item.appModel.copy(appIcon = updatedIcon)
                    item.copy(appModel = updatedAppModel)
                }
                is HomeItem.Widget -> item
                is HomeItem.Folder -> item
            }
        }

        val updatedLayout = currentLayout.copy(items = updatedItems)
        _homeLayoutState.value = updatedLayout
        settingsRepository.saveHomeLayout(updatedLayout)
    }

    suspend fun updateGridSize(newRows: Int, newColumns: Int) {
        val currentLayout = _homeLayoutState.value

        val itemsOutOfBounds = currentLayout.items.filter { item ->
            item.row + item.rowSpan > newRows || item.column + item.columnSpan > newColumns
        }

        if (itemsOutOfBounds.isNotEmpty()) {
            val updatedItems = currentLayout.items.map { item ->
                if (item.row + item.rowSpan > newRows || item.column + item.columnSpan > newColumns) {
                    val newPosition = findNextAvailableGridPosition(
                        currentLayout.copy(rows = newRows, columns = newColumns),
                        item.columnSpan,
                        item.rowSpan
                    )
                    when (item) {
                        is HomeItem.App -> item.copy(
                            row = newPosition?.first ?: 0,
                            column = newPosition?.second ?: 0
                        )
                        is HomeItem.Widget -> item.copy(
                            row = newPosition?.first ?: 0,
                            column = newPosition?.second ?: 0
                        )
                        is HomeItem.Folder -> item.copy(
                            row = newPosition?.first ?: 0,
                            column = newPosition?.second ?: 0
                        )
                    }
                } else item
            }

            val newLayout = currentLayout.copy(
                items = updatedItems,
                rows = newRows,
                columns = newColumns
            )
            settingsRepository.saveHomeLayout(newLayout)
        } else {
            val newLayout = currentLayout.copy(rows = newRows, columns = newColumns)
            settingsRepository.saveHomeLayout(newLayout)
        }
    }

    private fun resolveEffectiveHomeOrientation(
        settings: AppSettings,
        requestedOrientation: HomeOrientation = _activeHomeOrientation.value
    ): HomeOrientation {
        return when (settings.screenOrientation) {
            1 -> HomeOrientation.PORTRAIT
            2 -> HomeOrientation.LANDSCAPE
            else -> if (requestedOrientation == HomeOrientation.LANDSCAPE && settings.isLandscapeHomeAvailable()) {
                HomeOrientation.LANDSCAPE
            } else {
                HomeOrientation.PORTRAIT
            }
        }
    }

    fun updateActiveHomeOrientation(isLandscape: Boolean) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val requested = if (isLandscape) HomeOrientation.LANDSCAPE else HomeOrientation.PORTRAIT
            val effective = resolveEffectiveHomeOrientation(settings, requested)
            settingsRepository.setActiveHomeOrientation(effective)
            _activeHomeOrientation.value = effective
        }
    }

    private suspend fun loadLayoutForOrientation(orientation: HomeOrientation): HomeLayout {
        val settings = settingsRepository.settings.first()
        return settings.homeLayouts.layoutFor(orientation).copy(
            rows = settings.homeRowsFor(orientation),
            columns = settings.homeColumnsFor(orientation),
            pageCount = settings.homePagesFor(orientation)
        )
    }

    private suspend fun saveLayoutForOrientation(orientation: HomeOrientation, layout: HomeLayout) {
        settingsRepository.saveHomeLayout(orientation, layout)
    }

    private fun HomeItem.Folder.hiddenCopy(): HomeItem.Folder =
        copy(showOnHome = false)

    private suspend fun updateFolderContentsAcrossOrientations(
        folderId: String,
        transform: (HomeItem.Folder) -> HomeItem.Folder
    ) {
        settingsRepository.updateHomeLayouts { _, layouts ->
            val baseFolder =
                layouts.portrait.items.filterIsInstance<HomeItem.Folder>().find { it.id == folderId }
                    ?: layouts.landscape.items.filterIsInstance<HomeItem.Folder>().find { it.id == folderId }
                    ?: return@updateHomeLayouts layouts

            val updatedBase = transform(baseFolder)

            fun update(layout: HomeLayout): HomeLayout {
                var found = false
                val updatedItems = layout.items.map { item ->
                    if (item is HomeItem.Folder && item.id == folderId) {
                        found = true
                        transform(item)
                    } else {
                        item
                    }
                }
                return if (found) {
                    layout.copy(items = updatedItems)
                } else {
                    layout.copy(items = updatedItems + updatedBase.hiddenCopy())
                }
            }

            layouts.copy(
                portrait = update(layouts.portrait),
                landscape = update(layouts.landscape)
            )
        }
    }

    private suspend fun updateFolderInOrientation(
        folderId: String,
        orientation: HomeOrientation,
        transform: (HomeItem.Folder) -> HomeItem.Folder
    ) {
        settingsRepository.updateHomeLayouts { _, layouts ->
            val sourceFolder =
                layouts.layoutFor(orientation).items.filterIsInstance<HomeItem.Folder>().find { it.id == folderId }
                    ?: layouts.layoutFor(
                        if (orientation == HomeOrientation.PORTRAIT) HomeOrientation.LANDSCAPE else HomeOrientation.PORTRAIT
                    ).items.filterIsInstance<HomeItem.Folder>().find { it.id == folderId }
                    ?: return@updateHomeLayouts layouts

            val targetLayout = layouts.layoutFor(orientation)
            var found = false
            val updatedItems = targetLayout.items.map { item ->
                if (item is HomeItem.Folder && item.id == folderId) {
                    found = true
                    transform(item)
                } else {
                    item
                }
            }
            val folderToInsert = transform(sourceFolder.hiddenCopy())
            val updatedLayout = if (found) {
                targetLayout.copy(items = updatedItems)
            } else {
                targetLayout.copy(items = updatedItems + folderToInsert)
            }
            layouts.withLayout(orientation, updatedLayout)
        }
    }

    private suspend fun removeFolderAcrossOrientations(folderId: String) {
        settingsRepository.updateHomeLayouts { _, layouts ->
            fun remove(layout: HomeLayout): HomeLayout =
                layout.copy(items = layout.items.filterNot { it is HomeItem.Folder && it.id == folderId })

            layouts.copy(
                portrait = remove(layouts.portrait),
                landscape = remove(layouts.landscape)
            )
        }
    }

    fun getAllFolders(): List<HomeItem.Folder> {
        val layouts = _settingsSnapshot.value.homeLayouts
        val active = _activeHomeOrientation.value
        val merged = linkedMapOf<String, HomeItem.Folder>()
        layouts.layoutFor(HomeOrientation.PORTRAIT).items.filterIsInstance<HomeItem.Folder>().forEach { merged[it.id] = it }
        layouts.layoutFor(HomeOrientation.LANDSCAPE).items.filterIsInstance<HomeItem.Folder>().forEach { folder ->
            if (merged[folder.id] == null || active == HomeOrientation.LANDSCAPE) {
                merged[folder.id] = folder
            }
        }
        return merged.values.toList()
    }

    fun getFolder(folderId: String, orientation: HomeOrientation): HomeItem.Folder? =
        _settingsSnapshot.value.homeLayouts
            .layoutFor(orientation)
            .items
            .filterIsInstance<HomeItem.Folder>()
            .find { it.id == folderId }

    fun availableHomeOrientations(): List<HomeOrientation> =
        _settingsSnapshot.value.availableHomeOrientations()

    fun addAppToHomeScreen(
        appModel: AppModel,
        orientation: HomeOrientation = _activeHomeOrientation.value,
        targetPage: Int? = null
    ) {
        viewModelScope.launch {
            Log.d("HomeScreen", "Attempting to add app: ${appModel.appLabel}")
            val currentLayout = if (orientation == _activeHomeOrientation.value) _homeLayoutState.value else loadLayoutForOrientation(orientation)
            val page = targetPage ?: _currentPage.value

            val nextPos = findNextAvailableGridPosition(currentLayout, 1, 1, page)

            if (nextPos != null) {
                val settings = settingsRepository.settings.first()
                val appModelWithUserString = appModel.copy(userString = appModel.userString)
                val appItem = HomeItem.App(
                    appModel = appModelWithUserString,
                    iconPlacement = if (appModel.isSystemShortcut) {
                        settings.shortcutIconPlacement
                    } else {
                        Constants.IconPlacement.LEFT
                    },
                    labelFontPath = settings.homeLabelFontPath,
                    page = page,
                    row = nextPos.first,
                    column = nextPos.second
                )
                val existingItem = currentLayout.items.find {
                    it is HomeItem.App && it.appModel.getKey() == appModel.getKey()
                }
                if (existingItem == null) {
                    val newItems = currentLayout.items + appItem
                    saveLayoutForOrientation(orientation, currentLayout.copy(items = newItems))
                }
            } else {
                if (page < currentLayout.pageCount - 1) {
                    addAppToHomeScreen(appModel, orientation, page + 1)
                } else if (currentLayout.pageCount < MAX_PAGES) {
                    // Adds a new page
                    val newLayout = currentLayout.copy(pageCount = currentLayout.pageCount + 1)
                    saveLayoutForOrientation(orientation, newLayout)
                    addAppToHomeScreen(appModel, orientation, currentLayout.pageCount)
                } else {
                    snackbarManager.show("No space available on any home screen page.")
                }
            }
        }
    }


    fun toggleAppInPrivateSpace(app: AppModel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            viewModelScope.launch {
                try {
                    val privateSpaceHelper = PrivateSpaceHelper(appContext)

                    if (!privateSpaceHelper.isPrivateSpaceSupported()) {
                        snackbarManager.show("Private Space requires Android 15 or higher")
                        return@launch
                    }

                    if (!privateSpaceHelper.isPrivateSpaceSetUp()) {
                        snackbarManager.show("Private Space is not set up on this device. Set it up in Android Settings.")
                        return@launch
                    }

                    if (privateSpaceHelper.isPrivateSpaceLocked()) {
                        snackbarManager.show("Private Space is locked. Please unlock it first.")
                        return@launch
                    }

                    val isInPrivateSpace = privateSpaceHelper.isPrivateSpaceProfile(app.user)
                    val message = if (isInPrivateSpace) {
                        "To remove apps from Private Space, use Android Settings > Private Space settings"
                    } else {
                        "To add apps to Private Space, use Android Settings > Private Space settings"
                    }

                    snackbarManager.show(message)

                    loadApps()
                    updatePrivateSpaceState()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error with Private Space operation", e)
                    snackbarManager.show("Private Space operation failed: ${e.message}")
                }
            }
        } else {
            snackbarManager.show("Private Space requires Android 15 or higher")
        }
    }

    fun removeAppFromHomeScreen(appItem: HomeItem.App) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value
            val newItems = currentLayout.items.filterNot { it.id == appItem.id }
            settingsRepository.saveHomeLayout(currentLayout.copy(items = newItems))
        }
    }

    // ─── Folder operations ────────────────────────────────────────────────────

    fun getFolders(): List<HomeItem.Folder> =
        getAllFolders()

    fun addFolderToHomeScreen(
        title: String,
        orientation: HomeOrientation = _activeHomeOrientation.value,
        targetPage: Int? = null
    ) {
        viewModelScope.launch {
            val currentLayout = if (orientation == _activeHomeOrientation.value) _homeLayoutState.value else loadLayoutForOrientation(orientation)
            val page = targetPage ?: _currentPage.value
            val nextPos = findNextAvailableGridPosition(currentLayout, 1, 1, page)
            if (nextPos != null) {
                val settings = settingsRepository.settings.first()
                val folder = HomeItem.Folder(
                    title = title.ifBlank { "Folder" },
                    iconPlacement = settings.folderIconPlacement,
                    titleFontPath = settings.folderLabelFontPath,
                    page = page,
                    row = nextPos.first,
                    column = nextPos.second,
                )
                saveLayoutForOrientation(orientation, currentLayout.copy(items = currentLayout.items + folder))
                val otherOrientation = if (orientation == HomeOrientation.PORTRAIT) {
                    HomeOrientation.LANDSCAPE
                } else {
                    HomeOrientation.PORTRAIT
                }
                updateFolderInOrientation(folder.id, otherOrientation) { existing ->
                    existing.copy(title = folder.title, apps = folder.apps, gridRows = folder.gridRows, gridColumns = folder.gridColumns, appTextSize = folder.appTextSize)
                }
            } else {
                if (page < currentLayout.pageCount - 1) {
                    addFolderToHomeScreen(title, orientation, page + 1)
                } else if (currentLayout.pageCount < MAX_PAGES) {
                    val newLayout = currentLayout.copy(pageCount = currentLayout.pageCount + 1)
                    saveLayoutForOrientation(orientation, newLayout)
                    addFolderToHomeScreen(title, orientation, currentLayout.pageCount)
                } else {
                    snackbarManager.show("No space available on any home screen page.")
                }
            }
        }
    }

    fun addAppToFolder(folderId: String, appModel: AppModel) {
        viewModelScope.launch {
            // Check the active orientation first; abort if it has no room.
            val activeFolder = _homeLayoutState.value.items
                .filterIsInstance<HomeItem.Folder>().find { it.id == folderId }
                ?: getAllFolders().find { it.id == folderId }
                ?: return@launch
            if (findNextAvailablePositionInFolder(activeFolder) == null) {
                snackbarManager.show("No space available in this folder.")
                return@launch
            }
            val iconPlacement = if (appModel.isSystemShortcut)
                _settingsSnapshot.value.shortcutIconPlacement
            else Constants.IconPlacement.LEFT

            // Add independently to each orientation so each layout gets its own position.
            settingsRepository.updateHomeLayouts { _, layouts ->
                fun addToLayout(layout: HomeLayout): HomeLayout {
                    val folder = layout.items.filterIsInstance<HomeItem.Folder>()
                        .find { it.id == folderId } ?: return layout
                    val pos = findNextAvailablePositionInFolder(folder) ?: Pair(0, 0)
                    val newApp = appModel.toFolderApp(
                        row = pos.first, column = pos.second,
                        iconPlacement = iconPlacement, labelFontPath = "",
                    )
                    val updatedFolder = folder.copy(apps = folder.apps + newApp)
                    return layout.copy(items = layout.items.map { if (it.id == folderId) updatedFolder else it })
                }
                layouts.copy(
                    portrait = addToLayout(layouts.portrait),
                    landscape = addToLayout(layouts.landscape),
                )
            }
            snackbarManager.show("Added \"${appModel.appLabel}\" to \"${activeFolder.title}\"")
        }
    }

    fun removeAppFromFolder(folderId: String, folderApp: FolderApp) {
        viewModelScope.launch {
            // Remove by identity so the correct entry is removed from both orientation copies
            // even when their position data has diverged.
            updateFolderContentsAcrossOrientations(folderId) { folder ->
                folder.copy(apps = folder.apps.filterNot { isSameFolderAppEntry(it, folderApp) })
            }
        }
    }

    fun moveFolderApp(folderId: String, folderApp: FolderApp, newRow: Int, newColumn: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value
            val folder = currentLayout.items.filterIsInstance<HomeItem.Folder>().find { it.id == folderId } ?: return@launch
            if (!validateFolderPlacement(folder, folderApp, newRow, newColumn, folderApp.rowSpan, folderApp.columnSpan)) {
                snackbarManager.show("Cannot place app there.")
                return@launch
            }
            var replaced = false
            val updatedApps = folder.apps.map {
                if (!replaced && isSameFolderAppEntry(it, folderApp)) {
                    replaced = true
                    it.copy(row = newRow, column = newColumn)
                } else {
                    it
                }
            }
            val updatedFolder = folder.copy(apps = updatedApps)
            val updatedItems = currentLayout.items.map { if (it.id == folderId) updatedFolder else it }
            settingsRepository.saveHomeLayout(currentLayout.copy(items = updatedItems))
        }
    }

    fun canMoveFolderApp(folderId: String, folderApp: FolderApp, newRow: Int, newColumn: Int): Boolean {
        val folder = _homeLayoutState.value.items
            .filterIsInstance<HomeItem.Folder>()
            .find { it.id == folderId }
            ?: return false

        return validateFolderPlacement(folder, folderApp, newRow, newColumn, folderApp.rowSpan, folderApp.columnSpan)
    }

    fun renameFolder(folderId: String, newTitle: String) {
        viewModelScope.launch {
            updateFolderContentsAcrossOrientations(folderId) { it.copy(title = newTitle.ifBlank { "Folder" }) }
        }
    }

    fun removeFolder(folderItem: HomeItem.Folder) {
        viewModelScope.launch {
            removeFolderAcrossOrientations(folderItem.id)
        }
    }

    /**
     * Returns true if the folder is already visible on home for [orientation], or if there is at
     * least one free 1×1 cell across any page of that orientation's layout.
     */
    fun hasFreeSpaceForFolder(folderId: String, orientation: HomeOrientation): Boolean {
        val settings = _settingsSnapshot.value
        val layout = settings.homeLayouts.layoutFor(orientation).copy(
            rows = settings.homeRowsFor(orientation),
            columns = settings.homeColumnsFor(orientation),
            pageCount = settings.homePagesFor(orientation)
        )
        val folder = layout.items.filterIsInstance<HomeItem.Folder>().find { it.id == folderId }
        if (folder?.showOnHome == true) return true
        return (0 until layout.pageCount).any { page ->
            findNextAvailableGridPosition(layout, 1, 1, page) != null
        }
    }

    fun setFolderShowOnHome(
        folderId: String,
        show: Boolean,
        orientation: HomeOrientation = _activeHomeOrientation.value
    ) {
        viewModelScope.launch {
            val currentLayout = if (orientation == _activeHomeOrientation.value) _homeLayoutState.value else loadLayoutForOrientation(orientation)
            val folder = currentLayout.items.filterIsInstance<HomeItem.Folder>()
                .find { it.id == folderId }
                ?: getFolder(folderId, if (orientation == HomeOrientation.PORTRAIT) HomeOrientation.LANDSCAPE else HomeOrientation.PORTRAIT)
                    ?.hiddenCopy()
                ?: return@launch

            val updatedFolder = if (!show) {
                folder.copy(showOnHome = false)
            } else {
                // Always place as 1×1 — the hidden counterpart may carry a span from the other
                // orientation which is wrong for this layout.
                val targetPage = folder.page

                // Prefer the page the folder was last on, then scan remaining pages.
                val pageOrder = listOf(targetPage) +
                    (0 until currentLayout.pageCount).filter { it != targetPage }

                var placed: HomeItem.Folder? = null
                for (page in pageOrder) {
                    val pos = findNextAvailableGridPosition(currentLayout, 1, 1, page)
                    if (pos != null) {
                        placed = folder.copy(
                            showOnHome = true,
                            page = page,
                            row = pos.first,
                            column = pos.second,
                            rowSpan = 1,
                            columnSpan = 1
                        )
                        break
                    }
                }

                if (placed == null) {
                    snackbarManager.show("No available grid space")
                    return@launch
                }
                placed
            }

            val existingFolder = currentLayout.items.filterIsInstance<HomeItem.Folder>().any { it.id == folderId }
            val updatedItems = if (existingFolder) {
                currentLayout.items.map { if (it.id == folderId) updatedFolder else it }
            } else {
                currentLayout.items + updatedFolder
            }
            saveLayoutForOrientation(orientation, currentLayout.copy(items = updatedItems))
        }
    }

    private val _openFolderEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>(replay = 0)
    val openFolderEvent: kotlinx.coroutines.flow.SharedFlow<String> = _openFolderEvent.asSharedFlow()

    fun openFolderById(folderId: String) {
        if (folderId.isBlank()) return
        viewModelScope.launch { _openFolderEvent.emit(folderId) }
    }

    fun moveFolder(folderItem: HomeItem.Folder, newRow: Int, newColumn: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value
            if (!validateAndReport(currentLayout, folderItem.id, folderItem.page, newRow, newColumn, folderItem.rowSpan, folderItem.columnSpan, "move folder")) return@launch
            val updatedItems = currentLayout.items.map { item ->
                if (item.id == folderItem.id && item is HomeItem.Folder) item.copy(row = newRow, column = newColumn) else item
            }
            settingsRepository.saveHomeLayout(currentLayout.copy(items = updatedItems))
        }
    }

    fun resizeFolder(folderItem: HomeItem.Folder, newRowSpan: Int, newColSpan: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value
            if (!validateAndReport(currentLayout, folderItem.id, folderItem.page, folderItem.row, folderItem.column, newRowSpan, newColSpan, "resize folder")) return@launch
            val updatedItems = currentLayout.items.map { item ->
                if (item.id == folderItem.id && item is HomeItem.Folder) item.copy(rowSpan = newRowSpan, columnSpan = newColSpan) else item
            }
            settingsRepository.saveHomeLayout(currentLayout.copy(items = updatedItems))
        }
    }

    fun updateFolderGridSize(folderId: String, newRows: Int, newCols: Int) {
        viewModelScope.launch {
            val folder = getAllFolders().find { it.id == folderId } ?: return@launch
            val clampedRows = newRows.coerceIn(Constants.GridSize.MIN_ROWS, Constants.GridSize.MAX_ROWS)
            val clampedCols = newCols.coerceIn(Constants.GridSize.MIN_COLUMNS, Constants.GridSize.MAX_COLUMNS)

            val validApps = folder.apps.filter { app ->
                app.row + app.rowSpan <= clampedRows && app.column + app.columnSpan <= clampedCols
            }
            val outOfBoundsApps = folder.apps - validApps.toSet()
            val relocatedApps = mutableListOf<FolderApp>()

            for (app in outOfBoundsApps) {
                val tempFolder = folder.copy(apps = validApps + relocatedApps, gridRows = clampedRows, gridColumns = clampedCols)
                val pos = findNextAvailablePositionInFolder(tempFolder, app.columnSpan, app.rowSpan)
                if (pos != null) {
                    relocatedApps.add(app.copy(row = pos.first, column = pos.second))
                } else {
                    snackbarManager.show("Some apps could not fit in the new grid and were removed from the folder")
                }
            }

            val updatedFolder = folder.copy(gridRows = clampedRows, gridColumns = clampedCols, apps = validApps + relocatedApps)
            updateFolderContentsAcrossOrientations(folderId) { updatedFolder }
        }
    }

    fun updateFolderTitle(folderId: String, newTitle: String) = renameFolder(folderId, newTitle)

    fun updateFolderAppTextSize(folderId: String, textSize: Float) {
        viewModelScope.launch {
            updateFolderContentsAcrossOrientations(folderId) { it.copy(appTextSize = textSize) }
        }
    }

    fun updateFolderTitleTextSize(
        folderId: String,
        textSize: Float,
        orientation: HomeOrientation = _activeHomeOrientation.value
    ) {
        viewModelScope.launch {
            updateFolderInOrientation(folderId, orientation) { it.copy(titleTextSize = textSize) }
        }
    }

    fun updateFolderTitleColor(
        folderId: String,
        color: Int,
        orientation: HomeOrientation = _activeHomeOrientation.value
    ) {
        viewModelScope.launch {
            updateFolderInOrientation(folderId, orientation) { it.copy(titleTextColor = color) }
        }
    }

    fun setFolderHideTitle(
        folderId: String,
        hide: Boolean,
        orientation: HomeOrientation = _activeHomeOrientation.value
    ) {
        viewModelScope.launch {
            updateFolderInOrientation(folderId, orientation) { it.copy(hideTitle = hide) }
        }
    }

    fun setFolderHideCloseButton(
        folderId: String,
        hide: Boolean,
        orientation: HomeOrientation = _activeHomeOrientation.value
    ) {
        viewModelScope.launch {
            updateFolderInOrientation(folderId, orientation) { it.copy(hideCloseButton = hide) }
        }
    }

    fun setFolderHideOutline(
        folderId: String,
        hide: Boolean,
        orientation: HomeOrientation = _activeHomeOrientation.value
    ) {
        viewModelScope.launch {
            updateFolderInOrientation(folderId, orientation) { it.copy(hideOutline = hide) }
        }
    }

    fun setFolderTapOutsideToClose(
        folderId: String,
        enabled: Boolean,
        orientation: HomeOrientation = _activeHomeOrientation.value
    ) {
        viewModelScope.launch {
            updateFolderInOrientation(folderId, orientation) { it.copy(tapOutsideToClose = enabled) }
        }
    }

    fun updateHomeAppTextSize(appItem: HomeItem.App, textSize: Float) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value
            val updatedApp = appItem.copy(appTextSize = textSize)
            val updatedItems = currentLayout.items.map { if (it.id == appItem.id) updatedApp else it }
            settingsRepository.saveHomeLayout(currentLayout.copy(items = updatedItems))
        }
    }

    fun updateFolderAppIndividualTextSize(folderId: String, folderApp: FolderApp, textSize: Float) {
        viewModelScope.launch {
            updateFolderAppInActiveOrientation(folderId, folderApp) { it.copy(appTextSize = textSize) }
        }
    }

    fun updateHomeAppLabelAlignment(appItem: HomeItem.App, alignment: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value
            val updatedApp = appItem.copy(appLabelAlignment = alignment)
            val updatedItems = currentLayout.items.map { if (it.id == appItem.id) updatedApp else it }
            settingsRepository.saveHomeLayout(currentLayout.copy(items = updatedItems))
        }
    }

    fun updateHomeAppIconPlacement(appItem: HomeItem.App, placement: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value
            val updatedApp = appItem.copy(iconPlacement = placement)
            val updatedItems = currentLayout.items.map { if (it.id == appItem.id) updatedApp else it }
            settingsRepository.saveHomeLayout(currentLayout.copy(items = updatedItems))
        }
    }

    fun updateHomeAppLabelFont(appItem: HomeItem.App, labelFontPath: String) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value
            val updatedApp = appItem.copy(labelFontPath = labelFontPath)
            val updatedItems = currentLayout.items.map { if (it.id == appItem.id) updatedApp else it }
            settingsRepository.saveHomeLayout(currentLayout.copy(items = updatedItems))
        }
    }

    fun updateFolderTitleLabelAlignment(folderId: String, alignment: Int) {
        viewModelScope.launch {
            updateFolderInOrientation(folderId, _activeHomeOrientation.value) { it.copy(titleLabelAlignment = alignment) }
        }
    }

    fun updateFolderIconPlacement(
        folderId: String,
        placement: Int,
        orientation: HomeOrientation = _activeHomeOrientation.value
    ) {
        viewModelScope.launch {
            updateFolderInOrientation(folderId, orientation) { it.copy(iconPlacement = placement) }
        }
    }

    fun updateFolderTitleFont(
        folderId: String,
        titleFontPath: String,
        orientation: HomeOrientation = _activeHomeOrientation.value
    ) {
        viewModelScope.launch {
            updateFolderInOrientation(folderId, orientation) { it.copy(titleFontPath = titleFontPath) }
        }
    }

    fun updateFolderDefaultAppFont(
        folderId: String,
        defaultAppFontPath: String,
        orientation: HomeOrientation = _activeHomeOrientation.value
    ) {
        viewModelScope.launch {
            updateFolderInOrientation(folderId, orientation) { it.copy(defaultAppFontPath = defaultAppFontPath) }
        }
    }

    fun updateFolderAppIndividualLabelAlignment(folderId: String, folderApp: FolderApp, alignment: Int) {
        viewModelScope.launch {
            updateFolderAppInActiveOrientation(folderId, folderApp) { it.copy(appLabelAlignment = alignment) }
        }
    }

    fun updateFolderAppIconPlacement(folderId: String, folderApp: FolderApp, placement: Int) {
        viewModelScope.launch {
            updateFolderAppInActiveOrientation(folderId, folderApp) { it.copy(iconPlacement = placement) }
        }
    }

    fun updateFolderAppLabelFont(folderId: String, folderApp: FolderApp, labelFontPath: String) {
        viewModelScope.launch {
            updateFolderAppInActiveOrientation(folderId, folderApp) { it.copy(labelFontPath = labelFontPath) }
        }
    }

    fun resizeFolderApp(folderId: String, folderApp: FolderApp, newRowSpan: Int, newColSpan: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value
            val folder = currentLayout.items.filterIsInstance<HomeItem.Folder>()
                .find { it.id == folderId } ?: return@launch
            if (!validateFolderPlacement(folder, folderApp, folderApp.row, folderApp.column, newRowSpan, newColSpan)) {
                snackbarManager.show("Cannot resize app there.")
                return@launch
            }
            updateFolderAppInActiveOrientation(folderId, folderApp) { it.copy(rowSpan = newRowSpan, columnSpan = newColSpan) }
        }
    }

    private suspend fun updateFolderAppInActiveOrientation(
        folderId: String,
        folderApp: FolderApp,
        transform: (FolderApp) -> FolderApp,
    ) {
        val currentLayout = _homeLayoutState.value
        val folder = currentLayout.items.filterIsInstance<HomeItem.Folder>()
            .find { it.id == folderId } ?: return
        var found = false
        val updatedApps = folder.apps.map { app ->
            if (!found && isSameFolderAppEntry(app, folderApp)) {
                found = true; transform(app)
            } else app
        }
        if (!found) return
        val updatedFolder = folder.copy(apps = updatedApps)
        val updatedItems = currentLayout.items.map { if (it.id == folderId) updatedFolder else it }
        settingsRepository.saveHomeLayout(currentLayout.copy(items = updatedItems))
    }

    private fun findNextAvailablePositionInFolder(folder: HomeItem.Folder, widthSpan: Int = 1, heightSpan: Int = 1): Pair<Int, Int>? {
        val occupied = Array(folder.gridRows) { BooleanArray(folder.gridColumns) }
        folder.apps.forEach { app ->
            for (r in app.row until (app.row + app.rowSpan).coerceAtMost(folder.gridRows)) {
                for (c in app.column until (app.column + app.columnSpan).coerceAtMost(folder.gridColumns)) {
                    if (r >= 0 && c >= 0) occupied[r][c] = true
                }
            }
        }
        for (r in 0..folder.gridRows - heightSpan) {
            for (c in 0..folder.gridColumns - widthSpan) {
                if (isSpaceFreeInternal(occupied, r, c, widthSpan, heightSpan, folder.gridRows, folder.gridColumns)) {
                    return Pair(r, c)
                }
            }
        }
        return null
    }

    private fun isSameFolderAppEntry(left: FolderApp, right: FolderApp): Boolean {
        return left.appPackage == right.appPackage &&
            left.activityClassName == right.activityClassName &&
            left.userString == right.userString &&
            left.appLabel == right.appLabel &&
            left.isSystemShortcut == right.isSystemShortcut &&
            left.systemShortcutId == right.systemShortcutId &&
            left.systemShortcutPackage == right.systemShortcutPackage
    }

    private fun validateFolderPlacement(folder: HomeItem.Folder, movingApp: FolderApp, newRow: Int, newCol: Int, rowSpan: Int, colSpan: Int): Boolean {
        if (newRow < 0 || newCol < 0) return false
        if (newRow + rowSpan > folder.gridRows || newCol + colSpan > folder.gridColumns) return false
        var skippedMovingApp = false
        val hasOverlap = folder.apps.any { app ->
            if (!skippedMovingApp && isSameFolderAppEntry(app, movingApp)) {
                skippedMovingApp = true
                return@any false
            }
            !(newRow >= app.row + app.rowSpan || newRow + rowSpan <= app.row ||
                newCol >= app.column + app.columnSpan || newCol + colSpan <= app.column)
        }
        return !hasOverlap
    }

    private fun getCellSizeDp(screenWidthDp: Int, screenHeightDp: Int, rows: Int, columns: Int): Pair<Float, Float> {
        val cellWidthDp = screenWidthDp.toFloat() / columns
        val cellHeightDp = screenHeightDp.toFloat() / rows
        return Pair(cellWidthDp, cellHeightDp)
    }

    suspend fun launchAppInternal(app: AppModel) {
        if (app.isSystemShortcut) {
            try {
                val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                if (app.systemShortcutId != null && app.systemShortcutPackage != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                        launcherApps.startShortcut(
                            app.systemShortcutPackage,
                            app.systemShortcutId,
                            null,
                            null,
                            app.user
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error launching system shortcut", e)
                snackbarManager.show("Failed to open shortcut: ${e.message}")
            }
        } else {
            appRepository.launchApp(app)
        }
        settingsRepository.updateAppLaunchTime(app.getKey())

        val legacyMoveKeys = AppKey.legacyMoveKeysForApp(app)
        val legacyCopyKeys = AppKey.legacyCopyKeysForApp(app)
        if (legacyMoveKeys.isNotEmpty() || legacyCopyKeys.isNotEmpty()) {
            val settings = settingsRepository.settings.first()
            val appTags = AppTagStorage.decode(settings.appTagsJson)
            val appKey = app.getKey()
            val hasNewRename = settings.renamedApps.containsKey(appKey)
            val hasNewHidden = settings.hiddenApps.contains(appKey)
            val newHistory = settings.recentAppHistory[appKey]
            val hasNewTags = appTags.containsKey(appKey)

            val legacyRename = legacyCopyKeys.firstNotNullOfOrNull { settings.renamedApps[it] }
            val legacyHidden = legacyCopyKeys.any { settings.hiddenApps.contains(it) }
            val legacyHistory = legacyCopyKeys.mapNotNull { settings.recentAppHistory[it] }.maxOrNull()
            val legacyTags = legacyCopyKeys.flatMap { appTags[it].orEmpty() }

            val shouldCopy = (!hasNewRename && legacyRename != null) ||
                (!hasNewHidden && legacyHidden) ||
                (legacyHistory != null && (newHistory == null || legacyHistory > newHistory)) ||
                (!hasNewTags && legacyTags.isNotEmpty())

            val copyKeys = if (shouldCopy) legacyCopyKeys else emptySet()

            settingsRepository.migrateAppKeys(
                listOf(
                    AppKeyMigration(
                        newKey = appKey,
                        moveKeys = legacyMoveKeys,
                        copyKeys = copyKeys
                    )
                )
            )
        }
    }


    private fun findNextAvailableGridPosition(
        layout: HomeLayout,
        widthSpan: Int,
        heightSpan: Int,
        page: Int = 0,
    ): Pair<Int, Int>? {
        val occupied = Array(layout.rows) { BooleanArray(layout.columns) }

        // Hidden folders are transparent — they don't occupy grid space.
        layout.itemsForPage(page)
            .filter { item -> item !is HomeItem.Folder || item.showOnHome }
            .forEach { item ->
                for (r in item.row until (item.row + item.rowSpan).coerceAtMost(layout.rows)) {
                    for (c in item.column until (item.column + item.columnSpan).coerceAtMost(layout.columns)) {
                        if (r >= 0 && c >= 0) occupied[r][c] = true
                    }
                }
            }

        for (r in 0..layout.rows - heightSpan) {
            for (c in 0..layout.columns - widthSpan) {
                if (isSpaceFreeInternal(occupied, r, c, widthSpan, heightSpan, layout.rows, layout.columns)) {
                    return Pair(r, c)
                }
            }
        }
        return null
    }

    private fun isSpaceFreeInternal(occupiedGrid: Array<BooleanArray>, startRow: Int, startCol: Int, spanW: Int, spanH: Int, maxRows: Int, maxCols: Int): Boolean {
        for (r in startRow until startRow + spanH) {
            for (c in startCol until startCol + spanW) {
                if (r >= maxRows || c >= maxCols || occupiedGrid[r][c]) return false
            }
        }
        return true
    }

    private fun updateAppDrawerState() {
        _appDrawerState.value = _appDrawerState.value.copy(
            apps = _appList.value,
            isLoading = false
        )
    }

    fun startWidgetConfiguration(providerInfo: android.appwidget.AppWidgetProviderInfo) {
        viewModelScope.launch {
            try {
                @Suppress("SENSELESS_COMPARISON")
                if (providerInfo == null) {
                    Log.e("WidgetDebug", "CRITICAL: providerInfo is NULL in startWidgetConfiguration")
                    snackbarManager.show("Internal error: Widget provider information missing.")
                    return@launch
                }

                val componentName = providerInfo.provider
                if (componentName == null) {
                    Log.e("WidgetDebug", "CRITICAL: providerInfo.provider is NULL")
                    snackbarManager.show("Internal error: Widget component name missing.")
                    return@launch
                }

                val appWidgetId = appWidgetHost.allocateAppWidgetId()
                val bindSuccess = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, componentName)

                if (bindSuccess) {
                    if (providerInfo.configure != null) {
                        pendingWidgetInfo = PendingWidgetInfo(appWidgetId, providerInfo)
                        emitEvent(UiEvent.ConfigureWidget(appWidgetId))
                    } else {
                        // No configuration needed — add immediately
                        addWidgetToLayout(appWidgetId, providerInfo)
                    }
                } else {
                    pendingWidgetInfo = PendingWidgetInfo(appWidgetId, providerInfo)
                    val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, componentName)
                    }
                    emitEvent(UiEvent.LaunchWidgetBindIntent(bindIntent))
                }
            } catch (e: Exception) {
                Log.e("WidgetDebug", "Error in startWidgetConfiguration", e)
                snackbarManager.show("Failed to start widget configuration: ${e.message}")
            }
        }
    }

    private fun addWidgetToLayout(appWidgetId: Int, providerInfo: android.appwidget.AppWidgetProviderInfo) {
        viewModelScope.launch {
            try {
                val screenDimensions = getScreenDimensions(context = appContext)
                val screenWidthDp = screenDimensions.first
                val screenHeightDp = screenDimensions.second

                val currentLayout = _homeLayoutState.value
                val cellWidthDp = screenWidthDp / currentLayout.columns
                val cellHeightDp = screenHeightDp / currentLayout.rows

                val widgetWidthCells = 1.coerceAtLeast(ceil(providerInfo.minWidth.toDouble() / cellWidthDp).toInt())
                val widgetHeightCells = 1.coerceAtLeast(ceil(providerInfo.minHeight.toDouble() / cellHeightDp).toInt())

                val replacementTarget = pendingWidgetReplacementTarget
                val layoutWithoutReplacement = replacementTarget?.let { target ->
                    currentLayout.copy(items = currentLayout.items.filterNot { it.id == target.id })
                } ?: currentLayout

                var targetPage = replacementTarget?.page ?: _currentPage.value
                var nextPos = replacementTarget
                    ?.takeIf {
                        validatePlacement(
                            layout = layoutWithoutReplacement,
                            itemId = it.id,
                            page = it.page,
                            row = it.row,
                            column = it.column,
                            rowSpan = widgetHeightCells,
                            columnSpan = widgetWidthCells,
                        ) is PlacementResult.Valid
                    }
                    ?.let { it.row to it.column }
                    ?: findNextAvailableGridPosition(layoutWithoutReplacement, widgetWidthCells, widgetHeightCells, targetPage)

                if (nextPos == null) {
                    for (page in 0 until layoutWithoutReplacement.pageCount) {
                        if (page != targetPage) {
                            nextPos = findNextAvailableGridPosition(layoutWithoutReplacement, widgetWidthCells, widgetHeightCells, page)
                            if (nextPos != null) {
                                targetPage = page
                                break
                            }
                        }
                    }
                }

                if (nextPos == null && layoutWithoutReplacement.pageCount < MAX_PAGES) {
                    targetPage = layoutWithoutReplacement.pageCount
                    val expandedLayout = layoutWithoutReplacement.copy(pageCount = layoutWithoutReplacement.pageCount + 1)
                    settingsRepository.saveHomeLayout(expandedLayout)
                    nextPos = Pair(0, 0)
                }

                if (nextPos != null) {
                    val widgetItem = HomeItem.Widget(
                        id = UUID.randomUUID().toString(),
                        appWidgetId = appWidgetId,
                        packageName = providerInfo.provider.packageName,
                        providerClassName = providerInfo.provider.className,
                        page = targetPage,
                        row = nextPos.first,
                        column = nextPos.second,
                        rowSpan = widgetHeightCells,
                        columnSpan = widgetWidthCells
                    )
                    val newItems = layoutWithoutReplacement.items + widgetItem
                    val newLayout = layoutWithoutReplacement.copy(items = newItems)
                    _homeLayoutState.value = newLayout
                    settingsRepository.saveHomeLayout(newLayout)

                    val options = Bundle().apply {
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, providerInfo.minWidth)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, providerInfo.minWidth)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, providerInfo.minHeight)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, providerInfo.minHeight)
                    }
                    appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
                    settingsRepository.triggerHomeLayoutRefresh()

                    _currentPage.value = targetPage
                    pendingWidgetReplacementTarget = null
                    _widgetReplacementTarget.value = null
                } else {
                    snackbarManager.show("No space available for widget on any home screen page.")
                    appWidgetHost.deleteAppWidgetId(appWidgetId)
                    pendingWidgetReplacementTarget = null
                    _widgetReplacementTarget.value = null
                }
            } catch (e: Exception) {
                Log.e("WidgetDebug", "Error adding widget to layout", e)
                snackbarManager.show("Failed to add widget: ${e.message}")
                try {
                    appWidgetHost.deleteAppWidgetId(appWidgetId)
                } catch (e2: Exception) {
                    Log.e("WidgetDebug", "Error cleaning up widget ID", e2)
                }
                pendingWidgetReplacementTarget = null
                _widgetReplacementTarget.value = null
            }
        }
    }

    private fun checkResizeValidity(layout: HomeLayout, widgetToResize: HomeItem.Widget, newRowSpan: Int, newColSpan: Int): Boolean {
        val targetRow = widgetToResize.row
        val targetCol = widgetToResize.column

        if (targetRow + newRowSpan > layout.rows || targetCol + newColSpan > layout.columns) return false

        for (item in layout.items) {
            if (item.id == widgetToResize.id) continue
            val horizontalOverlap = (item.column < targetCol + newColSpan) && (item.column + item.columnSpan > targetCol)
            val verticalOverlap = (item.row < targetRow + newRowSpan) && (item.row + item.rowSpan > targetRow)
            if (horizontalOverlap && verticalOverlap) return false
        }
        return true
    }

    fun moveItemToPage(item: HomeItem, targetPage: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value

            val newPageCount = if (targetPage >= currentLayout.pageCount) {
                (targetPage + 1).coerceAtMost(MAX_PAGES)
            } else {
                currentLayout.pageCount
            }

            if (targetPage >= newPageCount) {
                snackbarManager.show("Cannot move: maximum pages reached")
                return@launch
            }

            val workingLayout = currentLayout.copy(pageCount = newPageCount)
            val nextPos = findNextAvailableGridPosition(
                workingLayout,
                item.columnSpan,
                item.rowSpan,
                targetPage
            )

            if (nextPos == null) {
                snackbarManager.show("No space available on page ${targetPage + 1}")
                return@launch
            }

            val updatedItems = currentLayout.items.map { existingItem ->
                if (existingItem.id == item.id) {
                    when (existingItem) {
                        is HomeItem.App -> existingItem.copy(
                            page = targetPage,
                            row = nextPos.first,
                            column = nextPos.second
                        )
                        is HomeItem.Widget -> existingItem.copy(
                            page = targetPage,
                            row = nextPos.first,
                            column = nextPos.second
                        )
                        is HomeItem.Folder -> existingItem.copy(
                            page = targetPage,
                            row = nextPos.first,
                            column = nextPos.second
                        )
                    }
                } else existingItem
            }

            val newLayout = workingLayout.copy(items = updatedItems)
            settingsRepository.saveHomeLayout(newLayout)
            _currentPage.value = targetPage
        }
    }

    fun renameApp(app: AppModel, newName: String) {
        viewModelScope.launch {
            val trimmedName = newName.trim()
            val appKey = app.getKey()
            val defaultLabel = appRepository.getDefaultAppLabel(app)
            val shouldClear = trimmedName.isBlank() || (defaultLabel != null && trimmedName == defaultLabel)

            val legacyMoveKeys = AppKey.legacyMoveKeysForApp(app)
            val legacyCopyKeys = AppKey.legacyCopyKeysForApp(app)
            val legacyAllKeys = legacyMoveKeys + legacyCopyKeys

            if (shouldClear) {
                settingsRepository.removeAppCustomNames(setOf(appKey) + legacyAllKeys)
            } else {
                settingsRepository.setAppCustomName(appKey, trimmedName)
            }
            val migrations = buildList {
                if (legacyMoveKeys.isNotEmpty() || legacyCopyKeys.isNotEmpty()) {
                    add(
                        AppKeyMigration(
                            newKey = appKey,
                            moveKeys = legacyMoveKeys,
                            copyKeys = legacyCopyKeys
                        )
                    )
                }
            }
            settingsRepository.migrateAppKeys(migrations)

            val targetLabel = if (shouldClear) defaultLabel ?: app.appLabel else trimmedName
            syncAppLabelAcrossHomeLayout(app, targetLabel)
            loadApps()
        }
    }

    private suspend fun syncAppLabelAcrossHomeLayout(app: AppModel, targetLabel: String) {
        val targetKey = app.getKey()
        val currentLayout = _homeLayoutState.value
        var changed = false

        val updatedItems = currentLayout.items.map { item ->
            when (item) {
                is HomeItem.App -> {
                    if (item.appModel.getKey() == targetKey && item.appModel.appLabel != targetLabel) {
                        changed = true
                        item.copy(appModel = item.appModel.copy(appLabel = targetLabel))
                    } else {
                        item
                    }
                }
                is HomeItem.Folder -> {
                    var folderChanged = false
                    val updatedApps = item.apps.map { folderApp ->
                        if (folderApp.toAppModel().getKey() == targetKey && folderApp.appLabel != targetLabel) {
                            folderChanged = true
                            folderApp.copy(appLabel = targetLabel)
                        } else {
                            folderApp
                        }
                    }
                    if (folderChanged) {
                        changed = true
                        item.copy(apps = updatedApps)
                    } else {
                        item
                    }
                }
                is HomeItem.Widget -> item
            }
        }

        if (changed) {
            val updatedLayout = currentLayout.copy(items = updatedItems)
            _homeLayoutState.value = updatedLayout
            settingsRepository.saveHomeLayout(updatedLayout)
        }
    }

    fun removeWidget(widgetItem: HomeItem.Widget) {
        viewModelScope.launch {
            try {
                val currentLayout = _homeLayoutState.value
                val newItems = currentLayout.items.filterNot { it.id == widgetItem.id }
                val newLayout = currentLayout.copy(items = newItems)
                _homeLayoutState.value = newLayout
                if (!widgetItem.isPlaceholder && widgetItem.appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    appWidgetHost.deleteAppWidgetId(widgetItem.appWidgetId)
                }
                settingsRepository.saveHomeLayout(newLayout)
            } catch (e: Exception) {
                Log.e("ViewModelWidget", "Error deleting widget ID ${widgetItem.appWidgetId}", e)
                snackbarManager.show("Failed to remove widget.")
            }
        }
    }

    fun requestWidgetReconfigure(widgetItem: HomeItem.Widget) {
        viewModelScope.launch {
            if (widgetItem.isPlaceholder) {
                snackbarManager.show("Placeholder widgets cannot be configured.")
                return@launch
            }
            val providerInfo = getAppWidgetInfo(widgetItem.packageName, widgetItem.providerClassName)
            if (providerInfo?.configure != null) {
                emitEvent(UiEvent.ConfigureWidget(widgetItem.appWidgetId))
            } else {
                snackbarManager.show("This widget cannot be reconfigured.")
            }
        }
    }

    fun startPlaceholderWidgetReplacement(widgetItem: HomeItem.Widget) {
        pendingWidgetReplacementTarget = widgetItem
        _widgetReplacementTarget.value = widgetItem
        viewModelScope.launch {
            emitEvent(UiEvent.NavigateToWidgetPicker)
        }
    }

    fun cancelPlaceholderWidgetReplacement() {
        pendingWidgetReplacementTarget = null
        _widgetReplacementTarget.value = null
    }

    private fun getAppWidgetInfo(packageName: String, className: String): android.appwidget.AppWidgetProviderInfo? {
        return appWidgetManager.installedProviders.find {
            it.provider.packageName == packageName && it.provider.className == className
        }
    }

    fun moveApp(appItem: HomeItem.App, newRow: Int, newColumn: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value

            if (!validateAndReport(
                    currentLayout,
                    appItem.id,
                    appItem.page,
                    newRow,
                    newColumn,
                    appItem.rowSpan,
                    appItem.columnSpan,
                    "move app"
                )) return@launch

            val updatedItems = currentLayout.items.map { item ->
                if (item.id == appItem.id && item is HomeItem.App) {
                    item.copy(row = newRow, column = newColumn)
                } else item
            }

            settingsRepository.saveHomeLayout(currentLayout.copy(items = updatedItems))
        }
    }

    fun canMoveHomeItem(item: HomeItem, newRow: Int, newColumn: Int): Boolean {
        return validatePlacement(
            layout = _homeLayoutState.value,
            itemId = item.id,
            page = item.page,
            row = newRow,
            column = newColumn,
            rowSpan = item.rowSpan,
            columnSpan = item.columnSpan,
        ) is PlacementResult.Valid
    }

    fun resizeWidget(widgetItem: HomeItem.Widget, newRowSpan: Int, newColSpan: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value

            if (!validateAndReport(
                    currentLayout,
                    widgetItem.id,
                    widgetItem.page,
                    widgetItem.row,
                    widgetItem.column,
                    newRowSpan,
                    newColSpan,
                    "resize widget"
                )) return@launch

            val newItems = currentLayout.items.map {
                if (it.id == widgetItem.id && it is HomeItem.Widget) {
                    it.copy(rowSpan = newRowSpan, columnSpan = newColSpan)
                } else it
            }

            val newLayout = currentLayout.copy(items = newItems)
            _homeLayoutState.value = newLayout
            settingsRepository.saveHomeLayout(newLayout)
            settingsRepository.triggerHomeLayoutRefresh()

            if (widgetItem.isPlaceholder || widgetItem.appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                return@launch
            }

            // Update options
            val screenWidthDp = getScreenDimensions(context = appContext).first
            val screenHeightDp = getScreenDimensions(appContext).second
            val cellWidthDp = screenWidthDp.toFloat() / currentLayout.columns
            val cellHeightDp = screenHeightDp.toFloat() / currentLayout.rows

            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, (newColSpan * cellWidthDp).toInt())
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, (newColSpan * cellWidthDp).toInt())
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, (newRowSpan * cellHeightDp).toInt())
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, (newRowSpan * cellHeightDp).toInt())
            }

            try {
                appWidgetManager.updateAppWidgetOptions(widgetItem.appWidgetId, options)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to update widget options", e)
            }
        }
    }

    fun resizeApp(appItem: HomeItem.App, newRowSpan: Int, newColSpan: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value

            if (!validateAndReport(
                    currentLayout,
                    appItem.id,
                    appItem.page,
                    appItem.row,
                    appItem.column,
                    newRowSpan,
                    newColSpan,
                    "resize app"
                )) return@launch

            val updatedItems = currentLayout.items.map { item ->
                if (item.id == appItem.id && item is HomeItem.App) {
                    item.copy(rowSpan = newRowSpan, columnSpan = newColSpan)
                } else item
            }

            val newLayout = currentLayout.copy(items = updatedItems)
            _homeLayoutState.value = newLayout
            settingsRepository.saveHomeLayout(newLayout)
            settingsRepository.triggerHomeLayoutRefresh()
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_CONFIGURE_WIDGET) {
            val widgetId = pendingWidgetInfo?.appWidgetId
                ?: data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                ?: AppWidgetManager.INVALID_APPWIDGET_ID

            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                if (resultCode == RESULT_OK) {
                    pendingWidgetInfo?.let { info ->
                        if(info.appWidgetId == widgetId) {
                            addWidgetToLayout(info.appWidgetId, info.providerInfo)
                        }
                    } ?: run {
                        Log.d("ViewModelWidget", "Widget ID $widgetId reconfigured successfully.")
                        viewModelScope.launch {
                            val currentLayout = _homeLayoutState.value
                            _homeLayoutState.value = currentLayout.copy()
                            settingsRepository.triggerHomeLayoutRefresh()
                        }
                    }
                } else {
                    Log.w("ViewModelWidget", "Widget configuration cancelled/failed for ID $widgetId")
                    appWidgetHost.deleteAppWidgetId(widgetId)
                    snackbarManager.show("Widget configuration cancelled.")
                    pendingWidgetReplacementTarget = null
                    _widgetReplacementTarget.value = null
                }
            }
            pendingWidgetInfo = null
        }
    }

    fun updatePrivateSpaceState() {
        viewModelScope.launch {
            try {
                if (!privateSpaceHelper.isPrivateSpaceSupported()) {
                    _privateSpaceState.value = PrivateSpaceState.Unsupported
                    return@launch
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    val isSetUp = privateSpaceHelper.isPrivateSpaceSetUp()
                    if (!isSetUp) {
                        _privateSpaceState.value = PrivateSpaceState.NotSetUp
                        return@launch
                    }
                    val isLocked = privateSpaceHelper.isPrivateSpaceLocked()
                    _privateSpaceState.value = if (isLocked) PrivateSpaceState.Locked else PrivateSpaceState.Unlocked
                } else {
                    _privateSpaceState.value = PrivateSpaceState.Unsupported
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error updating Private Space state", e)
                _privateSpaceState.value = PrivateSpaceState.NotSetUp
            }
        }
    }

    fun togglePrivateSpace() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            privateSpaceHelper.togglePrivateSpaceLock(
                onSuccess = {
                    updatePrivateSpaceState()
                    loadApps()
                    _refreshTrigger.value++
                },
                onFailure = { message -> snackbarManager.show(message) }
            )
        } else {
            snackbarManager.show("Private Space requires Android 15 or higher")
        }
    }

    fun openPrivateSpaceSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val intentSender = privateSpaceHelper.getPrivateSpaceSettingsIntentSender()
            if (intentSender != null) {
                try {
                    @Suppress("DEPRECATION")
                    intentSender.sendIntent(appContext, 0, null, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                    snackbarManager.show("Failed to open Private Space settings")
                }
            } else {
                snackbarManager.show("Private Space settings intent available only on Android 16+")
            }
        } else {
            snackbarManager.show("Private Space requires Android 15 or higher")
        }
    }

    fun isAppInPrivateSpace(app: AppModel): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return privateSpaceHelper.isPrivateSpaceProfile(app.user)
        }
        return false
    }

    fun firstOpen(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFirstOpen(value)
        }
    }

    fun loadApps() {
        requestAppReload("manualLoadApps", forceEmit = true)
    }

    private suspend fun migrateLegacyKeysForCurrentApps() {
        val settings = settingsRepository.settings.first()
        val renamedKeys = settings.renamedApps.keys
        val hiddenKeys = settings.hiddenApps
        val historyKeys = settings.recentAppHistory.keys
        val appTags = AppTagStorage.decode(settings.appTagsJson)
        val tagKeys = appTags.keys
        val existingKeys = buildSet {
            addAll(renamedKeys)
            addAll(hiddenKeys)
            addAll(historyKeys)
            addAll(tagKeys)
        }

        val migrations = mutableListOf<AppKeyMigration>()
        for (app in appRepository.appListAll.value) {
            val appKey = app.getKey()
            val legacyMoveKeys = AppKey.legacyMoveKeysForApp(app)
                .filter { existingKeys.contains(it) }
                .toSet()
            val legacyCopyCandidates = AppKey.legacyCopyKeysForApp(app)
                .filter { existingKeys.contains(it) }
                .toSet()

            val hasNewRename = settings.renamedApps.containsKey(appKey)
            val hasNewHidden = settings.hiddenApps.contains(appKey)
            val newHistory = settings.recentAppHistory[appKey]
            val hasNewTags = appTags.containsKey(appKey)

            val legacyRename = legacyCopyCandidates.firstNotNullOfOrNull { settings.renamedApps[it] }
            val legacyHidden = legacyCopyCandidates.any { settings.hiddenApps.contains(it) }
            val legacyHistory = legacyCopyCandidates.mapNotNull { settings.recentAppHistory[it] }.maxOrNull()
            val legacyTags = legacyCopyCandidates.flatMap { appTags[it].orEmpty() }

            val shouldCopy = (!hasNewRename && legacyRename != null) ||
                (!hasNewHidden && legacyHidden) ||
                (legacyHistory != null && (newHistory == null || legacyHistory > newHistory)) ||
                (!hasNewTags && legacyTags.isNotEmpty())

            val legacyCopyKeys = if (shouldCopy) legacyCopyCandidates else emptySet()

            if (legacyMoveKeys.isNotEmpty() || legacyCopyKeys.isNotEmpty()) {
                migrations.add(
                    AppKeyMigration(
                        newKey = appKey,
                        moveKeys = legacyMoveKeys,
                        copyKeys = legacyCopyKeys
                    )
                )
            }
        }

        if (migrations.isNotEmpty()) {
            settingsRepository.migrateAppKeys(migrations)
            appRepository.loadApps(forceEmit = true)
        }
    }

    fun getHiddenApps() {
        viewModelScope.launch {
            try {
                _appDrawerState.value = _appDrawerState.value.copy(isLoading = true)
                appRepository.loadHiddenApps()
                _appDrawerState.value = _appDrawerState.value.copy(isLoading = false)
            } catch (e: Exception) {
                snackbarManager.show("Failed to load hidden apps: ${e.message}")
                _appDrawerState.value = _appDrawerState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun toggleAppHidden(app: AppModel) {
        viewModelScope.launch {
            try {
                appRepository.toggleAppHidden(app)
            } catch (e: Exception) {
                snackbarManager.show("Failed to toggle app visibility: ${e.message}")
            }
        }
    }

    fun getTagsForApp(app: AppModel): List<String> =
        _appTags.value[app.getKey()].orEmpty()

    fun getTagsForAppKey(appKey: String): List<String> =
        _appTags.value[appKey].orEmpty()

    fun saveTagsForApp(app: AppModel, tags: List<String>) {
        viewModelScope.launch {
            val appKey = app.getKey()
            settingsRepository.setAppTags(appKey, tags)

            val legacyMoveKeys = AppKey.legacyMoveKeysForApp(app)
            val legacyCopyKeys = AppKey.legacyCopyKeysForApp(app)
            if (legacyMoveKeys.isNotEmpty() || legacyCopyKeys.isNotEmpty()) {
                settingsRepository.migrateAppKeys(
                    listOf(
                        AppKeyMigration(
                            newKey = appKey,
                            moveKeys = legacyMoveKeys,
                            copyKeys = legacyCopyKeys
                        )
                    )
                )
            }
        }
    }

    fun saveTagsForAppKey(appKey: String, tags: List<String>) {
        viewModelScope.launch {
            settingsRepository.setAppTags(appKey, tags)
        }
    }

    fun renameTagAcrossApps(oldTag: String, newTag: String) {
        viewModelScope.launch {
            settingsRepository.renameTagAcrossApps(oldTag, newTag)
        }
    }

    fun removeTagAcrossApps(tag: String) {
        viewModelScope.launch {
            settingsRepository.removeTagAcrossApps(tag)
        }
    }

    fun launchApp(app: AppModel) {
        viewModelScope.launch {
            try {
                launchAppInternal(app)
            } catch (e: Exception) {
                snackbarManager.show("Failed to launch app: ${e.message}")
            }
        }
    }

    fun setShortcutForApp(app: AppModel, modifier: Int, keyCode: Int) {
        viewModelScope.launch {
            settingsRepository.setKeyboardShortcut(app.getKey(), modifier, keyCode)
        }
    }

    fun clearShortcutForApp(app: AppModel) {
        viewModelScope.launch {
            settingsRepository.clearKeyboardShortcut(app.getKey())
        }
    }

    fun getShortcutForApp(appKey: String): KeyboardShortcut? =
        _keyboardShortcuts.value.find { it.appKey == appKey }

    fun launchAppByKey(appKey: String) {
        val app = _appListAll.value.find { it.getKey() == appKey } ?: return
        launchApp(app)
    }

    fun getAppShortcuts(app: AppModel, onResult: (List<AppShortcut>) -> Unit) {
        viewModelScope.launch {
            val shortcuts = appRepository.getAppShortcuts(app)
            onResult(shortcuts)
        }
    }

    fun launchShortcut(app: AppModel, shortcutId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            snackbarManager.show("Shortcuts require Android 7.1 or higher")
            return
        }

        viewModelScope.launch {
            try {
                val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                launcherApps.startShortcut(
                    app.appPackage,
                    shortcutId,
                    null,
                    null,
                    app.user
                )
                settingsRepository.updateAppLaunchTime(app.getKey())
            } catch (e: Exception) {
                snackbarManager.show("Failed to open shortcut: ${e.message}")
            }
        }
    }

    fun pinShortcutToLauncher(app: AppModel, shortcutId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            snackbarManager.show("Shortcuts require Android 7.1 or higher")
            return
        }

        viewModelScope.launch {
            try {
                val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                if (!launcherApps.hasShortcutHostPermission()) {
                    snackbarManager.show("Set CCLauncher as the default launcher to pin shortcuts")
                    return@launch
                }

                val query = LauncherApps.ShortcutQuery()
                    .setPackage(app.appPackage)
                    .setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                    )
                val shortcuts = launcherApps.getShortcuts(query, app.user).orEmpty()
                val shortcut = shortcuts.firstOrNull { it.id == shortcutId }
                if (shortcut == null) {
                    snackbarManager.show("Shortcut not available")
                    return@launch
                }

                val pinnedQuery = LauncherApps.ShortcutQuery()
                    .setPackage(app.appPackage)
                    .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
                val pinnedIds = launcherApps.getShortcuts(pinnedQuery, app.user)
                    .orEmpty()
                    .mapNotNull { it.id }
                    .toMutableSet()
                pinnedIds.add(shortcutId)

                launcherApps.pinShortcuts(app.appPackage, pinnedIds.toList(), app.user)
                requestAppReload("shortcutAdded", forceEmit = true)
                snackbarManager.show("Shortcut added")
            } catch (e: Exception) {
                snackbarManager.show("Failed to add shortcut: ${e.message}")
            }
        }
    }

    fun deleteSystemShortcut(app: AppModel) {
        if (!app.isSystemShortcut) return

        viewModelScope.launch {
            try {
                appRepository.deletePinnedShortcut(
                    packageName = app.systemShortcutPackage!!,
                    shortcutId = app.systemShortcutId!!,
                    user = app.user
                )
                loadApps()
                snackbarManager.show("Shortcut deleted")
            } catch (e: Exception) {
                snackbarManager.show("Failed to delete shortcut: ${e.message}")
            }
        }
    }

    fun selectedApp(appModel: AppModel, flag: Int) {
        when (flag) {
            Constants.FLAG_LAUNCH_APP, Constants.FLAG_HIDDEN_APPS -> launchApp(appModel)
            Constants.FLAG_SET_SWIPE_LEFT_APP,
            Constants.FLAG_SET_PORTRAIT_SWIPE_LEFT_APP -> setSwipeLeftApp(appModel, HomeOrientation.PORTRAIT)
            Constants.FLAG_SET_SWIPE_RIGHT_APP,
            Constants.FLAG_SET_PORTRAIT_SWIPE_RIGHT_APP -> setSwipeRightApp(appModel, HomeOrientation.PORTRAIT)
            Constants.FLAG_SET_SWIPE_UP_APP,
            Constants.FLAG_SET_PORTRAIT_SWIPE_UP_APP -> setSwipeUpApp(appModel, HomeOrientation.PORTRAIT)
            Constants.FLAG_SET_SWIPE_DOWN_APP,
            Constants.FLAG_SET_PORTRAIT_SWIPE_DOWN_APP -> setSwipeDownApp(appModel, HomeOrientation.PORTRAIT)
            Constants.FLAG_SET_LANDSCAPE_SWIPE_LEFT_APP -> setSwipeLeftApp(appModel, HomeOrientation.LANDSCAPE)
            Constants.FLAG_SET_LANDSCAPE_SWIPE_RIGHT_APP -> setSwipeRightApp(appModel, HomeOrientation.LANDSCAPE)
            Constants.FLAG_SET_LANDSCAPE_SWIPE_UP_APP -> setSwipeUpApp(appModel, HomeOrientation.LANDSCAPE)
            Constants.FLAG_SET_LANDSCAPE_SWIPE_DOWN_APP -> setSwipeDownApp(appModel, HomeOrientation.LANDSCAPE)
        }
    }

    private fun setSwipeLeftApp(app: AppModel, orientation: HomeOrientation) {
        viewModelScope.launch {
            val appPreference = AppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString(),
                isSystemShortcut = app.isSystemShortcut,
                systemShortcutId = app.systemShortcutId,
                systemShortcutPackage = app.systemShortcutPackage
            )
            settingsRepository.setSwipeLeftApp(orientation, appPreference)
        }
    }

    private fun setSwipeRightApp(app: AppModel, orientation: HomeOrientation) {
        viewModelScope.launch {
            val appPreference = AppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString(),
                isSystemShortcut = app.isSystemShortcut,
                systemShortcutId = app.systemShortcutId,
                systemShortcutPackage = app.systemShortcutPackage
            )
            settingsRepository.setSwipeRightApp(orientation, appPreference)
        }
    }

    fun launchSwipeUpApp() {
        viewModelScope.launch {
            val orientation = _activeHomeOrientation.value
            val swipeUpApp = settingsRepository.settings.first().swipeAppFor(orientation, "up")
            if (swipeUpApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = swipeUpApp.label,
                    key = null,
                    appPackage = swipeUpApp.packageName,
                    activityClassName = swipeUpApp.activityClassName,
                    user = getUserHandleFromString(appContext, swipeUpApp.userString),
                    isSystemShortcut = swipeUpApp.isSystemShortcut,
                    systemShortcutId = swipeUpApp.systemShortcutId,
                    systemShortcutPackage = swipeUpApp.systemShortcutPackage
                )
                launchApp(app)
            }
        }
    }

    fun launchSwipeDownApp() {
        viewModelScope.launch {
            val orientation = _activeHomeOrientation.value
            val swipeDownApp = settingsRepository.settings.first().swipeAppFor(orientation, "down")
            if (swipeDownApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = swipeDownApp.label,
                    key = null,
                    appPackage = swipeDownApp.packageName,
                    activityClassName = swipeDownApp.activityClassName,
                    user = getUserHandleFromString(appContext, swipeDownApp.userString),
                    isSystemShortcut = swipeDownApp.isSystemShortcut,
                    systemShortcutId = swipeDownApp.systemShortcutId,
                    systemShortcutPackage = swipeDownApp.systemShortcutPackage
                )
                launchApp(app)
            }
        }
    }

    fun launchSwipeLeftApp() {
        viewModelScope.launch {
            val swipeLeftApp = settingsRepository.getSwipeLeftApp(_activeHomeOrientation.value)
            if (swipeLeftApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = swipeLeftApp.label,
                    key = null,
                    appPackage = swipeLeftApp.packageName,
                    activityClassName = swipeLeftApp.activityClassName,
                    user = getUserHandleFromString(appContext, swipeLeftApp.userString),
                    isSystemShortcut = swipeLeftApp.isSystemShortcut,
                    systemShortcutId = swipeLeftApp.systemShortcutId,
                    systemShortcutPackage = swipeLeftApp.systemShortcutPackage
                )
                launchApp(app)
            }
        }
    }

    fun launchSwipeRightApp() {
        viewModelScope.launch {
            val swipeRightApp = settingsRepository.getSwipeRightApp(_activeHomeOrientation.value)
            if (swipeRightApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = swipeRightApp.label,
                    key = null,
                    appPackage = swipeRightApp.packageName,
                    activityClassName = swipeRightApp.activityClassName,
                    user = getUserHandleFromString(appContext, swipeRightApp.userString),
                    isSystemShortcut = swipeRightApp.isSystemShortcut,
                    systemShortcutId = swipeRightApp.systemShortcutId,
                    systemShortcutPackage = swipeRightApp.systemShortcutPackage
                )
                launchApp(app)
            }
        }
    }

    private fun setSwipeUpApp(app: AppModel, orientation: HomeOrientation) {
        viewModelScope.launch {
            settingsRepository.setSwipeUpApp(
                orientation,
                AppPreference(
                    label = app.appLabel,
                    packageName = app.appPackage,
                    activityClassName = app.activityClassName,
                    userString = app.user.toString(),
                    isSystemShortcut = app.isSystemShortcut,
                    systemShortcutId = app.systemShortcutId,
                    systemShortcutPackage = app.systemShortcutPackage
                )
            )
        }
    }

    private fun setSwipeDownApp(app: AppModel, orientation: HomeOrientation) {
        viewModelScope.launch {
            settingsRepository.setSwipeDownApp(
                orientation,
                AppPreference(
                    label = app.appLabel,
                    packageName = app.appPackage,
                    activityClassName = app.activityClassName,
                    userString = app.user.toString(),
                    isSystemShortcut = app.isSystemShortcut,
                    systemShortcutId = app.systemShortcutId,
                    systemShortcutPackage = app.systemShortcutPackage
                )
            )
        }
    }

    fun setCurrentPage(page: Int) {
        val layout = _homeLayoutState.value
        if (page in 0 until layout.pageCount) {
            _currentPage.value = page
        }
    }

    fun willPageChangeAffectItems(newPageCount: Int, orientation: HomeOrientation): Boolean {
        val layout = if (orientation == _activeHomeOrientation.value) {
            _homeLayoutState.value
        } else {
            val settings = _settingsSnapshot.value
            settings.homeLayouts.layoutFor(orientation).copy(
                rows = settings.homeRowsFor(orientation),
                columns = settings.homeColumnsFor(orientation),
                pageCount = settings.homePagesFor(orientation)
            )
        }
        if (newPageCount >= layout.pageCount) return false
        return layout.items.any { it.page >= newPageCount }
    }

    fun updatePageCount(newPageCount: Int, orientation: HomeOrientation) {
        viewModelScope.launch {
            val currentLayout = if (orientation == _activeHomeOrientation.value) {
                _homeLayoutState.value
            } else {
                loadLayoutForOrientation(orientation)
            }
            val clampedCount = newPageCount.coerceIn(1, MAX_PAGES)

            val removedItems = currentLayout.items.filter { it.page >= clampedCount }
            removedItems.filterIsInstance<HomeItem.Widget>()
                .filter { !it.isPlaceholder }
                .forEach { widget ->
                    try {
                        appWidgetHost.deleteAppWidgetId(widget.appWidgetId)
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Error deleting widget ID ${widget.appWidgetId}", e)
                    }
                }

            val keptItems = currentLayout.items.filter { it.page < clampedCount }
            saveLayoutForOrientation(orientation, currentLayout.copy(pageCount = clampedCount, items = keptItems))

            if (orientation == _activeHomeOrientation.value && _currentPage.value >= clampedCount) {
                _currentPage.value = clampedCount - 1
            }
        }
    }

    fun lockScreen() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.doubleTapToLock) {
                val intent = Intent(appContext, MyAccessibilityService::class.java)
                intent.action = "LOCK_SCREEN"
                appContext.startService(intent)
            }
        }
    }

    /**
     * Search apps by query with alias support.
     */
    fun searchApps(query: String) {
        appDrawerSearchQuery.value = query
    }

    private suspend fun filterAndRank(query: String): List<AppModel> {
        val settings = settingsRepository.settings.first()
        val listToFilter = if (settings.showHiddenAppsOnSearch) _appListAll.value else _appList.value

        if (query.isBlank()) return listToFilter

        val mode = settings.searchAliasesMode
        val searchType = settings.searchType
        val queryVariants = SearchAliasUtils.buildQueryVariants(query, mode)

        val filtered = listToFilter.filter { app ->
            val label = app.appLabel
            val labelNorm = label.lowercase()

            val direct = when (searchType) {
                Constants.SearchType.FUZZY -> fuzzyMatch(label, query)
                Constants.SearchType.STARTS_WITH -> queryVariants.any { v -> labelNorm.startsWith(v) }
                Constants.SearchType.EXACT -> queryVariants.any { v -> labelNorm == v }
                else -> queryVariants.any { v -> labelNorm.contains(v) }
            }
            if (direct) return@filter true

            val aliases = searchAliasIndex[app.getKey()] ?: emptySet()
            when (searchType) {
                Constants.SearchType.STARTS_WITH -> queryVariants.any { v -> aliases.any { it.startsWith(v) } }
                Constants.SearchType.FUZZY -> queryVariants.any { v -> aliases.any { it.contains(v) } }
                Constants.SearchType.EXACT -> queryVariants.any { v -> aliases.any { it == v } }
                else -> queryVariants.any { v -> aliases.any { it.contains(v) } }
            }
        }

        return filtered
    }

    private suspend fun reapplySearchFilter() {
        val current = _appDrawerState.value
        val newFiltered = filterAndRank(current.searchQuery)
        _appDrawerState.value = current.copy(
            filteredApps = newFiltered,
            isLoading = false,
            error = null
        )
    }

    fun moveWidget(widgetItem: HomeItem.Widget, newRow: Int, newColumn: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value

            if (!validateAndReport(
                    currentLayout,
                    widgetItem.id,
                    widgetItem.page,
                    newRow,
                    newColumn,
                    widgetItem.rowSpan,
                    widgetItem.columnSpan,
                    "move widget"
                )) return@launch

            val updatedItems = currentLayout.items.map { item ->
                if (item.id == widgetItem.id && item is HomeItem.Widget) {
                    item.copy(row = newRow, column = newColumn)
                } else item
            }

            settingsRepository.saveHomeLayout(currentLayout.copy(items = updatedItems))
        }
    }

    private fun fuzzyMatch(text: String, pattern: String): Boolean {
        val textLower = text.lowercase()
        val patternLower = pattern.lowercase()
        var textIndex = 0
        var patternIndex = 0
        while (textIndex < textLower.length && patternIndex < patternLower.length) {
            if (textLower[textIndex] == patternLower[patternIndex]) {
                patternIndex++
            }
            textIndex++
        }
        return patternIndex == patternLower.length
    }

    fun emitEvent(event: UiEvent) {
        viewModelScope.launch { _eventsFlow.emit(event) }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            appContext.unregisterReceiver(appRefreshReceiver)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error unregistering receiver", e)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                launcherApps.unregisterCallback(launcherAppsCallback)
                Log.d("MainViewModel", "LauncherApps callback unregistered")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error unregistering LauncherApps callback", e)
            }
        }
    }

    enum class PrivateSpaceState {
        Unsupported,
        NotSetUp,
        Locked,
        Unlocked
    }

    private sealed class PlacementResult {
        object Valid : PlacementResult()
        data class Invalid(val reason: String) : PlacementResult()
    }

    private fun validatePlacement(
        layout: HomeLayout,
        itemId: String,
        page: Int,
        row: Int,
        column: Int,
        rowSpan: Int,
        columnSpan: Int
    ): PlacementResult {
        // Bounds check
        if (row < 0 || column < 0) {
            return PlacementResult.Invalid("Invalid position")
        }

        if (row + rowSpan > layout.rows) {
            return PlacementResult.Invalid("Would go out of bounds vertically")
        }

        if (column + columnSpan > layout.columns) {
            return PlacementResult.Invalid("Would go out of bounds horizontally")
        }

        // Overlap check - only check items on the same page.
        // Hidden folders (showOnHome == false) are treated as not occupying space.
        val hasOverlap = layout.itemsForPage(page).any { item ->
            if (item.id == itemId) return@any false
            if (item is HomeItem.Folder && !item.showOnHome) return@any false

            val itemEndRow = item.row + item.rowSpan
            val itemEndCol = item.column + item.columnSpan
            val newEndRow = row + rowSpan
            val newEndCol = column + columnSpan

            // Check if rectangles overlap
            !(row >= itemEndRow || newEndRow <= item.row ||
                    column >= itemEndCol || newEndCol <= item.column)
        }

        return if (hasOverlap) {
            PlacementResult.Invalid("Would overlap with other items")
        } else {
            PlacementResult.Valid
        }
    }

    /**
     * Validates and shows error if invalid, returns true if valid
     */
    private fun validateAndReport(
        layout: HomeLayout,
        itemId: String,
        page: Int,
        row: Int,
        column: Int,
        rowSpan: Int,
        columnSpan: Int,
        actionName: String
    ): Boolean {
        return when (val result = validatePlacement(layout, itemId, page, row, column, rowSpan, columnSpan)) {
            is PlacementResult.Valid -> true
            is PlacementResult.Invalid -> {
                snackbarManager.show("Cannot $actionName: ${result.reason}")
                false
            }
        }
    }
}
