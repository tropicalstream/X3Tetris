#!/usr/bin/env python3
"""
TetraLlama 3D sound effects — synthesized in the spirit of Tempest 2000:
short, punchy, slightly too enthusiastic. numpy -> WAV -> ffmpeg -> Ogg.

    python3 app/tools/generate_sfx.py
"""
import pathlib, subprocess, tempfile, wave
import numpy as np

APP = pathlib.Path(__file__).resolve().parent.parent
SFX = APP / "src/main/assets/sfx"
SR = 44100
rng = np.random.default_rng(2000)

def t(d): return np.arange(int(SR * d)) / SR
def env(sig, a=0.004, r=0.08):
    n = len(sig); e = np.ones(n)
    na = max(1, min(int(SR * a), n // 2))
    nr = max(1, min(int(SR * r), n - na))
    e[:na] = np.linspace(0, 1, na); e[-nr:] = np.linspace(1, 0, nr)
    return sig * e
def norm(s, peak=0.8):
    m = np.max(np.abs(s)) or 1.0
    return s / m * peak
def sweep(f0, f1, d):
    tt = t(d); f = f0 * (f1 / f0) ** (tt / d)
    return np.sin(2 * np.pi * np.cumsum(f) / SR)
def save(name, sig):
    SFX.mkdir(parents=True, exist_ok=True)
    pcm = (np.clip(sig, -1, 1) * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
        wav = f.name
    with wave.open(wav, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    out = SFX / f"{name}.ogg"
    subprocess.check_call(["ffmpeg", "-y", "-loglevel", "error", "-i", wav,
                           "-c:a", "libvorbis", "-q:a", "3", str(out)])
    pathlib.Path(wav).unlink()
    print(f"  + {out.name} ({out.stat().st_size // 1024} KB)")

print("Zapping up TetraLlama SFX …")
# move: tiny zap blip
save("move", norm(env(sweep(900, 1400, 0.05))))
# rotate: upward chirp
save("rotate", norm(env(sweep(500, 1800, 0.08))))
# tick: soft drop step
save("tick", norm(env(sweep(300, 220, 0.04)), 0.5))
# bump: rejected move (dull thok)
save("bump", norm(env(sweep(180, 110, 0.07)), 0.55))
# harddrop: whoosh + slam
d = 0.30; tt = t(d)
sig = sweep(1800, 120, d) * 0.6
slam = np.zeros(len(tt))
i0 = int(SR * 0.16); seg = t(d)[:len(tt) - i0]
slam[i0:] = np.sin(2 * np.pi * 70 * seg) * np.exp(-seg * 24) * 1.2
save("harddrop", norm(env(sig + slam, 0.002, 0.06)))
# lock: firm click-thud
d = 0.12; tt = t(d)
save("lock", norm(env(np.sin(2*np.pi*140*tt)*np.exp(-tt*30) + 0.3*rng.standard_normal(len(tt))*np.exp(-tt*60))))
# hold: swap shimmer
save("hold", norm(env(sweep(700, 1200, 0.1) + sweep(1200, 700, 0.1) * 0.6, 0.004, 0.1)))
# clears: rising zap chords, one per line count
def chord(fs, d=0.35):
    tt = t(d)
    return sum(np.sin(2*np.pi*f*tt*(1+0.15*tt)) for f in fs) / len(fs)
save("clear1", norm(env(chord([523, 659]), 0.004, 0.2)))
save("clear2", norm(env(chord([523, 659, 784]), 0.004, 0.22)))
save("clear3", norm(env(chord([587, 740, 880]), 0.004, 0.25)))
# tetris: THE BANG — noise burst + major chord + sub drop
d = 0.9; tt = t(d)
bang = rng.standard_normal(len(tt)) * np.exp(-tt*10)
chordy = chord([523, 659, 784, 1046], d) * np.exp(-tt*2.2)
sub = np.sin(2*np.pi*55*tt) * np.exp(-tt*4)
save("tetris", norm(env(bang*0.7 + chordy*1.2 + sub*1.1, 0.002, 0.3), 0.9))
# tspin: twisty gliss
d=0.5; tt=t(d)
save("tspin", norm(env(np.sin(2*np.pi*(600+300*np.sin(tt*30))*tt)*np.exp(-tt*3), 0.004, 0.2)))
# combo: rising ping (pitch scales in code via rate)
save("combo", norm(env(sweep(800, 1600, 0.16), 0.003, 0.1)))
# levelup: little fanfare
d=0.8; tt=t(d); sig=np.zeros(len(tt))
for k,f in [(0.0,523),(0.12,659),(0.24,784),(0.36,1046)]:
    i=int(k*SR); seg=t(0.3)
    sig[i:i+len(seg)] += np.sin(2*np.pi*f*seg)*np.exp(-seg*6)*0.6
save("levelup", norm(sig, 0.8))
# gameover: sad power-down
save("gameover", norm(env(sweep(880, 55, 1.4) + 0.4*sweep(440, 28, 1.4), 0.01, 0.5)))
# yak: the llama's electric bleat (Tetris celebration)
d=0.55; tt=t(d)
bleat = np.sin(2*np.pi*(300+120*np.sign(np.sin(tt*90)))*tt) * (0.6+0.4*np.sin(tt*55))
save("yak", norm(env(bleat, 0.01, 0.15), 0.75))
# menu: soft tap
save("menu", norm(env(sweep(1000, 900, 0.05)), 0.5))
# chroma: bubbly color-pop arpeggio (pitch rises with chain via play rate)
d = 0.32; tt = t(d); sig = np.zeros(len(tt))
for k, f in [(0.0, 740), (0.07, 932), (0.14, 1244)]:
    i = int(k * SR); seg = t(0.16)
    sig[i:i + len(seg)] += np.sin(2*np.pi*f*seg*(1+0.3*seg)) * np.exp(-seg*14) * 0.6
save("chroma", norm(env(sig, 0.003, 0.1), 0.7))
print("Done — the well is loud.")
