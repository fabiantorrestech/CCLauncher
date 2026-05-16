package app.cclauncher.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Central configuration for all animations in the app (similar to github's kotlin animation repos)
 */
object AnimationConfig {

    // Standard durations
    const val QUICK = 150
    const val SUB_QUICK = 200
    const val STANDARD = 300
    const val SLOW = 500
    const val EXTRA_SLOW = 1000

    // Standard easing
    val standardEasing = FastOutSlowInEasing
    val emphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    // Reusable animation specs
    val quickTween = tween<Float>(QUICK, easing = standardEasing)
    val standardTween = tween<Float>(STANDARD, easing = standardEasing)
    val slowTween = tween<Float>(SLOW, easing = standardEasing)

    val standardIntTween = tween<IntSize>(STANDARD, easing = standardEasing)


    // Overlay animations (fade + blur, ported from android_shortcut_hub)
    const val OVERLAY_ENTER = 320
    const val OVERLAY_EXIT = 280
    val overlayAlphaEnter = tween<Float>(OVERLAY_ENTER)
    val overlayAlphaExit = tween<Float>(OVERLAY_EXIT)
    val overlayBlurEnter = tween<Dp>(OVERLAY_ENTER)
    val overlayBlurExit = tween<Dp>(OVERLAY_EXIT)
    // Keyframe exit: hold alpha at 1.0 for the first 45% so blur is visible, then fade out
    val overlayAlphaExitKeyframe = keyframes<Float> {
        durationMillis = OVERLAY_EXIT
        1f at (OVERLAY_EXIT * 0.45).toInt()
        0f at OVERLAY_EXIT
    }

    // Context menu dialog animations
    const val CONTEXT_MENU_EXIT_MS = 160

    // List item animations
    val listItemAnimationSpec = tween<IntOffset>(STANDARD, easing = standardEasing)
    val listItemFadeSpec = tween<Float>(QUICK, easing = standardEasing)

    // Navigation transitions
    object Navigation {

        fun slideUpTransition() = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(STANDARD, easing = standardEasing)
        ) togetherWith slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(STANDARD, easing = standardEasing)
        )

        fun slideDownTransition() = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(STANDARD, easing = standardEasing)
        ) togetherWith slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(STANDARD, easing = standardEasing)
        )

        fun slideLeftTransition() = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(STANDARD, easing = standardEasing)
        ) togetherWith slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(STANDARD, easing = standardEasing)
        )

        fun slideRightTransition() = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(STANDARD, easing = standardEasing)
        ) togetherWith slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(STANDARD, easing = standardEasing)
        )

        fun appDrawerOpenTransition() = (
            slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = tween(380, easing = emphasizedEasing)
            ) + fadeIn(animationSpec = tween(220, easing = standardEasing))
        ) togetherWith fadeOut(animationSpec = tween(180, easing = standardEasing))

        fun appDrawerCloseTransition() = fadeIn(
            animationSpec = tween(220, easing = standardEasing)
        ) togetherWith (
            slideOutVertically(
                targetOffsetY = { it / 3 },
                animationSpec = tween(280, easing = standardEasing)
            ) + fadeOut(animationSpec = tween(200, easing = standardEasing))
        )

        fun fadeTransition() = fadeIn(
            animationSpec = tween(STANDARD, easing = standardEasing)
        ) togetherWith fadeOut(
            animationSpec = tween(STANDARD, easing = standardEasing)
        )

        fun scaleAndFadeTransition() = (
                fadeIn(animationSpec = tween(STANDARD)) +
                        scaleIn(initialScale = 0.95f, animationSpec = tween(STANDARD))
                ) togetherWith (
                fadeOut(animationSpec = tween(STANDARD)) +
                        scaleOut(targetScale = 0.95f, animationSpec = tween(STANDARD))
                )

        fun widgetPickerTransition() = (
                slideInHorizontally(
                    initialOffsetX = { it / 5 },
                    animationSpec = tween(STANDARD)
                ) + fadeIn(animationSpec = tween(STANDARD)) +
                        scaleIn(initialScale = 0.95f, animationSpec = tween(STANDARD))
                ) togetherWith (
                slideOutHorizontally(
                    targetOffsetX = { -it / 5 },
                    animationSpec = tween(STANDARD)
                ) + fadeOut(animationSpec = tween(STANDARD)) +
                        scaleOut(targetScale = 0.95f, animationSpec = tween(STANDARD))
                )
    }

    fun contentSizeAnimationSpec() = tween<IntSize>(STANDARD, easing = standardEasing)

    // Visibility animations
    fun enterTransition() = fadeIn(animationSpec = tween(QUICK)) +
            expandVertically(animationSpec = tween(STANDARD))

    fun exitTransition() = fadeOut(animationSpec = tween(QUICK)) +
            shrinkVertically(animationSpec = tween(STANDARD))
}

fun getAdjustedDuration(baseDuration: Int, speedMultiplier: Float): Int {
    return (baseDuration / speedMultiplier).toInt().coerceAtLeast(1)
}