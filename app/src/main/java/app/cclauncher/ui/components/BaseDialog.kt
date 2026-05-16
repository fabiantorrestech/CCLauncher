package app.cclauncher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.cclauncher.ui.theme.AnimationConfig
import kotlinx.coroutines.delay

/**
 * Base dialog composable for consistent dialog styling
 */
@Composable
fun BaseDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Column {
                content()
            }
        },
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}

/**
 * Card-style dialog for more complex layouts
 */
@Composable
fun CardDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                content()
            }
        }
    }
}

/**
 * Simple confirmation dialog
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BaseDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    ) {
        Text(message)
    }
}

/**
 * Animated context menu dialog with scale+fade enter/exit and a clickable scrim.
 *
 * Pass [visible] derived from the nullable state that drives the menu (e.g. `appContextMenu != null`).
 * Pass [onDismissRequest] to null-out that state.
 * The composable manages deferred dismiss so the exit animation plays before the Dialog window closes.
 */
@Composable
fun AnimatedContextMenuDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    animationsEnabled: Boolean = true,
    instantExit: Boolean = false,
    title: (@Composable () -> Unit)? = null,
    confirmButton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var dialogOpen by remember { mutableStateOf(false) }
    var internalVisible by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            dialogOpen = true
            internalVisible = true
        } else if (dialogOpen) {
            internalVisible = false
            if (animationsEnabled && !instantExit) delay(AnimationConfig.CONTEXT_MENU_EXIT_MS.toLong())
            dialogOpen = false
        }
    }

    if (!dialogOpen) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        BackHandler { onDismissRequest() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismissRequest,
                ),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = internalVisible,
                enter = if (animationsEnabled)
                    scaleIn(
                        initialScale = 0.88f,
                        animationSpec = tween(AnimationConfig.SUB_QUICK, easing = AnimationConfig.emphasizedEasing),
                    ) + fadeIn(animationSpec = tween(AnimationConfig.QUICK))
                else EnterTransition.None,
                exit = if (animationsEnabled)
                    scaleOut(
                        targetScale = 0.92f,
                        animationSpec = tween(AnimationConfig.CONTEXT_MENU_EXIT_MS),
                    ) + fadeOut(animationSpec = tween(AnimationConfig.CONTEXT_MENU_EXIT_MS))
                else ExitTransition.None,
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(min = 280.dp, max = 360.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {},
                        ),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        title?.let { titleContent ->
                            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                                titleContent()
                            }
                        }
                        content()
                        confirmButton?.let {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                it()
                            }
                        }
                    }
                }
            }
        }
    }
}
