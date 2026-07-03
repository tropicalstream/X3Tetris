package com.x3tetris

import kotlin.math.sin

/**
 * The resident panda. A neon vector panda face floats through a bamboo grove
 * in the deep background, munching as it goes. On a Tetris it charges across
 * the foreground in a very un-panda-like burst of enthusiasm.
 */
class Panda {
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
        // ambient panda: a front-facing head through the grove, nodding while it munches
        drawAt(batch, -16f + (t * 0.5f) % 36f, -5.2f, -26f, 1.68f, hueBase + 0.55f, 0.5f)
        if (charge > 0f) {
            // TETRIS! the panda face rushes across the glass
            val p = 1f - charge / 3.2f
            val x = -22f + p * 46f
            drawAt(batch, x, -3.5f + sin(t * 14f) * 0.6f, 6f, 4.08f, hueBase, 1f,
                run = sin(t * 16f) * 0.18f)
        }
    }

    private fun drawAt(batch: LineBatch, x: Float, y: Float, z: Float, s: Float,
                       hue: Float, alpha: Float, run: Float = 0f) {
        GlUtil.hue(hue, rgb)
        val nod = sin(t * 2.1f) * 0.08f + run

        // unmistakable front-facing panda: round head, two round ears.
        oval(batch, x, y + nod * s, z, 1.28f * s, 1.05f * s, rgb[0], rgb[1], rgb[2], alpha)
        oval(batch, x - 0.82f * s, y + (0.78f + nod) * s, z, 0.36f * s, 0.34f * s, rgb[0], rgb[1], rgb[2], alpha)
        oval(batch, x + 0.82f * s, y + (0.78f + nod) * s, z, 0.36f * s, 0.34f * s, rgb[0], rgb[1], rgb[2], alpha)

        // black eye patches as tilted neon loops, with bright pupils inside.
        GlUtil.hue(hue + 0.58f, rgb2, 0.8f, 0.9f)
        oval(batch, x - 0.43f * s, y + (0.28f + nod) * s, z, 0.34f * s, 0.48f * s, rgb2[0], rgb2[1], rgb2[2], alpha)
        oval(batch, x + 0.43f * s, y + (0.28f + nod) * s, z, 0.34f * s, 0.48f * s, rgb2[0], rgb2[1], rgb2[2], alpha)
        batch.glow(x - 0.43f * s, y + (0.31f + nod) * s, z, 4.6f * s / 1.4f, 1f, 1f, 1f, alpha)
        batch.glow(x + 0.43f * s, y + (0.31f + nod) * s, z, 4.6f * s / 1.4f, 1f, 1f, 1f, alpha)

        // muzzle, nose, tiny mouth.
        oval(batch, x, y + (-0.28f + nod) * s, z, 0.42f * s, 0.28f * s, rgb[0], rgb[1], rgb[2], alpha)
        batch.glow(x, y + (-0.18f + nod) * s, z, 5.5f * s / 1.4f, rgb2[0], rgb2[1], rgb2[2], alpha)
        batch.line(x, y + (-0.23f + nod) * s, z, x, y + (-0.40f + nod) * s, z, rgb[0], rgb[1], rgb[2], alpha)
        batch.line(x - 0.18f * s, y + (-0.45f + nod) * s, z, x, y + (-0.40f + nod) * s, z, rgb[0], rgb[1], rgb[2], alpha)
        batch.line(x, y + (-0.40f + nod) * s, z, x + 0.18f * s, y + (-0.45f + nod) * s, z, rgb[0], rgb[1], rgb[2], alpha)

        // Bamboo snack crossing the face.
        GlUtil.hue(0.33f, rgb2, 0.95f, 0.95f)
        batch.line(x + 0.15f * s, y + (-0.42f + nod) * s, z, x + 1.12f * s, y + (-0.75f + nod) * s, z,
            rgb2[0], rgb2[1], rgb2[2], alpha)
        batch.line(x + 0.48f * s, y + (-0.52f + nod) * s, z, x + 0.60f * s, y + (-0.32f + nod) * s, z,
            rgb2[0], rgb2[1], rgb2[2], alpha * 0.8f)
        batch.line(x + 0.78f * s, y + (-0.63f + nod) * s, z, x + 0.92f * s, y + (-0.42f + nod) * s, z,
            rgb2[0], rgb2[1], rgb2[2], alpha * 0.8f)
    }

    private fun oval(batch: LineBatch, cx: Float, cy: Float, z: Float, rx: Float, ry: Float,
                     r: Float, g: Float, b: Float, a: Float) {
        val n = 24
        for (i in 0 until n) {
            val a0 = i / n.toFloat() * 6.283185f
            val a1 = (i + 1) / n.toFloat() * 6.283185f
            batch.line(cx + kotlin.math.cos(a0) * rx, cy + kotlin.math.sin(a0) * ry, z,
                cx + kotlin.math.cos(a1) * rx, cy + kotlin.math.sin(a1) * ry, z,
                r, g, b, a)
        }
    }
}
