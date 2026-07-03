package com.x3tetris

/** Lock-free blackboard between input (main thread) and the GL/game thread. */
object AppState {
    const val PHASE_TITLE = 0
    const val PHASE_PLAY = 1
    const val PHASE_OVER = 2

    @Volatile var phase = PHASE_TITLE
    @Volatile var paused = false

    // settings menu: swipe up/down moves, tap selects, double-tap exits
    @Volatile var menuOpen = false
    @Volatile var menuIndex = 0

    // options
    @Volatile var ghostOn = true
    @Volatile var swapAxes = false
    @Volatile var invertMove = false
    @Volatile var musicVol = 0.8f
    @Volatile var sfxVol = 1.0f

    /**
     * SKILL: 0 Apprentice · 1 Journeyman · 2 Adept · 3 Wizard.
     * Governs the CHROMA rule (straight same-color rows/columns pop at a
     * skill threshold — Tetris 2, 1993, says hello),
     * plus gravity multiplier and lock delay. Wizard = pure classic rows, fast.
     */
    @Volatile var skill = 0
    val SKILL_NAMES = arrayOf("APPRENTICE", "JOURNEYMAN", "ADEPT", "WIZARD")

    // transient HUD
    @Volatile var message = ""
    @Volatile var messageUntil = 0L
    @Volatile var history = ""
    @Volatile var historyUntil = 0L
    @Volatile var flash = 0f              // screen-flash on big events (decays in renderer)

    // input actions from the touchpad, consumed on the game thread
    // 1 left, 2 right, 3 rotate, 4 softdrop, 5 harddrop, 6 hold, 7 tap-anywhere(title/over)
    val actions = java.util.concurrent.ConcurrentLinkedQueue<Int>()

    fun say(text: String, ms: Long = 2200) {
        message = text
        messageUntil = System.currentTimeMillis() + ms
    }

    fun card(text: String, ms: Long = 6000) {
        history = text
        historyUntil = System.currentTimeMillis() + ms
    }
}
