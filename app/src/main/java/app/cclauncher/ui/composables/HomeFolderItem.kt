package app.cclauncher.ui.composables

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cclauncher.data.Constants
import app.cclauncher.data.HomeItem
import app.cclauncher.loadFontFamily
import app.cclauncher.LocalLauncherFontSettings
import app.cclauncher.settings.AppSettings

@Composable
fun HomeFolderItem(
    folder: HomeItem.Folder,
    settings: AppSettings,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val textColor = if (settings.useCustomTextColor && settings.textColor != 0) {
        Color(settings.textColor)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

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

    val fontScale = settings.textSizeScale * folder.titleTextSize
    val effectiveFontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * fontScale).sp

    val effectiveAlignment = if (folder.titleLabelAlignment >= 0) folder.titleLabelAlignment else settings.appLabelAlignment
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
    val showFolderIconOnRight = folder.iconPlacement == Constants.IconPlacement.RIGHT
    val launcherFontSettings = LocalLauncherFontSettings.current
    val labelFontFamily = remember(launcherFontSettings, folder.titleFontPath) {
        if (!launcherFontSettings.customFontsEnabled) {
            null
        } else {
            loadFontFamily(folder.titleFontPath)
                ?: loadFontFamily(launcherFontSettings.folderDefaultFontPath)
                ?: loadFontFamily(launcherFontSettings.mainFontPath)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(onClick, onLongClick) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = columnHorizontalAlignment,
    ) {
        if (settings.showFolderIcon) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = rowArrangement,
            ) {
                if (!showFolderIconOnRight) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(14.dp),
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(4.dp))
                }
                Text(
                    text = folder.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = effectiveFontSize,
                        fontFamily = labelFontFamily,
                        fontWeight = fontWeight,
                    ),
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showFolderIconOnRight) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(4.dp))
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        } else {
            Text(
                text = folder.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = effectiveFontSize,
                    fontFamily = labelFontFamily,
                    fontWeight = fontWeight,
                ),
                color = textColor,
                textAlign = textAlign,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
