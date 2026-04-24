package app.cclauncher.settings

import android.content.Context
import android.provider.OpenableColumns
import android.net.Uri
import android.os.Build
import android.util.Log
import app.cclauncher.data.HomeLayout
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
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Clock

class AppSettingsRepository(private val context: Context): KoinComponent {

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

    private fun readAppTags(settings: AppSettings): Map<String, List<String>> =
        AppTagStorage.decode(settings.appTagsJson)

    private fun writeAppTags(tagsByApp: Map<String, List<String>>): String =
        AppTagStorage.encode(tagsByApp)

    suspend fun updateSetting(propertyName: String, value: Any) {
        repo.set(propertyName, value)
    }

    suspend fun updateSetting(update: (AppSettings) -> AppSettings) {
        repo.update(update)
    }

    fun getHomeLayout(): Flow<HomeLayout> =
        repo.observeField("homeLayout")

    suspend fun saveHomeLayout(layout: HomeLayout) {
        repo.set("homeLayout", layout)
    }

    suspend fun triggerHomeLayoutRefresh() {
        val currentLayout = getHomeLayout().first()
        saveHomeLayout(currentLayout)
    }

    suspend fun setSwipeLeftApp(app: AppPreference) { repo.update { it.copy(swipeLeftApp = app) } }

    suspend fun setSwipeRightApp(app: AppPreference) { repo.update { it.copy(swipeRightApp = app) } }

    suspend fun setSwipeUpApp(app: AppPreference) { repo.update { it.copy(swipeUpApp = app) } }

    suspend fun setSwipeDownApp(app: AppPreference) { repo.update { it.copy(swipeDownApp = app) } }

    suspend fun setSwipeFolderId(direction: String, folderId: String) {
        repo.update { s ->
            when (direction) {
                "up" -> s.copy(swipeUpFolderId = folderId)
                "down" -> s.copy(swipeDownFolderId = folderId)
                "left" -> s.copy(swipeLeftFolderId = folderId)
                "right" -> s.copy(swipeRightFolderId = folderId)
                else -> s
            }
        }
    }

    suspend fun updateCornerZoneConfig(corner: Int, config: CornerZoneConfig) {
        repo.update { s ->
            when (corner) {
                app.cclauncher.data.Constants.CornerPosition.TOP_LEFT -> s.copy(cornerZoneTopLeft = config)
                app.cclauncher.data.Constants.CornerPosition.TOP_RIGHT -> s.copy(cornerZoneTopRight = config)
                app.cclauncher.data.Constants.CornerPosition.BOTTOM_LEFT -> s.copy(cornerZoneBottomLeft = config)
                app.cclauncher.data.Constants.CornerPosition.BOTTOM_RIGHT -> s.copy(cornerZoneBottomRight = config)
                else -> s
            }
        }
    }

    suspend fun updateUniversalCornerZoneConfig(config: CornerZoneConfig) {
        repo.update { it.copy(cornerZoneUniversal = config) }
    }

    suspend fun updateAllCornerZoneAppearance(sourceConfig: CornerZoneConfig) {
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
            s.copy(
                cornerZoneTopLeft = applyAppearance(s.cornerZoneTopLeft),
                cornerZoneTopRight = applyAppearance(s.cornerZoneTopRight),
                cornerZoneBottomLeft = applyAppearance(s.cornerZoneBottomLeft),
                cornerZoneBottomRight = applyAppearance(s.cornerZoneBottomRight),
            )
        }
    }

    suspend fun getSwipeLeftApp(): AppPreference = settings.first().swipeLeftApp
    suspend fun getSwipeRightApp(): AppPreference = settings.first().swipeRightApp

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

    suspend fun importSettingsFromUri(uri: Uri): ImportResult {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            } ?: return ImportResult.Error(
                io.github.mlmgames.settings.core.backup.ImportError.PARSE_ERROR,
                "Could not read file"
            )
            importSettings(jsonString)
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to import settings from URI", e)
            ImportResult.Error(
                io.github.mlmgames.settings.core.backup.ImportError.PARSE_ERROR,
                e.message ?: "Unknown error"
            )
        }
    }
}
