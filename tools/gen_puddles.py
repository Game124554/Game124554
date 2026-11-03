#!/usr/bin/env python3
"""Generates puddle textures, models and the blockstate (4 shapes x 16 neighbour combinations).
Usage: python3 tools/gen_puddles.py   (needs Pillow)"""
import json, math, os, random
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'realisticoverhaul')
TEX = os.path.join(ROOT, 'textures', 'block', 'puddle')
MOD = os.path.join(ROOT, 'models', 'block', 'puddle')
os.makedirs(TEX, exist_ok=True)
os.makedirs(MOD, exist_ok=True)
N = 32  # texture size
EDGE_HALF = 9.5  # half width of the joined part at a shared edge (seamless across blocks)


def blob_radius(shape, ang):
    r = random.Random(shape * 7919)
    k = [(r.uniform(0.4, 1.1), r.randint(2, 5), r.uniform(0, 6.28)) for _ in range(3)]
    return 11.5 + sum(a * math.sin(f * ang + p) for a, f, p in k)


def inside(shape, n, e, s, w, x, y):
    cx = cy = N / 2
    dx, dy = x + 0.5 - cx, y + 0.5 - cy
    if math.hypot(dx, dy) < blob_radius(shape, math.atan2(dy, dx)):
        return True
    wob = lambda t: 1.4 * math.sin(t * 0.55 + shape)
    # arms reaching the connected edges; at the border the width is fixed so blocks join cleanly
    if n and dy <= 0 and abs(dx) < EDGE_HALF + wob(dy) * min(1, -dy / 8 + 0.0) * (1 if -dy < 15 else 0) + (0 if -dy < 15 else 0):
        return True
    if s and dy >= 0 and abs(dx) < EDGE_HALF + wob(dy) * min(1, dy / 8) * (1 if dy < 15 else 0):
        return True
    if w and dx <= 0 and abs(dy) < EDGE_HALF + wob(dx) * min(1, -dx / 8) * (1 if -dx < 15 else 0):
        return True
    if e and dx >= 0 and abs(dy) < EDGE_HALF + wob(dx) * min(1, dx / 8) * (1 if dx < 15 else 0):
        return True
    # fill the corner between two joined sides
    if n and e and dx >= 0 and dy <= 0 and dx * -dy < 40: return True
    if n and w and dx <= 0 and dy <= 0 and dx * dy < 40: return True
    if s and e and dx >= 0 and dy >= 0 and dx * dy < 40: return True
    if s and w and dx <= 0 and dy >= 0 and -dx * dy < 40: return True
    return False


def texture(shape, n, e, s, w):
    img = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    mask = [[inside(shape, n, e, s, w, x, y) for x in range(N)] for y in range(N)]
    rnd = random.Random(shape * 31 + n * 2 + e * 4 + s * 8 + w * 16)
    for y in range(N):
        for x in range(N):
            if not mask[y][x]:
                continue
            # distance to the dry edge (inside this tile; joined sides count as wet)
            edge = 9
            for r in range(1, 4):
                for ox, oy in ((r, 0), (-r, 0), (0, r), (0, -r)):
                    xx, yy = x + ox, y + oy
                    out = xx < 0 or yy < 0 or xx >= N or yy >= N
                    if out:
                        if (xx < 0 and w) or (xx >= N and e) or (yy < 0 and n) or (yy >= N and s):
                            continue
                        edge = min(edge, r)
                    elif not mask[yy][xx]:
                        edge = min(edge, r)
            # sky reflection: bright, slightly streaky; darker wet rim at the edge
            streak = 0.5 + 0.5 * math.sin((x * 0.35 + y * 0.9) + rnd.uniform(-0.4, 0.4))
            v = 205 + int(30 * streak) + rnd.randint(-6, 6)
            a = 165
            if edge == 1:
                v, a = 110, 120
            elif edge == 2:
                v, a = 160, 150
            v = max(0, min(255, v))
            img.putpixel((x, y), (v, min(255, v + 6), min(255, v + 12), a))
    return img


variants = {}
for shape in range(4):
    for bits in range(16):
        n, e, s, w = (bits >> 0) & 1, (bits >> 1) & 1, (bits >> 2) & 1, (bits >> 3) & 1
        name = f'puddle_{shape}_{bits}'
        texture(shape, n, e, s, w).save(os.path.join(TEX, name + '.png'))
        model = {
            "ambientocclusion": False,
            "textures": {"particle": "minecraft:block/water_still", "puddle": f"realisticoverhaul:block/puddle/{name}"},
            "elements": [{
                "from": [0, 0, 0], "to": [16, 0.2, 16],
                "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#puddle", "tintindex": 0}}
            }]
        }
        with open(os.path.join(MOD, name + '.json'), 'w') as f:
            json.dump(model, f, indent=1)
        key = f'east={str(bool(e)).lower()},north={str(bool(n)).lower()},shape={shape},south={str(bool(s)).lower()},west={str(bool(w)).lower()}'
        variants[key] = {"model": f"realisticoverhaul:block/puddle/{name}"}

with open(os.path.join(ROOT, 'blockstates', 'puddle.json'), 'w') as f:
    json.dump({"variants": variants}, f, indent=1)
print('generated', len(variants), 'puddle variants')
