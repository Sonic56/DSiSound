package com.dsi.sound

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Draws the iconic DSi Sound dancing chick sprites
 * overlaid on top of the visualizer background.
 */
class ChickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Chick(
        var x: Float,
        var y: Float,
        var phase: Float,      // animation phase offset
        var frameIndex: Int = 0,
        var bounceY: Float = 0f,
        var scale: Float = 1f,
        val isGhost: Boolean = false
    )

    private val chickFrameIds = listOf(
        R.drawable.chick_idle,
        R.drawable.chick_wave,
        R.drawable.chick_up,
        R.drawable.chick_turn,
        R.drawable.chick_big,
        R.drawable.chick_duck,
        R.drawable.chick_lean,
    )

    private val chickBitmaps = arrayOfNulls<Bitmap>(chickFrameIds.size)
    private var ghostBitmap: Bitmap? = null
    private var heartBitmap: Bitmap? = null
    private var bubbleBitmap: Bitmap? = null

    private val chicks = mutableListOf<Chick>()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val alphaPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = 180 }

    private var tick = 0L
    private var amplitude = 0f
    private var isPlaying = false
    private var showChicks = true  // only shown when chick visualizer is active

    private val animRunnable = object : Runnable {
        override fun run() {
            if (isPlaying) {
                tick++
                amplitude = amplitude * 0.85f + 0.15f * (0.4f + 0.6f * abs(sin(tick * 0.05)).toFloat())
                updateChicks()
                invalidate()
                postDelayed(this, 40) // ~25fps like the DSi
            }
        }
    }

    fun setShowChicks(show: Boolean) {
        showChicks = show
        if (!show) invalidate()
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

    private fun getChickBitmap(index: Int): Bitmap? {
        val safeIdx = index.coerceIn(0, chickFrameIds.size - 1)
        if (chickBitmaps[safeIdx] == null) {
            chickBitmaps[safeIdx] = BitmapFactory.decodeResource(resources, chickFrameIds[safeIdx])
        }
        return chickBitmaps[safeIdx]
    }

    private fun initChicks() {
        if (chicks.isNotEmpty() || width == 0) return
        val count = 8
        chicks.clear()
        for (i in 0 until count) {
            chicks.add(Chick(
                x = width * (i + 0.5f) / count,
                y = height * 0.7f,
                phase = i * 0.8f,
                scale = 0.8f + Random.nextFloat() * 0.4f,
                isGhost = i == 3 || i == 6
            ))
        }
    }

    private fun updateChicks() {
        chicks.forEach { c ->
            val bounce = amplitude * 20f * sin(tick * 0.2f + c.phase).toFloat()
            c.bounceY = bounce
            // Advance frame based on amplitude
            if (tick % 4L == 0L) {
                c.frameIndex = ((c.frameIndex + 1) % chickFrameIds.size)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initChicks()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!showChicks || !isPlaying) return
        initChicks()

        val chickSize = (width / 10f).coerceIn(32f, 64f)

        chicks.forEach { c ->
            val bmp = getChickBitmap(c.frameIndex) ?: return@forEach
            val drawSize = chickSize * c.scale
            val left = c.x - drawSize / 2
            val top = c.y - drawSize + c.bounceY

            val dest = RectF(left, top, left + drawSize, top + drawSize)
            canvas.drawBitmap(bmp, null, dest, if (c.isGhost) alphaPaint else paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
        chickBitmaps.fill(null)
    }
}
