package app.cclauncher.data

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class KeyboardShortcut(
    val modifier: Int,
    val keyCode: Int,
    val appKey: String,
)

object KeyModifier {
    const val CTRL = 1
    const val ALT = 2
}

object KeyboardShortcutStorage {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun decode(raw: String): List<KeyboardShortcut> {
        if (raw.isBlank()) return emptyList()
        return try {
            json.decodeFromString<List<KeyboardShortcut>>(raw)
        } catch (e: Exception) {
            Log.e("KeyboardShortcutStorage", "Failed to decode keyboard shortcuts", e)
            emptyList()
        }
    }

    fun encode(shortcuts: List<KeyboardShortcut>): String =
        json.encodeToString(shortcuts)
}
