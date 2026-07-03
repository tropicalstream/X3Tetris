package com.x3tetris

import kotlin.math.sin

/**
 * The resident panda. A neon vector panda ambles through a bamboo grove in
 * the deep background, munching as it goes. On a Tetris it CHARGES across
 * the foreground in a very un-panda-like burst of enthusiasm.
 */
class Panda {
    // side-view outline segments (x right, y up), chunky and unmistakably panda
    private val outline = arrayOf(
        // rump + back
        floatArrayOf(-1.8f, -0.2f), floatArrayOf(-1.6f, 0.7f),
        floatArrayOf(-1.6f, 0.7f), floatArrayOf(-0.2f, 1.0f),
        // back legs (stout)
        floatArrayOf(-1.7f, -1.6f), floatArrayOf(-1.8f, -0.2f),
        floatArrayOf(-1.2f, -1.6f), floatArrayOf(-1.15f, -0.5f),
        // belly
        floatArrayOf(-1.5f, -0.55f), floatArrayOf(0.6f, -0.6f),
        // front legs
        floatArrayOf(0.45f, -1.6f), floatArrayOf(0.5f, -0.55f),
        floatArrayOf(1.0f, -1.6f), floatArrayOf(1.05f, -0.3f),
        // chest up to head
        floatArrayOf(0.6f, -0.6f), floatArrayOf(1.15f, 0.5f),
        floatArrayOf(-0.2f, 1.0f), floatArrayOf(0.55f, 1.05f),
        // head (big round-ish)
        floatArrayOf(0.55f, 1.05f), floatArrayOf(1.05f, 1.45f),
        floatArrayOf(1.05f, 1.45f), floatArrayOf(1.75f, 1.35f),
        floatArrayOf(1.75f, 1.35f), floatArrayOf(2.0f, 0.95f),
        floatArrayOf(2.0f, 0.95f), floatArrayOf(1.7f, 0.55f),
        floatArrayOf(1.7f, 0.55f), floatArrayOf(1.15f, 0.5f),
        // round ears
        floatArrayOf(0.85f, 1.4f), floatArrayOf(0.75f, 1.75f),
        floatArrayOf(0.75f, 1.75f), floatArrayOf(1.05f, 1.7f),
        floatArrayOf(1.5f, 1.45f), floatArrayOf(1.55f, 1.8f),
        floatArrayOf(1.55f, 1.8f), floatArrayOf(1.85f, 1.6f),
        // eye patches (the signature)
        floatArrayOf(1.25f, 1.15f), floatArrayOf(1.45f, 0.95f),
        floatArrayOf(1.55f, 1.1f), floatArrayOf(1.75f, 0.95f),
        // held bamboo stalk, mid-munch
        floatArrayOf(1.9f, 0.55f), floatArrayOf(1.45f, -0.7f))

    var charge = 0f          // >0 while charging the foreground
    private var t = 0f
    private val rgb = FloatArray(3)
    private val rgb2 = FloatArray(3)

    fun tetris() { charge = 3.2f }

    fun update(dt: Float) {
        t += dt
        if (charge > 0f) charge -= dt
    }

    fun emit(batch: LineBatch, hueBase: Float) {
        // bamboo grove: swaying neon stalks across the deep background
        GlUtil.hue(0.33f, rgb2, 0.85f, 0.9f)                    // bamboo green
        for (i in 0 until 7) {
            val bx = -30f + i * 10f + sin(t * 0.3f + i * 2f) * 0.8f
            val sway = sin(t * 0.5f + i) * 1.4f
            var y = -8f
            var x = bx
            for (seg in 0 until 4) {                             // segmented stalk
                val ny = y + 4.2f
                val nx = bx + sway * (seg + 1) / 4f
                batch.line(x, y, -27f, nx, ny, -27f, rgb2[0], rgb2[1], rgb2[2], 0.35f)
                // node ring
                batch.line(nx - 0.35f, ny, -27f, nx + 0.35f, ny, -27f, rgb2[0], rgb2[1], rgb2[2], 0.3f)
                // leaf pair on upper segments
                if (seg >= 2) {
                    batch.line(nx, ny - 1f, -27f, nx + 1.6f, ny - 0.2f, -27f, rgb2[0], rgb2[1], rgb2[2], 0.28f)
                    batch.line(nx, ny - 2f, -27f, nx - 1.5f, ny - 1.2f, -27f, rgb2[0], rgb2[1], rgb2[2], 0.28f)
                }
                x = nx; y = ny
            }
        }
        // ambient panda: ambles through the grove, nodding while it munches
        drawAt(batch, -16f + (t * 0.5f) % 36f, -5.2f, -26f, 1.4f, hueBase + 0.55f, 0.5f)
        if (charge > 0f) {
            // TETRIS! the panda forgets it is famously sedentary
            val p = 1f - charge / 3.2f
            val x = -22f + p * 46f
            drawAt(batch, x, -3.5f + sin(t * 14f) * 0.6f, 6f, 3.4f, hueBase, 1f,
                run = sin(t * 16f) * 0.35f)
        }
    }

    private fun drawAt(batch: LineBatch, x: Float, y: Float, z: Float, s: Float,
                       hue: Float, alpha: Float, run: Float = 0f) {
        GlUtil.hue(hue, rgb)
        val nod = sin(t * 2.1f) * 0.07f
        var i = 0
        while (i < outline.size) {
            val a = outline[i]; val b = outline[i + 1]
            // stout legs pump when charging (y < -0.5 == leg points)
            val ga = if (a[1] < -0.5f) run * (if (a[0] < 0) 1f else -1f) else nod
            val gb = if (b[1] < -0.5f) run * (if (b[0] < 0) 1f else -1f) else nod
            batch.line(x + (a[0] + ga) * s, y + a[1] * s, z,
                x + (b[0] + gb) * s, y + b[1] * s, z,
                rgb[0], rgb[1], rgb[2], alpha)
            i += 2
        }
        // eyes inside the patches — always watching the well
        batch.glow(x + 1.37f * s, y + 1.05f * s, z, 5f * s / 1.4f, 1f, 1f, 1f, alpha)
        batch.glow(x + 1.66f * s, y + 1.02f * s, z, 5f * s / 1.4f, 1f, 1f, 1f, alpha)
    }
}
