package app.cclauncher.helper

data class RetainedAppTagRecord(
    val appKey: String,
    val displayLabel: String,
    val packageName: String,
    val isDeletedApp: Boolean,
)

object AppTagKeyUtils {
    fun retainedRecordForKey(appKey: String): RetainedAppTagRecord? {
        if (appKey.isBlank() || appKey.startsWith("shortcut:")) return null

        val parts = appKey.split('/')
        val packageName = parts.firstOrNull().orEmpty().trim()
        if (packageName.isBlank()) return null

        return RetainedAppTagRecord(
            appKey = appKey,
            displayLabel = packageName,
            packageName = packageName,
            isDeletedApp = true,
        )
    }
}
