"""Generates textures, models, blockstates, lang, loot tables, recipes, tags and the
generated Java model-data class for the SCP Foundation mod.

Run from the project root:  python3 tools/gen_assets.py
All artwork is procedurally drawn, original pixel art.
"""
import json
import math
import os
import random
import zlib
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODID = "scpfoundation"
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", MODID)
DATA = os.path.join(RES, "data", MODID)
JAVA_GEN = os.path.join(ROOT, "src", "main", "java", "net", "scpfoundation", "client", "model", "ScpModelData.java")


def p(*parts):
    path = os.path.join(*parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    return path


def write_json(path, obj):
    with open(p(path), "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)


# --------------------------------------------------------------------------------------
# Pixel helpers
# --------------------------------------------------------------------------------------

def clamp(v): return max(0, min(255, int(v)))


def jitter(c, amt, rng):
    d = rng.randint(-amt, amt)
    if len(c) == 4:
        return (clamp(c[0] + d), clamp(c[1] + d), clamp(c[2] + d), c[3])
    return (clamp(c[0] + d), clamp(c[1] + d), clamp(c[2] + d), 255)


def shade(c, f):
    a = c[3] if len(c) == 4 else 255
    return (clamp(c[0] * f), clamp(c[1] * f), clamp(c[2] * f), a)


def fill_rect(img, x, y, w, h, color, noise=0, rng=None):
    rng = rng or random.Random(1)
    for i in range(int(x), int(x + w)):
        for j in range(int(y), int(y + h)):
            if 0 <= i < img.width and 0 <= j < img.height:
                img.putpixel((i, j), jitter(color, noise, rng) if noise else (tuple(color) + (255,))[:4] if len(color) == 3 else tuple(color))


def px(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tuple(color) if len(color) == 4 else tuple(color) + (255,))


def new16(color=None, noise=0, seed=1):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    if color:
        fill_rect(img, 0, 0, 16, 16, color, noise, random.Random(seed))
    return img


def save_png(img, *parts):
    img.save(p(*parts))


# --------------------------------------------------------------------------------------
# Block textures
# --------------------------------------------------------------------------------------
BT = os.path.join(ASSETS, "textures", "block")
IT = os.path.join(ASSETS, "textures", "item")
ET = os.path.join(ASSETS, "textures", "entity")

READER_COLORS = {1: (60, 200, 80), 2: (230, 210, 40), 3: (240, 140, 30), 4: (220, 40, 40), 5: (170, 60, 220)}


def block_textures():
    rng = random.Random(42)
    # reinforced concrete
    img = new16((128, 128, 130), 9, 2)
    for _ in range(10):
        px(img, rng.randrange(16), rng.randrange(16), (95, 95, 98))
    for i in range(16):
        px(img, i, 0, (105, 105, 108)); px(img, 0, i, (105, 105, 108))
    for x, y in [(3, 3), (12, 3), (3, 12), (12, 12)]:
        px(img, x, y, (80, 80, 84))
    save_png(img, BT, "reinforced_concrete.png")

    # facility wall: white panels
    img = new16((226, 226, 220), 4, 3)
    for i in range(16):
        px(img, i, 0, (190, 190, 186)); px(img, i, 15, (175, 175, 170))
        px(img, 0, i, (200, 200, 196)); px(img, 15, i, (185, 185, 180))
    for x, y in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        px(img, x, y, (150, 150, 150))
    for i in range(1, 15):
        px(img, i, 7, (210, 210, 205))
    save_png(img, BT, "facility_wall.png")

    # facility floor: 2x2 tiles
    img = new16()
    r2 = random.Random(5)
    for tx in range(2):
        for ty in range(2):
            col = (172, 172, 168) if (tx + ty) % 2 == 0 else (150, 152, 150)
            fill_rect(img, tx * 8, ty * 8, 8, 8, col, 5, r2)
    for i in range(16):
        px(img, i, 0, (110, 110, 110)); px(img, i, 8, (110, 110, 110))
        px(img, 0, i, (110, 110, 110)); px(img, 8, i, (110, 110, 110))
    save_png(img, BT, "facility_floor.png")

    # hazard stripes
    img = new16()
    for x in range(16):
        for y in range(16):
            band = ((x + y) // 4) % 2
            px(img, x, y, jitter((235, 195, 25) if band == 0 else (30, 30, 30), 6, rng))
    save_png(img, BT, "hazard_block.png")

    # containment glass (translucent)
    img = Image.new("RGBA", (16, 16), (205, 232, 250, 55))
    for i in range(16):
        for c in [(i, 0), (i, 15), (0, i), (15, i)]:
            px(img, c[0], c[1], (150, 175, 190, 255))
    for i in range(3, 8):
        px(img, i, 11 - i, (240, 250, 255, 150))
    for i in range(9, 12):
        px(img, i, 22 - i - 4, (240, 250, 255, 120))
    save_png(img, BT, "containment_glass.png")

    # foundation emblem (original design: ring + three inward chevrons + core)
    img = new16((232, 232, 228), 3, 7)
    cx = cy = 7.5
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - cx, y - cy)
            if 5.6 <= d <= 7.0:
                px(img, x, y, (25, 25, 28))
            if d <= 1.6:
                px(img, x, y, (25, 25, 28))
    for ang in (90, 210, 330):
        a = math.radians(ang)
        for t in range(2, 5):
            x = cx + math.cos(a) * t
            y = cy - math.sin(a) * t
            px(img, int(round(x)), int(round(y)), (25, 25, 28))
    save_png(img, BT, "foundation_emblem.png")

    # corroded block (pocket dimension)
    img = new16((42, 28, 22), 8, 9)
    r3 = random.Random(11)
    for _ in range(18):
        x, y = r3.randrange(16), r3.randrange(16)
        for k in range(r3.randint(1, 4)):
            px(img, x, (y + k) % 16, (18, 12, 9))
    for _ in range(6):
        px(img, r3.randrange(16), r3.randrange(16), (60, 72, 40))
    save_png(img, BT, "corroded_block.png")

    # keycard readers
    side = new16((70, 70, 76), 5, 12)
    save_png(side, BT, "keycard_reader_side.png")
    for lvl, col in READER_COLORS.items():
        img = new16((52, 52, 58), 4, 20 + lvl)
        fill_rect(img, 2, 2, 12, 12, (38, 38, 42))
        fill_rect(img, 4, 9, 8, 2, (10, 10, 10))
        fill_rect(img, 11, 3, 2, 2, col)
        for i in range(lvl):
            px(img, 3 + i * 2, 4, col)
        for i in range(2, 14):
            px(img, i, 13, shade(col, 0.6))
        save_png(img, BT, f"keycard_reader_{lvl}_front.png")

    # SCP-914
    brass = (160, 118, 52)
    img = new16(brass, 8, 30)
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 6:
                px(img, x, y, jitter((220, 210, 180), 5, rng))
            if 5.2 <= d <= 6.2:
                px(img, x, y, (70, 50, 20))
    for ang in (150, 115, 90, 65, 30):
        a = math.radians(ang)
        px(img, int(round(7.5 + math.cos(a) * 4.5)), int(round(7.5 - math.sin(a) * 4.5)), (40, 30, 15))
    for t in range(0, 4):
        px(img, int(round(7.5 + t * 0.6)), int(round(7.5 - t * 0.8)), (150, 20, 20))
    px(img, 7, 7, (30, 30, 30)); px(img, 8, 8, (30, 30, 30))
    save_png(img, BT, "scp_914_front.png")
    img = new16((130, 95, 42), 8, 31)
    for x, y in [(1, 1), (14, 1), (1, 14), (14, 14), (7, 1), (7, 14)]:
        px(img, x, y, (210, 180, 110))
    for i in range(16):
        px(img, i, 5, (100, 72, 30)); px(img, i, 10, (100, 72, 30))
    save_png(img, BT, "scp_914_side.png")
    img = new16((120, 88, 40), 8, 32)
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 3 <= d <= 4 or (d <= 6.5 and (int(math.degrees(math.atan2(y - 7.5, x - 7.5))) // 30) % 2 == 0 and d > 4):
                px(img, x, y, (185, 150, 80))
    save_png(img, BT, "scp_914_top.png")

    # SCP-294
    img = new16((205, 205, 200), 4, 40)
    fill_rect(img, 2, 2, 7, 5, (30, 60, 40))  # screen
    for i in range(3, 8):
        px(img, i, 4, (90, 220, 120))
    for kx in range(3):
        for ky in range(4):
            px(img, 10 + kx * 2, 2 + ky * 2, (30, 30, 30))
    fill_rect(img, 4, 9, 8, 5, (25, 25, 25))
    fill_rect(img, 6, 11, 4, 3, (235, 235, 235))
    save_png(img, BT, "scp_294_front.png")
    save_png(new16((180, 180, 176), 5, 41), BT, "scp_294_side.png")

    # SCP-330 bowl pieces
    save_png(new16((60, 42, 28), 6, 50), BT, "scp_330_stand.png")
    img = new16((235, 235, 240), 4, 51)
    r4 = random.Random(52)
    for x in range(2, 14):
        for y in range(2, 14):
            if r4.random() < 0.45:
                px(img, x, y, r4.choice([(230, 60, 90), (60, 160, 230), (250, 210, 40), (120, 220, 90), (200, 90, 230)]))
    save_png(img, BT, "scp_330_bowl_top.png")
    save_png(new16((228, 228, 235), 4, 53), BT, "scp_330_bowl_side.png")

    # SCP-458 pizza box
    img = new16((196, 160, 110), 5, 60)
    for x in range(16):
        for y in range(16):
            if math.hypot(x - 7.5, y - 7.5) <= 4.5:
                px(img, x, y, (200, 40, 35))
            if math.hypot(x - 7.5, y - 7.5) <= 3:
                px(img, x, y, (245, 230, 210))
    px(img, 7, 7, (200, 40, 35)); px(img, 8, 8, (200, 40, 35))
    save_png(img, BT, "scp_458_top.png")
    img = new16((180, 145, 98), 5, 61)
    for i in range(16):
        px(img, i, 7, (150, 118, 75))
    save_png(img, BT, "scp_458_side.png")

    # SCP-012 stand + score
    save_png(new16((70, 45, 25), 6, 80), BT, "scp_012_stand.png")
    img = new16((228, 220, 196), 4, 81)
    for y in (3, 6, 9, 12):
        for x in range(1, 15):
            px(img, x, y, (60, 55, 50))
    r5 = random.Random(82)
    for _ in range(14):
        x, y = r5.randrange(2, 14), r5.choice([2, 4, 5, 7, 8, 10, 11])
        px(img, x, y, (140, 15, 15)); px(img, x + 1, y, (120, 10, 10))
    save_png(img, BT, "scp_012_score.png")

    # SCP-1162 hole in wall
    img = new16((128, 128, 130), 9, 70)
    for x in range(16):
        for y in range(16):
            d = math.hypot((x - 7.5) * 1.0, (y - 8.0) * 1.15)
            if d <= 4.3 + random.Random(x * 31 + y).random() * 0.8:
                px(img, x, y, (8, 8, 8))
            elif d <= 5.2:
                px(img, x, y, (85, 85, 88))
    save_png(img, BT, "scp_1162_front.png")


# --------------------------------------------------------------------------------------
# Item textures
# --------------------------------------------------------------------------------------
KEYCARD_STYLE = {
    "keycard_level_1": ((235, 235, 225), (60, 200, 80)),
    "keycard_level_2": ((235, 235, 225), (230, 210, 40)),
    "keycard_level_3": ((235, 235, 225), (240, 140, 30)),
    "keycard_level_4": ((235, 235, 225), (220, 40, 40)),
    "keycard_level_5": ((235, 235, 225), (170, 60, 220)),
    "keycard_o5": ((25, 25, 28), (230, 190, 60)),
}


def item_textures():
    for name, (base, stripe) in KEYCARD_STYLE.items():
        img = new16()
        fill_rect(img, 1, 4, 14, 9, base)
        for c in [(1, 4), (14, 4), (1, 12), (14, 12)]:
            px(img, c[0], c[1], (0, 0, 0, 0))
        fill_rect(img, 2, 5, 12, 2, stripe)
        fill_rect(img, 3, 8, 3, 3, (200, 170, 60) if base[0] > 100 else (180, 180, 180))
        for i in range(7, 13):
            px(img, i, 9, shade(base, 0.55) if base[0] > 100 else (120, 120, 120))
        for i in range(7, 11):
            px(img, i, 11, shade(base, 0.55) if base[0] > 100 else (120, 120, 120))
        save_png(img, IT, f"{name}.png")

    # document
    img = new16()
    fill_rect(img, 3, 1, 10, 14, (238, 232, 212))
    for y in (3, 5, 7, 9):
        for x in range(4, 12):
            px(img, x, y, (120, 120, 125))
    fill_rect(img, 8, 11, 4, 3, (190, 30, 30))
    px(img, 9, 12, (238, 232, 212)); px(img, 10, 12, (238, 232, 212))
    for x in range(3, 13):
        px(img, x, 14, (200, 195, 175))
    save_png(img, IT, "scp_document.png")

    # SCP-500 pill
    img = new16()
    for x in range(16):
        for y in range(16):
            dx, dy = (x - 7.5) / 5.0, (y - 7.5) / 3.0
            u = (dx + dy) / 1.4142 * 1.0
            v = (dy - dx) / 1.4142 * 1.9
            if u * u + v * v <= 1.0:
                px(img, x, y, (200, 25, 30) if u < 0.05 else (225, 50, 50))
    px(img, 5, 6, (255, 170, 170)); px(img, 6, 6, (255, 170, 170))
    save_png(img, IT, "scp_500.png")

    # SCP-207 cola bottle
    img = new16()
    fill_rect(img, 7, 1, 2, 4, (70, 35, 20))
    fill_rect(img, 6, 5, 4, 2, (80, 40, 22))
    fill_rect(img, 5, 7, 6, 8, (90, 45, 25))
    fill_rect(img, 5, 9, 6, 3, (200, 30, 35))
    for x in range(6, 10):
        px(img, x, 10, (245, 245, 245))
    px(img, 6, 7, (160, 110, 80))
    fill_rect(img, 7, 0, 2, 1, (180, 180, 180))
    save_png(img, IT, "scp_207.png")

    # SCP-018 super ball
    img = new16()
    for x in range(16):
        for y in range(16):
            if math.hypot(x - 7.5, y - 7.5) <= 5.2:
                px(img, x, y, (220, 30, 30))
            if math.hypot(x - 7.5, y - 7.5) <= 5.2 and x + y > 18:
                px(img, x, y, (170, 20, 20))
    px(img, 5, 5, (255, 180, 180)); px(img, 6, 5, (255, 200, 200)); px(img, 5, 6, (255, 200, 200))
    save_png(img, IT, "scp_018.png")

    # SCP-005 skeleton key
    img = new16()
    gold = (210, 170, 70)
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 4, y - 4)
            if 1.5 <= d <= 3.2:
                px(img, x, y, gold)
    for t in range(6, 15):
        px(img, t, t, gold); px(img, t - 1, t, shade(gold, 0.75))
    px(img, 12, 14, gold); px(img, 13, 13, gold); px(img, 11, 13, gold); px(img, 14, 11, gold); px(img, 13, 12, gold)
    save_png(img, IT, "scp_005.png")

    # SCP-427 locket
    img = new16()
    for i in range(3, 13):
        px(img, i, 2 + abs(i - 8) // 2, (200, 170, 80))
    for x in range(16):
        for y in range(16):
            dx, dy = (x - 7.5) / 3.6, (y - 10) / 4.2
            if dx * dx + dy * dy <= 1:
                px(img, x, y, (190, 150, 60))
            if dx * dx + dy * dy <= 0.45:
                px(img, x, y, (120, 30, 60))
    px(img, 6, 8, (255, 230, 150))
    save_png(img, IT, "scp_427.png")

    # SCP-714 jaded ring
    img = new16()
    for x in range(16):
        for y in range(16):
            dx, dy = (x - 7.5) / 5.5, (y - 8) / 4.5
            dd = dx * dx + dy * dy
            if 0.42 <= dd <= 1.0:
                px(img, x, y, (40, 150, 90) if y < 8 else (25, 110, 65))
    px(img, 4, 5, (150, 240, 190)); px(img, 5, 4, (150, 240, 190))
    save_png(img, IT, "scp_714.png")

    # SCP-268 cap
    img = new16()
    tweed = (110, 105, 95)
    r = random.Random(268)
    for x in range(2, 14):
        for y in range(5, 11):
            if not (y == 5 and (x < 4 or x > 11)):
                px(img, x, y, jitter(tweed, 12, r))
    fill_rect(img, 0, 10, 9, 2, (85, 80, 72))
    fill_rect(img, 2, 11, 12, 1, (70, 66, 60))
    save_png(img, IT, "scp_268.png")

    # SCP-063 toothbrush (diagonal handheld)
    img = new16()
    for t in range(2, 12):
        px(img, t, 15 - t, (50, 110, 210)); px(img, t + 1, 15 - t, (35, 80, 170))
    for t in range(11, 15):
        px(img, t, 15 - t, (240, 240, 250)); px(img, t, 14 - t, (220, 220, 235)); px(img, t - 1, 14 - t, (255, 255, 255))
    save_png(img, IT, "scp_063.png")

    # SCP-294 cup
    img = new16()
    fill_rect(img, 4, 5, 8, 9, (240, 240, 235))
    fill_rect(img, 5, 5, 6, 2, (110, 70, 40))
    fill_rect(img, 12, 7, 2, 1, (240, 240, 235)); fill_rect(img, 13, 7, 1, 4, (240, 240, 235)); fill_rect(img, 12, 10, 2, 1, (240, 240, 235))
    for x in range(4, 12):
        px(img, x, 13, (200, 200, 195))
    px(img, 7, 3, (220, 220, 220, 140)); px(img, 8, 2, (220, 220, 220, 140)); px(img, 9, 3, (220, 220, 220, 140))
    save_png(img, IT, "scp_294_cup.png")

    # SCP-330 candy
    img = new16()
    for x in range(16):
        for y in range(16):
            if math.hypot(x - 7.5, y - 7.5) <= 3.3:
                px(img, x, y, (230, 60, 110))
    for d in range(3):
        for k in range(-d, d + 1):
            px(img, 3 - d, 8 + k, (245, 160, 190)); px(img, 12 + d, 7 + k, (245, 160, 190))
    px(img, 6, 6, (255, 200, 220))
    save_png(img, IT, "scp_330_candy.png")

    # SCP-1471 phone (MalO app open: a skull-faced figure on screen)
    img = new16()
    fill_rect(img, 4, 1, 8, 14, (25, 25, 30))
    fill_rect(img, 5, 2, 6, 11, (60, 70, 80))
    fill_rect(img, 7, 5, 2, 2, (226, 221, 205)); px(img, 7, 7, (226, 221, 205)); px(img, 8, 7, (226, 221, 205))
    fill_rect(img, 6, 8, 4, 5, (10, 10, 12))
    px(img, 7, 14, (90, 90, 95)); px(img, 8, 14, (90, 90, 95))
    save_png(img, IT, "scp_1471.png")

    # SCP-1025 encyclopedia
    img = new16()
    fill_rect(img, 3, 2, 10, 12, (40, 90, 50))
    fill_rect(img, 11, 3, 2, 11, (230, 225, 205))
    fill_rect(img, 3, 2, 1, 12, (25, 60, 30))
    fill_rect(img, 5, 5, 5, 1, (220, 200, 90)); fill_rect(img, 5, 7, 4, 1, (220, 200, 90))
    px(img, 7, 10, (200, 40, 40)); px(img, 6, 11, (200, 40, 40)); px(img, 8, 11, (200, 40, 40)); px(img, 7, 12, (200, 40, 40))
    save_png(img, IT, "scp_1025.png")

    # SCP-006 water bottle
    img = new16()
    fill_rect(img, 7, 2, 2, 3, (200, 220, 230, 200))
    fill_rect(img, 7, 1, 2, 1, (120, 90, 60))
    for x in range(16):
        for y in range(16):
            if math.hypot(x - 7.5, y - 10) <= 4.3:
                px(img, x, y, (110, 200, 235) if y > 8 else (190, 230, 245))
    px(img, 6, 9, (255, 255, 255)); px(img, 5, 10, (255, 255, 255)); px(img, 10, 11, (230, 250, 255))
    save_png(img, IT, "scp_006.png")

    # SCP-008 vial
    img = new16()
    fill_rect(img, 6, 1, 4, 2, (60, 60, 65))
    fill_rect(img, 6, 3, 4, 11, (200, 225, 210, 170))
    fill_rect(img, 6, 7, 4, 7, (95, 150, 60))
    fill_rect(img, 5, 9, 6, 3, (235, 200, 40))
    px(img, 7, 10, (20, 20, 20)); px(img, 8, 10, (20, 20, 20))
    px(img, 6, 4, (255, 255, 255))
    save_png(img, IT, "scp_008.png")

    # SCP-963 amulet (original design: dark disc with a bright red core)
    img = new16()
    for i in range(4, 12):
        px(img, i, 1 + abs(i - 8) // 3, (180, 160, 100))
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 9.5)
            if d <= 4.6:
                px(img, x, y, (60, 50, 45))
            if d <= 3.2:
                px(img, x, y, (150, 120, 70))
            if d <= 1.6:
                px(img, x, y, (210, 30, 40))
    px(img, 6, 8, (255, 120, 120))
    save_png(img, IT, "scp_963.png")

    # SCP-178 3-D specs
    img = new16()
    fill_rect(img, 1, 6, 14, 1, (240, 240, 235))
    fill_rect(img, 2, 7, 5, 4, (220, 40, 40, 220))
    fill_rect(img, 9, 7, 5, 4, (40, 160, 230, 220))
    fill_rect(img, 1, 7, 1, 4, (240, 240, 235)); fill_rect(img, 7, 7, 2, 2, (240, 240, 235)); fill_rect(img, 14, 7, 1, 4, (240, 240, 235))
    fill_rect(img, 1, 11, 14, 1, (240, 240, 235))
    save_png(img, IT, "scp_178.png")

    # SCP-035 mask (original design: cracked white porcelain, half smiling half frowning, black tears)
    img = new16()
    for x in range(16):
        for y in range(16):
            dx, dy = (x - 7.5) / 6.8, (y - 7.5) / 7.6
            if dx * dx + dy * dy <= 1.0:
                px(img, x, y, (238, 234, 226))
    for (x, y) in [(4, 5), (5, 5), (4, 6), (5, 6), (10, 5), (11, 5), (10, 6), (11, 6)]:
        px(img, x, y, (10, 10, 12))
    for y in range(7, 12):
        px(img, 4, y, (30, 20, 25)); px(img, 11, y + 1 if y < 11 else y, (30, 20, 25))
    for (x, y) in [(5, 11), (6, 12), (7, 12)]:
        px(img, x, y, (40, 20, 25))
    for (x, y) in [(8, 12), (9, 11), (10, 11)]:
        px(img, x, y, (40, 20, 25))
    for (x, y) in [(8, 2), (8, 3), (9, 4), (9, 5)]:
        px(img, x, y, (150, 145, 140))
    save_png(img, IT, "scp_035.png")

    # SCP-049-2 face (zombie-player head)
    img = new16((122, 140, 118), 8, 90)
    fill_rect(img, 3, 6, 3, 2, (230, 230, 220)); fill_rect(img, 10, 6, 3, 2, (230, 230, 220))
    fill_rect(img, 4, 11, 8, 2, (60, 30, 30))
    for y in range(2, 14):
        px(img, 8, y, (120, 30, 30))
    save_png(img, IT, "scp_049_2_face.png")
    save_png(new16((112, 130, 108), 10, 91), IT, "scp_049_2_skin.png")

    # SCP-008 infection effect icon (18x18)
    icon = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    for x in range(18):
        for y in range(18):
            d = math.hypot(x - 8.5, y - 8.5)
            if d <= 3.5:
                px(icon, x, y, (95, 150, 60))
    for ang in range(0, 360, 45):
        a = math.radians(ang)
        for t in range(4, 8):
            px(icon, int(round(8.5 + math.cos(a) * t)), int(round(8.5 + math.sin(a) * t)), (70, 120, 45))
    icon.save(p(ASSETS, "textures", "mob_effect", "scp_008_infection.png"))

    # SCP-458 pizza slice
    img = new16()
    for y in range(2, 14):
        half = (y - 2) * 0.55
        for x in range(int(8 - half), int(8 + half) + 1):
            px(img, x, y, (240, 190, 80))
    for x in range(1, 15):
        px(img, x, 13, (190, 120, 50)); px(img, x, 14, (170, 100, 40))
    for c in [(7, 6), (6, 10), (9, 9), (10, 11), (5, 12)]:
        px(img, c[0], c[1], (200, 40, 35)); px(img, c[0] + 1, c[1], (200, 40, 35))
    save_png(img, IT, "scp_458_pizza.png")


# --------------------------------------------------------------------------------------
# Entity models (generated Java) + entity textures
# --------------------------------------------------------------------------------------
# A box-UV face map for a cuboid with uv (u,v) and size (w,h,d)
def faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


def paint_box(img, uv, size, color, noise, rng, top_f=1.12, bottom_f=0.8, side_f=0.92):
    u, v = uv
    w, h, d = size
    for name, (x, y, fw, fh) in faces(u, v, w, h, d).items():
        f = {"top": top_f, "bottom": bottom_f, "front": 1.0, "back": side_f, "left": side_f, "right": side_f}[name]
        fill_rect(img, x, y, fw, fh, shade(color, f), noise, rng)


# Each beast model: parts = list of dict(name, pivot, rot, cubes=[(x,y,z,w,h,d, key)], children=[...])
# key = tag used by painters to add features.
BEASTS = {
    "scp_682": {
        "color": (92, 84, 52), "noise": 14,
        "parts": [
            dict(name="body", pivot=(0, 10, 0), cubes=[(-7, -6, -14, 14, 12, 28, "body"),
                                                       (-1, -9, -10, 2, 3, 4, "spike"), (-1, -9, -2, 2, 3, 4, "spike"), (-1, -9, 6, 2, 3, 4, "spike")]),
            dict(name="head", pivot=(0, 8, -14), cubes=[(-5, -5, -10, 10, 8, 10, "head"), (-4, -2, -18, 8, 5, 8, "snout")]),
            dict(name="leg_a1", pivot=(6, 14, -9), cubes=[(-3, 0, -3, 6, 10, 6, "leg")]),
            dict(name="leg_b1", pivot=(-6, 14, -9), cubes=[(-3, 0, -3, 6, 10, 6, "leg")]),
            dict(name="leg_b2", pivot=(6, 14, 9), cubes=[(-3, 0, -3, 6, 10, 6, "leg")]),
            dict(name="leg_a2", pivot=(-6, 14, 9), cubes=[(-3, 0, -3, 6, 10, 6, "leg")]),
            dict(name="tail1", pivot=(0, 8, 14), cubes=[(-3, -2, 0, 6, 5, 14, "tail")],
                 children=[dict(name="tail2", pivot=(0, 1, 14), cubes=[(-2, -1, 0, 4, 3, 12, "tail")])]),
        ],
    },
    "scp_939": {
        "color": (125, 24, 24), "noise": 12,
        "parts": [
            dict(name="body", pivot=(0, 9, 0), cubes=[(-4, -4, -10, 8, 8, 20, "body")]),
            dict(name="head", pivot=(0, 8, -10), cubes=[(-4, -3, -9, 8, 6, 9, "head939"), (-3, 3, -8, 6, 2, 8, "jaw939")]),
            dict(name="leg_a1", pivot=(3, 12, -7), cubes=[(-1.5, 0, -1.5, 3, 12, 3, "leg")]),
            dict(name="leg_b1", pivot=(-3, 12, -7), cubes=[(-1.5, 0, -1.5, 3, 12, 3, "leg")]),
            dict(name="leg_b2", pivot=(3, 12, 7), cubes=[(-1.5, 0, -1.5, 3, 12, 3, "leg")]),
            dict(name="leg_a2", pivot=(-3, 12, 7), cubes=[(-1.5, 0, -1.5, 3, 12, 3, "leg")]),
            dict(name="tail1", pivot=(0, 7, 10), cubes=[(-1, -1, 0, 2, 2, 12, "tail")]),
        ],
    },
    "scp_999": {
        "color": (242, 142, 30), "noise": 6,
        "parts": [
            dict(name="bob_body", pivot=(0, 24, 0), cubes=[(-7, -12, -7, 14, 12, 14, "face999"), (-5, -15, -5, 10, 3, 10, "blob")]),
        ],
    },
    "scp_131": {
        "color": (230, 120, 30), "noise": 6,
        "parts": [
            dict(name="bob_body", pivot=(0, 24, 0), cubes=[(-3, -9, -3, 6, 7, 6, "eye131"), (-2, -11, -2, 4, 2, 4, "tip"), (-2, -2, -2, 4, 2, 4, "wheel")]),
        ],
    },
    "scp_529": {
        "color": (128, 128, 130), "noise": 10,
        "parts": [
            dict(name="head", pivot=(0, 15, -6), cubes=[(-3, -2, -5, 6, 4, 5, "cathead"), (-2, -3, -3, 1, 1, 2, "ear"), (1, -3, -3, 1, 1, 2, "ear"), (-2, 0, -6, 4, 2, 1, "snout")]),
            dict(name="body", pivot=(0, 16, -2), cubes=[(-2, -3, -4, 4, 5, 6, "halfbody")]),
            dict(name="leg_a1", pivot=(1, 18, -5), cubes=[(-1, 0, -1, 2, 6, 2, "leg")]),
            dict(name="leg_b1", pivot=(-1, 18, -5), cubes=[(-1, 0, -1, 2, 6, 2, "leg")]),
        ],
    },
    "scp_066": {
        "color": (130, 30, 40), "noise": 14,
        "parts": [
            dict(name="bob_body", pivot=(0, 24, 0), cubes=[(-4, -7, -4, 8, 6, 8, "yarn"), (-3, -9, -3, 6, 2, 6, "yarn")]),
            dict(name="tail1", pivot=(0, 21, 4), cubes=[(-1, -1, 0, 2, 2, 7, "yarn")]),
            dict(name="tail2", pivot=(3, 22, -2), rot=(0, 1.2, 0), cubes=[(-1, -1, 0, 2, 2, 6, "yarn")]),
            dict(name="tail3", pivot=(-3, 22, -2), rot=(0, -1.9, 0), cubes=[(-1, -1, 0, 2, 2, 6, "yarn")]),
        ],
    },
    "scp_1048": {
        "color": (140, 95, 55), "noise": 8,
        "parts": [
            dict(name="head", pivot=(0, 13, 0), cubes=[(-3, -6, -3, 6, 6, 5, "bearhead"), (-4, -8, -1, 2, 2, 1, "ear"), (2, -8, -1, 2, 2, 1, "ear"), (-2, -3, -4, 3, 2, 1, "snout")]),
            dict(name="body", pivot=(0, 13, 0), cubes=[(-3, 0, -2, 6, 7, 4, "bearbody")]),
            dict(name="arm_a", pivot=(-4, 14, 0), cubes=[(-1, 0, -1, 2, 5, 2, "leg")]),
            dict(name="arm_b", pivot=(4, 14, 0), cubes=[(-1, 0, -1, 2, 5, 2, "leg")]),
            dict(name="leg_a1", pivot=(-2, 20, 0), cubes=[(-1, 0, -2, 2, 4, 3, "leg")]),
            dict(name="leg_b1", pivot=(2, 20, 0), cubes=[(-1, 0, -2, 2, 4, 3, "leg")]),
        ],
    },
}


BEASTS["scp_2295"] = {"color": (200, 110, 140), "noise": 6, "patchwork": True,
                      "parts": [dict(part, cubes=[c[:6] + ("patch" if c[6] not in ("ear",) else "patch",) for c in part["cubes"]])
                                for part in BEASTS["scp_1048"]["parts"]]}


def pack_uvs(model):
    """Shelf-pack every cuboid's UV footprint; returns texture size."""
    cubes = []

    def walk(parts):
        for part in parts:
            for c in part["cubes"]:
                cubes.append(c)
            walk(part.get("children", []))
    walk(model["parts"])
    for tw, th in [(64, 32), (64, 64), (128, 64), (128, 128), (256, 128)]:
        x = y = row_h = 0
        uvs = {}
        ok = True
        for c in cubes:
            w, h, d = int(math.ceil(c[3])), int(math.ceil(c[4])), int(math.ceil(c[5]))
            fw, fh = 2 * (w + d), d + h
            if x + fw > tw:
                x, y, row_h = 0, y + row_h, 0
            if y + fh > th or fw > tw:
                ok = False
                break
            uvs[id(c)] = (x, y)
            x += fw
            row_h = max(row_h, fh)
        if ok:
            return tw, th, uvs
    raise RuntimeError("model too large")


def jf(v):
    return f"{float(v)}f"


def java_part(lines, parent_var, part, uvs, counter):
    var = f"p{counter[0]}"
    counter[0] += 1
    b = "ModelPartBuilder.create()"
    for c in part["cubes"]:
        u, v = uvs[id(c)]
        b += f".uv({u}, {v}).cuboid({jf(c[0])}, {jf(c[1])}, {jf(c[2])}, {jf(c[3])}, {jf(c[4])}, {jf(c[5])})"
    px_, py_, pz_ = part["pivot"]
    rx, ry, rz = part.get("rot", (0, 0, 0))
    lines.append(f"        ModelPartData {var} = {parent_var}.addChild(\"{part['name']}\", {b}, "
                 f"ModelTransform.of({jf(px_)}, {jf(py_)}, {jf(pz_)}, {jf(rx)}, {jf(ry)}, {jf(rz)}));")
    for ch in part.get("children", []):
        java_part(lines, var, ch, uvs, counter)


def part_paths(parts, prefix=""):
    out = []
    for part in parts:
        path = prefix + part["name"]
        out.append(path)
        out += part_paths(part.get("children", []), path + "/")
    return out


def paint_features(img, key, uv, size, base, rng, variant=None):
    u, v = uv
    w, h, d = [int(math.ceil(s)) for s in size]
    F = faces(u, v, w, h, d)
    fx, fy, fw, fh = F["front"]
    if key == "head":  # 682 head: red eyes on top area
        px(img, fx + 1, fy + 1, (220, 30, 20)); px(img, fx + 2, fy + 1, (255, 80, 40))
        px(img, fx + fw - 3, fy + 1, (255, 80, 40)); px(img, fx + fw - 2, fy + 1, (220, 30, 20))
    elif key == "snout":
        for i in range(fw):
            if i % 2 == 0:
                px(img, fx + i, fy + fh - 2, (235, 230, 210))
        fill_rect(img, fx, fy + fh - 1, fw, 1, (40, 10, 10))
        for name in ("left", "right"):
            sx, sy, sw, sh = F[name]
            for i in range(sw):
                if i % 2 == 0:
                    px(img, sx + i, sy + sh - 2, (235, 230, 210))
    elif key == "body":
        bx, by, bw, bh = F["bottom"]
        fill_rect(img, bx, by, bw, bh, shade(base, 1.25), 6, rng)
    elif key in ("head939", "jaw939"):
        # no eyes; rows of teeth
        row = fy + fh - 1 if key == "head939" else fy
        for i in range(fw):
            px(img, fx + i, row, (240, 235, 220) if i % 2 == 0 else (60, 0, 0))
        if key == "head939":
            fill_rect(img, fx + 1, fy + 1, fw - 2, 1, shade(base, 0.7))
    elif key == "face999":
        fill_rect(img, fx + 3, fy + 3, 2, 3, (20, 20, 20)); fill_rect(img, fx + fw - 5, fy + 3, 2, 3, (20, 20, 20))
        px(img, fx + 3, fy + 3, (255, 255, 255)); px(img, fx + fw - 5, fy + 3, (255, 255, 255))
        for i in range(4, fw - 4):
            px(img, fx + i, fy + 8, (120, 40, 10))
        px(img, fx + 3, fy + 7, (120, 40, 10)); px(img, fx + fw - 4, fy + 7, (120, 40, 10))
    elif key == "eye131":
        fill_rect(img, fx + 1, fy + 1, 4, 4, (250, 250, 245))
        fill_rect(img, fx + 2, fy + 2, 2, 2, (20, 20, 20))
    elif key == "wheel":
        for name in F:
            x, y, ww, hh = F[name]
            fill_rect(img, x, y, ww, hh, (45, 45, 48), 5, rng)
    elif key == "cathead":
        px(img, fx + 1, fy + 1, (110, 200, 80)); px(img, fx + fw - 2, fy + 1, (110, 200, 80))
        tx, ty, tw_, th_ = F["top"]
        for i in range(0, tw_, 2):
            px(img, tx + i, ty + 1, shade(base, 0.6))
    elif key == "snout" or key == "halfbody":
        pass
    if key == "halfbody":
        bx, by, bw, bh = F["back"]
        fill_rect(img, bx, by, bw, bh, shade(base, 0.5), 4, rng)  # the "missing" half reads as shadow
        tx, ty, tw_, th_ = F["top"]
        for i in range(0, th_, 2):
            for j in range(tw_):
                px(img, tx + j, ty + i, shade(base, 0.7))
    if key == "yarn":  # braided strands
        for name, (x, y, ww, hh) in F.items():
            for i in range(ww):
                for j in range(hh):
                    if (i + j) % 3 == 0:
                        px(img, x + i, y + j, shade(base, 1.35))
                    elif (i - j) % 4 == 0:
                        px(img, x + i, y + j, shade(base, 0.7))
    if key == "patch":  # sewn-together patches of different fabrics with stitches
        cols = [(200, 110, 140), (90, 155, 210), (235, 200, 90), (120, 190, 120), (230, 230, 225), (170, 120, 200)]
        for name, (x, y, ww, hh) in F.items():
            for i in range(0, ww, 3):
                for j in range(0, hh, 3):
                    fill_rect(img, x + i, y + j, min(3, ww - i), min(3, hh - j), rng.choice(cols), 6, rng)
            for i in range(ww):
                if i % 2 == 0:
                    px(img, x + i, y + hh // 2, (60, 40, 40))
        if key == "patch" and fw >= 6 and fh >= 6:
            px(img, fx + 1, fy + 2, (15, 15, 15)); px(img, fx + fw - 2, fy + 2, (15, 15, 15))
            fill_rect(img, fx + fw // 2 - 1, fy + fh - 3, 2, 2, (220, 40, 60))
    if key == "bearhead":
        px(img, fx + 1, fy + 2, (15, 15, 15)); px(img, fx + fw - 2, fy + 2, (15, 15, 15))
    if key == "bearbody":
        fill_rect(img, fx + 1, fy + 1, fw - 2, fh - 2, shade(base, 1.3), 4, rng)
    if key == "snout" and base[0] > 120 and base[1] < 120:  # bear snout
        pass


def gen_beasts():
    java_methods = []
    java_parts = []
    for name, model in BEASTS.items():
        tw, th, uvs = pack_uvs(model)
        variants = [None] if name != "scp_131" else ["a", "b"]
        for var in variants:
            rng = random.Random(zlib.crc32(name.encode()) & 0xffff)
            img = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
            base = model["color"] if var != "b" else (40, 95, 205)

            def walk(parts):
                for part in parts:
                    for c in part["cubes"]:
                        col = base
                        if c[6] == "spike":
                            col = (60, 55, 40)
                        if c[6] == "snout" and name == "scp_1048":
                            col = (205, 175, 130)
                        paint_box(img, uvs[id(c)], (int(math.ceil(c[3])), int(math.ceil(c[4])), int(math.ceil(c[5]))), col, model["noise"], rng)
                        paint_features(img, c[6], uvs[id(c)], c[3:6], col, rng, var)
                    walk(part.get("children", []))
            walk(model["parts"])
            if name == "scp_529":  # tabby stripes on body sides
                pass
            fname = name if var is None else f"{name}_{var}"
            save_png(img, ET, f"{fname}.png")
        lines = []
        counter = [0]
        for part in model["parts"]:
            java_part(lines, "root", part, uvs, counter)
        method = "scp" + name.split("_", 1)[1]
        java_methods.append(
            f"    public static TexturedModelData {method}() {{\n"
            f"        ModelData data = new ModelData();\n"
            f"        ModelPartData root = data.getRoot();\n"
            + "\n".join(lines) + "\n"
            f"        return TexturedModelData.of(data, {tw}, {th});\n"
            f"    }}\n")
        paths = ", ".join(f'"{x}"' for x in part_paths(model["parts"]))
        java_parts.append(f"    public static final String[] {name.upper()}_PARTS = {{{paths}}};\n")
    src = (
        "// GENERATED by tools/gen_assets.py - do not edit by hand.\n"
        "package net.scpfoundation.client.model;\n\n"
        "import net.minecraft.client.model.ModelData;\n"
        "import net.minecraft.client.model.ModelPartBuilder;\n"
        "import net.minecraft.client.model.ModelPartData;\n"
        "import net.minecraft.client.model.ModelTransform;\n"
        "import net.minecraft.client.model.TexturedModelData;\n\n"
        "public final class ScpModelData {\n"
        "    private ScpModelData() {}\n\n"
        + "".join(java_parts) + "\n" + "\n".join(java_methods) + "}\n")
    with open(p(JAVA_GEN), "w") as f:
        f.write(src)


# Humanoid (BipedEntityModel, 64x64; left limbs mirror the right UVs)
BIPED = {
    "head": ((0, 0), (8, 8, 8)),
    "hat": ((32, 0), (8, 8, 8)),
    "body": ((16, 16), (8, 12, 4)),
    "arm": ((40, 16), (4, 12, 4)),
    "leg": ((0, 16), (4, 12, 4)),
}


def biped(name, skin, body, arms, legs, face_fn, extra_fn=None, noise=8, seed=0):
    rng = random.Random(seed or zlib.crc32(name.encode()) & 0xfff)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    paint_box(img, *BIPED["head"], skin, noise, rng)
    paint_box(img, *BIPED["body"], body, noise, rng)
    paint_box(img, *BIPED["arm"], arms, noise, rng)
    paint_box(img, *BIPED["leg"], legs, noise, rng)
    # hand / foot colouring
    F = faces(40, 16, 4, 12, 4)
    for k in ("front", "back", "left", "right"):
        x, y, w, h = F[k]
        fill_rect(img, x, y + h - 2, w, 2, shade(skin if arms != skin else arms, 0.95), noise, rng)
    face_fn(img, rng)
    if extra_fn:
        extra_fn(img, rng)
    save_png(img, ET, f"{name}.png")


def entity_textures():
    gen_beasts()

    # SCP-173: an original design - a grey cast-concrete figure with a blank, chiselled face,
    # exposed rusty rebar and chalk containment markings.
    concrete = (148, 148, 146)

    def f173(img, rng):
        fx, fy = 8, 8
        for x in (1, 2, 5, 6):  # two narrow chiselled slits
            px(img, fx + x, fy + 3, (70, 70, 72))
        for y in range(1, 7):  # chalk containment mark down the face
            px(img, fx + 4, fy + y, (235, 235, 230))

    def x173(img, rng):
        for _ in range(14):  # rust from exposed rebar
            x, y = rng.randrange(16, 40), rng.randrange(20, 32)
            for k in range(rng.randint(1, 3)):
                px(img, x, min(31, y + k), (150, 85, 40))
        for x in range(20, 28):  # chalk band across the chest
            px(img, x, 24, (235, 235, 230))
    biped("scp_173", concrete, concrete, concrete, concrete, f173, x173, noise=12)

    # SCP-096: pale, elongated, black eyes and a gaping mouth
    pale = (226, 220, 214)

    def f096(img, rng):
        fx, fy = 8, 8
        for (x, y) in [(1, 2), (2, 2), (1, 3), (2, 3), (5, 2), (6, 2), (5, 3), (6, 3)]:
            px(img, fx + x, fy + y, (10, 10, 10))
        for y in range(5, 8):
            px(img, fx + 3, fy + y, (25, 5, 5)); px(img, fx + 4, fy + y, (25, 5, 5))
    biped("scp_096", pale, shade(pale, 0.95), pale, shade(pale, 0.93), f096, noise=5)

    # SCP-049: dark robes, pale mask with dark lenses and a beak (uv 0,32)
    robe = (28, 26, 32)
    mask = (205, 195, 172)

    def f049(img, rng):
        fx, fy = 8, 8
        fill_rect(img, 8, 8, 8, 2, robe, 4, rng)  # hood line
        for (x, y) in [(1, 3), (2, 3), (5, 3), (6, 3)]:
            px(img, fx + x, fy + y, (35, 60, 60))
        px(img, fx + 2, fy + 3, (90, 140, 140))

    def x049(img, rng):
        paint_box(img, (0, 32), (2, 2, 4), (190, 180, 158), 4, rng)
        for k in ("top", "left", "right"):
            pass
        # hood over top/back of head
        F = faces(0, 0, 8, 8, 8)
        for k in ("top", "back", "left", "right"):
            x, y, w, h = F[k]
            fill_rect(img, x, y, w, h if k == "top" else 5, robe, 4, rng)
        # gloves
        F = faces(40, 16, 4, 12, 4)
        for k in ("front", "back", "left", "right", "bottom"):
            x, y, w, h = F[k]
            fill_rect(img, x, y + (h - 3 if k != "bottom" else 0), w, 3 if k != "bottom" else h, (15, 15, 15), 2, rng)
        # robe belt
        F = faces(16, 16, 8, 12, 4)
        x, y, w, h = F["front"]
        fill_rect(img, x, y + 6, w, 1, (90, 60, 30))
    biped("scp_049", mask, robe, robe, robe, f049, x049, noise=5)

    # SCP-049-2: grey-green reanimated patient with stitched gown
    skin = (122, 140, 118)

    def f0492(img, rng):
        fx, fy = 8, 8
        px(img, fx + 2, fy + 3, (230, 230, 220)); px(img, fx + 5, fy + 3, (230, 230, 220))
        for x in range(2, 6):
            px(img, fx + x, fy + 6, (60, 30, 30))

    def x0492(img, rng):
        F = faces(16, 16, 8, 12, 4)
        x, y, w, h = F["front"]
        for yy in range(y, y + h):
            px(img, x + 4, yy, (120, 30, 30))
            if yy % 2 == 0:
                px(img, x + 3, yy, (120, 30, 30)); px(img, x + 5, yy, (120, 30, 30))
    biped("scp_049_2", skin, (150, 160, 165), skin, (80, 85, 90), f0492, x0492, noise=8)

    # SCP-106: corroded black-brown figure
    tar = (38, 30, 24)

    def f106(img, rng):
        fx, fy = 8, 8
        px(img, fx + 2, fy + 3, (200, 200, 180)); px(img, fx + 5, fy + 3, (200, 200, 180))
        for x in range(1, 7):
            px(img, fx + x, fy + 6, (170, 160, 140) if x % 2 else (10, 8, 6))

    def x106(img, rng):
        for _ in range(60):
            x, y = rng.randrange(0, 56), rng.randrange(0, 32)
            if img.getpixel((x, y))[3] > 0:
                px(img, x, y, (70, 80, 40) if rng.random() < 0.3 else (15, 10, 8))
    biped("scp_106", tar, (22, 20, 20), tar, (22, 20, 20), f106, x106, noise=7)

    # SCP-1471-A: original design - gaunt black-furred figure with a pale canine skull for a head
    fur = (18, 18, 22)
    bone = (226, 221, 205)

    def f1471(img, rng):
        fill_rect(img, 8, 8, 8, 8, bone, 6, rng)
        for (x, y) in [(1, 2), (2, 2), (1, 3), (2, 3), (5, 2), (6, 2), (5, 3), (6, 3)]:
            px(img, 8 + x, 8 + y, (8, 8, 8))
        px(img, 11, 15, (150, 140, 120)); px(img, 12, 15, (150, 140, 120))

    def x1471(img, rng):
        paint_box(img, (0, 32), (4, 3, 5), bone, 6, rng)
        F = faces(0, 32, 4, 3, 5)
        x, y, w, h = F["front"]
        px(img, x + 1, y, (20, 20, 20)); px(img, x + 2, y, (20, 20, 20))
        for i in range(w):
            px(img, x + i, y + h - 1, (240, 240, 230) if i % 2 == 0 else (40, 30, 30))
        F = faces(0, 0, 8, 8, 8)
        for k in ("top", "back", "left", "right"):
            x, y, w, h = F[k]
            fill_rect(img, x, y, w, h, bone if k == "top" else shade(bone, 0.9), 6, rng)
        for _ in range(40):
            x, y = rng.randrange(16, 56), rng.randrange(16, 32)
            if img.getpixel((x, y))[3] > 0:
                px(img, x, y, (34, 34, 40))
    biped("scp_1471_a", bone, fur, fur, fur, f1471, x1471, noise=5)

    save_png(Image.new("RGBA", (16, 16), (0, 0, 0, 0)), ET, "empty.png")


# --------------------------------------------------------------------------------------
# Models, blockstates, loot, lang, recipes, tags
# --------------------------------------------------------------------------------------
SIMPLE_BLOCKS = ["reinforced_concrete", "facility_wall", "facility_floor", "hazard_block",
                 "containment_glass", "foundation_emblem", "corroded_block"]
FACING_BLOCKS = {  # name -> (front, side, top)
    **{f"keycard_reader_{i}": (f"keycard_reader_{i}_front", "keycard_reader_side", "keycard_reader_side") for i in range(1, 6)},
    "scp_914": ("scp_914_front", "scp_914_side", "scp_914_top"),
    "scp_294": ("scp_294_front", "scp_294_side", "scp_294_side"),
    "scp_1162": ("scp_1162_front", "reinforced_concrete", "reinforced_concrete"),
}
SHAPED_BLOCKS = ["scp_330", "scp_458", "scp_012"]
ALL_BLOCKS = SIMPLE_BLOCKS + list(FACING_BLOCKS) + SHAPED_BLOCKS

GENERATED_ITEMS = ["scp_document", "keycard_level_1", "keycard_level_2", "keycard_level_3", "keycard_level_4",
                   "keycard_level_5", "keycard_o5", "scp_500", "scp_207", "scp_018", "scp_005", "scp_427",
                   "scp_714", "scp_268", "scp_294_cup", "scp_1471", "scp_1025", "scp_006", "scp_008", "scp_963", "scp_178", "scp_330_candy", "scp_458_pizza"]
HANDHELD_ITEMS = ["scp_063"]
SPAWN_EGGS = ["scp_173", "scp_096", "scp_049", "scp_049_2", "scp_106", "scp_682", "scp_939", "scp_999",
              "scp_131", "scp_529", "scp_1048", "scp_066", "scp_1471_a", "scp_2295"]

FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def models_and_states():
    bm = os.path.join(ASSETS, "models", "block")
    im = os.path.join(ASSETS, "models", "item")
    bs = os.path.join(ASSETS, "blockstates")
    for b in SIMPLE_BLOCKS:
        write_json(os.path.join(bm, f"{b}.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MODID}:block/{b}"}})
        write_json(os.path.join(bs, f"{b}.json"), {"variants": {"": {"model": f"{MODID}:block/{b}"}}})
    for b, (front, side, top) in FACING_BLOCKS.items():
        write_json(os.path.join(bm, f"{b}.json"), {"parent": "minecraft:block/orientable", "textures": {
            "front": f"{MODID}:block/{front}", "side": f"{MODID}:block/{side}", "top": f"{MODID}:block/{top}"}})
        write_json(os.path.join(bs, f"{b}.json"), {"variants": {
            f"facing={f}": ({"model": f"{MODID}:block/{b}", "y": y} if y else {"model": f"{MODID}:block/{b}"}) for f, y in FACING_Y.items()}})
    # SCP-330: stand + bowl
    write_json(os.path.join(bm, "scp_330.json"), {
        "parent": "minecraft:block/block",
        "textures": {"particle": f"{MODID}:block/scp_330_bowl_side", "stand": f"{MODID}:block/scp_330_stand",
                     "bowl": f"{MODID}:block/scp_330_bowl_side", "candy": f"{MODID}:block/scp_330_bowl_top"},
        "elements": [
            {"from": [6, 0, 6], "to": [10, 9, 10], "faces": {d: {"texture": "#stand"} for d in ["north", "south", "east", "west", "up", "down"]}},
            {"from": [3, 9, 3], "to": [13, 13, 13], "faces": {**{d: {"texture": "#bowl"} for d in ["north", "south", "east", "west", "down"]}, "up": {"texture": "#candy"}}},
        ]})
    write_json(os.path.join(bs, "scp_330.json"), {"variants": {"": {"model": f"{MODID}:block/scp_330"}}})
    write_json(os.path.join(bm, "scp_458.json"), {
        "parent": "minecraft:block/block",
        "textures": {"particle": f"{MODID}:block/scp_458_side", "top": f"{MODID}:block/scp_458_top", "side": f"{MODID}:block/scp_458_side"},
        "elements": [{"from": [1, 0, 1], "to": [15, 3, 15], "faces": {
            "up": {"texture": "#top"}, "down": {"texture": "#side"},
            **{d: {"texture": "#side", "uv": [0, 6, 16, 9]} for d in ["north", "south", "east", "west"]}}}]})
    write_json(os.path.join(bs, "scp_458.json"), {"variants": {
        f"facing={f}": ({"model": f"{MODID}:block/scp_458", "y": y} if y else {"model": f"{MODID}:block/scp_458"}) for f, y in FACING_Y.items()}})
    write_json(os.path.join(bm, "scp_012.json"), {
        "parent": "minecraft:block/block",
        "textures": {"particle": f"{MODID}:block/scp_012_score", "stand": f"{MODID}:block/scp_012_stand", "score": f"{MODID}:block/scp_012_score"},
        "elements": [
            {"from": [7, 0, 7], "to": [9, 11, 9], "faces": {d: {"texture": "#stand"} for d in ["north", "south", "east", "west", "up", "down"]}},
            {"from": [4, 0, 4], "to": [12, 1, 12], "faces": {d: {"texture": "#stand"} for d in ["north", "south", "east", "west", "up", "down"]}},
            {"from": [2, 10, 7], "to": [14, 13, 8], "faces": {d: {"texture": "#stand"} for d in ["south", "east", "west", "up", "down"]}},
            {"from": [2, 11, 6], "to": [14, 21, 7], "rotation": {"origin": [8, 11, 7], "axis": "x", "angle": -22.5},
             "faces": {"north": {"texture": "#score"}, "south": {"texture": "#stand"}, "east": {"texture": "#stand"},
                       "west": {"texture": "#stand"}, "up": {"texture": "#stand"}, "down": {"texture": "#stand"}}},
        ]})
    write_json(os.path.join(bs, "scp_012.json"), {"variants": {
        f"facing={f}": ({"model": f"{MODID}:block/scp_012", "y": y} if y else {"model": f"{MODID}:block/scp_012"}) for f, y in FACING_Y.items()}})
    for b in ALL_BLOCKS:
        write_json(os.path.join(im, f"{b}.json"), {"parent": f"{MODID}:block/{b}"})
    for it in GENERATED_ITEMS:
        write_json(os.path.join(im, f"{it}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MODID}:item/{it}"}})
    # SCP-035: a porcelain plate worn over the face
    write_json(os.path.join(im, "scp_035.json"), {
        "parent": "minecraft:block/block", "textures": {"mask": f"{MODID}:item/scp_035", "particle": f"{MODID}:item/scp_035"},
        "elements": [{"from": [1, 0, -1], "to": [15, 16, 0.5], "faces": {"north": {"texture": "#mask"}, "south": {"texture": "#mask"}}}],
        "display": {"gui": {"rotation": [0, 180, 0], "scale": [0.8, 0.8, 0.8]},
                    "ground": {"translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
                    "fixed": {"rotation": [0, 180, 0]},
                    "thirdperson_righthand": {"rotation": [75, 180, 0], "translation": [0, 2.5, 0], "scale": [0.4, 0.4, 0.4]},
                    "firstperson_righthand": {"rotation": [0, 135, 0], "scale": [0.4, 0.4, 0.4]},
                    "head": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]}}})
    # SCP-049-2 head for zombie players
    write_json(os.path.join(im, "scp_049_2_mask.json"), {
        "parent": "minecraft:block/block",
        "textures": {"face": f"{MODID}:item/scp_049_2_face", "skin": f"{MODID}:item/scp_049_2_skin", "particle": f"{MODID}:item/scp_049_2_skin"},
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {"north": {"texture": "#face"}, "south": {"texture": "#skin"},
                      "east": {"texture": "#skin"}, "west": {"texture": "#skin"}, "up": {"texture": "#skin"}, "down": {"texture": "#skin"}}}],
        "display": {"head": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.85, 0.85, 0.85]},
                    "gui": {"rotation": [30, 225, 0], "scale": [0.625, 0.625, 0.625]}}})
    for it in HANDHELD_ITEMS:
        write_json(os.path.join(im, f"{it}.json"), {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MODID}:item/{it}"}})
    for e in SPAWN_EGGS:
        write_json(os.path.join(im, f"{e}_spawn_egg.json"), {"parent": "minecraft:item/template_spawn_egg"})
    # block loot
    for b in ALL_BLOCKS:
        write_json(os.path.join(DATA, "loot_tables", "blocks", f"{b}.json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MODID}:{b}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    # mining tags
    write_json(os.path.join(RES, "data", "minecraft", "tags", "blocks", "mineable", "pickaxe.json"),
               {"replace": False, "values": [f"{MODID}:{b}" for b in ALL_BLOCKS if b not in ("scp_458", "scp_012")]})
    write_json(os.path.join(RES, "data", "minecraft", "tags", "blocks", "mineable", "axe.json"),
               {"replace": False, "values": [f"{MODID}:scp_458", f"{MODID}:scp_012"]})


def recipes():
    rd = os.path.join(DATA, "recipes")

    def shaped(name, pattern, key, result, count=1, cat="building"):
        write_json(os.path.join(rd, f"{name}.json"), {"type": "minecraft:crafting_shaped", "category": cat, "pattern": pattern,
                                                    "key": {k: {"item": v} for k, v in key.items()},
                                                    "result": {"item": result, "count": count}})

    def shapeless(name, items, result, count=1, cat="misc"):
        write_json(os.path.join(rd, f"{name}.json"), {"type": "minecraft:crafting_shapeless", "category": cat,
                                                    "ingredients": [{"item": i} for i in items],
                                                    "result": {"item": result, "count": count}})
    m = MODID
    shaped("reinforced_concrete", ["###", "#I#", "###"], {"#": "minecraft:gray_concrete", "I": "minecraft:iron_ingot"}, f"{m}:reinforced_concrete", 8)
    shaped("facility_wall", ["###", "#I#", "###"], {"#": "minecraft:white_concrete", "I": "minecraft:iron_nugget"}, f"{m}:facility_wall", 8)
    shaped("facility_floor", ["###", "#I#", "###"], {"#": "minecraft:light_gray_concrete", "I": "minecraft:iron_nugget"}, f"{m}:facility_floor", 8)
    shaped("hazard_block", ["YB", "BY"], {"Y": "minecraft:yellow_concrete", "B": "minecraft:black_concrete"}, f"{m}:hazard_block", 4)
    shaped("containment_glass", ["###", "#I#", "###"], {"#": "minecraft:glass", "I": "minecraft:iron_ingot"}, f"{m}:containment_glass", 8)
    shaped("foundation_emblem", [" D ", "D#D", " D "], {"D": "minecraft:black_dye", "#": "minecraft:white_concrete"}, f"{m}:foundation_emblem", 1)
    shapeless("keycard_level_1", ["minecraft:paper", "minecraft:iron_nugget", "minecraft:redstone"], f"{m}:keycard_level_1")
    shaped("keycard_reader_1", ["III", "IRI", "IKI"], {"I": "minecraft:iron_ingot", "R": "minecraft:redstone", "K": f"{m}:keycard_level_1"}, f"{m}:keycard_reader_1", 1, "redstone")
    for lvl in range(2, 6):
        shapeless(f"keycard_reader_{lvl}", [f"{m}:keycard_reader_{lvl - 1}", f"{m}:keycard_level_{lvl}"], f"{m}:keycard_reader_{lvl}", 1, "redstone")


LANG = {
    "itemGroup.scpfoundation.main": "SCP Foundation",
    "itemGroup.scpfoundation.archive": "SCP Archive (001-9999)",
    "block.scpfoundation.reinforced_concrete": "Reinforced Concrete",
    "block.scpfoundation.facility_wall": "Facility Wall Panel",
    "block.scpfoundation.facility_floor": "Facility Floor Tiles",
    "block.scpfoundation.hazard_block": "Hazard Stripe Block",
    "block.scpfoundation.containment_glass": "Containment Glass",
    "block.scpfoundation.foundation_emblem": "Foundation Emblem",
    "block.scpfoundation.corroded_block": "Corroded Matter",
    "block.scpfoundation.keycard_reader_1": "Keycard Reader (Level 1)",
    "block.scpfoundation.keycard_reader_2": "Keycard Reader (Level 2)",
    "block.scpfoundation.keycard_reader_3": "Keycard Reader (Level 3)",
    "block.scpfoundation.keycard_reader_4": "Keycard Reader (Level 4)",
    "block.scpfoundation.keycard_reader_5": "Keycard Reader (Level 5)",
    "block.scpfoundation.scp_914": "SCP-914 \"The Clockworks\"",
    "block.scpfoundation.scp_294": "SCP-294 \"The Coffee Machine\"",
    "block.scpfoundation.scp_330": "SCP-330 \"Take Only Two\"",
    "block.scpfoundation.scp_458": "SCP-458 \"The Never-Ending Pizza Box\"",
    "block.scpfoundation.scp_1162": "SCP-1162 \"Hole in the Wall\"",
    "item.scpfoundation.scp_document": "Classified Document",
    "item.scpfoundation.scp_document.numbered": "SCP-%s Personnel File",
    "item.scpfoundation.keycard_level_1": "Keycard - Level 1",
    "item.scpfoundation.keycard_level_2": "Keycard - Level 2",
    "item.scpfoundation.keycard_level_3": "Keycard - Level 3",
    "item.scpfoundation.keycard_level_4": "Keycard - Level 4",
    "item.scpfoundation.keycard_level_5": "Keycard - Level 5",
    "item.scpfoundation.keycard_o5": "O5 Council Keycard",
    "item.scpfoundation.scp_500": "SCP-500 \"Panacea\"",
    "item.scpfoundation.scp_207": "SCP-207 \"Cola Bottle\"",
    "item.scpfoundation.scp_018": "SCP-018 \"Super Ball\"",
    "item.scpfoundation.scp_005": "SCP-005 \"Skeleton Key\"",
    "item.scpfoundation.scp_427": "SCP-427 \"Lovecraftian Locket\"",
    "item.scpfoundation.scp_714": "SCP-714 \"The Jaded Ring\"",
    "item.scpfoundation.scp_268": "SCP-268 \"Cap of Neglect\"",
    "item.scpfoundation.scp_063": "SCP-063 \"World's Best Toothbrush\"",
    "block.scpfoundation.scp_012": "SCP-012 \"A Bad Composition\"",
    "item.scpfoundation.scp_1471": "Phone with SCP-1471 \"MalO ver1.0.0\"",
    "item.scpfoundation.scp_1025": "SCP-1025 \"Encyclopedia of Diseases\"",
    "item.scpfoundation.scp_006": "Bottle of SCP-006 Water",
    "entity.scpfoundation.scp_066": "SCP-066",
    "entity.scpfoundation.scp_1471_a": "SCP-1471-A",
    "tooltip.scpfoundation.scp_1471": "Use to install MalO. Use again to take a photo. Sneak + use to uninstall.",
    "tooltip.scpfoundation.scp_1025": "Reading it is not recommended.",
    "tooltip.scpfoundation.scp_006": "Heals and washes away poison and fatigue.",
    "message.scpfoundation.1471_installed": "[MalO ver1.0.0] Installed successfully. You will never be alone again.",
    "message.scpfoundation.1471_uninstall": "[MalO ver1.0.0] Uninstall failed. MalO ver1.0.0 has been reinstalled.",
    "message.scpfoundation.1471_image": "[MalO ver1.0.0] 1 new image received.",
    "message.scpfoundation.1471_photo_figure": "*click* There is something standing behind you in the photo.",
    "message.scpfoundation.1471_photo_clear": "*click* Nothing unusual in the photo... this time.",
    "message.scpfoundation.066_eric": "Eric?",
    "message.scpfoundation.012_compelled": "You feel compelled to finish the composition...",
    "message.scpfoundation.012_writing": "You are writing the score with your own blood.",
    "message.scpfoundation.1025_read": "You read the entry on %s. You start to feel the symptoms.",
    "message.scpfoundation.2521_taken": "Something with too many arms took your %s.",
    "disease.scpfoundation.cold": "the common cold",
    "disease.scpfoundation.flu": "influenza",
    "disease.scpfoundation.food_poisoning": "food poisoning",
    "disease.scpfoundation.malaria": "malaria",
    "disease.scpfoundation.vertigo": "vertigo",
    "disease.scpfoundation.night_blindness": "night blindness",
    "disease.scpfoundation.necrosis": "necrosis",
    "disease.scpfoundation.insomnia": "insomnia",
    "item.scpfoundation.scp_008": "SCP-008 Vial \"Zombie Plague\"",
    "item.scpfoundation.scp_035": "SCP-035 \"Possessive Mask\"",
    "item.scpfoundation.scp_963": "SCP-963 \"Immortality\"",
    "item.scpfoundation.scp_178": "SCP-178 \"3-D Specs\"",
    "item.scpfoundation.scp_049_2_mask": "Face of SCP-049-2",
    "entity.scpfoundation.scp_2295": "SCP-2295",
    "effect.scpfoundation.scp_008_infection": "SCP-008 Infection",
    "tooltip.scpfoundation.scp_008": "Use on a creature to infect it. Sneak + use to infect yourself.",
    "tooltip.scpfoundation.scp_035": "Put it on. It wants you to.",
    "tooltip.scpfoundation.scp_963": "Carry it: cheats death once every 5 minutes.",
    "tooltip.scpfoundation.scp_178": "Wear them to see every living thing around you.",
    "message.scpfoundation.008_infected": "You have been infected with SCP-008.",
    "message.scpfoundation.008_stage2": "SCP-008: you feel feverish and starving...",
    "message.scpfoundation.008_stage3": "SCP-008: your skin is turning grey. Find SCP-500!",
    "message.scpfoundation.zombie_became": "You have become SCP-049-2. Spread the plague. (SCP-500 cures you)",
    "message.scpfoundation.zombie_announce": "%s has turned into SCP-049-2!",
    "message.scpfoundation.zombie_cured": "The plague leaves your body. You are human again.",
    "message.scpfoundation.963_saved": "Your consciousness refuses to leave this body...",
    "message.scpfoundation.2295_heal": "SCP-2295 stitches your wounds closed.",
    "message.scpfoundation.035_worn": "The mask fuses to your face. You feel... free.",
    "message.scpfoundation.035_call": "Something in your bag whispers: put me on...",
    "message.scpfoundation.035.0": "Isn't this better than being yourself?",
    "message.scpfoundation.035.1": "They locked me in a box. You let me out.",
    "message.scpfoundation.035.2": "Your body will not last. I will find another.",
    "message.scpfoundation.035.3": "We could do so much together.",
    "message.scpfoundation.035.4": "Don't take me off. You'd miss me.",
    "message.scpfoundation.035.5": "I can see everything you've done.",
    "gamerule.scpDeadPlayersRise": "SCP: dead players rise as SCP-049-2",
    "gamerule.scpPlayableZombies": "SCP: players killed by SCP-049/008 play as SCP-049-2",
    "item.scpfoundation.scp_294_cup": "Cup",
    "item.scpfoundation.scp_294_cup.named": "Cup of %s",
    "item.scpfoundation.scp_330_candy": "SCP-330 Candy",
    "item.scpfoundation.scp_458_pizza": "Slice of SCP-458 Pizza",
    "entity.scpfoundation.scp_173": "SCP-173",
    "entity.scpfoundation.scp_096": "SCP-096",
    "entity.scpfoundation.scp_049": "SCP-049",
    "entity.scpfoundation.scp_049_2": "SCP-049-2",
    "entity.scpfoundation.scp_106": "SCP-106",
    "entity.scpfoundation.scp_682": "SCP-682",
    "entity.scpfoundation.scp_939": "SCP-939",
    "entity.scpfoundation.scp_999": "SCP-999",
    "entity.scpfoundation.scp_131": "SCP-131",
    "entity.scpfoundation.scp_529": "SCP-529",
    "entity.scpfoundation.scp_1048": "SCP-1048",
    "entity.scpfoundation.scp_018": "SCP-018",
    "tooltip.scpfoundation.document.classified": "Right-click to decrypt",
    "tooltip.scpfoundation.document.read": "Right-click to read the file",
    "tooltip.scpfoundation.object_class": "Object Class: %s",
    "tooltip.scpfoundation.in_game": "Physically present in this world",
    "tooltip.scpfoundation.keycard": "Clearance level %s",
    "tooltip.scpfoundation.scp_500": "Cures all ailments. Handle with care.",
    "tooltip.scpfoundation.scp_207": "Speed comes at a cost.",
    "tooltip.scpfoundation.scp_018": "Gains energy every time it bounces.",
    "tooltip.scpfoundation.scp_005": "Opens any door, trapdoor, gate or keycard reader.",
    "tooltip.scpfoundation.scp_427": "Heals the holder... if used in moderation.",
    "tooltip.scpfoundation.scp_714": "Hold in off-hand: immunity to poison and wither, but drowsiness.",
    "tooltip.scpfoundation.scp_268": "Wear it: nobody pays any attention to you.",
    "tooltip.scpfoundation.scp_063": "Destroys organic matter instantly.",
    "tooltip.scpfoundation.scp_294": "Insert an iron nugget. Rename it in an anvil to order a specific drink.",
    "tooltip.scpfoundation.scp_914": "Sneak + use to change setting. Use with an item to refine it.",
    "message.scpfoundation.blink": "*blink*",
    "message.scpfoundation.reader_need": "ACCESS DENIED - Level %s keycard required",
    "message.scpfoundation.reader_denied": "ACCESS DENIED - Clearance level %s is insufficient (needs %s)",
    "message.scpfoundation.reader_granted": "ACCESS GRANTED",
    "message.scpfoundation.pocket_enter": "You sink through the floor into a corroded void...",
    "message.scpfoundation.pocket_escape": "You found a way out of the pocket dimension.",
    "message.scpfoundation.pocket_loop": "The corridor loops back on itself.",
    "message.scpfoundation.096_triggered": "You saw its face.",
    "message.scpfoundation.330_ok": "You take a piece of candy. (%s/2)",
    "message.scpfoundation.330_punish": "You took more than two.",
    "message.scpfoundation.1162_hurt": "Something inside the hole cuts your hand.",
    "message.scpfoundation.1162_trade": "You pull something out of the hole.",
    "message.scpfoundation.914_setting": "SCP-914 setting: %s",
    "message.scpfoundation.914_refined": "SCP-914 [%s]: %s -> %s",
    "message.scpfoundation.294_unknown": "OUT OF RANGE",
    "message.scpfoundation.294_dispensed": "SCP-294 dispenses a cup of %s.",
    "message.scpfoundation.294_coin": "SCP-294 requires a coin (iron nugget).",
    "message.scpfoundation.decrypted": "Document decrypted: SCP-%s",
    "message.scpfoundation.427_overuse": "Your flesh starts to feel... wrong.",
    "message.scpfoundation.049.0": "You are not well. I can help you.",
    "message.scpfoundation.049.1": "Hold still. The cure is painless.",
    "message.scpfoundation.049.2": "The sickness is all around us...",
    "message.scpfoundation.049.3": "Do not run. I am the cure.",
    "message.scpfoundation.939.0": "Hello? Is someone there?",
    "message.scpfoundation.939.1": "Please, I'm hurt... over here.",
    "message.scpfoundation.939.2": "I found something, come look!",
    "message.scpfoundation.939.3": "Don't leave me down here.",
    "scp914.setting.0": "Rough",
    "scp914.setting.1": "Coarse",
    "scp914.setting.2": "1:1",
    "scp914.setting.3": "Fine",
    "scp914.setting.4": "Very Fine",
    "drink.scpfoundation.coffee": "Coffee",
    "drink.scpfoundation.tea": "Tea",
    "drink.scpfoundation.hot_chocolate": "Hot Chocolate",
    "drink.scpfoundation.water": "Water",
    "drink.scpfoundation.milk": "Milk",
    "drink.scpfoundation.lava": "Lava",
    "drink.scpfoundation.courage": "Liquid Courage",
    "drink.scpfoundation.joy": "Joy",
    "drink.scpfoundation.despair": "Despair",
    "drink.scpfoundation.starlight": "Starlight",
    "drink.scpfoundation.antimatter": "Antimatter",
    "drink.scpfoundation.air": "Air",
    "drink.scpfoundation.quantum": "Quantum Soup",
    "drink.scpfoundation.cola": "Cola",
    "database.scpfoundation.header": "===== SCP FOUNDATION DATABASE =====",
    "database.scpfoundation.item": "Item #: SCP-%s",
    "database.scpfoundation.nickname": "Nickname: %s",
    "database.scpfoundation.class": "Object Class: %s",
    "database.scpfoundation.summary": "Summary: %s",
    "database.scpfoundation.restricted": "File contents restricted. Consult the full file.",
    "database.scpfoundation.ingame": "[This anomaly exists in your world]",
    "database.scpfoundation.link": "[Open the full file on the SCP Wiki]",
    "database.scpfoundation.license": "SCP Wiki content is licensed CC BY-SA 3.0",
    "database.scpfoundation.redacted": "[REDACTED]",
    "command.scpfoundation.list": "Anomalies present in this world: %s",
}


def lang():
    for e in SPAWN_EGGS:
        LANG[f"item.{MODID}.{e}_spawn_egg"] = LANG[f"entity.{MODID}.{e}"] + " Spawn Egg"
    write_json(os.path.join(ASSETS, "lang", "en_us.json"), LANG)


def icon():
    img = Image.new("RGBA", (128, 128), (20, 20, 24, 255))
    d = ImageDraw.Draw(img)
    d.ellipse([14, 14, 114, 114], outline=(235, 235, 230), width=10)
    d.ellipse([52, 52, 76, 76], fill=(235, 235, 230))
    for ang in (90, 210, 330):
        a = math.radians(ang)
        tip = (64 + math.cos(a) * 18, 64 - math.sin(a) * 18)
        b1 = (64 + math.cos(a) * 40 + math.cos(a + 1.57) * 9, 64 - math.sin(a) * 40 - math.sin(a + 1.57) * 9)
        b2 = (64 + math.cos(a) * 40 - math.cos(a + 1.57) * 9, 64 - math.sin(a) * 40 + math.sin(a + 1.57) * 9)
        d.polygon([tip, b1, b2], fill=(235, 235, 230))
    img.save(p(ASSETS, "icon.png"))


if __name__ == "__main__":
    block_textures()
    item_textures()
    entity_textures()
    models_and_states()
    recipes()
    lang()
    icon()
    print("assets generated")
