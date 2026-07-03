package com.tetrallama

import android.content.Context
import android.media.MediaPlayer
import java.io.File

/**
 * Level music: drop YOUR MP3s into assets/music/ (any names — they're sorted
 * alphabetically and mapped to levels in order; the last track carries all
 * higher levels). Each loops until the level changes. No files = silence,
 * which Jeff would call a missed opportunity.
 */
class MusicPlayer(private val ctx: Context) {
    private var tracks: List<String> = emptyList()
    private var current: MediaPlayer? = null
    private var currentIdx = -1

    init {
        tracks = runCatching {
            ctx.assets.list("music")?.filter { it.endsWith(".mp3", true) }?.sorted() ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun playForLevel(level: Int) {
        if (tracks.isEmpty()) return
        val idx = (level - 1).coerceIn(0, tracks.size - 1)
        if (idx == currentIdx && current?.isPlaying == true) return
        currentIdx = idx
        runCatching {
            current?.release()
            val f = File(ctx.cacheDir, "music_${tracks[idx]}")
            if (!f.exists() || f.length() == 0L) {
                ctx.assets.open("music/${tracks[idx]}").use { i -> f.outputStream().use { i.copyTo(it) } }
            }
            val mp = MediaPlayer()
            mp.setDataSource(f.absolutePath)
            mp.isLooping = true
            mp.setOnPreparedListener {
                mp.setVolume(AppState.musicVol, AppState.musicVol)
                mp.start()
            }
            mp.prepareAsync()
            current = mp
        }
    }

    fun applyVolume() = runCatching {
        current?.setVolume(AppState.musicVol, AppState.musicVol)
    }

    fun pause() = runCatching { if (current?.isPlaying == true) current?.pause() }
    fun resume() = runCatching { current?.start() }
    fun stop() { runCatching { current?.release() }; current = null; currentIdx = -1 }
}
