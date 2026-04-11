package app.cclauncher.settings

import app.cclauncher.data.Constants
import kotlinx.serialization.Serializable

@Serializable
data class CornerZoneConfig(
    val enabled: Boolean = false,
    /** Tap action. */
    val action: Int = Constants.SwipeAction.NULL,
    /** Folder ID used when action == OPEN_FOLDER. */
    val folderId: String = "",
    /** App preference used when action == APP. */
    val appPreference: AppPreference = AppPreference(),
    /** Whether the hold (long-press) action is active. */
    val holdEnabled: Boolean = false,
    /** Action fired after a 500 ms long-press. */
    val holdAction: Int = Constants.SwipeAction.NULL,
    /** Folder ID used when holdAction == OPEN_FOLDER. */
    val holdFolderId: String = "",
    /** App preference used when holdAction == APP. */
    val holdAppPreference: AppPreference = AppPreference(),
    /** Leg length of the right-triangle zone in dp (both legs equal → 45° hypotenuse). */
    val size: Float = 80f,
    /** ARGB color int for the triangle fill. */
    val color: Int = 0xFFFFFFFF.toInt(),
    /** Opacity 0.0–1.0 applied to both fill and border. */
    val opacity: Float = 0.35f,
    /** When false the zone is invisible but still receives touches (if enabled). */
    val visible: Boolean = true,
    val borderEnabled: Boolean = true,
    val borderColor: Int = 0xFFFFFFFF.toInt(),
    val borderWidth: Float = 1.5f,
)
