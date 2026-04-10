package app.cclauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun ScrollbarIndicator(
    listState: LazyListState,
    totalItems: Int,
    reverseLayout: Boolean = false,
    modifier: Modifier = Modifier,
    thumbWidthDp: Dp = 6.dp,
    minThumbHeightDp: Dp = 40.dp,
    touchTargetWidthDp: Dp = 36.dp,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var trackHeightPx by remember { mutableStateOf(0f) }

    // Fix 4: isolate sumOf in its own derivedStateOf so it only re-runs when
    // item *sizes* change (e.g. font scale change), not on every scroll tick.
    val averageItemSize by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) 0f
            else visibleItems.sumOf { it.size } / visibleItems.size.toFloat()
        }
    }

    // Fix 2: all position math runs only when the derived result actually changes.
    val thumbGeometry by remember(totalItems, reverseLayout) {
        derivedStateOf {
            val avgSize = averageItemSize
            if (avgSize <= 0f || totalItems <= 0) return@derivedStateOf null

            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf null

            val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
            if (viewportHeight <= 0f) return@derivedStateOf null

            val estimatedTotalHeight = avgSize * totalItems
            if (estimatedTotalHeight <= viewportHeight) return@derivedStateOf null

            val proportion = viewportHeight / estimatedTotalHeight
            val firstItem = visibleItems.first()
            val scrollOffset = firstItem.index * avgSize + (firstItem.offset - layoutInfo.viewportStartOffset) * -1f
            val scrollRange = estimatedTotalHeight - viewportHeight
            var scrollFraction = (scrollOffset / scrollRange).coerceIn(0f, 1f)
            if (reverseLayout) scrollFraction = 1f - scrollFraction

            ThumbGeometry(proportion = proportion, scrollFraction = scrollFraction)
        }
    }

    val geometry = thumbGeometry ?: return

    val minThumbHeightPx = with(density) { minThumbHeightDp.toPx() }
    val thumbHeightPx = (trackHeightPx * geometry.proportion).coerceAtLeast(minThumbHeightPx)
    val thumbHeightDp = with(density) { thumbHeightPx.toDp() }
    val usableTrack = trackHeightPx - thumbHeightPx
    // Fix 3: direct offset — no animation stacking on every scroll tick.
    val thumbOffsetDp = with(density) { (usableTrack * geometry.scrollFraction).toDp() }

    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(touchTargetWidthDp)
            .fillMaxHeight()
            .onSizeChanged { trackHeightPx = it.height.toFloat() }
            .pointerInput(totalItems, reverseLayout) {
                detectVerticalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                ) { change, _ ->
                    change.consume()
                    val y = change.position.y
                    val usable = trackHeightPx - thumbHeightPx
                    if (usable <= 0f) return@detectVerticalDragGestures
                    var dragFraction = ((y - thumbHeightPx / 2f) / usable).coerceIn(0f, 1f)
                    if (reverseLayout) dragFraction = 1f - dragFraction
                    val targetIndex = (dragFraction * (totalItems - 1)).roundToInt()
                    scope.launch { listState.scrollToItem(targetIndex) }
                }
            },
        contentAlignment = Alignment.TopEnd
    ) {
        Box(
            modifier = Modifier
                .width(thumbWidthDp)
                .height(thumbHeightDp)
                .offset(y = thumbOffsetDp)
                .background(
                    color = if (isDragging) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(thumbWidthDp / 2)
                )
        )
    }
}

private data class ThumbGeometry(
    val proportion: Float,
    val scrollFraction: Float,
)
