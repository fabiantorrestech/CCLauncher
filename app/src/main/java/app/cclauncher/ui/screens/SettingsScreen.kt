package app.cclauncher.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cclauncher.MainViewModel
import app.cclauncher.data.Constants
import app.cclauncher.data.HomeItem
import app.cclauncher.settings.AppPreference
import app.cclauncher.settings.AppSettings
import app.cclauncher.helper.IconCache
import app.cclauncher.helper.PermissionManager
import app.cclauncher.helper.iconpack.IconPackManager
import app.cclauncher.helper.setPlainWallpaperByTheme
import app.cclauncher.ui.AppSelectionType
import app.cclauncher.ui.BackHandler
import app.cclauncher.ui.UiEvent
import app.cclauncher.ui.components.ColorPickerDialog
import app.cclauncher.ui.components.FontPickerDialog
import app.cclauncher.ui.components.GridSizeWarningDialog
import app.cclauncher.ui.components.IconPackSelectionDialog
import app.cclauncher.ui.components.SettingsAction
import app.cclauncher.ui.components.SettingsItem
import app.cclauncher.ui.components.SettingsSection
import app.cclauncher.ui.components.SettingsToggle
import app.cclauncher.ui.dialogs.AccessibilityDisclosureDialog
import app.cclauncher.ui.dialogs.DropdownSettingDialog
import app.cclauncher.ui.dialogs.SettingsLockDialog
import app.cclauncher.ui.dialogs.SliderSettingDialog
import app.cclauncher.ui.util.updateStatusBarVisibility
import app.cclauncher.ui.viewmodels.SettingsViewModel
import app.cclauncher.settings.AppPicker
import app.cclauncher.settings.AppSettingsSchema
import app.cclauncher.settings.ColorPicker
import app.cclauncher.settings.FontPicker
import app.cclauncher.settings.IconPackPicker
import androidx.compose.ui.tooling.preview.Preview
import app.cclauncher.ui.components.PageReduceWarningDialog
import app.cclauncher.ui.components.snackbar.SnackbarManager
import app.cclauncher.ui.dialogs.ImportExportResultDialog
import app.cclauncher.ui.dialogs.ImportValidationDialog
import app.cclauncher.ui.viewmodels.ImportExportState
import io.github.mlmgames.settings.core.types.Button
import io.github.mlmgames.settings.core.SettingField
import io.github.mlmgames.settings.core.backup.ValidationResult
import io.github.mlmgames.settings.core.types.Dropdown
import io.github.mlmgames.settings.core.types.Slider
import io.github.mlmgames.settings.core.types.Toggle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Defines the settings screen information architecture.
 */
private data class SettingsTab(val title: String, val sections: List<SettingsSectionSpec>)

private data class SettingsSectionSpec(
    val title: String,
    val entries: List<SettingsSectionEntry>,
)

private sealed interface SettingsSectionEntry {
    data class Field(val name: String) : SettingsSectionEntry
    data class Manual(val key: String) : SettingsSectionEntry
}

private fun field(name: String) = SettingsSectionEntry.Field(name)
private fun manual(key: String) = SettingsSectionEntry.Manual(key)

private const val MANUAL_ADD_WIDGET = "add_widget"
private const val MANUAL_MANAGE_FOLDERS = "manage_folders"
private const val MANUAL_CORNER_ZONES = "corner_zones"
private const val MANUAL_PRIVATE_SPACE = "private_space"
private const val MANUAL_DEFAULT_LAUNCHER = "default_launcher"
private const val MANUAL_LOCK_SETTINGS = "lock_settings"
private const val MANUAL_HIDDEN_APPS = "hidden_apps"
private const val MANUAL_APP_INFO = "app_info"
private const val MANUAL_ABOUT = "about"
private const val MANUAL_EXPORT_SETTINGS = "export_settings"
private const val MANUAL_IMPORT_SETTINGS = "import_settings"

private val settingsTabs = listOf(
    SettingsTab(
        title = "Home",
        sections = listOf(
            SettingsSectionSpec(
                title = "Grid & Pages",
                entries = listOf(
                    field("homeScreenRows"),
                    field("homeScreenColumns"),
                    field("homeScreenPages"),
                    field("showPageIndicator"),
                    field("showMoveGridOverlay"),
                ),
            ),
            SettingsSectionSpec(
                title = "Icons & Labels",
                entries = listOf(
                    field("scaleHomeApps"),
                    field("showHomeScreenIcons"),
                    field("showIconsInPortrait"),
                    field("showIconsInLandscape"),
                    field("textSizeScale"),
                    field("homeLabelFontPath"),
                    field("appLabelAlignment"),
                ),
            ),
            SettingsSectionSpec(
                title = "Shortcuts & PWAs",
                entries = listOf(
                    field("showShortcutIcon"),
                    field("shortcutIconPlacement"),
                ),
            ),
            SettingsSectionSpec(
                title = "Widgets",
                entries = listOf(manual(MANUAL_ADD_WIDGET)),
            ),
        ),
    ),
    SettingsTab(
        title = "App Drawer",
        sections = listOf(
            SettingsSectionSpec(
                title = "Launch & Search",
                entries = listOf(
                    field("defaultScreen"),
                    field("autoShowKeyboard"),
                    field("autoOpenFilteredApp"),
                    field("returnToHomeAfterApp"),
                    field("appDrawerTapToOpen"),
                    field("appDrawerLongPressEnabled"),
                ),
            ),
            SettingsSectionSpec(
                title = "Search Matching",
                entries = listOf(
                    field("searchType"),
                    field("searchSortOrder"),
                    field("searchAliasesMode"),
                    field("showHiddenAppsOnSearch"),
                    field("showWebSearchOption"),
                ),
            ),
            SettingsSectionSpec(
                title = "List Layout",
                entries = listOf(
                    field("showAppNames"),
                    field("showAppNamesInSearchAfter"),
                    field("searchBarPlacement"),
                    field("invertSearchResultsOrder"),
                    field("reverseAppListDirection"),
                    field("avoidCameraBottomSearch"),
                    field("showScrollbar"),
                    field("scrollbarOnLeft"),
                    field("appDrawerAlignment"),
                    field("itemSpacing"),
                ),
            ),
            SettingsSectionSpec(
                title = "Shortcuts & Typography",
                entries = listOf(
                    field("showPinnedShortcuts"),
                    field("appDrawerLabelFontPath"),
                    field("searchResultsUseHomeFont"),
                    field("searchResultsFontSize"),
                ),
            ),
        ),
    ),
    SettingsTab(
        title = "Appearance",
        sections = listOf(
            SettingsSectionSpec(
                title = "Theme & Display",
                entries = listOf(
                    field("appTheme"),
                    field("useDynamicTheme"),
                    field("screenOrientation"),
                    field("statusBar"),
                ),
            ),
            SettingsSectionSpec(
                title = "Fonts",
                entries = listOf(
                    field("fontWeight"),
                    field("useSystemFont"),
                    field("customFontPath"),
                    field("headerFontPath"),
                    field("tertiaryFontPath"),
                ),
            ),
            SettingsSectionSpec(
                title = "Colors & Icons",
                entries = listOf(
                    field("useCustomTextColor"),
                    field("textColor"),
                    field("iconCornerRadius"),
                    field("selectedIconPack"),
                ),
            ),
            SettingsSectionSpec(
                title = "Wallpaper",
                entries = listOf(
                    field("plainWallpaper"),
                    field("autoUpdateWallpaper"),
                ),
            ),
        ),
    ),
    SettingsTab(
        title = "Gestures",
        sections = listOf(
            SettingsSectionSpec(
                title = "General",
                entries = listOf(
                    field("gestureSensitivity"),
                    field("doubleTapToLock"),
                    field("swipeGesturesInFolders"),
                ),
            ),
            SettingsSectionSpec(
                title = "Swipe Down",
                entries = listOf(
                    field("swipeDownAction"),
                    field("swipeDownApp"),
                ),
            ),
            SettingsSectionSpec(
                title = "Swipe Up",
                entries = listOf(
                    field("swipeUpAction"),
                    field("swipeUpApp"),
                ),
            ),
            SettingsSectionSpec(
                title = "Swipe Left",
                entries = listOf(
                    field("swipeLeftAction"),
                    field("swipeLeftApp"),
                ),
            ),
            SettingsSectionSpec(
                title = "Swipe Right",
                entries = listOf(
                    field("swipeRightAction"),
                    field("swipeRightApp"),
                ),
            ),
            SettingsSectionSpec(
                title = "Corner Zones",
                entries = listOf(manual(MANUAL_CORNER_ZONES)),
            ),
        ),
    ),
    SettingsTab(
        title = "Folders",
        sections = listOf(
            SettingsSectionSpec(
                title = "Folder Defaults",
                entries = listOf(
                    field("showFolderIcon"),
                    field("folderIconPlacement"),
                    field("folderLabelFontPath"),
                    field("folderBackgroundOpacity"),
                ),
            ),
            SettingsSectionSpec(
                title = "Manage Folders",
                entries = listOf(manual(MANUAL_MANAGE_FOLDERS)),
            ),
        ),
    ),
    SettingsTab(
        title = "System",
        sections = listOf(
            SettingsSectionSpec(
                title = "Launcher Access",
                entries = listOf(
                    manual(MANUAL_DEFAULT_LAUNCHER),
                    manual(MANUAL_PRIVATE_SPACE),
                    manual(MANUAL_HIDDEN_APPS),
                ),
            ),
            SettingsSectionSpec(
                title = "Security",
                entries = listOf(manual(MANUAL_LOCK_SETTINGS)),
            ),
            SettingsSectionSpec(
                title = "Backup & Restore",
                entries = listOf(
                    manual(MANUAL_EXPORT_SETTINGS),
                    manual(MANUAL_IMPORT_SETTINGS),
                ),
            ),
            SettingsSectionSpec(
                title = "About",
                entries = listOf(
                    manual(MANUAL_ABOUT),
                    manual(MANUAL_APP_INFO),
                ),
            ),
        ),
    ),
)

/**
 * A manually-defined (non-schema) item that should appear in search results.
 * [onClick] is stable for the lifetime of the composable since it captures only
 * coroutineScope/viewModel references and MutableState setters.
 */
private data class ManualSearchItem(
    val key: String,
    val title: String,
    val description: String,
    val category: String,
    val onClick: () -> Unit,
)

/** Union type for the unified search results list. */
private sealed interface Either {
    val category: String
    data class Field(val field: SettingField<AppSettings, *>, override val category: String) : Either
    data class Manual(val item: ManualSearchItem) : Either {
        override val category get() = item.category
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    mainViewModel: MainViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToHiddenApps: () -> Unit = {},
    onNavigateToFolderList: () -> Unit = {},
    onNavigateToCornerDotSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val uiState by viewModel.settingsState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val snackbarManager: SnackbarManager = koinInject()

    val schema = remember { AppSettingsSchema }

    // Dialog states (now track a SettingField instead of KProperty/annotations)
    var showingDialog by remember { mutableStateOf<String?>(null) }
    var currentField by remember { mutableStateOf<SettingField<AppSettings, *>?>(null) }

    var showGridWarningDialog by remember { mutableStateOf(false) }
    var pendingGridChange by remember { mutableStateOf<Pair<String, Int>?>(null) }

    var showPageWarningDialog by remember { mutableStateOf(false) }
    var pendingPageChange by remember { mutableStateOf<Int?>(null) }

    val homeLayoutState by mainViewModel.homeLayoutState.collectAsState()
    val allFolders = remember(homeLayoutState.items) {
        homeLayoutState.items.filterIsInstance<HomeItem.Folder>()
    }
    // "swipeDirection" -> show folder-picker dialog for that direction; null = closed
    var swipeFolderPickerFor by remember { mutableStateOf<String?>(null) }
    var swipeFolderSearch by remember { mutableStateOf("") }
    var pendingFontField by remember { mutableStateOf<String?>(null) }


    val pickFontLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            val fieldName = pendingFontField
            if (uri != null && fieldName != null) {
                viewModel.setFontForField(fieldName, uri)
            }
            pendingFontField = null
        }
    )

    val importExportState by viewModel.importExportState.collectAsState()
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var validationResult by remember { mutableStateOf<ValidationResult?>(null) }
    var showValidationDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportSettings(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val result = viewModel.validateBackup(selectedUri)
            if (result != null) {
                validationResult = result
                pendingImportUri = selectedUri
                showValidationDialog = true
            } else {
                snackbarManager.show("Could not read the selected file")
            }
        }
    }

    var showAccessibilityDisclosure by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetUnlockState() }
    }

    val effectiveLockState by viewModel.effectiveLockState.collectAsState()
    val showLockDialog by viewModel.showLockDialog.collectAsState()
    val isSettingPin by viewModel.isSettingPin.collectAsState()

    // Search state
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(onBack = {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        } else {
            viewModel.resetUnlockState()
            onNavigateBack()
        }
    })

    val grouped = remember(uiState) { schema.groupedByCategory() }
    val allFields = remember(uiState) {
        schema.orderedCategories().flatMap { grouped[it].orEmpty() }
    }
    val fieldsByName = remember(allFields) { allFields.associateBy { it.name } }
    val fieldCategories = remember {
        buildMap {
            settingsTabs.forEach { tab ->
                tab.sections.forEach { section ->
                    section.entries.forEach { entry ->
                        if (entry is SettingsSectionEntry.Field) {
                            put(entry.name, tab.title)
                        }
                    }
                }
            }
        }
    }
    val manualCategories = remember {
        buildMap {
            settingsTabs.forEach { tab ->
                tab.sections.forEach { section ->
                    section.entries.forEach { entry ->
                        if (entry is SettingsSectionEntry.Manual) {
                            put(entry.key, tab.title)
                        }
                    }
                }
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { settingsTabs.size })

    val searchableFields = remember(allFields) {
        allFields.mapNotNull { field ->
            field.meta?.let {
                field to (fieldCategories[field.name] ?: "Other")
            }
        }
    }

    val manualSearchItems = remember(uiState.lockSettings) {
        listOf(
            ManualSearchItem(
                key = MANUAL_ADD_WIDGET,
                title = "Add Widget",
                description = "Add a widget to your home screen",
                category = manualCategories.getValue(MANUAL_ADD_WIDGET),
                onClick = { coroutineScope.launch { viewModel.emitEvent(UiEvent.NavigateToWidgetPicker) } }
            ),
            ManualSearchItem(
                key = MANUAL_MANAGE_FOLDERS,
                title = "Manage Folders",
                description = "View and configure existing folders",
                category = manualCategories.getValue(MANUAL_MANAGE_FOLDERS),
                onClick = onNavigateToFolderList,
            ),
            ManualSearchItem(
                key = MANUAL_CORNER_ZONES,
                title = "Configure Corner Zones",
                description = "Set up tap and hold shortcut zones in the home screen corners",
                category = manualCategories.getValue(MANUAL_CORNER_ZONES),
                onClick = onNavigateToCornerDotSettings,
            ),
            ManualSearchItem(
                key = MANUAL_PRIVATE_SPACE,
                title = "Private Space",
                description = "Set up or manage Android Private Space",
                category = manualCategories.getValue(MANUAL_PRIVATE_SPACE),
                onClick = { mainViewModel.openPrivateSpaceSettings() },
            ),
            ManualSearchItem(
                key = MANUAL_DEFAULT_LAUNCHER,
                title = "Set as Default Launcher",
                description = "Set CCLauncher as your default launcher app",
                category = manualCategories.getValue(MANUAL_DEFAULT_LAUNCHER),
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                },
            ),
            ManualSearchItem(
                key = MANUAL_LOCK_SETTINGS,
                title = "Lock Settings",
                description = "Require a PIN before settings can be changed",
                category = manualCategories.getValue(MANUAL_LOCK_SETTINGS),
                onClick = {
                    if (uiState.lockSettings) {
                        viewModel.toggleLockSettings(false)
                    } else {
                        viewModel.setShowLockDialog(true, true)
                    }
                },
            ),
            ManualSearchItem(
                key = MANUAL_HIDDEN_APPS,
                title = "Hidden Apps",
                description = "Manage apps hidden from the app drawer",
                category = manualCategories.getValue(MANUAL_HIDDEN_APPS),
                onClick = onNavigateToHiddenApps,
            ),
            ManualSearchItem(
                key = MANUAL_APP_INFO,
                title = "App Info",
                description = "Open CCLauncher's system app info page",
                category = manualCategories.getValue(MANUAL_APP_INFO),
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                },
            ),
            ManualSearchItem(
                key = MANUAL_ABOUT,
                title = "About CCLauncher",
                description = "Version info and links",
                category = manualCategories.getValue(MANUAL_ABOUT),
                onClick = {
                    coroutineScope.launch { viewModel.emitEvent(UiEvent.ShowDialog(Constants.Dialog.ABOUT)) }
                },
            ),
            ManualSearchItem(
                key = MANUAL_EXPORT_SETTINGS,
                title = "Export Settings",
                description = "Save your settings to a backup file",
                category = manualCategories.getValue(MANUAL_EXPORT_SETTINGS),
                onClick = {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    exportLauncher.launch("cclauncher_settings_$timestamp.json")
                },
            ),
            ManualSearchItem(
                key = MANUAL_IMPORT_SETTINGS,
                title = "Import Settings",
                description = "Restore settings from a backup file",
                category = manualCategories.getValue(MANUAL_IMPORT_SETTINGS),
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
            ),
        )
    }

    // ----- Shared callback for rendering a setting field's action (used by both pager and search) -----
    val onFieldAction: (SettingField<AppSettings, *>, String) -> Unit = remember(uiState) {
        { field, dialogType ->
            currentField = field
            showingDialog = dialogType
        }
    }

    // ----- Dialogs (unchanged from original) -----

    if (showLockDialog) {
        SettingsLockDialog(
            isSettingPin = isSettingPin,
            onDismiss = { viewModel.setShowLockDialog(false) },
            onConfirm = { pin ->
                if (isSettingPin) {
                    viewModel.setPin(pin)
                    viewModel.toggleLockSettings(true)
                    viewModel.setShowLockDialog(false)
                } else {
                    coroutineScope.launch {
                        if (viewModel.validatePin(pin)) {
                            viewModel.setShowLockDialog(false)
                        }
                    }
                }
            }
        )
    }

    if (showGridWarningDialog && pendingGridChange != null) {
        GridSizeWarningDialog(
            onConfirm = {
                coroutineScope.launch {
                    val (propertyName, newValue) = pendingGridChange!!
                    viewModel.updateGridSize(propertyName, newValue)
                    showGridWarningDialog = false
                    pendingGridChange = null
                }
            },
            onDismiss = {
                showGridWarningDialog = false
                pendingGridChange = null
            }
        )
    }

    if (showPageWarningDialog && pendingPageChange != null) {
        PageReduceWarningDialog(
            onConfirm = {
                coroutineScope.launch {
                    mainViewModel.updatePageCount(pendingPageChange!!)
                    showPageWarningDialog = false
                    pendingPageChange = null
                }
            },
            onDismiss = {
                showPageWarningDialog = false
                pendingPageChange = null
            }
        )
    }

    // Swipe-action folder picker dialog
    if (swipeFolderPickerFor != null) {
        val direction = swipeFolderPickerFor!!
        val filteredFolders = remember(swipeFolderSearch, allFolders) {
            if (swipeFolderSearch.isBlank()) allFolders
            else allFolders.filter { it.title.contains(swipeFolderSearch, ignoreCase = true) }
        }
        AlertDialog(
            onDismissRequest = { swipeFolderPickerFor = null; swipeFolderSearch = "" },
            title = { Text("Open Folder on Swipe ${direction.replaceFirstChar { it.uppercase() }}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = swipeFolderSearch,
                        onValueChange = { swipeFolderSearch = it },
                        placeholder = { Text("Search folders…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.height(280.dp)
                    ) {
                        if (filteredFolders.isEmpty()) {
                            item {
                                Text(
                                    "No folders found",
                                    modifier = Modifier.padding(8.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            items(filteredFolders, key = { it.id }) { folder ->
                                androidx.compose.material3.ListItem(
                                    headlineContent = { Text(folder.title) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            coroutineScope.launch {
                                                viewModel.updateSetting(
                                                    "swipe${direction.replaceFirstChar { it.uppercase() }}FolderId",
                                                    folder.id
                                                )
                                            }
                                            swipeFolderPickerFor = null
                                            swipeFolderSearch = ""
                                        }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { swipeFolderPickerFor = null; swipeFolderSearch = "" }) { Text("Cancel") }
            }
        )
    }

    when (showingDialog) {
        "slider" -> {
            val field = currentField
            val meta = field?.meta
            if (field != null && meta != null) {
                val currentValue = when (val v = field.get(uiState)) {
                    is Int -> v.toFloat()
                    is Float -> v
                    is Double -> v.toFloat()
                    is Long -> v.toFloat()
                    else -> 0f
                }

                SliderSettingDialog(
                    title = meta.title,
                    currentValue = currentValue,
                    min = meta.min,
                    max = meta.max,
                    step = meta.step,
                    onDismiss = { showingDialog = null },
                    onValueSelected = { newValue ->
                        coroutineScope.launch {
                            val propertyName = field.name
                            val intValue = newValue.toInt()

                            when {
                                (propertyName == "homeScreenRows" || propertyName == "homeScreenColumns") -> {
                                    if (viewModel.willGridChangeAffectItems(propertyName, intValue)) {
                                        pendingGridChange = propertyName to intValue
                                        showGridWarningDialog = true
                                        showingDialog = null
                                        return@launch
                                    }
                                    viewModel.updateGridSize(propertyName, intValue)
                                }

                                propertyName == "homeScreenPages" -> {
                                    if (mainViewModel.willPageChangeAffectItems(intValue)) {
                                        pendingPageChange = intValue
                                        showPageWarningDialog = true
                                        showingDialog = null
                                        return@launch
                                    }
                                    mainViewModel.updatePageCount(intValue)
                                }

                                else -> {
                                    when (field.get(uiState)) {
                                        is Int -> viewModel.updateSetting(propertyName, intValue)
                                        is Float -> viewModel.updateSetting(propertyName, newValue)
                                        is Double -> viewModel.updateSetting(propertyName, newValue.toDouble())
                                        is Long -> viewModel.updateSetting(propertyName, newValue.toLong())
                                    }
                                }
                            }

                            showingDialog = null
                        }
                    }
                )
            }
        }

        "dropdown" -> {
            val field = currentField
            val meta = field?.meta
            if (field != null && meta != null) {
                val selectedIndex = (field.get(uiState) as? Int) ?: 0

                DropdownSettingDialog(
                    title = meta.title,
                    options = meta.options,
                    selectedIndex = selectedIndex,
                    onDismiss = { showingDialog = null },
                    onOptionSelected = { index ->
                        coroutineScope.launch {
                            viewModel.updateSetting(field.name, index)
                            showingDialog = null
                        }
                    }
                )
            }
        }

        "button" -> {
            val field = currentField
            if (field != null) {
                when (field.name) {
                    "plainWallpaper" -> {
                        setPlainWallpaperByTheme(context, appTheme = uiState.appTheme)
                        showingDialog = null
                        snackbarManager.show("Plain wallpaper applied")
                    }
                    else -> showingDialog = null
                }
            }
        }

        "font_picker" -> {
            val field = currentField
            val meta = field?.meta
            if (field != null && meta != null) {
                FontPickerDialog(
                    title = meta.title,
                    currentPath = (field.get(uiState) as? String).orEmpty(),
                    onDismiss = { showingDialog = null },
                    onSelectClicked = {
                        pendingFontField = field.name
                        pickFontLauncher.launch("font/*")
                    },
                    onResetClicked = {
                        viewModel.clearFontForField(field.name)
                        showingDialog = null
                    }
                )
            }
        }

        "color_picker" -> {
            val field = currentField
            val meta = field?.meta
            if (field != null && meta != null) {
                val currentColor = (field.get(uiState) as? Int) ?: 0

                ColorPickerDialog(
                    title = meta.title,
                    currentColor = currentColor,
                    onDismiss = { showingDialog = null },
                    onColorSelected = { color ->
                        coroutineScope.launch {
                            viewModel.updateSetting(field.name, color)
                            showingDialog = null
                        }
                    }
                )
            }
        }
    }

    // ----- Main Scaffold -----

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search settings...") },
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
                        Text("Settings")
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isSearchActive) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    } else {
                        IconButton(onClick = {
                            isSearchActive = true
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search settings")
                        }
                    }
                }
            )
        },
    ) { paddingValues ->
        if (viewModel.isLoading.value) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (effectiveLockState) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(0.8f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Settings Locked",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Settings are locked",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Enter your PIN to access settings",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.setShowLockDialog(true, false) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Unlock Settings")
                        }
                    }
                }
            }
            return@Scaffold
        }

        // Focus the search field when it becomes active
        LaunchedEffect(isSearchActive) {
            if (isSearchActive) {
                searchFocusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        if (isSearchActive) {
            // ----- Search results: flat filtered list -----
            val query = searchQuery.trim().lowercase()

            // Sealed type combining schema fields and manual items for uniform filtering/rendering.
            val filteredResults: List<Either> = remember(query, searchableFields, manualSearchItems) {
                val schemaMatches = if (query.isEmpty()) {
                    searchableFields.map { (f, cat) -> Either.Field(f, cat) }
                } else {
                    searchableFields.mapNotNull { (field, categoryLabel) ->
                        val meta = field.meta ?: return@mapNotNull null
                        val hit = meta.title.lowercase().contains(query) ||
                            meta.description.lowercase().contains(query) ||
                            meta.options.any { it.lowercase().contains(query) } ||
                            categoryLabel.lowercase().contains(query)
                        if (hit) Either.Field(field, categoryLabel) else null
                    }
                }
                val manualMatches = if (query.isEmpty()) {
                    manualSearchItems.map { Either.Manual(it) }
                } else {
                    manualSearchItems.mapNotNull { item ->
                        val hit = item.title.lowercase().contains(query) ||
                            item.description.lowercase().contains(query) ||
                            item.category.lowercase().contains(query)
                        if (hit) Either.Manual(item) else null
                    }
                }
                schemaMatches + manualMatches
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (filteredResults.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No settings found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Group by category, preserving schema-field order then manual-item order
                    val byCategory = filteredResults.groupBy { it.category }
                    for ((categoryLabel, results) in byCategory) {
                        item(key = "search_cat_$categoryLabel") {
                            SettingsSection(title = categoryLabel) {
                                results.forEach { entry ->
                                    when (entry) {
                                        is Either.Field -> key(entry.field.name) {
                                            SettingsFieldRenderer(
                                                field = entry.field,
                                                uiState = uiState,
                                                schema = schema,
                                                coroutineScope = coroutineScope,
                                                viewModel = viewModel,
                                                context = context,
                                                onShowDialog = onFieldAction,
                                                onShowAccessibilityDisclosure = { showAccessibilityDisclosure = true },
                                            )
                                        }
                                        is Either.Manual -> key(entry.item.key) {
                                            SettingsItem(
                                                title = entry.item.title,
                                                description = entry.item.description.takeIf { it.isNotEmpty() },
                                                onClick = entry.item.onClick,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ----- Tabbed pager -----
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
            ) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {},
                ) {
                    settingsTabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 0,
                ) { pageIndex ->
                    val tab = settingsTabs[pageIndex]
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        tab.sections.forEach { section ->
                            item(key = "${tab.title}_${section.title}") {
                                SettingsSection(title = section.title) {
                                    section.entries.forEach { entry ->
                                        when (entry) {
                                            is SettingsSectionEntry.Field -> {
                                                fieldsByName[entry.name]?.let { field ->
                                                    SettingsFieldRenderer(
                                                        field = field,
                                                        uiState = uiState,
                                                        schema = schema,
                                                        coroutineScope = coroutineScope,
                                                        viewModel = viewModel,
                                                        context = context,
                                                        onShowDialog = onFieldAction,
                                                        onShowAccessibilityDisclosure = { showAccessibilityDisclosure = true },
                                                    )
                                                    SwipeFolderSettingRenderer(
                                                        fieldName = field.name,
                                                        uiState = uiState,
                                                        allFolders = allFolders,
                                                        onOpenFolderPicker = { swipeFolderPickerFor = it },
                                                    )
                                                }
                                            }

                                            is SettingsSectionEntry.Manual -> {
                                                ManualSettingsEntryRenderer(
                                                    key = entry.key,
                                                    uiState = uiState,
                                                    context = context,
                                                    mainViewModel = mainViewModel,
                                                    viewModel = viewModel,
                                                    coroutineScope = coroutineScope,
                                                    onNavigateToHiddenApps = onNavigateToHiddenApps,
                                                    onNavigateToFolderList = onNavigateToFolderList,
                                                    onNavigateToCornerDotSettings = onNavigateToCornerDotSettings,
                                                    exportLauncher = exportLauncher,
                                                    importLauncher = importLauncher,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAccessibilityDisclosure) {
        AccessibilityDisclosureDialog(
            onDismiss = {
                showAccessibilityDisclosure = false
                coroutineScope.launch { viewModel.updateSetting("doubleTapToLock", false) }
            },
            onAccept = {
                showAccessibilityDisclosure = false
                coroutineScope.launch {
                    viewModel.updateSetting("doubleTapToLock", true)
                    viewModel.updateSetting("accessibilityConsent", false)
                }
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    Toast.makeText(
                        context,
                        "Enable CCLauncher under Accessibility > Downloaded services.",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (_: Exception) {}
            }
        )
    }

    if (importExportState != ImportExportState.Idle) {
        ImportExportResultDialog(
            state = importExportState,
            onDismiss = { viewModel.resetImportExportState() }
        )
    }

    if (showValidationDialog && validationResult != null) {
        ImportValidationDialog(
            validationResult = validationResult!!,
            onConfirm = {
                showValidationDialog = false
                pendingImportUri?.let { viewModel.importSettings(it) }
                pendingImportUri = null
                validationResult = null
            },
            onDismiss = {
                showValidationDialog = false
                pendingImportUri = null
                validationResult = null
            }
        )
    }
}

/**
 * Renders a single schema-driven setting field. Extracted to avoid duplicating the large
 * when-block in both the pager pages and the search results list.
 */
@Composable
private fun SettingsFieldRenderer(
    field: SettingField<AppSettings, *>,
    uiState: AppSettings,
    schema: AppSettingsSchema,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    viewModel: SettingsViewModel,
    context: Context,
    onShowDialog: (SettingField<AppSettings, *>, String) -> Unit,
    onShowAccessibilityDisclosure: () -> Unit = {},
) {
    val meta = field.meta ?: return
    val isEnabled = schema.isEnabled(uiState, field)

    val visuallyGroupedUnderParent = setOf(
        "invertSearchResultsOrder",
        "reverseAppListDirection",
    )
    val isSubSetting = meta.dependsOn.isNotBlank()
            || field.name in visuallyGroupedUnderParent

    when (meta.type) {
        Toggle::class -> {
            val value = (field.get(uiState) as? Boolean) ?: false
            SettingsToggle(
                title = meta.title,
                modifier = if (isSubSetting) Modifier.padding(start = 24.dp) else Modifier,
                description = meta.description.takeIf { it.isNotEmpty() },
                isChecked = value,
                enabled = isEnabled,
                onCheckedChange = { checked ->
                    coroutineScope.launch {
                        viewModel.updateSetting(field.name, checked)

                        when (field.name) {
                            "statusBar" -> {
                                try {
                                    (context as? Activity)?.let { activity ->
                                        updateStatusBarVisibility(activity, checked)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            "doubleTapToLock" -> {
                                if (checked) {
                                    onShowAccessibilityDisclosure()
                                } else {
                                    viewModel.updateSetting("doubleTapToLock", false)
                                }
                            }
                        }
                    }
                }
            )
        }

        Slider::class -> {
            val subtitle = when (val v = field.get(uiState)) {
                is Int -> v.toString()
                is Float -> String.format(Locale.getDefault(), "%.1f", v)
                is Double -> String.format(Locale.getDefault(), "%.1f", v)
                is Long -> v.toString()
                else -> ""
            }

            SettingsItem(
                title = meta.title,
                subtitle = subtitle,
                description = meta.description.takeIf { it.isNotEmpty() },
                enabled = isEnabled,
                onClick = { onShowDialog(field, "slider") }
            )
        }

        Dropdown::class -> {
            val idx = (field.get(uiState) as? Int) ?: 0
            val options = meta.options
            val displayText = options.getOrNull(idx) ?: "Unknown"

            SettingsItem(
                title = meta.title,
                subtitle = displayText,
                description = meta.description.takeIf { it.isNotEmpty() },
                enabled = isEnabled,
                onClick = { onShowDialog(field, "dropdown") }
            )
        }

        Button::class -> {
            SettingsAction(
                title = meta.title,
                description = meta.description.takeIf { it.isNotEmpty() },
                enabled = isEnabled,
                onClick = { onShowDialog(field, "button") }
            )
        }

        AppPicker::class -> {
            val pref = (field.get(uiState) as? AppPreference)
                ?: AppPreference(label = "Not set")
            SettingsItem(
                title = meta.title,
                subtitle = pref.label.ifBlank { "Not set" },
                description = meta.description.takeIf { it.isNotEmpty() },
                enabled = isEnabled,
                onClick = {
                    val selectionType = when (field.name) {
                        "swipeLeftApp" -> AppSelectionType.SWIPE_LEFT_APP
                        "swipeRightApp" -> AppSelectionType.SWIPE_RIGHT_APP
                        "swipeUpApp" -> AppSelectionType.SWIPE_UP_APP
                        "swipeDownApp" -> AppSelectionType.SWIPE_DOWN_APP
                        else -> null
                    }

                    selectionType?.let {
                        coroutineScope.launch {
                            viewModel.emitEvent(UiEvent.NavigateToAppSelection(it))
                        }
                    }
                }
            )
        }

        IconPackPicker::class -> {
            val iconCache = remember { IconCache(context) }
            var availableIconPacks by remember {
                mutableStateOf<List<IconPackManager.IconPackInfo>>(emptyList())
            }
            var showIconPackDialog by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                availableIconPacks = iconCache.getAvailableIconPacks()
            }

            val selectedPackName = (field.get(uiState) as? String) ?: "default"
            val selectedPackDisplayName = availableIconPacks.find {
                it.packageName == selectedPackName
            }?.name ?: "Default Icons"

            SettingsItem(
                title = meta.title,
                subtitle = selectedPackDisplayName,
                description = meta.description.takeIf { it.isNotEmpty() },
                enabled = isEnabled,
                onClick = { showIconPackDialog = true }
            )

            if (showIconPackDialog) {
                IconPackSelectionDialog(
                    iconPacks = availableIconPacks,
                    selectedPack = selectedPackName,
                    onDismiss = { showIconPackDialog = false },
                    onPackSelected = { selectedPack ->
                        coroutineScope.launch {
                            viewModel.updateSetting(field.name, selectedPack)
                            iconCache.clearCache()
                            showIconPackDialog = false
                        }
                    }
                )
            }
        }

        FontPicker::class -> {
            val fontPath = (field.get(uiState) as? String).orEmpty()
            val displayText = if (fontPath.isEmpty()) {
                "System default"
            } else {
                fontPath.split("/").last()
            }

            SettingsItem(
                title = meta.title,
                subtitle = displayText,
                description = meta.description.takeIf { it.isNotEmpty() },
                enabled = isEnabled,
                onClick = { onShowDialog(field, "font_picker") }
            )
        }

        ColorPicker::class -> {
            val colorValue = (field.get(uiState) as? Int) ?: 0
            val displayText =
                if (colorValue == 0) "Theme Default" else "Custom Color"

            SettingsItem(
                title = meta.title,
                subtitle = displayText,
                description = meta.description.takeIf { it.isNotEmpty() },
                enabled = isEnabled,
                onClick = { onShowDialog(field, "color_picker") }
            )
        }

        else -> {
            SettingsItem(
                title = meta.title,
                subtitle = "Unsupported setting type",
                description = meta.description.takeIf { it.isNotEmpty() },
                enabled = false,
                onClick = {}
            )
        }
    }
}

// Helper functions
@Composable
private fun SwipeFolderSettingRenderer(
    fieldName: String,
    uiState: AppSettings,
    allFolders: List<HomeItem.Folder>,
    onOpenFolderPicker: (String) -> Unit,
) {
    val titleAndSubtitle = when (fieldName) {
        "swipeDownAction" -> {
            if (uiState.swipeDownAction != Constants.SwipeAction.OPEN_FOLDER) return
            "Swipe Down Folder" to (allFolders.find { it.id == uiState.swipeDownFolderId }?.title ?: "Not set")
        }
        "swipeUpAction" -> {
            if (uiState.swipeUpAction != Constants.SwipeAction.OPEN_FOLDER) return
            "Swipe Up Folder" to (allFolders.find { it.id == uiState.swipeUpFolderId }?.title ?: "Not set")
        }
        "swipeLeftAction" -> {
            if (uiState.swipeLeftAction != Constants.SwipeAction.OPEN_FOLDER) return
            "Swipe Left Folder" to (allFolders.find { it.id == uiState.swipeLeftFolderId }?.title ?: "Not set")
        }
        "swipeRightAction" -> {
            if (uiState.swipeRightAction != Constants.SwipeAction.OPEN_FOLDER) return
            "Swipe Right Folder" to (allFolders.find { it.id == uiState.swipeRightFolderId }?.title ?: "Not set")
        }
        else -> return
    }

    val direction = fieldName.removePrefix("swipe").removeSuffix("Action").lowercase()
    SettingsItem(
        title = titleAndSubtitle.first,
        subtitle = titleAndSubtitle.second,
        modifier = Modifier.padding(start = 24.dp),
        onClick = { onOpenFolderPicker(direction) },
    )
}

@Composable
private fun ManualSettingsEntryRenderer(
    key: String,
    uiState: AppSettings,
    context: Context,
    mainViewModel: MainViewModel,
    viewModel: SettingsViewModel,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onNavigateToHiddenApps: () -> Unit,
    onNavigateToFolderList: () -> Unit,
    onNavigateToCornerDotSettings: () -> Unit,
    exportLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    importLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
) {
    when (key) {
        MANUAL_ADD_WIDGET -> {
            SettingsAction(
                title = "Add Widget",
                description = "Add a widget to your home screen",
                onClick = {
                    coroutineScope.launch {
                        viewModel.emitEvent(UiEvent.NavigateToWidgetPicker)
                    }
                }
            )
        }

        MANUAL_MANAGE_FOLDERS -> {
            SettingsAction(
                title = "Manage Folders",
                description = "Create, rename, and inspect folders on the home screen",
                onClick = onNavigateToFolderList,
            )
        }

        MANUAL_CORNER_ZONES -> {
            SettingsAction(
                title = "Configure Corner Zones",
                description = "Set up tap and hold shortcut zones in the home screen corners",
                onClick = onNavigateToCornerDotSettings,
            )
        }

        MANUAL_PRIVATE_SPACE -> {
            if (mainViewModel.isPrivateSpaceSupported) {
                val privateSpaceState by mainViewModel.privateSpaceState.collectAsState()
                val subtitle = when (privateSpaceState) {
                    MainViewModel.PrivateSpaceState.NotSetUp -> {
                        "Tap to set up or manage Private Space (needs to be the default launcher)"
                    }
                    MainViewModel.PrivateSpaceState.Locked,
                    MainViewModel.PrivateSpaceState.Unlocked -> "Tap to manage Private Space"
                    else -> ""
                }
                SettingsItem(
                    title = "Private Space",
                    subtitle = subtitle,
                    onClick = { mainViewModel.openPrivateSpaceSettings() },
                )
            } else {
                SettingsItem(
                    title = "Private Space",
                    subtitle = "Requires Android 15 or higher",
                    enabled = false,
                    onClick = {},
                    transparency = 0.7f,
                )
            }
        }

        MANUAL_DEFAULT_LAUNCHER -> {
            val isDefault = isClauncherDefault(context)
            SettingsItem(
                title = "Set as Default Launcher",
                subtitle = if (isDefault) "CCLauncher is default" else "CCLauncher is not default",
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                },
                transparency = if (isDefault) 0.7f else 1.0f,
            )
        }

        MANUAL_LOCK_SETTINGS -> {
            SettingsToggle(
                title = "Lock Settings",
                description = "Prevent changes to settings without a PIN",
                isChecked = uiState.lockSettings,
                onCheckedChange = { locked ->
                    if (locked) {
                        viewModel.setShowLockDialog(true, true)
                    } else {
                        viewModel.toggleLockSettings(false)
                    }
                }
            )
        }

        MANUAL_HIDDEN_APPS -> {
            SettingsItem(
                title = "Hidden Apps",
                description = "Manage apps hidden from the app drawer",
                onClick = onNavigateToHiddenApps,
            )
        }

        MANUAL_APP_INFO -> {
            SettingsItem(
                title = "App Info",
                description = "Open CCLauncher's system app info page",
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            )
        }

        MANUAL_ABOUT -> {
            SettingsItem(
                title = "About CCLauncher",
                subtitle = "Version ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}",
                onClick = {
                    coroutineScope.launch {
                        viewModel.emitEvent(UiEvent.ShowDialog(Constants.Dialog.ABOUT))
                    }
                }
            )
        }

        MANUAL_EXPORT_SETTINGS -> {
            SettingsAction(
                title = "Export Settings",
                description = "Save your settings to a file",
                onClick = {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(Date())
                    exportLauncher.launch("cclauncher_settings_$timestamp.json")
                }
            )
        }

        MANUAL_IMPORT_SETTINGS -> {
            SettingsAction(
                title = "Import Settings",
                description = "Restore settings from a backup file",
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
            )
        }
    }
}

fun isClauncherDefault(context: Context): Boolean {
    val permissionManager = PermissionManager(context)
    return permissionManager.isDefaultLauncher()
}

// ─── PREVIEWS (debug-only, stripped from release) ────────────────────────────
// Note: SettingsScreen uses Koin ViewModels so can't be previewed directly.
// These previews show the tab + section layout structure with fake data so you
// can adjust spacing, typography, and row layout in the IDE without running the app.

@Preview(showBackground = true, name = "Settings - General Tab", widthDp = 400, heightDp = 800)
@Composable
private fun PreviewSettingsGeneralTab() {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Settings") })
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val tabs = settingsTabs.map { it.title }
                val pagerState = rememberPagerState(pageCount = { tabs.size })
                val scope = rememberCoroutineScope()

                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {},
                ) {
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(label, style = MaterialTheme.typography.labelLarge) }
                        )
                    }
                }

                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        when (page) {
                            0 -> {
                                item {
                                    SettingsSection(title = "Grid & Pages") {
                                        SettingsItem(title = "Home Screen Rows", subtitle = "8", onClick = {})
                                        SettingsItem(title = "Home Screen Columns", subtitle = "4", onClick = {})
                                        SettingsItem(title = "Home Screen Pages", subtitle = "1", onClick = {})
                                        SettingsToggle(title = "Show Page Indicator", isChecked = true, onCheckedChange = {})
                                    }
                                }
                                item {
                                    SettingsSection(title = "Icons & Labels") {
                                        SettingsToggle(title = "Show App Icons on Home Screen", isChecked = false, onCheckedChange = {})
                                        SettingsItem(title = "Home Text Size (Default)", subtitle = "1.0", onClick = {})
                                        SettingsItem(title = "Home Label Font (Default)", subtitle = "System default", onClick = {})
                                        SettingsItem(title = "Home App Label Alignment (Default)", subtitle = "Left", onClick = {})
                                    }
                                }
                                item {
                                    SettingsSection(title = "Shortcuts & PWAs") {
                                        SettingsToggle(title = "Show Web Icon for Shortcuts", isChecked = true, onCheckedChange = {})
                                        SettingsItem(title = "PWA Icon Placement (Default)", subtitle = "Left", onClick = {})
                                    }
                                }
                            }
                            1 -> {
                                item {
                                    SettingsSection(title = "Launch & Search") {
                                        SettingsItem(title = "Default Screen", subtitle = "Home", onClick = {})
                                        SettingsToggle(title = "Auto Show Keyboard", isChecked = true, onCheckedChange = {})
                                        SettingsToggle(title = "Auto Open Single Matches", isChecked = true, onCheckedChange = {})
                                        SettingsToggle(title = "Tap to Open in App Drawer", isChecked = true, onCheckedChange = {})
                                    }
                                }
                                item {
                                    SettingsSection(title = "List Layout") {
                                        SettingsToggle(title = "Show App Names", isChecked = false, onCheckedChange = {})
                                        SettingsItem(title = "Search Bar Placement", subtitle = "Top", onClick = {})
                                        SettingsToggle(title = "Show Scrollbar", isChecked = false, onCheckedChange = {})
                                        SettingsItem(title = "App Drawer Alignment", subtitle = "Left", onClick = {})
                                    }
                                }
                            }
                            2 -> {
                                item {
                                    SettingsSection(title = "Theme & Display") {
                                        SettingsItem(title = "Theme", subtitle = "Dark", onClick = {})
                                        SettingsToggle(title = "Use Dynamic Theme", isChecked = false, onCheckedChange = {})
                                        SettingsItem(title = "Screen Orientation", subtitle = "System Default", onClick = {})
                                        SettingsToggle(title = "Show Status Bar", isChecked = false, onCheckedChange = {})
                                    }
                                }
                                item {
                                    SettingsSection(title = "Fonts") {
                                        SettingsItem(title = "Font Weight", subtitle = "Normal", onClick = {})
                                        SettingsToggle(title = "Use System Font", isChecked = true, onCheckedChange = {})
                                        SettingsItem(title = "Custom Font", subtitle = "System default", onClick = {})
                                    }
                                }
                                item {
                                    SettingsSection(title = "Wallpaper") {
                                        SettingsAction(title = "Set Plain Wallpaper", description = "Set a plain black or white wallpaper based on theme", onClick = {})
                                        SettingsToggle(title = "Auto Update Wallpaper", isChecked = false, onCheckedChange = {})
                                    }
                                }
                            }
                            3 -> {
                                item {
                                    SettingsSection(title = "General") {
                                        SettingsItem(title = "Gesture Sensitivity", subtitle = "1.0", onClick = {})
                                        SettingsToggle(title = "Double Tap to Lock Screen", isChecked = false, onCheckedChange = {})
                                        SettingsToggle(title = "Swipe Gestures in Folders", isChecked = false, onCheckedChange = {})
                                    }
                                }
                                item {
                                    SettingsSection(title = "Swipe Down") {
                                        SettingsItem(title = "Swipe Down Action", subtitle = "Notifications", onClick = {})
                                        SettingsItem(title = "Swipe Down App", subtitle = "Not set", onClick = {})
                                    }
                                }
                                item {
                                    SettingsSection(title = "Corner Zones") {
                                        SettingsAction(title = "Configure Corner Zones", description = "Set up tap and hold shortcut zones in the home screen corners", onClick = {})
                                    }
                                }
                            }
                            4 -> {
                                item {
                                    SettingsSection(title = "Folder Defaults") {
                                        SettingsToggle(title = "Show Folder Icon", isChecked = true, onCheckedChange = {})
                                        SettingsItem(title = "Folder Icon Placement (Default)", subtitle = "Left", onClick = {})
                                        SettingsItem(title = "Folder Label Font (Default)", subtitle = "System default", onClick = {})
                                        SettingsItem(title = "Folder Background Opacity", subtitle = "0.6", onClick = {})
                                    }
                                }
                                item {
                                    SettingsSection(title = "Manage Folders") {
                                        SettingsAction(title = "Manage Folders", description = "Create, rename, and inspect folders on the home screen", onClick = {})
                                    }
                                }
                            }
                            5 -> {
                                item {
                                    SettingsSection(title = "Launcher Access") {
                                        SettingsItem(title = "Set as Default Launcher", subtitle = "CCLauncher is not default", onClick = {})
                                        SettingsItem(title = "Private Space", subtitle = "Tap to set up or manage Private Space", onClick = {})
                                        SettingsItem(title = "Hidden Apps", onClick = {})
                                    }
                                }
                                item {
                                    SettingsSection(title = "Security") {
                                        SettingsToggle(title = "Lock Settings", description = "Prevent changes to settings without a PIN", isChecked = false, onCheckedChange = {})
                                    }
                                }
                                item {
                                    SettingsSection(title = "Backup & Restore") {
                                        SettingsAction(title = "Export Settings", description = "Save your settings to a backup file", onClick = {})
                                        SettingsAction(title = "Import Settings", description = "Restore settings from a backup file", onClick = {})
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
