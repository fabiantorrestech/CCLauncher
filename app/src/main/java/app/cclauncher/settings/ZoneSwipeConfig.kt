package app.cclauncher.settings

import app.cclauncher.data.Constants
import kotlinx.serialization.Serializable

@Serializable
data class ZoneSwipeConfig(
    val enabled: Boolean = false,
    val action: Int = Constants.SwipeAction.NULL,
    val folderId: String = "",
    val appPreference: AppPreference = AppPreference(),
)
