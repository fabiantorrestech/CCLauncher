package app.cclauncher.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID

@Serializable
@Immutable
sealed class HomeItem {
    abstract val id: String
    abstract val page: Int
    abstract val row: Int
    abstract val column: Int
    abstract val rowSpan: Int
    abstract val columnSpan: Int

    @Serializable(with = HomeItemAppSerializer::class)
    data class App(
        val appModel: AppModel,
        override val id: String = appModel.getKey(),
        override val page: Int = 0,
        override val row: Int,
        override val column: Int,
        override val rowSpan: Int = 1,
        override val columnSpan: Int = 1,
    ) : HomeItem()

    @Serializable(with = HomeItemWidgetSerializer::class)
    data class Widget(
        val appWidgetId: Int,
        @Transient
        val providerInfo: android.appwidget.AppWidgetProviderInfo? = null,
        val packageName: String,
        val providerClassName: String,
        override val id: String = "widget_$appWidgetId",
        override val page: Int = 0,
        override val row: Int,
        override val column: Int,
        override val rowSpan: Int,
        override val columnSpan: Int,
    ) : HomeItem()

    @Serializable(with = HomeItemFolderSerializer::class)
    data class Folder(
        val title: String,
        val apps: List<FolderApp> = emptyList(),
        val gridRows: Int = Constants.GridSize.DEFAULT_ROWS,
        val gridColumns: Int = Constants.GridSize.DEFAULT_COLUMNS,
        val appTextSize: Float = 1.0f,
        override val id: String = "folder_${UUID.randomUUID()}",
        override val page: Int = 0,
        override val row: Int,
        override val column: Int,
        override val rowSpan: Int = 1,
        override val columnSpan: Int = 1,
    ) : HomeItem()
}

/** A lightweight app reference stored inside a folder. Position fields place it in the folder's internal grid. */
@Serializable
data class FolderApp(
    val appLabel: String,
    val appPackage: String,
    val activityClassName: String? = null,
    val userString: String = "",
    val row: Int = 0,
    val column: Int = 0,
    val rowSpan: Int = 1,
    val columnSpan: Int = 1,
    val isSystemShortcut: Boolean = false,
    val systemShortcutId: String? = null,
    val systemShortcutPackage: String? = null,
) {
    fun toAppModel() = AppModel(
        appLabel = appLabel,
        appPackage = appPackage,
        activityClassName = activityClassName?.takeIf { it.isNotBlank() },
        userString = userString,
        isSystemShortcut = isSystemShortcut,
        systemShortcutId = systemShortcutId,
        systemShortcutPackage = systemShortcutPackage,
    )
}

fun AppModel.toFolderApp(row: Int, column: Int) = FolderApp(
    appLabel = appLabel,
    appPackage = appPackage,
    activityClassName = activityClassName,
    userString = userString,
    row = row,
    column = column,
    isSystemShortcut = isSystemShortcut,
    systemShortcutId = systemShortcutId,
    systemShortcutPackage = systemShortcutPackage,
)

@Serializable
data class HomeLayout(
    val items: List<HomeItem> = emptyList(),
    val rows: Int = 8,
    val columns: Int = 4,
    val pageCount: Int = 1
) {
    fun itemsForPage(page: Int): List<HomeItem> = items.filter { it.page == page }

    fun isPageEmpty(page: Int): Boolean = items.none { it.page == page }

    fun lastNonEmptyPage(): Int = items.maxOfOrNull { it.page } ?: 0
}