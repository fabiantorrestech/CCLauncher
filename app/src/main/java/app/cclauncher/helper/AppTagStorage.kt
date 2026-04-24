package app.cclauncher.helper

import android.util.Log
import kotlinx.serialization.json.Json

object AppTagStorage {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun decode(raw: String): Map<String, List<String>> {
        if (raw.isBlank()) return emptyMap()
        return try {
            AppTagUtils.normalizeTagMap(json.decodeFromString<Map<String, List<String>>>(raw))
        } catch (e: Exception) {
            Log.e("AppTagStorage", "Failed to decode app tags", e)
            emptyMap()
        }
    }

    fun encode(tagsByApp: Map<String, List<String>>): String =
        json.encodeToString(AppTagUtils.normalizeTagMap(tagsByApp))
}
