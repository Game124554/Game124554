"""Synthesises every sound effect of the mod and writes sounds.json.
Needs numpy and ffmpeg (libvorbis).  Run:  python3 tools/gen_sounds.py
"""
import json
import os
import subprocess
import tempfile
import wave

import numpy as np

import content as C

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", C.MODID)
OUT = os.path.join(ASSETS, "sounds")
SR = 44100
rng = np.random.default_rng(1207)


def T(d):
    return np.arange(int(SR * d)) / SR


def noise(d):
    return rng.uniform(-1, 1, int(SR * d))


def env(n, a=0.01, r=0.05):
    e = np.ones(n)
    na, nr = max(1, int(SR * a)), max(1, int(SR * r))
    na, nr = min(na, n), min(nr, n)
    e[:na] *= np.linspace(0, 1, na)
    e[-nr:] *= np.linspace(1, 0, nr)
    return e


def decay(n, tau):
    return np.exp(-np.arange(n) / (SR * tau))


def filt(x, lo=None, hi=None, order=4):
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    g = np.ones_like(f)
    if hi is not None:
        g *= 1 / np.sqrt(1 + (f / hi) ** (2 * order))
    if lo is not None:
        g *= 1 - 1 / np.sqrt(1 + (f / lo) ** (2 * order))
    return np.fft.irfft(X * g, len(x))


def sweep(d, f0, f1, shape="sine", exp=False):
    t = T(d)
    if exp:
        fr = f0 * (f1 / f0) ** (t / d)
    else:
        fr = f0 + (f1 - f0) * (t / d)
    ph = 2 * np.pi * np.cumsum(fr) / SR
    if shape == "sine":
        return np.sin(ph)
    if shape == "square":
        return np.sign(np.sin(ph))
    return 2 * ((ph / (2 * np.pi)) % 1) - 1   # saw


def tone(d, f, shape="sine"):
    return sweep(d, f, f, shape)


def place(buf, x, at):
    i = int(SR * at)
    j = min(len(buf), i + len(x))
    buf[i:j] += x[: j - i]
    return buf


def voice(d, f0, f1, formants):
    """Very small formant voice: harmonics of a falling pitch weighted by formant peaks."""
    t = T(d)
    fr = f0 + (f1 - f0) * (t / d)
    ph = 2 * np.pi * np.cumsum(fr) / SR
    out = np.zeros_like(t)
    for k in range(1, 30):
        fk = k * (f0 + f1) / 2
        w = sum(np.exp(-((fk - F) / bw) ** 2) for F, bw in formants) + 0.02
        out += w * np.sin(k * ph) / k ** 0.3
    out += 0.08 * filt(noise(d), 1500, 6000)
    return out * env(len(t), 0.012, 0.05)


def norm(x, peak=0.85):
    m = np.max(np.abs(x))
    return x if m == 0 else x * (peak / m)


def write(name, x, peak=0.85):
    os.makedirs(OUT, exist_ok=True)
    x = norm(np.asarray(x, dtype=np.float64), peak)
    pcm = (x * 32767).astype("<i2")
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        path = tmp.name
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    dst = os.path.join(OUT, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", path, "-c:a", "libvorbis", "-q:a", "3",
                    "-ac", "1", "-ar", str(SR), dst], check=True)
    os.remove(path)


def main():
    # --- drones
    d = 0.9
    hum = (tone(d, 90) + 0.5 * tone(d, 180)) * (0.7 + 0.3 * np.sin(2 * np.pi * 8 * T(d)))
    x = 0.5 * hum
    for at, f in ((0.12, 880), (0.30, 1320), (0.50, 990)):
        b = np.sign(tone(0.06, f)) * 0.25 + tone(0.06, f) * 0.5
        place(x, b * env(len(b), 0.004, 0.02), at)
    write("drone_ambient1", filt(x, hi=5000) * env(len(x), 0.05, 0.2))
    x = 0.4 * hum.copy()
    for at in (0.1, 0.42):
        b = sweep(0.22, 1300, 620, exp=True)
        place(x, 0.7 * b * env(len(b), 0.005, 0.06), at)
    write("drone_ambient2", filt(x, hi=5000) * env(len(x), 0.05, 0.2))

    d = 0.35
    x = filt(noise(d), 800, 4000) * decay(int(SR * d), 0.06) + 0.8 * sweep(d, 950, 280, "square", True) * decay(int(SR * d), 0.09)
    write("drone_hurt", filt(x, hi=6000))

    d = 1.2
    s = sweep(d, 720, 55, "saw", True) * (1 + 0.3 * np.sin(2 * np.pi * 22 * T(d)))
    s = np.round(s * 6) / 6
    x = s * env(len(s), 0.01, 0.5) + 0.3 * filt(noise(d), 300, 3000) * decay(len(s), 0.35)
    write("drone_death", filt(x, hi=4500))

    for i, seed in enumerate((0.0, 0.3), 1):
        d = 0.18
        x = filt(noise(d), hi=900 + 300 * seed) * decay(int(SR * d), 0.04) + 0.6 * sweep(d, 190 + 40 * seed, 85, exp=True) * decay(int(SR * d), 0.05)
        write(f"drone_step{i}", x, 0.7)

    d = 0.95
    g = sweep(d, 78, 62, "saw") * (0.55 + 0.45 * np.sign(np.sin(2 * np.pi * 31 * T(d))))
    g += 0.25 * filt(noise(d), 200, 1200)
    write("beast_growl", filt(g, hi=1100) * env(len(g), 0.12, 0.3))

    d = 0.32
    x = 0.5 * filt(noise(d), hi=700) * decay(int(SR * d), 0.07)
    for at in (0.0, 0.07, 0.13, 0.2):
        b = sweep(0.05, 220 + 400 * rng.random(), 520 + 300 * rng.random(), exp=True)
        place(x, 0.6 * b * env(len(b), 0.004, 0.02), at)
    write("blob_squish", x, 0.75)

    d = 0.8
    x = sweep(d, 112, 118, "square") * (0.6 + 0.4 * np.sin(2 * np.pi * 41 * T(d))) + 0.5 * sweep(d, 224, 236, "saw")
    write("hover_buzz", filt(x, 80, 2600) * env(len(x), 0.08, 0.15), 0.6)

    # --- scientists
    x = np.zeros(int(SR * 0.95))
    for i, at in enumerate((0.0, 0.19, 0.38, 0.57)):
        place(x, voice(0.12, 168 - 12 * i, 138 - 12 * i, ((620, 160), (1750, 260), (2600, 300))) * (1 - 0.12 * i), at)
    write("scientist_laugh", filt(x, 90, 5000))
    write("scientist_hurt", filt(voice(0.26, 185, 105, ((720, 180), (1250, 240))), 90, 4500))

    # --- weapons
    d = 0.3
    x = 0.8 * filt(noise(d), lo=3500) * decay(int(SR * d), 0.07)
    x[: int(SR * 0.002)] += 1.0
    place(x, tone(0.05, 120) * env(int(SR * 0.05), 0.002, 0.03), 0.0)
    write("inject", x)

    d = 0.22
    x = filt(noise(d), 1500, 6500) * decay(int(SR * d), 0.03) + 0.9 * sweep(d, 160, 65, exp=True) * decay(int(SR * d), 0.05)
    write("dart_shoot", x)

    d = 0.42
    x = filt(noise(d), hi=650) * decay(int(SR * d), 0.08)
    for at in (0.03, 0.1, 0.16, 0.23, 0.3):
        b = sweep(0.035, 280 + 200 * rng.random(), 620 + 200 * rng.random(), exp=True)
        place(x, 0.5 * b * env(len(b), 0.003, 0.015), at)
    write("goo_splat", x)

    d = 0.7
    x = filt(noise(d), 120, 1900) + 0.6 * filt(noise(d), hi=180)
    write("flamethrower", x * env(len(x), 0.08, 0.35))

    d = 0.3
    gate = (np.sign(np.sin(2 * np.pi * 58 * T(d))) + 1) / 2
    x = filt(noise(d), 2000, 9000) * gate + 0.5 * sweep(d, 2400, 700, "square", True) * gate
    write("zap", x * decay(int(SR * d), 0.12))

    # --- transformation
    d = 3.0
    t = T(d)
    x = (sweep(d, 55, 112, "saw", True) + sweep(d, 55.8, 113.5, "saw", True) + 0.7 * sweep(d, 82, 168, "saw", True))
    chunks = np.array_split(np.arange(len(x)), 24)
    y = np.zeros_like(x)
    for i, idx in enumerate(chunks):          # opening low-pass sweep
        seg = np.zeros_like(x)
        seg[idx] = x[idx]
        y += filt(seg, hi=300 + 2900 * (i / 23) ** 2)
    x = y * (0.75 + 0.25 * np.sin(2 * np.pi * (3 + 4 * t / d) * t))
    at = 0.2
    gap = 0.62
    while at < d - 0.3:                       # accelerating heartbeat
        th = sweep(0.09, 64, 40, exp=True) * env(int(SR * 0.09), 0.004, 0.05)
        place(x, 2.2 * th, at)
        place(x, 1.6 * th, at + 0.16 * gap / 0.62)
        at += gap
        gap *= 0.86
    for _ in range(26):                       # bubbling
        b = sweep(0.04, 300 + 500 * rng.random(), 700 + 600 * rng.random(), exp=True)
        place(x, 0.35 * b * env(len(b), 0.003, 0.02), 0.3 + 2.5 * rng.random())
    write("transform_start", x * env(len(x), 0.4, 0.25))

    d = 1.7
    x = np.zeros(int(SR * d))
    place(x, 1.4 * sweep(0.55, 130, 38, exp=True) * env(int(SR * 0.55), 0.005, 0.3), 0.0)
    place(x, 0.4 * filt(noise(0.5), 400, 5000) * env(int(SR * 0.5), 0.02, 0.4), 0.0)
    for i, f in enumerate((440, 660, 880)):
        b = (0.5 * tone(0.13, f, "square") + tone(0.13, f)) * env(int(SR * 0.13), 0.004, 0.05)
        place(x, 0.45 * b, 0.45 + 0.13 * i)
    tail = (tone(0.8, 220) + tone(0.8, 221.5) + 0.5 * tone(0.8, 440)) * decay(int(SR * 0.8), 0.3)
    place(x, 0.5 * tail, 0.85)
    write("transform_complete", filt(x, hi=6000))

    d = 2.4
    t = T(d)
    x = tone(d, 174) + tone(d, 177) + 0.4 * tone(d, 348) + 0.25 * np.sin(2 * np.pi * 1392 * t + 3 * np.sin(2 * np.pi * 5 * t)) * (0.5 + 0.5 * np.sin(2 * np.pi * 6 * t))
    write("hypnosis", x * env(len(x), 0.5, 0.6), 0.6)

    d = 1.6
    t = T(d)
    fr = np.where((t // 0.2) % 2 == 0, 880.0, 660.0)
    x = np.sign(np.sin(2 * np.pi * np.cumsum(fr) / SR))
    write("alarm", filt(x, hi=4200) * env(len(x), 0.01, 0.05), 0.7)

    x = np.zeros(int(SR * 0.6))
    th = sweep(0.1, 62, 38, exp=True) * env(int(SR * 0.1), 0.004, 0.06)
    place(x, th, 0.0)
    place(x, 0.75 * th, 0.22)
    write("heartbeat", x)

    x = np.zeros(int(SR * 1.0))
    for i, f in enumerate((523.25, 659.25, 783.99, 1046.5)):
        b = (tone(0.5, f) + 0.3 * tone(0.5, 2 * f)) * decay(int(SR * 0.5), 0.14)
        place(x, b, 0.11 * i)
    write("cure", x, 0.7)

    # --- sounds.json
    sj = {}
    for event, _sub, variants in C.SOUNDS:
        files = [f"{C.MODID}:{event}"] if variants == 1 else [f"{C.MODID}:{event}{i}" for i in range(1, variants + 1)]
        for f in files:
            assert os.path.exists(os.path.join(OUT, f.split(":")[1] + ".ogg")), f
        sj[event] = {"subtitle": f"subtitles.{C.MODID}.{event}", "sounds": files}
    with open(os.path.join(ASSETS, "sounds.json"), "w") as f:
        json.dump(sj, f, indent=2)
    print("sounds:", len(os.listdir(OUT)), "files,", len(sj), "events")


if __name__ == "__main__":
    main()
