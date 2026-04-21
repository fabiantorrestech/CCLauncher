package app.cclauncher

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.content.pm.LauncherApps
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.os.UserManager
import android.util.Log

class ShortcutProvider : ContentProvider() {

    override fun onCreate() = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        if (uriMatcher.match(uri) != PINNED) return null
        val ctx = context ?: return null

        val cursor = MatrixCursor(COLUMNS)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return cursor

        val launcherApps = ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        if (!launcherApps.hasShortcutHostPermission()) {
            Log.d(TAG, "No shortcut host permission — not the default launcher")
            return cursor
        }

        val userManager = ctx.getSystemService(Context.USER_SERVICE) as UserManager
        val query = LauncherApps.ShortcutQuery()
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)

        var rowId = 0
        for (user in userManager.userProfiles) {
            try {
                val shortcuts = launcherApps.getShortcuts(query, user) ?: continue
                for (shortcut in shortcuts) {
                    // Collect all intents and find first http/https URL
                    val intents = buildList {
                        shortcut.intent?.let { add(it) }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                            shortcut.intents?.let { addAll(it) }
                        }
                    }
                    val url = intents
                        .mapNotNull { it.data?.toString() }
                        .firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
                        ?: continue  // skip non-web shortcuts

                    val label = shortcut.shortLabel?.toString() ?: shortcut.id
                    cursor.addRow(arrayOf<Any>(rowId++, label, url, shortcut.`package`, shortcut.id))
                }
            } catch (_: SecurityException) {
                Log.d(TAG, "SecurityException for user $user — skipping")
            }
        }
        return cursor
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

    companion object {
        const val AUTHORITY = "app.cclauncher.shortcuts"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/pinned")
        private val COLUMNS = arrayOf("_id", "label", "url", "browser_package", "shortcut_id")
        private const val PINNED = 1
        private const val TAG = "ShortcutProvider"

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "pinned", PINNED)
        }
    }
}
