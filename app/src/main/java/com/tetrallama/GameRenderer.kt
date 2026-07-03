package com.tetrallama

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Stereo (side-by-side) Tempest-2000-style renderer:
 *  · everything is glowing additive vectors (LineBatch, two-pass halo)
 *  · VIDEO FEEDBACK trails: each frame re-projects the previous frame,
 *    slightly zoomed + rotated + decayed — the classic T2K melt
 *  · hue cycles forever; level ups shift the palette; Tetrises flash white
 *  · a neon llama patrols the deep background and stampedes on Tetrises
 */
class GameRenderer(
    private val game: GameState,
    private val sfx: Sfx,
    private val music: MusicPlayer
) : GLSurfaceView.Renderer {

    companion object { const val EYE_OFF = 0.05f }

    private val batch = LineBatch()
    private val particles = Particles()
    private val llama = Llama()
    private val hudText = NeonText(512, 256, 40f, 16)
    private val msgText = NeonText(1024, 256, 88f, 18)
    private val cardText = NeonText(1200, 512, 52f, 34)
    private val menuText = NeonText(900, 640, 48f, 24)
    private val bigText = NeonText(1200, 512, 96f, 20)

    private var width = 0; private var height = 0
    private var timeSec = 0f
    private var lastNs = 0L
    private var hue = 0f
    private val rgb = FloatArray(3)
    private val rgb2 = FloatArray(3)
    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val vp = FloatArray(16)
    private val stars = FloatArray(360)     // xyz per star
    private val rnd = Random(9)

    // ping-pong feedback FBOs
    private val fbo = IntArray(2); private val fboTex = IntArray(2)
    private var cur = 0
    private var fbProg = 0
    private var fbPos = 0; private var fbUV = 0
    private var fbTex = 0; private var fbOff = 0; private var fbSpan = 0
    private var fbZoom = 0; private var fbRot = 0; private var fbDecay = 0; private var fbAdd = 0
    private val quad = GlUtil.buffer(floatArrayOf(
        -1f, -1f, 0f, 0f,   1f, -1f, 1f, 0f,   -1f, 1f, 0f, 1f,   1f, 1f, 1f, 1f))

    // piece hues: I O T S Z J L (guideline colors, neonized)
    private val pieceHue = floatArrayOf(0.50f, 0.14f, 0.78f, 0.33f, 0.0f, 0.62f, 0.08f)
    private var chromaCardShown = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)      // vectors: painter's order, additive
        batch.init()
        hudText.init(); msgText.init(); cardText.init(); menuText.init(); bigText.init()
        for (i in 0 until 120) {
            stars[i * 3] = (rnd.nextFloat() - 0.5f) * 70f
            stars[i * 3 + 1] = (rnd.nextFloat() - 0.5f) * 44f
            stars[i * 3 + 2] = -30f - rnd.nextFloat() * 30f
        }
        fbProg = GlUtil.program("""
            attribute vec2 aPos;
            attribute vec2 aUV;
            varying vec2 vNdc;
            void main() { vNdc = aPos; gl_Position = vec4(aPos, 0.0, 1.0); }
        """, """
            precision mediump float;
            varying vec2 vNdc;
            uniform sampler2D uTex;
            uniform vec2 uOff;         // this eye's rect in the texture
            uniform vec2 uSpan;
            uniform float uZoom;
            uniform float uRot;
            uniform float uDecay;
            uniform vec3 uAdd;         // flash tint
            void main() {
                float c = cos(uRot); float s = sin(uRot);
                vec2 p = vNdc / uZoom;
                p = vec2(p.x * c - p.y * s, p.x * s + p.y * c);
                vec2 uv = clamp(p * 0.5 + 0.5, 0.0, 1.0);
                vec3 col = texture2D(uTex, uOff + uv * uSpan).rgb * uDecay + uAdd;
                gl_FragColor = vec4(col, 1.0);
            }
        """)
        fbPos = GLES20.glGetAttribLocation(fbProg, "aPos")
        fbUV = GLES20.glGetAttribLocation(fbProg, "aUV")
        fbTex = GLES20.glGetUniformLocation(fbProg, "uTex")
        fbOff = GLES20.glGetUniformLocation(fbProg, "uOff")
        fbSpan = GLES20.glGetUniformLocation(fbProg, "uSpan")
        fbZoom = GLES20.glGetUniformLocation(fbProg, "uZoom")
        fbRot = GLES20.glGetUniformLocation(fbProg, "uRot")
        fbDecay = GLES20.glGetUniformLocation(fbProg, "uDecay")
        fbAdd = GLES20.glGetUniformLocation(fbProg, "uAdd")
        lastNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = w; height = h
        for (i in 0..1) {
            if (fbo[i] != 0) {
                GLES20.glDeleteFramebuffers(1, fbo, i)
                GLES20.glDeleteTextures(1, fboTex, i)
            }
            val id = IntArray(1)
            GLES20.glGenFramebuffers(1, id, 0); fbo[i] = id[0]
            GLES20.glGenTextures(1, id, 0); fboTex[i] = id[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex[i])
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, w, h, 0,
                GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, null)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[i])
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, fboTex[i], 0)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastNs) / 1e9f).coerceIn(0f, 0.08f)
        lastNs = now
        timeSec += dt
        hue = (hue + dt * (0.02f + game.level * 0.004f)) % 1f
        AppState.flash = (AppState.flash - dt * 2.2f).coerceAtLeast(0f)

        consumeActions()
        if (AppState.phase == AppState.PHASE_PLAY && !AppState.paused && !AppState.menuOpen) {
            game.update((dt * 1000).toLong())
        }
        drainEvents()
        particles.update(dt)
        llama.update(dt)

        // ---- pass 1: feedback + vectors into the current FBO ----
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[cur])
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        val half = width / 2
        val eyeAspect = half.toFloat() / height
        Matrix.perspectiveM(proj, 0, 55f, eyeAspect, 0.5f, 200f)
        for (eye in 0..1) {
            GLES20.glViewport(eye * half, 0, if (eye == 0) half else width - half, height)
            drawFeedback(eye)
            val off = if (eye == 0) -EYE_OFF else EYE_OFF
            val ex = sin(timeSec * 0.13f) * 2.6f + off * 20f
            val ey = 1.2f + sin(timeSec * 0.09f) * 0.9f
            Matrix.setLookAtM(view, 0, ex, ey, 27f, 0f, 0f, 0f, 0f, 1f, 0f)
            Matrix.multiplyMM(vp, 0, proj, 0, view, 0)
            batch.begin()
            emitScene()
            batch.draw(vp)
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        // ---- pass 2: blit FBO to screen, then crisp text HUD per eye ----
        GLES20.glViewport(0, 0, width, height)
        blitToScreen()
        for (eye in 0..1) {
            GLES20.glViewport(eye * half, 0, if (eye == 0) half else width - half, height)
            drawTextHud(eyeAspect)
        }
        cur = 1 - cur
    }

    // ---------------- input & events ----------------
    private fun consumeActions() {
        while (true) {
            val a = AppState.actions.poll() ?: break
            when (AppState.phase) {
                AppState.PHASE_TITLE -> if (a == 5 || a == 7) startGame()
                AppState.PHASE_OVER -> if (a == 5 || a == 7) startGame()
                else -> if (!AppState.menuOpen && !AppState.paused) when (a) {
                    1 -> if (game.moveX(-1)) sfx.play("move", 0.7f) else sfx.play("bump", 0.5f)
                    2 -> if (game.moveX(1)) sfx.play("move", 0.7f) else sfx.play("bump", 0.5f)
                    3 -> if (game.rotateCW()) sfx.play("rotate", 0.85f) else sfx.play("bump", 0.5f)
                    4 -> if (game.softDrop()) sfx.play("tick", 0.55f)
                    5 -> game.hardDrop()
                    6 -> if (game.holdPiece()) sfx.play("hold") else sfx.play("bump", 0.5f)
                }
            }
        }
    }

    private fun startGame() {
        game.restart()
        AppState.phase = AppState.PHASE_PLAY
        AppState.paused = false
        music.playForLevel(1)
        sfx.play("levelup", 0.9f)
        AppState.say("LEVEL 1 · GO!", 2000)
    }

    private fun drainEvents() {
        while (true) {
            val e = game.events.removeFirstOrNull() ?: break
            when (e.type) {
                "harddrop" -> { sfx.play("harddrop"); AppState.flash = 0.12f }
                "lock" -> sfx.play("lock", 0.8f)
                "hold" -> {}
                "clear" -> {
                    sfx.play(when (e.a) { 1 -> "clear1"; 2 -> "clear2"; 3 -> "clear3"; else -> "tetris" })
                    burstRows()
                    if (e.a >= 3) AppState.flash = 0.5f
                }
                "tetris" -> {
                    llama.tetris(); AppState.flash = 1f
                    AppState.say(e.text)
                    sfx.play("yak", 1f)
                }
                "tspinclear" -> { AppState.say(e.text); sfx.play("tspin") }
                "chroma" -> {
                    // color-group pop: bursts in the group's own colors, chain pitch rises
                    var i = 0
                    while (i < e.cells.size) {
                        val c = e.cells[i]; val r = e.cells[i + 1]; val v = e.cells[i + 2]
                        particles.burst(c - 5f + 0.5f, r - 10f + 0.5f, 0f, 5,
                            pieceHue[(v - 1).coerceIn(0, 6)], 7f)
                        i += 3
                    }
                    sfx.play("chroma", 0.9f, 1f + (e.a - 1) * 0.12f)
                    if (e.a >= 2) AppState.say("CHROMA CHAIN ×${e.a} — TASTY", 1800)
                    if (!chromaCardShown) {
                        chromaCardShown = true
                        AppState.card("CHROMA RULE · TETRIS 2 (1993)\n\nColor-matching came to Tetris in 1993.\nConnected same-color blocks pop at ${game.chromaThreshold()}+ —\nthe bar rises as you level. Wizards get nothing.", 6500)
                    }
                    AppState.flash = (AppState.flash + 0.15f).coerceAtMost(0.6f)
                }
                "message" -> AppState.say(e.text)
                "combo" -> { AppState.say("COMBO ×${e.a} — MOO?"); sfx.play("combo", 0.9f, 1f + e.a * 0.06f) }
                "levelup" -> {
                    sfx.play("levelup")
                    music.playForLevel(e.a)
                    AppState.card("LEVEL ${e.a} · TETRIS HISTORY\n\n${e.text}", 7000)
                    AppState.flash = 0.6f
                }
                "gameover" -> {
                    AppState.phase = AppState.PHASE_OVER
                    sfx.play("gameover"); AppState.flash = 1f
                    music.pause()
                    burstAll()
                }
                "restart" -> {}
            }
        }
    }

    private fun burstRows() {
        for (r in game.clearingRows) {
            val y = r - 10f + 0.5f
            for (c in 0 until GameState.W) {
                val idx = game.board[r * GameState.W + c]
                val h = if (idx > 0) pieceHue[idx - 1] else hue
                particles.burst(c - 5f + 0.5f, y, 0f, 6, h, 8f)
            }
        }
    }

    private fun burstAll() {
        for (r in 0 until GameState.VISIBLE_H) for (c in 0 until GameState.W) {
            if (game.board[r * GameState.W + c] != 0) {
                particles.burst(c - 5f + 0.5f, r - 10f + 0.5f, 0f, 3, hue, 12f)
            }
        }
    }

    // ---------------- vector scene ----------------
    private fun emitScene() {
        val t = timeSec
        // Tempest web: radial tunnel behind the well
        GlUtil.hue(hue + 0.5f, rgb, 1f, 0.5f)
        for (i in 0 until 16) {
            val a = i / 16f * 6.283f + t * 0.05f
            val r1 = 8f; val r2 = 34f
            batch.line(cos(a) * r1, sin(a) * r1, -18f, cos(a) * r2, sin(a) * r2, -34f,
                rgb[0], rgb[1], rgb[2], 0.35f)
        }
        // starfield glows
        GlUtil.hue(hue + 0.3f, rgb2, 0.6f, 1f)
        for (i in 0 until 120) {
            val tw = 0.4f + 0.6f * sin(t * 1.7f + i).coerceAtLeast(0f)
            batch.glow(stars[i * 3], stars[i * 3 + 1], stars[i * 3 + 2], 5f, rgb2[0], rgb2[1], rgb2[2], tw * 0.5f)
        }
        llama.emit(batch, hue)
        // well frame (hue-cycled) + inner grid
        GlUtil.hue(hue, rgb, 1f, 1f)
        frame(-5f, -10f, 5f, 10f, 0.9f)
        GlUtil.hue(hue + 0.08f, rgb, 1f, 0.4f)
        for (c in 1 until GameState.W) {
            batch.line(c - 5f, -10f, 0f, c - 5f, 10f, 0f, rgb[0], rgb[1], rgb[2], 0.10f)
        }
        for (r in 1 until GameState.VISIBLE_H) {
            batch.line(-5f, r - 10f, 0f, 5f, r - 10f, 0f, rgb[0], rgb[1], rgb[2], 0.10f)
        }
        // depth rails: the well recedes (3D!)
        GlUtil.hue(hue + 0.15f, rgb, 1f, 0.8f)
        for (p in arrayOf(floatArrayOf(-5f, -10f), floatArrayOf(5f, -10f),
                floatArrayOf(-5f, 10f), floatArrayOf(5f, 10f))) {
            batch.line(p[0], p[1], 0f, p[0] * 1.6f, p[1] * 1.6f, -16f, rgb[0], rgb[1], rgb[2], 0.35f)
        }

        if (AppState.phase == AppState.PHASE_PLAY || AppState.phase == AppState.PHASE_OVER) {
            // stack
            for (r in 0 until GameState.H) for (c in 0 until GameState.W) {
                val v = game.board[r * GameState.W + c]
                if (v == 0) continue
                val clearing = game.clearingRows.contains(r)
                val h = pieceHue[v - 1]
                val bright = if (clearing) 1.6f else 1f
                val a = if (clearing) (game.clearAnimMs / 380f).coerceIn(0f, 1f) else 0.85f
                cube(c - 5f + 0.5f, r - 10f + 0.5f, 0.46f, h, a, bright, clearing)
            }
            if (game.running) {
                // ghost (1998 says hello)
                if (AppState.ghostOn) {
                    val gy = game.ghostY
                    for (cell in GameState.SHAPES[game.pieceType][game.rot]) {
                        cube(game.px + cell[0] - 5f + 0.5f, gy + cell[1] - 10f + 0.5f,
                            0.44f, pieceHue[game.pieceType], 0.20f, 0.7f, false)
                    }
                }
                // falling piece: brightest thing in the well
                for (cell in GameState.SHAPES[game.pieceType][game.rot]) {
                    val x = game.px + cell[0] - 5f + 0.5f
                    val y = game.py + cell[1] - 10f + 0.5f
                    cube(x, y, 0.48f, pieceHue[game.pieceType], 1f, 1.5f, true)
                }
            }
            // hold (left) + next 3 (right) minis
            if (game.holdType >= 0) mini(game.holdType, -8.6f, 6.5f)
            game.nextQueue.forEachIndexed { i, tpe -> mini(tpe, 8.6f, 6.5f - i * 3.2f) }
        }
        particles.emit(batch)
    }

    private fun frame(x0: Float, y0: Float, x1: Float, y1: Float, a: Float) {
        batch.line(x0, y0, 0f, x1, y0, 0f, rgb[0], rgb[1], rgb[2], a)
        batch.line(x0, y1, 0f, x1, y1, 0f, rgb[0], rgb[1], rgb[2], a)
        batch.line(x0, y0, 0f, x0, y1, 0f, rgb[0], rgb[1], rgb[2], a)
        batch.line(x1, y0, 0f, x1, y1, 0f, rgb[0], rgb[1], rgb[2], a)
    }

    /** Neon wireframe cube for one cell (front + back square + connectors). */
    private fun cube(cx: Float, cy: Float, r: Float, h: Float, a: Float, bright: Float, glow: Boolean) {
        GlUtil.hue(h, rgb, 1f, bright)
        val z0 = r; val z1 = -r
        // front square
        batch.line(cx - r, cy - r, z0, cx + r, cy - r, z0, rgb[0], rgb[1], rgb[2], a)
        batch.line(cx + r, cy - r, z0, cx + r, cy + r, z0, rgb[0], rgb[1], rgb[2], a)
        batch.line(cx + r, cy + r, z0, cx - r, cy + r, z0, rgb[0], rgb[1], rgb[2], a)
        batch.line(cx - r, cy + r, z0, cx - r, cy - r, z0, rgb[0], rgb[1], rgb[2], a)
        // back square (dimmer)
        val ab = a * 0.5f
        batch.line(cx - r, cy - r, z1, cx + r, cy - r, z1, rgb[0], rgb[1], rgb[2], ab)
        batch.line(cx + r, cy - r, z1, cx + r, cy + r, z1, rgb[0], rgb[1], rgb[2], ab)
        batch.line(cx + r, cy + r, z1, cx - r, cy + r, z1, rgb[0], rgb[1], rgb[2], ab)
        batch.line(cx - r, cy + r, z1, cx - r, cy - r, z1, rgb[0], rgb[1], rgb[2], ab)
        // connectors
        batch.line(cx - r, cy - r, z0, cx - r, cy - r, z1, rgb[0], rgb[1], rgb[2], ab)
        batch.line(cx + r, cy - r, z0, cx + r, cy - r, z1, rgb[0], rgb[1], rgb[2], ab)
        batch.line(cx + r, cy + r, z0, cx + r, cy + r, z1, rgb[0], rgb[1], rgb[2], ab)
        batch.line(cx - r, cy + r, z0, cx - r, cy + r, z1, rgb[0], rgb[1], rgb[2], ab)
        if (glow) batch.glow(cx, cy, z0, 16f, rgb[0], rgb[1], rgb[2], a * 0.5f)
    }

    private fun mini(type: Int, ox: Float, oy: Float) {
        for (cell in GameState.SHAPES[type][0]) {
            cube(ox + (cell[0] - 1.5f) * 0.55f, oy + (cell[1] - 1f) * 0.55f, 0.24f,
                pieceHue[type], 0.8f, 1f, false)
        }
    }

    // ---------------- feedback / blit ----------------
    private fun drawFeedback(eye: Int) {
        GLES20.glUseProgram(fbProg)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex[1 - cur])
        GLES20.glUniform1i(fbTex, 0)
        GLES20.glUniform2f(fbOff, eye * 0.5f, 0f)
        GLES20.glUniform2f(fbSpan, 0.5f, 1f)
        GLES20.glUniform1f(fbZoom, 1.018f)
        GLES20.glUniform1f(fbRot, 0.004f + AppState.flash * 0.02f)
        GLES20.glUniform1f(fbDecay, 0.86f)
        val f = AppState.flash * 0.25f
        GLES20.glUniform3f(fbAdd, f, f, f * 1.2f)
        drawQuad()
    }

    private fun blitToScreen() {
        GLES20.glUseProgram(fbProg)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex[cur])
        GLES20.glUniform1i(fbTex, 0)
        GLES20.glUniform2f(fbOff, 0f, 0f)
        GLES20.glUniform2f(fbSpan, 1f, 1f)
        GLES20.glUniform1f(fbZoom, 1f)
        GLES20.glUniform1f(fbRot, 0f)
        GLES20.glUniform1f(fbDecay, 1f)
        GLES20.glUniform3f(fbAdd, 0f, 0f, 0f)
        drawQuad()
    }

    private fun drawQuad() {
        quad.position(0)
        GLES20.glVertexAttribPointer(fbPos, 2, GLES20.GL_FLOAT, false, 16, quad)
        GLES20.glEnableVertexAttribArray(fbPos)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(fbPos)
    }

    // ---------------- text HUD (crisp, no trails) ----------------
    private fun drawTextHud(eyeAspect: Float) {
        val now = System.currentTimeMillis()
        when (AppState.phase) {
            AppState.PHASE_TITLE -> {
                bigText.setText("TETRA LLAMA 3D")
                bigText.draw(0f, 0.45f, 0.16f, eyeAspect, 0.8f + 0.2f * sin(timeSec * 3f))
                cardText.setText("TAP · PLACE PIECE (hard drop)\nSWIPE FWD/BACK · MOVE\nSWIPE UP · SPIN  DOWN · SOFT DROP\nHOLD · KEEP A PIECE FOR LATER\nDOUBLE-TAP · SETTINGS\n\nSKILL: ${AppState.SKILL_NAMES[AppState.skill]} — color groups of ${if (AppState.skill == 3) "∞ (rows only!)" else "${game.chromaThreshold()}+"} pop.\n40 YEARS OF TETRIS, ONE NEON WELL.\nTAP TO BEGIN.")
                cardText.draw(0f, -0.35f, 0.32f, eyeAspect, 0.95f)
            }
            AppState.PHASE_OVER -> {
                bigText.setText("GAME OVER\n${"%,d".format(game.score)}")
                bigText.draw(0f, 0.25f, 0.22f, eyeAspect, 0.95f)
                cardText.setText("LINES ${game.lines} · LEVEL ${game.level}\nTHE LLAMA REMEMBERS.\nTAP TO GO AGAIN.")
                cardText.draw(0f, -0.42f, 0.2f, eyeAspect, 0.9f)
            }
            else -> {
                val thr = game.chromaThreshold()
                hudText.setText("SCORE ${"%,d".format(game.score)}\nLINES ${game.lines} · LVL ${game.level}" +
                        if (thr <= GameState.W * GameState.H) "\nCHROMA ≥$thr · ${AppState.SKILL_NAMES[AppState.skill]}"
                        else "\nWIZARD · ROWS ONLY")
                hudText.draw(-0.62f, 0.78f, 0.12f, eyeAspect, 0.9f)
                if (now < AppState.messageUntil && AppState.message.isNotBlank()) {
                    val fade = ((AppState.messageUntil - now) / 400f).coerceIn(0f, 1f)
                    msgText.setText(AppState.message)
                    msgText.draw(0f, 0.55f, 0.14f, eyeAspect, fade)
                }
                if (now < AppState.historyUntil && AppState.history.isNotBlank()) {
                    val fade = ((AppState.historyUntil - now) / 500f).coerceIn(0f, 1f)
                    cardText.setText(AppState.history)
                    cardText.draw(0f, -0.55f, 0.30f, eyeAspect, fade * 0.95f)
                }
                if (AppState.menuOpen) {
                    val items = listOf(
                        "RESUME",
                        "RESTART GAME",
                        "SKILL: ${AppState.SKILL_NAMES[AppState.skill]}",
                        "MUSIC VOL: ${(AppState.musicVol * 100).toInt()}%",
                        "SFX VOL: ${(AppState.sfxVol * 100).toInt()}%",
                        "GHOST PIECE: ${if (AppState.ghostOn) "ON" else "OFF"}",
                        "SWAP PAD AXES: ${if (AppState.swapAxes) "ON" else "OFF"}",
                        "INVERT MOVE: ${if (AppState.invertMove) "ON" else "OFF"}")
                    val b = StringBuilder("— SETTINGS —\nSWIPE UP/DOWN · TAP SELECT · DOUBLE EXIT")
                    items.forEachIndexed { i, s ->
                        b.append('\n').append(if (i == AppState.menuIndex) "▶ $s" else "· $s")
                    }
                    menuText.setText(b.toString())
                    menuText.draw(0f, 0f, 0.42f, eyeAspect, 0.97f)
                } else if (AppState.paused) {
                    msgText.setText("PAUSED — TAP TO RESUME")
                    msgText.draw(0f, 0f, 0.12f, eyeAspect, 0.9f)
                }
            }
        }
    }
}
