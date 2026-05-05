package com.dsi.sound

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Recreates the DSi Sound 2D pitch/speed touch pad.
 * X axis = speed (left=slow, right=fast)
 * Y axis = pitch (up=high, down=low)
 * Centre = 1.0x / 1.0x (normal)
 */
class PitchSpeedPad @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onPitchSpeedChanged: ((pitch: Float, speed: Float) -> Unit)? = null

    // 0..1 normalized position, 0.5 = centre = 1.0x
    private var dotX = 0.5f
    private var dotY = 0.5f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33223355
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8AAABB.toInt()
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4488FF.toInt()
    }
    private val dotRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF223355.toInt()
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }
    private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x44223355
        strokeWidth = 1f
    }

    // Gradient background
    private var bgShader: Shader? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bgShader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(0xFFD0E8FF.toInt(), 0xFFB0CCEE.toInt(), 0xFFD0E8FF.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        bgPaint.shader = bgShader
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = 12f

        // Background
        canvas.drawRoundRect(0f, 0f, w, h, r, r, bgPaint)
        canvas.drawRoundRect(0f, 0f, w, h, r, r, borderPaint)

        // Grid lines
        val gridCols = 6
        val gridRows = 4
        for (i in 1 until gridCols) {
            val x = w * i / gridCols
            canvas.drawLine(x, 0f, x, h, gridPaint)
        }
        for (i in 1 until gridRows) {
            val y = h * i / gridRows
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        // Centre crosshair
        canvas.drawLine(w / 2, 0f, w / 2, h, crossPaint)
        canvas.drawLine(0f, h / 2, w, h / 2, crossPaint)

        // Corner labels (like the real DSi)
        labelPaint.textSize = h * 0.14f
        labelPaint.color = 0x88223355.toInt()
        canvas.drawText("◀ Slow", w * 0.22f, h - 4f, labelPaint)
        canvas.drawText("Fast ▶", w * 0.78f, h - 4f, labelPaint)
        canvas.drawText("▲ Hi", w / 2, h * 0.12f, labelPaint)
        canvas.drawText("Lo ▼", w / 2, h * 0.96f, labelPaint)

        // Dot position
        val dx = dotX * w
        val dy = dotY * h
        val dotR = (minOf(w, h) * 0.1f).coerceIn(8f, 18f)

        // Shadow
        dotPaint.color = 0x44000000
        canvas.drawCircle(dx + 2f, dy + 2f, dotR, dotPaint)

        // Dot
        dotPaint.color = 0xFF4488FF.toInt()
        canvas.drawCircle(dx, dy, dotR, dotPaint)
        canvas.drawCircle(dx, dy, dotR, dotRingPaint)

        // Inner highlight
        dotPaint.color = 0x88FFFFFF.toInt()
        canvas.drawCircle(dx - dotR * 0.25f, dy - dotR * 0.25f, dotR * 0.35f, dotPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                dotX = (event.x / width).coerceIn(0f, 1f)
                dotY = (event.y / height).coerceIn(0f, 1f)
                invalidate()

                val speed = valueFromNorm(dotX)
                val pitch = valueFromNorm(1f - dotY)  // invert Y: up = high pitch
                onPitchSpeedChanged?.invoke(pitch, speed)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /** Map 0..1 → 0.25x..4.0x (centre 0.5 → 1.0x) */
    private fun valueFromNorm(n: Float): Float {
        return when {
            n <= 0.5f -> 0.25f + n * 2f * 0.75f
            else -> 1.0f + (n - 0.5f) * 2f * 3.0f
        }
    }

    fun resetToCenter() {
        dotX = 0.5f
        dotY = 0.5f
        invalidate()
        onPitchSpeedChanged?.invoke(1f, 1f)
    }
}
