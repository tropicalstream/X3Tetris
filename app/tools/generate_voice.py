#!/usr/bin/env python3
"""
X3Tetris announcer — one-time Fish Audio render (run locally, needs network).

    pip install requests
    python3 app/tools/generate_voice.py           # render missing clips
    python3 app/tools/generate_voice.py --force   # re-render everything

Voice model: a387e2e593f74e899e45cf17a7c81dd7 (set in fish.config).
Model chain s2.1-pro -> s2.1-pro-free -> s2-pro; Ogg/Opus into assets/voice/.
Key lives in gitignored app/tools/fish.config (or env FISH_API_KEY).
"""
import argparse, json, os, pathlib, sys, time
import requests

HERE = pathlib.Path(__file__).resolve().parent
OUT = HERE.parent / "src/main/assets/voice"
CFG = {}
cfg_file = HERE / "fish.config"
if cfg_file.exists():
    CFG = dict(l.split("=", 1) for l in cfg_file.read_text().splitlines()
               if "=" in l and not l.strip().startswith("#"))
API_KEY = os.environ.get("FISH_API_KEY", CFG.get("API_KEY", ""))
VOICE_ID = CFG.get("VOICE_ID", "a387e2e593f74e899e45cf17a7c81dd7")
MODEL_CHAIN = [m for m in (CFG.get("MODEL", "s2.1-pro"), "s2.1-pro", "s2.1-pro-free", "s2-pro")]
MODEL_CHAIN = list(dict.fromkeys(MODEL_CHAIN))
ACTIVE = [MODEL_CHAIN[0]]

# The announcer: hyped arcade host with a light cosmic-zen streak.
LINES = {
    "welcome1": "[booming arcade announcer, huge grin] WELCOME TO X3 TETRIS! The well is open, the neon is warm — drop something beautiful!",
    "welcome2": "[warm hype, conspiratorial] The panda is watching. The bamboo is swaying. Show us what you've got, player.",
    "levelup1": "[thrilled roar] LEVEL UP! The well hungers for more!",
    "levelup2": "[cosmic calm over excitement] Faster now. Breathe with the music. You were built for this.",
    "levelup3": "[cheeky, impressed] Ohhh, you're getting GOOD at this.",
    "levelup4": "[announcer roar] NEXT STAGE! Keep that stack low and that meter draining!",
    "tetris1": "[explosive] TEEEE-TRIS! Four lines, one blow!",
    "tetris2": "[awed, savoring it] Beautiful. Textbook. Devastating.",
    "tetris3": "[gleeful shout] THE PANDA APPROVES!",
    "fireworks1": "[delighted roar] LEVEL CLEAR! Light it ALL up!",
    "fireworks2": "[grand, sweeping] And the board goes SUPERNOVA! Bonus on everything!",
    "fireworks3": "[gleeful] Fireworks for the champion! On to the next one!",
    "gameover1": "[gentle, respectful] The well wins this round. It always does, eventually. [beat] Tap to answer back.",
    "gameover2": "[warm] Good run, player. The panda remembers every line.",
    "gameover3": "[playful] Blocked out! Hey — it happens to wizards too. Again?",
    "idle1": "[soft encouragement] Steady hands. Watch the colors, not the clock.",
    "idle2": "[zen whisper] The next piece is already on its way. Make room for it.",
    "idle3": "[cheeky whisper] Psst — the hold slot is feeling lonely.",
    "idle4": "[calm hype] You're doing fine. Stack low. Dream big.",
    "almost1": "[rising excitement] Meter's almost empty — one good clear and this level FALLS!",
    "almost2": "[urgent joy] So close! Finish it! FINISH IT!",
    "chroma1": "[pop of delight] CHROMA CASCADE! Taste the rainbow physics!",
    "chroma2": "[amazed] Look at those colors GO!",
    "b2b1": "[impressed growl] Back... to... BACK! Now that's momentum!",
}

def synth(name, text, force):
    out = OUT / f"{name}.ogg"
    if out.exists() and not force:
        return "skip"
    body = {"text": text, "reference_id": VOICE_ID, "format": "opus", "opus_bitrate": 48000,
            "temperature": 0.9, "top_p": 0.85, "normalize": True,
            "prosody": {"speed": 1.02, "volume": 0, "normalize_loudness": True}}
    for attempt in range(3):
        model = ACTIVE[0]
        try:
            r = requests.post("https://api.fish.audio/v1/tts",
                headers={"Authorization": f"Bearer {API_KEY}",
                         "Content-Type": "application/json", "model": model},
                json=body, timeout=180)
        except requests.RequestException as e:
            print(f"  ! {name}: network {e}; retry"); time.sleep(2 ** attempt); continue
        if r.status_code == 200 and r.content[:4] == b"OggS":
            OUT.mkdir(parents=True, exist_ok=True)
            out.write_bytes(r.content)
            print(f"  + {name}.ogg [{model}]")
            return "ok"
        if r.status_code in (400, 402, 404) and MODEL_CHAIN.index(model) + 1 < len(MODEL_CHAIN):
            ACTIVE[0] = MODEL_CHAIN[MODEL_CHAIN.index(model) + 1]
            print(f"  ! {model} rejected -> {ACTIVE[0]}"); continue
        if r.status_code == 429:
            time.sleep(4 * (attempt + 1)); continue
        print(f"  ! {name}: {r.status_code} {r.text[:120]}")
        time.sleep(1)
    return "fail"

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--force", action="store_true")
    args = ap.parse_args()
    if not API_KEY:
        sys.exit("No API key: set FISH_API_KEY or put API_KEY=... in app/tools/fish.config")
    ok = skip = fail = 0
    for name, text in LINES.items():
        r = synth(name, text, args.force)
        ok += r == "ok"; skip += r == "skip"; fail += r == "fail"
    print(f"\n{ok} rendered, {skip} skipped, {fail} failed. "
          f"{'ANNOUNCER READY.' if fail == 0 else 'Re-run to retry failures.'}")

if __name__ == "__main__":
    main()
