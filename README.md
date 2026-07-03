# X3TETRIS

A 3D Tetris for the **RayNeo X3 Pro** wearing the soul of **Tempest 2000**:
everything is glowing additive vector lines, the screen melts with video-
feedback trails, line clears detonate into particles, the palette never stops
hue-cycling, and — as the house style demands — a neon panda ambles through a swaying
bamboo grove in the background and **charges across your face when you
score a Tetris**.

Stereo side-by-side rendering (same pipeline as Project Pale Blue), RayNeo
Mercury registration, fully offline.

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

## The Announcer

A Fish-voiced arcade host (voice model `a387e2e593f74e899e45cf17a7c81dd7`)
calls the big moments — welcome, level clears, Tetrises, near-empty meter,
game over, and the occasional zen nudge if things go quiet. A hard 45-second
global cooldown keeps it engaging, never chatty. Render the clips once:

```bash
pip install requests
python3 app/tools/generate_voice.py     # key in gitignored app/tools/fish.config
```

Clips land in `assets/voice/`; the game is silent-but-happy without them.

## Skill tiers & the CHROMA rule

Settings -> SKILL. Straight horizontal or vertical **same-color runs pop
automatically** once they reach a threshold (a nod to *Tetris 2*, 1993, when
color-matching entered the family). Apprentice starts forgiving without letting
single tetrominoes erase themselves. Pops cascade: floaters fall straight down
and can chain (x2, x3... — 20 x blocks x level x chain points).
Chroma pops never advance the line counter — leveling stays honest rows.

| Tier | Color run pops at | Gravity | Lock delay |
| --- | --- | --- | --- |
| **APPRENTICE** | 8+ in a row/column | x0.55 | 700 ms |
| **JOURNEYMAN** | 10+ in a row/column | x0.8 | 600 ms |
| **ADEPT** | 12+ in a row/column | x1.0 | 500 ms |
| **WIZARD** | never — rows only | x1.35 | 400 ms, x1.5 score |

## Controls (right temple pad)

| Gesture | Action |
| --- | --- |
| swipe **forward / back** | move piece right / left (drag keeps stepping — DAS) |
| swipe **up** | rotate (SRS, kicks and all) |
| swipe **down** | soft drop |
| **TAP** | **place piece** (hard drop) — also starts/retries |
| **double tap** | settings menu (tap=next · double=select · hold=close) |
| **long press** | hold piece |

Settings: resume, restart, music/SFX volume, ghost on/off, swap pad axes,
invert move — the axis options cover pads that report rotated coordinates.

## Music

Drop your MP3s in `app/src/main/assets/music/` — sorted alphabetically,
one per level, last track loops for all later levels. See the README there.
SFX are already synthesized (`app/tools/generate_sfx.py` regenerates).

## Build

Android Studio (AGP 8.7.3 / Kotlin 2.0.21 / JDK 17) or
`gradle wrapper --gradle-version 8.9 && ./gradlew assembleDebug`.
Optional RayNeo AARs go in `app/libs/`. Screen touches mirror the pad,
so it runs on a phone for testing.

## Repo

X3Tetris lives in its own directory and its own git repository, on branch
`neon-tetris` — independent of Project Pale Blue next door.
