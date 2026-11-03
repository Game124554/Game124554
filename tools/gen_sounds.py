"""Synthesizes every sound effect with numpy and encodes to Ogg Vorbis (what Minecraft needs)."""
import os, sys, subprocess, wave, tempfile
import numpy as np
sys.path.insert(0, os.path.dirname(__file__))
from spec import MODID

SR = 22050
OUT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets", MODID, "sounds")
os.makedirs(OUT, exist_ok=True)


def t(d):
    return np.linspace(0, d, int(SR * d), endpoint=False)


def env(n, a=0.01, r=0.2):
    e = np.ones(n)
    na, nr = max(1, int(a * SR)), max(1, int(r * SR))
    e[:na] = np.linspace(0, 1, na)
    e[-nr:] *= np.linspace(1, 0, nr)
    return e


def lowpass(x, k):
    """one-pole low-pass, k in (0,1): smaller = darker"""
    y = np.zeros_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc += k * (v - acc)
        y[i] = acc
    return y


def sweep(f0, f1, d, shape="sin"):
    tt = t(d)
    f = np.geomspace(max(f0, 1), max(f1, 1), len(tt))
    ph = 2 * np.pi * np.cumsum(f) / SR
    if shape == "saw":
        return 2 * ((ph / (2 * np.pi)) % 1) - 1
    if shape == "sq":
        return np.sign(np.sin(ph))
    return np.sin(ph)


def bubble(rng, d=0.08, f=None):
    f = f or rng.uniform(250, 700)
    tt = t(d)
    return np.sin(2 * np.pi * (f + 900 * tt / d) * tt) * np.exp(-tt * 40)


def place(buf, clip, at):
    i = int(at * SR)
    j = min(len(buf), i + len(clip))
    buf[i:j] += clip[:j - i]


def norm(x, peak=0.85):
    m = np.max(np.abs(x)) or 1
    return x / m * peak


def gurgle(d, seed, pitch=1.0):
    rng = np.random.default_rng(seed)
    n = int(SR * d)
    base = lowpass(rng.standard_normal(n), 0.04) * 3
    lfo = 0.5 + 0.5 * np.sin(2 * np.pi * rng.uniform(5, 9) * t(d))
    growl = sweep(110 * pitch, 70 * pitch, d, "saw") * 0.35 * lfo
    out = base * lfo + growl
    for _ in range(int(d * 14)):
        place(out, bubble(rng) * 0.6, rng.uniform(0, d - 0.1))
    return out * env(n, 0.05, 0.3)


def formant_voice(f0s, d, vowels, seed):
    """crude formant synthesis for the scientists' sinister chuckles / grunts"""
    tt = t(d)
    f0 = np.interp(tt, np.linspace(0, d, len(f0s)), f0s)
    ph = 2 * np.pi * np.cumsum(f0) / SR
    src = sum(np.sin(k * ph) / k for k in range(1, 25))
    out = np.zeros_like(src)
    for (fr, bw, g) in vowels:
        # resonator via two-pole filter
        r = np.exp(-np.pi * bw / SR)
        c = 2 * r * np.cos(2 * np.pi * fr / SR)
        y1 = y2 = 0.0
        y = np.zeros_like(src)
        for i, v in enumerate(src):
            yy = v + c * y1 - r * r * y2
            y[i] = yy
            y2, y1 = y1, yy
        out += norm(y) * g
    return out


SOUNDS = {}


def latex_ambient(i):
    return gurgle(1.3, 10 + i, 1.0 + 0.15 * i)


def latex_hurt(i):
    rng = np.random.default_rng(20 + i)
    d = 0.35
    n = int(SR * d)
    sq = lowpass(rng.standard_normal(n), 0.25) * np.exp(-t(d) * 12)
    tone = sweep(420, 160, d, "saw") * np.exp(-t(d) * 8) * 0.6
    return (sq + tone) * env(n, 0.003, 0.08)


def latex_death(i):
    d = 1.3
    rng = np.random.default_rng(30 + i)
    g = gurgle(d, 31 + i, 0.8) * np.linspace(1, 0.4, int(SR * d))
    tone = sweep(300, 50, d, "saw") * 0.5 * np.exp(-t(d) * 2)
    splat = lowpass(rng.standard_normal(int(SR * 0.25)), 0.3) * np.exp(-t(0.25) * 18)
    out = g + tone
    place(out, splat * 1.5, 1.0)
    return out * env(len(out), 0.01, 0.2)


def drone_ambient(i):
    d = 1.1
    tt = t(d)
    out = np.zeros(len(tt))
    hum = np.sin(2 * np.pi * 110 * tt) * np.sin(2 * np.pi * (37 + 9 * i) * tt) * 0.4
    out += hum
    notes = [880, 660, 990, 740] if i == 0 else [523, 784, 659]
    for k, f in enumerate(notes):
        b = np.sign(np.sin(2 * np.pi * f * t(0.09))) * env(int(SR * 0.09), 0.005, 0.03) * 0.35
        place(out, b, 0.12 + k * 0.17)
    return out * env(len(tt), 0.05, 0.25)


def drone_hurt(i):
    d = 0.3
    s = sweep(1200, 200, d, "sq") * 0.6
    s = np.round(s * 4) / 4  # bitcrush
    return s * env(len(s), 0.002, 0.1)


def goo_ambient(i):
    rng = np.random.default_rng(60 + i)
    d = 1.4
    n = int(SR * d)
    out = lowpass(rng.standard_normal(n), 0.02) * 2
    for _ in range(22):
        place(out, bubble(rng, 0.07) * rng.uniform(0.3, 0.9), rng.uniform(0, d - 0.1))
    return out * env(n, 0.1, 0.3)


AH = [(700, 110, 1.0), (1220, 120, 0.5), (2600, 160, 0.25)]
EH = [(530, 90, 1.0), (1840, 110, 0.55), (2480, 160, 0.25)]
UH = [(640, 100, 1.0), (1190, 110, 0.4), (2390, 160, 0.2)]


def scientist_ambient(i):
    d = 1.1
    out = np.zeros(int(SR * d))
    for k in range(3):
        f = 150 - k * 12 + i * 20
        v = formant_voice([f * 1.1, f], 0.17, EH, 70 + k) * env(int(SR * 0.17), 0.01, 0.06)
        breath = np.random.default_rng(k).standard_normal(int(SR * 0.17)) * 0.15 * env(int(SR * 0.17), 0.005, 0.1)
        place(out, v + breath, 0.08 + k * 0.28)
    return out


def scientist_hurt(i):
    d = 0.32
    return formant_voice([210, 130], d, UH, 80) * env(int(SR * d), 0.005, 0.12)


def scientist_death(i):
    d = 1.0
    return formant_voice([230, 180, 90], d, AH, 90) * env(int(SR * d), 0.01, 0.4)


def syringe_inject(i):
    rng = np.random.default_rng(100)
    d = 0.45
    n = int(SR * d)
    hiss = rng.standard_normal(n)
    hiss = hiss - lowpass(hiss, 0.3)
    hiss *= np.exp(-t(d) * 7) * 0.6
    click = np.zeros(n)
    click[:120] = np.sin(2 * np.pi * 2500 * np.arange(120) / SR) * np.exp(-np.arange(120) / 20)
    squish = sweep(300, 900, d, "sin") * 0.2 * np.exp(-t(d) * 5)
    return (hiss + click + squish) * env(n, 0.001, 0.1)


def syringe_shoot(i):
    rng = np.random.default_rng(110)
    d = 0.25
    n = int(SR * d)
    pop = lowpass(rng.standard_normal(n), 0.5) * np.exp(-t(d) * 30)
    thwip = sweep(500, 1800, d, "sin") * np.exp(-t(d) * 14) * 0.6
    return (pop + thwip) * env(n, 0.001, 0.05)


def hypno_hum(i):
    d = 2.0
    tt = t(d)
    vib = 1 + 0.01 * np.sin(2 * np.pi * 5 * tt)
    chord = sum(np.sin(2 * np.pi * f * vib * tt) for f in (220, 277.2, 329.6, 440)) / 4
    pulse = 0.55 + 0.45 * np.sin(2 * np.pi * 3.5 * tt)
    shimmer = np.sin(2 * np.pi * 1760 * tt) * 0.08 * (0.5 + 0.5 * np.sin(2 * np.pi * 7 * tt))
    return (chord * pulse + shimmer) * env(len(tt), 0.3, 0.6)


def transform(i):
    d = 3.5
    rng = np.random.default_rng(130)
    n = int(SR * d)
    rise = sweep(70, 650, d, "saw") * 0.35 * np.linspace(0.2, 1, n)
    rise = np.tanh(rise * 3) * 0.5
    goo = gurgle(d, 131, 1.0) * np.linspace(0.6, 1.2, n)
    out = rise + goo
    for _ in range(40):
        place(out, bubble(rng, 0.06, rng.uniform(300, 1200)) * 0.5, rng.uniform(0, d - 0.1))
    splat = lowpass(rng.standard_normal(int(SR * 0.4)), 0.25) * np.exp(-t(0.4) * 9) * 2
    place(out, splat, d - 0.45)
    sh = np.sin(2 * np.pi * 1320 * t(0.6)) * np.exp(-t(0.6) * 5) * 0.3
    place(out, sh, d - 0.6)
    return out * env(n, 0.05, 0.15)


def heartbeat(i):
    d = 0.8
    out = np.zeros(int(SR * d))
    for at, amp in ((0.0, 1.0), (0.22, 0.7)):
        tt = t(0.18)
        thump = np.sin(2 * np.pi * (55 + 30 * np.exp(-tt * 30)) * tt) * np.exp(-tt * 22) * amp
        place(out, thump, at + 0.02)
    return out


def purifier_shot(i):
    d = 0.45
    n = int(SR * d)
    zap = sweep(1600, 280, d, "sq") * 0.3 * np.exp(-t(d) * 6)
    ring = np.sin(2 * np.pi * 2400 * t(d)) * 0.2 * np.exp(-t(d) * 10)
    return (zap + ring) * env(n, 0.002, 0.1)


def blade_swing(i):
    rng = np.random.default_rng(160)
    d = 0.35
    n = int(SR * d)
    w = rng.standard_normal(n)
    w = lowpass(w, 0.15) - lowpass(w, 0.03)
    shape = np.sin(np.pi * np.linspace(0, 1, n)) ** 2
    slap = lowpass(rng.standard_normal(int(SR * 0.1)), 0.3) * np.exp(-t(0.1) * 40)
    out = w * shape * 2
    place(out, slap, 0.22)
    return out


def pod_activate(i):
    d = 1.6
    rng = np.random.default_rng(170)
    n = int(SR * d)
    whir = sweep(60, 420, d, "saw") * 0.25 * np.linspace(0.3, 1, n)
    hiss = rng.standard_normal(n)
    hiss = (hiss - lowpass(hiss, 0.2)) * 0.25 * np.clip(np.linspace(-1, 1, n), 0, 1)
    out = whir + hiss
    for k, f in enumerate((660, 880, 1320)):
        place(out, np.sin(2 * np.pi * f * t(0.25)) * np.exp(-t(0.25) * 8) * 0.4, 1.0 + k * 0.12)
    return out * env(n, 0.05, 0.2)


def infect_hit(i):
    rng = np.random.default_rng(180)
    d = 0.3
    n = int(SR * d)
    splat = lowpass(rng.standard_normal(n), 0.3) * np.exp(-t(d) * 16)
    thump = np.sin(2 * np.pi * 90 * t(d)) * np.exp(-t(d) * 18)
    return (splat + thump) * env(n, 0.001, 0.05)


VARIANTS = {"latex_ambient": 3, "drone_ambient": 2, "goo_ambient": 2, "scientist_ambient": 2, "latex_hurt": 2}
FUNCS = {k: v for k, v in globals().items() if callable(v) and k in [
    "latex_ambient", "latex_hurt", "latex_death", "drone_ambient", "drone_hurt", "goo_ambient", "scientist_ambient",
    "scientist_hurt", "scientist_death", "syringe_inject", "syringe_shoot", "hypno_hum", "transform", "heartbeat",
    "purifier_shot", "blade_swing", "pod_activate", "infect_hit"]}


def write_ogg(data, path):
    data = norm(data)
    pcm = (data * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tf:
        wav = tf.name
    with wave.open(wav, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "4", path], check=True)
    os.remove(wav)


def main():
    files = {}
    for name, fn in FUNCS.items():
        files[name] = []
        for i in range(VARIANTS.get(name, 1)):
            fname = name if i == 0 else f"{name}{i + 1}"
            write_ogg(fn(i), f"{OUT}/{fname}.ogg")
            files[name].append(fname)
    import json
    with open(os.path.join(OUT, "..", "sounds.json"), "w") as fh:
        json.dump({k: {"subtitle": f"subtitles.{MODID}.{k}", "sounds": [f"{MODID}:{f}" for f in v]} for k, v in files.items()}, fh, indent=2)
    print("sounds:", sum(len(v) for v in files.values()))


if __name__ == "__main__":
    main()
