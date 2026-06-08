package app.cclauncher.helper

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.os.Build
import android.os.UserHandle
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.cclauncher.helper.iconpack.IconPackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IconCache(context: Context) {
    private val appContext = context.applicationContext
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    // cacheKey = "pack|package|class|userHash"
    // Byte-sized LRU: a 192² ARGB_8888 icon is ~148 KB; 8 MB holds ~55 such icons, or many more
    // smaller ones. Bound by memory, not item count, so it doesn't blow up on huge launchers.
    private val iconCache = object : LruCache<String, Bitmap>(ICON_CACHE_MAX_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    private val iconPackManager = IconPackManager(context)

    suspend fun getAvailableIconPacks() = iconPackManager.getAvailableIconPacks()

    suspend fun getIcon(
        packageName: String,
        className: String?,
        user: UserHandle,
        iconPackName: String = "default"
    ): ImageBitmap? = withContext(Dispatchers.IO) {

        // Resolve class name if null.
        val resolvedClassName = runCatching {
            val list = launcherApps.getActivityList(packageName, user)
            val info = list.firstOrNull { className == null || it.componentName.className == className }
                ?: list.firstOrNull()
            info?.componentName?.className
        }.getOrNull() ?: className

        val cacheKey = "$iconPackName|$packageName|$resolvedClassName|${user.hashCode()}"
        synchronized(iconCache) {
            iconCache[cacheKey]?.let { return@withContext it.asImageBitmap() }
        }

        val activityInfo = runCatching {
            launcherApps.getActivityList(packageName, user)
                .firstOrNull { resolvedClassName != null && it.componentName.className == resolvedClassName }
                ?: launcherApps.getActivityList(packageName, user).firstOrNull()
        }.getOrNull()

        val originalDrawable = runCatching { activityInfo?.getIcon(0) }.getOrNull()
        val componentName = if (resolvedClassName.isNullOrBlank()) {
            "$packageName/"
        } else {
            "$packageName/$resolvedClassName"
        }

        val finalBitmap: Bitmap? = when {
            iconPackName != "default" -> {
                iconPackManager.getBitmapFromPack(iconPackName, componentName)
                    ?: originalDrawable?.let { BitmapUtils.drawableToBitmap(it) }
            }
            else -> originalDrawable?.let { BitmapUtils.drawableToBitmap(it) }
        }

        finalBitmap?.let { bmp ->
            synchronized(iconCache) { iconCache.put(cacheKey, bmp) }
            return@withContext bmp.asImageBitmap()
        }

        null
    }

    /**
     * Icon loader for pinned system shortcuts. Goes through the same byte-sized LRU as
     * regular app icons so memory stays bounded when many shortcuts are on the home screen.
     */
    suspend fun getShortcutIcon(
        packageName: String?,
        shortcutId: String?,
        user: UserHandle
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        if (packageName.isNullOrBlank() || shortcutId.isNullOrBlank()) return@withContext null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return@withContext null
        if (!launcherApps.hasShortcutHostPermission()) return@withContext null

        val cacheKey = "shortcut|$packageName|$shortcutId|${user.hashCode()}"
        synchronized(iconCache) {
            iconCache[cacheKey]?.let { return@withContext it.asImageBitmap() }
        }

        val query = LauncherApps.ShortcutQuery()
            .setPackage(packageName)
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
        val shortcut = runCatching { launcherApps.getShortcuts(query, user) }
            .getOrNull()
            .orEmpty()
            .firstOrNull { it.id == shortcutId }
            ?: return@withContext null

        val drawable = runCatching {
            launcherApps.getShortcutIconDrawable(shortcut, appContext.resources.displayMetrics.densityDpi)
        }.getOrNull() ?: return@withContext null

        val bmp = BitmapUtils.drawableToBitmap(drawable) ?: return@withContext null

        synchronized(iconCache) { iconCache.put(cacheKey, bmp) }
        bmp.asImageBitmap()
    }

    fun clearCache() {
        synchronized(iconCache) {
            iconCache.evictAll()
        }
        iconPackManager.clearCache()
    }

    companion object {
        private const val ICON_CACHE_MAX_BYTES = 8 * 1024 * 1024 // 8 MB
    }
}