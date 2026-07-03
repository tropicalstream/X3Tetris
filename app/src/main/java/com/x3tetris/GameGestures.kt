package com.x3tetris

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import kotlin.math.abs

/**
 * Right temple-pad game controls (cyttsp5 = right arm on the X3 Pro; screen
 * touches mirror everything for flat testing):
 *
 *   swipe forward / back ... move piece right / left
 *   swipe up ............... rotate (SRS, with kicks)
 *   swipe down ............. soft drop
 *   TAP .................... PLACE PIECE (hard drop)
 *   double tap ............. settings menu
 *   long press (600 ms) .... HOLD piece
 *
 * Drag-repeat: keep the finger moving and the piece keeps stepping every
 * REPEAT_PX — feels like DAS. SWAP AXES / INVERT in settings if your pad
 * reports differently.
 */
class GameGestures(
    private val onSwipeH: (dir: Int) -> Unit,     // -1 left, +1 right
    private val onSwipeUp: () -> Unit,
    private val onSwipeDown: () -> Unit,
    private val onTap: () -> Unit,
    private val onDoubleTap: () -> Unit,
    private val onLongPress: () -> Unit
) {
    companion object {
        private const val TAP_MS = 220L
        private const val DOUBLE_MS = 320L
        private const val LONG_MS = 600L
        private const val SLOP_PX = 30f
        private const val REPEAT_PX = 70f
    }

    private val handler = Handler(Looper.getMainLooper())
    private var downT = 0L
    private var downX = 0f; private var downY = 0f
    private var emittedH = 0                       // horizontal steps already emitted
    private var swipedV = false
    private var moved = false
    private var longFired = false
    private var lastTapT = 0L
    private var pendingTap: Runnable? = null
    private var longRunnable: Runnable? = null

    fun onTouchEvent(e: MotionEvent): Boolean {
        // pad axes: x runs along the temple (forward/back), y across it
        var gx = e.x; var gy = e.y
        if (AppState.swapAxes) { val t = gx; gx = gy; gy = t }
        val now = SystemClock.uptimeMillis()
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downT = now; downX = gx; downY = gy
                emittedH = 0; swipedV = false; moved = false; longFired = false
                longRunnable = Runnable {
                    if (!moved && !longFired) { longFired = true; onLongPress() }
                }.also { handler.postDelayed(it, LONG_MS) }
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = gx - downX; val dy = gy - downY
                if (abs(dx) > SLOP_PX || abs(dy) > SLOP_PX) moved = true
                if (abs(dx) >= abs(dy)) {
                    // horizontal drag: step per REPEAT_PX (DAS feel)
                    val steps = (dx / REPEAT_PX).toInt()
                    while (emittedH < abs(steps)) {
                        emittedH++
                        var dir = if (steps > 0) 1 else -1
                        if (AppState.invertMove) dir = -dir
                        onSwipeH(dir)
                    }
                } else if (!swipedV && abs(dy) > SLOP_PX * 1.6f) {
                    swipedV = true
                    if (dy < 0) onSwipeUp() else onSwipeDown()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                longRunnable?.let { handler.removeCallbacks(it) }
                val held = now - downT
                if (longFired || moved || held > TAP_MS ||
                    e.actionMasked == MotionEvent.ACTION_CANCEL) return true
                val playLive = !AppState.menuOpen && !AppState.paused &&
                        AppState.phase == AppState.PHASE_PLAY
                if (playLive) {
                    // gameplay taps fire INSTANTLY (a delayed hard drop is unplayable);
                    // a second tap inside the window opens settings instead of dropping
                    if (now - lastTapT <= DOUBLE_MS) { lastTapT = 0; onDoubleTap() }
                    else { lastTapT = now; onTap() }
                } else if (now - lastTapT <= DOUBLE_MS) {
                    pendingTap?.let { handler.removeCallbacks(it) }
                    pendingTap = null; lastTapT = 0
                    onDoubleTap()
                } else {
                    lastTapT = now
                    val r = Runnable { pendingTap = null; onTap() }
                    pendingTap = r
                    handler.postDelayed(r, DOUBLE_MS)
                }
            }
        }
        return true
    }
}
