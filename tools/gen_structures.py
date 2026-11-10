"""Builds the 19 structures as vanilla structure-template .nbt files plus the
worldgen JSON (structure, template_pool, structure_set).  Run:  python3 tools/gen_structures.py
"""
import gzip
import json
import math
import os
import random
import struct

import content as C

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
DATA = os.path.join(ROOT, "src", "main", "resources", "data", C.MODID)
M = C.MODID
DATA_VERSION = 3465  # Minecraft 1.20.1


# ------------------------------------------------------------------ tiny NBT writer
class Byte(int):
    pass


class Short(int):
    pass


class Double(float):
    pass


def _type(v):
    if isinstance(v, Byte):
        return 1
    if isinstance(v, Short):
        return 2
    if isinstance(v, bool):
        raise TypeError("use Byte")
    if isinstance(v, int):
        return 3
    if isinstance(v, Double):
        return 6
    if isinstance(v, float):
        return 6
    if isinstance(v, str):
        return 8
    if isinstance(v, list):
        return 9
    if isinstance(v, dict):
        return 10
    raise TypeError(type(v))


def _payload(out, v):
    t = _type(v)
    if t == 1:
        out.append(struct.pack(">b", v))
    elif t == 2:
        out.append(struct.pack(">h", v))
    elif t == 3:
        out.append(struct.pack(">i", v))
    elif t == 6:
        out.append(struct.pack(">d", v))
    elif t == 8:
        b = v.encode("utf-8")
        out.append(struct.pack(">H", len(b)) + b)
    elif t == 9:
        et = _type(v[0]) if v else 0
        out.append(struct.pack(">bi", et, len(v)))
        for e in v:
            assert _type(e) == et
            _payload(out, e)
    elif t == 10:
        for k, e in v.items():
            b = k.encode("utf-8")
            out.append(struct.pack(">bH", _type(e), len(b)) + b)
            _payload(out, e)
        out.append(b"\x00")


def nbt_bytes(root):
    out = [struct.pack(">bH", 10, 0)]
    _payload(out, root)
    return gzip.compress(b"".join(out), mtime=0)


# ------------------------------------------------------------------ structure DSL
def L(name):
    return f"{M}:{name}"


AIR = "minecraft:air"


def parse(spec):
    if ":" not in spec.split("[")[0]:
        spec = "minecraft:" + spec
    if "[" in spec:
        name, rest = spec.split("[", 1)
        props = dict(p.split("=") for p in rest.rstrip("]").split(","))
    else:
        name, props = spec, {}
    return name, tuple(sorted(props.items()))


class S:
    def __init__(self, sx, sy, sz, seed=1):
        self.size = (sx, sy, sz)
        self.cells = {}
        self.ents = []
        self.rnd = random.Random(seed)
        self.yoff = 0

    def inside(self, x, y, z):
        return 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]

    def set(self, x, y, z, b, nbt=None):
        if self.inside(x, y, z):
            self.cells[(x, y, z)] = (parse(b), nbt)

    def get(self, x, y, z):
        c = self.cells.get((x, y, z))
        return c[0][0] if c else None

    def box(self, x1, y1, z1, x2, y2, z2, b):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, b)

    def shell(self, x1, y1, z1, x2, y2, z2, wall, floor=None, roof=None):
        """Hollow room: walls on the perimeter, floor at y1, roof at y2, air inside."""
        self.box(x1, y1, z1, x2, y2, z2, wall)
        self.box(x1 + 1, y1 + 1, z1 + 1, x2 - 1, y2 - 1, z2 - 1, AIR)
        if floor:
            self.box(x1, y1, z1, x2, y1, z2, floor)
        if roof:
            self.box(x1, y2, z1, x2, y2, z2, roof)

    def sphere(self, cx, cy, cz, r, b, hollow=False, ry=None, only_air=False, ymin=None):
        ry = ry or r
        R = int(math.ceil(max(r, ry))) + 1
        for x in range(int(cx) - R, int(cx) + R + 1):
            for y in range(int(cy) - R, int(cy) + R + 1):
                for z in range(int(cz) - R, int(cz) + R + 1):
                    if ymin is not None and y < ymin:
                        continue
                    d = ((x - cx) / r) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / r) ** 2
                    if d <= 1.0 and (not hollow or d > ((r - 1.15) / r) ** 2):
                        self.set(x, y, z, b)

    def scatter(self, blocks, count, x1, z1, x2, z2, ys):
        """Drop decoration blocks on top of solid cells where the cell above is free air."""
        placed = 0
        for _ in range(count * 12):
            if placed >= count:
                break
            x, z = self.rnd.randint(x1, x2), self.rnd.randint(z1, z2)
            for y in ys:
                below, here = self.get(x, y - 1, z), self.get(x, y, z)
                if below not in (None, AIR) and here in (None, AIR) and not self.deco(below):
                    self.set(x, y, z, self.rnd.choice(blocks))
                    placed += 1
                    break

    @staticmethod
    def deco(name):
        return any(k in name for k in ("puddle", "crystal", "sprout", "tendrils", "chest", "spawner", "ladder", "iron_bars"))

    def mob(self, x, y, z, name):
        self.ents.append((x, y, z, name))

    def chest(self, x, y, z, table, facing="south"):
        self.set(x, y, z, f"minecraft:chest[facing={facing},type=single,waterlogged=false]",
                 {"id": "minecraft:chest", "LootTable": f"{M}:chests/{table}"})

    def spawner(self, x, y, z, mob):
        self.set(x, y, z, "minecraft:spawner", {
            "id": "minecraft:mob_spawner", "Delay": Short(20), "MinSpawnDelay": Short(300), "MaxSpawnDelay": Short(900),
            "SpawnCount": Short(2), "MaxNearbyEntities": Short(5), "RequiredPlayerRange": Short(14),
            "SpawnRange": Short(4), "SpawnData": {"entity": {"id": f"{M}:{mob}"}}})

    def to_nbt(self):
        palette, index, blocks = [], {}, []
        for (x, y, z), (state, nbt) in sorted(self.cells.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            if state not in index:
                index[state] = len(palette)
                entry = {"Name": state[0]}
                if state[1]:
                    entry["Properties"] = {k: v for k, v in state[1]}
                palette.append(entry)
            b = {"pos": [x, y, z], "state": index[state]}
            if nbt:
                b["nbt"] = nbt
            blocks.append(b)
        ents = [{"pos": [Double(x + 0.5), Double(y), Double(z + 0.5)], "blockPos": [x, y, z],
                 "nbt": {"id": f"{M}:{name}", "PersistenceRequired": Byte(1)}} for (x, y, z, name) in self.ents]
        return {"DataVersion": DATA_VERSION, "size": list(self.size), "palette": palette, "blocks": blocks, "entities": ents}


W, WS, TILE, TD, TC, CEIL = L("lab_wall"), L("lab_wall_striped"), L("lab_tile"), L("lab_tile_dark"), L("lab_tile_checker"), L("lab_ceiling")
LIGHT, ELIGHT, GLASS, RGLASS, VENT, HAZ, PIL = L("lab_light"), L("lab_emergency_light"), L("lab_glass"), L("lab_reinforced_glass"), L("lab_vent"), L("lab_hazard_block"), L("lab_pillar")
BLK, WHT = L("black_latex_block"), L("white_latex_block")


def lab_room(s, x1, y1, z1, x2, y2, z2, floor=TILE, windows=True):
    s.shell(x1, y1, z1, x2, y2, z2, W, floor, CEIL)
    s.box(x1, y1 + 2, z1, x2, y1 + 2, z2, WS)
    s.box(x1 + 1, y1 + 2, z1 + 1, x2 - 1, y1 + 2, z2 - 1, AIR)
    if windows:
        for x in range(x1 + 2, x2 - 1, 3):
            s.set(x, y1 + 2, z1, GLASS)
            s.set(x, y1 + 2, z2, GLASS)
        for z in range(z1 + 2, z2 - 1, 3):
            s.set(x1, y1 + 2, z, GLASS)
            s.set(x2, y1 + 2, z, GLASS)
    for x in range(x1 + 2, x2 - 1, 4):
        for z in range(z1 + 2, z2 - 1, 4):
            s.set(x, y2, z, LIGHT)


def door(s, x, y, z):
    s.set(x, y, z, AIR)
    s.set(x, y + 1, z, AIR)


def infect(s, n, blocks=(BLK,), solid_only=("lab_wall", "lab_tile", "lab_ceiling", "planks", "cobble", "stone")):
    keys = [k for k, v in s.cells.items() if any(t in v[0][0] for t in solid_only)]
    for k in s.rnd.sample(keys, min(n, len(keys))):
        s.set(*k, s.rnd.choice(blocks))


# ------------------------------------------------------------------ the 19 structures
def abandoned_lab():
    s = S(11, 6, 9, 101)
    lab_room(s, 0, 0, 0, 10, 5, 8)
    door(s, 5, 1, 0)
    s.set(1, 1, 7, L("lab_computer[facing=north]")); s.set(2, 1, 7, L("lab_table")); s.set(3, 1, 7, L("lab_shelf[facing=north]"))
    s.set(9, 1, 7, L("containment_tank")); s.set(9, 2, 7, L("containment_tank"))
    s.set(9, 1, 1, L("lab_crate")); s.set(8, 1, 1, L("lab_crate")); s.set(9, 2, 1, L("lab_crate"))
    s.chest(1, 1, 1, "lab_common", "east")
    for (x, z) in ((8, 6), (7, 7), (9, 5)):   # the tank leaked
        s.set(x, 1, z, L("black_latex_puddle"))
    for k in ((7, 4, 8), (8, 4, 8), (8, 5, 8), (8, 5, 7), (10, 3, 6), (10, 4, 6)):
        s.cells.pop(k, None)                   # broken corner
    infect(s, 14)
    s.mob(5, 1, 4, "drone_unit"); s.mob(3, 1, 5, "scientist_wolf")
    return s


def research_lab():
    s = S(17, 7, 13, 102)
    lab_room(s, 0, 0, 0, 16, 6, 12, TC)
    s.box(8, 1, 1, 8, 5, 11, W)                # dividing wall
    door(s, 8, 1, 6); door(s, 4, 1, 0); door(s, 12, 1, 12)
    s.box(8, 3, 2, 8, 3, 4, RGLASS); s.box(8, 3, 8, 8, 3, 10, RGLASS)
    for z in (2, 4, 8, 10):                    # specimen tanks
        s.set(15, 1, z, L("containment_tank")); s.set(15, 2, z, L("containment_tank"))
    for x in (10, 12, 14):
        s.set(x, 1, 1, L("lab_server_rack[facing=south]")); s.set(x, 2, 1, L("lab_server_rack[facing=south]"))
    for x in (1, 2, 3):
        s.set(x, 1, 11, L("lab_table"))
    s.set(1, 2, 11, L("lab_computer[facing=north]")); s.set(3, 2, 11, L("lab_computer[facing=north]"))
    for z in (2, 3, 4):
        s.set(1, 1, z, L("lab_shelf[facing=east]")); s.set(1, 2, z, L("lab_shelf[facing=east]"))
    s.set(7, 3, 9, L("hypno_screen[facing=west]"))
    s.set(4, 6, 6, ELIGHT); s.set(12, 6, 6, ELIGHT)
    s.set(12, 1, 6, L("conversion_chamber"))
    s.chest(6, 1, 1, "lab_rare", "south"); s.chest(14, 1, 11, "lab_common", "north")
    s.set(10, 1, 11, L("decontamination_block"))
    infect(s, 10)
    s.mob(4, 1, 6, "scientist_fox"); s.mob(11, 1, 4, "scientist_rabbit")
    s.mob(5, 1, 3, "drone_guard"); s.mob(13, 1, 8, "drone_guard"); s.mob(11, 1, 9, "drone_medic")
    return s


def containment_facility():
    s = S(21, 8, 21, 103)
    s.box(0, 0, 0, 20, 0, 20, TD)
    for (x1, z1, x2, z2) in ((0, 0, 20, 0), (0, 20, 20, 20), (0, 0, 0, 20), (20, 0, 20, 20)):   # perimeter wall
        s.box(x1, 1, z1, x2, 1, z2, HAZ)
        s.box(x1, 2, z1, x2, 3, z2, W)
        s.box(x1, 4, z1, x2, 4, z2, "iron_bars")
    for z in (9, 10, 11):
        for y in (1, 2, 3, 4):
            s.set(0, y, z, AIR)                # gate
    for (x, z) in ((0, 0), (20, 0), (0, 20), (20, 20)):   # corner towers
        s.box(x, 1, z, x, 6, z, PIL)
        s.set(x, 7, z, ELIGHT)
    lab_room(s, 5, 0, 5, 15, 6, 15, TC, windows=False)   # central block
    door(s, 5, 1, 10); door(s, 15, 1, 10)
    for i, mob in enumerate(("latex_wolf_black", "latex_cat_white", "latex_tiger")):   # three cells
        x = 7 + i * 3
        s.box(x - 1, 1, 12, x + 1, 4, 14, RGLASS)
        s.box(x, 1, 13, x, 3, 13, AIR)
        s.set(x, 4, 13, VENT)
        s.mob(x, 1, 13, mob)
    s.set(10, 1, 7, L("conversion_chamber")); s.set(10, 6, 7, ELIGHT)
    s.set(7, 1, 6, L("lab_computer[facing=south]")); s.set(8, 1, 6, L("lab_server_rack[facing=south]"))
    s.set(13, 1, 6, L("alarm_block")); s.set(12, 1, 6, L("lab_shelf[facing=south]"))
    s.chest(14, 1, 8, "lab_rare", "west"); s.chest(6, 1, 8, "lab_common", "east")
    for (x, z) in ((3, 3), (17, 3), (3, 17), (17, 17)):
        s.set(x, 1, z, L("lab_crate"))
    s.set(2, 1, 10, L("decontamination_block")); s.set(2, 0, 10, L("decontamination_block"))
    for (x, z) in ((3, 6), (17, 14), (10, 17), (10, 3)):
        s.set(x, 1, z, L("drone_charging_pad"))
    s.mob(3, 1, 10, "drone_enforcer"); s.mob(17, 1, 10, "drone_soldier"); s.mob(10, 1, 18, "drone_soldier")
    s.mob(9, 1, 9, "scientist_cat"); s.mob(12, 1, 9, "scientist_lizard"); s.mob(10, 1, 2, "drone_sniper")
    return s


def latex_pit():
    s = S(13, 6, 13, 104)
    s.yoff = -4
    s.sphere(6, 5.5, 6, 6.2, BLK, ry=5.2)
    s.sphere(6, 5.5, 6, 5.0, AIR, ry=4.2)
    for k in [k for k in s.cells if k[1] > 4]:
        s.cells.pop(k)
    s.box(0, 5, 0, 12, 5, 12, AIR)
    for (x, y, z), v in list(s.cells.items()):          # goo pool at the bottom
        if v[0][0] == AIR and y <= 2:
            s.set(x, y, z, L("black_latex_goo"))
    s.set(6, 1, 6, L("latex_core"))
    for k, v in list(s.cells.items()):
        if v[0][0] == BLK and s.rnd.random() < 0.18:
            s.set(*k, s.rnd.choice((L("infected_dirt"), L("infected_stone"), L("raw_latex_block"))))
    s.scatter([L("latex_crystal_black"), L("latex_tendrils"), L("latex_sprout")], 10, 0, 0, 12, 12, (3, 4, 5))
    s.chest(2, 3, 6, "hive", "east")
    s.mob(6, 3, 4, "goo_blob_black"); s.mob(4, 3, 7, "goo_blob_black"); s.mob(8, 3, 7, "goo_walker_black")
    return s


def hive(block, goo, puddle, crystal, mobs, spawn, seed):
    def build():
        s = S(15, 10, 15, seed)
        s.box(0, 0, 0, 14, 0, 14, block)
        s.sphere(7, 0, 7, 7.2, block, hollow=True, ry=8.6, ymin=1)
        for (x, y, z), v in list(s.cells.items()):
            if y >= 1 and v[0][0] != AIR and s.rnd.random() < 0.14:
                s.set(x, y, z, L("raw_latex_block") if s.rnd.random() < 0.6 else goo)
        for x in range(1, 14):                  # hollow interior
            for y in range(1, 9):
                for z in range(1, 14):
                    if ((x - 7) / 6.0) ** 2 + (y / 7.4) ** 2 + ((z - 7) / 6.0) ** 2 < 1.0:
                        s.set(x, y, z, AIR)
        for y in (1, 2):
            for x in (6, 7, 8):
                for z in (0, 1, 2):
                    s.set(x, y, z, AIR)         # entrance tunnel
        s.set(7, 1, 7, L("raw_latex_block")); s.set(7, 2, 7, L("latex_core"))
        s.spawner(7, 1, 10, spawn)
        for (x, z) in ((5, 6), (9, 8), (6, 9), (8, 5), (4, 8), (10, 6)):
            s.set(x, 0, z, goo)
        s.scatter([crystal, L("latex_tendrils"), puddle, L("latex_sprout")], 16, 2, 2, 12, 12, (1,))
        s.chest(3, 1, 7, "hive", "east"); s.chest(11, 1, 7, "hive", "west")
        for i, m in enumerate(mobs):
            s.mob((5, 9, 7)[i % 3], 1, (4, 4, 11)[i % 3], m)
        return s
    return build


def drone_outpost():
    s = S(9, 12, 9, 107)
    s.box(0, 0, 0, 8, 0, 8, TD)
    for (x, z) in ((1, 1), (7, 1), (1, 7), (7, 7)):
        s.box(x, 1, z, x, 7, z, PIL)
    s.box(0, 7, 0, 8, 7, 8, VENT)
    for (x1, z1, x2, z2) in ((0, 0, 8, 0), (0, 8, 8, 8), (0, 0, 0, 8), (8, 0, 8, 8)):
        s.box(x1, 8, z1, x2, 8, z2, HAZ)
    s.box(4, 1, 4, 4, 10, 4, PIL)
    s.set(4, 11, 4, ELIGHT)
    for y in range(1, 8):
        s.set(4, y, 5, "ladder[facing=south,waterlogged=false]")
    s.set(4, 7, 5, AIR)
    s.set(1, 8, 1, LIGHT); s.set(7, 8, 7, LIGHT)
    s.set(6, 8, 2, L("alarm_block")); s.set(2, 8, 6, L("lab_crate"))
    s.set(2, 1, 4, L("drone_charging_pad")); s.set(6, 1, 4, L("drone_charging_pad"))
    s.chest(4, 1, 2, "lab_common", "north")
    s.mob(3, 8, 3, "drone_sniper"); s.mob(5, 8, 5, "drone_scout"); s.mob(4, 1, 6, "drone_unit")
    return s


def crashed_transport():
    s = S(13, 5, 7, 108)
    s.box(0, 0, 0, 12, 0, 6, L("infected_dirt"))
    s.shell(1, 1, 1, 9, 4, 5, "iron_block", VENT, VENT)      # cargo container
    s.box(1, 1, 1, 1, 4, 5, AIR)                              # torn open rear
    s.box(2, 2, 1, 8, 3, 1, HAZ); s.box(2, 2, 5, 8, 3, 5, HAZ)
    s.box(10, 1, 2, 11, 2, 4, DGB := "gray_concrete")         # cab
    s.set(11, 2, 3, GLASS); s.set(10, 3, 3, ELIGHT)
    for (x, z) in ((2, 1), (9, 1), (2, 5), (9, 5), (11, 1), (11, 5)):
        s.set(x, 0, z, "coal_block")                          # wheels
    for (x, y, z) in ((7, 2, 3), (8, 2, 2), (8, 2, 4), (8, 3, 3), (6, 2, 4)):
        s.set(x, y, z, L("lab_crate"))
    for (x, y, z) in ((3, 2, 3), (0, 1, 3), (0, 1, 2), (4, 2, 2)):
        s.set(x, y, z, L("raw_latex_block"))
    s.set(5, 2, 3, L("containment_tank"))
    s.scatter([L("black_latex_puddle")], 7, 0, 0, 5, 6, (1, 2))
    infect(s, 8, solid_only=("iron_block", "lab_vent"))
    s.chest(7, 2, 2, "lab_common", "west")
    s.mob(0, 1, 5, "goo_blob_black"); s.mob(3, 2, 4, "goo_blob_black"); s.mob(11, 3, 2, "drone_worker")
    return s


def conversion_shrine():
    s = S(9, 7, 9, 109)
    s.box(0, 0, 0, 8, 0, 8, TD)
    s.box(1, 1, 1, 7, 1, 7, TD)
    for i in range(1, 8):
        for (x, z, f) in ((i, 0, "north"), (i, 8, "south"), (0, i, "west"), (8, i, "east")):
            s.set(x, 1, z, L(f"lab_tile_stairs[facing={'south' if f == 'north' else 'north' if f == 'south' else 'east' if f == 'west' else 'west'},half=bottom,shape=straight,waterlogged=false]"))
    for (x, z, f) in ((1, 1, "south"), (7, 1, "south"), (1, 7, "north"), (7, 7, "north")):
        s.box(x, 2, z, x, 5, z, PIL)
        s.set(x, 6, z, ELIGHT)
        s.set(x, 4, z, L(f"hypno_screen[facing={f}]"))
    s.set(4, 2, 4, L("conversion_chamber"))
    for (x, z) in ((3, 4), (5, 4), (4, 3), (4, 5)):
        s.set(x, 1, z, HAZ)
    s.chest(4, 2, 6, "lab_rare", "north")
    s.mob(2, 2, 4, "drone_guard"); s.mob(6, 2, 4, "drone_guard"); s.mob(4, 2, 2, "scientist_cat")
    return s


def infected_house():
    s = S(9, 8, 9, 110)
    P, LOG, COB = L("infected_planks"), L("infected_log[axis=y]"), L("infected_cobblestone")
    s.box(0, 0, 0, 8, 0, 8, L("infected_grass"))
    s.shell(1, 0, 1, 7, 5, 7, P, COB, P)
    for (x, z) in ((1, 1), (7, 1), (1, 7), (7, 7)):
        s.box(x, 0, z, x, 5, z, LOG)
    for i, inset in enumerate((1, 2, 3)):                   # stepped roof
        s.box(inset, 5 + i, inset, 8 - inset, 5 + i, 8 - inset, P)
    s.box(2, 5, 2, 6, 5, 6, AIR)
    door(s, 4, 1, 1)
    for (x, z) in ((2, 1), (6, 1), (1, 4), (7, 4), (4, 7)):
        s.set(x, 2, z, "glass")
    s.set(2, 1, 6, "crafting_table"); s.set(6, 1, 6, "barrel[facing=up,open=false]")
    s.set(6, 1, 2, L("black_latex_goo")); s.set(5, 1, 2, L("black_latex_goo")); s.set(6, 2, 2, L("black_latex_goo"))
    s.chest(2, 1, 2, "hive", "east")
    s.scatter([L("latex_tendrils"), L("latex_sprout"), L("black_latex_puddle")], 8, 2, 2, 6, 6, (1,))
    s.scatter([L("latex_sprout"), L("latex_tendrils")], 6, 0, 0, 8, 8, (1,))
    for k, v in list(s.cells.items()):
        if "infected_planks" in v[0][0] and s.rnd.random() < 0.12:
            s.set(*k, BLK)
    s.set(4, 6, 4, L("infected_leaves"))
    s.mob(4, 1, 4, "latex_mimic"); s.mob(3, 1, 5, "goo_walker_black")
    return s


def latex_spire():
    s = S(7, 16, 7, 111)
    s.box(0, 0, 0, 6, 0, 6, L("infected_stone"))
    for y in range(1, 16):
        r = 2.6 * (1 - y / 16.5) + 0.35
        for x in range(7):
            for z in range(7):
                if (x - 3) ** 2 + (z - 3) ** 2 <= r * r:
                    b = BLK if s.rnd.random() > 0.2 else L("raw_latex_block")
                    s.set(x, y, z, b)
    s.set(3, 9, 3, L("latex_core"))
    for (x, y, z) in ((2, 6, 0), (5, 4, 1)):
        pass
    s.scatter([L("latex_crystal_black"), L("latex_crystal_black"), L("latex_tendrils")], 9, 0, 0, 6, 6, (1,))
    s.set(3, 15, 3, L("latex_crystal_black"))
    s.mob(1, 1, 1, "latex_crystal_guardian"); s.mob(5, 1, 5, "latex_stalker")
    return s


def hypno_tower():
    s = S(7, 14, 7, 112)
    s.box(0, 0, 0, 6, 0, 6, TD)
    s.shell(1, 0, 1, 5, 9, 5, W, TD, VENT)
    for y in (3, 6):
        s.box(1, y, 1, 5, y, 5, WS)
        s.box(2, y, 2, 4, y, 4, AIR)
    door(s, 3, 1, 1)
    for y in range(1, 10):
        s.set(3, y, 4, "ladder[facing=north,waterlogged=false]")
    s.shell(0, 9, 0, 6, 13, 6, GLASS, VENT, CEIL)           # observation room
    s.set(3, 9, 4, AIR)
    for (x, z, f) in ((3, 0, "north"), (3, 6, "south"), (0, 3, "west"), (6, 3, "east")):
        s.set(x, 11, z, L(f"hypno_screen[facing={f}]"))
    s.set(3, 13, 3, LIGHT)
    for (x, z) in ((0, 0), (6, 0), (0, 6), (6, 6)):
        s.box(x, 9, z, x, 13, z, PIL)
    s.chest(1, 10, 1, "lab_rare", "south")
    s.set(5, 10, 5, L("lab_computer[facing=north]"))
    s.mob(4, 10, 2, "latex_hypnotist"); s.mob(2, 10, 4, "hover_drone_hypno"); s.mob(2, 1, 2, "drone_guard")
    return s


def scientist_camp():
    s = S(13, 5, 11, 113)
    s.box(0, 0, 0, 12, 0, 10, "coarse_dirt")
    for x0 in (1, 8):                                        # two wool tents
        for i in range(3):
            s.box(x0 + i, 1 + i, 1, x0 + 4 - i, 1 + i, 5, "white_wool")
            if i < 2:
                s.box(x0 + i + 1, 1 + i, 1, x0 + 3 - i, 1 + i, 4, AIR)
        s.set(x0 + 2, 1, 5, AIR)
        s.set(x0 + 2, 1, 1, "white_wool")
    s.set(6, 1, 7, "campfire[facing=north,lit=true,signal_fire=false,waterlogged=false]")
    s.set(2, 1, 8, L("lab_table")); s.set(2, 2, 8, L("lab_computer[facing=north]")); s.set(3, 1, 8, L("lab_table"))
    s.set(3, 2, 8, L("lab_shelf[facing=north]"))
    for (x, y, z) in ((10, 1, 8), (11, 1, 8), (10, 2, 8), (10, 1, 9)):
        s.set(x, y, z, L("lab_crate"))
    s.set(8, 1, 9, L("containment_tank"))
    s.chest(3, 1, 3, "lab_common", "south"); s.chest(10, 1, 3, "lab_rare", "south")
    s.set(6, 1, 2, L("lab_light"))
    s.mob(5, 1, 8, "scientist_fox"); s.mob(7, 1, 5, "scientist_raven"); s.mob(6, 1, 9, "drone_guard")
    return s


def charging_station():
    s = S(11, 6, 9, 114)
    s.box(0, 0, 0, 10, 0, 8, TD)
    for (x, z) in ((0, 0), (10, 0), (0, 8), (10, 8)):
        s.box(x, 1, z, x, 4, z, PIL)
    s.box(0, 5, 0, 10, 5, 8, CEIL)
    for x in (2, 5, 8):
        s.set(x, 5, 4, LIGHT)
        for z in (2, 6):
            s.set(x, 0, z, L("drone_charging_pad"))
            s.set(x, 1, z, AIR)
    for x in (3, 4, 6, 7):
        s.set(x, 1, 4, L("lab_server_rack[facing=north]")); s.set(x, 2, 4, L("lab_server_rack[facing=south]"))
    s.set(5, 1, 4, L("lab_computer[facing=north]"))
    s.set(1, 1, 4, L("lab_crate")); s.set(9, 1, 4, HAZ)
    s.chest(9, 1, 5, "lab_common", "west")
    s.mob(2, 1, 2, "drone_worker"); s.mob(8, 1, 6, "drone_worker"); s.mob(5, 1, 2, "drone_technician"); s.mob(5, 2, 6, "hover_drone_scout")
    return s


def quarantine_checkpoint():
    s = S(15, 6, 9, 115)
    s.box(0, 0, 0, 14, 0, 8, "gravel")
    s.box(0, 0, 3, 14, 0, 5, TD)                             # road
    for x in (5, 9):
        s.set(x, 0, 4, L("decontamination_block"))
    for z in ((0, 2), (6, 8)):                               # barriers left/right of the road
        s.box(7, 1, z[0], 7, 2, z[1], HAZ)
        s.box(7, 3, z[0], 7, 3, z[1], "iron_bars")
    for z in (2, 6):
        s.box(7, 1, z, 7, 4, z, PIL)
        s.set(7, 5, z, ELIGHT)
    s.shell(9, 0, 0, 13, 4, 2, W, TILE, CEIL)                # guard booth
    s.box(10, 2, 2, 12, 2, 2, GLASS); s.set(13, 2, 1, GLASS)
    door(s, 9, 1, 1)
    s.set(12, 1, 1, L("lab_computer[facing=west]")); s.set(11, 4, 1, LIGHT)
    s.set(12, 1, 7, L("alarm_block")); s.set(2, 1, 1, L("lab_crate")); s.set(2, 1, 7, L("lab_crate")); s.set(3, 1, 7, L("lab_crate"))
    s.chest(10, 1, 1, "lab_common", "south")
    s.mob(6, 1, 4, "drone_guard"); s.mob(8, 1, 4, "drone_guard"); s.mob(11, 1, 5, "drone_warden")
    return s


def vent_field():
    s = S(13, 4, 13, 116)
    for x in range(13):
        for z in range(13):
            if (x - 6) ** 2 + (z - 6) ** 2 <= 42:
                s.set(x, 0, z, s.rnd.choice((L("infected_grass"), L("infected_dirt"), L("infected_dirt"), L("green_latex_block"))))
    for (x, z) in ((6, 6), (3, 4), (9, 4), (4, 9), (8, 9), (6, 2), (6, 10)):
        s.set(x, 0, z, L("latex_gas_vent"))
        s.set(x, 1, z, AIR)
    s.scatter([L("latex_crystal_green"), L("latex_tendrils"), L("black_latex_puddle"), L("latex_sprout")], 22, 1, 1, 11, 11, (1,))
    s.set(6, 1, 5, L("lab_hazard_block")); s.set(6, 2, 5, L("lab_emergency_light"))
    s.mob(4, 1, 6, "goo_walker_green"); s.mob(8, 1, 6, "goo_walker_green"); s.mob(6, 1, 8, "toxic_blob")
    return s


def signal_tower():
    s = S(7, 18, 7, 117)
    s.box(0, 0, 0, 6, 0, 6, TD)
    s.shell(1, 0, 1, 5, 4, 5, W, TD, VENT)                   # base hut
    door(s, 3, 1, 1)
    s.set(2, 1, 4, L("lab_server_rack[facing=north]")); s.set(4, 1, 4, L("lab_server_rack[facing=north]"))
    s.set(2, 2, 4, L("lab_server_rack[facing=north]")); s.set(4, 2, 4, L("lab_computer[facing=north]"))
    s.set(3, 4, 3, LIGHT)
    s.chest(4, 1, 2, "lab_common", "west")
    s.box(3, 5, 3, 3, 15, 3, PIL)                             # mast
    for y in range(5, 15):
        for (x, z) in ((2, 2), (4, 2), (2, 4), (4, 4)):
            if y % 3 != 1:
                s.set(x, y, z, "iron_bars")
    for y in (8, 12):
        s.box(1, y, 1, 5, y, 5, VENT)
        s.set(3, y, 3, PIL)
    s.set(3, 16, 3, L("alarm_block")); s.set(3, 17, 3, ELIGHT)
    s.mob(3, 1, 3, "drone_technician"); s.mob(2, 9, 3, "hover_drone_sentry"); s.mob(4, 13, 3, "hover_drone_scout")
    return s


def underground_bunker():
    s = S(13, 12, 13, 118)
    s.yoff = -11
    s.shell(0, 0, 0, 12, 5, 12, W, TC, CEIL)                 # main level, deep underground
    s.box(0, 2, 0, 12, 2, 12, WS)
    s.box(1, 2, 1, 11, 2, 11, AIR)
    s.box(6, 1, 1, 6, 4, 11, W)
    door(s, 6, 1, 3); door(s, 6, 1, 9)
    for (x, z) in ((3, 3), (3, 9), (9, 3), (9, 9)):
        s.set(x, 5, z, LIGHT)
    s.shell(1, 5, 1, 3, 11, 3, W)                            # ladder shaft to the surface
    s.set(2, 5, 2, AIR)
    for y in range(1, 12):
        s.set(2, y, 2, "ladder[facing=south,waterlogged=false]")
    s.set(2, 11, 2, AIR)
    for (x, z) in ((1, 1), (3, 1), (1, 3), (3, 3), (2, 1), (1, 2), (3, 2), (2, 3)):
        s.set(x, 11, z, HAZ)
    s.box(8, 1, 6, 11, 4, 11, RGLASS)                        # holding cell
    s.box(9, 1, 7, 10, 3, 10, AIR)
    s.mob(9, 1, 8, "latex_stalker")
    s.set(1, 1, 11, L("lab_computer[facing=north]")); s.set(2, 1, 11, L("lab_table")); s.set(3, 1, 11, L("lab_shelf[facing=north]"))
    s.set(1, 1, 6, L("containment_tank")); s.set(1, 2, 6, L("containment_tank"))
    s.set(8, 1, 1, L("lab_server_rack[facing=south]")); s.set(9, 1, 1, L("lab_server_rack[facing=south]")); s.set(10, 1, 1, L("alarm_block"))
    s.set(11, 1, 3, L("conversion_chamber")); s.set(4, 1, 6, L("decontamination_block"))
    s.spawner(9, 1, 4, "drone_soldier")
    s.chest(5, 1, 11, "lab_rare", "north"); s.chest(11, 1, 1, "lab_rare", "west"); s.chest(4, 1, 1, "lab_common", "south")
    s.mob(4, 1, 8, "scientist_lizard"); s.mob(3, 1, 5, "drone_soldier"); s.mob(8, 1, 3, "drone_soldier")
    return s


def latex_cave_nest():
    s = S(13, 9, 13, 119)
    s.sphere(6, 4, 6, 6.3, L("infected_stone"), hollow=True, ry=4.4)
    for k, v in list(s.cells.items()):
        r = s.rnd.random()
        if r < 0.3:
            s.set(*k, BLK)
        elif r < 0.4:
            s.set(*k, L("raw_latex_block"))
        elif r < 0.46:
            s.set(*k, L("latex_ore"))
    for x in range(13):
        for y in range(9):
            for z in range(13):
                if ((x - 6) / 5.1) ** 2 + ((y - 4) / 3.2) ** 2 + ((z - 6) / 5.1) ** 2 < 1.0:
                    s.set(x, y, z, AIR)
    for x in range(3, 10):
        for z in range(3, 10):
            if s.get(x, 1, z) == AIR and s.rnd.random() < 0.45:
                s.set(x, 1, z, L("black_latex_goo"))
    s.set(6, 1, 6, L("raw_latex_block")); s.set(6, 2, 6, L("latex_core"))
    s.spawner(6, 1, 8, "latex_hound_black")
    s.scatter([L("latex_crystal_black"), L("latex_tendrils"), L("latex_crystal_white")], 10, 2, 2, 10, 10, (1, 2))
    s.chest(3, 2, 6, "hive", "east")
    s.mob(6, 2, 4, "latex_crawler"); s.mob(8, 2, 6, "latex_crawler"); s.mob(4, 2, 8, "goo_blob_black")
    return s



# ------------------------------------------------------------------ drone life update structures
DWALL, STEEL, GRATE, BRICK = L("lab_dark_wall"), L("lab_steel_plate"), L("lab_floor_grate"), L("latex_bricks")


def drone_barracks():
    s = S(15, 6, 11, 201)
    s.shell(0, 0, 0, 14, 5, 10, DWALL, STEEL, CEIL)
    s.box(0, 2, 0, 14, 2, 10, WS)
    s.box(1, 2, 1, 13, 2, 9, AIR)
    door(s, 7, 1, 0)
    for x in (2, 5, 9, 12):                       # two rows of sleep pods
        for z in (2, 8):
            s.set(x, 1, z, L("drone_pod")); s.set(x, 2, z, L("drone_pod"))
    for x in (3, 4, 10, 11):
        s.set(x, 0, 5, L("drone_charging_pad"))
    s.set(7, 1, 9, L("drone_terminal[facing=north]"))
    s.set(13, 1, 5, L("drone_locker[facing=west]")); s.set(13, 1, 4, L("drone_locker[facing=west]"))
    s.set(1, 1, 5, L("drone_locker[facing=east]"))
    for x in (3, 7, 11):
        s.set(x, 5, 5, L("drone_beacon"))
    s.chest(13, 1, 6, "lab_common", "west")
    s.mob(4, 1, 5, "drone_unit"); s.mob(10, 1, 5, "drone_worker"); s.mob(7, 1, 7, "scientist_wolf")
    return s


def processing_center():
    s = S(19, 7, 13, 202)
    lab_room(s, 0, 0, 0, 18, 6, 12, TC)
    door(s, 9, 1, 0)
    for x in (3, 6, 9, 12, 15):                   # conversion line behind glass
        s.set(x, 1, 10, L("conversion_chamber"))
        s.box(x - 1, 1, 9, x + 1, 3, 9, RGLASS)
        s.set(x, 4, 10, L("hypno_screen[facing=north]"))
    for x in (3, 6, 9, 12, 15):
        s.set(x, 1, 9, AIR); s.set(x, 2, 9, AIR)
    for x in (2, 16):
        s.set(x, 1, 2, L("drone_terminal[facing=south]"))
    for x in range(4, 15):
        s.set(x, 0, 5, HAZ)
    s.set(1, 1, 6, L("research_desk[facing=east]")); s.set(17, 1, 6, L("lab_monitor[facing=west]"))
    s.set(17, 1, 4, L("specimen_freezer[facing=west]")); s.set(1, 1, 4, L("lab_cabinet[facing=east]"))
    s.set(9, 6, 6, ELIGHT)
    s.chest(17, 1, 2, "lab_rare", "west")
    s.mob(5, 1, 4, "scientist_fox"); s.mob(13, 1, 4, "scientist_lizard")
    s.mob(9, 1, 7, "drone_overseer"); s.mob(7, 1, 3, "drone_guard"); s.mob(11, 1, 3, "drone_guard")
    return s


def contaminated_storage():
    s = S(13, 6, 11, 203)
    s.shell(0, 0, 0, 12, 5, 10, STEEL, TD, VENT)
    door(s, 6, 1, 0)
    for x in (2, 4, 8, 10):                       # crate stacks
        for z in (2, 8):
            for y in range(1, 1 + s.rnd.randint(1, 3)):
                s.set(x, y, z, L("lab_crate"))
    s.set(6, 1, 9, L("toxic_tank")); s.set(6, 2, 9, L("toxic_tank"))
    s.set(1, 1, 5, L("drone_terminal[facing=east]"))
    s.scatter([L("black_latex_puddle"), L("green_latex_puddle"), L("purple_latex_puddle")], 18, 1, 1, 11, 9, (1,))
    s.set(11, 1, 5, L("green_latex_goo")); s.set(11, 1, 6, L("green_latex_goo"))
    s.chest(11, 1, 2, "lab_common", "west")
    s.mob(6, 1, 5, "drone_worker"); s.mob(3, 1, 5, "goo_blob_green"); s.mob(9, 1, 5, "drone_collector")
    return s


def directors_office():
    s = S(17, 10, 15, 204)
    s.box(0, 0, 0, 16, 0, 14, TC)
    s.shell(1, 0, 1, 15, 5, 13, W, TC, CEIL)
    s.shell(3, 5, 3, 13, 9, 11, GLASS, L("polished_latex"), CEIL)    # glass office on top
    for (x, z) in ((1, 1), (15, 1), (1, 13), (15, 13), (3, 3), (13, 3), (3, 11), (13, 11)):
        s.box(x, 1 if x in (1, 15) else 6, z, x, 5 if x in (1, 15) else 9, z, PIL)
    door(s, 8, 1, 1)
    for y in range(1, 6):
        s.set(14, y, 7, "ladder[facing=west,waterlogged=false]")
    s.set(14, 5, 7, AIR); s.set(13, 5, 7, AIR); s.set(12, 5, 7, AIR)
    s.set(8, 6, 9, L("research_desk[facing=south]")); s.set(7, 6, 9, L("lab_monitor[facing=south]"))
    s.set(9, 6, 9, L("lab_monitor[facing=south]")); s.set(8, 6, 10, L("drone_terminal[facing=south]"))
    s.set(4, 6, 4, L("lab_shelf[facing=south]")); s.set(12, 6, 4, L("specimen_freezer[facing=south]"))
    s.chest(4, 6, 10, "lab_rare", "east")
    for x in (3, 6, 10, 13):                    # ground floor: guarded lobby
        s.set(x, 1, 12, L("lab_cabinet[facing=north]"))
    s.set(8, 1, 12, L("drone_terminal[facing=north]")); s.set(2, 1, 7, L("alarm_block"))
    s.set(8, 5, 7, LIGHT); s.set(8, 9, 7, LIGHT)
    s.mob(8, 6, 7, "scientist_raven"); s.mob(6, 6, 6, "scientist_cat")
    s.mob(5, 1, 6, "drone_commander"); s.mob(11, 1, 6, "drone_sentinel"); s.mob(8, 1, 4, "drone_guard")
    return s


def drone_factory():
    s = S(19, 8, 13, 205)
    s.shell(0, 0, 0, 18, 7, 12, STEEL, GRATE, VENT)
    s.box(0, 0, 0, 18, 0, 12, TD)
    door(s, 9, 1, 0); door(s, 0, 1, 6)
    for x in range(2, 17):                        # assembly line
        s.set(x, 1, 6, L("lab_steel_slab[type=bottom,waterlogged=false]"))
    for x in (4, 9, 14):
        s.set(x, 1, 4, L("drone_fabricator[facing=south]")); s.set(x, 1, 8, L("drone_fabricator[facing=north]"))
        s.box(x, 2, 4, x, 6, 4, L("lab_pipe[axis=y]"))
    for x in (2, 16):
        s.set(x, 1, 11, L("lab_generator[facing=north]")); s.set(x, 2, 11, L("lab_server_rack[facing=north]"))
    s.set(9, 1, 11, L("drone_terminal[facing=north]"))
    for x in (4, 9, 14):
        s.set(x, 7, 6, L("drone_beacon"))
    s.box(1, 6, 1, 17, 6, 1, L("lab_pipe[axis=x]"))
    s.chest(17, 1, 1, "lab_rare", "west")
    s.mob(6, 1, 7, "drone_technician"); s.mob(12, 1, 5, "drone_prototype"); s.mob(9, 1, 9, "scientist_rabbit")
    s.mob(15, 1, 9, "hover_drone_carrier")
    return s


def hypno_garden():
    s = S(15, 6, 15, 206)
    for x in range(15):
        for z in range(15):
            if (x - 7) ** 2 + (z - 7) ** 2 <= 50:
                s.set(x, 0, z, s.rnd.choice((L("infected_grass"), L("infected_grass"), L("purple_latex_block"))))
    for (x, z) in ((7, 7), (3, 7), (11, 7), (7, 3), (7, 11)):
        s.set(x, 1, z, L("hypno_pylon")); s.set(x, 2, z, L("hypno_pylon"))
        if (x, z) == (7, 7):
            s.set(x, 3, z, L("hypno_pylon")); s.set(x, 4, z, L("drone_beacon"))
    s.scatter([L("glowing_latex_pod"), L("latex_crystal_black"), L("latex_sprout"), L("purple_latex_puddle")], 26, 1, 1, 13, 13, (1,))
    s.mob(5, 1, 5, "latex_hypnotist"); s.mob(9, 1, 9, "latex_siren"); s.mob(9, 1, 5, "hover_drone_hypno")
    s.chest(7, 1, 12, "hive", "north")
    return s


def latex_monolith():
    s = S(9, 18, 9, 207)
    s.box(0, 0, 0, 8, 0, 8, L("polished_latex"))
    s.box(1, 1, 1, 7, 1, 7, BRICK)
    for (x, z) in ((1, 4), (7, 4), (4, 1), (4, 7)):
        s.set(x, 1, z, L("latex_brick_slab[type=bottom,waterlogged=false]"))
    s.box(3, 2, 3, 5, 14, 5, BRICK)
    for y in (4, 8, 12):
        for (x, z) in ((4, 3), (4, 5), (3, 4), (5, 4)):
            s.set(x, y, z, L("chiseled_latex"))
    s.box(4, 15, 4, 4, 16, 4, L("polished_latex"))
    s.set(4, 17, 4, L("latex_crystal_black"))
    s.set(4, 2, 2, L("latex_core"))
    s.scatter([L("latex_crystal_black"), L("glowing_latex_pod"), L("latex_tendrils")], 10, 0, 0, 8, 8, (1, 2))
    s.mob(1, 2, 1, "latex_assimilator"); s.mob(7, 2, 7, "latex_queen_guard")
    return s


def assimilated_farm():
    s = S(15, 7, 13, 208)
    s.box(0, 0, 0, 14, 0, 12, L("infected_dirt"))
    for x in range(1, 8):                         # infected crop field
        for z in range(1, 12):
            if z % 3 == 0:
                s.set(x, 0, z, L("black_latex_goo"))
            else:
                s.set(x, 1, z, s.rnd.choice((L("latex_sprout"), L("latex_tendrils"), L("glowing_latex_pod"))))
    P, LOG = L("infected_planks"), L("infected_log[axis=y]")
    s.shell(9, 0, 3, 14, 4, 9, P, L("infected_planks"), P)
    for (x, z) in ((9, 3), (14, 3), (9, 9), (14, 9)):
        s.box(x, 0, z, x, 4, z, LOG)
    for i in range(3):
        s.box(9 + i, 5 + i, 3, 14 - i, 5 + i, 9, L("infected_planks_slab[type=bottom,waterlogged=false]"))
    door(s, 9, 1, 6)
    s.set(13, 1, 4, "barrel[facing=up,open=false]"); s.set(13, 1, 8, L("drone_terminal[facing=north]"))
    s.chest(10, 1, 8, "hive", "east")
    s.mob(4, 1, 6, "drone_worker"); s.mob(11, 1, 6, "latex_mimic"); s.mob(5, 1, 2, "drone_collector")
    return s


def observation_dome():
    s = S(15, 9, 15, 209)
    s.box(0, 0, 0, 14, 0, 14, STEEL)
    s.sphere(7, 0, 7, 7.0, RGLASS, hollow=True, ry=8.0, ymin=1)
    s.box(6, 1, 0, 8, 3, 1, AIR)
    for (x, z) in ((6, 1), (8, 1)):
        s.box(x, 1, z, x, 3, z, PIL)
    s.set(7, 4, 1, PIL)
    s.set(7, 1, 7, L("research_desk[facing=south]")); s.set(6, 1, 7, L("lab_monitor[facing=south]"))
    s.set(8, 1, 7, L("lab_monitor[facing=south]")); s.set(7, 1, 11, L("drone_terminal[facing=north]"))
    s.set(3, 1, 7, L("lab_generator[facing=east]")); s.set(11, 1, 7, L("specimen_freezer[facing=west]"))
    s.set(7, 7, 7, LIGHT)
    s.chest(10, 1, 10, "lab_rare", "west")
    s.mob(7, 1, 9, "scientist_cat"); s.mob(4, 1, 4, "drone_sentinel"); s.mob(10, 1, 4, "hover_drone_scout")
    return s


BUILDERS = {
    "abandoned_lab": abandoned_lab, "research_lab": research_lab, "containment_facility": containment_facility,
    "latex_pit": latex_pit,
    "black_latex_hive": hive(BLK, L("black_latex_goo"), L("black_latex_puddle"), L("latex_crystal_black"),
                             ("latex_wolf_black", "latex_wolf_black", "latex_assimilator"), "drone_unit", 105),
    "white_latex_hive": hive(WHT, L("white_latex_goo"), L("white_latex_puddle"), L("latex_crystal_white"),
                             ("latex_wolf_white", "latex_ghost", "latex_wolf_white"), "goo_walker_white", 106),
    "drone_outpost": drone_outpost, "crashed_transport": crashed_transport, "conversion_shrine": conversion_shrine,
    "infected_house": infected_house, "latex_spire": latex_spire, "hypno_tower": hypno_tower,
    "scientist_camp": scientist_camp, "charging_station": charging_station,
    "quarantine_checkpoint": quarantine_checkpoint, "vent_field": vent_field, "signal_tower": signal_tower,
    "underground_bunker": underground_bunker, "latex_cave_nest": latex_cave_nest,
    "drone_barracks": drone_barracks, "processing_center": processing_center,
    "contaminated_storage": contaminated_storage, "directors_office": directors_office,
    "drone_factory": drone_factory, "hypno_garden": hypno_garden, "latex_monolith": latex_monolith,
    "assimilated_farm": assimilated_farm, "observation_dome": observation_dome,
}


def wj(obj, *path):
    p = os.path.join(*path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w") as f:
        json.dump(obj, f, indent=2)


def main():
    assert set(BUILDERS) == {n for n, *_ in C.STRUCTURES}
    mob_names = {m["name"] for m in C.MOBS}
    block_names = {b["name"] for b in C.BLOCKS}
    for i, (name, title, kind, spacing, separation) in enumerate(C.STRUCTURES):
        s = BUILDERS[name]()
        for (_x, _y, _z, mob) in s.ents:
            assert mob in mob_names, mob
        for (state, nbt) in s.cells.values():
            ns, n = state[0].split(":")
            assert ns == "minecraft" or n in block_names, state[0]
            if nbt and "SpawnData" in nbt:
                assert nbt["SpawnData"]["entity"]["id"].split(":")[1] in mob_names
        p = os.path.join(DATA, "structures", name + ".nbt")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "wb") as f:
            f.write(nbt_bytes(s.to_nbt()))

        struct_json = {
            "type": "minecraft:jigsaw",
            "biomes": f"#{M}:has_structure/" + ("underground" if kind == "underground" else "land"),
            "step": "surface_structures" if kind == "surface" else "underground_structures",
            "spawn_overrides": {},
            "terrain_adaptation": "beard_thin" if (kind == "surface" and s.yoff == 0) else "none",
            "start_pool": f"{M}:{name}",
            "size": 1,
            "max_distance_from_center": 80,
            "use_expansion_hack": False,
        }
        if kind == "underground":
            struct_json["start_height"] = {"type": "minecraft:uniform", "min_inclusive": {"absolute": -40},
                                           "max_inclusive": {"absolute": 16}}
        else:
            struct_json["start_height"] = {"absolute": s.yoff}
            struct_json["project_start_to_heightmap"] = "WORLD_SURFACE_WG"
        wj(struct_json, DATA, "worldgen", "structure", name + ".json")
        wj({"fallback": "minecraft:empty", "elements": [{"weight": 1, "element": {
            "element_type": "minecraft:single_pool_element", "location": f"{M}:{name}",
            "projection": "rigid", "processors": "minecraft:empty"}}]},
           DATA, "worldgen", "template_pool", name + ".json")
        wj({"structures": [{"structure": f"{M}:{name}", "weight": 1}],
            "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": separation,
                          "salt": 143000 + i * 7919}},
           DATA, "worldgen", "structure_set", name + ".json")
        print(f"{name:24s} size={s.size} blocks={len(s.cells):5d} mobs={len(s.ents)} yoff={s.yoff}")
    print("structures:", len(C.STRUCTURES))


if __name__ == "__main__":
    main()
