"""Procedural 64x64 player-layout skins (+ emissive glow layers) for 50 latex forms and 6 scientists."""
import os, sys, random, math
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image
from spec import FORMS, SCIENTISTS, MODID

OUT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets", MODID, "textures/entity")
os.makedirs(OUT + "/latex", exist_ok=True)
os.makedirs(OUT + "/scientist", exist_ok=True)

# (u, v, w, h, d)
BASE = {"head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "rarm": (40, 16, 4, 12, 4), "larm": (32, 48, 4, 12, 4),
        "rleg": (0, 16, 4, 12, 4), "lleg": (16, 48, 4, 12, 4)}
OVER = {"head": (32, 0, 8, 8, 8), "body": (16, 32, 8, 12, 4), "rarm": (40, 32, 4, 12, 4), "larm": (48, 48, 4, 12, 4),
        "rleg": (0, 32, 4, 12, 4), "lleg": (0, 48, 4, 12, 4)}


def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def clamp(c):
    return tuple(max(0, min(255, int(x))) for x in c)


def shade(c, k):
    return clamp((c[0] + k, c[1] + k, c[2] + k))


def mix(a, b, t):
    return clamp(tuple(a[i] * (1 - t) + b[i] * t for i in range(3)))


def put(img, x, y, c, a=255):
    if 0 <= x < 64 and 0 <= y < 64:
        img.putpixel((x, y), (c[0], c[1], c[2], a))


def latex_face(img, rect, base, rnd, face, gloss=True):
    """Glossy latex shading: vertical gradient, rim darkening, specular streak, slight noise."""
    x0, y0, w, h = rect
    lum = sum(base) / 3
    hi = 70 if lum < 60 else (45 if lum < 160 else 25)
    streak = rnd.randint(1, max(1, w - 2)) if w > 2 else 0
    for y in range(h):
        for x in range(w):
            k = 0
            if face not in ("top", "bottom"):
                k += int(18 * (0.5 - y / max(1, h - 1)))
            if x == 0 or x == w - 1:
                k -= 14
            if face == "top":
                k += 12
            if face == "bottom":
                k -= 18
            k += rnd.randint(-4, 4)
            c = shade(base, k)
            if gloss and face in ("front", "back", "right", "left") and x == streak and 1 <= y < h - 2 and rnd.random() < 0.8:
                c = shade(c, hi)
            put(img, x0 + x, y0 + y, c)


def paint_box(img, box, color_fn, rnd, gloss=True):
    for face, rect in faces(*box).items():
        latex_face(img, rect, color_fn(face), rnd, face, gloss)


def front(box):
    return faces(*box)["front"]


# ---------------------------------------------------------------- latex forms
def draw_form(f):
    rnd = random.Random(f["id"])
    P, S, G = f["primary"], f["secondary"], f["glow"]
    style = f["style"]
    skin = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    dark = shade(P, -40) if sum(P) / 3 > 60 else shade(P, 25)

    for name, box in BASE.items():
        paint_box(skin, box, lambda face: P, rnd)

    # limb tips: gloves & boots in secondary (drones) or darker paws (beasts)
    tip = S if style in ("drone", "hypno") else mix(P, (0, 0, 0), 0.25) if sum(P) > 200 else mix(P, S, 0.35)
    for name in ("rarm", "larm", "rleg", "lleg"):
        u, v, w, h, d = BASE[name]
        for face, (x0, y0, fw, fh) in faces(u, v, w, h, d).items():
            if face in ("top",):
                continue
            rows = range(fh) if face == "bottom" else range(fh - 4, fh)
            for y in rows:
                for x in range(fw):
                    put(skin, x0 + x, y0 + y, shade(tip, rnd.randint(-5, 5) - (8 if x in (0, fw - 1) else 0)))

    hx, hy, hw, hh = front(BASE["head"])
    bx, by, bw, bh = front(BASE["body"])

    if style in ("drone", "hypno"):
        # visor band wrapping the head
        for face in ("front", "right", "left", "back"):
            x0, y0, fw, fh = faces(*BASE["head"])[face]
            for y in (2, 3, 4):
                for x in range(fw):
                    if face == "back" and 1 < x < fw - 2:
                        continue
                    put(skin, x0 + x, y0 + y, shade((12, 12, 16), rnd.randint(-3, 3)))
            if face in ("front", "right", "left"):
                for x in range(fw):
                    put(glow, x0 + x, y0 + 3, G)
                    put(skin, x0 + x, y0 + 3, mix(G, (0, 0, 0), 0.3))
        # chin plate & secondary panels on the chest / back
        for x in range(1, 7):
            put(skin, hx + x, hy + 6, shade(S, 5))
        for y in range(1, 11):
            put(skin, bx + 0, by + y, shade(S, -5))
            put(skin, bx + 7, by + y, shade(S, -5))
        # emblem
        if style == "hypno":
            pts = [(3, 2), (4, 2), (5, 3), (5, 4), (4, 5), (3, 5), (2, 4), (2, 3), (3, 3), (4, 4)]
        else:
            pts = [(3, 2), (4, 2), (2, 3), (5, 3), (2, 4), (5, 4), (3, 5), (4, 5), (3, 3), (4, 4)]
        for (x, y) in pts:
            put(skin, bx + x, by + y, mix(G, (0, 0, 0), 0.25))
            put(glow, bx + x, by + y, G)
        # belt line and number code
        for x in range(8):
            put(skin, bx + x, by + 8, shade(S, -10))
        for x in (1, 3, 4):
            put(glow, bx + x, by + 8, mix(G, (0, 0, 0), 0.3))
        # back spine light
        x0, y0, fw, fh = faces(*BASE["body"])["back"]
        for y in range(1, 9, 2):
            put(glow, x0 + 3, y0 + y, G)
            put(glow, x0 + 4, y0 + y, G)
    else:
        # ---- beast / goo / patterned faces ----
        eye_white = G
        if style == "goo":
            for (x, y) in [(1, 3), (2, 3), (1, 4), (2, 4), (5, 3), (6, 3), (5, 4), (6, 4)]:
                put(skin, hx + x, hy + y, eye_white)
                put(glow, hx + x, hy + y, eye_white)
            # gaping smile
            for x in range(2, 6):
                put(skin, hx + x, hy + 6, shade(P, -25) if sum(P) > 60 else (60, 60, 70))
        else:
            # muzzle (lighter) + nose
            muzzle = S if style in ("twotone", "spotted", "striped") else mix(P, S, 0.6)
            for y in (5, 6, 7):
                for x in range(2, 6):
                    put(skin, hx + x, hy + y, shade(muzzle, rnd.randint(-4, 4)))
            put(skin, hx + 3, hy + 5, (10, 10, 12))
            put(skin, hx + 4, hy + 5, (10, 10, 12))
            # glowing eyes with pupils
            for (x, y) in [(1, 3), (2, 3), (5, 3), (6, 3)]:
                put(skin, hx + x, hy + y, eye_white)
                put(glow, hx + x, hy + y, eye_white)
            put(skin, hx + 2, hy + 3, (5, 5, 8))
            put(skin, hx + 5, hy + 3, (5, 5, 8))
            put(glow, hx + 2, hy + 3, (0, 0, 0, 0)) if False else None
            glow.putpixel((hx + 2, hy + 3), (0, 0, 0, 0))
            glow.putpixel((hx + 5, hy + 3), (0, 0, 0, 0))
            # brows
            for x in (1, 2, 5, 6):
                put(skin, hx + x, hy + 2, shade(P, -20 if sum(P) > 90 else 18))
            # belly
            for y in range(2, 11):
                for x in range(2, 6):
                    put(skin, bx + x, by + y, shade(mix(P, S, 0.55), rnd.randint(-4, 4)))

        if style == "twotone":
            # whole front light, back dark
            for name in ("body", "rleg", "lleg", "rarm", "larm"):
                x0, y0, fw, fh = front(BASE[name])
                for y in range(fh - (4 if name != "body" else 0)):
                    for x in range(1 if name == "body" else 0, fw - (1 if name == "body" else 0)):
                        put(skin, x0 + x, y0 + y, shade(S, rnd.randint(-4, 4) - (6 if x in (0, fw - 1) else 0)))
            for y in (5, 6, 7):
                for x in range(1, 7):
                    put(skin, hx + x, hy + y, shade(S, rnd.randint(-3, 3)))
            # orca eye patch / shark gills
            for (x, y) in [(1, 2), (2, 2), (6, 2), (5, 2)]:
                put(skin, hx + x, hy + y, shade(S, 0))
        elif style == "striped":
            for name, box in BASE.items():
                for face, (x0, y0, fw, fh) in faces(*box).items():
                    if face in ("top", "bottom"):
                        continue
                    phase = rnd.randint(0, 2)
                    for y in range(fh):
                        if (y + phase) % 4 == 0:
                            for x in range(fw):
                                if rnd.random() < 0.85:
                                    put(skin, x0 + x, y0 + y + (1 if (x + phase) % 3 == 0 and y + 1 < fh else 0), shade(S, rnd.randint(-5, 5)))
        elif style == "spotted":
            for name, box in BASE.items():
                for face, (x0, y0, fw, fh) in faces(*box).items():
                    for _ in range(max(1, fw * fh // 14)):
                        sx, sy = rnd.randrange(fw), rnd.randrange(fh)
                        put(skin, x0 + sx, y0 + sy, shade(S, rnd.randint(-6, 6)))
                        if rnd.random() < 0.5 and sx + 1 < fw:
                            put(skin, x0 + sx + 1, y0 + sy, shade(S, rnd.randint(-6, 6)))
        elif style == "crystal":
            for name, box in BASE.items():
                for face, (x0, y0, fw, fh) in faces(*box).items():
                    for _ in range(max(1, fw * fh // 20)):
                        sx, sy = rnd.randrange(fw), rnd.randrange(fh)
                        for t in range(3):
                            if sx + t < fw and sy + t < fh:
                                put(skin, x0 + sx + t, y0 + sy + t, shade(S, 30 - t * 8))
            # glowing crystal shards on shoulders/back
            x0, y0, fw, fh = faces(*BASE["body"])["back"]
            for (x, y) in [(1, 1), (2, 2), (5, 1), (6, 2), (3, 5), (4, 6)]:
                put(skin, x0 + x, y0 + y, G)
                put(glow, x0 + x, y0 + y, G)
        elif style == "goo":
            # lumpy darker blotches
            for name, box in BASE.items():
                for face, (x0, y0, fw, fh) in faces(*box).items():
                    for _ in range(fw * fh // 10):
                        put(skin, x0 + rnd.randrange(fw), y0 + rnd.randrange(fh), shade(S, rnd.randint(-8, 8)))

    # ---- overlay layer: glossy drips on top of everything ----
    drip = shade(P, 28) if sum(P) / 3 < 140 else shade(P, -22)
    for name, box in OVER.items():
        for face, (x0, y0, fw, fh) in faces(*box).items():
            if face in ("top", "bottom"):
                continue
            n = 1 if name != "body" else 2
            if style == "goo":
                n += 2
            for _ in range(n):
                if rnd.random() < 0.65:
                    dx = rnd.randrange(fw)
                    start = 0 if name != "head" else rnd.randint(0, 1)
                    length = rnd.randint(2, max(3, fh // 2))
                    for y in range(start, min(fh, start + length)):
                        put(skin, x0 + dx, y0 + y, drip, 255)
                    if start + length < fh:
                        put(skin, x0 + dx, y0 + start + length, shade(drip, 15), 255)
    if style == "goo":
        # goo dripping off the head top
        x0, y0, fw, fh = faces(*OVER["head"])["top"]
        for x in range(fw):
            for y in range(fh):
                if rnd.random() < 0.35:
                    put(skin, x0 + x, y0 + y, drip, 255)
    return skin, glow


# ---------------------------------------------------------------- scientists
COAT = (236, 238, 240)


def draw_scientist(s):
    rnd = random.Random(s["id"])
    fur, fur2, G = s["fur"], s["fur2"], s["glow"]
    sp = s["species"]
    skin = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    def matte(box, color):
        for face, rect in faces(*box).items():
            latex_face(skin, rect, color, rnd, face, gloss=False)

    matte(BASE["head"], fur)
    matte(BASE["body"], COAT)
    matte(BASE["rarm"], COAT)
    matte(BASE["larm"], COAT)
    trousers = (40, 42, 52)
    matte(BASE["rleg"], trousers)
    matte(BASE["lleg"], trousers)

    hx, hy, hw, hh = front(BASE["head"])
    # muzzle & nose
    for y in (5, 6, 7):
        for x in range(2, 6):
            put(skin, hx + x, hy + y, shade(fur2, rnd.randint(-4, 4)))
    nose = (230, 140, 160) if sp == "rat" else (20, 18, 20)
    put(skin, hx + 3, hy + 5, nose)
    put(skin, hx + 4, hy + 5, nose)
    if sp == "fox":
        for (x, y) in [(0, 5), (1, 6), (6, 6), (7, 5), (1, 5), (6, 5)]:
            put(skin, hx + x, hy + y, (245, 240, 232))
    if sp == "raven":
        for y in (5, 6, 7):
            for x in range(2, 6):
                put(skin, hx + x, hy + y, shade(fur, 10))
    # sinister grin
    for x in (2, 3, 4, 5):
        put(skin, hx + x, hy + 7, (30, 10, 12))
    put(skin, hx + 3, hy + 7, (240, 240, 240))
    # goggles: dark rims + glowing lenses
    for x in range(8):
        put(skin, hx + x, hy + 3, (25, 25, 30))
    for side in faces(*BASE["head"]):
        if side in ("right", "left", "back"):
            x0, y0, fw, fh = faces(*BASE["head"])[side]
            for x in range(fw):
                put(skin, x0 + x, y0 + 3, (25, 25, 30))
    for (x, y) in [(1, 3), (2, 3), (5, 3), (6, 3)]:
        put(skin, hx + x, hy + y, G)
        put(glow, hx + x, hy + y, G)
    for (x, y) in [(1, 2), (2, 2), (5, 2), (6, 2), (1, 4), (2, 4), (5, 4), (6, 4)]:
        put(skin, hx + x, hy + y, (60, 60, 68))

    # lab coat front: shirt + tie + lapels + pocket + badge + latex stains
    bx, by, bw, bh = front(BASE["body"])
    for y in range(0, 8):
        put(skin, bx + 3, by + y, (70, 80, 100))
        put(skin, bx + 4, by + y, (70, 80, 100))
    for y in range(1, 7):
        put(skin, bx + 3 + (y % 2 == 0), by + y, (150, 20, 30))
    for y in range(0, 12):
        put(skin, bx + 2, by + y, shade(COAT, -35))
        put(skin, bx + 5, by + y, shade(COAT, -35))
    put(skin, bx + 1, by + 0, shade(COAT, -20))
    put(skin, bx + 6, by + 0, shade(COAT, -20))
    # pocket with pens
    put(skin, bx + 6, by + 3, (40, 70, 200))
    put(skin, bx + 7, by + 3, (200, 40, 40))
    for x in (6, 7):
        put(skin, bx + x, by + 4, shade(COAT, -25))
    # name badge
    put(skin, bx + 0, by + 4, (210, 180, 60))
    put(skin, bx + 1, by + 4, (210, 180, 60))
    # buttons
    for y in (8, 10):
        put(skin, bx + 1, by + y, (120, 120, 130))
    # latex stains on the coat
    for _ in range(4):
        box = rnd.choice(["body", "rarm", "larm"])
        face = rnd.choice(["front", "right", "left", "back"])
        x0, y0, fw, fh = faces(*BASE[box])[face]
        sx, sy = rnd.randrange(fw), rnd.randrange(2, fh - 4)
        put(skin, x0 + sx, y0 + sy, (15, 15, 20))
        if sy + 1 < fh:
            put(skin, x0 + sx, y0 + sy + 1, (25, 25, 32))
    # hands (fur, gloved in black rubber for some)
    glove = (20, 20, 24) if sp in ("bear", "wolf") else fur
    for name in ("rarm", "larm"):
        for face, (x0, y0, fw, fh) in faces(*BASE[name]).items():
            rows = range(fh) if face == "bottom" else (range(fh - 3, fh) if face != "top" else [])
            for y in rows:
                for x in range(fw):
                    put(skin, x0 + x, y0 + y, shade(glove, rnd.randint(-4, 4)))
            if face not in ("top", "bottom"):
                for x in range(fw):
                    put(skin, x0 + x, y0 + fh - 4, shade(COAT, -30))  # cuff
    # shoes
    for name in ("rleg", "lleg"):
        for face, (x0, y0, fw, fh) in faces(*BASE[name]).items():
            rows = range(fh) if face == "bottom" else (range(fh - 2, fh) if face != "top" else [])
            for y in rows:
                for x in range(fw):
                    put(skin, x0 + x, y0 + y, shade((18, 16, 16), rnd.randint(-3, 3)))
    # back of coat: stitched seam
    x0, y0, fw, fh = faces(*BASE["body"])["back"]
    for y in range(4, 12):
        put(skin, x0 + 4, y0 + y, shade(COAT, -18))

    # overlay: coat tails hanging over the upper legs
    for name in ("rleg", "lleg"):
        for face, (x0, y0, fw, fh) in faces(*OVER[name]).items():
            if face in ("top", "bottom"):
                continue
            for y in range(0, 5):
                for x in range(fw):
                    if face == "front" and ((name == "rleg" and x == fw - 1) or (name == "lleg" and x == 0)):
                        continue
                    put(skin, x0 + x, y0 + y, shade(COAT, rnd.randint(-4, 4) - (12 if y == 4 else 0)))
    # overlay: collar
    x0, y0, fw, fh = faces(*OVER["body"])["back"]
    for x in range(fw):
        put(skin, x0 + x, y0, shade(COAT, 6))
    # overlay: hair tuft / head feathers
    x0, y0, fw, fh = faces(*OVER["head"])["top"]
    for _ in range(6):
        put(skin, x0 + rnd.randrange(fw), y0 + rnd.randrange(fh), shade(fur, -15))
    return skin, glow


def main():
    for f in FORMS:
        skin, glow = draw_form(f)
        skin.save(f"{OUT}/latex/{f['id']}.png")
        glow.save(f"{OUT}/latex/{f['id']}_glow.png")
    for s in SCIENTISTS:
        skin, glow = draw_scientist(s)
        skin.save(f"{OUT}/scientist/{s['id']}.png")
        glow.save(f"{OUT}/scientist/{s['id']}_glow.png")

    # parts texture (tinted in code)
    rnd = random.Random(7)
    parts = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(32):
        for x in range(32):
            v = 225 + rnd.randint(-6, 6) + (18 if (x + y) % 9 == 0 else 0)
            parts.putpixel((x, y), (min(255, v), min(255, v), min(255, v), 255))
    parts.save(f"{OUT}/latex_parts.png")

    # syringe dart (arrow-style UV layout, 32x32)
    dart = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for x in range(0, 16):
        for y in range(0, 5):
            c = None
            if x <= 3 and y == 2:
                c = (210, 215, 225)                     # needle
            elif 4 <= x <= 10 and 1 <= y <= 3:
                c = (190, 230, 245) if y == 1 else (40, 30, 60) if x < 9 else (120, 60, 200)  # glass + latex serum
            elif x == 11 and 0 <= y <= 4:
                c = (90, 90, 100)
            elif 12 <= x <= 15 and y in (0, 4) or (12 <= x <= 15 and y == 2):
                c = (230, 60, 120)                      # tail fins
            if c:
                dart.putpixel((x, y), c + (255,))
    for x in range(5):
        for y in range(5, 10):
            if x == 2 or y == 7:
                dart.putpixel((x, y), (230, 60, 120, 255))
    dart.save(f"{OUT}/syringe_dart.png")
    print("skins done")


if __name__ == "__main__":
    main()
