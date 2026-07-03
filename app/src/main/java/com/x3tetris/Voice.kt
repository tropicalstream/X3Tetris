package com.x3tetris

import android.content.Context
import android.media.MediaPlayer
import java.io.File

/**
 * The announcer — Fish-voiced (pre-baked OGGs in assets/voice/, generated once
 * by app/tools/generate_voice.py with voice model a387e2e593f74e899e45cf17a7c81dd7).
 * Engagement without nagging: one line at a time, a hard global cooldown, and
 * per-pool round-robin so nothing repeats back-to-back. Missing files = silence,
 * so the game runs fine before the clips are rendered.
 */
class Voice(private val ctx: Context) {
    @Volatile var lastSpokeAt = 0L
        private set
    private var mp: MediaPlayer? = null
    private val counters = HashMap<String, Int>()
    private val poolSizes = mapOf(
        "welcome" to 2, "levelup" to 4, "tetris" to 3, "fireworks" to 3,
        "gameover" to 3, "idle" to 4, "almost" to 2, "chroma" to 2, "b2b" to 1)

    /**
     * Speaks the next line of [pool] if the global cooldown allows and the
     * dice agree. [cooldownMs] = 0 forces it (welcome/game over).
     */
    fun say(pool: String, cooldownMs: Long = 45_000, chance: Float = 1f): Boolean {
        val now = System.currentTimeMillis()
        if (cooldownMs > 0 && now - lastSpokeAt < cooldownMs) return false
        if (chance < 1f && Math.random() > chance) return false
        if (mp?.isPlaying == true) return false
        val n = poolSizes[pool] ?: 1
        val idx = counters.getOrDefault(pool, 0) % n
        val f = cache("voice/$pool${idx + 1}.ogg") ?: return false
        counters[pool] = idx + 1
        return runCatching {
            mp?.release()
            val p = MediaPlayer()
            mp = p
            p.setDataSource(f.absolutePath)
            p.setOnPreparedListener {
                val v = AppState.sfxVol
                p.setVolume(v, v)
                p.start()
            }
            p.prepareAsync()
            lastSpokeAt = now
            true
        }.getOrDefault(false)
    }

    private fun cache(assetPath: String): File? = try {
        val out = File(ctx.cacheDir, assetPath.replace('/', '_'))
        if (!out.exists() || out.length() == 0L) {
            ctx.assets.open(assetPath).use { i -> out.outputStream().use { i.copyTo(it) } }
        }
        out
    } catch (e: Exception) { null }

    fun release() { runCatching { mp?.release() }; mp = null }
}
