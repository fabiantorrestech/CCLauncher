package app.cclauncher.helper

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap

/**
 * Utilities for bitmap related operations
 */
object BitmapUtils {

    /**
     * Convert a drawable to a bitmap, sized for display (not the drawable's intrinsic size).
     * Adaptive icons frequently report intrinsic dimensions of 192–432 px; rasterising at that
     * size and storing in ARGB_8888 (the default) is what was inflating the icon cache.
     *
     * We clamp the rasterised side to `targetDp * displayDensity` (default 48 dp) and use
     * RGB_565 when the source has no alpha channel (halves bytes-per-pixel).
     */
    fun drawableToBitmap(
        drawable: Drawable?,
        targetDp: Int = 48
    ): Bitmap? {
        if (drawable == null) return null

        return try {
            val density = Resources.getSystem().displayMetrics.density
            val maxPx = (targetDp * density).toInt().coerceAtLeast(1)

            val intrinsicW = drawable.intrinsicWidth
            val intrinsicH = drawable.intrinsicHeight

            val (width, height) = if (intrinsicW <= 0 || intrinsicH <= 0) {
                maxPx to maxPx
            } else {
                val scale = (maxPx.toFloat() / maxOf(intrinsicW, intrinsicH)).coerceAtMost(1f)
                (intrinsicW * scale).toInt().coerceAtLeast(1) to
                    (intrinsicH * scale).toInt().coerceAtLeast(1)
            }

            val config = if (hasAlpha(drawable)) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
            val bitmap = createBitmap(width, height, config)
            val canvas = Canvas(bitmap)

            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun hasAlpha(drawable: Drawable): Boolean {
        if (drawable is BitmapDrawable) {
            // Bitmap.hasAlpha() is a reliable signal for raster drawables.
            drawable.bitmap?.let { return it.hasAlpha() }
        }
        // For VectorDrawable, AdaptiveIconDrawable, etc. we can't cheaply prove opacity,
        // so default to ARGB_8888 to avoid visual regressions.
        return true
    }
}