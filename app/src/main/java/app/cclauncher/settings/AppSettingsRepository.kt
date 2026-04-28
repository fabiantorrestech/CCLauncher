package app.cclauncher.settings

import android.content.Context
import android.content.pm.PackageManager
import android.provider.OpenableColumns
import android.net.Uri
import android.os.Build
import android.util.Log
import app.cclauncher.data.HomeLayout
import app.cclauncher.data.HomeItem
import app.cclauncher.data.HomeOrientation
import app.cclauncher.data.OrientationHomeLayouts
import app.cclauncher.helper.AppTagStorage
import app.cclauncher.helper.AppTagUtils
import io.github.mlmgames.settings.core.SettingsRepository
import io.github.mlmgames.settings.core.backup.DeviceInfo
import io.github.mlmgames.settings.core.backup.ExportResult
import io.github.mlmgames.settings.core.backup.ImportOptions
import io.github.mlmgames.settings.core.backup.ImportResult
import io.github.mlmgames.settings.core.backup.SettingsBackupManager
import io.github.mlmgames.settings.core.backup.ValidationResult
import io.github.mlmgames.settings.core.datastore.createSettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Clock

enum class WidgetImportMode {
    PLACEHOLDERS,
    WIDGETS,
}

class AppSettingsRepository(private val context: Context): KoinComponent {
    private var activeHomeOrientation: HomeOrientation = HomeOrientation.PORTRAIT

    private val dataStore = createSettingsDataStore(context, name = "app.cclauncher.settings")
    private val repo = SettingsRepository(dataStore, AppSettingsSchema)

    private val backupManager by lazy {
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            null
        }
        val versionName = packageInfo?.versionName ?: "unknown"

        SettingsBackupManager(
            dataStore = dataStore,
            schema = AppSettingsSchema,
            appId = "app.cclauncher",
            schemaVersion = 1,
            deviceInfoProvider = {
                DeviceInfo(
                    platform = "Android",
                    osVersion = "API ${Build.VERSION.SDK_INT}",
                    appVersion = versionName
                )
            }
        )
    }

    val settings: Flow<AppSettings> = repo.flow

    fun setActiveHomeOrientation(orientation: HomeOrientation) {
        activeHomeOrientation = orientation
    }

    fun getActiveHomeOrientation(): HomeOrientation = activeHomeOrientation

    private fun readAppTags(settings: AppSettings): Map<String, List<String>> =
        AppTagStorage.decode(settings.appTagsJson)

    private fun writeAppTags(tagsByApp: Map<String, List<String>>): String =
        AppTagStorage.encode(tagsByApp)

    private fun migrateOrientationAwareSettings(settings: AppSettings): AppSettings {
        val portraitLayout = settings.homeLayout.copy(
            rows = settings.homeScreenRows,
            columns = settings.homeScreenColumns,
            pageCount = settings.homeScreenPages
        )
        val landscapeLayout = HomeLayout(
            items = emptyList(),
            rows = settings.landscapeHomeScreenRows,
            columns = settings.landscapeHomeScreenColumns,
            pageCount = settings.landscapeHomeScreenPages
        )

        return settings.copy(
            landscapeLayoutEnabled = settings.landscapeLayoutEnabled,
            portraitHomeScreenRows = settings.homeScreenRows,
            portraitHomeScreenColumns = settings.homeScreenColumns,
            portraitHomeScreenPages = settings.homeScreenPages,
            portraitSwipeDownAction = settings.swipeDownAction,
            portraitSwipeDownApp = settings.swipeDownApp,
            portraitSwipeDownFolderId = settings.swipeDownFolderId,
            portraitSwipeUpAction = settings.swipeUpAction,
            portraitSwipeUpApp = settings.swipeUpApp,
            portraitSwipeUpFolderId = settings.swipeUpFolderId,
            portraitSwipeLeftAction = settings.swipeLeftAction,
            portraitSwipeLeftApp = settings.swipeLeftApp,
            portraitSwipeLeftFolderId = settings.swipeLeftFolderId,
            portraitSwipeRightAction = settings.swipeRightAction,
            portraitSwipeRightApp = settings.swipeRightApp,
            portraitSwipeRightFolderId = settings.swipeRightFolderId,
            landscapeSwipeDownAction = settings.swipeDownAction,
            landscapeSwipeDownApp = settings.swipeDownApp,
            landscapeSwipeDownFolderId = settings.swipeDownFolderId,
            landscapeSwipeUpAction = settings.swipeUpAction,
            landscapeSwipeUpApp = settings.swipeUpApp,
            landscapeSwipeUpFolderId = settings.swipeUpFolderId,
            landscapeSwipeLeftAction = settings.swipeLeftAction,
            landscapeSwipeLeftApp = settings.swipeLeftApp,
            landscapeSwipeLeftFolderId = settings.swipeLeftFolderId,
            landscapeSwipeRightAction = settings.swipeRightAction,
            landscapeSwipeRightApp = settings.swipeRightApp,
            landscapeSwipeRightFolderId = settings.swipeRightFolderId,
            portraitCornerZoneTopLeft = settings.cornerZoneTopLeft,
            portraitCornerZoneTopRight = settings.cornerZoneTopRight,
            portraitCornerZoneBottomLeft = settings.cornerZoneBottomLeft,
            portraitCornerZoneBottomRight = settings.cornerZoneBottomRight,
            portraitApplyToAllCornerZones = settings.applyToAllCornerZones,
            portraitCornerZoneUniversal = settings.cornerZoneUniversal,
            portraitCornerZonesInFolders = settings.cornerZonesInFolders,
            portraitCornerZoneDangerFade = settings.cornerZoneDangerFade,
            landscapeCornerZoneTopLeft = settings.cornerZoneTopLeft,
            landscapeCornerZoneTopRight = settings.cornerZoneTopRight,
            landscapeCornerZoneBottomLeft = settings.cornerZoneBottomLeft,
            landscapeCornerZoneBottomRight = settings.cornerZoneBottomRight,
            landscapeApplyToAllCornerZones = settings.applyToAllCornerZones,
            landscapeCornerZoneUniversal = settings.cornerZoneUniversal,
            landscapeCornerZonesInFolders = settings.cornerZonesInFolders,
            landscapeCornerZoneDangerFade = settings.cornerZoneDangerFade,
            homeLayout = portraitLayout,
            homeLayouts = OrientationHomeLayouts(
                portrait = portraitLayout,
                landscape = landscapeLayout
            ),
            orientationAwareHomeMigrated = true,
            landscapeHomeDefaultFixApplied = true
        )
    }

    private fun repairLandscapeDefaultRegression(settings: AppSettings): AppSettings {
        if (settings.landscapeHomeDefaultFixApplied) return settings

        val portrait = settings.homeLayouts.portrait
        val landscape = settings.homeLayouts.landscape
        val shouldClearMirroredLandscape =
            !settings.landscapeLayoutEnabled &&
                landscape.items.isNotEmpty() &&
                landscape.items == portrait.items

        return if (shouldClearMirroredLandscape) {
            settings.copy(
                homeLayouts = settings.homeLayouts.copy(
                    landscape = HomeLayout(
                        items = emptyList(),
                        rows = settings.landscapeHomeScreenRows,
                        columns = settings.landscapeHomeScreenColumns,
                        pageCount = settings.landscapeHomeScreenPages
                    )
                ),
                landscapeHomeDefaultFixApplied = true
            )
        } else {
            settings.copy(landscapeHomeDefaultFixApplied = true)
        }
    }

    suspend fun updateSetting(propertyName: String, value: Any) {
        repo.set(propertyName, value)
    }

    suspend fun updateSetting(update: (AppSettings) -> AppSettings) {
        repo.update(update)
    }

    suspend fun ensureOrientationAwareMigration() {
        repo.update { current ->
            val migrated = if (current.orientationAwareHomeMigrated) current else migrateOrientationAwareSettings(current)
            repairLandscapeDefaultRegression(migrated)
        }
    }

    fun getHomeLayouts(): Flow<OrientationHomeLayouts> =
        settings.map { it.homeLayouts }.distinctUntilChanged()

    fun getHomeLayout(): Flow<HomeLayout> = getHomeLayout(activeHomeOrientation)

    fun getHomeLayout(orientation: HomeOrientation): Flow<HomeLayout> =
        settings
            .map { appSettings ->
                appSettings.homeLayouts.layoutFor(orientation).copy(
                    rows = appSettings.homeRowsFor(orientation),
                    columns = appSettings.homeColumnsFor(orientation),
                    pageCount = appSettings.homePagesFor(orientation)
                )
            }
            .distinctUntilChanged()

    suspend fun saveHomeLayout(orientation: HomeOrientation, layout: HomeLayout) {
        repo.update { settings ->
            val normalized = layout.copy(
                rows = settings.homeRowsFor(orientation),
                columns = settings.homeColumnsFor(orientation),
                pageCount = settings.homePagesFor(orientation)
            )
            val updatedLayouts = settings.homeLayouts.withLayout(orientation, normalized)
            settings.copy(
                homeLayout = updatedLayouts.portrait,
                homeLayouts = updatedLayouts
            )
        }
    }

    suspend fun saveHomeLayout(layout: HomeLayout) {
        saveHomeLayout(activeHomeOrientation, layout)
    }

    suspend fun updateHomeLayouts(
        transform: (AppSettings, OrientationHomeLayouts) -> OrientationHomeLayouts
    ) {
        repo.update { settings ->
            val updatedLayouts = transform(settings, settings.homeLayouts)
            settings.copy(
                homeLayout = updatedLayouts.portrait,
                homeLayouts = updatedLayouts
            )
        }
    }

    suspend fun triggerHomeLayoutRefresh(orientation: HomeOrientation) {
        val currentLayout = getHomeLayout(orientation).first()
        saveHomeLayout(orientation, currentLayout)
    }

    suspend fun triggerHomeLayoutRefresh() {
        triggerHomeLayoutRefresh(activeHomeOrientation)
    }

    suspend fun updateHomePageSetting(orientation: HomeOrientation, pageCount: Int) {
        when (orientation) {
            HomeOrientation.PORTRAIT -> repo.set("portraitHomeScreenPages", pageCount)
            HomeOrientation.LANDSCAPE -> repo.set("landscapeHomeScreenPages", pageCount)
        }
    }

    suspend fun setSwipeLeftApp(orientation: HomeOrientation, app: AppPreference) {
        repo.update { s ->
            when (orientation) {
                HomeOrientation.PORTRAIT -> s.copy(portraitSwipeLeftApp = app)
                HomeOrientation.LANDSCAPE -> s.copy(landscapeSwipeLeftApp = app)
            }
        }
    }

    suspend fun setSwipeLeftApp(app: AppPreference) = setSwipeLeftApp(activeHomeOrientation, app)

    suspend fun setSwipeRightApp(orientation: HomeOrientation, app: AppPreference) {
        repo.update { s ->
            when (orientation) {
                HomeOrientation.PORTRAIT -> s.copy(portraitSwipeRightApp = app)
                HomeOrientation.LANDSCAPE -> s.copy(landscapeSwipeRightApp = app)
            }
        }
    }

    suspend fun setSwipeRightApp(app: AppPreference) = setSwipeRightApp(activeHomeOrientation, app)

    suspend fun setSwipeUpApp(orientation: HomeOrientation, app: AppPreference) {
        repo.update { s ->
            when (orientation) {
                HomeOrientation.PORTRAIT -> s.copy(portraitSwipeUpApp = app)
                HomeOrientation.LANDSCAPE -> s.copy(landscapeSwipeUpApp = app)
            }
        }
    }

    suspend fun setSwipeUpApp(app: AppPreference) = setSwipeUpApp(activeHomeOrientation, app)

    suspend fun setSwipeDownApp(orientation: HomeOrientation, app: AppPreference) {
        repo.update { s ->
            when (orientation) {
                HomeOrientation.PORTRAIT -> s.copy(portraitSwipeDownApp = app)
                HomeOrientation.LANDSCAPE -> s.copy(landscapeSwipeDownApp = app)
            }
        }
    }

    suspend fun setSwipeDownApp(app: AppPreference) = setSwipeDownApp(activeHomeOrientation, app)

    suspend fun setSwipeFolderId(orientation: HomeOrientation, direction: String, folderId: String) {
        repo.update { s ->
            when (orientation) {
                HomeOrientation.PORTRAIT -> when (direction) {
                    "up" -> s.copy(portraitSwipeUpFolderId = folderId)
                    "down" -> s.copy(portraitSwipeDownFolderId = folderId)
                    "left" -> s.copy(portraitSwipeLeftFolderId = folderId)
                    "right" -> s.copy(portraitSwipeRightFolderId = folderId)
                    else -> s
                }
                HomeOrientation.LANDSCAPE -> when (direction) {
                    "up" -> s.copy(landscapeSwipeUpFolderId = folderId)
                    "down" -> s.copy(landscapeSwipeDownFolderId = folderId)
                    "left" -> s.copy(landscapeSwipeLeftFolderId = folderId)
                    "right" -> s.copy(landscapeSwipeRightFolderId = folderId)
                    else -> s
                }
            }
        }
    }

    suspend fun setSwipeFolderId(direction: String, folderId: String) =
        setSwipeFolderId(activeHomeOrientation, direction, folderId)

    suspend fun updateCornerZoneConfig(orientation: HomeOrientation, corner: Int, config: CornerZoneConfig) {
        repo.update { s ->
            when (orientation) {
                HomeOrientation.PORTRAIT -> when (corner) {
                    app.cclauncher.data.Constants.CornerPosition.TOP_LEFT -> s.copy(portraitCornerZoneTopLeft = config)
                    app.cclauncher.data.Constants.CornerPosition.TOP_RIGHT -> s.copy(portraitCornerZoneTopRight = config)
                    app.cclauncher.data.Constants.CornerPosition.BOTTOM_LEFT -> s.copy(portraitCornerZoneBottomLeft = config)
                    app.cclauncher.data.Constants.CornerPosition.BOTTOM_RIGHT -> s.copy(portraitCornerZoneBottomRight = config)
                    else -> s
                }
                HomeOrientation.LANDSCAPE -> when (corner) {
                    app.cclauncher.data.Constants.CornerPosition.TOP_LEFT -> s.copy(landscapeCornerZoneTopLeft = config)
                    app.cclauncher.data.Constants.CornerPosition.TOP_RIGHT -> s.copy(landscapeCornerZoneTopRight = config)
                    app.cclauncher.data.Constants.CornerPosition.BOTTOM_LEFT -> s.copy(landscapeCornerZoneBottomLeft = config)
                    app.cclauncher.data.Constants.CornerPosition.BOTTOM_RIGHT -> s.copy(landscapeCornerZoneBottomRight = config)
                    else -> s
                }
            }
        }
    }

    suspend fun updateCornerZoneConfig(corner: Int, config: CornerZoneConfig) =
        updateCornerZoneConfig(activeHomeOrientation, corner, config)

    suspend fun updateUniversalCornerZoneConfig(orientation: HomeOrientation, config: CornerZoneConfig) {
        repo.update {
            when (orientation) {
                HomeOrientation.PORTRAIT -> it.copy(portraitCornerZoneUniversal = config)
                HomeOrientation.LANDSCAPE -> it.copy(landscapeCornerZoneUniversal = config)
            }
        }
    }

    suspend fun updateUniversalCornerZoneConfig(config: CornerZoneConfig) =
        updateUniversalCornerZoneConfig(activeHomeOrientation, config)

    suspend fun updateAllCornerZoneAppearance(orientation: HomeOrientation, sourceConfig: CornerZoneConfig) {
        repo.update { s ->
            val applyAppearance = { target: CornerZoneConfig ->
                if (target.enabled) target.copy(
                    size = sourceConfig.size,
                    color = sourceConfig.color,
                    opacity = sourceConfig.opacity,
                    visible = sourceConfig.visible,
                    borderEnabled = sourceConfig.borderEnabled,
                    borderColor = sourceConfig.borderColor,
                    borderWidth = sourceConfig.borderWidth,
                ) else target
            }
            when (orientation) {
                HomeOrientation.PORTRAIT -> s.copy(
                    portraitCornerZoneTopLeft = applyAppearance(s.portraitCornerZoneTopLeft),
                    portraitCornerZoneTopRight = applyAppearance(s.portraitCornerZoneTopRight),
                    portraitCornerZoneBottomLeft = applyAppearance(s.portraitCornerZoneBottomLeft),
                    portraitCornerZoneBottomRight = applyAppearance(s.portraitCornerZoneBottomRight),
                )
                HomeOrientation.LANDSCAPE -> s.copy(
                    landscapeCornerZoneTopLeft = applyAppearance(s.landscapeCornerZoneTopLeft),
                    landscapeCornerZoneTopRight = applyAppearance(s.landscapeCornerZoneTopRight),
                    landscapeCornerZoneBottomLeft = applyAppearance(s.landscapeCornerZoneBottomLeft),
                    landscapeCornerZoneBottomRight = applyAppearance(s.landscapeCornerZoneBottomRight),
                )
            }
        }
    }

    suspend fun updateAllCornerZoneAppearance(sourceConfig: CornerZoneConfig) =
        updateAllCornerZoneAppearance(activeHomeOrientation, sourceConfig)

    suspend fun getSwipeLeftApp(orientation: HomeOrientation): AppPreference = settings.first().swipeAppFor(orientation, "left")
    suspend fun getSwipeRightApp(orientation: HomeOrientation): AppPreference = settings.first().swipeAppFor(orientation, "right")
    suspend fun getSwipeLeftApp(): AppPreference = getSwipeLeftApp(activeHomeOrientation)
    suspend fun getSwipeRightApp(): AppPreference = getSwipeRightApp(activeHomeOrientation)

    suspend fun setSettingsLock(locked: Boolean) = repo.set("lockSettings", locked)
    suspend fun setSettingsLockPin(pin: String) = repo.set("settingsLockPin", pin)
    suspend fun validateSettingsPin(pin: String): Boolean = settings.first().settingsLockPin == pin

    suspend fun setCustomFont(uri: Uri) {
        setFontForSetting("customFontPath", uri)
    }

    suspend fun clearCustomFont() {
        clearFontForSetting("customFontPath")
    }

    suspend fun setFontForSetting(fieldName: String, uri: Uri) {
        val oldPath = getFontPathForField(settings.first(), fieldName)
        val newPath = copyFontToInternal(uri, fieldName) ?: return
        try {
            repo.set(fieldName, newPath)
            deleteFontAtPath(oldPath)
        } catch (e: Exception) {
            deleteFontAtPath(newPath)
            Log.e("SettingsRepo", "Failed updating font field: $fieldName", e)
        }
    }

    suspend fun clearFontForSetting(fieldName: String) {
        val currentPath = getFontPathForField(settings.first(), fieldName)
        if (currentPath.isNotEmpty()) {
            deleteFontAtPath(currentPath)
        }
        repo.set(fieldName, "")
    }

    fun importFontFile(uri: Uri, slotHint: String): String? = copyFontToInternal(uri, slotHint)

    fun deleteFontFile(path: String) = deleteFontAtPath(path)

    private fun copyFontToInternal(uri: Uri, slotHint: String): String? {
        return try {
            val fontDir = File(context.filesDir, "fonts").apply { mkdirs() }
            val cleanHint = slotHint.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val importDir = File(fontDir, "${cleanHint}_${java.lang.System.currentTimeMillis()}").apply { mkdirs() }
            val displayName = readDisplayName(uri)
                ?.takeIf { it.isNotBlank() }
                ?.substringAfterLast('/')
                ?: "font.ttf"
            val sanitizedName = displayName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val fontFile = File(importDir, sanitizedName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(fontFile).use { output -> input.copyTo(output) }
            } ?: return null
            fontFile.absolutePath
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to copy font file", e)
            null
        }
    }

    private fun deleteFontAtPath(path: String) {
        if (path.isBlank()) return
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
                file.parentFile
                    ?.takeIf { it.name.startsWith("home_item_font_") || it.name.startsWith("folder_title_font_") || it.name.startsWith("folder_item_font_") || it.name.startsWith("customFontPath_") || it.name.startsWith("headerFontPath_") || it.name.startsWith("tertiaryFontPath_") || it.name.startsWith("homeLabelFontPath_") || it.name.startsWith("folderLabelFontPath_") || it.name.startsWith("appDrawerLabelFontPath_") }
                    ?.takeIf { it.isDirectory && it.list().isNullOrEmpty() }
                    ?.delete()
            }
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Error deleting font file: $path", e)
        }
    }

    private fun readDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return null
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex == -1) null else cursor.getString(columnIndex)
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun getFontPathForField(settings: AppSettings, fieldName: String): String {
        return when (fieldName) {
            "customFontPath" -> settings.customFontPath
            "headerFontPath" -> settings.headerFontPath
            "tertiaryFontPath" -> settings.tertiaryFontPath
            "homeLabelFontPath" -> settings.homeLabelFontPath
            "folderLabelFontPath" -> settings.folderLabelFontPath
            "appDrawerLabelFontPath" -> settings.appDrawerLabelFontPath
            else -> ""
        }
    }

    suspend fun toggleAppHidden(packageKey: String) {
        toggleAppHidden(packageKey, emptySet())
    }

    suspend fun toggleAppHidden(appKey: String, legacyKeys: Set<String>) {
        repo.update { s ->
            val set = s.hiddenApps.toMutableSet()
            val candidates = (setOf(appKey) + legacyKeys)
                .filter { it.isNotBlank() }
                .distinct()

            val shouldUnhide = candidates.any { set.contains(it) }
            if (shouldUnhide) {
                candidates.forEach { set.remove(it) }
            } else {
                set.add(appKey)
            }
            s.copy(hiddenApps = set)
        }
    }

    suspend fun setAppCustomName(appKey: String, customName: String) {
        repo.update { s ->
            val map = s.renamedApps.toMutableMap()
            if (customName.isBlank()) map.remove(appKey) else map[appKey] = customName
            s.copy(renamedApps = map)
        }
    }

    suspend fun removeAppCustomName(appKey: String) {
        repo.update { s ->
            val map = s.renamedApps.toMutableMap()
            map.remove(appKey)
            s.copy(renamedApps = map)
        }
    }

    suspend fun removeAppCustomNames(appKeys: Set<String>) {
        if (appKeys.isEmpty()) return
        repo.update { s ->
            val map = s.renamedApps.toMutableMap()
            appKeys.forEach { map.remove(it) }
            s.copy(renamedApps = map)
        }
    }

    suspend fun getAppTags(appKey: String): List<String> =
        AppTagUtils.normalizeTags(readAppTags(settings.first())[appKey].orEmpty())

    suspend fun setAppTags(appKey: String, tags: Collection<String>) {
        val normalized = AppTagUtils.normalizeTags(tags)
        repo.update { s ->
            val updated = readAppTags(s).toMutableMap()
            if (normalized.isEmpty()) {
                updated.remove(appKey)
            } else {
                updated[appKey] = normalized
            }
            s.copy(appTagsJson = writeAppTags(updated))
        }
    }

    suspend fun updateAppTags(appKey: String, transform: (List<String>) -> List<String>) {
        val current = getAppTags(appKey)
        setAppTags(appKey, transform(current))
    }

    suspend fun addAppTag(appKey: String, tag: String) {
        updateAppTags(appKey) { it + tag }
    }

    suspend fun updateAppTag(appKey: String, oldTag: String, newTag: String) {
        updateAppTags(appKey) { tags ->
            tags.map { if (AppTagUtils.canonicalizeTag(it) == AppTagUtils.canonicalizeTag(oldTag)) newTag else it }
        }
    }

    suspend fun removeAppTag(appKey: String, tag: String) {
        updateAppTags(appKey) { tags ->
            tags.filterNot { AppTagUtils.canonicalizeTag(it) == AppTagUtils.canonicalizeTag(tag) }
        }
    }

    suspend fun removeAppTags(appKeys: Set<String>) {
        if (appKeys.isEmpty()) return
        repo.update { s ->
            val updated = readAppTags(s).toMutableMap()
            appKeys.forEach { updated.remove(it) }
            s.copy(appTagsJson = writeAppTags(updated))
        }
    }

    suspend fun renameTagAcrossApps(oldTag: String, newTag: String) {
        val oldCanonical = AppTagUtils.canonicalizeTag(oldTag)
        if (oldCanonical.isBlank()) return

        repo.update { s ->
            val updated = readAppTags(s).mapValues { (_, tags) ->
                AppTagUtils.normalizeTags(
                    tags.map { existing ->
                        if (AppTagUtils.canonicalizeTag(existing) == oldCanonical) newTag else existing
                    }
                )
            }.filterValues { it.isNotEmpty() }
            s.copy(appTagsJson = writeAppTags(updated))
        }
    }

    suspend fun removeTagAcrossApps(tag: String) {
        val canonical = AppTagUtils.canonicalizeTag(tag)
        if (canonical.isBlank()) return

        repo.update { s ->
            val updated = readAppTags(s).mapValues { (_, tags) ->
                tags.filterNot { AppTagUtils.canonicalizeTag(it) == canonical }
            }.filterValues { it.isNotEmpty() }
            s.copy(appTagsJson = writeAppTags(updated))
        }
    }

    suspend fun removeOrphanedShortcutTags(validShortcutKeys: Set<String>) {
        repo.update { s ->
            val current = readAppTags(s)
            val updated = current.filterKeys { key ->
                !key.startsWith("shortcut:") || validShortcutKeys.contains(key)
            }
            if (updated == current) s else s.copy(appTagsJson = writeAppTags(updated))
        }
    }

    suspend fun migrateAppKeys(migrations: List<AppKeyMigration>) {
        if (migrations.isEmpty()) return
        repo.update { s ->
            val renamed = s.renamedApps.toMutableMap()
            val hidden = s.hiddenApps.toMutableSet()
            val history = s.recentAppHistory.toMutableMap()
            val appTags = readAppTags(s).toMutableMap()

            val renamedOriginal = s.renamedApps
            val hiddenOriginal = s.hiddenApps
            val historyOriginal = s.recentAppHistory
            val appTagsOriginal = readAppTags(s)

            for (migration in migrations) {
                val newKey = migration.newKey
                val sourceKeys = (migration.moveKeys + migration.copyKeys)
                    .filter { it.isNotBlank() && it != newKey }
                    .distinct()

                if (sourceKeys.isEmpty()) continue

                val renameCandidate = sourceKeys.firstNotNullOfOrNull { renamedOriginal[it] }
                if (!renamed.containsKey(newKey) && renameCandidate != null) {
                    renamed[newKey] = renameCandidate
                }

                if (!hidden.contains(newKey) && sourceKeys.any { hiddenOriginal.contains(it) }) {
                    hidden.add(newKey)
                }

                val maxLaunchTime = sourceKeys.mapNotNull { historyOriginal[it] }.maxOrNull()
                if (maxLaunchTime != null) {
                    val existing = history[newKey]
                    if (existing == null || maxLaunchTime > existing) {
                        history[newKey] = maxLaunchTime
                    }
                }

                val mergedTags = buildList {
                    addAll(appTags[newKey].orEmpty())
                    sourceKeys.forEach { addAll(appTagsOriginal[it].orEmpty()) }
                }
                val normalizedTags = AppTagUtils.normalizeTags(mergedTags)
                if (normalizedTags.isNotEmpty()) {
                    appTags[newKey] = normalizedTags
                }

                for (oldKey in migration.moveKeys) {
                    if (oldKey == newKey) continue
                    renamed.remove(oldKey)
                    hidden.remove(oldKey)
                    history.remove(oldKey)
                    appTags.remove(oldKey)
                }
            }

            s.copy(
                renamedApps = renamed,
                hiddenApps = hidden,
                recentAppHistory = history,
                appTagsJson = writeAppTags(appTags.filterValues { it.isNotEmpty() })
            )
        }
    }

    suspend fun updateAppLaunchTime(appKey: String) {
        repo.update { s ->
            val history = s.recentAppHistory.toMutableMap()
            history[appKey] = Clock.System.now().toEpochMilliseconds()
            if (history.size > 100) {
                val oldest = history.entries.sortedBy { it.value }.take(20)
                oldest.forEach { history.remove(it.key) }
            }
            s.copy(recentAppHistory = history)
        }
    }

    suspend fun setFirstOpen(value: Boolean) = repo.set("firstOpen", value)
    suspend fun setAppTheme(value: Int) = repo.set("appTheme", value)

    // Import/Export functionality
    suspend fun exportSettings(): ExportResult {
        // App tags are included automatically because APP_TAGS_JSON is part of AppSettingsSchema.
        return backupManager.export()
    }

    suspend fun importSettings(jsonString: String, options: ImportOptions = ImportOptions()): ImportResult {
        return backupManager.import(jsonString, options)
    }

    fun validateSettingsBackup(jsonString: String): ValidationResult {
        return backupManager.validate(jsonString)
    }

    suspend fun exportSettingsToUri(uri: Uri): Result<Unit> {
        return try {
            when (val result = exportSettings()) {
                is ExportResult.Success -> {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(result.json.toByteArray(Charsets.UTF_8))
                    } ?: return Result.failure(Exception("Could not open output stream"))
                    Result.success(Unit)
                }
                is ExportResult.Error -> {
                    Result.failure(Exception(result.message))
                }
            }
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to export settings to URI", e)
            Result.failure(e)
        }
    }

    suspend fun importSettingsFromUri(
        uri: Uri,
        widgetImportMode: WidgetImportMode = WidgetImportMode.WIDGETS
    ): ImportResult {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            } ?: return ImportResult.Error(
                io.github.mlmgames.settings.core.backup.ImportError.PARSE_ERROR,
                "Could not read file"
            )
            val result = importSettings(jsonString)
            if (result is ImportResult.Success && widgetImportMode == WidgetImportMode.PLACEHOLDERS) {
                convertImportedWidgetsToPlaceholders()
            }
            result
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to import settings from URI", e)
            ImportResult.Error(
                io.github.mlmgames.settings.core.backup.ImportError.PARSE_ERROR,
                e.message ?: "Unknown error"
            )
        }
    }

    private suspend fun convertImportedWidgetsToPlaceholders() {
        val currentDensity = context.resources.displayMetrics.densityDpi
        val widgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        val pm = context.packageManager

        updateHomeLayouts { _, layouts ->
            fun convertLayout(layout: HomeLayout): HomeLayout {
                val updatedItems = layout.items.map { item ->
                    if (item is HomeItem.Widget && !item.isPlaceholder) {
                        val providerInfo = widgetManager.installedProviders.find { info ->
                            info.provider.packageName == item.packageName &&
                                info.provider.className == item.providerClassName
                        }

                        item.copy(
                            appWidgetId = android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID,
                            isPlaceholder = true,
                            appName = resolveAppName(pm, item.packageName),
                            widgetName = providerInfo?.loadLabel(pm).orEmpty(),
                            intendedColumnSpan = providerInfo?.let {
                                estimateWidgetColumnSpan(it, layout.columns)
                            } ?: item.columnSpan,
                            intendedRowSpan = providerInfo?.let {
                                estimateWidgetRowSpan(it, layout.rows)
                            } ?: item.rowSpan,
                            sourceDensityDpi = currentDensity,
                        )
                    } else {
                        item
                    }
                }
                return layout.copy(items = updatedItems)
            }

            layouts.copy(
                portrait = convertLayout(layouts.portrait),
                landscape = convertLayout(layouts.landscape)
            )
        }
    }

    private fun resolveAppName(pm: PackageManager, packageName: String): String {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            ""
        }
    }

    private fun estimateWidgetColumnSpan(
        info: android.appwidget.AppWidgetProviderInfo,
        layoutColumns: Int
    ): Int {
        val screenWidthDp = context.resources.configuration.screenWidthDp.takeIf { it > 0 } ?: 1
        val cellWidthDp = screenWidthDp.toFloat() / layoutColumns.coerceAtLeast(1)
        return 1.coerceAtLeast(kotlin.math.ceil(info.minWidth.toDouble() / cellWidthDp).toInt())
    }

    private fun estimateWidgetRowSpan(
        info: android.appwidget.AppWidgetProviderInfo,
        layoutRows: Int
    ): Int {
        val screenHeightDp = context.resources.configuration.screenHeightDp.takeIf { it > 0 } ?: 1
        val cellHeightDp = screenHeightDp.toFloat() / layoutRows.coerceAtLeast(1)
        return 1.coerceAtLeast(kotlin.math.ceil(info.minHeight.toDouble() / cellHeightDp).toInt())
    }
}
