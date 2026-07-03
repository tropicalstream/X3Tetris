package com.x3tetris

import kotlin.random.Random

/**
 * Guideline-faithful Tetris core — a living museum of how the game improved:
 *   · 7-bag randomizer (standardized 2001) — no droughts, no floods
 *   · SRS rotation with full wall/floor kicks (2001)
 *   · HOLD piece (1998 TGM lineage → guideline)
 *   · Ghost piece (Tetris DX 1998 →)
 *   · Lock delay with move-reset cap (TGM 1998)
 *   · T-spin detection + Back-to-Back bonus (2004+)
 *   · Combos (2006+), guideline scoring, 3-piece preview
 * Every level-up shows the next chapter of that history.
 */
class GameState {
    companion object {
        const val W = 10
        const val H = 22           // rows 20..21 hidden (vanish zone)
        const val VISIBLE_H = 20
        const val LOCK_DELAY_MS = 500L
        const val MAX_LOCK_RESETS = 15

        // piece order: I O T S Z J L  (SRS cell tables, y-up, box origin bottom-left)
        val SHAPES: Array<Array<Array<IntArray>>> = arrayOf(
            arrayOf( // I (4x4 box)
                arrayOf(intArrayOf(0,2), intArrayOf(1,2), intArrayOf(2,2), intArrayOf(3,2)),
                arrayOf(intArrayOf(2,3), intArrayOf(2,2), intArrayOf(2,1), intArrayOf(2,0)),
                arrayOf(intArrayOf(0,1), intArrayOf(1,1), intArrayOf(2,1), intArrayOf(3,1)),
                arrayOf(intArrayOf(1,3), intArrayOf(1,2), intArrayOf(1,1), intArrayOf(1,0))),
            arrayOf( // O
                arrayOf(intArrayOf(1,1), intArrayOf(2,1), intArrayOf(1,2), intArrayOf(2,2)),
                arrayOf(intArrayOf(1,1), intArrayOf(2,1), intArrayOf(1,2), intArrayOf(2,2)),
                arrayOf(intArrayOf(1,1), intArrayOf(2,1), intArrayOf(1,2), intArrayOf(2,2)),
                arrayOf(intArrayOf(1,1), intArrayOf(2,1), intArrayOf(1,2), intArrayOf(2,2))),
            arrayOf( // T
                arrayOf(intArrayOf(0,1), intArrayOf(1,1), intArrayOf(2,1), intArrayOf(1,2)),
                arrayOf(intArrayOf(1,0), intArrayOf(1,1), intArrayOf(1,2), intArrayOf(2,1)),
                arrayOf(intArrayOf(0,1), intArrayOf(1,1), intArrayOf(2,1), intArrayOf(1,0)),
                arrayOf(intArrayOf(1,0), intArrayOf(1,1), intArrayOf(1,2), intArrayOf(0,1))),
            arrayOf( // S
                arrayOf(intArrayOf(0,1), intArrayOf(1,1), intArrayOf(1,2), intArrayOf(2,2)),
                arrayOf(intArrayOf(1,2), intArrayOf(1,1), intArrayOf(2,1), intArrayOf(2,0)),
                arrayOf(intArrayOf(0,0), intArrayOf(1,0), intArrayOf(1,1), intArrayOf(2,1)),
                arrayOf(intArrayOf(0,2), intArrayOf(0,1), intArrayOf(1,1), intArrayOf(1,0))),
            arrayOf( // Z
                arrayOf(intArrayOf(0,2), intArrayOf(1,2), intArrayOf(1,1), intArrayOf(2,1)),
                arrayOf(intArrayOf(2,2), intArrayOf(2,1), intArrayOf(1,1), intArrayOf(1,0)),
                arrayOf(intArrayOf(0,1), intArrayOf(1,1), intArrayOf(1,0), intArrayOf(2,0)),
                arrayOf(intArrayOf(1,2), intArrayOf(1,1), intArrayOf(0,1), intArrayOf(0,0))),
            arrayOf( // J
                arrayOf(intArrayOf(0,2), intArrayOf(0,1), intArrayOf(1,1), intArrayOf(2,1)),
                arrayOf(intArrayOf(1,2), intArrayOf(2,2), intArrayOf(1,1), intArrayOf(1,0)),
                arrayOf(intArrayOf(0,1), intArrayOf(1,1), intArrayOf(2,1), intArrayOf(2,0)),
                arrayOf(intArrayOf(1,2), intArrayOf(1,1), intArrayOf(0,0), intArrayOf(1,0))),
            arrayOf( // L
                arrayOf(intArrayOf(2,2), intArrayOf(0,1), intArrayOf(1,1), intArrayOf(2,1)),
                arrayOf(intArrayOf(1,2), intArrayOf(1,1), intArrayOf(1,0), intArrayOf(2,0)),
                arrayOf(intArrayOf(0,1), intArrayOf(1,1), intArrayOf(2,1), intArrayOf(0,0)),
                arrayOf(intArrayOf(0,2), intArrayOf(1,2), intArrayOf(1,1), intArrayOf(1,0))))

        // SRS wall kicks: [from*4+to] -> 5 tests of (dx, dy), y-up
        private fun k(vararg p: Int) = Array(p.size / 2) { intArrayOf(p[it * 2], p[it * 2 + 1]) }
        val KICKS_JLSTZ = mapOf(
            (0 to 1) to k(0,0, -1,0, -1,1, 0,-2, -1,-2),
            (1 to 0) to k(0,0, 1,0, 1,-1, 0,2, 1,2),
            (1 to 2) to k(0,0, 1,0, 1,-1, 0,2, 1,2),
            (2 to 1) to k(0,0, -1,0, -1,1, 0,-2, -1,-2),
            (2 to 3) to k(0,0, 1,0, 1,1, 0,-2, 1,-2),
            (3 to 2) to k(0,0, -1,0, -1,-1, 0,2, -1,2),
            (3 to 0) to k(0,0, -1,0, -1,-1, 0,2, -1,2),
            (0 to 3) to k(0,0, 1,0, 1,1, 0,-2, 1,-2))
        val KICKS_I = mapOf(
            (0 to 1) to k(0,0, -2,0, 1,0, -2,-1, 1,2),
            (1 to 0) to k(0,0, 2,0, -1,0, 2,1, -1,-2),
            (1 to 2) to k(0,0, -1,0, 2,0, -1,2, 2,-1),
            (2 to 1) to k(0,0, 1,0, -2,0, 1,-2, -2,1),
            (2 to 3) to k(0,0, 2,0, -1,0, 2,1, -1,-2),
            (3 to 2) to k(0,0, -2,0, 1,0, -2,-1, 1,2),
            (3 to 0) to k(0,0, 1,0, -2,0, 1,-2, -2,1),
            (0 to 3) to k(0,0, -1,0, 2,0, -1,2, 2,-1))

        /** Each level-up turns a page of Tetris history. */
        val HISTORY = arrayOf(
            "1984 · MOSCOW\nAlexey Pajitnov builds Tetris on an Electronika-60.\nNo graphics chip — the blocks are bracket characters.",
            "1985 · FIRST COLOR\nVadim Gerasimov ports it to the IBM PC.\nColor arrives. Productivity worldwide never recovers.",
            "1989 · GAME BOY\nBundled with Nintendo's brick, 35 million carts.\nKorobeiniki becomes the most hummed code in history.",
            "1989 · NES GRAVITY\nConsole speed curves are born — and with them,\nthe decades-long hunt for the kill screen.",
            "1998 · TETRIS DX\nThe GHOST PIECE appears — look down:\nthat faint outline is 1998 helping you aim.",
            "1998 · TGM (ARCADE)\nLOCK DELAY invented: the piece rests half a beat\nbefore freezing. You are using it right now.",
            "2001 · THE GUIDELINE\nSRS rotation + wall kicks standardized.\nEvery spin you just kicked off a wall — that's this.",
            "2001 · 7-BAG RANDOMIZER\nAll seven pieces, shuffled, dealt fairly.\nThe I-piece drought officially goes extinct.",
            "2002 · HOLD PIECE\nSave one for later (long-press!). Strategy\nreplaces panic. Purists grumble; everyone uses it.",
            "2004 · T-SPINS\nTwist a T into an impossible slot for bonus points.\nThe move that turned stacking into judo.",
            "2006 · BACK-TO-BACK\nChain Tetrises and T-spins for multiplied glory.\nDifficult clears finally pay rent.",
            "2007 · COMBOS\nConsecutive clears stack bonuses.\nThe well becomes a drum solo.",
            "1994 · MEANWHILE...\nJeff Minter's TEMPEST 2000 proves vectors + particles\n+ pure absurdity = enlightenment. This game's entire soul.",
            "2018 · TETRIS EFFECT\nSynesthesia: the game becomes music.\n(Your MP3s per level — same idea. Drop them in.)",
            "TODAY · YOU\nAll of it — bag, kicks, hold, ghost, B2B —\nfalling through one neon well on your face.\nThe panda is proud. Onward.")

        val TETRIS_SHOUTS = arrayOf("SUPERZAPPER!!", "PANDA SLAM!", "ABSOLUTELY BAMBOO",
            "EAT ELECTRIC DEATH", "FOUR-LINE FRENZY", "BEAR-ILLIANT!!", "GROOVY.")
        val TSPIN_SHOUTS = arrayOf("SPIN DOCTOR", "TWISTY BEAST", "T FOR TREMENDOUS", "MIND THE KICK")
        val B2B_SHOUTS = arrayOf("BACK-2-BACK BLISS", "CHAIN OF GLORY", "THE PANDA NODS")
        val LEVEL_SHOUTS = arrayOf("WARP LEVEL", "FASTER, FLUFFIER", "THE WELL HUNGERS")
    }

    class Event(val type: String, val a: Int = 0, val text: String = "",
                val cells: IntArray = IntArray(0))

    // ---- state ----
    val board = IntArray(W * H)                 // 0 empty, else pieceIdx+1
    val events = ArrayDeque<Event>()
    var running = true; private set
    var gameOver = false; private set
    var pieceType = 0; private set
    var rot = 0; private set
    var px = 3; private set
    var py = 18; private set
    var holdType = -1; private set
    var pieceColor = 0; private set
    var holdColor = -1; private set
    val nextColors = ArrayDeque<Int>()
    private var holdUsed = false
    private val bag = ArrayDeque<Int>()
    val nextQueue = ArrayDeque<Int>()
    var score = 0L; private set
    var lines = 0; private set
    var level = 1; private set
    var combo = -1; private set
    private var b2b = false
    private var gravityAcc = 0f
    private var lockTimerMs = -1L
    private var lockResets = 0
    private var lastActionRotate = false
    var clearingRows: List<Int> = emptyList(); private set
    var clearAnimMs = 0L; private set

    // LEVEL GOAL METER — starts full, DRAINS as you progress; empty = level clear.
    //  Apprentice: chroma points (pops + lines)   Journeyman: line credits (+2 per 6+ run)
    //  Adept: 10-line sprint (Tetris/T-spin count double)   Wizard: 10 strict rows
    var goalTarget = 10f; private set
    var goalProgress = 0f; private set
    val goalFrac: Float get() = if (goalTarget <= 0f) 1f else (goalProgress / goalTarget).coerceIn(0f, 1f)
    private var lastClearTspin = false
    private val rnd = Random(System.nanoTime())
    private val shoutBags = HashMap<String, MutableList<String>>()
    private val colorOrder = intArrayOf(0, 4, 1, 2, 3, 5, 6)  // cyan red yellow purple green blue orange

    init { refillBag(); repeat(3) { val s = drawBag(); nextQueue.add(s); nextColors.add(rollColor(s)) }; initGoal(); spawn() }

    // ---- helpers ----
    private fun refillBag() { bag.addAll((0..6).shuffled(rnd)) }
    private fun drawBag(): Int { if (bag.isEmpty()) refillBag(); return bag.removeFirst() }
    private fun cells(type: Int, r: Int) = SHAPES[type][r]
    private fun at(x: Int, y: Int) = if (x < 0 || x >= W || y < 0) 1 else if (y >= H) 0 else board[y * W + x]

    private fun collides(type: Int, r: Int, ox: Int, oy: Int): Boolean {
        for (c in cells(type, r)) if (at(ox + c[0], oy + c[1]) != 0) return true
        return false
    }

    val ghostY: Int
        get() {
            var y = py
            while (!collides(pieceType, rot, px, y - 1)) y--
            return y
        }

    private fun grounded() = collides(pieceType, rot, px, py - 1)

    /**
     * Guideline gravity curve with a per-tier RAMP: Apprentice speeds up
     * gently (levels count half), Adept a little faster with each level
     * (1.1x), Wizard steep (1.3x). The multiplier sets each tier's floor.
     */
    private fun gravitySec(): Float {
        val ramp = when (AppState.skill) { 0 -> 0.5f; 1 -> 0.75f; 2 -> 1.1f; else -> 1.3f }
        val eff = (1f + (level - 1) * ramp).coerceAtMost(20f)
        val base = (0.8f - (eff - 1f) * 0.007f).coerceAtLeast(0.05f)
        val t = Math.pow(base.toDouble(), (eff - 1f).toDouble())
        val mult = when (AppState.skill) { 0 -> 0.55f; 1 -> 0.8f; 2 -> 1f; else -> 1.35f }
        return (t.toFloat() / mult).coerceAtLeast(0.016f)
    }

    private fun lockDelayMs(): Long =
        when (AppState.skill) { 0 -> 700L; 1 -> 600L; 2 -> 500L; else -> 400L }

    /**
     * THE CHROMA RULE (skill-driven): straight horizontal/vertical same-color
     * runs of at least this many blocks pop on their own. Wizard tier disables
     * it entirely: rows or nothing.
     */
    fun chromaThreshold(): Int = when (AppState.skill) {
        0 -> 8
        1 -> 10
        2 -> 12
        else -> Int.MAX_VALUE                            // wizard: earn your clears
    }

    /**
     * PALETTE SIZE BY SKILL: Apprentice starts with only 3 block colors
     * (big friendly same-color groups), growing to 5 across levels;
     * Journeyman 4->6; Adept 4->7; Wizard plays classic shape colors, all 7.
     */
    fun colorCount(): Int = when (AppState.skill) {
        0 -> (3 + (level - 1) / 3).coerceAtMost(5)
        1 -> (4 + (level - 1) / 3).coerceAtMost(6)
        2 -> (4 + (level - 1) / 2).coerceAtMost(7)
        else -> 7
    }

    private fun rollColor(shape: Int): Int =
        if (AppState.skill == 3) shape else colorOrder[rnd.nextInt(colorCount())]

    private fun shout(pool: Array<String>, key: String): String {
        val bagList = shoutBags.getOrPut(key) { mutableListOf() }
        if (bagList.isEmpty()) bagList.addAll(pool.toList().shuffled(rnd))
        return bagList.removeAt(bagList.size - 1)
    }

    // ---- flow ----
    private fun spawn() {
        pieceType = nextQueue.removeFirst()
        pieceColor = nextColors.removeFirst()
        val s = drawBag()
        nextQueue.add(s); nextColors.add(rollColor(s))
        rot = 0
        px = if (pieceType == 0) 3 else 3
        py = 18
        gravityAcc = 0f; lockTimerMs = -1; lockResets = 0
        holdUsed = false; lastActionRotate = false
        if (collides(pieceType, rot, px, py)) {        // block out
            gameOver = true; running = false
            events.add(Event("gameover"))
        } else events.add(Event("spawn"))
    }

    fun update(dtMs: Long) {
        if (!running) {
            if (clearingRows.isNotEmpty()) {
                clearAnimMs -= dtMs
                if (clearAnimMs <= 0) finishClear()
            }
            return
        }
        if (grounded()) {
            if (lockTimerMs < 0) lockTimerMs = lockDelayMs()
            lockTimerMs -= dtMs
            if (lockTimerMs <= 0) lock()
        } else {
            lockTimerMs = -1
            gravityAcc += dtMs / 1000f
            val g = gravitySec()
            while (gravityAcc >= g && !grounded()) {
                gravityAcc -= g
                py--
                lastActionRotate = false
            }
        }
    }

    private fun resetLock() {
        if (lockTimerMs >= 0 && lockResets < MAX_LOCK_RESETS) {
            lockTimerMs = lockDelayMs(); lockResets++
        }
    }

    // ---- inputs (return true when they took effect — drives SFX) ----
    fun moveX(dir: Int): Boolean {
        if (!running) return false
        if (collides(pieceType, rot, px + dir, py)) return false
        px += dir; lastActionRotate = false; resetLock()
        events.add(Event("move")); return true
    }

    fun rotateCW(): Boolean {
        if (!running || pieceType == 1) { if (running) events.add(Event("move")); return running }
        val to = (rot + 1) % 4
        val kicks = (if (pieceType == 0) KICKS_I else KICKS_JLSTZ)[rot to to] ?: return false
        for (t in kicks) {
            if (!collides(pieceType, to, px + t[0], py + t[1])) {
                px += t[0]; py += t[1]; rot = to
                lastActionRotate = true; resetLock()
                events.add(Event("rotate")); return true
            }
        }
        return false
    }

    fun softDrop(): Boolean {
        if (!running) return false
        if (collides(pieceType, rot, px, py - 1)) { lock(); return true }
        py--; score += 1; lastActionRotate = false
        events.add(Event("softdrop")); return true
    }

    fun hardDrop(): Boolean {                          // TAP = place the piece
        if (!running) return false
        val dist = py - ghostY
        py = ghostY
        score += dist * 2L
        events.add(Event("harddrop", dist))
        lock()
        return true
    }

    fun holdPiece(): Boolean {
        if (!running || holdUsed) return false
        holdUsed = true
        val h = holdType; val hc = holdColor
        holdType = pieceType; holdColor = pieceColor
        events.add(Event("hold"))
        if (h < 0) spawn() else {
            pieceType = h; pieceColor = hc; rot = 0; px = 3; py = 18
            gravityAcc = 0f; lockTimerMs = -1; lockResets = 0; lastActionRotate = false
            if (collides(pieceType, rot, px, py)) { gameOver = true; running = false; events.add(Event("gameover")) }
        }
        return true
    }

    // ---- lock & clear ----
    private fun lock() {
        val tSpin = pieceType == 2 && lastActionRotate && tCorners() >= 3
        for (c in cells(pieceType, rot)) {
            val x = px + c[0]; val y = py + c[1]
            if (y in 0 until H) board[y * W + x] = pieceColor + 1
        }
        events.add(Event("lock"))
        val full = (0 until H).filter { r -> (0 until W).all { board[r * W + it] != 0 } }
        if (full.isEmpty()) {
            if (tSpin) { score += 400L * level; events.add(Event("message", 0, shout(TSPIN_SHOUTS, "ts"))) }
            combo = -1
            applyChroma()                            // skill rule: color groups may pop
            checkGoal()
            if (!gameOver) spawn()
            return
        }
        // scoring (guideline)
        val n = full.size
        combo++
        val base = if (tSpin) intArrayOf(0, 800, 1200, 1600)[n.coerceAtMost(3)]
                   else intArrayOf(0, 100, 300, 500, 800)[n]
        val difficult = tSpin || n == 4
        var pts = base.toLong() * level
        if (difficult && b2b) { pts = pts * 3 / 2; events.add(Event("message", 0, shout(B2B_SHOUTS, "b2b"))) }
        if (combo > 0) pts += 50L * combo * level
        b2b = difficult
        if (AppState.skill == 3) pts = pts * 3 / 2   // Wizard: no chroma help, more glory
        score += pts
        if (n == 4) events.add(Event("tetris", 0, shout(TETRIS_SHOUTS, "tet")))
        else if (tSpin) events.add(Event("tspinclear", n, shout(TSPIN_SHOUTS, "ts")))
        if (combo > 1) events.add(Event("combo", combo))
        events.add(Event("clear", n))
        lastClearTspin = tSpin
        clearingRows = full
        clearAnimMs = 380L
        running = false                                  // hold the world for the bang
    }

    private fun finishClear() {
        val keep = (0 until H).filter { it !in clearingRows }
        val newBoard = IntArray(W * H)
        var dst = 0
        for (r in keep) {
            System.arraycopy(board, r * W, newBoard, dst * W, W)
            dst++
        }
        System.arraycopy(newBoard, 0, board, 0, board.size)
        val n = clearingRows.size
        clearingRows = emptyList()
        lines += n
        goalProgress += when (AppState.skill) {
            0 -> n * 60f                                        // chroma points per line
            2 -> if (n == 4 || lastClearTspin) n * 2f else n.toFloat()  // sprint: hard clears double
            else -> n.toFloat()                                 // line credits
        }
        running = true
        applyChroma()                                // settled stack may chain color pops
        checkGoal()
        spawn()
    }

    // ---------------- level goal (the draining meter) ----------------

    private fun initGoal() {
        goalProgress = 0f
        goalTarget = if (AppState.skill == 0) 240f + (level - 1) * 60f else 10f
    }

    private fun checkGoal() {
        if (gameOver || goalProgress < goalTarget) return
        // LEVEL CLEAR: Apprentice/Journeyman/Adept get the firework send-off;
        // Wizard transitions instantly — the drop never breaks stride.
        if (AppState.skill != 3) fireworks()
        level++
        initGoal()
        val fact = HISTORY[(level - 2).coerceIn(0, HISTORY.size - 1)]
        if (AppState.skill != 3) events.add(Event("fireworks", level))
        events.add(Event("levelup", level, fact))
        if (AppState.skill == 3) events.add(Event("message", 0, shout(LEVEL_SHOUTS, "lvl")))
    }

    /** Level-clear fireworks: every leftover 3+ run chain-pops for bonus points. */
    private fun fireworks() {
        var chain = 0
        while (chain < 6) {
            val doomed = collectDoomed(3)
            if (doomed.isEmpty()) break
            chain++
            val cells = IntArray(doomed.size * 3)
            doomed.forEachIndexed { i, idx ->
                cells[i * 3] = idx % W; cells[i * 3 + 1] = idx / W; cells[i * 3 + 2] = board[idx]
                board[idx] = 0
            }
            score += 5L * doomed.size * level * chain
            events.add(Event("chroma", chain, "", cells))
            columnGravity()
        }
    }

    // ---------------- chroma (skill) mechanic ----------------

    /**
     * Finds straight same-color horizontal/vertical runs; any run of at least
     * chromaThreshold() pops, columns compact downward, and cascades chain.
     * Scoring:
     * 20 × blocks × level × chain. Chroma pops do NOT advance the line
     * counter — leveling stays honest row-clearing, as tradition demands.
     */
    private var lastSweepBigRuns = 0

    /** Marks straight horizontal/vertical same-color runs >= [thr]; counts 6+ runs. */
    private fun collectDoomed(thr: Int): List<Int> {
        val marked = BooleanArray(W * H)
        lastSweepBigRuns = 0
        for (r in 0 until H) {
            var c = 0
            while (c < W) {
                val start = c
                val color = board[r * W + c]
                if (color == 0) { c++; continue }
                while (c < W && board[r * W + c] == color) c++
                if (c - start >= thr) {
                    for (x in start until c) marked[r * W + x] = true
                    if (c - start >= 6) lastSweepBigRuns++
                }
            }
        }
        for (c in 0 until W) {
            var r = 0
            while (r < H) {
                val start = r
                val color = board[r * W + c]
                if (color == 0) { r++; continue }
                while (r < H && board[r * W + c] == color) r++
                if (r - start >= thr) {
                    for (y in start until r) marked[y * W + c] = true
                    if (r - start >= 6) lastSweepBigRuns++
                }
            }
        }
        val doomed = ArrayList<Int>()
        for (idx in marked.indices) if (marked[idx]) doomed.add(idx)
        return doomed
    }

    private fun columnGravity() {
        for (c in 0 until W) {
            var write = 0
            for (r in 0 until H) {
                val v = board[r * W + c]
                if (v != 0) {
                    board[r * W + c] = 0
                    board[write * W + c] = v
                    write++
                }
            }
        }
    }

    private fun applyChroma() {
        val thr = chromaThreshold()
        if (thr > W * H) return
        var chain = 0
        while (chain < 8) {
            val doomed = collectDoomed(thr)
            if (doomed.isEmpty()) break
            chain++
            val cells = IntArray(doomed.size * 3)
            doomed.forEachIndexed { i, idx ->
                cells[i * 3] = idx % W; cells[i * 3 + 1] = idx / W; cells[i * 3 + 2] = board[idx]
                board[idx] = 0
            }
            score += 20L * doomed.size * level * chain
            // goal credit: Apprentice feeds the meter with every pop; Journeyman
            // earns +2 line credits per 6+ run; Adept/Wizard pops are survival only
            when (AppState.skill) {
                0 -> goalProgress += doomed.size * 10f * chain
                1 -> goalProgress += lastSweepBigRuns * 2f
            }
            events.add(Event("chroma", chain, "", cells))
            columnGravity()
        }
    }

    fun restart() {
        board.fill(0)
        score = 0; lines = 0; level = 1; combo = -1; b2b = false
        initGoal()
        holdType = -1; gameOver = false; running = true
        clearingRows = emptyList()
        bag.clear(); nextQueue.clear(); nextColors.clear()
        holdColor = -1
        refillBag(); repeat(3) { val s = drawBag(); nextQueue.add(s); nextColors.add(rollColor(s)) }
        spawn()
        events.add(Event("restart"))
    }
}
