package app.cclauncher.data

object Constants {
    const val FLAG_LAUNCH_APP = 100
    const val FLAG_HIDDEN_APPS = 101

    const val FLAG_SET_SWIPE_LEFT_APP = 17
    const val FLAG_SET_SWIPE_RIGHT_APP = 18
    const val FLAG_SET_SWIPE_UP_APP = 19
    const val FLAG_SET_SWIPE_DOWN_APP = 20
    const val FLAG_SET_PORTRAIT_SWIPE_LEFT_APP = 21
    const val FLAG_SET_PORTRAIT_SWIPE_RIGHT_APP = 22
    const val FLAG_SET_PORTRAIT_SWIPE_UP_APP = 23
    const val FLAG_SET_PORTRAIT_SWIPE_DOWN_APP = 24
    const val FLAG_SET_LANDSCAPE_SWIPE_LEFT_APP = 25
    const val FLAG_SET_LANDSCAPE_SWIPE_RIGHT_APP = 26
    const val FLAG_SET_LANDSCAPE_SWIPE_UP_APP = 27
    const val FLAG_SET_LANDSCAPE_SWIPE_DOWN_APP = 28

    const val HINT_RATE_US = 25
    const val CUSTOM_FONT_FILENAME = "custom_font.ttf"

    const val URL_ABOUT_CCLAUNCHER = "https://github.com/mlm-games/CCLauncher"
    const val URL_CCLAUNCHER_PRIVACY = "https://github.com/mlm-games/CCLauncher"
    const val URL_DOUBLE_TAP = ""
    const val URL_CCLAUNCHER_GITHUB = "https://github.com/mlm-games/CCLauncher"
    const val URL_DUCK_SEARCH = "https://duckduckgo.com?q="

    const val MAX_PAGES = 5

    object Key {
        const val FLAG = "flag"
        const val RENAME = "rename"
    }

    object Dialog {
        const val ABOUT = "ABOUT"
    }

    object UserState {
        const val START = "START"
        const val REVIEW = "REVIEW"
        const val RATE = "RATE"
        const val SHARE = "SHARE"
    }

    object SortOrder {
        const val ALPHABETICAL = 0
        const val REVERSE_ALPHABETICAL = 1
        const val RECENT_FIRST = 2
    }

    object SwipeAction {
        const val NULL = 0
        const val SEARCH = 1
        const val NOTIFICATIONS = 2
        const val APP = 3
        const val NEXT_PAGE = 4
        const val PREVIOUS_PAGE = 5
        const val OPEN_FOLDER = 6
        const val OPEN_SETTINGS = 7
    }

    object CornerPosition {
        const val TOP_LEFT = 0
        const val TOP_RIGHT = 1
        const val BOTTOM_LEFT = 2
        const val BOTTOM_RIGHT = 3
    }

    enum class ZoneSwipeDir { LEFT, RIGHT, UP, DOWN }

    /** Returns the swipe directions that are valid for a given corner (point into the screen interior). */
    fun validSwipeDirs(cornerPos: Int): Set<ZoneSwipeDir> = when (cornerPos) {
        CornerPosition.TOP_LEFT     -> setOf(ZoneSwipeDir.RIGHT, ZoneSwipeDir.DOWN)
        CornerPosition.TOP_RIGHT    -> setOf(ZoneSwipeDir.LEFT, ZoneSwipeDir.DOWN)
        CornerPosition.BOTTOM_LEFT  -> setOf(ZoneSwipeDir.RIGHT, ZoneSwipeDir.UP)
        else                        -> setOf(ZoneSwipeDir.LEFT, ZoneSwipeDir.UP) // BOTTOM_RIGHT
    }

    object GridSize {
        const val MIN_ROWS = 4
        const val MAX_ROWS = 24
        const val MIN_COLUMNS = 2
        const val MAX_COLUMNS = 16
        const val MAX_LANDSCAPE_ROWS = 16
        const val MAX_LANDSCAPE_COLUMNS = 24
        const val DEFAULT_ROWS = 8
        const val DEFAULT_COLUMNS = 4
    }

    object TextSize {
        const val ONE = 0.6f
        const val TWO = 0.75f
        const val THREE = 0.9f
        const val FOUR = 1f
        const val FIVE = 1.15f
        const val SIX = 1.3f
        const val SEVEN = 1.45f
    }

    object SearchType {
        const val CONTAINS = 0
        const val FUZZY = 1
        const val STARTS_WITH = 2
        const val EXACT = 3
    }

    object SearchBarPlacement {
        const val TOP = 0
        const val BOTTOM = 1
    }

    object AppDrawerAlignment {
        const val LEFT = 0
        const val RIGHT = 1
    }

    object IconPlacement {
        const val LEFT = 0
        const val RIGHT = 1
    }

    object LandscapeDrawerStyle {
        const val SCROLLABLE_LIST = 0
        const val PAGED_GRID = 1
    }
}

object WidgetConstants {
    const val APPWIDGET_HOST_ID = 1024
    const val REQUEST_CONFIGURE_WIDGET = 1001
    const val REQUEST_CODE_BIND_WIDGET = 102
    const val DEFAULT_WIDGET_SIZE = 48
}

object AnimationConstants {
    const val STANDARD_DURATION_MS = 300
    const val LONG_PRESS_DELAY_MS = 500L
    const val DOUBLE_TAP_DELAY_MS = 300L
    const val ONE_DAY_IN_MILLIS = 86400000L
    const val ONE_HOUR_IN_MILLIS = 3600000L
    const val ONE_MINUTE_IN_MILLIS = 60000L
    const val MIN_ANIM_REFRESH_RATE = 10f
}

object RequestCodes {
    const val ENABLE_ADMIN = 666
    const val LAUNCHER_SELECTOR = 678
}
