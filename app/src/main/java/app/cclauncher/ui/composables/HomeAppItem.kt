package app.cclauncher.ui.composables

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cclauncher.data.AppModel
import app.cclauncher.data.Constants
import app.cclauncher.helper.rememberAppIcon
import app.cclauncher.loadFontFamily
import app.cclauncher.LocalLauncherFontSettings
import app.cclauncher.settings.AppSettings

@Composable
fun HomeAppItem(
    modifier: Modifier = Modifier,
    app: AppModel,
    settings: AppSettings,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    appWidth: Dp,
    appHeight: Dp,
    appTextSize: Float = 1.0f,
    appLabelAlignment: Int = -1,
    shortcutIconPlacement: Int = Constants.IconPlacement.LEFT,
    labelFontPath: String = "",
) {
    val textColor = if (settings.useCustomTextColor && settings.textColor != 0) {
        Color(settings.textColor)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val showHomeIcons = if (settings.showHomeScreenIcons) {
        if (isLandscape) settings.showIconsInLandscape else settings.showIconsInPortrait
    } else { false }

    val loadedIcon = rememberAppIcon(
        app = app,
        iconPackName = settings.selectedIconPack,
        enabled = showHomeIcons
    )

    val showIcons = showHomeIcons
    val showName = if (showHomeIcons) settings.showAppNames else true //TODO: Add a separate setting later? When settings are arranged properly ig
    val fontScale = settings.textSizeScale
    val fontWeight = remember(settings.fontWeight) {
        when (settings.fontWeight) {
            0 -> FontWeight.Thin
            1 -> FontWeight.Light
            2 -> FontWeight.Normal
            3 -> FontWeight.Medium
            4 -> FontWeight.Bold
            5 -> FontWeight.Black
            else -> FontWeight.Normal
        }
    }
    val iconCornerRadius = settings.iconCornerRadius.dp

    val (iconSize, baseFontSize) = if (settings.scaleHomeApps) {
        val computedIconSize = (minOf(appWidth, appHeight) * 0.6f)

        // Use a baseline cell height (e.g. 80.dp) to compute scale factor; adjust as needed. (rem. when you want to)
        val baselineCellHeight = 80.dp
        val scaleFactor = appWidth / baselineCellHeight
        val computedFontSize =
            (MaterialTheme.typography.bodyMedium.fontSize.value * scaleFactor).sp

        Pair(computedIconSize, computedFontSize)
    } else {
        Pair(48.dp, MaterialTheme.typography.bodyMedium.fontSize)
    }

    val effectiveFontSize = (baseFontSize.value * fontScale * appTextSize).sp

    val effectiveAlignment = if (appLabelAlignment >= 0) appLabelAlignment else settings.appLabelAlignment
    val textAlign = when (effectiveAlignment) {
        1 -> TextAlign.Center
        2 -> TextAlign.Right
        else -> TextAlign.Left
    }
    val columnHorizontalAlignment = when (effectiveAlignment) {
        1 -> Alignment.CenterHorizontally
        2 -> Alignment.End
        else -> Alignment.Start
    }
    val rowArrangement = when (effectiveAlignment) {
        1 -> Arrangement.Center
        2 -> Arrangement.End
        else -> Arrangement.Start
    }
    val showShortcutIcon = app.isSystemShortcut && settings.showShortcutIcon
    val showShortcutIconOnRight = shortcutIconPlacement == Constants.IconPlacement.RIGHT
    val launcherFontSettings = LocalLauncherFontSettings.current
    val labelFontFamily = remember(launcherFontSettings, labelFontPath) {
        if (!launcherFontSettings.customFontsEnabled) {
            null
        } else {
            loadFontFamily(labelFontPath)
                ?: loadFontFamily(launcherFontSettings.homeDefaultFontPath)
                ?: loadFontFamily(launcherFontSettings.mainFontPath)
        }
    }

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { currentOnClick() },
                    onLongPress = { currentOnLongClick() }
                )
            }
            .padding(4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = if (settings.showHomeScreenIcons) Alignment.CenterHorizontally else columnHorizontalAlignment
    ) {
        if (showIcons && loadedIcon != null) {
            Surface(
                shape = RoundedCornerShape(iconCornerRadius),
                modifier = Modifier
                    .size(iconSize)
                    .aspectRatio(1f), // Ensure square aspect ratio
                color = Color.Transparent
            ) {
                Image(
                    bitmap = loadedIcon!!,
                    contentDescription = "${app.appLabel} icon",
                )
            }
            Spacer(modifier = Modifier.height(if (showName) 4.dp else 0.dp)) // Space between icon and text
        }

        if (showName) {
            if (showShortcutIcon) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = rowArrangement,
                ) {
                    if (!showShortcutIconOnRight) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                    Text(
                        text = app.appLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = effectiveFontSize,
                            fontFamily = labelFontFamily,
                            fontWeight = fontWeight
                        ),
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (showShortcutIconOnRight) {
                        Spacer(modifier = Modifier.size(4.dp))
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            } else {
                Text(
                    text = app.appLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = effectiveFontSize,
                        fontFamily = labelFontFamily,
                        fontWeight = fontWeight
                    ),
                    color = textColor,
                    textAlign = textAlign,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
