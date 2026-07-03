package com.x3tetris

import kotlin.random.Random

/**
 * The BANG. CPU particle pool fed into the LineBatch as glow sprites and
 * short velocity streaks — line clears detonate, Tetrises go thermonuclear.
 */
class Particles(private val max: Int = 1400) {
    private val px = FloatArray(max); private val py = FloatArray(max); private val pz = FloatArray(max)
    private val vx = FloatArray(max); private val vy = FloatArray(max); private val vz = FloatArray(max)
    private val life = FloatArray(max); private val hue = FloatArray(max); private val size = FloatArray(max)
    private var head = 0
    private val rnd = Random(1)
    private val rgb = FloatArray(3)

    fun burst(x: Float, y: Float, z: Float, count: Int, baseHue: Float, speed: Float = 9f) {
        repeat(count) {
            val i = head; head = (head + 1) % max
            px[i] = x; py[i] = y; pz[i] = z
            val a = rnd.nextFloat() * 6.283f
            val b = (rnd.nextFloat() - 0.35f) * 3.14f
            val s = speed * (0.3f + rnd.nextFloat())
            vx[i] = (kotlin.math.cos(a) * kotlin.math.cos(b) * s)
            vy[i] = (kotlin.math.sin(b) * s)
            vz[i] = (kotlin.math.sin(a) * kotlin.math.cos(b) * s)
            life[i] = 0.7f + rnd.nextFloat() * 0.9f
            hue[i] = baseHue + (rnd.nextFloat() - 0.5f) * 0.18f
            size[i] = 6f + rnd.nextFloat() * 14f
        }
    }

    fun update(dt: Float) {
        for (i in 0 until max) {
            if (life[i] <= 0f) continue
            life[i] -= dt
            px[i] += vx[i] * dt; py[i] += vy[i] * dt; pz[i] += vz[i] * dt
            vy[i] -= 6f * dt                     // a little gravity: sparks fall
            vx[i] *= (1f - dt * 0.6f); vz[i] *= (1f - dt * 0.6f)
        }
    }

    fun emit(batch: LineBatch) {
        for (i in 0 until max) {
            val l = life[i]
            if (l <= 0f) continue
            GlUtil.hue(hue[i], rgb)
            val a = (l * 1.2f).coerceAtMost(1f)
            batch.glow(px[i], py[i], pz[i], size[i] * a, rgb[0], rgb[1], rgb[2], a)
            batch.line(px[i], py[i], pz[i],
                px[i] - vx[i] * 0.04f, py[i] - vy[i] * 0.04f, pz[i] - vz[i] * 0.04f,
                rgb[0], rgb[1], rgb[2], a * 0.8f)
        }
    }
}
