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
        val appTextSize: Float = 1.0f,
        val appLabelAlignment: Int = -1,
        val iconPlacement: Int = Constants.IconPlacement.LEFT,
        val labelFontPath: String = "",
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
        val isPlaceholder: Boolean = false,
        val appName: String = "",
        val widgetName: String = "",
        val intendedColumnSpan: Int = 0,
        val intendedRowSpan: Int = 0,
        val sourceDensityDpi: Int = 0,
        override val id: String = "widget_$appWidgetId",
        override val page: Int = 0,
        override val row: Int,
        override val column: Int,
        override val rowSpan: Int,
        override val columnSpan: Int,
    ) : HomeItem() {
        fun placeholderPrimaryLines(): List<String> {
            val appDisplay = listOf(packageName, appName.takeIf { it.isNotBlank() })
                .filterNotNull()
                .joinToString(" • ")
                .ifBlank { packageName.ifBlank { "Unknown app" } }
            val widgetDisplay = buildString {
                append(widgetName.ifBlank { providerClassName.substringAfterLast('.') })
                val intendedSize = intendedSizeLabel()
                val density = sourceDensityDpi.takeIf { it > 0 }?.let { "$it dpi" }
                val extras = listOfNotNull(intendedSize, density)
                if (extras.isNotEmpty()) {
                    append(" • ")
                    append(extras.joinToString(" • "))
                }
            }.ifBlank { "Unknown widget" }

            return listOf(
                appDisplay,
                widgetDisplay,
                "Imported as ${columnSpan}x${rowSpan}"
            )
        }

        fun placeholderDisplayText(includeProviderClassName: Boolean = true): String {
            val lines = placeholderPrimaryLines().map { "• $it" }.toMutableList()
            if (includeProviderClassName && providerClassName.isNotBlank()) {
                lines += "• Provider: $providerClassName"
            }
            return lines.joinToString("\n")
        }

        fun intendedSizeLabel(): String? {
            if (intendedColumnSpan <= 0 || intendedRowSpan <= 0) return null
            return "${intendedColumnSpan}x${intendedRowSpan} default"
        }
    }

    @Serializable(with = HomeItemFolderSerializer::class)
    data class Folder(
        val title: String,
        val apps: List<FolderApp> = emptyList(),
        val gridRows: Int = Constants.GridSize.DEFAULT_ROWS,
        val gridColumns: Int = Constants.GridSize.DEFAULT_COLUMNS,
        val appTextSize: Float = 1.0f,
        val titleTextSize: Float = 1.0f,
        val titleTextColor: Int = 0,
        val titleLabelAlignment: Int = -1,
        val iconPlacement: Int = Constants.IconPlacement.LEFT,
        val titleFontPath: String = "",
        val defaultAppFontPath: String = "",
        /** When false the folder tile is hidden from the home grid but can still be opened via gestures/dots. */
        val showOnHome: Boolean = true,
        /** Hide the folder name text in the overlay header. */
        val hideTitle: Boolean = true,
        /** Hide the close (X) button in the overlay header. */
        val hideCloseButton: Boolean = true,
        /** Hide the rounded-rectangle border around the folder overlay. */
        val hideOutline: Boolean = true,
        /** Dismiss the folder by tapping any unoccupied space (backdrop or empty grid cell). */
        val tapOutsideToClose: Boolean = true,
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
    val appTextSize: Float = 1.0f,
    val appLabelAlignment: Int = -1,
    val iconPlacement: Int = Constants.IconPlacement.LEFT,
    val labelFontPath: String = "",
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

fun AppModel.toFolderApp(
    row: Int,
    column: Int,
    iconPlacement: Int = Constants.IconPlacement.LEFT,
    labelFontPath: String = "",
) = FolderApp(
    appLabel = appLabel,
    appPackage = appPackage,
    activityClassName = activityClassName,
    userString = userString,
    row = row,
    column = column,
    isSystemShortcut = isSystemShortcut,
    systemShortcutId = systemShortcutId,
    systemShortcutPackage = systemShortcutPackage,
    iconPlacement = iconPlacement,
    labelFontPath = labelFontPath,
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
