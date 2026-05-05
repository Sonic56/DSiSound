package com.dsi.sound

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

/**
 * Renders one of the DSi Sound visualizer backgrounds, scrolling it
 * horizontally in sync with the music amplitude.
 */
class VisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // All available background bitmaps (loaded lazily)
    private val bgResIds = listOf(
        R.drawable.bg_mosaic,
        R.drawable.bg_mario,
        R.drawable.bg_mountain,
        R.drawable.bg_grid,
        R.drawable.bg_forest,
        R.drawable.bg_sky,
        R.drawable.bg_clouds,
        R.drawable.bg_lines,
        R.drawable.bg_flower,
        R.drawable.bg_cards,
        R.drawable.bg_chick,
    )

    val bgNames = listOf(
        "Mosaic", "Mario", "Mountains", "Tunnel",
        "Forest", "Meadow", "Clouds", "Lines",
        "Flower", "Cards", "Chick"
    )

    private val bitmaps = arrayOfNulls<Bitmap>(bgResIds.size)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    var currentBg = 0
        set(value) { field = value.coerceIn(0, bgResIds.size - 1); invalidate() }

    // Animation state
    private var scrollX = 0f
    private var amplitude = 0f   // 0..1 representing volume level
    private var tick = 0L
    private var isPlaying = false

    private val animRunnable = object : Runnable {
        override fun run() {
            if (isPlaying) {
                tick++
                // Gentle wave scroll
                scrollX += 0.8f + amplitude * 3f
                amplitude = amplitude * 0.92f + (if (isPlaying) 0.1f else 0f) * (0.5f + 0.5f * sin(tick * 0.1).toFloat())
                invalidate()
                postDelayed(this, 16)  // ~60fps
            }
        }
    }

    fun setAmplitude(amp: Float) {
        amplitude = amp.coerceIn(0f, 1f)
    }

    fun startAnimation() {
        isPlaying = true
        post(animRunnable)
    }

    fun stopAnimation() {
        isPlaying = false
        removeCallbacks(animRunnable)
        invalidate()
    }

    private fun getBitmap(index: Int): Bitmap? {
        if (bitmaps[index] == null) {
            val opts = BitmapFactory.Options().apply {
                inScaled = false
            }
            bitmaps[index] = BitmapFactory.decodeResource(resources, bgResIds[index], opts)
        }
        return bitmaps[index]
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = getBitmap(currentBg) ?: return

        val viewW = width.toFloat()
        val viewH = height.toFloat()

        // Scale bitmap to fill height, maintain aspect
        val scale = viewH / bmp.height
        val scaledW = bmp.width * scale

        // Tile horizontally
        val offset = scrollX % scaledW
        var x = -offset

        while (x < viewW) {
            canvas.drawBitmap(
                bmp,
                null,
                android.graphics.RectF(x, 0f, x + scaledW, viewH),
                paint
            )
            x += scaledW
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
        bitmaps.fill(null)
    }
}
