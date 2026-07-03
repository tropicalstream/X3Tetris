package com.tetrallama

import kotlin.math.sin

/**
 * The mandatory llama. A neon vector llama ambles across deep background,
 * nodding to the beat. On a Tetris it STAMPEDES across the foreground,
 * because Jeff Minter taught us that this is simply what must happen.
 */
class Llama {
    // side-view outline, unit-ish coordinates (x right, y up)
    private val outline = arrayOf(
        // back leg pair
        floatArrayOf(-1.6f, -2.0f), floatArrayOf(-1.5f, -0.6f),
        floatArrayOf(-1.5f, -0.6f), floatArrayOf(-1.9f, -0.5f),
        floatArrayOf(-1.9f, -0.5f), floatArrayOf(-2.0f, -2.0f),
        // body
        floatArrayOf(-2.0f, -0.4f), floatArrayOf(-1.0f, 0.2f),
        floatArrayOf(-1.0f, 0.2f), floatArrayOf(0.9f, 0.2f),
        floatArrayOf(0.9f, 0.2f), floatArrayOf(1.4f, -0.3f),
        floatArrayOf(-2.0f, -0.4f), floatArrayOf(-1.6f, -0.5f),
        // front legs
        floatArrayOf(0.9f, -2.0f), floatArrayOf(1.0f, -0.4f),
        floatArrayOf(1.3f, -0.4f), floatArrayOf(1.4f, -2.0f),
        // tail
        floatArrayOf(-2.0f, -0.4f), floatArrayOf(-2.3f, 0.1f),
        // neck (long, proud)
        floatArrayOf(1.1f, 0.2f), floatArrayOf(1.5f, 1.8f),
        floatArrayOf(1.5f, 1.8f), floatArrayOf(1.75f, 1.85f),
        // head + snoot
        floatArrayOf(1.75f, 1.85f), floatArrayOf(2.25f, 1.7f),
        floatArrayOf(2.25f, 1.7f), floatArrayOf(2.2f, 1.5f),
        floatArrayOf(2.2f, 1.5f), floatArrayOf(1.7f, 1.5f),
        // ears (banana-shaped, naturally)
        floatArrayOf(1.7f, 1.9f), floatArrayOf(1.6f, 2.35f),
        floatArrayOf(1.85f, 1.9f), floatArrayOf(1.95f, 2.3f))

    var stampede = 0f          // >0 while charging the foreground
    private var t = 0f
    private val rgb = FloatArray(3)

    fun tetris() { stampede = 3.2f }

    fun update(dt: Float) {
        t += dt
        if (stampede > 0f) stampede -= dt
    }

    fun emit(batch: LineBatch, hueBase: Float) {
        // ambient llama: paces deep background, gentle nod
        drawAt(batch, -14f + (t * 0.7f) % 34f, -4.5f, -26f, 1.4f, hueBase + 0.55f, 0.5f)
        if (stampede > 0f) {
            // STAMPEDE: giant llama gallops across the foreground
            val p = 1f - stampede / 3.2f
            val x = -22f + p * 46f
            drawAt(batch, x, -3.5f + sin(t * 14f) * 0.6f, 6f, 3.6f, hueBase, 1f,
                gallop = sin(t * 16f) * 0.35f)
        }
    }

    private fun drawAt(batch: LineBatch, x: Float, y: Float, z: Float, s: Float,
                       hue: Float, alpha: Float, gallop: Float = 0f) {
        GlUtil.hue(hue, rgb)
        val nod = sin(t * 2.3f) * 0.08f
        var i = 0
        while (i < outline.size) {
            val a = outline[i]; val b = outline[i + 1]
            // legs swing when galloping (y < -0.5 == leg points)
            val ga = if (a[1] < -0.5f) gallop * (if (a[0] < 0) 1f else -1f) else nod
            val gb = if (b[1] < -0.5f) gallop * (if (b[0] < 0) 1f else -1f) else nod
            batch.line(x + (a[0] + ga) * s, y + a[1] * s, z,
                x + (b[0] + gb) * s, y + b[1] * s, z,
                rgb[0], rgb[1], rgb[2], alpha)
            i += 2
        }
        // the eye — always watching
        batch.glow(x + 1.95f * s, y + 1.75f * s, z, 7f * s / 1.4f, 1f, 1f, 1f, alpha)
    }
}
