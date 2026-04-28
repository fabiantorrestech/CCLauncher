package app.cclauncher.settings

import androidx.appcompat.app.AppCompatDelegate
import app.cclauncher.data.Constants
import app.cclauncher.data.HomeOrientation
import kotlinx.serialization.Serializable
import app.cclauncher.data.HomeLayout
import app.cclauncher.data.OrientationHomeLayouts
import io.github.mlmgames.settings.core.annotations.CategoryDefinition
import io.github.mlmgames.settings.core.annotations.Persisted
import io.github.mlmgames.settings.core.annotations.SchemaVersion
import io.github.mlmgames.settings.core.annotations.Serialized
import io.github.mlmgames.settings.core.annotations.Setting
import io.github.mlmgames.settings.core.types.Button
import io.github.mlmgames.settings.core.types.Dropdown
import io.github.mlmgames.settings.core.types.SettingTypeMarker
import io.github.mlmgames.settings.core.types.Slider
import io.github.mlmgames.settings.core.types.Toggle

@SchemaVersion(3)
data class AppSettings(

    @Setting(
        title = "Show App Names",
        category = General::class,
        type = Toggle::class,
        key = "SHOW_APP_NAMES",
    )
    val showAppNames: Boolean = false,

    @Setting(
        title = "Show Names in Search After",
        description = "Show app names in search results after typing this many characters. Set to 0 to use the 'Show App Names' setting instead.",
        category = General::class,
        type = Slider::class,
        min = 0f,
        max = 7f,
        step = 1f,
        key = "SHOW_APP_NAMES_IN_SEARCH_AFTER",
    )
    val showAppNamesInSearchAfter: Int = 0,

    @Setting(
        title = "Show Pinned Shortcuts",
        description = "Display pinned app shortcuts in the app drawer.",
        category = General::class,
        type = Toggle::class,
        key = "SHOW_PINNED_SHORTCUTS",
    )
    val showPinnedShortcuts: Boolean = true,

    @Setting(
        title = "Show Web Icon for Shortcuts",
        description = "Show a small web icon next to browser shortcuts and PWAs.",
        category = General::class,
        type = Toggle::class,
        key = "SHOW_SHORTCUT_ICON",
    )
    val showShortcutIcon: Boolean = true,

    @Setting(
        title = "PWA Icon Placement (Default)",
        description = "Default side for the web icon on newly placed shortcuts and PWAs",
        category = General::class,
        type = Dropdown::class,
        options = ["Left", "Right"],
        key = "SHORTCUT_ICON_PLACEMENT",
    )
    val shortcutIconPlacement: Int = Constants.IconPlacement.LEFT,

    @Persisted(key = "SHOW_APP_ICONS")
    val showAppIcons: Boolean = true,

    @Setting(
        title = "Auto Show Keyboard",
        category = General::class,
        type = Toggle::class,
        key = "AUTO_SHOW_KEYBOARD",
    )
    val autoShowKeyboard: Boolean = true,

    @Setting(
        title = "Show Hidden in Search",
        category = General::class,
        type = Toggle::class,
        key = "SHOW_HIDDEN_APPS_IN_SEARCH",
    )
    val showHiddenAppsOnSearch: Boolean = false,

    @Setting(
        title = "Auto Open Single Matches",
        category = General::class,
        type = Toggle::class,
        key = "AUTO_OPEN_FILTERED_APP",
    )
    val autoOpenFilteredApp: Boolean = true,

    @Setting(
        title = "Search Bar Placement",
        description = "Where the search bar sits in the app drawer",
        category = General::class,
        type = Dropdown::class,
        options = ["Top", "Bottom"],
        key = "SEARCH_BAR_PLACEMENT",
    )
    val searchBarPlacement: Int = Constants.SearchBarPlacement.TOP,

    @Setting(
        title = "Invert Search Results Order",
        description = "Recommended for Bottom Search Bar Placement. Best matches appear at the bottom instead of the top.",
        category = General::class,
        type = Toggle::class,
        key = "INVERT_SEARCH_RESULTS_ORDER",
    )
    val invertSearchResultsOrder: Boolean = false,

    @Setting(
        title = "Reverse App List Direction",
        description = "Full unfiltered app list renders bottom-to-top.",
        category = General::class,
        type = Toggle::class,
        key = "REVERSE_APP_LIST_DIRECTION",
    )
    val reverseAppListDirection: Boolean = false,

    @Setting(
        title = "Avoid Camera Cutout",
        description = "Recommended for Bottom Search Bar Placement. Reserves space at the top of results, preventing punch-hole cameras from obscuring items",
        category = General::class,
        type = Toggle::class,
        dependsOn = "searchBarPlacement",
        key = "AVOID_CAMERA_BOTTOM_SEARCH",
    )
    val avoidCameraBottomSearch: Boolean = false,

    @Setting(
        title = "Show Scrollbar",
        description = "Show a scroll indicator on the right side of the app drawer",
        category = General::class,
        type = Toggle::class,
        key = "SHOW_SCROLLBAR",
    )
    val showScrollbar: Boolean = false,

    @Setting(
        title = "Scrollbar on Left",
        description = "Place the scrollbar on the left side of the app drawer",
        category = General::class,
        type = Toggle::class,
        dependsOn = "showScrollbar",
        key = "SCROLLBAR_ON_LEFT",
    )
    val scrollbarOnLeft: Boolean = false,

    @Setting(
        title = "App Drawer Alignment",
        description = "Align app names and search results to the left or right side",
        category = General::class,
        type = Dropdown::class,
        options = ["Left", "Right"],
        key = "APP_DRAWER_ALIGNMENT",
    )
    val appDrawerAlignment: Int = Constants.AppDrawerAlignment.LEFT,

    @Setting(
        title = "Search Type",
        category = General::class,
        type = Dropdown::class,
        options = ["Contains", "Fuzzy Match", "Starts With", "Exact Match"],
        key = "SEARCH_TYPE",
    )
    val searchType: Int = Constants.SearchType.CONTAINS,

    @Setting(
        title = "Return to Home After App",
        description = "Return to home screen instead of search after closing an app",
        category = General::class,
        type = Toggle::class,
        key = "RETURN_TO_HOME_AFTER_APP",
    )
    val returnToHomeAfterApp: Boolean = false,

    @Setting(
        title = "Default Screen",
        description = "Choose which screen to show when opening the launcher",
        category = General::class,
        type = Dropdown::class,
        options = ["Home", "App Drawer"],
        key = "DEFAULT_SCREEN",
    )
    val defaultScreen: Int = 0,

    @Setting(
        title = "Search Sort Order",
        category = General::class,
        type = Dropdown::class,
        options = ["Alphabetical", "Reverse Alpha", "Last Launch"],
        key = "SEARCH_SORT_ORDER",
    )
    val searchSortOrder: Int = Constants.SortOrder.ALPHABETICAL,

    @Setting(
        title = "Search Aliases",
        description = "Match app names across transliterations and keyboard layouts (e.g. Африка ↔ Afrika). May slightly increase CPU use on older devices.",
        category = General::class,
        type = Dropdown::class,
        options = ["Off", "Transliteration", "Keyboard layout swap", "Both"],
        key = "SEARCH_ALIASES_MODE",
    )
    val searchAliasesMode: Int = 0,

    @Persisted(key = "SEARCH_INCLUDE_PACKAGE_NAMES")
    val searchIncludePackageNames: Boolean = false,

    @Setting(
        title = "Theme",
        category = Appearance::class,
        type = Dropdown::class,
        options = ["System", "Light", "Dark"],
        key = "APP_THEME",
    )
    val appTheme: Int = AppCompatDelegate.MODE_NIGHT_YES,

    @Setting(
        title = "Home Text Size (Default)",
        category = Appearance::class,
        type = Slider::class,
        min = 0.5f,
        max = 2.0f,
        step = 0.1f,
        key = "TEXT_SIZE_SCALE",
    )
    val textSizeScale: Float = 1.0f,

    @Persisted(key = "ANIMATION_SPEED")
    val animationSpeed: Float = 1.0f,

    @Setting(
        title = "Font Weight",
        category = Appearance::class,
        type = Dropdown::class,
        options = ["Thin", "Light", "Normal", "Medium", "Bold", "Black"],
        key = "FONT_WEIGHT",
    )
    val fontWeight: Int = 2,

    @Setting(
        title = "Use System Font",
        category = Appearance::class,
        type = Toggle::class,
        key = "USE_SYSTEM_FONT",
    )
    val useSystemFont: Boolean = true,

    @Setting(
        title = "Main Font",
        description = "Select the main font file for launcher UI fallback",
        category = Appearance::class,
        type = FontPicker::class,
        key = "CUSTOM_FONT_PATH",
    )
    val customFontPath: String = "",

    @Setting(
        title = "Header Font",
        description = "Font used primarily for headers and titles",
        category = Appearance::class,
        type = FontPicker::class,
        key = "HEADER_FONT_PATH",
    )
    val headerFontPath: String = "",

    @Setting(
        title = "Tertiary Font",
        description = "Font used for supporting details and smaller labels",
        category = Appearance::class,
        type = FontPicker::class,
        key = "TERTIARY_FONT_PATH",
    )
    val tertiaryFontPath: String = "",

    @Setting(
        title = "Home Label Font (Default)",
        description = "Default font for labels on the home screen",
        category = Appearance::class,
        type = FontPicker::class,
        key = "HOME_LABEL_FONT_PATH",
    )
    val homeLabelFontPath: String = "",

    @Setting(
        title = "Folder Label Font (Default)",
        description = "Default font for folder tiles and folder overlay labels",
        category = Folders::class,
        type = FontPicker::class,
        key = "FOLDER_LABEL_FONT_PATH",
    )
    val folderLabelFontPath: String = "",

    @Setting(
        title = "App Drawer Font",
        description = "Font used for app labels in the app drawer",
        category = Appearance::class,
        type = FontPicker::class,
        key = "APP_DRAWER_LABEL_FONT_PATH",
    )
    val appDrawerLabelFontPath: String = "",

    @Setting(
        title = "Use Dynamic Theme",
        category = Appearance::class,
        type = Toggle::class,
        key = "USE_DYNAMIC_THEME",
    )
    val useDynamicTheme: Boolean = false,

    @Setting(
        title = "Screen Orientation",
        type = Dropdown::class,
        options = ["System Default", "Force Portrait", "Force Landscape"],
        category = Appearance::class
    )
    var screenOrientation: Int = 0,

    @Setting(
        title = "Item Spacing",
        category = Appearance::class,
        type = Dropdown::class,
        options = ["None", "Small", "Medium", "Large"]
    )
    val itemSpacing: Int = 1,

    @Setting(
        title = "Search Results Use Home Text Size",
        category = Appearance::class,
        type = Toggle::class,
        description = "Use the same text size for search results as home screen"
    )
    val searchResultsUseHomeFont: Boolean = false,

    @Setting(
        title = "Search Results Text Size",
        category = Appearance::class,
        type = Slider::class,
        min = 0.5f,
        max = 2.0f,
        step = 0.1f
    )
    val searchResultsFontSize: Float = 1.0f,

    @Setting(
        title = "Icon Corner Radius",
        category = Appearance::class,
        type = Slider::class,
        min = 0f,
        max = 50f,
        step = 1f,
        key = "ICON_CORNER_RADIUS",
    )
    val iconCornerRadius: Int = 0,

    @Setting(
        title = "Text Color",
        description = "Customize text color for better visibility",
        category = Appearance::class,
        type = ColorPicker::class,
        key = "TEXT_COLOR",
    )
    val textColor: Int = 0,

    @Setting(
        title = "Use Custom Text Color",
        description = "Override theme text color with custom color",
        category = Appearance::class,
        type = Toggle::class,
        key = "USE_CUSTOM_TEXT_COLOR",
    )
    val useCustomTextColor: Boolean = false,

    @Setting(
        title = "Icon Pack",
        category = Appearance::class,
        type = IconPackPicker::class,
        description = "Choose custom icon pack for apps",
        key = "SELECTED_ICON_PACK",
    )
    val selectedIconPack: String = "default",

    // TODO: This is an action
    @Setting(
        title = "Set Plain Wallpaper",
        description = "Set a plain black/white wallpaper based on theme",
        category = Appearance::class,
        type = Button::class,
        key = "PLAIN_WALLPAPER",
    )
    val plainWallpaper: Boolean = false,

    @Setting(
        title = "Long Press in App Drawer",
        description = "Long press on apps shows options menu",
        category = Gestures::class,
        type = Toggle::class,
        key = "APP_DRAWER_LONG_PRESS_ENABLED",
    )
    val appDrawerLongPressEnabled: Boolean = true,

    @Setting(
        title = "Auto Update Wallpaper",
        description = "Automatically update plain wallpaper when system theme changes",
        category = Appearance::class,
        type = Toggle::class,
        key = "AUTO_UPDATE_WALLPAPER",
    )
    val autoUpdateWallpaper: Boolean = false,

    @Setting(
        title = "Show Status Bar",
        category = Layout::class,
        type = Toggle::class,
        key = "STATUS_BAR",
    )
    val statusBar: Boolean = false,

    @Setting(
        title = "Tap to Open in App Drawer",
        description = "When disabled, tapping an app drawer app does nothing. Long‑press still opens the menu.",
        category = General::class,
        type = Toggle::class,
        key = "APP_DRAWER_TAP_TO_OPEN",
    )
    val appDrawerTapToOpen: Boolean = true,

    @Setting(
        title = "Scale Home Apps",
        category = Layout::class,
        type = Toggle::class,
        key = "SCALE_HOME_APPS",
    )
    val scaleHomeApps: Boolean = true,

    @Setting(
        title = "Show Web Search Option",
        description = "Show 'Search Web' button when no apps match",
        category = General::class,
        type = Toggle::class,
        key = "SHOW_WEB_SEARCH_OPTION",
    )
    val showWebSearchOption: Boolean = true,

    @Setting(
        title = "Home Screen Rows",
        description = "Number of rows in the home screen grid",
        category = Layout::class,
        type = Slider::class,
        min = 4f,
        max = 12f,
        step = 1f,
        key = "HOME_SCREEN_ROWS",
    )
    val homeScreenRows: Int = 8,

    @Setting(
        title = "Home Screen Columns",
        description = "Number of columns in the home screen grid",
        category = Layout::class,
        type = Slider::class,
        min = 2f,
        max = 8f,
        step = 1f,
        key = "HOME_SCREEN_COLUMNS",
    )
    val homeScreenColumns: Int = 4,

    @Setting(
        title = "Home Screen Pages",
        description = "Number of home screen pages",
        category = Layout::class,
        type = Slider::class,
        min = 1f,
        max = 5f,
        step = 1f,
        key = "HOME_SCREEN_PAGES",
    )
    val homeScreenPages: Int = 1,

    @Setting(
        title = "Enable Landscape Layout",
        description = "Use a separate home layout when the launcher is in landscape",
        category = Layout::class,
        type = Toggle::class,
        key = "LANDSCAPE_LAYOUT_ENABLED",
    )
    val landscapeLayoutEnabled: Boolean = false,

    @Setting(
        title = "Rows",
        description = "Number of rows in the portrait home grid",
        category = Layout::class,
        type = Slider::class,
        min = 4f,
        max = 12f,
        step = 1f,
        key = "PORTRAIT_HOME_SCREEN_ROWS",
    )
    val portraitHomeScreenRows: Int = 8,

    @Setting(
        title = "Columns",
        description = "Number of columns in the portrait home grid",
        category = Layout::class,
        type = Slider::class,
        min = 2f,
        max = 8f,
        step = 1f,
        key = "PORTRAIT_HOME_SCREEN_COLUMNS",
    )
    val portraitHomeScreenColumns: Int = 4,

    @Setting(
        title = "Pages",
        description = "Number of portrait home pages",
        category = Layout::class,
        type = Slider::class,
        min = 1f,
        max = 5f,
        step = 1f,
        key = "PORTRAIT_HOME_SCREEN_PAGES",
    )
    val portraitHomeScreenPages: Int = 1,

    @Setting(
        title = "Rows",
        description = "Number of rows in the landscape home grid",
        category = Layout::class,
        type = Slider::class,
        min = 4f,
        max = 12f,
        step = 1f,
        key = "LANDSCAPE_HOME_SCREEN_ROWS",
    )
    val landscapeHomeScreenRows: Int = 4,

    @Setting(
        title = "Columns",
        description = "Number of columns in the landscape home grid",
        category = Layout::class,
        type = Slider::class,
        min = 2f,
        max = 8f,
        step = 1f,
        key = "LANDSCAPE_HOME_SCREEN_COLUMNS",
    )
    val landscapeHomeScreenColumns: Int = 8,

    @Setting(
        title = "Pages",
        description = "Number of landscape home pages",
        category = Layout::class,
        type = Slider::class,
        min = 1f,
        max = 5f,
        step = 1f,
        key = "LANDSCAPE_HOME_SCREEN_PAGES",
    )
    val landscapeHomeScreenPages: Int = 1,

    @Setting(
        title = "Show Page Indicator",
        description = "Show page dots at the bottom of the home screen",
        category = Layout::class,
        type = Toggle::class,
        key = "SHOW_PAGE_INDICATOR",
    )
    val showPageIndicator: Boolean = true,

    @Setting(
        title = "Show Grid Overlay in Move Mode",
        description = "Show a grid of cells while moving an app, widget, or folder so you can see exactly where each grid slot begins and ends.",
        category = Layout::class,
        type = Toggle::class,
        key = "SHOW_MOVE_GRID_OVERLAY",
    )
    val showMoveGridOverlay: Boolean = false,

    @Setting(
        title = "Show App Icons on Home Screen",
        description = "Display app icons on the home screen",
        category = Appearance::class,
        type = Toggle::class,
        key = "SHOW_HOME_SCREEN_ICONS",
    )
    val showHomeScreenIcons: Boolean = false,

    @Setting(
        title = "Show App Icons in Landscape",
        category = Layout::class,
        type = Toggle::class,
        key = "SHOW_ICONS_IN_LANDSCAPE",
    )
    val showIconsInLandscape: Boolean = false,

    @Setting(
        title = "Show App Icons in Portrait",
        category = Layout::class,
        type = Toggle::class,
        key = "SHOW_ICONS_IN_PORTRAIT",
    )
    val showIconsInPortrait: Boolean = false,

    @Setting(
        title = "Home App Label Alignment (Default)",
        description = "Align app names on the home screen",
        category = Appearance::class,
        type = Dropdown::class,
        options = ["Left", "Center", "Right"],
        key = "APP_LABEL_ALIGNMENT",
    )
    val appLabelAlignment: Int = 0,

    @Setting(
        title = "Gesture Sensitivity",
        description = "Adjust how easily swipe gestures are triggered",
        category = Gestures::class,
        type = Slider::class,
        min = 0.1f,
        max = 2.0f,
        step = 0.1f,
        key = "GESTURE_SENSITIVITY",
    )
    val gestureSensitivity: Float = 1.0f,

    @Setting(
        title = "Double Tap to Lock Screen",
        category = Gestures::class,
        type = Toggle::class,
        key = "DOUBLE_TAP_TO_LOCK",
    )
    val doubleTapToLock: Boolean = false,

    @Setting(
        title = "Swipe Gestures in Folders",
        description = "Allow swipe up/down/left/right actions while a folder is open",
        category = Gestures::class,
        type = Toggle::class,
        key = "SWIPE_GESTURES_IN_FOLDERS",
    )
    val swipeGesturesInFolders: Boolean = false,

    @Setting(
        title = "Swipe Down Action",
        category = Gestures::class,
        type = Dropdown::class,
        options = ["None", "Search", "Notifications", "App", "Next Page", "Previous Page", "Open Folder", "Open Settings"],
        key = "SWIPE_DOWN_ACTION",
    )
    val swipeDownAction: Int = Constants.SwipeAction.NOTIFICATIONS,

    @Setting(
        title = "Swipe Down App",
        category = Gestures::class,
        type = AppPicker::class,
        key = "SWIPE_DOWN_APP_JSON",
    )
    @Serialized
    val swipeDownApp: AppPreference = AppPreference(),

    @Persisted(key = "SWIPE_DOWN_FOLDER_ID")
    val swipeDownFolderId: String = "",

    @Setting(
        title = "Swipe Up Action",
        category = Gestures::class,
        type = Dropdown::class,
        options = ["None", "Search", "Notifications", "App", "Next Page", "Previous Page", "Open Folder", "Open Settings"],
        key = "SWIPE_UP_ACTION",
    )
    val swipeUpAction: Int = Constants.SwipeAction.SEARCH,

    @Setting(
        title = "Swipe Up App",
        category = Gestures::class,
        type = AppPicker::class,
        key = "SWIPE_UP_APP_JSON",
    )
    @Serialized
    val swipeUpApp: AppPreference = AppPreference(),

    @Persisted(key = "SWIPE_UP_FOLDER_ID")
    val swipeUpFolderId: String = "",

    @Setting(
        title = "Swipe Left Action",
        category = Gestures::class,
        type = Dropdown::class,
        options = ["None", "Search", "Notifications", "App", "Next Page", "Previous Page", "Open Folder", "Open Settings"],
        key = "SWIPE_LEFT_ACTION",
    )
    val swipeLeftAction: Int = Constants.SwipeAction.NULL,

    @Setting(
        title = "Left Swipe App",
        category = Gestures::class,
        type = AppPicker::class,
        key = "SWIPE_LEFT_APP_JSON",
    )
    @Serialized
    val swipeLeftApp: AppPreference = AppPreference(label = "Not set"),

    @Persisted(key = "SWIPE_LEFT_FOLDER_ID")
    val swipeLeftFolderId: String = "",

    @Setting(
        title = "Swipe Right Action",
        category = Gestures::class,
        type = Dropdown::class,
        options = ["None", "Search", "Notifications", "App", "Next Page", "Previous Page", "Open Folder", "Open Settings"],
        key = "SWIPE_RIGHT_ACTION",
    )
    val swipeRightAction: Int = Constants.SwipeAction.NULL,

    @Setting(
        title = "Right Swipe App",
        category = Gestures::class,
        type = AppPicker::class,
        key = "SWIPE_RIGHT_APP_JSON",
    )
    @Serialized
    val swipeRightApp: AppPreference = AppPreference(label = "Not set"),

    @Persisted(key = "SWIPE_RIGHT_FOLDER_ID")
    val swipeRightFolderId: String = "",

    @Setting(
        title = "Swipe Down Action",
        category = Gestures::class,
        type = Dropdown::class,
        options = ["None", "Search", "Notifications", "App", "Next Page", "Previous Page", "Open Folder", "Open Settings"],
        key = "PORTRAIT_SWIPE_DOWN_ACTION",
    )
    val portraitSwipeDownAction: Int = Constants.SwipeAction.NOTIFICATIONS,

    @Setting(
        title = "Swipe Down App",
        category = Gestures::class,
        type = AppPicker::class,
        key = "PORTRAIT_SWIPE_DOWN_APP_JSON",
    )
    @Serialized
    val portraitSwipeDownApp: AppPreference = AppPreference(),

    @Persisted(key = "PORTRAIT_SWIPE_DOWN_FOLDER_ID")
    val portraitSwipeDownFolderId: String = "",

    @Setting(
        title = "Swipe Up Action",
        category = Gestures::class,
        type = Dropdown::class,
        options = ["None", "Search", "Notifications", "App", "Next Page", "Previous Page", "Open Folder", "Open Settings"],
        key = "PORTRAIT_SWIPE_UP_ACTION",
    )
    val portraitSwipeUpAction: Int = Constants.SwipeAction.SEARCH,

    @Setting(
        title = "Swipe Up App",
        category = Gestures::class,
        type = AppPicker::class,
        key = "PORTRAIT_SWIPE_UP_APP_JSON",
    )
    @Serialized
    val portraitSwipeUpApp: AppPreference = AppPreference(),

    @Persisted(key = "PORTRAIT_SWIPE_UP_FOLDER_ID")
    val portraitSwipeUpFolderId: String = "",

    @Setting(
        title = "Swipe Left Action",
        category = Gestures::class,
        type = Dropdown::class,
        options = ["None", "Search", "Notifications", "App", "Next Page", "Previous Page", "Open Folder", "Open Settings"],
        key = "PORTRAIT_SWIPE_LEFT_ACTION",
    )
    val portraitSwipeLeftAction: Int = Constants.SwipeAction.NULL,

    @Setting(
        title = "Swipe Left App",
        category = Gestures::class,
        type = AppPicker::class,
        key = "PORTRAIT_SWIPE_LEFT_APP_JSON",
    )
    @Serialized
    val portraitSwipeLeftApp: AppPreference = AppPreference(label = "Not set"),

    @Persisted(key = "PORTRAIT_SWIPE_LEFT_FOLDER_ID")
    val portraitSwipeLeftFolderId: String = "",

    @Setting(
        title = "Swipe Right Action",
        category = Gestures::class,
        type = Dropdown::class,
        options = ["None", "Search", "Notifications", "App", "Next Page", "Previous Page", "Open Folder", "Open Settings"],
        key = "PORTRAIT_SWIPE_RIGHT_ACTION",
    )
    val portraitSwipeRightAction: Int = Constants.SwipeAction.NULL,

    @Setting(
        title = "Swipe Right App",
        category = Gestures::class,
        type = AppPicker::class,
        key = "PORTRAIT_SWIPE_RIGHT_APP_JSON",
    )
    @Serialized
    val portraitSwipeRightApp: AppPreference = AppPreference(label = "Not set"),

    @Persisted(key = "PORTRAIT_SWIPE_RIGHT_FOLDER_ID")
    val portraitSwipeRightFolderId: String = "",

    @Setting(
        title = "Swipe Down Action",
        category = Gestures::class,
        type = Dropdown::class,
        options = ["None", "Search", "Notifications", "App", "Next Page", "Previous Page", "Open Folder", "Open Settings"],
        key = "LANDSCAPE_SWIPE_DOWN_ACTION",
    )
    val landscapeSwipeDownAction: Int = Constants.SwipeAction.NOTIFICATIONS,

    @Setting(
        title = "Swipe Down App",
        category = Gestures::class,
        type = AppPicker::class,
        key = "LANDSCAPE_SWIPE_DOWN_APP_JSON",
    )
    @Serialized
    val landscapeSwipeDownApp: AppPreference = AppPreference(),

    @Persisted(key = "LANDSCAPE_SWIPE_DOWN_FOLDER_ID")
    val landscapeSwipeDownFolderId: String = "",

    @Setting(
        title = "Swipe Up Action",
        category = Gestures::class,
        type = Dropdown::class,
        options = ["None", "Search", "Notifications", "App", "Next Page", "Previous Page", "Open Folder", "Open Settings"],
        key = "LANDSCAPE_SWIPE_UP_ACTION",
    )
    val landscapeSwipeUpAction: Int = Constants.SwipeAction.SEARCH,

    @Setting(
        title = "Swipe Up App",
        category = Gestures::class,
        type = AppPicker::class,
        key = "LANDSCAPE_SWIPE_UP_APP_JSON",
    )
    @Serialized
    val landscapeSwipeUpApp: AppPreference = AppPreference(),

    @Persisted(key = "LANDSCAPE_SWIPE_UP_FOLDER_ID")
    val landscapeSwipeUpFolderId: String = "",

    @Setting(
        title = "Swipe Left Action",
        category = Gestures::class,
        type = Dropdown::class,
        options = ["None", "Search", "Notifications", "App", "Next Page", "Previous Page", "Open Folder", "Open Settings"],
        key = "LANDSCAPE_SWIPE_LEFT_ACTION",
    )
    val landscapeSwipeLeftAction: Int = Constants.SwipeAction.NULL,

    @Setting(
        title = "Swipe Left App",
        category = Gestures::class,
        type = AppPicker::class,
        key = "LANDSCAPE_SWIPE_LEFT_APP_JSON",
    )
    @Serialized
    val landscapeSwipeLeftApp: AppPreference = AppPreference(label = "Not set"),

    @Persisted(key = "LANDSCAPE_SWIPE_LEFT_FOLDER_ID")
    val landscapeSwipeLeftFolderId: String = "",

    @Setting(
        title = "Swipe Right Action",
        category = Gestures::class,
        type = Dropdown::class,
        options = ["None", "Search", "Notifications", "App", "Next Page", "Previous Page", "Open Folder", "Open Settings"],
        key = "LANDSCAPE_SWIPE_RIGHT_ACTION",
    )
    val landscapeSwipeRightAction: Int = Constants.SwipeAction.NULL,

    @Setting(
        title = "Swipe Right App",
        category = Gestures::class,
        type = AppPicker::class,
        key = "LANDSCAPE_SWIPE_RIGHT_APP_JSON",
    )
    @Serialized
    val landscapeSwipeRightApp: AppPreference = AppPreference(label = "Not set"),

    @Persisted(key = "LANDSCAPE_SWIPE_RIGHT_FOLDER_ID")
    val landscapeSwipeRightFolderId: String = "",

    @Persisted(key = "CORNER_ZONE_TOP_LEFT_JSON")
    @Serialized
    val cornerZoneTopLeft: CornerZoneConfig = CornerZoneConfig(),

    @Persisted(key = "CORNER_ZONE_TOP_RIGHT_JSON")
    @Serialized
    val cornerZoneTopRight: CornerZoneConfig = CornerZoneConfig(),

    @Persisted(key = "CORNER_ZONE_BOTTOM_LEFT_JSON")
    @Serialized
    val cornerZoneBottomLeft: CornerZoneConfig = CornerZoneConfig(),

    @Persisted(key = "CORNER_ZONE_BOTTOM_RIGHT_JSON")
    @Serialized
    val cornerZoneBottomRight: CornerZoneConfig = CornerZoneConfig(),

    @Persisted(key = "APPLY_TO_ALL_CORNER_ZONES")
    val applyToAllCornerZones: Boolean = false,

    /** Shared appearance profile used when applyToAllCornerZones == true. */
    @Persisted(key = "CORNER_ZONE_UNIVERSAL_JSON")
    @Serialized
    val cornerZoneUniversal: CornerZoneConfig = CornerZoneConfig(),

    /** Whether corner zones remain active while a folder overlay is open. */
    @Persisted(key = "CORNER_ZONES_IN_FOLDERS")
    val cornerZonesInFolders: Boolean = true,

    /** Show the danger-edge gradient fade on corner zones. */
    @Persisted(key = "CORNER_ZONE_DANGER_FADE")
    val cornerZoneDangerFade: Boolean = true,

    @Persisted(key = "PORTRAIT_CORNER_ZONE_TOP_LEFT_JSON")
    @Serialized
    val portraitCornerZoneTopLeft: CornerZoneConfig = CornerZoneConfig(),

    @Persisted(key = "PORTRAIT_CORNER_ZONE_TOP_RIGHT_JSON")
    @Serialized
    val portraitCornerZoneTopRight: CornerZoneConfig = CornerZoneConfig(),

    @Persisted(key = "PORTRAIT_CORNER_ZONE_BOTTOM_LEFT_JSON")
    @Serialized
    val portraitCornerZoneBottomLeft: CornerZoneConfig = CornerZoneConfig(),

    @Persisted(key = "PORTRAIT_CORNER_ZONE_BOTTOM_RIGHT_JSON")
    @Serialized
    val portraitCornerZoneBottomRight: CornerZoneConfig = CornerZoneConfig(),

    @Persisted(key = "PORTRAIT_APPLY_TO_ALL_CORNER_ZONES")
    val portraitApplyToAllCornerZones: Boolean = false,

    @Persisted(key = "PORTRAIT_CORNER_ZONE_UNIVERSAL_JSON")
    @Serialized
    val portraitCornerZoneUniversal: CornerZoneConfig = CornerZoneConfig(),

    @Persisted(key = "PORTRAIT_CORNER_ZONES_IN_FOLDERS")
    val portraitCornerZonesInFolders: Boolean = true,

    @Persisted(key = "PORTRAIT_CORNER_ZONE_DANGER_FADE")
    val portraitCornerZoneDangerFade: Boolean = true,

    @Persisted(key = "LANDSCAPE_CORNER_ZONE_TOP_LEFT_JSON")
    @Serialized
    val landscapeCornerZoneTopLeft: CornerZoneConfig = CornerZoneConfig(),

    @Persisted(key = "LANDSCAPE_CORNER_ZONE_TOP_RIGHT_JSON")
    @Serialized
    val landscapeCornerZoneTopRight: CornerZoneConfig = CornerZoneConfig(),

    @Persisted(key = "LANDSCAPE_CORNER_ZONE_BOTTOM_LEFT_JSON")
    @Serialized
    val landscapeCornerZoneBottomLeft: CornerZoneConfig = CornerZoneConfig(),

    @Persisted(key = "LANDSCAPE_CORNER_ZONE_BOTTOM_RIGHT_JSON")
    @Serialized
    val landscapeCornerZoneBottomRight: CornerZoneConfig = CornerZoneConfig(),

    @Persisted(key = "LANDSCAPE_APPLY_TO_ALL_CORNER_ZONES")
    val landscapeApplyToAllCornerZones: Boolean = false,

    @Persisted(key = "LANDSCAPE_CORNER_ZONE_UNIVERSAL_JSON")
    @Serialized
    val landscapeCornerZoneUniversal: CornerZoneConfig = CornerZoneConfig(),

    @Persisted(key = "LANDSCAPE_CORNER_ZONES_IN_FOLDERS")
    val landscapeCornerZonesInFolders: Boolean = true,

    @Persisted(key = "LANDSCAPE_CORNER_ZONE_DANGER_FADE")
    val landscapeCornerZoneDangerFade: Boolean = true,

    @Setting(
        title = "Show Folder Icon",
        description = "Show a small folder icon next to folder names on the home screen",
        category = Folders::class,
        type = Toggle::class,
        key = "SHOW_FOLDER_ICON",
    )
    val showFolderIcon: Boolean = true,

    @Setting(
        title = "Folder Icon Placement (Default)",
        description = "Default side for the folder icon on newly placed folders",
        category = Folders::class,
        type = Dropdown::class,
        options = ["Left", "Right"],
        key = "FOLDER_ICON_PLACEMENT",
    )
    val folderIconPlacement: Int = Constants.IconPlacement.LEFT,

    @Setting(
        title = "Folder Background Opacity",
        description = "Adjust the darkness of the folder overlay background (0.1 = nearly transparent, 0.9 = nearly opaque)",
        category = Folders::class,
        type = Slider::class,
        min = 0.1f,
        max = 0.9f,
        step = 0.1f,
        key = "FOLDER_BACKGROUND_OPACITY",
    )
    val folderBackgroundOpacity: Float = 0.6f,

    @Persisted(key = "FIRST_OPEN") val firstOpen: Boolean = true,
    @Persisted(key = "FIRST_OPEN_TIME") val firstOpenTime: Long = 0L,
    @Persisted(key = "FIRST_SETTINGS_OPEN") val firstSettingsOpen: Boolean = true,
    @Persisted(key = "FIRST_HIDE") val firstHide: Boolean = true,
    @Persisted(key = "USER_STATE") val userState: String = Constants.UserState.START,
    @Persisted(key = "LOCK_MODE") val lockMode: Boolean = false,
    @Persisted(key = "KEYBOARD_MESSAGE") val keyboardMessage: Boolean = false,

    // These were JSON strings before; kmp-settings has Map fields that will read/write the same JSON format.
    @Persisted(key = "RENAMED_APPS_JSON") val renamedApps: Map<String, String> = emptyMap(),
    @Persisted(key = "RECENT_APP_HISTORY") val recentAppHistory: Map<String, Long> = emptyMap(),
    @Persisted(key = "APP_TAGS_JSON") val appTagsJson: String = "",

    @Persisted(key = "HIDDEN_APPS") val hiddenApps: Set<String> = emptySet(),
    @Persisted(key = "HIDDEN_APPS_UPDATED") val hiddenAppsUpdated: Boolean = false,

    @Persisted(key = "SHOW_HINT_COUNTER") val showHintCounter: Int = 1,
    @Persisted(key = "ABOUT_CLICKED") val aboutClicked: Boolean = false,
    @Persisted(key = "RATE_CLICKED") val rateClicked: Boolean = false,
    @Persisted(key = "SHARE_SHOWN_TIME") val shareShownTime: Long = 0L,

    @Persisted(key = "ACCESSIBILITY_CONSENT") val accessibilityConsent: Boolean = false,

    // Settings lock is currently implemented in your app; keep persisted for now
    @Persisted(key = "LOCK_SETTINGS") val lockSettings: Boolean = false,
    @Persisted(key = "SETTINGS_LOCK_PIN") val settingsLockPin: String = "",

    // Home layout: move into kmp-settings (requires HomeLayout to be @Serializable)
    @Persisted(key = "HOME_LAYOUT_JSON")
    @Serialized
    val homeLayout: HomeLayout = HomeLayout(),

    @Persisted(key = "ORIENTATION_HOME_LAYOUTS_JSON")
    @Serialized
    val homeLayouts: OrientationHomeLayouts = OrientationHomeLayouts(),

    @Persisted(key = "ORIENTATION_AWARE_HOME_MIGRATED")
    val orientationAwareHomeMigrated: Boolean = false,

    @Persisted(key = "LANDSCAPE_HOME_DEFAULT_FIX_APPLIED")
    val landscapeHomeDefaultFixApplied: Boolean = false,
)

@Serializable
data class AppPreference(
    val label: String = "",
    val packageName: String = "",
    val activityClassName: String? = null,
    val userString: String = "",
    val isSystemShortcut: Boolean = false,
    val systemShortcutId: String? = null,
    val systemShortcutPackage: String? = null
)

data class AppKeyMigration(
    val newKey: String,
    val moveKeys: Set<String> = emptySet(),
    val copyKeys: Set<String> = emptySet()
)

fun AppSettings.isLandscapeHomeAvailable(): Boolean =
    landscapeLayoutEnabled || screenOrientation == 2

fun AppSettings.resolveHomeOrientation(isCurrentlyLandscape: Boolean): HomeOrientation =
    when (screenOrientation) {
        1 -> HomeOrientation.PORTRAIT
        2 -> HomeOrientation.LANDSCAPE
        else -> if (isCurrentlyLandscape && isLandscapeHomeAvailable()) {
            HomeOrientation.LANDSCAPE
        } else {
            HomeOrientation.PORTRAIT
        }
    }

fun AppSettings.availableHomeOrientations(): List<HomeOrientation> =
    buildList {
        add(HomeOrientation.PORTRAIT)
        if (isLandscapeHomeAvailable()) {
            add(HomeOrientation.LANDSCAPE)
        }
    }

fun AppSettings.homeRowsFor(orientation: HomeOrientation): Int =
    when (orientation) {
        HomeOrientation.PORTRAIT -> portraitHomeScreenRows
        HomeOrientation.LANDSCAPE -> landscapeHomeScreenRows
    }

fun AppSettings.homeColumnsFor(orientation: HomeOrientation): Int =
    when (orientation) {
        HomeOrientation.PORTRAIT -> portraitHomeScreenColumns
        HomeOrientation.LANDSCAPE -> landscapeHomeScreenColumns
    }

fun AppSettings.homePagesFor(orientation: HomeOrientation): Int =
    when (orientation) {
        HomeOrientation.PORTRAIT -> portraitHomeScreenPages
        HomeOrientation.LANDSCAPE -> landscapeHomeScreenPages
    }

fun AppSettings.swipeActionFor(orientation: HomeOrientation, direction: String): Int =
    when (orientation) {
        HomeOrientation.PORTRAIT -> when (direction) {
            "up" -> portraitSwipeUpAction
            "down" -> portraitSwipeDownAction
            "left" -> portraitSwipeLeftAction
            "right" -> portraitSwipeRightAction
            else -> Constants.SwipeAction.NULL
        }
        HomeOrientation.LANDSCAPE -> when (direction) {
            "up" -> landscapeSwipeUpAction
            "down" -> landscapeSwipeDownAction
            "left" -> landscapeSwipeLeftAction
            "right" -> landscapeSwipeRightAction
            else -> Constants.SwipeAction.NULL
        }
    }

fun AppSettings.swipeAppFor(orientation: HomeOrientation, direction: String): AppPreference =
    when (orientation) {
        HomeOrientation.PORTRAIT -> when (direction) {
            "up" -> portraitSwipeUpApp
            "down" -> portraitSwipeDownApp
            "left" -> portraitSwipeLeftApp
            "right" -> portraitSwipeRightApp
            else -> AppPreference()
        }
        HomeOrientation.LANDSCAPE -> when (direction) {
            "up" -> landscapeSwipeUpApp
            "down" -> landscapeSwipeDownApp
            "left" -> landscapeSwipeLeftApp
            "right" -> landscapeSwipeRightApp
            else -> AppPreference()
        }
    }

fun AppSettings.swipeFolderIdFor(orientation: HomeOrientation, direction: String): String =
    when (orientation) {
        HomeOrientation.PORTRAIT -> when (direction) {
            "up" -> portraitSwipeUpFolderId
            "down" -> portraitSwipeDownFolderId
            "left" -> portraitSwipeLeftFolderId
            "right" -> portraitSwipeRightFolderId
            else -> ""
        }
        HomeOrientation.LANDSCAPE -> when (direction) {
            "up" -> landscapeSwipeUpFolderId
            "down" -> landscapeSwipeDownFolderId
            "left" -> landscapeSwipeLeftFolderId
            "right" -> landscapeSwipeRightFolderId
            else -> ""
        }
    }

fun AppSettings.cornerConfigFor(orientation: HomeOrientation, corner: Int): CornerZoneConfig =
    when (orientation) {
        HomeOrientation.PORTRAIT -> when (corner) {
            Constants.CornerPosition.TOP_LEFT -> portraitCornerZoneTopLeft
            Constants.CornerPosition.TOP_RIGHT -> portraitCornerZoneTopRight
            Constants.CornerPosition.BOTTOM_LEFT -> portraitCornerZoneBottomLeft
            else -> portraitCornerZoneBottomRight
        }
        HomeOrientation.LANDSCAPE -> when (corner) {
            Constants.CornerPosition.TOP_LEFT -> landscapeCornerZoneTopLeft
            Constants.CornerPosition.TOP_RIGHT -> landscapeCornerZoneTopRight
            Constants.CornerPosition.BOTTOM_LEFT -> landscapeCornerZoneBottomLeft
            else -> landscapeCornerZoneBottomRight
        }
    }

fun AppSettings.cornerUniversalConfigFor(orientation: HomeOrientation): CornerZoneConfig =
    when (orientation) {
        HomeOrientation.PORTRAIT -> portraitCornerZoneUniversal
        HomeOrientation.LANDSCAPE -> landscapeCornerZoneUniversal
    }

fun AppSettings.applyToAllCornerZonesFor(orientation: HomeOrientation): Boolean =
    when (orientation) {
        HomeOrientation.PORTRAIT -> portraitApplyToAllCornerZones
        HomeOrientation.LANDSCAPE -> landscapeApplyToAllCornerZones
    }

fun AppSettings.cornerZonesInFoldersFor(orientation: HomeOrientation): Boolean =
    when (orientation) {
        HomeOrientation.PORTRAIT -> portraitCornerZonesInFolders
        HomeOrientation.LANDSCAPE -> landscapeCornerZonesInFolders
    }

fun AppSettings.cornerZoneDangerFadeFor(orientation: HomeOrientation): Boolean =
    when (orientation) {
        HomeOrientation.PORTRAIT -> portraitCornerZoneDangerFade
        HomeOrientation.LANDSCAPE -> landscapeCornerZoneDangerFade
    }

@CategoryDefinition(order = 0) object General
@CategoryDefinition(order = 1) object Appearance
@CategoryDefinition(order = 2) object Layout
@CategoryDefinition(order = 3) object Gestures
@CategoryDefinition(order = 4) object System
@CategoryDefinition(order = 5) object Folders

object FontPicker : SettingTypeMarker
object AppPicker : SettingTypeMarker
object IconPackPicker : SettingTypeMarker
object ColorPicker : SettingTypeMarker
