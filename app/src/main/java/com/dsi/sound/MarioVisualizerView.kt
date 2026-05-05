package com.dsi.sound

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.random.Random

/**
 * Animated Mario World visualizer — Mario and Luigi run across the screen,
 * coins spin overhead, blocks bounce, all reacting to the music amplitude.
 */
class MarioVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // ── Sprite resource IDs ──────────────────────────────────────────────
    private val marioWalkIds = intArrayOf(
        R.drawable.mario_walk0, R.drawable.mario_walk1,
        R.drawable.mario_walk2, R.drawable.mario_walk3
    )
    private val luigiWalkIds = intArrayOf(
        R.drawable.luigi_walk0, R.drawable.luigi_walk1, R.drawable.luigi_walk2
    )
    private val coinIds = intArrayOf(
        R.drawable.coin_0, R.drawable.coin_1, R.drawable.coin_2, R.drawable.coin_3
    )
    private val starIds = intArrayOf(
        R.drawable.star_0, R.drawable.star_1, R.drawable.star_2,
        R.drawable.star_3, R.drawable.star_4, R.drawable.star_5
    )

    // ── Bitmap caches ────────────────────────────────────────────────────
    private val marioWalk = arrayOfNulls<Bitmap>(4)
    private val luigiWalk = arrayOfNulls<Bitmap>(3)
    private val coinFrames = arrayOfNulls<Bitmap>(4)
    private val starFrames = arrayOfNulls<Bitmap>(6)
    private var bgBitmap: Bitmap? = null
    private var blockBitmap: Bitmap? = null
    private var questionBitmap: Bitmap? = null
    private var brickBitmap: Bitmap? = null

    // ── Paint ────────────────────────────────────────────────────────────
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val pixelPaint = Paint().apply {
        // No filtering — keep pixel art crisp
        isFilterBitmap = false
        isAntiAlias = false
    }

    // ── Animation state ─────────────────────────────────────────────────
    private var tick = 0L
    private var amplitude = 0f
    private var isPlaying = false

    // Mario character
    private var marioX = 0f
    private var marioY = 0f
    private var marioVelY = 0f
    private var marioFrame = 0
    private var marioOnGround = true
    private var marioSpeed = 3f

    // Luigi character
    private var luigiX = -200f
    private var luigiY = 0f
    private var luigiVelY = 0f
    private var luigiFrame = 0
    private var luigiOnGround = true

    // Coins
    data class Coin(var x: Float, var y: Float, var baseY: Float, var frame: Int = 0, var active: Boolean = true)
    private val coins = mutableListOf<Coin>()

    // Flying stars (beat particles)
    data class Star(var x: Float, var y: Float, var vx: Float, var vy: Float,
                    var frame: Int = 0, var life: Int = 20)
    private val stars = mutableListOf<Star>()

    // Ground blocks
    data class Block(val x: Float, val y: Float, val isQuestion: Boolean,
                     var bouncing: Boolean = false, var bounceY: Float = 0f)
    private val blocks = mutableListOf<Block>()

    private var groundY = 0f
    private var spriteSize = 0f
    private var bgScrollX = 0f
    private var initialized = false

    // ── Public API ───────────────────────────────────────────────────────
    fun setAmplitude(amp: Float) {
        val prev = amplitude
        amplitude = amp.coerceIn(0f, 1f)
        // Big beat = Mario jumps
        if (amplitude > 0.7f && prev < 0.5f && marioOnGround) {
            marioVelY = -spriteSize * 0.35f
            marioOnGround = false
            spawnStars(marioX + spriteSize / 2, marioY)
        }
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

    private val animRunnable = object : Runnable {
        override fun run() {
            if (isPlaying) {
                tick++
                update()
                invalidate()
                postDelayed(this, 33) // ~30fps
            }
        }
    }

    // ── Initialisation ───────────────────────────────────────────────────
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        spriteSize = (w / 12f).coerceIn(24f, 48f)
        groundY = h * 0.75f
        marioX = w * 0.25f
        marioY = groundY - spriteSize
        luigiX = -spriteSize * 3
        luigiY = groundY - spriteSize
        setupBlocks()
        setupCoins()
        initialized = true
    }

    private fun setupBlocks() {
        blocks.clear()
        val blockY = groundY - spriteSize * 2.5f
        val spacing = width / 5f
        for (i in 0..4) {
            blocks.add(Block(
                x = spacing * i + spacing * 0.5f,
                y = blockY,
                isQuestion = i % 2 == 0
            ))
        }
    }

    private fun setupCoins() {
        coins.clear()
        val coinY = groundY - spriteSize * 4f
        val spacing = width / 8f
        for (i in 0..7) {
            coins.add(Coin(
                x = spacing * i + spacing * 0.3f,
                y = coinY + sin(i * 0.8).toFloat() * spriteSize,
                baseY = coinY + sin(i * 0.8).toFloat() * spriteSize
            ))
        }
    }

    private fun spawnStars(x: Float, y: Float) {
        repeat(4) {
            stars.add(Star(
                x = x, y = y,
                vx = (Random.nextFloat() - 0.5f) * spriteSize * 0.4f,
                vy = -Random.nextFloat() * spriteSize * 0.3f,
                frame = Random.nextInt(6)
            ))
        }
    }

    // ── Update logic ─────────────────────────────────────────────────────
    private fun update() {
        val gravity = spriteSize * 0.04f
        marioSpeed = 2f + amplitude * 6f

        // Scroll background
        bgScrollX += marioSpeed * 0.5f

        // ── Mario ──
        marioX += marioSpeed
        if (marioX > width + spriteSize) marioX = -spriteSize

        // Gravity
        if (!marioOnGround) {
            marioVelY += gravity
            marioY += marioVelY
            if (marioY >= groundY - spriteSize) {
                marioY = groundY - spriteSize
                marioVelY = 0f
                marioOnGround = true
            }
        }
        if (tick % 6 == 0L) marioFrame = (marioFrame + 1) % 4

        // ── Luigi ── (follows Mario with offset)
        luigiX += marioSpeed * 0.85f
        if (luigiX > width + spriteSize) luigiX = -spriteSize * 4
        if (tick % 7 == 0L) luigiFrame = (luigiFrame + 1) % 3

        // ── Coins bob ──
        coins.forEach { coin ->
            coin.y = coin.baseY + sin(tick * 0.1 + coin.x * 0.05).toFloat() * spriteSize * 0.3f
            if (tick % 5 == 0L) coin.frame = (coin.frame + 1) % 4
        }

        // ── Block bounces on beat ──
        if (amplitude > 0.6f) {
            blocks.forEach { block ->
                if (!block.bouncing && Random.nextFloat() < 0.3f) {
                    block.bouncing = true
                    block.bounceY = 0f
                }
            }
        }
        blocks.forEach { block ->
            if (block.bouncing) {
                block.bounceY -= spriteSize * 0.08f
                if (block.bounceY < -spriteSize * 0.4f) {
                    block.bounceY = 0f
                    block.bouncing = false
                }
            }
        }

        // ── Stars ──
        val starIter = stars.iterator()
        while (starIter.hasNext()) {
            val s = starIter.next()
            s.x += s.vx
            s.y += s.vy
            s.vy += gravity * 0.5f
            s.life--
            if (tick % 3 == 0L) s.frame = (s.frame + 1) % 6
            if (s.life <= 0) starIter.remove()
        }
    }

    // ── Drawing ─────────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!initialized) return

        // Background
        drawBackground(canvas)

        // Ground blocks row
        drawGround(canvas)

        // Floating blocks
        blocks.forEach { block ->
            val bmp = if (block.isQuestion) getQuestionBitmap() else getBrickBitmap()
            bmp?.let {
                drawSprite(canvas, it, block.x, block.y + block.bounceY, spriteSize)
            }
        }

        // Coins
        coins.forEach { coin ->
            getCoinFrame(coin.frame)?.let {
                drawSprite(canvas, it, coin.x, coin.y, spriteSize * 0.8f)
            }
        }

        // Luigi
        getLuigiFrame(luigiFrame)?.let {
            drawSprite(canvas, it, luigiX, luigiY, spriteSize)
        }

        // Mario
        getMarioFrame(marioFrame)?.let {
            drawSprite(canvas, it, marioX, marioY, spriteSize)
        }

        // Stars
        stars.forEach { star ->
            getStarFrame(star.frame)?.let {
                val alpha = (star.life * 255f / 20f).toInt().coerceIn(0, 255)
                pixelPaint.alpha = alpha
                drawSprite(canvas, it, star.x, star.y, spriteSize * 0.6f, pixelPaint)
                pixelPaint.alpha = 255
            }
        }
    }

    private fun drawBackground(canvas: Canvas) {
        val bmp = getBgBitmap() ?: return
        val scale = height.toFloat() / bmp.height
        val scaledW = bmp.width * scale
        var x = -(bgScrollX % scaledW)
        while (x < width) {
            canvas.drawBitmap(bmp, null, RectF(x, 0f, x + scaledW, height.toFloat()), paint)
            x += scaledW
        }
    }

    private fun drawGround(canvas: Canvas) {
        val bmp = getBlockBitmap() ?: return
        var x = 0f
        while (x < width + spriteSize) {
            drawSprite(canvas, bmp, x, groundY, spriteSize)
            drawSprite(canvas, bmp, x, groundY + spriteSize, spriteSize)
            x += spriteSize
        }
    }

    private fun drawSprite(canvas: Canvas, bmp: Bitmap, x: Float, y: Float,
                            size: Float, p: Paint = pixelPaint) {
        canvas.drawBitmap(bmp, null, RectF(x, y, x + size, y + size), p)
    }

    // ── Lazy bitmap loaders ──────────────────────────────────────────────
    private fun getMarioFrame(f: Int): Bitmap? {
        val i = f.coerceIn(0, 3)
        if (marioWalk[i] == null) marioWalk[i] = load(marioWalkIds[i])
        return marioWalk[i]
    }

    private fun getLuigiFrame(f: Int): Bitmap? {
        val i = f.coerceIn(0, 2)
        if (luigiWalk[i] == null) luigiWalk[i] = load(luigiWalkIds[i])
        return luigiWalk[i]
    }

    private fun getCoinFrame(f: Int): Bitmap? {
        val i = f.coerceIn(0, 3)
        if (coinFrames[i] == null) coinFrames[i] = load(coinIds[i])
        return coinFrames[i]
    }

    private fun getStarFrame(f: Int): Bitmap? {
        val i = f.coerceIn(0, 5)
        if (starFrames[i] == null) starFrames[i] = load(starIds[i])
        return starFrames[i]
    }

    private fun getBgBitmap(): Bitmap? {
        if (bgBitmap == null) bgBitmap = load(R.drawable.bg_mario)
        return bgBitmap
    }

    private fun getBlockBitmap(): Bitmap? {
        if (blockBitmap == null) blockBitmap = load(R.drawable.block)
        return blockBitmap
    }

    private fun getQuestionBitmap(): Bitmap? {
        if (questionBitmap == null) questionBitmap = load(R.drawable.mario_question)
        return questionBitmap
    }

    private fun getBrickBitmap(): Bitmap? {
        if (brickBitmap == null) brickBitmap = load(R.drawable.brick_tile)
        return brickBitmap
    }

    private fun load(resId: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inScaled = false }
        return BitmapFactory.decodeResource(resources, resId, opts)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
        marioWalk.fill(null)
        luigiWalk.fill(null)
        coinFrames.fill(null)
        starFrames.fill(null)
        bgBitmap = null
        blockBitmap = null
        questionBitmap = null
        brickBitmap = null
    }
}
