"""Procedurally paints every texture of the mod (entities, player forms, spawn eggs,
blocks, items, GUI overlays).  Run:  python3 tools/gen_textures.py
"""
import json
import math
import os
import random
import zlib

from PIL import Image, ImageDraw

import content as C

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", C.MODID)
TEX = os.path.join(ASSETS, "textures")
PREVIEW = os.path.join(ROOT, "tools", "_preview")


# ------------------------------------------------------------------ helpers
def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def clamp(v):
    return max(0, min(255, int(round(v))))


def mix(a, b, t):
    return tuple(clamp(a[i] * (1 - t) + b[i] * t) for i in range(3))


WHITE = (255, 255, 255)
BLACK = (0, 0, 0)


def shade(c, f):
    if f <= 1.0:
        return tuple(clamp(c[i] * f) for i in range(3))
    return mix(c, WHITE, min(1.0, (f - 1.0)))


def hv(*parts):
    """Deterministic pseudo random value in [0,1)."""
    return (zlib.crc32("|".join(str(p) for p in parts).encode()) & 0xFFFFFFFF) / 4294967296.0


def save(img, *path):
    p = os.path.join(TEX, *path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


def new(w=16, h=16, fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (w, h), fill)


def put(img, x, y, c, a=255):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), (c[0], c[1], c[2], a if len(c) == 3 else c[3]))


# ------------------------------------------------------------------ box unwrapping
def faces(u, v, w, h, d):
    """Minecraft box-UV layout. west = min X side, front = min Z (north), east, back."""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "west": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "east": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


def paint_box(img, u, v, w, h, d, fn):
    for face, (fx, fy, fw, fh) in faces(u, v, w, h, d).items():
        for y in range(fh):
            for x in range(fw):
                c = fn(face, x, y, fw, fh)
                if c is not None:
                    put(img, fx + x, fy + y, c)


def gloss(c, face, x, y, fw, fh):
    """Shiny latex shading for one pixel of a box face."""
    if face == "top":
        col = shade(c, 1.16)
        if x == 1 and y == 1:
            col = mix(col, WHITE, 0.45)
        return col
    if face == "bottom":
        return shade(c, 0.68)
    f = 1.10 - 0.30 * (y / max(1, fh - 1))
    if x == 0 or x == fw - 1:
        f -= 0.08
    col = shade(c, f)
    lum = (c[0] + c[1] + c[2]) / 3
    if lum < 60:  # very dark latex needs a visible sheen
        col = mix(col, (70, 74, 96), 0.22 * (1 - y / max(1, fh - 1)))
    if fw >= 3 and x == 1 and 1 <= y <= max(1, fh // 3):
        col = mix(col, WHITE, 0.42)
    return col


COAT = (242, 242, 245)
COAT_SHADE = (200, 204, 212)
PANTS = (48, 52, 60)
SHOES = (24, 24, 28)
BONE = (232, 220, 192)
BEAKC = (232, 160, 32)
INNER_EAR = (240, 160, 184)


def pattern(m, part, face, x, y, fw, fh, col):
    """Overlay of the body pattern that applies to every limb."""
    base, acc, glow = (rgb(v) for v in m["pal"])
    p = m["pattern"]
    h = hv(m["name"], part, face, x, y)
    if p == "tiger":
        if face != "bottom" and (y + (x // 2)) % 3 == 0 and h > 0.22:
            return shade(acc, 1.0 if face != "top" else 1.15)
    elif p == "spots":
        if h < 0.15:
            return shade(acc, 1.0)
    elif p == "veins":
        if (x + 2 * y) % 7 == 0 and h < 0.7:
            return shade(glow, 0.85)
        if h < 0.05:
            return glow
    elif p == "goo":
        drip = int(hv(m["name"], part, face, x) * fh * 0.6)
        if face in ("front", "back", "west", "east"):
            if y < drip:
                col = shade(col, 1.14)
            elif y == drip:
                col = shade(col, 0.78)
        if h < 0.04:
            col = mix(col, WHITE, 0.55)
    elif p == "crystal":
        if (x + y) % 4 == 0:
            return shade(acc, 1.1)
        if (x - y) % 5 == 0 and h < 0.5:
            return glow
    return col


def head_fn(m):
    base, acc, glow = (rgb(v) for v in m["pal"])
    face_style = m["face"]

    def fn(f, x, y, fw, fh):
        col = pattern(m, "head", f, x, y, fw, fh, gloss(base, f, x, y, fw, fh))
        if f == "front":
            if face_style == "visor":
                if y in (3, 4) and 1 <= x <= 6:
                    col = shade(glow, 1.3 if y == 3 else 0.9)
                    if x in (1, 6):
                        col = shade(col, 0.8)
                elif y in (2, 5) and 1 <= x <= 6:
                    col = shade(base, 0.5)
            elif face_style == "eyes":
                if x in (1, 2, 5, 6) and y == 3:
                    col = shade(glow, 1.25)
                elif x in (1, 2, 5, 6) and y == 4:
                    col = shade(glow, 0.8)
            elif face_style == "animal":
                if 5 <= y <= 7 and 2 <= x <= 5:
                    col = gloss(acc, f, x, y, fw, fh)
                if y == 3 and x in (1, 6):
                    col = (245, 245, 245)
                if y == 3 and x in (2, 5):
                    col = glow
                if y == 2 and x in (1, 2, 5, 6):
                    col = shade(base, 0.6)
                if y == 5 and x in (3, 4):
                    col = (28, 22, 28)
                if y == 7 and x in (3, 4):
                    col = shade(acc, 0.65)
            elif face_style == "spiral":
                for ex in (1, 4):
                    if ex <= x <= ex + 2 and 2 <= y <= 4:
                        col = WHITE if (x == ex + 1 and y == 3) else glow
            elif face_style == "beak":
                if y == 3 and x in (1, 6):
                    col = glow
                if y == 2 and x in (1, 2, 5, 6):
                    col = shade(base, 0.55)
                if 4 <= y <= 6 and x in (3, 4):
                    col = shade(BEAKC, 1.0 if y < 6 else 0.75)
            elif face_style == "blank":
                if x == 2 and 2 <= y <= 4:
                    col = mix(col, WHITE, 0.2)
        if face_style == "visor" and m["pattern"] != "coat":
            if f == "top" and x in (3, 4):
                col = shade(acc, 1.05)
            if f == "back" and x in (3, 4) and y <= 5:
                col = shade(acc, 0.9)
        return col

    return fn


def body_fn(m):
    base, acc, glow = (rgb(v) for v in m["pal"])
    p = m["pattern"]
    shirt = rgb(m["coat"]) if m.get("coat") else acc

    def fn(f, x, y, fw, fh):
        if p == "coat":
            col = gloss(COAT, f, x, y, fw, fh)
            if f == "bottom":
                return PANTS
            if f == "front":
                if x in (3, 4) and y <= 7:
                    col = shade(shirt, 1.0 if y != 0 else 1.0)
                    if y == 0:
                        col = gloss(base, f, x, y, fw, fh)
                    elif 1 <= y <= 4 and x == 3:
                        col = shade(shirt, 0.7)
                if x in (2, 5) and y <= 2:
                    col = COAT_SHADE
                if x == 5 and y in (5, 8):
                    col = (120, 124, 132)
                if y == 6 and x in (1, 2):
                    col = COAT_SHADE
                if (x, y) == (1, 3):
                    col = glow
            if f == "back" and x in (3, 4) and y >= 6:
                col = COAT_SHADE
            return col
        col = pattern(m, "body", f, x, y, fw, fh, gloss(base, f, x, y, fw, fh))
        if f == "front":
            if p == "drone":
                if 2 <= x <= 5 and 1 <= y <= 4:
                    col = shade(base, 0.6) if sum(base) > 120 else mix(base, (80, 84, 100), 0.5)
                    if y == 2 and x in (3, 4):
                        col = glow
                if 6 <= y <= 7 and 2 <= x <= 5 and x % 2 == 0:
                    col = acc
                if y == 10:
                    col = shade(acc, 0.9)
            elif p == "plates":
                if y in (4, 8):
                    col = shade(base, 0.5) if sum(base) > 120 else mix(base, (90, 94, 110), 0.5)
                if y == 0:
                    col = acc
                if x in (3, 4) and 1 <= y <= 3:
                    col = shade(acc, 0.9)
                if (x, y) in ((3, 6), (4, 6)):
                    col = glow
            elif p == "hazard":
                if 4 <= y <= 7:
                    col = acc if ((x + y) // 2) % 2 == 0 else (28, 28, 30)
                if y == 1 and x in (3, 4):
                    col = glow
            elif p == "cross":
                if (x in (3, 4) and 2 <= y <= 7) or (1 <= x <= 6 and y in (4, 5)):
                    col = acc
            elif p == "belly":
                if (2 <= x <= 5 and 2 <= y <= 10) or (x in (1, 6) and 4 <= y <= 8):
                    col = gloss(acc, f, x, y, fw, fh)
            elif p == "stripe":
                if x in (3, 4):
                    col = shade(acc, 1.0)
                if y == 2 and x in (3, 4):
                    col = glow
            elif p == "royal":
                if y == 0 or x in (3, 4):
                    col = shade(acc, 1.05)
                if (x, y) in ((3, 4), (4, 4), (3, 5), (4, 5)):
                    col = glow
                if y == 10:
                    col = shade(acc, 0.85)
        elif f == "back":
            if p in ("drone", "plates", "royal") and x in (3, 4) and 1 <= y <= 2:
                col = glow
            if p == "stripe" and x in (3, 4):
                col = shade(acc, 0.9)
            if p == "plates" and y in (4, 8):
                col = shade(base, 0.5) if sum(base) > 120 else mix(base, (90, 94, 110), 0.5)
            if p == "hazard" and 4 <= y <= 7:
                col = acc if ((x + y) // 2) % 2 == 0 else (28, 28, 30)
        return col

    return fn


TECH = ("drone", "plates", "stripe", "hazard", "cross", "royal", "veins")


def arm_fn(m):
    base, acc, glow = (rgb(v) for v in m["pal"])
    p = m["pattern"]
    limb = acc if p == "limbs" else base

    def fn(f, x, y, fw, fh):
        if p == "coat":
            if f == "bottom":
                return shade(base, 0.8)
            col = gloss(COAT, f, x, y, fw, fh)
            if f != "top":
                if y == 9:
                    col = COAT_SHADE
                elif y >= 10:
                    col = gloss(base, f, x, y, fw, fh)
            return col
        col = pattern(m, "arm", f, x, y, fw, fh, gloss(limb, f, x, y, fw, fh))
        if f in ("front", "back", "west", "east"):
            if p in TECH:
                if y == 8:
                    col = shade(acc, 1.0)
                elif y >= 9:
                    col = shade(limb, 0.6) if sum(limb) > 120 else mix(limb, (80, 84, 100), 0.4)
                if p == "royal" and y == 1:
                    col = acc
                if p == "hazard" and 3 <= y <= 4:
                    col = acc if (x + y) % 2 == 0 else (28, 28, 30)
            elif p in ("belly", "plain", "spots") and y >= 10:
                col = gloss(acc, f, x, y, fw, fh)
        return col

    return fn


def leg_fn(m):
    base, acc, glow = (rgb(v) for v in m["pal"])
    p = m["pattern"]
    limb = acc if p == "limbs" else base

    def fn(f, x, y, fw, fh):
        if p == "coat":
            if f == "top":
                return COAT
            if f == "bottom":
                return SHOES
            if y <= 2:
                return gloss(COAT, f, x, y, fw, fh)
            if y >= 10:
                return SHOES
            return shade(PANTS, 1.1 - 0.02 * y)
        col = pattern(m, "leg", f, x, y, fw, fh, gloss(limb, f, x, y, fw, fh))
        if f in ("front", "back", "west", "east"):
            if p in TECH:
                if y == 7:
                    col = shade(acc, 1.0)
                elif y >= 8:
                    col = shade(limb, 0.55) if sum(limb) > 120 else mix(limb, (80, 84, 100), 0.45)
            elif p in ("belly", "plain", "spots") and y >= 10:
                col = gloss(acc, f, x, y, fw, fh)
        return col

    return fn


# ------------------------------------------------------------------ extras (ears, tails, ...)
def paint_extras(img, m):
    base, acc, glow = (rgb(v) for v in m["pal"])
    sci = m["scientist"]
    p = m["pattern"]

    def plain(c):
        return lambda f, x, y, fw, fh: gloss(c, f, x, y, fw, fh)

    def ear(inner):
        def fn(f, x, y, fw, fh):
            col = gloss(base, f, x, y, fw, fh)
            if f == "front" and 0 < x < fw - 1 and y >= 1:
                col = inner
            return col
        return fn

    inner = INNER_EAR if m["face"] == "animal" else shade(acc, 1.1)
    paint_box(img, 0, 32, 3, 3, 1, ear(inner))        # ears_pointy R
    paint_box(img, 8, 32, 3, 3, 1, ear(inner))        # ears_pointy L

    def long_ear(side):
        def fn(f, x, y, fw, fh):
            col = gloss(base, f, x, y, fw, fh)
            if f == "front" and x == side and 1 <= y <= 5:
                col = INNER_EAR
            return col
        return fn
    paint_box(img, 16, 32, 2, 7, 1, long_ear(1))
    paint_box(img, 22, 32, 2, 7, 1, long_ear(0))
    paint_box(img, 0, 36, 3, 3, 1, ear(shade(acc, 1.0)))   # ears_round R
    paint_box(img, 8, 36, 3, 3, 1, ear(shade(acc, 1.0)))   # ears_round L

    horn = rgb(m["pal"][1]) if p == "royal" else (glow if p == "crystal" else BONE)

    def horn_fn(f, x, y, fw, fh):
        return shade(horn, 1.1 - 0.12 * y)
    paint_box(img, 28, 32, 1, 3, 1, horn_fn)
    paint_box(img, 32, 32, 1, 3, 1, horn_fn)

    muzzle = acc if m["face"] == "animal" else base

    def snout_fn(f, x, y, fw, fh):
        col = gloss(muzzle, f, x, y, fw, fh)
        if f == "front" and y == 0 and x in (1, 2):
            col = (28, 22, 28)
        if f == "front" and y == 2 and x in (1, 2):
            col = shade(muzzle, 0.6)
        if f == "top":
            col = gloss(mix(base, muzzle, 0.5), f, x, y, fw, fh)
        return col
    paint_box(img, 36, 32, 4, 3, 2, snout_fn)
    paint_box(img, 48, 32, 2, 2, 3, lambda f, x, y, fw, fh: shade(BEAKC, 0.7 if f == "bottom" else (1.1 if f == "top" else 0.95)))

    def visor_fn(f, x, y, fw, fh):
        if sci:  # lab goggles
            frame = (80, 84, 92)
            if f == "front":
                if y == 1 and (1 <= x <= 3 or 5 <= x <= 7):
                    return mix(glow, WHITE, 0.25 if x in (1, 5) else 0.0)
                return frame
            return shade(frame, 0.85)
        if f == "front":
            if y == 0:
                return shade(glow, 0.7)
            if y == 1:
                return mix(glow, WHITE, 0.35 if 2 <= x <= 3 else 0.12)
            return shade(glow, 0.82)
        if f in ("west", "east"):
            return shade(glow, 0.6)
        return shade(base, 0.5) if sum(base) > 120 else (60, 64, 78)
    paint_box(img, 0, 40, 9, 3, 2, visor_fn)

    def antenna_fn(f, x, y, fw, fh):
        if f == "top" or (f not in ("bottom",) and y <= 1):
            return glow
        return (128, 133, 144)
    paint_box(img, 58, 32, 1, 4, 1, antenna_fn)

    def fin_fn(f, x, y, fw, fh):
        if f == "top":
            return shade(acc, 0.95)
        return shade(base, 0.85)
    paint_box(img, 22, 40, 1, 3, 5, fin_fn)

    def tail_fn(d, tip_len, tipc, under=None):
        def fn(f, x, y, fw, fh):
            col = gloss(base, f, x, y, fw, fh)
            tip = False
            if f == "west":
                tip = x < tip_len
            elif f == "east":
                tip = x >= d - tip_len
            elif f in ("top", "bottom"):
                tip = y < tip_len
            elif f == "back":
                tip = True
            if tip:
                col = gloss(tipc, f, x, y, fw, fh)
            elif under is not None and f == "bottom":
                col = shade(under, 0.9)
            return col
        return fn
    tipc = acc
    paint_box(img, 0, 48, 3, 3, 8, tail_fn(8, 3, tipc))            # tail_bushy
    paint_box(img, 44, 37, 1, 1, 9, tail_fn(9, 2, tipc))           # tail_thin
    paint_box(img, 34, 40, 3, 3, 2, plain(shade(base, 1.12)))      # tail_short
    paint_box(img, 22, 48, 4, 3, 9, tail_fn(9, 2, shade(base, 0.7), acc))  # tail_thick

    feather = m["face"] == "beak"

    def wing_fn(f, x, y, fw, fh):
        if f in ("front", "back"):
            if feather:
                return shade(base, 1.15 if y % 2 == 0 else 0.85)
            if y == 0 or x in (0, 3):
                return gloss(base, f, x, y, fw, fh)
            return shade(acc, 0.85 - 0.03 * y)
        return shade(base, 0.9)
    paint_box(img, 48, 48, 6, 8, 1, wing_fn)


def form_texture(m):
    """Humanoid mob layout (classic 64x32 biped UVs on a 64x64 sheet + extras below)."""
    img = new(64, 64)
    paint_box(img, 0, 0, 8, 8, 8, head_fn(m))
    paint_box(img, 16, 16, 8, 12, 4, body_fn(m))
    paint_box(img, 40, 16, 4, 12, 4, arm_fn(m))
    paint_box(img, 0, 16, 4, 12, 4, leg_fn(m))
    paint_extras(img, m)
    return img


def player_texture(m):
    """Standard 64x64 player skin layout (overlay layers left empty)."""
    img = new(64, 64)
    paint_box(img, 0, 0, 8, 8, 8, head_fn(m))
    paint_box(img, 16, 16, 8, 12, 4, body_fn(m))
    paint_box(img, 40, 16, 4, 12, 4, arm_fn(m))    # right arm
    paint_box(img, 32, 48, 4, 12, 4, arm_fn(m))    # left arm
    paint_box(img, 0, 16, 4, 12, 4, leg_fn(m))     # right leg
    paint_box(img, 16, 48, 4, 12, 4, leg_fn(m))    # left leg
    return img


# ------------------------------------------------------------------ non-humanoid mobs
def beast_texture(m):
    base, acc, glow = (rgb(v) for v in m["pal"])
    img = new(64, 64)

    def body(f, x, y, fw, fh):
        col = pattern(m, "body", f, x, y, fw, fh, gloss(base, f, x, y, fw, fh))
        if f == "bottom" and m["pattern"] == "belly":
            col = shade(acc, 0.85)
        if f == "top" and x in (3, 4) and m["pattern"] == "veins":
            col = glow
        return col

    def head(f, x, y, fw, fh):
        col = pattern(m, "head", f, x, y, fw, fh, gloss(base, f, x, y, fw, fh))
        if f == "front":
            if y == 2 and x in (1, 4):
                col = glow
            if y == 1 and x in (0, 1, 4, 5):
                col = shade(base, 0.55)
            if y >= 4 and 1 <= x <= 4:
                col = gloss(acc, f, x, y, fw, fh)
        return col

    def snout(f, x, y, fw, fh):
        col = gloss(acc, f, x, y, fw, fh)
        if f == "front" and y == 0:
            col = (28, 22, 28)
        if f == "front" and y == 2:
            col = shade(acc, 0.55)
        return col

    def ear(f, x, y, fw, fh):
        col = gloss(base, f, x, y, fw, fh)
        if f == "front" and y == 1:
            col = INNER_EAR
        return col

    def leg(f, x, y, fw, fh):
        col = pattern(m, "leg", f, x, y, fw, fh, gloss(base, f, x, y, fw, fh))
        if f not in ("top", "bottom") and y >= 6:
            col = gloss(acc, f, x, y, fw, fh)
        return col

    def tail(f, x, y, fw, fh):
        col = gloss(base, f, x, y, fw, fh)
        tip = (f == "west" and x < 3) or (f == "east" and x >= 5) or (f in ("top", "bottom") and y < 3) or f == "back"
        return gloss(acc, f, x, y, fw, fh) if tip else col

    paint_box(img, 0, 0, 8, 6, 16, body)
    paint_box(img, 0, 22, 6, 6, 6, head)
    paint_box(img, 24, 22, 3, 3, 3, snout)
    paint_box(img, 36, 22, 2, 2, 1, ear)
    paint_box(img, 42, 22, 2, 2, 1, ear)
    paint_box(img, 48, 0, 3, 8, 3, leg)
    paint_box(img, 24, 28, 2, 2, 8, tail)
    return img


def blob_texture(m):
    base, acc, glow = (rgb(v) for v in m["pal"])
    img = new(64, 64)

    def body(f, x, y, fw, fh):
        col = pattern(m, "body", f, x, y, fw, fh, gloss(base, f, x, y, fw, fh))
        if f == "front":
            if x in (3, 8) and y in (3, 4):
                col = glow if y == 3 else shade(glow, 0.75)
            if y == 7 and 4 <= x <= 7:
                col = shade(base, 0.45) if sum(base) > 120 else (60, 60, 76)
        if f in ("front", "west", "east", "back") and y >= fh - 2:
            col = shade(acc, 0.9)
        if f == "top" and 2 <= x <= 4 and 2 <= y <= 3:
            col = mix(col, WHITE, 0.5)
        return col

    def nub(f, x, y, fw, fh):
        return pattern(m, "nub", f, x, y, fw, fh, gloss(shade(base, 1.08), f, x, y, fw, fh))

    paint_box(img, 0, 0, 12, 10, 12, body)
    paint_box(img, 0, 22, 4, 3, 4, nub)
    paint_box(img, 16, 22, 2, 4, 4, nub)
    return img


def hover_texture(m):
    base, acc, glow = (rgb(v) for v in m["pal"])
    img = new(64, 64)
    metal = (58, 62, 72)

    def core(f, x, y, fw, fh):
        col = gloss(base, f, x, y, fw, fh)
        if x in (0, fw - 1) or y in (0, fh - 1):
            col = shade(col, 0.72)
        if f in ("west", "east", "back") and y in (3, 4):
            col = shade(acc, 1.0)
        if f == "top" and 3 <= x <= 4 and 3 <= y <= 4:
            col = glow
        if f == "front" and 1 <= x <= 6 and 1 <= y <= 6 and (x in (1, 6) or y in (1, 6)):
            col = shade(acc, 0.9)
        return col

    def lens(f, x, y, fw, fh):
        if f == "front":
            if 1 <= x <= 2 and 1 <= y <= 2:
                return mix(glow, WHITE, 0.55 if (x, y) == (1, 1) else 0.1)
            return shade(glow, 0.65)
        return (30, 32, 38)

    def bar(f, x, y, fw, fh):
        return shade(metal, 1.15 if f == "top" else 0.9)

    def rotor(f, x, y, fw, fh):
        if f in ("top", "bottom"):
            if x == y or x == fw - 1 - y:
                return (120, 126, 138)
            return (196, 202, 212)
        return (150, 156, 168)

    def needle(f, x, y, fw, fh):
        if f == "bottom" or (f != "top" and y >= 3):
            return glow
        return (200, 206, 216)

    paint_box(img, 0, 0, 8, 8, 8, core)
    paint_box(img, 32, 0, 4, 4, 1, lens)
    paint_box(img, 0, 16, 18, 1, 2, bar)
    paint_box(img, 0, 19, 2, 1, 18, bar)
    paint_box(img, 0, 38, 6, 1, 6, rotor)
    paint_box(img, 42, 0, 1, 5, 1, needle)
    return img


# ------------------------------------------------------------------ spawn eggs
EGG_ROWS = {1: (6, 9), 2: (5, 10), 3: (4, 11), 4: (4, 11), 5: (3, 12), 6: (3, 12), 7: (2, 13),
            8: (2, 13), 9: (2, 13), 10: (2, 13), 11: (2, 13), 12: (3, 12), 13: (4, 11), 14: (5, 10)}


def egg_texture(m):
    base, acc, glow = (rgb(v) for v in m["pal"])
    if m["scientist"]:
        base, acc = COAT, rgb(m["pal"][0])
    img = new()
    for y, (x0, x1) in EGG_ROWS.items():
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1) or y in (1, 14)
            f = 1.18 - 0.035 * y - (0.04 * (x - x0))
            col = shade(base, max(0.55, f))
            if sum(base) < 180:
                col = mix(col, (86, 90, 112), 0.18)
            if hv(m["name"], "egg", x // 2, y // 2) < 0.2 and not edge:
                col = shade(acc, 1.0)
            if edge:
                col = shade(col, 0.62)
            put(img, x, y, col)
    # motif: visor band or eyes so every egg hints at its mob
    if m["face"] == "visor":
        for x in range(4, 12):
            put(img, x, 7, mix(glow, WHITE, 0.2))
            put(img, x, 8, shade(glow, 0.8))
    else:
        for x in (5, 6, 9, 10):
            put(img, x, 7, mix(glow, WHITE, 0.15))
            put(img, x, 8, shade(glow, 0.75))
    put(img, 5, 3, WHITE, 200)
    put(img, 6, 3, WHITE, 120)
    put(img, 5, 4, WHITE, 120)
    if m["arch"] == "HOVER":
        for x in (3, 4, 11, 12):
            put(img, x, 2, (196, 202, 212))
    return img


def gen_entities():
    dolls = []
    for m in C.MOBS:
        form = form_texture(m)
        save(form, "entity", "form", m["name"] + ".png")
        save(player_texture(m), "entity", "player", m["name"] + ".png")
        if m["arch"] == "BEAST":
            save(beast_texture(m), "entity", "mob", m["name"] + ".png")
        elif m["arch"] == "BLOB":
            save(blob_texture(m), "entity", "mob", m["name"] + ".png")
        elif m["arch"] == "HOVER":
            save(hover_texture(m), "entity", "mob", m["name"] + ".png")
        save(egg_texture(m), "item", "egg", m["name"] + ".png")
        # flat front-view paper doll for the preview sheet
        doll = new(16, 32, (40, 44, 52, 255))
        doll.paste(form.crop((8, 8, 16, 16)), (4, 0))
        doll.paste(form.crop((20, 20, 28, 32)), (4, 8))
        doll.paste(form.crop((44, 20, 48, 32)), (0, 8))
        doll.paste(form.crop((44, 20, 48, 32)).transpose(Image.FLIP_LEFT_RIGHT), (12, 8))
        doll.paste(form.crop((4, 20, 8, 32)), (4, 20))
        doll.paste(form.crop((4, 20, 8, 32)).transpose(Image.FLIP_LEFT_RIGHT), (8, 20))
        dolls.append(doll)
    return dolls


# ------------------------------------------------------------------ blocks
def noise(base, var, seed, w=16, h=16):
    rnd = random.Random(seed)
    img = new(w, h)
    for y in range(h):
        for x in range(w):
            d = rnd.randint(-var, var)
            put(img, x, y, tuple(clamp(c + d) for c in base))
    return img


def latex_block(c, seed):
    img = new()
    for y in range(16):
        for x in range(16):
            f = 1.14 - 0.016 * (x + y)
            col = shade(c, f)
            if sum(c) < 180:
                col = mix(col, (84, 88, 110), 0.20 * (1 - (x + y) / 30))
            if x in (0, 15) or y in (0, 15):
                col = shade(col, 0.78)
            put(img, x, y, col)
    for (x, y, t) in ((3, 2, .6), (4, 2, .6), (5, 2, .45), (3, 3, .45), (4, 3, .3), (11, 5, .3), (12, 6, .25), (2, 9, .18)):
        put(img, x, y, mix(img.getpixel((x, y))[:3], WHITE, t))
    rnd = random.Random(seed)
    for _ in range(3):
        x, y0, ln = rnd.randint(2, 13), rnd.randint(6, 10), rnd.randint(2, 4)
        for y in range(y0, min(15, y0 + ln)):
            put(img, x, y, shade(img.getpixel((x, y))[:3], 0.82))
    return img


def goo_tex(c, seed, alpha=255):
    rnd = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            f = 1.0 + 0.10 * math.sin(x * 0.9 + seed) * math.cos(y * 0.8)
            col = shade(c, f) if f <= 1 else mix(c, (120, 124, 150) if sum(c) < 180 else WHITE, f - 1)
            put(img, x, y, col, alpha)
    for _ in range(5):
        cx, cy, r = rnd.randint(2, 13), rnd.randint(2, 13), rnd.choice((1, 1, 2))
        for y in range(cy - r, cy + r + 1):
            for x in range(cx - r, cx + r + 1):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r and 0 <= x < 16 and 0 <= y < 16:
                    put(img, x, y, mix(img.getpixel((x, y))[:3], WHITE, 0.25), alpha)
        put(img, cx - (1 if r > 1 else 0), cy - (1 if r > 1 else 0), mix(c, WHITE, 0.75), alpha)
    return img


def latex_veins(img, seed, color=(22, 22, 26), blobs=4, steps=14):
    rnd = random.Random(seed)
    w, h = img.size
    for _ in range(blobs):
        x, y = rnd.randint(0, w - 1), rnd.randint(0, h - 1)
        for i in range(steps):
            put(img, x % w, y % h, color)
            if rnd.random() < 0.18:
                put(img, (x + 1) % w, y % h, mix(color, (110, 114, 140), 0.6))
            x += rnd.choice((-1, 0, 1))
            y += rnd.choice((0, 1, 1, -1))
    return img


def gen_blocks():
    out = {}
    for cid, _n, _mc, hexc in C.LATEX_COLORS:
        out[f"{cid}_latex_block"] = latex_block(rgb(hexc), zlib.crc32(cid.encode()) % 1000)
    out["black_latex_goo"] = goo_tex((26, 26, 32), 3, 225)
    out["white_latex_goo"] = goo_tex((226, 228, 236), 5, 225)
    out["black_latex_puddle"] = goo_tex((22, 22, 27), 7)
    out["white_latex_puddle"] = goo_tex((230, 232, 238), 9)

    dirt = noise((134, 96, 67), 16, 11)
    out["infected_dirt"] = latex_veins(dirt.copy(), 12)
    stone = noise((125, 125, 125), 9, 13)
    out["infected_stone"] = latex_veins(stone.copy(), 14)
    cob = noise((122, 122, 122), 14, 15)
    rnd = random.Random(16)
    for _ in range(22):
        x, y = rnd.randint(0, 15), rnd.randint(0, 15)
        for i in range(rnd.randint(2, 4)):
            put(cob, (x + i) % 16, y, (78, 78, 78))
        for i in range(rnd.randint(1, 3)):
            put(cob, x, (y + i) % 16, (86, 86, 86))
    out["infected_cobblestone"] = latex_veins(cob, 17)
    out["infected_sand"] = latex_veins(noise((219, 207, 163), 8, 18), 19, blobs=3)
    gtop = noise((96, 148, 60), 18, 20)
    out["infected_grass_top"] = latex_veins(gtop, 21, blobs=5)
    gside = dirt.copy()
    rnd = random.Random(22)
    for x in range(16):
        d = rnd.randint(2, 4)
        for y in range(d):
            put(gside, x, y, tuple(clamp(c + rnd.randint(-14, 14)) for c in (96, 148, 60)))
    out["infected_grass_side"] = latex_veins(gside, 23, blobs=3)

    log = new()
    rnd = random.Random(24)
    for x in range(16):
        b = rnd.randint(-12, 12)
        for y in range(16):
            put(log, x, y, tuple(clamp(c + b + rnd.randint(-6, 6)) for c in (102, 81, 50)))
    out["infected_log"] = latex_veins(log, 25, blobs=5, steps=18)
    logtop = new()
    for y in range(16):
        for x in range(16):
            r = max(abs(x - 7.5), abs(y - 7.5))
            col = (102, 81, 50) if r > 6.5 else ((176, 142, 86) if int(r) % 2 == 0 else (150, 118, 70))
            put(logtop, x, y, col)
    out["infected_log_top"] = latex_veins(logtop, 26, blobs=2, steps=8)
    planks = new()
    rnd = random.Random(27)
    for y in range(16):
        for x in range(16):
            col = tuple(clamp(c + rnd.randint(-7, 7)) for c in (160, 130, 80))
            if y % 4 == 3:
                col = shade(col, 0.7)
            if (x + (y // 4) * 5) % 16 == 0:
                col = shade(col, 0.78)
            put(planks, x, y, col)
    out["infected_planks"] = latex_veins(planks, 28, blobs=3)
    leaves = new()
    rnd = random.Random(29)
    for y in range(16):
        for x in range(16):
            r = rnd.random()
            if r < 0.22:
                continue
            col = (30, 30, 36) if r < 0.45 else tuple(clamp(c + rnd.randint(-22, 22)) for c in (58, 110, 48))
            put(leaves, x, y, col)
    out["infected_leaves"] = leaves

    def crystal(body, shine):
        img = new()
        d = ImageDraw.Draw(img)
        for (x0, x1, top) in ((2, 6, 6), (6, 11, 1), (10, 14, 8)):
            mid = (x0 + x1) // 2
            d.polygon([(x0, 15), (mid, top), (x1, 15)], fill=body + (255,))
            d.line([(mid, top), (x0, 15)], fill=shine + (255,))
            d.line([(mid, top + 2), (mid, 15)], fill=shade(body, 0.7) + (255,))
        return img
    out["latex_crystal_black"] = crystal((34, 32, 46), (150, 110, 230))
    out["latex_crystal_white"] = crystal((232, 238, 250), (140, 200, 255))
    out["latex_crystal_green"] = crystal((70, 200, 86), (220, 255, 150))
    sprout = new()
    d = ImageDraw.Draw(sprout)
    d.line([(8, 15), (8, 8), (6, 5)], fill=(24, 24, 30, 255))
    d.line([(8, 11), (11, 8), (12, 6)], fill=(24, 24, 30, 255))
    d.line([(8, 12), (5, 10)], fill=(24, 24, 30, 255))
    for (x, y) in ((6, 4), (12, 5), (4, 10)):
        put(sprout, x, y, (60, 62, 80))
    for (x, y) in ((7, 9), (9, 9)):
        put(sprout, x, y, (120, 124, 150))
    out["latex_sprout"] = sprout
    tend = new()
    rnd = random.Random(31)
    for x0 in (2, 5, 8, 11, 13):
        x, top = x0, rnd.randint(2, 8)
        for y in range(15, top, -1):
            put(tend, x, y, (22, 22, 28))
            if y % 3 == 0:
                put(tend, x, y, (110, 114, 140))
            if rnd.random() < 0.4:
                x = max(0, min(15, x + rnd.choice((-1, 1))))
                put(tend, x, y, (22, 22, 28))
    out["latex_tendrils"] = tend

    def ore(base_img, seed):
        rnd = random.Random(seed)
        for _ in range(6):
            cx, cy = rnd.randint(2, 13), rnd.randint(2, 13)
            for (dx, dy) in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 0)):
                if rnd.random() < 0.85:
                    put(base_img, cx + dx, cy + dy, (20, 20, 24))
            put(base_img, cx, cy, (200, 204, 224))
        return base_img
    out["latex_ore"] = ore(noise((125, 125, 125), 9, 33), 34)
    deep = noise((72, 72, 78), 7, 35)
    for y in range(0, 16, 4):
        for x in range(16):
            put(deep, x, y, shade(deep.getpixel((x, y))[:3], 0.78))
    out["deepslate_latex_ore"] = ore(deep, 36)
    raw = noise((34, 34, 40), 5, 37)
    rnd = random.Random(38)
    for _ in range(7):
        cx, cy = rnd.randint(1, 14), rnd.randint(1, 14)
        for (dx, dy, f) in ((0, 0, 1.5), (1, 0, 1.25), (0, 1, 0.7), (1, 1, 0.6), (-1, 0, 1.2)):
            px = raw.getpixel(((cx + dx) % 16, (cy + dy) % 16))[:3]
            put(raw, (cx + dx) % 16, (cy + dy) % 16, shade(px, f) if f <= 1 else mix(px, (130, 134, 160), f - 1))
    out["raw_latex_block"] = raw

    # ---- laboratory set
    def panel(base, seed, bevel=True, rivets=True):
        img = noise(base, 3, seed)
        if bevel:
            for i in range(16):
                put(img, i, 0, shade(base, 1.25)); put(img, 0, i, shade(base, 1.25))
                put(img, i, 15, shade(base, 0.78)); put(img, 15, i, shade(base, 0.78))
        if rivets:
            for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
                put(img, x, y, shade(base, 0.62))
        return img
    WALL = (228, 230, 235)
    out["lab_wall"] = panel(WALL, 40)
    striped = panel(WALL, 41)
    for x in range(16):
        for y in range(6, 10):
            put(striped, x, y, (36, 86, 200) if y != 6 else (86, 136, 236))
    out["lab_wall_striped"] = striped

    def tile(base, seed):
        img = noise(base, 3, seed)
        for i in range(16):
            for k in (0, 8):
                put(img, i, k, shade(base, 0.72)); put(img, k, i, shade(base, 0.72))
            for k in (1, 9):
                put(img, i, k, shade(base, 1.18)) if i % 8 else None
        return img
    out["lab_tile"] = tile((200, 204, 210), 42)
    out["lab_tile_dark"] = tile((70, 74, 82), 43)
    chk = new()
    for y in range(16):
        for x in range(16):
            lightq = (x // 8 + y // 8) % 2 == 0
            col = (214, 217, 222) if lightq else (58, 62, 70)
            if x % 8 == 0 or y % 8 == 0:
                col = shade(col, 0.8)
            put(chk, x, y, col)
    out["lab_tile_checker"] = chk
    ceil = panel((210, 212, 216), 44, rivets=False)
    for y in range(3, 14, 2):
        for x in range(3, 14, 2):
            put(ceil, x, y, (150, 152, 158))
    out["lab_ceiling"] = ceil

    def lamp(frame, inner, hot):
        img = new()
        for y in range(16):
            for x in range(16):
                if x in (0, 1, 14, 15) or y in (0, 1, 14, 15):
                    put(img, x, y, shade(frame, 1.1 if (x in (0,) or y in (0,)) else 0.9))
                else:
                    dd = math.hypot(x - 7.5, y - 7.5) / 8.5
                    put(img, x, y, mix(hot, inner, min(1, dd)))
        return img
    out["lab_light"] = lamp((150, 154, 162), (232, 236, 220), (255, 255, 246))
    out["lab_emergency_light"] = lamp((52, 54, 60), (190, 28, 24), (255, 150, 120))

    def glass(mesh):
        img = new()
        for y in range(16):
            for x in range(16):
                if x in (0, 15) or y in (0, 15):
                    put(img, x, y, (150, 184, 196), 255)
                else:
                    put(img, x, y, (190, 226, 236), 46)
        for (x, y) in ((3, 4), (4, 3), (5, 2), (4, 5), (5, 4), (6, 3), (10, 12), (11, 11), (12, 10)):
            put(img, x, y, (245, 252, 255), 150)
        if mesh:
            for y in range(1, 15):
                for x in range(1, 15):
                    if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                        put(img, x, y, (96, 104, 114), 215)
        return img
    out["lab_glass"] = glass(False)
    out["lab_reinforced_glass"] = glass(True)
    vent = new()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                col = (120, 126, 136)
            elif x in (1, 14) or y in (1, 14):
                col = (70, 74, 82)
            else:
                col = (98, 104, 114) if y % 3 == 2 else ((42, 44, 50) if y % 3 == 0 else (62, 66, 74))
            put(vent, x, y, col)
    out["lab_vent"] = vent
    haz = new()
    for y in range(16):
        for x in range(16):
            col = (232, 196, 46) if ((x + y) // 4) % 2 == 0 else (30, 30, 32)
            if x in (0, 15) or y in (0, 15):
                col = shade(col, 0.75)
            put(haz, x, y, col)
    out["lab_hazard_block"] = haz
    pil = new()
    for y in range(16):
        for x in range(16):
            f = (0.82, 1.12, 1.0, 0.94)[x % 4]
            col = shade(WALL, f) if f <= 1 else mix(WALL, WHITE, f - 1)
            if y in (0, 15):
                col = shade(WALL, 0.7)
            put(pil, x, y, col)
    out["lab_pillar"] = pil
    piltop = panel(WALL, 45, rivets=False)
    ImageDraw.Draw(piltop).rectangle([3, 3, 12, 12], outline=(170, 174, 182, 255))
    out["lab_pillar_top"] = piltop

    CASE = (150, 155, 165)
    def case(seed, vents=False):
        img = panel(CASE, seed)
        if vents:
            for y in range(5, 12, 2):
                for x in range(4, 12):
                    put(img, x, y, (84, 88, 98))
        return img
    comp = case(46)
    d = ImageDraw.Draw(comp)
    d.rectangle([2, 2, 13, 9], fill=(10, 22, 16, 255), outline=(60, 64, 72, 255))
    rnd = random.Random(47)
    for y in (4, 6, 8):
        for x in range(4, 4 + rnd.randint(4, 8)):
            put(comp, x, y, (64, 240, 120))
    d.rectangle([3, 11, 11, 13], fill=(46, 48, 56, 255))
    for x in range(4, 11, 2):
        put(comp, x, 12, (120, 124, 134))
    put(comp, 13, 12, (255, 60, 50))
    out["lab_computer_front"] = comp
    out["lab_computer_side"] = case(48, True)
    out["lab_computer_top"] = case(49)
    DARKC = (44, 46, 54)
    srv = panel(DARKC, 50, rivets=False)
    rnd = random.Random(51)
    for row in range(4):
        y = 2 + row * 3
        for x in range(2, 14):
            put(srv, x, y, (24, 26, 30)); put(srv, x, y + 1, (66, 70, 80))
        for x in (3, 5, 7):
            put(srv, x, y + 1, rnd.choice(((64, 240, 120), (255, 180, 40), (64, 160, 255))))
        put(srv, 12, y + 1, (140, 144, 154))
    out["lab_server_rack_front"] = srv
    out["lab_server_rack_side"] = panel(DARKC, 52)
    out["lab_server_rack_top"] = panel(DARKC, 53)
    crate = panel((188, 193, 200), 54)
    d = ImageDraw.Draw(crate)
    d.rectangle([0, 0, 15, 15], outline=(110, 114, 124, 255))
    d.rectangle([1, 1, 14, 14], outline=(140, 144, 154, 255))
    d.line([(2, 2), (13, 13)], fill=(150, 154, 164, 255))
    d.line([(13, 2), (2, 13)], fill=(150, 154, 164, 255))
    d.rectangle([5, 6, 10, 10], fill=(232, 196, 46, 255), outline=(30, 30, 32, 255))
    put(crate, 7, 7, (30, 30, 32)); put(crate, 8, 7, (30, 30, 32)); put(crate, 7, 9, (30, 30, 32)); put(crate, 8, 9, (30, 30, 32))
    out["lab_crate"] = crate
    shelf = panel(WALL, 55, rivets=False)
    d = ImageDraw.Draw(shelf)
    d.rectangle([1, 1, 14, 14], fill=(40, 44, 52, 255))
    for by in (7, 14):
        d.line([(1, by), (14, by)], fill=(170, 174, 182, 255))
    rnd = random.Random(56)
    for by in (6, 13):
        x = 2
        while x < 13:
            hgt = rnd.randint(2, 4)
            col = rnd.choice(((22, 22, 28), (70, 200, 86), (150, 80, 220), (64, 200, 220), (230, 232, 238)))
            d.rectangle([x, by - hgt + 1, x + 1, by], fill=col + (255,))
            put(shelf, x, by - hgt, (190, 220, 230))
            put(shelf, x, by - hgt + 1, mix(col, WHITE, 0.5))
            x += 3
    out["lab_shelf_front"] = shelf
    out["lab_shelf_side"] = panel(WALL, 57)
    out["lab_shelf_top"] = panel(WALL, 58)
    STEEL = (170, 176, 186)
    ttop = noise(STEEL, 3, 59)
    for y in range(0, 16, 3):
        for x in range(16):
            put(ttop, x, y, shade(STEEL, 1.12))
    ImageDraw.Draw(ttop).rectangle([0, 0, 15, 15], outline=(120, 126, 136, 255))
    out["lab_table_top"] = ttop
    tside = new(16, 16, (30, 32, 36, 255))
    d = ImageDraw.Draw(tside)
    d.rectangle([0, 0, 15, 3], fill=STEEL + (255,))
    d.line([(0, 3), (15, 3)], fill=(110, 116, 126, 255))
    d.rectangle([1, 4, 2, 15], fill=(96, 100, 110, 255))
    d.rectangle([13, 4, 14, 15], fill=(96, 100, 110, 255))
    d.rectangle([4, 5, 11, 9], fill=(70, 74, 84, 255), outline=(110, 116, 126, 255))
    put(tside, 7, 7, (200, 204, 214)); put(tside, 8, 7, (200, 204, 214))
    out["lab_table_side"] = tside

    tank = new()
    for y in range(16):
        for x in range(16):
            if x in (0, 1, 14, 15) or y in (0, 15):
                col = (120, 126, 136) if x in (0, 14) or y == 0 else (86, 90, 100)
            else:
                level = 6 + int(1.5 * math.sin(x * 0.9))
                col = (22, 22, 27) if y >= level else (66, 104, 112)
                if y == level:
                    col = (96, 100, 124)
            put(tank, x, y, col)
    for (x, y) in ((4, 3), (9, 2), (11, 4)):
        put(tank, x, y, (150, 255, 170))
    for (x, y) in ((3, 9), (3, 10), (4, 9)):
        put(tank, x, y, (110, 114, 140))
    for y in range(2, 14):
        put(tank, 12, y, mix(tank.getpixel((12, y))[:3], WHITE, 0.22))
    out["containment_tank"] = tank
    tanktop = panel((120, 126, 136), 60)
    ImageDraw.Draw(tanktop).ellipse([3, 3, 12, 12], outline=(60, 64, 72, 255), fill=(86, 90, 100, 255))
    put(tanktop, 7, 7, (150, 255, 170)); put(tanktop, 8, 8, (150, 255, 170))
    out["containment_tank_top"] = tanktop

    def machine_top(seed, glow, draw):
        img = panel((70, 74, 84), seed)
        draw(ImageDraw.Draw(img), img, glow)
        return img

    def machine_side(seed, glow, rows=(2, 3)):
        img = panel((86, 90, 100), seed)
        for x in range(1, 15):
            for y in rows:
                put(img, x, y, glow if y == rows[0] else shade(glow, 0.65))
        for y in range(7, 13, 2):
            for x in range(4, 12):
                put(img, x, y, (52, 54, 62))
        return img
    MAG = (255, 70, 200)
    out["conversion_chamber_top"] = machine_top(61, MAG, lambda d, i, g: (
        d.ellipse([2, 2, 13, 13], outline=g + (255,)), d.ellipse([5, 5, 10, 10], outline=shade(g, 0.6) + (255,)),
        d.rectangle([7, 7, 8, 8], fill=(255, 220, 250, 255))))
    side = machine_side(62, MAG)
    for (x, y) in ((7, 5), (8, 5), (6, 6), (9, 6), (7, 14), (8, 14)):
        put(side, x, y, MAG)
    out["conversion_chamber_side"] = side
    CY = (60, 230, 255)
    out["drone_charging_pad_top"] = machine_top(63, CY, lambda d, i, g: (
        d.polygon([(9, 2), (5, 8), (8, 8), (6, 13), (11, 6), (8, 6)], fill=g + (255,)),
        [put(i, x, y, g) for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14))]))
    out["drone_charging_pad_side"] = machine_side(64, CY)
    GAS = (120, 255, 90)
    gv = panel((70, 74, 84), 65)
    for y in range(3, 13):
        for x in range(3, 13):
            put(gv, x, y, (24, 26, 30) if x % 3 != 0 else (96, 100, 110))
            if x % 3 == 1 and y % 2 == 0:
                put(gv, x, y, shade(GAS, 0.75))
    out["latex_gas_vent_top"] = gv
    out["latex_gas_vent_side"] = machine_side(66, GAS, rows=(12, 13))
    BLUE = (50, 140, 255)
    dc = panel((214, 230, 242), 67)
    d = ImageDraw.Draw(dc)
    d.rectangle([6, 3, 9, 12], fill=BLUE + (255,))
    d.rectangle([3, 6, 12, 9], fill=BLUE + (255,))
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        put(dc, x, y, (120, 200, 255))
    out["decontamination_block_top"] = dc
    ds = panel(WALL, 68)
    for x in range(16):
        put(ds, x, 3, BLUE); put(ds, x, 4, shade(BLUE, 0.7))
    out["decontamination_block_side"] = ds
    out["hypno_screen_side"] = panel(DARKC, 69)
    out["hypno_screen_top"] = panel(DARKC, 70)

    # ---- animated textures (frames stacked vertically + .mcmeta)
    frames = 8
    hyp = new(16, 16 * frames)
    for fr in range(frames):
        for y in range(16):
            for x in range(16):
                if x in (0, 15) or y in (0, 15):
                    col = (30, 32, 38)
                else:
                    dx, dy = x - 7.5, y - 7.5
                    r, a = math.hypot(dx, dy), math.atan2(dy, dx)
                    v = math.sin(a * 2 + r * 1.1 - fr * (2 * math.pi / frames))
                    col = (255, 80, 220) if v > 0.25 else ((120, 30, 160) if v > -0.4 else (18, 8, 28))
                    if r < 1.2:
                        col = (255, 235, 255)
                put(hyp, x, fr * 16 + y, col)
    out["hypno_screen_front"] = hyp
    al = new(16, 32)
    for fr, (lampc, hot) in enumerate((((255, 40, 30), (255, 190, 170)), ((110, 14, 12), (150, 30, 26)))):
        for y in range(16):
            for x in range(16):
                if x in (0, 15) or y in (0, 15):
                    col = (60, 64, 72)
                elif x in (1, 14) or y in (1, 14):
                    col = (40, 42, 48)
                else:
                    dd = math.hypot(x - 7.5, y - 7.5) / 7.0
                    col = mix(hot, lampc, min(1, dd))
                put(al, x, fr * 16 + y, col)
    out["alarm_block"] = al
    core = new(16, 64)
    for fr in range(4):
        pulse = (0.35, 0.65, 1.0, 0.65)[fr]
        base_img = latex_block((26, 22, 34), 71)
        rnd = random.Random(72)
        for _ in range(5):
            x, y = rnd.randint(1, 14), rnd.randint(1, 14)
            for i in range(9):
                put(base_img, x % 16, y % 16, mix((60, 20, 90), (230, 90, 255), pulse))
                x += rnd.choice((-1, 0, 1)); y += rnd.choice((-1, 0, 1))
        for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
            put(base_img, x, y, mix((150, 60, 200), (255, 220, 255), pulse))
        core.paste(base_img, (0, fr * 16))
    out["latex_core"] = core

    for name, img in out.items():
        save(img, "block", name + ".png")
    for name, ft in (("hypno_screen_front", 2), ("alarm_block", 8), ("latex_core", 5)):
        with open(os.path.join(TEX, "block", name + ".png.mcmeta"), "w") as f:
            json.dump({"animation": {"frametime": ft}}, f)
    return out


# ------------------------------------------------------------------ items
def thick_diag(img, i0, i1, col, hi=None):
    """Diagonal bar from bottom-left to top-right, 2px thick (handheld weapons)."""
    for i in range(i0, i1 + 1):
        x, y = 1 + i, 14 - i
        put(img, x, y, col)
        put(img, x + 1, y, shade(col, 0.72))
        if hi is not None:
            put(img, x, y - 1, hi)


def gen_items():
    out = {}
    OUT = (20, 20, 24)

    # 1 stun baton
    im = new()
    thick_diag(im, 0, 3, (70, 54, 40))
    thick_diag(im, 4, 9, (40, 42, 50), (86, 90, 104))
    thick_diag(im, 10, 12, (255, 220, 60), (255, 250, 190))
    for (x, y) in ((14, 3), (12, 0), (15, 1)):
        put(im, x, y, (255, 255, 220))
    out["stun_baton"] = im
    # 2 tesla prod
    im = new()
    thick_diag(im, 0, 2, (60, 62, 72))
    thick_diag(im, 3, 9, (150, 156, 168), (210, 214, 224))
    for (x, y) in ((11, 3), (12, 2), (12, 4), (13, 3), (13, 1), (14, 2), (10, 2), (13, 5)):
        put(im, x, y, (200, 206, 216))
    for (x, y) in ((14, 0), (15, 3), (12, 0), (15, 1), (14, 4)):
        put(im, x, y, (90, 230, 255))
    put(im, 13, 2, (235, 252, 255))
    out["tesla_prod"] = im
    # 3 bone saw
    im = new()
    thick_diag(im, 0, 3, (110, 76, 46), (150, 110, 70))
    for i in range(4, 13):
        x, y = 1 + i, 14 - i
        put(im, x, y - 1, (236, 240, 246)); put(im, x, y, (200, 206, 216)); put(im, x + 1, y, (160, 166, 178))
        put(im, x + 1, y + 1, (110, 116, 128) if i % 2 == 0 else (0, 0, 0, 0))
    put(im, 5, 10, (60, 62, 72)); put(im, 6, 9, (60, 62, 72))
    out["bone_saw"] = im
    # 4 latex blade
    im = new()
    thick_diag(im, 0, 2, (90, 94, 106))
    for (x, y) in ((3, 10), (4, 9), (6, 12), (5, 11), (4, 12), (3, 11), (6, 11), (5, 12)):
        put(im, x, y, (150, 70, 230))
    for i in range(4, 13):
        x, y = 1 + i, 14 - i
        put(im, x, y, (26, 26, 32)); put(im, x + 1, y, (14, 14, 18)); put(im, x, y - 1, (60, 62, 80))
        if i in (5, 6, 7, 10):
            put(im, x, y - 1, (200, 204, 230))
    put(im, 14, 1, (60, 62, 80))
    out["latex_blade"] = im

    def rect(img, x0, y0, x1, y1, col, outline=None):
        ImageDraw.Draw(img).rectangle([x0, y0, x1, y1], fill=col + (255,), outline=(outline + (255,)) if outline else None)

    # 5 syringe gun
    im = new()
    rect(im, 2, 7, 10, 9, (130, 136, 148), (60, 64, 74))
    rect(im, 10, 8, 13, 8, (200, 206, 216))
    put(im, 14, 8, (240, 244, 250)); put(im, 15, 8, (240, 244, 250))
    rect(im, 3, 10, 5, 13, (56, 58, 66), (30, 32, 38))
    put(im, 7, 10, (60, 64, 74)); put(im, 7, 11, (60, 64, 74)); put(im, 8, 10, (60, 64, 74))
    rect(im, 4, 3, 8, 6, (180, 220, 232), (90, 110, 120))
    rect(im, 5, 5, 7, 5, (22, 22, 28)); rect(im, 5, 4, 6, 4, (22, 22, 28))
    put(im, 7, 4, (245, 252, 255))
    out["syringe_gun"] = im
    # 6 tranq rifle
    im = new()
    rect(im, 0, 8, 3, 10, (120, 84, 50), (70, 48, 28))
    rect(im, 3, 7, 9, 8, (58, 62, 72), (30, 32, 38))
    rect(im, 9, 7, 14, 7, (150, 156, 168))
    put(im, 15, 7, (90, 240, 120))
    rect(im, 5, 5, 8, 5, (30, 32, 38)); put(im, 6, 6, (30, 32, 38)); put(im, 9, 5, (120, 200, 255))
    rect(im, 5, 9, 6, 11, (70, 48, 28)); put(im, 7, 9, (30, 32, 38))
    out["tranq_rifle"] = im
    # 7 goo launcher
    im = new()
    rect(im, 3, 5, 12, 9, (78, 82, 94), (34, 36, 42))
    rect(im, 12, 4, 14, 10, (150, 156, 168), (60, 64, 74))
    rect(im, 13, 6, 13, 8, (16, 16, 20))
    rect(im, 5, 10, 8, 12, (22, 22, 28), (50, 52, 66)); put(im, 6, 11, (120, 124, 150))
    rect(im, 10, 10, 11, 13, (50, 52, 60), (30, 32, 38))
    rect(im, 4, 6, 10, 6, (120, 126, 138))
    put(im, 15, 9, (22, 22, 28)); put(im, 15, 10, (22, 22, 28)); put(im, 14, 11, (22, 22, 28))
    out["goo_launcher"] = im
    # 8 purifier flamethrower
    im = new()
    rect(im, 1, 6, 4, 11, (200, 46, 40), (110, 20, 18)); put(im, 2, 7, (255, 150, 140))
    rect(im, 4, 7, 10, 8, (120, 126, 138), (60, 64, 74))
    rect(im, 10, 7, 12, 7, (190, 196, 206))
    rect(im, 6, 9, 7, 12, (50, 52, 60), (30, 32, 38))
    for (x, y, c) in ((13, 7, (255, 240, 120)), (14, 7, (255, 200, 60)), (15, 7, (255, 120, 30)), (13, 6, (255, 170, 40)),
                      (14, 6, (255, 120, 30)), (13, 8, (255, 170, 40)), (14, 8, (255, 120, 30)), (15, 5, (230, 70, 20)),
                      (14, 5, (255, 140, 30)), (15, 9, (230, 70, 20)), (14, 9, (240, 100, 24))):
        put(im, x, y, c)
    out["purifier_flamethrower"] = im
    # 9 hypno ray
    im = new()
    rect(im, 2, 7, 8, 9, (122, 53, 201), (60, 24, 110))
    rect(im, 3, 10, 5, 13, (60, 44, 80), (34, 24, 48))
    put(im, 4, 8, (220, 170, 255)); put(im, 6, 8, (220, 170, 255))
    d = ImageDraw.Draw(im)
    d.ellipse([9, 3, 15, 13], outline=(255, 90, 220, 255), fill=(40, 14, 60, 255))
    d.ellipse([11, 6, 13, 10], outline=(255, 190, 245, 255))
    put(im, 12, 8, (255, 255, 255))
    out["hypno_ray"] = im

    def dart(col, tipc=(220, 224, 232)):
        im = new()
        for i in range(3, 9):
            put(im, 2 + i, 13 - i, col); put(im, 3 + i, 13 - i, shade(col, 0.7))
        for i in range(9, 12):
            put(im, 2 + i, 13 - i, tipc)
        for (x, y) in ((3, 10), (4, 9), (3, 12), (4, 12), (5, 12), (6, 11), (3, 11), (5, 13)):
            put(im, x, y, (200, 60, 70))
        put(im, 7, 7, mix(col, WHITE, 0.6))
        return im
    out["latex_dart"] = dart((28, 28, 34))
    out["tranq_dart"] = dart((70, 210, 100))
    out["antidote_dart"] = dart((70, 210, 240))

    im = new()
    rect(im, 5, 3, 10, 13, (140, 146, 158), (60, 64, 74))
    rect(im, 6, 2, 9, 2, (90, 94, 104)); rect(im, 7, 1, 8, 1, (200, 60, 50))
    rect(im, 6, 6, 9, 10, (22, 22, 28), (50, 52, 66)); put(im, 7, 7, (120, 124, 150))
    put(im, 6, 4, (220, 224, 232))
    out["goo_canister"] = im

    def syringe(fill):
        im = new()
        for i in range(0, 3):                      # needle
            put(im, 12 + i, 3 - i, (226, 230, 238))
        for i in range(0, 6):                      # barrel
            x, y = 6 + i, 9 - i
            put(im, x, y, fill); put(im, x + 1, y, shade(fill, 0.75)); put(im, x, y - 1, (210, 236, 244)); put(im, x + 1, y + 1, (120, 140, 150))
        for (x, y) in ((4, 11), (5, 10), (3, 12)):  # plunger
            put(im, x, y, (150, 156, 168))
        for (x, y) in ((2, 11), (3, 13), (2, 12), (4, 13), (2, 13)):
            put(im, x, y, (90, 94, 104))
        put(im, 8, 6, mix(fill, WHITE, 0.6))
        return im
    out["latex_syringe"] = syringe((24, 24, 30))
    out["antidote_syringe"] = syringe((70, 215, 245))

    im = new()
    d = ImageDraw.Draw(im)
    d.polygon([(6, 5), (9, 5), (12, 13), (3, 13)], fill=(190, 226, 236, 255), outline=(90, 110, 120, 255))
    d.polygon([(6, 9), (9, 9), (11, 12), (4, 12)], fill=(255, 205, 60, 255))
    rect(im, 6, 2, 9, 4, (190, 226, 236), (90, 110, 120)); rect(im, 6, 1, 9, 1, (150, 100, 60))
    put(im, 7, 10, (255, 245, 180)); put(im, 6, 7, (245, 252, 255))
    out["cure_serum"] = im

    im = new()
    d = ImageDraw.Draw(im)
    d.ellipse([2, 5, 13, 14], fill=(24, 24, 30, 255), outline=(10, 10, 12, 255))
    d.ellipse([6, 3, 11, 9], fill=(30, 30, 38, 255), outline=(10, 10, 12, 255))
    for (x, y, t) in ((5, 7, .7), (6, 7, .5), (5, 8, .45), (8, 5, .6), (9, 5, .35), (10, 11, .2), (4, 11, .15)):
        put(im, x, y, mix((40, 40, 50), WHITE, t))
    out["raw_latex"] = im

    im = new()
    d = ImageDraw.Draw(im)
    d.polygon([(8, 1), (14, 4), (14, 11), (8, 14), (2, 11), (2, 4)], fill=(44, 46, 56, 255), outline=(110, 116, 130, 255))
    d.ellipse([5, 5, 10, 10], fill=(50, 220, 255, 255), outline=(20, 110, 150, 255))
    put(im, 7, 7, (230, 252, 255)); put(im, 6, 6, (170, 240, 255))
    for (x, y) in ((8, 3), (4, 5), (11, 5), (4, 10), (11, 10), (8, 12)):
        put(im, x, y, (50, 220, 255))
    out["drone_core"] = im

    for name, img in out.items():
        save(img, "item", name + ".png")
    return out


# ------------------------------------------------------------------ GUI overlays, effect icons, mod icon
def gen_gui():
    n = 256
    spiral = new(n, n)
    for y in range(n):
        for x in range(n):
            dx, dy = x - n / 2 + 0.5, y - n / 2 + 0.5
            r, a = math.hypot(dx, dy), math.atan2(dy, dx)
            v = math.sin(a * 3 + r * 0.14)
            edge = min(1.0, r / (n * 0.5))
            if v > 0:
                col = mix((255, 110, 235), (150, 60, 255), edge)
                alpha = 60 + 120 * v
            else:
                col = (20, 0, 40)
                alpha = 30 + 90 * (-v) * edge
            alpha *= (1.0 - 0.75 * max(0.0, (r - n * 0.38) / (n * 0.12))) if r > n * 0.38 else 1.0
            if r < 10:
                col, alpha = (255, 240, 255), 220 - r * 8
            put(spiral, x, y, col, clamp(alpha))
    save(spiral, "gui", "hypnosis_spiral.png")

    vig = new(n, n)
    rnd = random.Random(5)
    phases = [(rnd.uniform(0, 6.28), rnd.randint(5, 23), rnd.uniform(0.02, 0.07)) for _ in range(7)]
    for y in range(n):
        for x in range(n):
            dx, dy = (x - n / 2 + 0.5) / (n / 2), (y - n / 2 + 0.5) / (n / 2)
            d = max(abs(dx), abs(dy)) * 0.75 + math.hypot(dx, dy) * 0.25
            a = math.atan2(dy, dx)
            wob = sum(amp * math.sin(a * k + ph) for (ph, k, amp) in phases)
            t = (d + wob - 0.52) / 0.40
            alpha = 0 if t <= 0 else min(1.0, t) ** 1.4
            col = mix((34, 30, 46), (8, 8, 12), min(1.0, max(0.0, t)))
            put(vig, x, y, col, clamp(alpha * 250))
    save(vig, "gui", "infection_vignette.png")

    drip = new(n, n)
    rnd = random.Random(9)
    cols = [0.0] * n
    v = 0.5
    for x in range(n):
        v += rnd.uniform(-0.06, 0.06)
        v = max(0.15, min(0.95, v))
        cols[x] = v
    for _ in range(6):   # smooth
        cols = [(cols[(i - 1) % n] + cols[i] + cols[(i + 1) % n]) / 3 for i in range(n)]
    for x in range(n):
        length = int(n * (0.35 + 0.65 * cols[x]))
        for y in range(n):
            if y < length:
                sheen = 0.10 * (1 + math.sin(x * 0.3)) * (1 - y / max(1, length))
                put(drip, x, y, mix((10, 10, 14), (90, 94, 120), sheen), 255)
            elif y < length + 3:
                put(drip, x, y, (60, 62, 80), 200 - 60 * (y - length))
    save(drip, "gui", "transform_drip.png")

    def icon(draw):
        img = new(18, 18)
        draw(ImageDraw.Draw(img), img)
        return img
    save(icon(lambda d, i: (d.polygon([(9, 2), (14, 10), (9, 16), (4, 10)], fill=(22, 22, 28, 255), outline=(90, 94, 120, 255)),
                            d.ellipse([4, 8, 14, 16], fill=(22, 22, 28, 255), outline=(90, 94, 120, 255)),
                            put(i, 7, 10, (220, 224, 240)), put(i, 7, 11, (150, 154, 180)), put(i, 8, 9, (150, 154, 180)))),
         "mob_effect", "latex_infection.png")
    hyp = new(18, 18)
    for y in range(18):
        for x in range(18):
            dx, dy = x - 8.5, y - 8.5
            r, a = math.hypot(dx, dy), math.atan2(dy, dx)
            if r <= 8.2:
                v = math.sin(a * 2 + r * 1.2)
                put(hyp, x, y, (255, 100, 230) if v > 0 else (70, 20, 110))
            if r < 1.2:
                put(hyp, x, y, (255, 240, 255))
    save(hyp, "mob_effect", "hypnosis.png")
    save(icon(lambda d, i: (d.polygon([(9, 1), (15, 4), (15, 10), (9, 16), (3, 10), (3, 4)], fill=(40, 150, 200, 255), outline=(200, 245, 255, 255)),
                            d.rectangle([8, 5, 9, 12], fill=(240, 252, 255, 255)), d.rectangle([5, 8, 12, 9], fill=(240, 252, 255, 255)))),
         "mob_effect", "inoculated.png")

    # mod icon: a drone face
    ic = new(128, 128, (18, 18, 24, 255))
    d = ImageDraw.Draw(ic)
    d.rounded_rectangle([20, 14, 108, 114], radius=18, fill=(26, 26, 32, 255), outline=(70, 74, 96, 255), width=3)
    d.rounded_rectangle([30, 50, 98, 72], radius=6, fill=(20, 150, 190, 255), outline=(10, 60, 80, 255), width=2)
    d.rounded_rectangle([34, 54, 94, 62], radius=3, fill=(60, 230, 255, 255))
    d.rectangle([40, 55, 58, 57], fill=(220, 250, 255, 255))
    d.rectangle([60, 6, 67, 16], fill=(128, 133, 144, 255))
    d.ellipse([58, 0, 69, 10], fill=(60, 230, 255, 255))
    d.line([(34, 24), (34, 44)], fill=(110, 114, 140, 255), width=4)
    d.rectangle([52, 86, 76, 90], fill=(110, 114, 140, 255))
    for i, x in enumerate(range(44, 86, 6)):
        d.rectangle([x, 96, x + (3 if i % 2 else 1), 106], fill=(107, 111, 120, 255))
    os.makedirs(ASSETS, exist_ok=True)
    ic.save(os.path.join(ASSETS, "icon.png"))


# ------------------------------------------------------------------ preview sheets
def sheet(images, cols, scale, name, bg=(40, 44, 52, 255)):
    if not images:
        return
    w = max(i.width for i in images)
    h = max(i.height for i in images)
    rows = (len(images) + cols - 1) // cols
    out = Image.new("RGBA", (cols * (w + 2) * scale, rows * (h + 2) * scale), bg)
    for idx, im in enumerate(images):
        big = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
        out.alpha_composite(big, ((idx % cols) * (w + 2) * scale + scale, (idx // cols) * (h + 2) * scale + scale))
    os.makedirs(PREVIEW, exist_ok=True)
    out.save(os.path.join(PREVIEW, name))


def main():
    dolls = gen_entities()
    blocks = gen_blocks()
    items = gen_items()
    gen_gui()
    sheet(dolls, 20, 5, "mobs.png")
    sheet([b.crop((0, 0, 16, 16)) for b in blocks.values()], 12, 6, "blocks.png")
    sheet(list(items.values()), 10, 8, "items.png")
    eggs = [Image.open(os.path.join(TEX, "item", "egg", m["name"] + ".png")) for m in C.MOBS]
    sheet(eggs, 20, 5, "eggs.png")
    n = sum(len(f) for _, _, f in os.walk(TEX))
    print("textures written:", n, "| block:", len(blocks), "| item:", len(items))


if __name__ == "__main__":
    main()
