"""Block textures, item sprites, custom spawn eggs, effect icons, hypno spiral, mod icon."""
import os, sys, random, math
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image, ImageDraw
from spec import FORMS, SCIENTISTS, BLOCKS, MODID, h as hexc

ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets", MODID)
TB, TI = ROOT + "/textures/block", ROOT + "/textures/item"
for d in (TB, TI, ROOT + "/textures/mob_effect", ROOT + "/textures/gui"):
    os.makedirs(d, exist_ok=True)


def clamp(c):
    return tuple(max(0, min(255, int(x))) for x in c)


def sh(c, k):
    return clamp((c[0] + k, c[1] + k, c[2] + k))


def new(size=16):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def noise_fill(img, base, amp, rnd):
    for y in range(img.height):
        for x in range(img.width):
            img.putpixel((x, y), sh(base, rnd.randint(-amp, amp)) + (255,))


# ------------------------------------------------------------------ blocks
def latex_tex(base, seed, gloss=True):
    rnd = random.Random(seed)
    img = new()
    lum = sum(base) / 3
    hi = 60 if lum < 70 else 35 if lum < 170 else 18
    # smooth blobby height field
    blobs = [(rnd.uniform(0, 16), rnd.uniform(0, 16), rnd.uniform(2.5, 5)) for _ in range(7)]
    for y in range(16):
        for x in range(16):
            v = 0
            for (bx, by, r) in blobs:
                for ox in (-16, 0, 16):
                    for oy in (-16, 0, 16):
                        d = math.hypot(x - bx - ox, y - by - oy)
                        v += max(0, 1 - d / r)
            k = int((v - 0.8) * 22) + rnd.randint(-3, 3)
            img.putpixel((x, y), sh(base, k) + (255,))
    if gloss:
        for (bx, by, r) in blobs[:4]:
            px, py = int(bx - r * 0.35) % 16, int(by - r * 0.35) % 16
            img.putpixel((px, py), sh(base, hi) + (255,))
            img.putpixel(((px + 1) % 16, py), sh(base, hi - 15) + (255,))
    return img


def bricks(base, mortar, seed, cracked=False):
    rnd = random.Random(seed)
    img = new()
    noise_fill(img, base, 6, rnd)
    for y in range(16):
        for x in range(16):
            row = y // 4
            off = 4 if row % 2 else 0
            if y % 4 == 3 or (x + off) % 8 == 7:
                img.putpixel((x, y), sh(mortar, rnd.randint(-4, 4)) + (255,))
            elif y % 4 == 0:
                img.putpixel((x, y), sh(base, 14) + (255,))
    if cracked:
        x, y = 3, 0
        while y < 16:
            img.putpixel((x % 16, y), (8, 8, 10, 255))
            x += rnd.choice((-1, 0, 1))
            y += 1
        for i in range(6):
            img.putpixel((10 + i % 3, 6 + i), (8, 8, 10, 255))
    return img


def veins(img, color, seed, n=3):
    rnd = random.Random(seed)
    for _ in range(n):
        x, y = rnd.randrange(16), rnd.randrange(16)
        for _ in range(rnd.randint(6, 12)):
            img.putpixel((x % 16, y % 16), color + (255,))
            if rnd.random() < 0.4:
                img.putpixel(((x + 1) % 16, y % 16), sh(color, 20) + (255,))
            x += rnd.choice((-1, 0, 1))
            y += rnd.choice((0, 1, 1))
    return img


def tiles(base, grout, size, seed):
    rnd = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            if x % size == size - 1 or y % size == size - 1:
                c = sh(grout, rnd.randint(-3, 3))
            else:
                c = sh(base, rnd.randint(-3, 3) + (6 if (x % size == 0 or y % size == 0) else 0))
            img.putpixel((x, y), c + (255,))
    return img


def frame(img, color, w=1):
    for i in range(16):
        for t in range(w):
            for (x, y) in ((i, t), (i, 15 - t), (t, i), (15 - t, i)):
                img.putpixel((x, y), color + (255,))
    return img


def panel(base, seed, rivets=True, rust=False):
    rnd = random.Random(seed)
    img = new()
    noise_fill(img, base, 5, rnd)
    frame(img, sh(base, -30))
    for i in range(1, 15):
        img.putpixel((i, 1), sh(base, 20) + (255,))
        img.putpixel((1, i), sh(base, 12) + (255,))
    if rivets:
        for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
            img.putpixel((x, y), sh(base, 40) + (255,))
            img.putpixel((x + 1, y + 1), sh(base, -40) + (255,))
    if rust:
        for _ in range(40):
            x, y = rnd.randrange(16), rnd.randrange(16)
            img.putpixel((x, y), (130 + rnd.randint(-20, 20), 70 + rnd.randint(-15, 15), 30, 255))
    return img


def block_textures():
    tex = {}
    for (bid, name, kind, arg) in BLOCKS:
        if kind == "latex":
            tex[bid] = latex_tex(hexc(arg), bid)
        elif kind == "puddle":
            tex[bid] = latex_tex(hexc(arg), bid)
        elif kind == "crystal":
            c = hexc(arg)
            img = new()
            rnd = random.Random(bid)
            for (cx, hgt, w) in ((4, 9, 2), (8, 14, 3), (12, 8, 2), (6, 6, 1), (10, 11, 2)):
                for y in range(16 - hgt, 16):
                    t = (y - (16 - hgt)) / hgt
                    half = max(0, int(w * t + 0.5))
                    for x in range(cx - half, cx + half + 1):
                        if 0 <= x < 16:
                            k = 45 if x == cx - half else (-25 if x == cx + half else 0)
                            img.putpixel((x, y), sh(c, k + rnd.randint(-4, 4)) + (255,))
                img.putpixel((cx, 16 - hgt), sh(c, 70) + (255,))
            tex[bid] = img
    rnd = random.Random(1)

    img = new(); noise_fill(img, (110, 78, 52), 12, rnd); tex["infected_dirt"] = veins(img, (14, 14, 18), 2, 5)
    img = new(); noise_fill(img, (120, 120, 124), 12, rnd); tex["infected_stone"] = veins(img, (14, 14, 18), 3, 4)
    # log side & top
    img = new()
    for y in range(16):
        for x in range(16):
            c = (60, 44, 32) if x % 4 else (40, 30, 22)
            img.putpixel((x, y), sh(c, rnd.randint(-6, 6)) + (255,))
    for x in (2, 7, 11, 14):
        for y in range(0, rnd.randint(5, 13)):
            img.putpixel((x, y), (12, 12, 16, 255))
    tex["infected_log"] = img
    img = new()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 7:
                c = (50, 38, 28)
            elif d < 2.5:
                c = (14, 14, 18)
            else:
                c = (140, 110, 80) if int(d) % 2 else (120, 92, 66)
            img.putpixel((x, y), sh(c, rnd.randint(-5, 5)) + (255,))
    tex["infected_log_top"] = img
    img = new()
    for y in range(16):
        for x in range(16):
            c = (96, 74, 54) if y % 4 != 3 else (60, 46, 34)
            if (y // 4) % 2 == 0 and x == 7 or (y // 4) % 2 == 1 and x == 12:
                c = (60, 46, 34)
            img.putpixel((x, y), sh(c, rnd.randint(-6, 6)) + (255,))
    tex["infected_planks"] = veins(img, (14, 14, 18), 5, 3)
    tex["latex_bricks"] = bricks((28, 26, 34), (10, 10, 12), 6)
    tex["cracked_latex_bricks"] = bricks((28, 26, 34), (10, 10, 12), 6, cracked=True)
    tex["lab_tile"] = tiles((232, 236, 240), (170, 176, 184), 8, 7)
    tex["dark_lab_tile"] = tiles((72, 76, 86), (40, 42, 50), 8, 8)
    img = tiles((226, 230, 234), (196, 200, 206), 16, 9)
    for x in range(16):
        img.putpixel((x, 8), (200, 204, 210, 255))
    tex["lab_wall"] = img
    img = tiles((226, 230, 234), (196, 200, 206), 16, 10)
    for y in range(6, 10):
        for x in range(16):
            img.putpixel((x, y), sh((24, 168, 170) if y in (7, 8) else (16, 110, 112), rnd.randint(-3, 3)) + (255,))
    tex["lab_wall_striped"] = img
    img = new(); noise_fill(img, (176, 180, 186), 4, rnd)
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0 and (x - y) % 8 == 0 or (x - y) % 4 == 0 and (x + y) % 8 == 4:
                img.putpixel((x, y), (210, 214, 220, 255))
    tex["lab_floor"] = frame(img, (150, 154, 160))
    img = new(); noise_fill(img, (250, 250, 245), 3, rnd); frame(img, (150, 154, 160), 2)
    tex["lab_ceiling_light"] = img
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), (190, 220, 235, 70))
    frame(img, (120, 130, 140))
    for i in range(16):
        img.putpixel((i, 8), (140, 150, 160, 255))
        img.putpixel((8, i), (140, 150, 160, 255))
    for i in range(3, 7):
        img.putpixel((i, 9 - i), (255, 255, 255, 170))
    tex["reinforced_lab_glass"] = img
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), (40 + y * 2, 20, 70 + y * 3, 150 + y * 4))
    frame(img, (110, 116, 126), 1)
    for (x, y) in ((4, 10), (6, 6), (10, 12), (11, 4), (5, 3)):
        img.putpixel((x, y), (220, 160, 255, 220))
    for i in range(2, 6):
        img.putpixel((2, i), (255, 255, 255, 140))
    tex["containment_tank"] = img
    img = panel((120, 124, 130), 11, rivets=False)
    for y in range(3, 14, 2):
        for x in range(2, 14):
            img.putpixel((x, y), (30, 32, 36, 255))
    tex["lab_vent"] = img
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), ((230, 190, 20) if ((x + y) // 4) % 2 == 0 else (24, 24, 24)) + (255,))
    tex["hazard_block"] = img
    tex["steel_panel"] = panel((140, 146, 154), 12)
    tex["rusted_steel_panel"] = panel((120, 116, 110), 13, rust=True)
    img = new()
    for y in range(16):
        for x in range(16):
            if x % 4 == 0 or y % 4 == 0:
                img.putpixel((x, y), sh((96, 100, 108), rnd.randint(-6, 6)) + (255,))
    tex["metal_grate"] = frame(img, (70, 74, 80))
    img = new()
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), sh((176, 140, 90) if y % 5 else (140, 108, 66), rnd.randint(-6, 6)) + (255,))
    for i in range(16):
        img.putpixel((i, i), (120, 92, 56, 255))
        img.putpixel((15 - i, i), (120, 92, 56, 255))
    frame(img, (90, 94, 100))
    for (x, y) in ((6, 6), (7, 6), (8, 6), (9, 6), (6, 9), (9, 9), (7, 10), (8, 10)):
        img.putpixel((x, y), (230, 190, 20, 255))
    tex["lab_crate"] = img
    img = new(); noise_fill(img, (60, 62, 66), 4, rnd)
    for y in range(3, 13):
        for x in range(3, 13):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 5:
                img.putpixel((x, y), (255, 40 + int(d * 15), 30, 255))
    img.putpixel((6, 5), (255, 220, 200, 255))
    tex["warning_light"] = img
    img = new(); noise_fill(img, (20, 30, 40), 3, rnd)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 6:
                img.putpixel((x, y), (40 + int(30 * math.sin(d * 1.3)), 220, 230, 255))
    frame(img, (90, 94, 100))
    tex["latex_glow_lamp"] = img

    # ---- facing machines (front / side / top) ----
    side = panel((96, 100, 108), 20); tex["machine_side"] = side
    top = panel((110, 114, 122), 21, rivets=False); tex["machine_top"] = top
    img = panel((96, 100, 108), 22, rivets=False)
    for y in range(2, 11):
        for x in range(2, 14):
            img.putpixel((x, y), (8, 24, 14, 255))
    for row, ln in ((3, 9), (5, 6), (7, 10), (9, 4)):
        for x in range(3, 3 + ln):
            if rnd.random() < 0.8:
                img.putpixel((x, row), (70, 240, 120, 255))
    for x in range(3, 13):
        img.putpixel((x, 13), (50, 52, 58, 255))
    img.putpixel((12, 13), (255, 60, 60, 255))
    tex["lab_computer_front"] = img
    img = new(); noise_fill(img, (22, 22, 26), 3, rnd); frame(img, (60, 62, 70))
    for y in range(2, 15, 3):
        for x in range(2, 14):
            img.putpixel((x, y), (44, 46, 52, 255))
        for x in (3, 5, 7):
            img.putpixel((x, y + 1), rnd.choice([(60, 255, 90), (60, 160, 255), (255, 180, 40), (30, 60, 30)]) + (255,))
    tex["server_rack_front"] = img
    img = new()
    for y in range(16):
        for x in range(16):
            a = math.atan2(y - 7.5, x - 7.5)
            r = math.hypot(x - 7.5, y - 7.5)
            v = math.sin(a * 2 + r * 1.4)
            img.putpixel((x, y), ((235, 60, 230) if v > 0.2 else (60, 10, 90) if v > -0.4 else (15, 5, 25)) + (255,))
    frame(img, (40, 40, 48))
    tex["hypno_screen_front"] = img
    img = panel((50, 52, 60), 23, rivets=False)
    for y in range(1, 15):
        for x in range(3, 13):
            img.putpixel((x, y), (60, 160, 200, 120) if (x in (3, 12) or y in (1, 14)) else (20, 40, 60, 255))
    for (x, y) in ((7, 4), (8, 4), (7, 5), (8, 5), (6, 6), (7, 6), (8, 6), (9, 6), (6, 7), (7, 7), (8, 7), (9, 7),
                   (7, 8), (8, 8), (7, 9), (8, 9), (7, 10), (8, 10), (6, 11), (9, 11), (6, 12), (9, 12)):
        img.putpixel((x, y), (8, 8, 12, 255))
    img.putpixel((7, 5), (60, 230, 255, 255)); img.putpixel((8, 5), (60, 230, 255, 255))
    for x in (4, 11):
        for y in (3, 7, 11):
            img.putpixel((x, y), (60, 230, 255, 255))
    tex["drone_conversion_pod_front"] = img
    for name, img in tex.items():
        img.save(f"{TB}/{name}.png")


# ------------------------------------------------------------------ items (pixel art)
def art(rows, pal):
    img = new()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch] + (255,))
    return img


ITEM_ART = {
    "latex_blade": ([
        "..............bW",
        ".............bwb",
        "............bwb.",
        "...........bwb..",
        "..........bwb...",
        ".........bwb....",
        "........bwb.....",
        ".......bwb......",
        "..p...bwb.......",
        "..pp.bwb........",
        "...ppwb.........",
        "....hp..........",
        "...hhpp.........",
        "..hh..pp........",
        ".hh.............",
        "gh..............",
    ], {"b": (20, 18, 26), "w": (70, 60, 95), "W": (190, 170, 255), "p": (160, 60, 220), "h": (60, 44, 34), "g": (230, 200, 60)}),
    "syringe_gun": ([
        "................",
        "................",
        "................",
        "...gggggggggn...",
        "..mmmmmmmmmmmnnn",
        "..mlllllllllm...",
        "..mppppppppm....",
        "..mmmmmmmmmm....",
        "..mmmk..........",
        "..mmmkk.........",
        "..mmm.k.........",
        "..mmmkk.........",
        ".dmmm...........",
        ".dddm...........",
        "................",
        "................",
    ], {"g": (180, 230, 245), "n": (220, 225, 235), "m": (70, 74, 84), "l": (110, 116, 128), "p": (150, 60, 230),
        "k": (40, 40, 46), "d": (40, 40, 46)}),
    "hypno_staff": ([
        "...........ppp..",
        "..........pPpPp.",
        "..........pPWPp.",
        "..........ppPpp.",
        "...........ppp..",
        "..........gg....",
        ".........ss.....",
        "........ss......",
        ".......ss.......",
        "......ss........",
        ".....ss.........",
        "....ss..........",
        "...ss...........",
        "..ss............",
        ".ss.............",
        "s...............",
    ], {"p": (120, 30, 170), "P": (240, 80, 240), "W": (255, 230, 255), "g": (230, 200, 60), "s": (50, 36, 60)}),
    "purifier_cannon": ([
        "................",
        "................",
        "....wwwwwwww....",
        "...wllllllllwc..",
        "..wllccccccllcC.",
        "..wllccCCcccllcC",
        "..wllccccccllcC.",
        "...wllllllllwc..",
        "....wwmmwwww....",
        ".....mmm........",
        ".....mm.........",
        "....mmm.........",
        "....mm..........",
        "................",
        "................",
        "................",
    ], {"w": (220, 226, 236), "l": (180, 190, 205), "c": (40, 200, 230), "C": (190, 250, 255), "m": (70, 74, 84)}),
    "latex_goo": ([
        "................", "................", "................", "................",
        "......bbbb......", ".....bwbbbb.....", "....bwbbbbbb....", "...bbbbbbbbbb...",
        "...bbbbbbbbbb...", "..bbbbbbbbbbbb..", "..bbbbbbbbbbbb..", "..b.bb.bbbb.bb..",
        "....b...bb......", "........b.......", "................", "................",
    ], {"b": (22, 22, 28), "w": (110, 110, 130)}),
    "white_latex_goo": ([
        "................", "................", "................", "................",
        "......bbbb......", ".....bwbbbb.....", "....bwbbbbbb....", "...bbbbbbbbbb...",
        "...bbbbbbbbbd...", "..bbbbbbbbbbbd..", "..bbbbbbbbbbbd..", "..b.bb.bbbd.bd..",
        "....b...bd......", "........d.......", "................", "................",
    ], {"b": (232, 232, 240), "w": (255, 255, 255), "d": (190, 194, 210)}),
    "latex_antidote": ([
        "................", "......kk........", "......gg........", ".....g..g.......",
        "....g....g......", "...g.cccc.g.....", "...gccCcccg.....", "...gcCccccg.....",
        "...gccccccg.....", "...gccccccg.....", "....gccccg......", ".....gggg.......",
        "................", "................", "................", "................",
    ], {"k": (120, 80, 50), "g": (200, 230, 240), "c": (40, 210, 220), "C": (200, 255, 255)}),
    "latex_injector": ([
        "...............n", "..............n.", ".............m..", "............gm..",
        "...........gbg..", "..........gbbg..", ".........gbbg...", "........gbbg....",
        ".......gbbg.....", "......gbbg......", ".....mggg.......", "....mm.m........",
        "...m..m.........", "..mm............", ".mm.............", "................",
    ], {"n": (220, 225, 235), "m": (90, 94, 104), "g": (190, 230, 245), "b": (25, 20, 35)}),
    "empty_syringe": ([
        "...............n", "..............n.", ".............m..", "............gm..",
        "...........g.g..", "..........g..g..", ".........g..g...", "........g..g....",
        ".......g..g.....", "......g..g......", ".....mggg.......", "....mm.m........",
        "...m..m.........", "..mm............", ".mm.............", "................",
    ], {"n": (220, 225, 235), "m": (90, 94, 104), "g": (190, 230, 245)}),
    "syringe_dart": ([
        "................", "................", "................", "...............n",
        "..............n.", ".............m..", "............gm..", "...........gpg..",
        "..........gppg..", ".........gppg...", "........mggg....", ".......f.m......",
        "......ff........", ".....f..........", "................", "................",
    ], {"n": (220, 225, 235), "m": (90, 94, 104), "g": (190, 230, 245), "p": (150, 60, 230), "f": (230, 60, 120)}),
    "purifier_charge": ([
        "................", "................", "................", "......cccc......",
        ".....cCCccc.....", "....cCWWCccc....", "....cCWCcccc....", "....ccCccccc....",
        "....cccccccc....", "....cccccccc....", ".....cccccc.....", "......cccc......",
        "................", "................", "................", "................",
    ], {"c": (30, 180, 220), "C": (120, 230, 250), "W": (255, 255, 255)}),
    "hypno_crystal_shard": ([
        "................", ".........P......", "........PpP.....", "........PpP.....",
        ".......PpppP....", ".......PpWpP....", "......PppWppP...", "......PpppppP...",
        ".....PppppppP...", ".....PpppppP....", "......PppppP....", ".......PppP.....",
        "........PP......", "................", "................", "................",
    ], {"p": (170, 50, 220), "P": (90, 20, 130), "W": (255, 190, 255)}),
}


def item_textures():
    for name, (rows, pal) in ITEM_ART.items():
        art(rows, pal).save(f"{TI}/{name}.png")


# ------------------------------------------------------------------ spawn eggs (each one hand-styled)
def egg_mask():
    m = set()
    for y in range(16):
        for x in range(16):
            ny = (y - 8.5) / 7.0
            rx = 5.6 * (1 - 0.28 * max(0, -ny))
            if ((x - 7.5) / rx) ** 2 + ny ** 2 <= 1.0:
                m.add((x, y))
    return m


EGG = egg_mask()


def egg(primary, secondary, glow, seed, kind):
    rnd = random.Random(seed)
    img = new()
    for (x, y) in EGG:
        k = int(18 * (0.5 - y / 15)) - int(10 * abs(x - 7.5) / 6) + rnd.randint(-4, 4)
        img.putpixel((x, y), sh(primary, k) + (255,))
    # outline
    for (x, y) in EGG:
        if any((x + dx, y + dy) not in EGG for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            img.putpixel((x, y), sh(primary, -45 if sum(primary) > 120 else -20) + (255,))
    if kind == "drone":
        for x in range(3, 13):
            if (x, 8) in EGG:
                img.putpixel((x, 7), (12, 12, 16, 255))
                img.putpixel((x, 8), glow + (255,))
                img.putpixel((x, 9), (12, 12, 16, 255))
        for (x, y) in ((7, 12), (8, 12), (7, 13), (8, 13)):
            img.putpixel((x, y), sh(secondary, 10) + (255,))
    elif kind == "scientist":
        for (x, y) in EGG:
            if y >= 9 and (x + y + seed.__len__()) % 5 == 0:
                img.putpixel((x, y), sh(secondary, rnd.randint(-5, 5)) + (255,))
        for (x, y) in ((5, 7), (6, 7), (9, 7), (10, 7)):
            img.putpixel((x, y), glow + (255,))
        for x in range(4, 12):
            img.putpixel((x, 6), (30, 30, 36, 255))
        for (x, y) in ((7, 11), (8, 11), (7, 12), (8, 12), (6, 12), (9, 12), (7, 13), (8, 13)):
            img.putpixel((x, y), (190, 30, 40, 255))  # red cross
        img.putpixel((7, 12), (240, 240, 240, 255))
    else:
        # dripping cap of secondary colour + glowing creature eyes
        for (x, y) in EGG:
            if y <= 4:
                img.putpixel((x, y), sh(secondary, rnd.randint(-5, 5)) + (255,))
        for x in range(2, 14):
            if rnd.random() < 0.5:
                for y in range(5, 5 + rnd.randint(1, 4)):
                    if (x, y) in EGG:
                        img.putpixel((x, y), sh(secondary, rnd.randint(-5, 5)) + (255,))
        for _ in range(4):
            p = rnd.choice(sorted(EGG))
            if p[1] > 10:
                img.putpixel(p, sh(secondary, 8) + (255,))
        for (x, y) in ((5, 8), (6, 8), (9, 8), (10, 8)):
            img.putpixel((x, y), glow + (255,))
    # gloss
    for (x, y) in ((5, 3), (4, 4), (4, 5)):
        if (x, y) in EGG:
            img.putpixel((x, y), (255, 255, 255, 200))
    return img


def egg_textures():
    for f in FORMS:
        kind = "drone" if f["style"] in ("drone", "hypno") else "beast"
        egg(f["primary"], f["secondary"], f["glow"], f["id"], kind).save(f"{TI}/{f['id']}_spawn_egg.png")
    for s in SCIENTISTS:
        egg((238, 240, 242), s["fur"], s["glow"], s["id"], "scientist").save(f"{TI}/{s['id']}_spawn_egg.png")


# ------------------------------------------------------------------ effect icons, spiral, mod icon
def misc():
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    for y in range(18):
        for x in range(18):
            a = math.atan2(y - 8.5, x - 8.5)
            r = math.hypot(x - 8.5, y - 8.5)
            if r < 8.5:
                v = math.sin(a * 2 + r * 1.2)
                img.putpixel((x, y), ((240, 80, 240) if v > 0 else (70, 10, 110)) + (255,))
    img.save(ROOT + "/textures/mob_effect/hypnosis.png")
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse((3, 2, 14, 13), fill=(20, 20, 26, 255))
    d.rectangle((5, 11, 7, 16), fill=(20, 20, 26, 255))
    d.rectangle((10, 11, 11, 15), fill=(20, 20, 26, 255))
    d.point([(6, 5), (7, 4)], fill=(140, 140, 170, 255))
    d.point([(6, 8), (11, 8)], fill=(51, 230, 255, 255))
    img.save(ROOT + "/textures/mob_effect/latex_infection.png")

    S = 256
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    px = img.load()
    for y in range(S):
        for x in range(S):
            dx, dy = x - S / 2 + 0.5, y - S / 2 + 0.5
            r = math.hypot(dx, dy)
            if r > S / 2:
                continue
            a = math.atan2(dy, dx)
            v = math.sin(2 * a + math.log(r + 1) * 9.0)
            edge = max(0.0, 1 - r / (S / 2))
            alpha = int(255 * min(1, edge * 2.2))
            if v > 0:
                px[x, y] = (235, 70, 235, alpha)
            else:
                px[x, y] = (40, 0, 70, int(alpha * 0.85))
    img.save(ROOT + "/textures/gui/hypno_spiral.png")

    # mod icon: drone face
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            icon.putpixel((x, y), sh((22, 22, 28), int(10 * (0.5 - y / 15))) + (255,))
    for x in range(16):
        for y in (5, 6, 7):
            icon.putpixel((x, y), (8, 8, 10, 255))
        icon.putpixel((x, 6), (51, 230, 255, 255))
    for x in range(4, 12):
        icon.putpixel((x, 12), (44, 44, 54, 255))
    for (x, y) in ((3, 2), (4, 1), (11, 1), (12, 2)):
        icon.putpixel((x, y), (90, 90, 110, 255))
    icon.resize((128, 128), Image.NEAREST).save(ROOT + "/icon.png")


if __name__ == "__main__":
    block_textures()
    item_textures()
    egg_textures()
    misc()
    print("icons done")
