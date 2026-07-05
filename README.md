# X3TETRIS

A 3D Tetris for the **RayNeo X3 Pro**:
everything is glowing additive vector lines, the screen melts with video-
feedback trails, line clears detonate into particles, the palette never stops
hue-cycling, and — as the house style demands — a neon panda ambles through a swaying
bamboo grove in the background and **charges across your face when you
score a Tetris**.


## The rules (and where they came from)

This is a **guideline-faithful** Tetris. Every "modern" mechanic in it is a
piece of the game's 40-year evolution, and the game tells you so — each
level-up shows the next chapter of Tetris history:

| Mechanic in this game | Where it came from |
| --- | --- |
| 10×20 well, 7 tetrominoes | Pajitnov, Electronika-60, Moscow 1984 |
| Guideline piece colors | IBM PC port 1985 → guideline |
| Level speed curve | NES/Game Boy era 1989 |
| **Ghost piece** | Tetris DX 1998 |
| **Lock delay** (500 ms, 15 move-resets) | Tetris: The Grand Master 1998 |
| **SRS rotation + wall kicks** | Tetris Guideline 2001 |
| **7-bag randomizer** | Guideline 2001 |
| **Hold piece** (long-press) | Guideline 2002 |
| **T-spins** (3-corner rule) + bonus | 2004 |
| **Back-to-Back ×1.5** | 2006 |
| **Combos** | 2007 |
| Music-reactive levels (your MP3s) | Tetris Effect 2018, in spirit |
| Feedback trails, particle bangs, absurd shouts | Tempest 2000, 1994. Obviously. |

Scoring: 100/300/500/800 × level; T-spins 800/1200/1600; B2B ×1.5;
combos 50 × n × level; soft drop +1/cell; hard drop +2/cell.

## The Goal Meter — how you finish a level

A neon meter stands beside the well. It starts **full** and **drains** as you
score toward the level goal — **when the meter is empty, the level is done.**
The HUD also spells it out ("METER: 6 LINE CREDITS TO GO"). Each tier fills
it differently:

| Tier | Goal | Inspired by |
| --- | --- | --- |
| **APPRENTICE** | *Chroma Points* — every popped block (10×chain) and line (60) feeds the meter; big color explosions melt levels fast | Tetris Worlds cascade, The New Tetris |
| **JOURNEYMAN** | *Line Credits* (10): a line = 1, any 6+ same-color pop = **+2** — tactical recovery pays | Tetris DS missions |
| **ADEPT** | *10-Line Sprint*: Tetrises & T-spin clears count **double**; chroma pops are your escape valve, not progress | Tetris Effect sprint, TGM |
| **WIZARD** | *10 strict rows*, nothing else counts | TGM, NES marathon |

Level clear in Apprentice/Journeyman/Adept: every leftover 3+ color run
**chain-pops in a firework display** for bonus points (5×blocks×level×chain)
before the next stage. Wizard transitions instantly — zero flair, unbroken drop.


