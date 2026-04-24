package app.cclauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class MoveDirection {
    Up,
    Down,
    Left,
    Right,
}

@Composable
fun MovePadOverlay(
    onMove: (MoveDirection) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val outerSize = 160.dp
    val buttonSize = 46.dp
    val centerSize = 40.dp
    val defaultMargin = 24.dp
    val outerSizePx = with(density) { outerSize.roundToPx() }
    val defaultMarginPx = with(density) { defaultMargin.roundToPx() }

    var parentSize by remember { mutableStateOf(IntSize.Zero) }
    var controlOffset by remember { mutableStateOf<IntOffset?>(null) }

    LaunchedEffect(parentSize) {
        if (parentSize == IntSize.Zero || controlOffset != null) return@LaunchedEffect

        val maxX = (parentSize.width - outerSizePx).coerceAtLeast(0)
        val maxY = (parentSize.height - outerSizePx).coerceAtLeast(0)
        controlOffset = IntOffset(
            x = (maxX - defaultMarginPx).coerceAtLeast(0),
            y = (maxY - defaultMarginPx).coerceAtLeast(0),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { parentSize = it }
    ) {
        val anchoredOffset = controlOffset ?: return@Box

        Box(
            modifier = Modifier
                .offset { anchoredOffset }
                .size(outerSize)
        ) {
            MovePadButton(
                icon = Icons.Default.KeyboardArrowUp,
                contentDescription = "Move up",
                buttonSize = buttonSize,
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                if (!onMove(MoveDirection.Up)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            MovePadButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Move left",
                buttonSize = buttonSize,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                if (!onMove(MoveDirection.Left)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            MovePadButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Move right",
                buttonSize = buttonSize,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                if (!onMove(MoveDirection.Right)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            MovePadButton(
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = "Move down",
                buttonSize = buttonSize,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                if (!onMove(MoveDirection.Down)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(centerSize)
                    .pointerInput(parentSize) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()

                            val current = controlOffset ?: IntOffset.Zero
                            val maxX = (parentSize.width - outerSizePx).coerceAtLeast(0)
                            val maxY = (parentSize.height - outerSizePx).coerceAtLeast(0)

                            controlOffset = IntOffset(
                                x = (current.x + dragAmount.x.roundToInt()).coerceIn(0, maxX),
                                y = (current.y + dragAmount.y.roundToInt()).coerceIn(0, maxY),
                            )
                        }
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                            shape = CircleShape,
                        )
                )
            }
        }
    }
}

@Composable
private fun MovePadButton(
    icon: ImageVector,
    contentDescription: String,
    buttonSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier.size(buttonSize),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
