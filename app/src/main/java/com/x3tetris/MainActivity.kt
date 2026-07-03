package com.x3tetris

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

/**
 * X3TETRIS — host activity. Right temple pad (cyttsp5) is the game pad;
 * screen touches mirror it for flat testing. Tap places the piece; double-tap
 * opens settings; inside the menu: swipe up/down moves, tap selects,
 * double-tap exits.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var glView: GLSurfaceView
    private lateinit var game: GameState
    private lateinit var sfx: Sfx
    private lateinit var music: MusicPlayer
    private lateinit var gestures: GameGestures
    private val menuItemCount = 8

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUi()

        game = GameState()
        sfx = Sfx(this)
        sfx.preload("move", "rotate", "tick", "harddrop", "lock", "bump", "hold",
            "clear1", "clear2", "clear3", "tetris", "tspin", "combo",
            "levelup", "gameover", "panda", "menu", "chroma")
        music = MusicPlayer(this)

        glView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 0, 0, 0)
            setRenderer(GameRenderer(game, sfx, music))
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        setContentView(glView)
        music.playIntro()

        gestures = GameGestures(
            onSwipeH = { dir ->
                if (AppState.menuOpen) return@GameGestures
                AppState.actions.add(if (dir < 0) 1 else 2)
            },
            onSwipeUp = {
                if (AppState.menuOpen) moveMenu(-1) else AppState.actions.add(3)
            },
            onSwipeDown = {
                if (AppState.menuOpen) moveMenu(1) else AppState.actions.add(4)
            },
            onTap = { onTap() },
            onDoubleTap = { onDoubleTap() },
            onLongPress = { onLongPress() }
        )
    }

    // ---------------- gesture semantics ----------------

    private fun onTap() {
        when {
            AppState.menuOpen -> selectMenuItem()
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
        closeMenu()
    }

    private fun moveMenu(delta: Int) {
        AppState.menuIndex = (AppState.menuIndex + delta + menuItemCount) % menuItemCount
        sfx.play("menu", 0.7f)
    }

    private fun selectMenuItem() {
        when (AppState.menuIndex) {
            0 -> { AppState.menuOpen = false; AppState.paused = false; music.resume() }
            1 -> {
                AppState.menuOpen = false; AppState.paused = false
                game.restart(); AppState.phase = AppState.PHASE_PLAY
                music.playForLevel(1); music.resume()
                AppState.say("RESTARTED. THE WELL FORGIVES.", 2500)
            }
            2 -> {                                                // SKILL tier
                AppState.skill = (AppState.skill + 1) % 4
                AppState.say("SKILL: ${AppState.SKILL_NAMES[AppState.skill]}" +
                        if (AppState.skill == 3) " — ROWS ONLY, FAST, ×1.5 SCORE"
                        else " — STRAIGHT RUNS POP AT ${game.chromaThreshold()}+", 3200)
            }
            3 -> { AppState.musicVol = step(AppState.musicVol); music.applyVolume() }
            4 -> { AppState.sfxVol = step(AppState.sfxVol); sfx.play("clear1") }
            5 -> AppState.ghostOn = !AppState.ghostOn
            6 -> AppState.swapAxes = !AppState.swapAxes
            7 -> AppState.invertMove = !AppState.invertMove
        }
        sfx.play("menu", 0.8f)
    }

    private fun closeMenu() {
        AppState.menuOpen = false
        if (AppState.phase == AppState.PHASE_PLAY) {
            AppState.paused = false
            music.resume()
        } else {
            music.playIntro()
        }
        sfx.play("menu", 0.7f)
    }

    private fun onLongPress() {
        if (AppState.menuOpen) {
            closeMenu()
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
        if (AppState.phase == AppState.PHASE_PLAY && !AppState.paused) {
            music.resume()
        } else if (!AppState.menuOpen) {
            music.playIntro()
        }
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
