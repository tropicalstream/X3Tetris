package com.tetrallama

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

/**
 * TETRA LLAMA 3D — host activity. Right temple pad (cyttsp5) is the game pad;
 * screen touches mirror it for flat testing. Tap places the piece; double-tap
 * opens settings; inside the menu: tap=next, double=select, hold=close.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var glView: GLSurfaceView
    private lateinit var game: GameState
    private lateinit var sfx: Sfx
    private lateinit var music: MusicPlayer
    private lateinit var gestures: GameGestures
    private val menuItemCount = 7

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUi()

        game = GameState()
        sfx = Sfx(this)
        sfx.preload("move", "rotate", "tick", "harddrop", "lock", "bump", "hold",
            "clear1", "clear2", "clear3", "tetris", "tspin", "combo",
            "levelup", "gameover", "yak", "menu")
        music = MusicPlayer(this)

        glView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 0, 0, 0)
            setRenderer(GameRenderer(game, sfx, music))
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        setContentView(glView)

        gestures = GameGestures(
            onSwipeH = { dir ->
                if (AppState.menuOpen) return@GameGestures
                AppState.actions.add(if (dir < 0) 1 else 2)
            },
            onSwipeUp = { if (!AppState.menuOpen) AppState.actions.add(3) },
            onSwipeDown = { if (!AppState.menuOpen) AppState.actions.add(4) },
            onTap = { onTap() },
            onDoubleTap = { onDoubleTap() },
            onLongPress = { onLongPress() }
        )
    }

    // ---------------- gesture semantics ----------------

    private fun onTap() {
        when {
            AppState.menuOpen -> {                                // menu: next item
                AppState.menuIndex = (AppState.menuIndex + 1) % menuItemCount
                sfx.play("menu", 0.7f)
            }
            AppState.phase != AppState.PHASE_PLAY -> AppState.actions.add(7)   // start / retry
            AppState.paused -> { AppState.paused = false; music.resume(); sfx.play("menu") }
            else -> AppState.actions.add(5)                       // TAP = PLACE PIECE
        }
    }

    private fun onDoubleTap() {
        if (!AppState.menuOpen) {                                 // open settings
            AppState.menuIndex = 0
            AppState.menuOpen = true
            AppState.paused = true
            music.pause()
            sfx.play("menu", 0.9f)
            return
        }
        when (AppState.menuIndex) {                               // select item
            0 -> { AppState.menuOpen = false; AppState.paused = false; music.resume() }
            1 -> {
                AppState.menuOpen = false; AppState.paused = false
                game.restart(); AppState.phase = AppState.PHASE_PLAY
                music.playForLevel(1); music.resume()
                AppState.say("RESTARTED. THE WELL FORGIVES.", 2500)
            }
            2 -> { AppState.musicVol = step(AppState.musicVol); music.applyVolume() }
            3 -> { AppState.sfxVol = step(AppState.sfxVol); sfx.play("clear1") }
            4 -> AppState.ghostOn = !AppState.ghostOn
            5 -> AppState.swapAxes = !AppState.swapAxes
            6 -> AppState.invertMove = !AppState.invertMove
        }
        sfx.play("menu", 0.8f)
    }

    private fun onLongPress() {
        if (AppState.menuOpen) {                                  // close menu (stay paused)
            AppState.menuOpen = false
            sfx.play("menu", 0.7f)
            return
        }
        if (AppState.phase == AppState.PHASE_PLAY && !AppState.paused) {
            AppState.actions.add(6)                               // HOLD piece (2002 tech!)
        }
    }

    private fun step(v: Float): Float = when {
        v < 0.2f -> 0.4f; v < 0.5f -> 0.8f; v < 0.9f -> 1.0f; else -> 0.0f
    }

    // route the right temple pad + screen touches into the game pad
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val name = ev.device?.name ?: ""
        if (name.contains("cyttsp5") || name.contains("cyttsp6")) return gestures.onTouchEvent(ev)
        gestures.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        val name = ev.device?.name ?: ""
        if (name.contains("cyttsp5") || name.contains("cyttsp6")) return gestures.onTouchEvent(ev)
        return super.dispatchGenericMotionEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
        if (AppState.phase == AppState.PHASE_PLAY && !AppState.paused) music.resume()
        hideSystemUi()
    }

    override fun onPause() {
        glView.onPause()
        if (AppState.phase == AppState.PHASE_PLAY) { AppState.paused = true }
        music.pause()
        super.onPause()
    }

    override fun onDestroy() {
        music.stop()
        sfx.release()
        super.onDestroy()
    }

    private fun hideSystemUi() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }
}
