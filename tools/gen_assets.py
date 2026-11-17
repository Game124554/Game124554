"""Textures, models, blockstates, lang, loot tables, recipes and tags for Changed: Thunder Science Expansion."""
import json
import os
import random
import sys

from PIL import Image

MOD = "changed_tsc"
ROOT = sys.argv[1]
RES = os.path.join(ROOT, "src/main/resources")
ASSETS = os.path.join(RES, "assets", MOD)
DATA = os.path.join(RES, "data", MOD)


def wjson(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def jitter(c, rng, amt=10):
    return tuple(max(0, min(255, v + rng.randint(-amt, amt))) for v in c[:3]) + (c[3],)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


# =====================================================================================  human skins

SKIN = ["#F2D3B1", "#E8B98F", "#D9A066", "#C68642", "#A0673C", "#8D5524", "#6B3E26", "#FFDFC4"]
HAIR = ["#2B1B0E", "#4A2F1A", "#6B4423", "#A0522D", "#D6B370", "#1C1C1C", "#8C8C8C", "#B03A2E", "#E8E0D0"]
EYES = ["#3B6EA5", "#4A7C3A", "#5C3B1E", "#2E2E2E", "#6E8B9C"]


def region(img, x, y, w, h, color, rng=None, amt=6):
    for i in range(x, x + w):
        for j in range(y, y + h):
            img.putpixel((i, j), jitter(color, rng, amt) if rng else color)


def paint_human(kind, outfit, variant):
    person = random.Random(f"{kind}-{variant}")      # same face for both outfit colors
    rng = random.Random(f"{kind}-{variant}-{outfit}")
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    skin = hexc(SKIN[person.randrange(len(SKIN))])
    hair = hexc(HAIR[person.randrange(len(HAIR))])
    eye = hexc(EYES[person.randrange(len(EYES))])
    style = person.choice(["short", "short", "long", "bald", "bun", "cap"])
    glasses = kind == "scientist" and person.random() < 0.5

    dark = outfit == "black"
    if kind == "guard":
        main = hexc("#1E1F24") if dark else hexc("#D9DCE1")
        second = hexc("#2D3038") if dark else hexc("#B8BEC7")
        pants, shoes, accent = main, hexc("#111111"), hexc("#F2C230")
    elif kind == "scientist":
        main = hexc("#202226") if dark else hexc("#F4F4F4")   # lab coat
        second = hexc("#3A7BD5") if person.random() < 0.5 else hexc("#7A9E7E")  # shirt
        pants, shoes, accent = hexc("#33363D") if not dark else hexc("#4A4D55"), hexc("#2A1E14"), hexc("#C0392B")
    else:  # assimilation staff jumpsuit
        main = hexc("#2A2A2E") if dark else hexc("#E6E6E6")
        second = hexc("#F28C28")
        pants, shoes, accent = main, hexc("#1A1A1A"), hexc("#F28C28")

    # ---- head (0,0 .. 32,16)
    region(img, 0, 0, 32, 16, skin, rng, 5)
    region(img, 8, 0, 8, 8, hair if style != "bald" else skin, rng, 8)        # top
    region(img, 24, 8, 8, 8 if style in ("long", "bun") else 4, hair, rng, 8) if style != "bald" else None
    if style != "bald":
        side_rows = 6 if style == "long" else 3
        region(img, 0, 8, 8, side_rows, hair, rng, 8)
        region(img, 16, 8, 8, side_rows, hair, rng, 8)
        region(img, 8, 8, 8, 1 if style != "cap" else 2, hair if style != "cap" else main, rng, 8)
        region(img, 24, 8, 8, 3, hair, rng, 8)
    if style == "cap":
        region(img, 8, 0, 8, 8, main, rng, 4)
        region(img, 8, 8, 8, 2, main, rng, 4)
    # face
    for ex in (9, 13):
        img.putpixel((ex, 12), (255, 255, 255, 255))
        img.putpixel((ex + 1, 12), eye)
    img.putpixel((11, 14), shade(skin, 0.75)); img.putpixel((12, 14), shade(skin, 0.75))
    img.putpixel((11, 13), shade(skin, 0.88)); img.putpixel((12, 13), shade(skin, 0.88))
    if glasses:
        for gx in range(8, 16):
            img.putpixel((gx, 11), hexc("#222222"))
        for gx in (8, 11, 12, 15):
            img.putpixel((gx, 12), hexc("#222222"))

    # ---- body (16,16 .. 40,32)
    region(img, 16, 16, 24, 16, main, rng, 6)
    if kind == "scientist":
        region(img, 23, 20, 2, 12, second, rng, 5)       # shirt under the open coat
        img.putpixel((23, 21), accent); img.putpixel((24, 21), accent)  # tie
        img.putpixel((23, 22), accent); img.putpixel((24, 23), accent)
        img.putpixel((21, 23), hexc("#3A7BD5"))           # badge
    elif kind == "guard":
        region(img, 20, 29, 8, 1, hexc("#0D0D0D"))        # belt
        img.putpixel((22, 23), accent); img.putpixel((22, 22), accent)  # badge
        region(img, 25, 22, 2, 2, second, rng, 4)
    else:
        region(img, 16, 24, 24, 1, accent)                # hazard stripe all around
        region(img, 23, 20, 2, 4, second, rng, 4)
        img.putpixel((21, 22), hexc("#FFFFFF") if dark else hexc("#222222"))
    # ---- right arm (40,16 .. 56,32)
    region(img, 40, 16, 16, 16, main, rng, 6)
    hand = hexc("#151515") if kind == "guard" else skin
    region(img, 40, 29, 16, 3, hand, rng, 5)
    region(img, 48, 16, 4, 4, hand, rng, 5)               # arm bottom = hand
    if kind == "staff":
        region(img, 40, 24, 16, 1, accent)
    # ---- right leg (0,16 .. 16,32)
    region(img, 0, 16, 16, 16, pants, rng, 6)
    region(img, 0, 30, 16, 2, shoes, rng, 4)
    region(img, 8, 16, 4, 4, shoes, rng, 4)
    if kind == "scientist":   # coat tail covers the top of the legs
        region(img, 0, 20, 16, 3, main, rng, 5)
    # ---- copy to left limbs (modern 64x64 layout)
    img.paste(img.crop((40, 16, 56, 32)), (32, 48))
    img.paste(img.crop((0, 16, 16, 32)), (16, 48))
    # helmet-style hat overlay for guards without a cap
    return img


def gen_skins():
    out = os.path.join(ASSETS, "textures/entity/human")
    os.makedirs(out, exist_ok=True)
    n = 0
    for kind in ("guard", "scientist", "staff"):
        for outfit in ("white", "black"):
            for v in range(20):
                paint_human(kind, outfit, v).save(os.path.join(out, f"{kind}_{outfit}_{v}.png"))
                n += 1
    return n


# =====================================================================================  pixel-art items

def pix(art, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(art):
        for x, ch in enumerate(row):
            if ch != "." and ch in palette:
                img.putpixel((x, y), palette[ch])
    return img


GUN_PAL = {"g": hexc("#5B5F66"), "d": hexc("#2F3237"), "l": hexc("#8A9099"), "y": hexc("#F2C230"),
           "b": hexc("#6B4423"), "k": hexc("#1A1A1A")}

ITEMS = {
    "tsc_pistol": ([
        "................",
        "................",
        "................",
        "................",
        "..lllllllllll...",
        "..gggggggggggd..",
        "..ddddddgggggd..",
        "........yddkd...",
        ".........dd.k...",
        ".........ddkk...",
        "........ddd.....",
        "........ddd.....",
        "........ddd.....",
        "................",
        "................",
        "................"], GUN_PAL),
    "tsc_assault_rifle": ([
        "................",
        "................",
        "................",
        "................",
        "......ddd.......",
        ".lllllllllllll..",
        "ggggggggggggggdd",
        ".dddddddgggggddd",
        ".......kddyd.ddd",
        "........dd.k..dd",
        "........dd.....d",
        ".......ddd......",
        "................",
        "................",
        "................",
        "................"], GUN_PAL),
    "tsc_shotgun": ([
        "................",
        "................",
        "................",
        "................",
        "................",
        "lllllllllll.....",
        "gggggggggggbbbb.",
        "ddddddddddbbbbbb",
        "......kdyd..bbbb",
        "......dd.k....bb",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................"], GUN_PAL),
    "bullet": ([
        "................",
        "................",
        "................",
        "................",
        "......c.........",
        ".....ccy........",
        ".....yyy........",
        ".....yyy..c.....",
        ".....yyy.ccy....",
        ".....oyy.yyy....",
        ".........yyy....",
        ".........yyy....",
        ".........oyy....",
        "................",
        "................",
        "................"], {"c": hexc("#B87333"), "y": hexc("#E3B341"), "o": hexc("#9C7A2B")}),
    "stun_baton": ([
        "................",
        "............yy..",
        "...........yYy..",
        "..........yYy...",
        ".........kky....",
        "........kkk.....",
        ".......kkk......",
        "......kkk.......",
        ".....kkk........",
        "....gkk.........",
        "...ggg..........",
        "..ggg...........",
        ".gg.............",
        "................",
        "................",
        "................"], {"y": hexc("#F2C230"), "Y": hexc("#FFF6B0"), "k": hexc("#1E1E1E"), "g": hexc("#555555")}),
    "purifier_blade": ([
        "..............WW",
        ".............WcW",
        "............WcW.",
        "...........WcW..",
        "..........WcW...",
        ".........WcW....",
        "........WcW.....",
        ".......WcW......",
        "..y...WcW.......",
        "..yy.WcW........",
        "...yycW.........",
        "....yy..........",
        "...kyyy.........",
        "..kk..y.........",
        ".kk.............",
        "kk.............."], {"W": hexc("#EAFBFF"), "c": hexc("#5FD8F0"), "y": hexc("#F2C230"), "k": hexc("#2A2A2A")}),
    "first_aid_kit": ([
        "................",
        "................",
        "......kkkk......",
        "......k..k......",
        "..gggggggggggg..",
        "..gGGGGGGGGGGg..",
        "..gGGGGwwGGGGg..",
        "..gGGGGwwGGGGg..",
        "..gGGwwwwwwGGg..",
        "..gGGwwwwwwGGg..",
        "..gGGGGwwGGGGg..",
        "..gGGGGwwGGGGg..",
        "..gGGGGGGGGGGg..",
        "..gggggggggggg..",
        "................",
        "................"], {"g": hexc("#1E7A3A"), "G": hexc("#2EA052"), "w": hexc("#FFFFFF"), "k": hexc("#333333")}),
    "bandage": ([
        "................",
        "................",
        "................",
        "................",
        ".....wwwwww.....",
        "....wWWWWWWw....",
        "...wWWwwwwWWw...",
        "...wWw....wWw...",
        "...wWw....wWw...",
        "...wWWwwwwWWw...",
        "....wWWWWWWwwww.",
        ".....wwwwwwWWWw.",
        "...........wwww.",
        "................",
        "................",
        "................"], {"w": hexc("#C9C2B0"), "W": hexc("#F5F1E6")}),
}


def armor_icon(piece, base, trim, visor):
    shapes = {
        "helmet": ["................", "................", "....bbbbbbbb....", "...bbbbbbbbbb...", "...bbttttttbb...",
                   "...bvvvvvvvvb...", "...bvvvvvvvvb...", "...bb......bb...", "................"],
        "chestplate": ["................", "..bbb......bbb..", "..bbbbbbbbbbbb..", "..bbbbbttbbbbb..", "...bbbbttbbbbb..",
                       "....bbbbbbbb....", "....bbbbbbbb....", "....bbttttbb....", "....bbbbbbbb....",
                       "....bbbbbbbb....", "................"],
        "leggings": ["................", "....bbbbbbbb....", "....tttttttt....", "....bbbbbbbb....", "....bbb..bbb....",
                     "....bbb..bbb....", "....bbb..bbb....", "....bbb..bbb....", "....bbb..bbb....",
                     "....bbb..bbb....", "................"],
        "boots": ["................", "................", "................", "................", "...bbb....bbb...",
                  "...bbb....bbb...", "...bbb....bbb...", "..tbbb...tbbb...", "..bbbb...bbbb...",
                  "................"],
    }
    art = shapes[piece] + ["................"] * (16 - len(shapes[piece]))
    pal = {"b": base, "t": trim, "v": visor}
    img = pix(art, pal)
    rng = random.Random(piece)
    for x in range(16):
        for y in range(16):
            p = img.getpixel((x, y))
            if p[3]:
                img.putpixel((x, y), jitter(p, rng, 8))
    return img


def orb(c1, c2, seed, shape="orb"):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if shape == "orb" and d <= 6.2:
                f = 1.25 - d / 7.0
                col = c1 if (x + y + rng.randint(0, 2)) % 5 else c2
                img.putpixel((x, y), shade(col, f))
            elif shape == "gem" and abs(x - 7.5) + abs(y - 7.5) <= 7:
                f = 1.2 - (abs(x - 7.5) + abs(y - 7.5)) / 10
                img.putpixel((x, y), shade(c1 if (x > 7) ^ (y > 7) else c2, f))
    img.putpixel((5, 4), (255, 255, 255, 255)); img.putpixel((6, 4), (255, 255, 255, 200))
    return img


def gen_items():
    out = os.path.join(ASSETS, "textures/item")
    os.makedirs(out, exist_ok=True)
    for name, (art, pal) in ITEMS.items():
        pix(art, pal).save(os.path.join(out, name + ".png"))
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        armor_icon(piece, hexc("#26272C"), hexc("#F2C230"), hexc("#2B4A6E")).save(os.path.join(out, f"guard_{piece}.png"))
        armor_icon(piece, hexc("#EEEEEA"), hexc("#F28C28"), hexc("#8ED6F0")).save(os.path.join(out, f"hazmat_{piece}.png"))
    # boss summons (gems) and cores (orbs) – abstract objects
    for boss, c1, c2 in (("squid_dog", "#F4F4F4", "#1B1B1B"), ("tiger_shark", "#3C7DD9", "#E8933A"),
                         ("elder_one", "#7D3CC9", "#E9E0FF"), ("wolf_king", "#2A2A2E", "#C0392B")):
        orb(hexc(c1), hexc(c2), boss + "s", "gem").save(os.path.join(out, f"{boss}_summon.png"))
        orb(hexc(c1), hexc(c2), boss + "c", "orb").save(os.path.join(out, f"{boss}_core.png"))


def gen_blocks():
    out = os.path.join(ASSETS, "textures/block")
    os.makedirs(out, exist_ok=True)
    for name, base, grout in (("lab_tile_white", "#EDEFF2", "#B9BEC6"), ("lab_tile_black", "#232428", "#3D3F46")):
        rng = random.Random(name)
        img = Image.new("RGBA", (16, 16))
        for x in range(16):
            for y in range(16):
                g = x % 8 == 0 or y % 8 == 0
                img.putpixel((x, y), jitter(hexc(grout if g else base), rng, 4))
        img.save(os.path.join(out, name + ".png"))
    rng = random.Random("emblem")
    img = Image.new("RGBA", (16, 16))
    bolt = ["......####", ".....####.", "....####..", "...####...", "..########", ".....####.", "....####..",
            "...####...", "..####....", ".###......", ".##.......", "##........"]
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), jitter(hexc("#1C1D21"), rng, 3))
    for y, row in enumerate(bolt):
        for x, ch in enumerate(row):
            if ch == "#":
                img.putpixel((x + 3, y + 2), jitter(hexc("#F2C230"), rng, 6))
    for i in range(16):
        for p in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel(p, hexc("#F2C230"))
    img.save(os.path.join(out, "tsc_emblem_block.png"))


def gen_armor_layers():
    out = os.path.join(ASSETS, "textures/models/armor")
    os.makedirs(out, exist_ok=True)
    for name, base, trim, visor in (("guard", "#26272C", "#F2C230", "#2B4A6E"), ("hazmat", "#EEEEEA", "#F28C28", "#8ED6F0")):
        rng = random.Random(name)
        l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        region(l1, 0, 0, 32, 16, hexc(base), rng, 6)          # helmet
        region(l1, 8, 10, 8, 4, hexc(visor), rng, 6)          # visor on the face
        region(l1, 8, 9, 8, 1, hexc(trim))
        region(l1, 16, 16, 24, 16, hexc(base), rng, 6)        # chest
        region(l1, 20, 22, 8, 1, hexc(trim))
        region(l1, 40, 16, 16, 16, hexc(base), rng, 6)        # arms
        region(l1, 40, 18, 16, 1, hexc(trim))
        region(l1, 0, 16, 16, 16, hexc(base), rng, 6)         # boots
        region(l1, 0, 26, 16, 1, hexc(trim))
        l1.save(os.path.join(out, f"{name}_layer_1.png"))
        l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        region(l2, 16, 16, 24, 16, hexc(base), rng, 6)        # belt area
        region(l2, 0, 16, 16, 16, hexc(base), rng, 6)         # legs
        region(l2, 0, 21, 16, 1, hexc(trim))
        l2.save(os.path.join(out, f"{name}_layer_2.png"))


# =====================================================================================  goo infection visuals

# outer-layer boxes of the 64x64 player skin: (u, v, w, h, d, body part)
OUTER_BOXES = [(32, 0, 8, 8, 8, "head"), (16, 32, 8, 12, 4, "torso"), (40, 32, 4, 12, 4, "arm"),
               (48, 48, 4, 12, 4, "arm"), (0, 32, 4, 12, 4, "leg"), (0, 48, 4, 12, 4, "leg")]
# goo starts at the hands and feet, creeps up the limbs, then the torso, and the head last
BIAS = {"arm": (0.30, 0.50), "leg": (0.22, 0.32), "torso": (0.18, 0.25), "head": (0.02, 0.30)}
THRESH = {1: 0.74, 2: 0.50, 3: 0.27}


def box_faces(u, v, w, h, d):
    """Yield (x, y, frac) for every pixel of a box; frac = 0 at the top of the part, 1 at the bottom."""
    for x in range(u + d, u + d + w):            # top face
        for y in range(v, v + d):
            yield x, y, 0.0
    for x in range(u + d + w, u + d + 2 * w):    # bottom face
        for y in range(v, v + d):
            yield x, y, 1.0
    for x in range(u, u + 2 * d + 2 * w):        # four side faces
        for y in range(v + d, v + d + h):
            yield x, y, (y - v - d + 0.5) / h


def paint_goo(stage, white):
    rng = random.Random(f"goo-{white}")           # same noise for every stage, so goo only ever grows
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    base = hexc("#E9E9EE") if white else hexc("#15151A")
    fur_a = hexc("#FFFFFF") if white else hexc("#2A2A33")
    fur_b = hexc("#C9C9D2") if white else hexc("#0A0A0D")
    shine = hexc("#FFFFFF") if white else hexc("#5A5A6A")
    for (u, v, w, h, d, part) in OUTER_BOXES:
        b0, b1 = BIAS[part]
        for x, y, frac in box_faces(u, v, w, h, d):
            value = b0 + b1 * frac + rng.random() * 0.32
            if part == "head" and 36 <= x < 48 and v + 8 + 3 <= y <= v + 8 + 4:
                value -= 1.0                      # keep the eyes clear
            if value < THRESH[stage]:
                continue
            if stage >= 2:                        # furry streaks
                c = fur_a if (x + y // 2) % 3 == 0 else (fur_b if (x * 7 + y) % 5 == 0 else base)
            else:
                c = base
            if rng.random() < 0.06:
                c = shine
            alpha = 235 if stage == 1 else 250
            img.putpixel((x, y), c[:3] + (alpha,))
    return img


def gen_goo():
    out = os.path.join(ASSETS, "textures/entity/goo")
    os.makedirs(out, exist_ok=True)
    for white in (False, True):
        for stage in (1, 2, 3):
            paint_goo(stage, white).save(os.path.join(out, f"goo_{'white' if white else 'black'}_{stage}.png"))
    # effect icons 18x18
    eff = os.path.join(ASSETS, "textures/mob_effect")
    os.makedirs(eff, exist_ok=True)
    drop = ["........##........", ".......####.......", "......######......", ".....########.....",
            "....##########....", "...############...", "...#####..######..", "..######..#######.",
            "..##############..", "..##############..", "...############...", "....##########....",
            ".....########....."]
    icons = {
        "goo_inside": ("#1A1A20", "#F2D3B1"),      # dark drop inside a skin-coloured circle (human body)
        "furry_body": ("#3A3A44", "#9A9AA8"),      # fur tuft
        "transfurring": ("#0B0B0F", "#C0392B"),    # dark drop with red warning ring
    }
    for name, (c1, c2) in icons.items():
        img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
        rng = random.Random(name)
        for x in range(18):
            for y in range(18):
                dd = ((x - 8.5) ** 2 + (y - 8.5) ** 2) ** 0.5
                if name == "goo_inside" and dd <= 8.5:
                    img.putpixel((x, y), hexc(c2))
                if name == "transfurring" and 7.2 <= dd <= 8.5:
                    img.putpixel((x, y), hexc(c2))
                if name == "furry_body" and dd <= 8.0 and (x + y) % 3 != 0:
                    img.putpixel((x, y), jitter(hexc(c2 if (x * 3 + y) % 4 == 0 else c1), rng, 10))
        if name != "furry_body":
            for y, row in enumerate(drop):
                for x, ch in enumerate(row):
                    if ch == "#":
                        img.putpixel((x, y + 3), jitter(hexc(c1), rng, 6))
            img.putpixel((6, 9), (255, 255, 255, 200))
        img.save(os.path.join(eff, name + ".png"))
    # syringe item
    art = ["................", "..............kk", ".............k.k", "............gkk.",
           "...........gcg..", "..........gccg..", ".........gccg...", "........gccg....",
           ".......gccg.....", "......gccg......", ".....gccg.......", "....kgg.........",
           "...k.k..........", "..k.............", ".k..............", "k..............."]
    pix(art, {"k": hexc("#9AA3AD"), "g": hexc("#DDEFF5"), "c": hexc("#3FD0E8")}).save(
        os.path.join(ASSETS, "textures/item/anti_goo_syringe.png"))
    # damage type for the goo transfur death
    wjson(os.path.join(DATA, "damage_type/goo_transfur.json"),
          {"message_id": "changed_tsc.goo_transfur", "exhaustion": 0.0, "scaling": "never"})
    for tag in ("bypasses_armor", "bypasses_shield", "bypasses_resistance", "bypasses_enchantments", "no_knockback"):
        wjson(os.path.join(RES, f"data/minecraft/tags/damage_type/{tag}.json"),
              {"replace": False, "values": [f"{MOD}:goo_transfur"]})


# =====================================================================================  models / states / lang

# the three guns have 3D models written by gen_gun_models.py, so they are not listed here
HANDHELD = {"stun_baton", "purifier_blade"}
PLAIN = ["bullet", "first_aid_kit", "bandage", "anti_goo_syringe",
         "guard_helmet", "guard_chestplate", "guard_leggings", "guard_boots",
         "hazmat_helmet", "hazmat_chestplate", "hazmat_leggings", "hazmat_boots",
         "squid_dog_summon", "tiger_shark_summon", "elder_one_summon", "wolf_king_summon",
         "squid_dog_core", "tiger_shark_core", "elder_one_core", "wolf_king_core"]
EGGS = ["tsc_guard_spawn_egg", "tsc_scientist_spawn_egg", "assimilation_staff_spawn_egg"]
BLOCKS = ["lab_tile_white", "lab_tile_black", "tsc_emblem_block"]


def gen_models():
    m = os.path.join(ASSETS, "models")
    for it in HANDHELD:
        wjson(f"{m}/item/{it}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MOD}:item/{it}"}})
    for it in PLAIN:
        wjson(f"{m}/item/{it}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{it}"}})
    for it in EGGS:
        wjson(f"{m}/item/{it}.json", {"parent": "minecraft:item/template_spawn_egg"})
    for b in BLOCKS:
        wjson(f"{m}/block/{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{b}"}})
        wjson(f"{m}/item/{b}.json", {"parent": f"{MOD}:block/{b}"})
        wjson(os.path.join(ASSETS, f"blockstates/{b}.json"), {"variants": {"": {"model": f"{MOD}:block/{b}"}}})
        wjson(os.path.join(DATA, f"loot_tables/blocks/{b}.json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{b}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    wjson(f"{m}/block/spawn_marker.json", {"textures": {"particle": "minecraft:item/barrier"}})
    wjson(os.path.join(ASSETS, "blockstates/spawn_marker.json"), {"variants": {"": {"model": f"{MOD}:block/spawn_marker"}}})
    wjson(os.path.join(RES, "data/minecraft/tags/blocks/mineable/pickaxe.json"),
          {"replace": False, "values": [f"{MOD}:{b}" for b in BLOCKS]})


LANG = {
    "itemGroup.changed_tsc": "Thunder Science Company",
    "item.changed_tsc.tsc_pistol": "TSC Pistol",
    "item.changed_tsc.tsc_assault_rifle": "TSC Assault Rifle",
    "item.changed_tsc.tsc_shotgun": "TSC Shotgun",
    "item.changed_tsc.bullet": "TSC Bullet",
    "item.changed_tsc.stun_baton": "Stun Baton",
    "item.changed_tsc.purifier_blade": "Thunder Purifier Blade",
    "item.changed_tsc.first_aid_kit": "First Aid Kit",
    "item.changed_tsc.bandage": "Bandage",
    "item.changed_tsc.guard_helmet": "Security Helmet",
    "item.changed_tsc.guard_chestplate": "Security Vest",
    "item.changed_tsc.guard_leggings": "Security Pants",
    "item.changed_tsc.guard_boots": "Security Boots",
    "item.changed_tsc.hazmat_helmet": "Hazmat Hood",
    "item.changed_tsc.hazmat_chestplate": "Hazmat Suit",
    "item.changed_tsc.hazmat_leggings": "Hazmat Trousers",
    "item.changed_tsc.hazmat_boots": "Hazmat Boots",
    "item.changed_tsc.squid_dog_summon": "Ink-Stained Beacon",
    "item.changed_tsc.tiger_shark_summon": "Shark Tooth Lure",
    "item.changed_tsc.elder_one_summon": "Elder Seal",
    "item.changed_tsc.wolf_king_summon": "Howling Crown",
    "item.changed_tsc.squid_dog_core": "Squid Dog Core",
    "item.changed_tsc.tiger_shark_core": "Tiger Shark Core",
    "item.changed_tsc.elder_one_core": "Elder One Core",
    "item.changed_tsc.wolf_king_core": "Wolf King Core",
    "item.changed_tsc.tsc_guard_spawn_egg": "TSC Guard Spawn Egg",
    "item.changed_tsc.tsc_scientist_spawn_egg": "TSC Scientist Spawn Egg",
    "item.changed_tsc.assimilation_staff_spawn_egg": "Assimilation Staff Spawn Egg",
    "block.changed_tsc.lab_tile_white": "White Lab Tile",
    "block.changed_tsc.lab_tile_black": "Black Lab Tile",
    "block.changed_tsc.tsc_emblem_block": "Thunder Science Emblem",
    "block.changed_tsc.spawn_marker": "Spawn Marker",
    "entity.changed_tsc.tsc_guard": "TSC Guard",
    "entity.changed_tsc.tsc_scientist": "TSC Scientist",
    "entity.changed_tsc.assimilation_staff": "Assimilation Staff",
    "entity.changed_tsc.bullet": "Bullet",
    "boss.changed_tsc.squid_dog": "Squid Dog",
    "boss.changed_tsc.tiger_shark": "Tiger Shark",
    "boss.changed_tsc.elder_one": "The Elder One",
    "boss.changed_tsc.wolf_king": "Wolf King",
    "message.changed_tsc.no_ammo": "Out of ammo! Craft TSC Bullets.",
    "message.changed_tsc.first_aid_disabled": "First aid is disabled in the config.",
    "message.changed_tsc.boss_failed": "The boss could not be summoned (is Changed installed? try /tsc resolve).",
    "message.changed_tsc.boss_awakens": "%s awakens!",
    "message.changed_tsc.boss_enraged": "%s is enraged!",
    "message.changed_tsc.boss_defeated": "%s has been defeated!",
    "tooltip.changed_tsc.gun_damage": "Damage: %s%s",
    "tooltip.changed_tsc.gun_ammo": "Uses TSC Bullets",
    "tooltip.changed_tsc.heals": "Heals %s hearts",
    "tooltip.changed_tsc.first_aid": "Cures harmful effects. Right-click others to treat them.",
    "tooltip.changed_tsc.purifier": "x%s damage against latex",
    "tooltip.changed_tsc.stun": "Slows and weakens targets",
    "tooltip.changed_tsc.summon": "Use on the ground to summon: %s",
    "tooltip.changed_tsc.goo_cure": "Removes %s%% goo infection",
    "tooltip.changed_tsc.syringe": "Removes ALL goo from the body (causes nausea)",
    "item.changed_tsc.anti_goo_syringe": "Anti-Goo Syringe",
    "effect.changed_tsc.goo_inside": "Goo Inside Body",
    "effect.changed_tsc.furry_body": "Furry Body",
    "effect.changed_tsc.transfurring": "Transfurring",
    "message.changed_tsc.goo.status": "Goo infection: %s%%",
    "message.changed_tsc.goo.stage1": "You feel goo inside your body... (Stage 1: Goo Inside Body)",
    "message.changed_tsc.goo.stage2": "Fur and goo are spreading over your skin! (Stage 2: Furry Body)",
    "message.changed_tsc.goo.stage3": "You are transfurring! Use a First Aid Kit or Anti-Goo Syringe NOW! (Stage 3)",
    "message.changed_tsc.goo.feel1": "Something cold is moving under your skin...",
    "message.changed_tsc.goo.feel2": "Your arms itch. Soft fur is growing where the goo touched you...",
    "message.changed_tsc.goo.feel3": "Your thoughts feel... squishy. Your body is not human anymore...",
    "message.changed_tsc.goo.cured": "The goo is gone. Your body is human again.",
    "message.changed_tsc.goo.clean": "There is no goo in your body.",
    "message.changed_tsc.goo.transfurred": "%s was transfurred by the goo!",
    "entity.changed_tsc.transfurred_name": "Transfurred %s",
    "death.attack.changed_tsc.goo_transfur": "%1$s was transfurred by the goo",
    "death.attack.changed_tsc.goo_transfur.player": "%1$s was transfurred by the goo while fighting %2$s",
}


def gen_lang():
    wjson(os.path.join(ASSETS, "lang/en_us.json"), LANG)


# =====================================================================================  loot tables

def entry(item, weight, lo=1, hi=1, extra=None):
    e = {"type": "minecraft:item", "name": item if ":" in item else f"minecraft:{item}", "weight": weight}
    fns = []
    if hi > 1 or lo > 1:
        fns.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
    if extra:
        fns.append(extra)
    if fns:
        e["functions"] = fns
    return e


def T(name):
    return f"{MOD}:{name}"


DMG = {"function": "minecraft:set_damage", "damage": {"type": "minecraft:uniform", "min": 0.4, "max": 0.9}}

LOOT = {
    "lab": [(2, 5, [entry(T("anti_goo_syringe"), 4), entry(T("bullet"), 10, 4, 12), entry(T("bandage"), 10, 1, 3), entry(T("first_aid_kit"), 4),
                    entry("paper", 10, 2, 6), entry("glass_bottle", 8, 1, 4), entry("redstone", 8, 2, 8),
                    entry("iron_ingot", 6, 1, 4), entry("book", 5, 1, 2), entry("slime_ball", 4, 1, 3),
                    entry(T("hazmat_helmet"), 2), entry(T("hazmat_chestplate"), 1), entry(T("stun_baton"), 2, extra=DMG)])],
    "armory": [(3, 6, [entry(T("bullet"), 16, 8, 24), entry(T("tsc_pistol"), 5, extra=DMG),
                       entry(T("tsc_assault_rifle"), 2, extra=DMG), entry(T("tsc_shotgun"), 3, extra=DMG),
                       entry(T("guard_helmet"), 4, extra=DMG), entry(T("guard_chestplate"), 3, extra=DMG),
                       entry(T("guard_leggings"), 3, extra=DMG), entry(T("guard_boots"), 4, extra=DMG),
                       entry(T("stun_baton"), 4, extra=DMG), entry("gunpowder", 8, 2, 6), entry("iron_ingot", 6, 2, 5)])],
    "medical": [(3, 6, [entry(T("anti_goo_syringe"), 6, 1, 2), entry(T("first_aid_kit"), 12, 1, 2), entry(T("bandage"), 16, 2, 5), entry("golden_apple", 2),
                        entry("honey_bottle", 6, 1, 2), entry("glistering_melon_slice", 5, 1, 3), entry("paper", 6, 1, 4),
                        entry("white_wool", 6, 1, 4), entry(T("hazmat_helmet"), 2), entry(T("hazmat_boots"), 2)])],
    "supply": [(3, 7, [entry("bread", 14, 2, 6), entry("cooked_beef", 8, 1, 4), entry("apple", 8, 1, 4),
                       entry("torch", 8, 4, 12), entry("coal", 8, 2, 8), entry("iron_ingot", 6, 1, 4),
                       entry("copper_ingot", 6, 2, 6), entry(T("bullet"), 10, 4, 16), entry(T("bandage"), 6, 1, 3),
                       entry("string", 6, 2, 6), entry("leather", 4, 1, 3)])],
    "abandoned": [(2, 5, [entry("rotten_flesh", 10, 1, 4), entry("cobweb", 6, 1, 3), entry("paper", 8, 1, 4),
                          entry(T("bullet"), 8, 2, 8), entry(T("bandage"), 6, 1, 2), entry("black_dye", 6, 1, 4),
                          entry("white_dye", 6, 1, 4), entry("slime_ball", 5, 1, 3), entry("iron_nugget", 8, 3, 9),
                          entry(T("tsc_pistol"), 2, extra=DMG), entry(T("first_aid_kit"), 2)])],
    "vault": [(4, 8, [entry("diamond", 8, 1, 4), entry("emerald", 8, 2, 6), entry("gold_ingot", 10, 2, 6),
                      entry(T("tsc_assault_rifle"), 6), entry(T("tsc_shotgun"), 5), entry(T("bullet"), 12, 16, 32),
                      entry(T("first_aid_kit"), 8, 1, 3), entry(T("purifier_blade"), 2), entry(T("anti_goo_syringe"), 6, 1, 3),
                      entry(T("guard_chestplate"), 4), entry(T("hazmat_chestplate"), 4),
                      entry(T("squid_dog_summon"), 1), entry(T("tiger_shark_summon"), 1),
                      entry(T("elder_one_summon"), 1), entry(T("wolf_king_summon"), 1)])],
}


def gen_loot():
    for name, pools in LOOT.items():
        wjson(os.path.join(DATA, f"loot_tables/chests/{name}.json"), {
            "type": "minecraft:chest",
            "pools": [{"rolls": {"type": "minecraft:uniform", "min": lo, "max": hi}, "entries": entries}
                      for lo, hi, entries in pools]})


# =====================================================================================  recipes

def item(i):
    return {"item": i if ":" in i else f"minecraft:{i}"}


def shaped(name, pattern, key, result, count=1, category="equipment"):
    wjson(os.path.join(DATA, f"recipes/{name}.json"), {
        "type": "minecraft:crafting_shaped", "category": category, "pattern": pattern,
        "key": {k: (item(v) if isinstance(v, str) else [item(x) for x in v]) for k, v in key.items()},
        "result": {"item": result if ":" in result else T(result), "count": count}})


def shapeless(name, ingredients, result, count=1, category="misc"):
    wjson(os.path.join(DATA, f"recipes/{name}.json"), {
        "type": "minecraft:crafting_shapeless", "category": category,
        "ingredients": [item(i) for i in ingredients],
        "result": {"item": T(result), "count": count}})


def gen_recipes():
    shaped("tsc_pistol", ["III", " GR", " I "], {"I": "iron_ingot", "G": "gunpowder", "R": "redstone"}, "tsc_pistol")
    shaped("tsc_assault_rifle", ["IIB", "GRI", " I "], {"I": "iron_ingot", "B": "iron_block", "G": "gunpowder",
                                                         "R": "redstone"}, "tsc_assault_rifle")
    shaped("tsc_shotgun", ["III", "GRL", "  L"], {"I": "iron_ingot", "G": "gunpowder", "R": "redstone",
                                                  "L": "spruce_log"}, "tsc_shotgun")
    shapeless("bullet", ["copper_ingot", "gunpowder"], "bullet", 12)
    shaped("stun_baton", ["  R", " I ", "B  "], {"R": "redstone_block", "I": "iron_ingot", "B": "black_wool"}, "stun_baton")
    shaped("purifier_blade", [" C ", " D ", " S "], {"C": [T("squid_dog_core"), T("tiger_shark_core"),
                                                          T("elder_one_core"), T("wolf_king_core")],
                                                    "D": "diamond_sword", "S": "lightning_rod"}, "purifier_blade")
    shaped("first_aid_kit", ["WWW", "PHP", "WWW"], {"W": "white_wool", "P": "paper", "H": "honey_bottle"}, "first_aid_kit")
    shapeless("bandage", ["paper", "white_wool", "string"], "bandage", 4)
    shapeless("anti_goo_syringe", ["glass_bottle", "iron_nugget", "glistering_melon_slice", "lapis_lazuli"], "anti_goo_syringe", 2)
    for prefix, mat, extra in (("guard", "black_wool", "iron_ingot"), ("hazmat", "white_wool", "slime_ball")):
        k = {"M": mat, "X": extra}
        shaped(f"{prefix}_helmet", ["MXM", "M M"], k, f"{prefix}_helmet")
        shaped(f"{prefix}_chestplate", ["M M", "MXM", "MMM"], k, f"{prefix}_chestplate")
        shaped(f"{prefix}_leggings", ["MXM", "M M", "M M"], k, f"{prefix}_leggings")
        shaped(f"{prefix}_boots", ["M M", "X X"], k, f"{prefix}_boots")
    shaped("lab_tile_white", ["CC", "CQ"], {"C": "white_concrete", "Q": "quartz"}, "lab_tile_white", 4, "building")
    shaped("lab_tile_black", ["CC", "CQ"], {"C": "black_concrete", "Q": "quartz"}, "lab_tile_black", 4, "building")
    shaped("tsc_emblem_block", ["BGB", "GLG", "BGB"], {"B": "black_concrete", "G": "gold_ingot", "L": "glowstone"},
           "tsc_emblem_block", 4, "building")
    shaped("squid_dog_summon", ["KPK", "PHP", "KPK"], {"K": "ink_sac", "P": "prismarine_shard",
                                                       "H": "heart_of_the_sea"}, "squid_dog_summon")
    shaped("tiger_shark_summon", ["CPC", "PNP", "CPC"], {"C": "tropical_fish", "P": "prismarine_crystals",
                                                         "N": "nautilus_shell"}, "tiger_shark_summon")
    shaped("elder_one_summon", ["ESE", "SYS", "ESE"], {"E": "echo_shard", "S": "sculk", "Y": "ender_eye"},
           "elder_one_summon")
    shaped("wolf_king_summon", ["BGB", "GDG", "BGB"], {"B": "bone_block", "G": "gold_block", "D": "diamond"},
           "wolf_king_summon")


if __name__ == "__main__":
    n = gen_skins()
    gen_items(); gen_goo(); gen_blocks(); gen_armor_layers(); gen_models(); gen_lang(); gen_loot(); gen_recipes()
    print("skins:", n)
