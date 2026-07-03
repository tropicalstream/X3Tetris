package com.tetrallama

import android.content.Context
import android.media.MediaPlayer
import java.io.File

/**
 * Title music and level music. "Astro Vinyl Intro.mp3" is reserved for the
 * start screen; every other MP3 in assets/music/ is mapped to levels in
 * alphabetical order, with the last track carrying all higher levels.
 */
class MusicPlayer(private val ctx: Context) {
    private var introTrack: String? = null
    private var levelTracks: List<String> = emptyList()
    private var current: MediaPlayer? = null
    private var currentKey = ""

    init {
        val tracks = runCatching {
            ctx.assets.list("music")?.filter { it.endsWith(".mp3", true) }?.sorted() ?: emptyList()
        }.getOrDefault(emptyList())
        introTrack = tracks.firstOrNull { it.equals("Astro Vinyl Intro.mp3", ignoreCase = true) }
        levelTracks = tracks.filterNot { it == introTrack }
    }

    fun playIntro() {
        introTrack?.let { playTrack("intro:$it", it) }
    }

    fun playForLevel(level: Int) {
        if (levelTracks.isEmpty()) return
        val idx = (level - 1).coerceIn(0, levelTracks.size - 1)
        playTrack("level:$idx:${levelTracks[idx]}", levelTracks[idx])
    }

    private fun playTrack(key: String, assetName: String) {
        if (key == currentKey && current?.isPlaying == true) return
        currentKey = key
        runCatching {
            current?.release()
            val f = File(ctx.cacheDir, "music_$assetName")
            if (!f.exists() || f.length() == 0L) {
                ctx.assets.open("music/$assetName").use { i -> f.outputStream().use { i.copyTo(it) } }
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
    fun stop() { runCatching { current?.release() }; current = null; currentKey = "" }
}
