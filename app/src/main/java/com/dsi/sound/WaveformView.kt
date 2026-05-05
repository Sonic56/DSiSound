package com.dsi.sound

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.random.Random

/**
 * DSi Sound-style waveform: a row of coloured bars that bounce
 * to the music amplitude, drawn at the bottom of the top screen.
 */
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val BAR_COUNT = 32
    private val barHeights = FloatArray(BAR_COUNT) { 0.1f }
    private val barTargets = FloatArray(BAR_COUNT) { 0.1f }
    private val barPhases = FloatArray(BAR_COUNT) { it * 0.4f }

    private var amplitude = 0f
    private var tick = 0L

    // DSi-style gradient colours: blue → cyan → green → yellow
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barColors = intArrayOf(
        0xFF44AAFF.toInt(),
        0xFF22DDFF.toInt(),
        0xFF44FF88.toInt(),
        0xFFAAFF44.toInt(),
        0xFFFFDD00.toInt(),
        0xFFFF8800.toInt(),
    )

    fun setAmplitude(amp: Float) {
        amplitude = amp.coerceIn(0f, 1f)
        // Randomise targets on each amplitude pulse
        for (i in 0 until BAR_COUNT) {
            val wave = sin(tick * 0.08 + barPhases[i]).toFloat()
            barTargets[i] = (amplitude * 0.7f + 0.15f) * (0.5f + 0.5f * abs(wave))
        }
    }

    fun tick() {
        tick++
        for (i in 0 until BAR_COUNT) {
            // Smooth interpolation toward target
            barHeights[i] += (barTargets[i] - barHeights[i]) * 0.25f
            barTargets[i] *= 0.88f  // decay
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val barW = w / BAR_COUNT
        val gap = barW * 0.15f

        for (i in 0 until BAR_COUNT) {
            val barH = barHeights[i].coerceIn(0.05f, 1f) * h
            val left = i * barW + gap
            val right = left + barW - gap * 2
            val top = h - barH

            // Pick colour based on height
            val colorIdx = ((barHeights[i] * (barColors.size - 1)).toInt()).coerceIn(0, barColors.size - 1)
            barPaint.color = barColors[colorIdx]
            barPaint.alpha = 200

            canvas.drawRoundRect(left, top, right, h, 2f, 2f, barPaint)
        }
    }
}
