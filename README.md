# TETRA LLAMA 3D

A 3D Tetris for the **RayNeo X3 Pro** wearing the soul of **Tempest 2000**:
everything is glowing additive vector lines, the screen melts with video-
feedback trails, line clears detonate into particles, the palette never stops
hue-cycling, and — as Llamasoft law requires — a neon llama patrols the
background and **stampedes across your face when you score a Tetris**.

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
| Llamas, feedback trails, absurd shouts | Tempest 2000, 1994. Obviously. |

Scoring: 100/300/500/800 × level; T-spins 800/1200/1600; B2B ×1.5;
combos 50 × n × level; soft drop +1/cell; hard drop +2/cell.

## Skill tiers & the CHROMA rule

Settings → SKILL. Connected **same-color groups pop automatically** once they
reach a threshold (a nod to *Tetris 2*, 1993, when color-matching entered the
family). The threshold starts small and **rises by one every two levels**, so
every run drifts from playground to gauntlet. Pops cascade: floaters fall
straight down and can chain (×2, ×3… — 20 × blocks × level × chain points).
Chroma pops never advance the line counter — leveling stays honest rows.

| Tier | Color pop at L1 | Cap | Gravity | Lock delay |
| --- | --- | --- | --- | --- |
| **APPRENTICE** | 4+ connected (super easy) | 8 | ×0.55 | 700 ms |
| **JOURNEYMAN** | 5+ | 10 | ×0.8 | 600 ms |
| **ADEPT** | 7+ | 12 | ×1.0 | 500 ms |
| **WIZARD** | never — rows only | — | ×1.35 | 400 ms, ×1.5 score |

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

This game lives in its own directory and its own git repository, on branch
`neon-tetris` — independent of Project Pale Blue next door.
