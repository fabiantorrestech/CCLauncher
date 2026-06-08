package app.cclauncher.helper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import app.cclauncher.data.AppModel
import org.koin.compose.koinInject

/**
 * Loads the icon for an AppModel from the shared IconCache singleton.
 *
 * The cache is byte-bounded (see IconCache.ICON_CACHE_MAX_BYTES), so we can call this freely
 * from list items without retaining bitmaps in StateFlows or AppModel instances.
 *
 * Returns null while loading and when [enabled] is false (the caller can branch on that to
 * skip rendering an icon slot entirely).
 */
@Composable
fun rememberAppIcon(
    app: AppModel,
    iconPackName: String,
    enabled: Boolean = true
): ImageBitmap? {
    val iconCache: IconCache = koinInject()
    val cacheKey = app.getKey()
    var icon by remember(cacheKey, iconPackName, enabled) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(cacheKey, iconPackName, enabled) {
        if (!enabled) {
            icon = null
            return@LaunchedEffect
        }
        icon = if (app.isSystemShortcut) {
            iconCache.getShortcutIcon(app.systemShortcutPackage, app.systemShortcutId, app.user)
        } else {
            iconCache.getIcon(
                packageName = app.appPackage,
                className = app.activityClassName,
                user = app.user,
                iconPackName = iconPackName
            )
        }
    }

    return icon
}
