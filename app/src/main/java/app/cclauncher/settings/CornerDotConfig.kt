package app.cclauncher.settings

import app.cclauncher.data.Constants
import kotlinx.serialization.Serializable

@Serializable
data class CornerDotConfig(
    val enabled: Boolean = false,
    val action: Int = Constants.SwipeAction.NULL,
    /** Folder ID used when action == OPEN_FOLDER. */
    val folderId: String = "",
    /** App preference used when action == APP. */
    val appPreference: AppPreference = AppPreference(),
    /** Dot diameter in dp. */
    val size: Float = 12f,
    /** ARGB color int for the dot fill. */
    val color: Int = 0xFFFFFFFF.toInt(),
    /** Opacity 0.0–1.0 applied on top of the color alpha. */
    val opacity: Float = 0.6f,
    /** When false the dot is invisible but still receives taps (if enabled). */
    val visible: Boolean = true,
    val borderEnabled: Boolean = false,
    val borderColor: Int = 0xFF000000.toInt(),
    val borderWidth: Float = 1f,
    /** Distance from the screen corner in dp. */
    val inset: Float = 16f,
)
