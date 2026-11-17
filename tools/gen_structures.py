"""Generates every structure template + worldgen JSON for Changed: Thunder Science Expansion."""
import json
import math
import os
import random
import sys

from nbtlib_mini import Template, B

MOD = "changed_tsc"
ROOT = sys.argv[1] if len(sys.argv) > 1 else "out"
DATA = os.path.join(ROOT, "src/main/resources/data", MOD)

THEMES = {
    "white": dict(wall="white_concrete", accent="light_gray_concrete", floor=f"{MOD}:lab_tile_white",
                  trim="smooth_quartz", glass="light_blue_stained_glass", light="sea_lantern",
                  door="birch_door", goo="black_carpet", slab="smooth_quartz_slab", stairs="smooth_quartz_stairs", bed="white_bed",
                  sign_color="black"),
    "black": dict(wall="black_concrete", accent="gray_concrete", floor=f"{MOD}:lab_tile_black",
                  trim="polished_blackstone_bricks", glass="gray_stained_glass", light="sea_lantern",
                  door="dark_oak_door", goo="white_carpet", slab="polished_blackstone_slab", stairs="polished_blackstone_stairs",
                  bed="black_bed",
                  sign_color="yellow"),
    "bunker": dict(wall="deepslate_tiles", accent="polished_deepslate", floor=f"{MOD}:lab_tile_black",
                   trim="reinforced_deepslate", glass="tinted_glass", light="sea_lantern",
                   door="dark_oak_door", goo="white_carpet", slab="polished_deepslate_slab", stairs="polished_deepslate_stairs",
                   bed="gray_bed",
                   sign_color="yellow"),
}
SOLID_SHELL = "stone_bricks"


# =====================================================================================  primitives

def marker(t, x, y, z, key, count=1):
    t.set(x, y, z, f"{MOD}:spawn_marker", None, {"id": f"{MOD}:spawn_marker", "Key": key, "Count": count})


DIRS = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}
EMPTY_BARREL = "minecraft:barrel[facing=up]"


def furn(t, x, y, z, kind, facing="north", fallback=""):
    """
    A piece of furniture from the Changed mod. The marker is replaced in game by Changed's block for
    this kind (lab table, office chair, computer, microscope...), found by name when the world runs.
    If Changed has no such block, `fallback` (a vanilla block state) is placed instead; "" = nothing.
    In the fallback, {facing} and {opposite} are replaced by the furniture's direction.
    """
    t.set(x, y, z, f"{MOD}:spawn_marker", None,
          {"id": f"{MOD}:spawn_marker", "Key": "furniture:" + kind, "Count": 1, "Facing": facing, "Fallback": fallback})


def chest(t, x, y, z, loot, facing="north", barrel=False):
    lt = f"{MOD}:chests/{loot}"
    if barrel:
        t.set(x, y, z, "barrel", {"facing": "up", "open": "false"}, {"id": "minecraft:barrel", "LootTable": lt})
    else:
        t.set(x, y, z, "chest", {"facing": facing, "type": "single", "waterlogged": "false"},
              {"id": "minecraft:chest", "LootTable": lt})


def door(t, x, y, z, facing, kind):
    base = {"facing": facing, "hinge": "left", "open": "false", "powered": "false"}
    t.set(x, y, z, kind, dict(base, half="lower"))
    t.set(x, y + 1, z, kind, dict(base, half="upper"))


def sign(t, x, y, z, facing, lines, color="black"):
    lines = (list(lines) + ["", "", "", ""])[:4]
    msgs = [json.dumps({"text": s}) for s in lines]
    blank = [json.dumps({"text": ""})] * 4
    nbt = {"id": "minecraft:sign",
           "front_text": {"messages": msgs, "color": color, "has_glowing_text": B(1)},
           "back_text": {"messages": blank, "color": "black", "has_glowing_text": B(0)},
           "is_waxed": B(1)}
    t.set(x, y, z, "oak_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)


def standing_sign(t, x, y, z, rotation, lines, color="black"):
    sign(t, x, y, z, "north", lines, color)
    key, nbt = t.blocks[(x, y, z)]
    t.set(x, y, z, "oak_sign", {"rotation": str(rotation), "waterlogged": "false"}, nbt)


def ladder(t, x, y1, y2, z, facing):
    for y in range(y1, y2 + 1):
        t.set(x, y, z, "ladder", {"facing": facing, "waterlogged": "false"})


def bed(t, x, y, z, color, facing="south"):
    dx, dz = {"south": (0, 1), "north": (0, -1), "east": (1, 0), "west": (-1, 0)}[facing]
    t.set(x, y, z, color, {"facing": facing, "part": "foot", "occupied": "false"})
    t.set(x + dx, y, z + dz, color, {"facing": facing, "part": "head", "occupied": "false"})


def slab_top(t, x, y, z, name):
    t.set(x, y, z, name, {"type": "top", "waterlogged": "false"})


def leaves(t, x, y, z, kind="jungle_leaves"):
    if t.get(x, y, z) in (None, "minecraft:air"):
        t.set(x, y, z, kind, {"distance": "1", "persistent": "true", "waterlogged": "false"})


def palm(t, x, y, z, h=6):
    for i in range(h):
        t.set(x, y + i, z, "jungle_log", {"axis": "y"})
    top = y + h
    leaves(t, x, top, z)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for r in (1, 2, 3):
            leaves(t, x + dx * r, top - (1 if r == 3 else 0), z + dz * r)
    for dx, dz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        leaves(t, x + dx, top, z + dz)


def air_box(t, x1, y1, z1, x2, y2, z2):
    t.fill(x1, y1, z1, x2, y2, z2, "air")


def shell_room(t, x1, y1, z1, x2, y2, z2, pal, windows=True, rng=None, light_step=4):
    """Floor at y1, ceiling at y2, walls around, air inside."""
    t.fill(x1, y1, z1, x2, y1, z2, pal["floor"])
    t.fill(x1, y2, z1, x2, y2, z2, pal["wall"])
    for y in range(y1 + 1, y2):
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                edge = x in (x1, x2) or z in (z1, z2)
                corner = x in (x1, x2) and z in (z1, z2)
                if corner:
                    t.set(x, y, z, pal["trim"])
                elif edge:
                    win = windows and y in (y1 + 2, y1 + 3) and ((x - x1) % 3 == 1 if z in (z1, z2) else (z - z1) % 3 == 1)
                    t.set(x, y, z, pal["glass"] if win else pal["wall"])
                else:
                    t.set(x, y, z, "air")
    for x in range(x1 + 2, x2 - 1, light_step):
        for z in range(z1 + 2, z2 - 1, light_step):
            t.set(x, y2, z, pal["light"])


# =====================================================================================  furnishing

class Floor:
    """Interior of one floor: free cells we can put furniture / markers on."""

    def __init__(self, t, x1, z1, x2, z2, y, pal, rng):
        self.t, self.x1, self.z1, self.x2, self.z2, self.y, self.pal, self.rng = t, x1, z1, x2, z2, y, pal, rng
        self.used = set()

    def reserve(self, x, z):
        self.used.add((x, z))

    def free(self, x, z):
        return self.x1 <= x <= self.x2 and self.z1 <= z <= self.z2 and (x, z) not in self.used \
            and self.t.get(x, self.y, z) in (None, "minecraft:air")

    def wall_cells(self):
        cells = []
        for x in range(self.x1, self.x2 + 1):
            cells += [(x, self.z1), (x, self.z2)]
        for z in range(self.z1 + 1, self.z2):
            cells += [(self.x1, z), (self.x2, z)]
        return [c for c in cells if self.free(*c)]

    def inner_cells(self):
        return [(x, z) for x in range(self.x1 + 1, self.x2) for z in range(self.z1 + 1, self.z2) if self.free(x, z)]

    def take(self, wall=True):
        cells = self.wall_cells() if wall else self.inner_cells()
        if not cells:
            cells = self.wall_cells() + self.inner_cells()
        if not cells:
            return None
        c = self.rng.choice(cells)
        self.used.add(c)
        return c

    # ----- furniture (Changed's own furniture, with a vanilla stand-in when Changed has no such block)
    BENCH_TOPS = [("microscope", "minecraft:brewing_stand[has_bottle_0=true,has_bottle_2=true]"),
                  ("petri_dish", "minecraft:flower_pot"),
                  ("clipboard", ""),
                  ("book_stack", "minecraft:white_candle[candles=2]"),
                  ("canned_peaches", "minecraft:flower_pot"),
                  (None, "")]
    EXTRAS = [("cardboard_box", EMPTY_BARREL), ("box_pile", EMPTY_BARREL), ("iron_crate", EMPTY_BARREL),
              ("speaker", "minecraft:note_block")]

    def inward(self, x, z):
        """Direction from a cell at the wall into the room."""
        if z == self.z1: return "south"
        if z == self.z2: return "north"
        if x == self.x1: return "east"
        if x == self.x2: return "west"
        return self.rng.choice(list(DIRS))

    def table(self, x, z, facing):
        furn(self.t, x, self.y, z, "lab_table", facing, f"minecraft:{self.pal['slab']}[type=top]")

    def chair(self, x, z, look):
        """`look` is the way the seated person faces."""
        furn(self.t, x, self.y, z, "office_chair", look,
             f"minecraft:{self.pal['stairs']}[facing={{opposite}},half=bottom]")

    def bench(self):
        """Lab table with a microscope, petri dish, clipboard... on it."""
        c = self.take()
        if not c: return
        x, z = c
        f = self.inward(x, z)
        self.table(x, z, f)
        kind, fallback = self.rng.choice(self.BENCH_TOPS)
        if kind:
            furn(self.t, x, self.y + 1, z, kind, f, fallback)

    def console(self):
        """Desk with a computer and an office chair in front of it."""
        c = self.take()
        if not c: return
        x, z = c
        f = self.inward(x, z)
        self.table(x, z, f)
        furn(self.t, x, self.y + 1, z, "computer", f, "minecraft:daylight_detector")
        dx, dz = DIRS[f]
        if self.free(x + dx, z + dz):
            self.used.add((x + dx, z + dz))
            self.chair(x + dx, z + dz, OPPOSITE[f])

    def shelf(self):
        c = self.take()
        if not c: return
        x, z = c
        if self.rng.random() < 0.35:
            # a stack of boxes instead of a bookshelf
            f = self.inward(x, z)
            furn(self.t, x, self.y, z, "iron_crate", f, EMPTY_BARREL)
            furn(self.t, x, self.y + 1, z, "cardboard_box", f, "")
            return
        self.t.set(x, self.y, z, "bookshelf")
        self.t.set(x, self.y + 1, z, "bookshelf")

    def lounge(self):
        """A table in the room with chairs around it."""
        cells = self.inner_cells()
        self.rng.shuffle(cells)
        for x, z in cells:
            seats = [(d, x + dx, z + dz) for d, (dx, dz) in DIRS.items() if self.free(x + dx, z + dz)]
            if len(seats) < 2:
                continue
            self.used.add((x, z))
            self.table(x, z, "north")
            top = self.rng.choice([("canned_peaches", "minecraft:flower_pot"), ("book_stack", ""), ("clipboard", ""), None])
            if top:
                furn(self.t, x, self.y + 1, z, top[0], "north", top[1])
            self.rng.shuffle(seats)
            for d, sx, sz in seats[:self.rng.randint(2, 3)]:
                self.used.add((sx, sz))
                self.chair(sx, sz, OPPOSITE[d])
            return

    def extras(self, n=1):
        """Small things along the walls: cardboard boxes, box piles, crates, speakers."""
        for _ in range(n):
            c = self.take()
            if not c: return
            kind, fallback = self.rng.choice(self.EXTRAS)
            furn(self.t, c[0], self.y, c[1], kind, self.inward(*c), fallback)

    def crate(self, loot):
        c = self.take()
        if not c: return
        chest(self.t, c[0], self.y, c[1], loot, barrel=True)
        if self.rng.random() < 0.5:
            # a cardboard box beside the loot barrel
            for dx, dz in DIRS.values():
                if self.free(c[0] + dx, c[1] + dz) and (c[0] + dx, c[1] + dz) in set(self.wall_cells()):
                    self.used.add((c[0] + dx, c[1] + dz))
                    furn(self.t, c[0] + dx, self.y, c[1] + dz, "cardboard_box", self.inward(c[0] + dx, c[1] + dz), "")
                    break

    def chest(self, loot):
        c = self.take()
        if not c: return
        x, z = c
        facing = "south" if z == self.z1 else "north" if z == self.z2 else "east" if x == self.x1 else "west"
        chest(self.t, x, self.y, z, loot, facing=facing)

    def plant(self):
        c = self.take()
        if not c: return
        self.t.set(c[0], self.y, c[1], self.rng.choice(["potted_fern", "potted_bamboo", "potted_azalea_bush"]))

    def bed(self):
        for _ in range(12):
            c = self.take()
            if not c: return
            x, z = c
            if self.free(x, z + 1) and z + 1 <= self.z2:
                self.used.add((x, z + 1))
                bed(self.t, x, self.y, z, self.pal["bed"], "south")
                self.bedside(x, z + 1, ((1, 0), (-1, 0)))
                return
            if self.free(x + 1, z) and x + 1 <= self.x2:
                self.used.add((x + 1, z))
                bed(self.t, x, self.y, z, self.pal["bed"], "east")
                self.bedside(x + 1, z, ((0, 1), (0, -1)))
                return

    def bedside(self, hx, hz, sides):
        """IV rack next to the head of a bed (nothing if Changed has none)."""
        if self.rng.random() > 0.6: return
        for dx, dz in sides:
            if self.free(hx + dx, hz + dz):
                self.used.add((hx + dx, hz + dz))
                furn(self.t, hx + dx, self.y, hz + dz, "iv_rack", "north", "")
                return

    def goo(self, n=3):
        for _ in range(n):
            c = self.take(wall=False)
            if c:
                self.t.set(c[0], self.y, c[1], self.pal["goo"])
                self.used.discard(c)

    def marker(self, key, count=1):
        c = self.take(wall=False)
        if c:
            marker(self.t, c[0], self.y, c[1], key, count)

    def pod(self, latex=True, broken=False):
        """3x3 glass containment pod with a latex inside."""
        for _ in range(20):
            x = self.rng.randint(self.x1 + 1, self.x2 - 1)
            z = self.rng.randint(self.z1 + 1, self.z2 - 1)
            area = [(x + dx, z + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1)]
            if all(self.free(*c) for c in area):
                for cx, cz in area:
                    self.used.add((cx, cz))
                    self.t.set(cx, self.y - 1, cz, self.pal["trim"])
                    self.t.set(cx, self.y + 2, cz, self.pal["trim"])
                    if (cx, cz) != (x, z):
                        for yy in (self.y, self.y + 1):
                            self.t.set(cx, yy, cz, "air" if broken and self.rng.random() < 0.6 else "glass")
                self.t.set(x, self.y + 2, z, self.pal["light"])
                if latex:
                    marker(self.t, x, self.y, z, "latex")
                return True
        return False


# =====================================================================================  building blocks

def building(t, x0, z0, w, d, floors, fh, pal, rng, door_side="north", roof_access=True):
    """Multi-floor box building. Returns list of Floor objects."""
    result = []
    for f in range(floors):
        y = f * fh
        shell_room(t, x0, y, z0, x0 + w - 1, y + fh, z0 + d - 1, pal, rng=rng)
    top = floors * fh
    for x in range(x0, x0 + w):
        for z in range(z0, z0 + d):
            if x in (x0, x0 + w - 1) or z in (z0, z0 + d - 1):
                t.set(x, top + 1, z, pal["trim"])
    # ladder in south-east corner
    lx, lz = x0 + w - 2, z0 + d - 2
    ladder_top = top if (roof_access or floors > 1) else 0
    if floors > 1 or roof_access:
        ladder(t, lx, 1, ladder_top, lz, "north")
    # door
    dx = x0 + w // 2
    if door_side == "north":
        door(t, dx, 1, z0, "north", pal["door"])
        t.set(dx, 0, z0 - 1, pal["accent"])
    else:
        door(t, dx, 1, z0 + d - 1, "south", pal["door"])
    for f in range(floors):
        fl = Floor(t, x0 + 1, z0 + 1, x0 + w - 2, z0 + d - 2, f * fh + 1, pal, rng)
        fl.reserve(lx, lz)
        fl.reserve(lx, lz - 1)
        if f == 0:
            for zz in range(z0 + 1, z0 + 3):
                fl.reserve(dx, zz)
        result.append(fl)
    return result


def decay(t, rng, amount, keep_names=()):
    """Make a structure look abandoned: broken glass, dead lights, holes, cobwebs and moss."""
    for pos, (key, nbt) in list(t.blocks.items()):
        name = key[0]
        if nbt is not None or name in keep_names or "spawn_marker" in name:
            continue
        r = rng.random()
        if "glass" in name and r < amount * 2.5:
            t.set(*pos, "air")
        elif name == "minecraft:sea_lantern" and r < 0.8:
            t.set(*pos, "air" if r < 0.6 else "redstone_lamp", {"lit": "false"} if r >= 0.6 else None)
        elif name.endswith("concrete") and r < amount:
            t.set(*pos, rng.choice(["cobblestone", "andesite", "air", "gravel" if pos[1] == 0 else "andesite"]))
        elif name == "minecraft:stone_bricks" and r < amount * 2:
            t.set(*pos, "cracked_stone_bricks" if r < amount * 1.5 else "mossy_stone_bricks")
        elif name == "minecraft:deepslate_tiles" and r < amount * 2:
            t.set(*pos, "cracked_deepslate_tiles")
        elif name == "minecraft:air" and pos[1] > 0:
            below = t.get(pos[0], pos[1] - 1, pos[2])
            covered = any(t.get(pos[0], yy, pos[2]) not in (None, "minecraft:air") for yy in range(pos[1] + 1, pos[1] + 5))
            if r < amount * 0.35 and covered:
                t.set(*pos, "cobweb")
            elif below and below.endswith(("lab_tile_white", "lab_tile_black", "concrete")) and r < amount * 0.9:
                t.set(*pos, rng.choice(["moss_carpet", "black_carpet", "white_carpet"]))


# =====================================================================================  structure types

def research_outpost(rng, theme, abandoned=False):
    pal = THEMES[theme]
    w, d = rng.randint(11, 17), rng.randint(11, 15)
    floors = rng.choice([1, 1, 2])
    t = Template(w + 2, floors * 5 + 2, d + 2)
    fls = building(t, 1, 1, w, d, floors, 5, pal, rng)
    sign(t, 1 + w // 2 + 1, 3, 0, "north", ["THUNDER", "SCIENCE CO.", "Research", "Outpost"], pal["sign_color"])
    for i, fl in enumerate(fls):
        for _ in range(rng.randint(2, 4)): fl.bench()
        fl.console()
        if rng.random() < 0.6: fl.shelf()
        fl.chest("lab")
        if rng.random() < 0.5: fl.plant()
        if rng.random() < 0.7: fl.lounge()
        fl.extras(rng.randint(1, 2))
        if rng.random() < 0.5: fl.pod(latex=True, broken=abandoned)
        if abandoned:
            fl.goo(rng.randint(3, 6))
            fl.marker("latex", rng.randint(1, 2))
            if rng.random() < 0.3: fl.marker("scientist")
        else:
            if i == 0: fl.marker("guard")
            fl.marker("scientist")
            if rng.random() < 0.7: fl.marker("staff")
            if rng.random() < 0.3: fl.marker("puro")
    if abandoned:
        decay(t, rng, 0.12)
    return t


def watchtower(rng, theme):
    pal = THEMES[theme]
    h = rng.randint(13, 18)
    t = Template(9, h + 4, 9)
    for y in range(0, h):
        for x, z in ((2, 2), (6, 2), (2, 6), (6, 6)):
            t.set(x, y, z, pal["trim"])
    t.fill(3, 0, 3, 5, 0, 5, pal["floor"])
    ladder(t, 4, 1, h, 4, "north")
    t.set(4, 0, 5, pal["wall"])
    for y in range(1, h + 1):
        t.set(4, y, 5, pal["wall"])
    # platform
    t.fill(1, h, 1, 7, h, 7, pal["floor"])
    t.set(4, h, 4, "ladder", {"facing": "north", "waterlogged": "false"})
    for x in range(1, 8):
        for z in range(1, 8):
            if x in (1, 7) or z in (1, 7):
                t.set(x, h + 1, z, pal["wall"])
    for x, z in ((1, 1), (7, 1), (1, 7), (7, 7)):
        t.set(x, h + 2, z, pal["trim"])
        t.set(x, h + 3, z, pal["light"])
    air_box(t, 2, h + 1, 2, 6, h + 3, 6)
    t.set(4, h + 1, 5, "air")
    marker(t, 3, h + 1, 3, "guard")
    marker(t, 5, h + 1, 3, "guard")
    chest(t, 6, h + 1, 6, "armory", "west")
    furn(t, 2, h + 1, 6, "lab_table", "north", f"minecraft:{pal['slab']}[type=top]")
    furn(t, 2, h + 2, 6, "computer", "north", "minecraft:daylight_detector")
    furn(t, 2, h + 1, 5, "office_chair", "south", f"minecraft:{pal['stairs']}[facing={{opposite}},half=bottom]")
    furn(t, 6, h + 1, 2, "cardboard_box", "south", EMPTY_BARREL)
    marker(t, 4, 1, 3, "staff") if rng.random() < 0.4 else None
    return t


def checkpoint(rng, theme):
    pal = THEMES[theme]
    t = Template(19, 6, 9)
    # barrier walls with hazard stripes
    for x in list(range(0, 7)) + list(range(12, 19)):
        for y in range(0, 3):
            t.set(x, y, 4, "yellow_concrete" if (x + y) % 2 == 0 else "black_concrete")
    # gate posts
    for x in (7, 11):
        for y in range(0, 4):
            t.set(x, y, 4, pal["trim"])
        t.set(x, 4, 4, pal["light"])
    # booth
    shell_room(t, 13, 0, 0, 17, 4, 3, pal, rng=rng)
    door(t, 13, 1, 1, "west", pal["door"])
    fl = Floor(t, 14, 1, 16, 2, 1, pal, rng)
    fl.reserve(14, 1)
    fl.console()
    fl.chest("armory")
    sign(t, 14, 3, 4, "south", ["TSC", "CHECKPOINT", "Show your", "keycard"], pal["sign_color"])
    marker(t, 9, 0, 6, "guard")
    marker(t, 9, 0, 2, "guard")
    if rng.random() < 0.5: marker(t, 3, 0, 6, "staff")
    t.fill(8, 0, 4, 10, 0, 4, "smooth_stone")
    return t


def bunker(rng, theme="bunker", rooms=3, latex_heavy=False):
    pal = THEMES[theme]
    rw, rd, gap = 9, 9, 3
    sx = rooms * rw + (rooms - 1) * gap + 2
    t = Template(sx, 8, rd + 2)
    # solid shell first
    t.fill(0, 0, 0, sx - 1, 7, rd + 1, SOLID_SHELL)
    x = 1
    fls = []
    for i in range(rooms):
        shell_room(t, x, 1, 1, x + rw - 1, 6, rd, pal, windows=False, rng=rng)
        fl = Floor(t, x + 1, 2, x + rw - 2, rd - 1, 2, pal, rng)
        fls.append((fl, x))
        if i < rooms - 1:
            cx = x + rw
            t.fill(cx - 1, 2, rd // 2, cx + gap, 3, rd // 2 + 2, "air")
            t.fill(cx - 1, 1, rd // 2, cx + gap, 1, rd // 2 + 2, pal["floor"])
            t.fill(cx - 1, 4, rd // 2, cx + gap, 4, rd // 2 + 2, pal["accent"])
            for xx in range(cx, cx + gap):
                t.set(xx, 4, rd // 2 + 1, pal["light"])
        x += rw + gap
    for i, (fl, _) in enumerate(fls):
        kind = rng.choice(["lab", "lab", "dorm", "storage", "containment"])
        if kind == "lab":
            for _ in range(3): fl.bench()
            fl.console(); fl.chest("lab"); fl.extras(1)
            fl.marker("scientist")
        elif kind == "dorm":
            for _ in range(3): fl.bed()
            fl.crate("supply"); fl.lounge(); fl.marker("staff")
        elif kind == "storage":
            for _ in range(4): fl.crate("supply")
            fl.chest("armory"); fl.extras(3); fl.marker("guard")
        else:
            fl.pod(); fl.pod(broken=latex_heavy); fl.console(); fl.marker("guard")
        if latex_heavy:
            fl.goo(4); fl.marker("latex", 2)
        elif rng.random() < 0.3:
            fl.marker("latex")
    if latex_heavy:
        decay(t, rng, 0.08, keep_names=("minecraft:" + SOLID_SHELL,))
    return t


def containment_wing(rng):
    pal = THEMES["bunker"]
    cells = rng.randint(3, 5)
    length = cells * 5 + 8
    t = Template(length, 7, 15)
    t.fill(0, 0, 0, length - 1, 6, 14, SOLID_SHELL)
    # corridor
    t.fill(1, 1, 6, length - 2, 1, 8, pal["floor"])
    air_box(t, 1, 2, 6, length - 2, 4, 8)
    t.fill(1, 5, 6, length - 2, 5, 8, pal["accent"])
    for xx in range(2, length - 2, 3):
        t.set(xx, 5, 7, pal["light"])
    # cells both sides
    for i in range(cells):
        cx = 2 + i * 5
        for side, (z1, z2, front) in enumerate(((1, 5, 5), (9, 13, 9))):
            t.fill(cx, 1, z1, cx + 3, 1, z2, pal["floor"])
            air_box(t, cx + 1, 2, min(z1, z2) + (0 if side == 0 else 0), cx + 2, 4, max(z1, z2) - (1 if side == 0 else 0))
            for xx in range(cx, cx + 4):
                for yy in range(2, 5):
                    t.set(xx, yy, front, "glass" if xx in (cx + 1, cx + 2) else pal["trim"])
            broken = rng.random() < 0.3
            if broken:
                t.set(cx + 1, 2, front, "air"); t.set(cx + 1, 3, front, "air")
                t.set(cx + 1, 2, front - (1 if side == 0 else -1), pal["goo"])
            inner_z = (z1 + 2) if side == 0 else (z2 - 2)
            marker(t, cx + 1 + rng.randint(0, 1), 2, inner_z, "latex")
            t.set(cx + 1, 5, inner_z, pal["light"])
    # control room at the end
    cx = length - 6
    t.fill(cx, 1, 1, length - 2, 1, 5, pal["floor"])
    air_box(t, cx, 2, 1, length - 2, 4, 5)
    fl = Floor(t, cx, 1, length - 2, 4, 2, pal, rng)
    fl.console(); fl.console(); fl.chest("armory"); fl.chest("lab")
    fl.marker("guard"); fl.marker("scientist")
    sign(t, 1, 3, 7, "east", ["CONTAINMENT", "WING", "Authorized", "staff only"], pal["sign_color"])
    return t


def aquatic_station(rng, boss=None):
    pal = THEMES["white"]
    r = rng.randint(8, 10)
    size = 2 * r + 3
    t = Template(size, r + 3, size)
    c = size // 2
    # platform
    for x in range(size):
        for z in range(size):
            dist = math.hypot(x - c, z - c)
            if dist <= r + 0.5:
                t.set(x, 0, z, pal["trim"] if dist > r - 1 else pal["floor"])
    # dome
    for x in range(size):
        for z in range(size):
            for y in range(1, r + 2):
                dist = math.sqrt((x - c) ** 2 + (z - c) ** 2 + (y * 1.0) ** 2)
                if dist <= r + 0.5:
                    if dist > r - 0.6:
                        rib = (x == c or z == c)
                        t.set(x, y, z, pal["trim"] if rib else pal["glass"])
                    else:
                        t.set(x, y, z, "air")
    for y in (r - 1,):
        t.set(c, y, c, pal["light"])
    for dx, dz in ((4, 0), (-4, 0), (0, 4), (0, -4)):
        t.set(c + dx, 0, c + dz, pal["light"])
    fl = Floor(t, c - r + 2, c - r + 2, c + r - 2, c + r - 2, 1, pal, rng)
    for x in range(size):
        for z in range(size):
            if math.hypot(x - c, z - c) > r - 2.2:
                fl.reserve(x, z)
    if boss:
        # central pool with the boss
        for x in range(c - 3, c + 4):
            for z in range(c - 3, c + 4):
                fl.reserve(x, z)
                if math.hypot(x - c, z - c) <= 3.2:
                    t.set(x, 0, z, "water", {"level": "0"})
                    t.set(x, -1, z, pal["trim"])
        marker(t, c, 1, c, "boss:" + boss)
        fl.chest("vault"); fl.chest("vault")
        fl.marker("guard"); fl.marker("guard")
    else:
        for _ in range(3): fl.bench()
        fl.console(); fl.chest("lab"); fl.crate("supply"); fl.lounge(); fl.extras(1)
        fl.marker("scientist"); fl.marker("staff")
        if rng.random() < 0.5: fl.pod()
        if rng.random() < 0.4: fl.marker("latex")
    return t


def supply_depot(rng, theme):
    pal = THEMES[theme]
    w, d = rng.randint(15, 19), rng.randint(13, 17)
    t = Template(w, 7, d)
    t.fill(0, 0, 0, w - 1, 0, d - 1, "gravel")
    for x in range(w):
        for z in range(d):
            if x in (0, w - 1) or z in (0, d - 1):
                t.set(x, 1, z, pal["accent"])
                t.set(x, 2, z, "spruce_planks" if (x + z) % 4 else pal["trim"])
    gx = w // 2
    for gxx in (gx - 1, gx, gx + 1):
        t.set(gxx, 1, 0, "air"); t.set(gxx, 2, 0, "air")
    air_box(t, 1, 1, 1, w - 2, 4, d - 2)
    # shed
    shell_room(t, w - 7, 0, d - 7, w - 2, 4, d - 2, pal, rng=rng)
    door(t, w - 5, 1, d - 7, "north", pal["door"])
    fl = Floor(t, w - 6, d - 6, w - 3, d - 3, 1, pal, rng)
    fl.reserve(w - 5, d - 6)
    fl.chest("armory"); fl.chest("supply"); fl.console(); fl.extras(1)
    # crate stacks
    for _ in range(rng.randint(5, 8)):
        x, z = rng.randint(2, w - 9), rng.randint(2, d - 3)
        chest(t, x, 1, z, "supply", barrel=True)
        if rng.random() < 0.5:
            t.set(x, 2, z, "barrel", {"facing": "up", "open": "false"})
    marker(t, gx, 1, 2, "guard")
    marker(t, 3, 1, d - 3, "staff")
    if rng.random() < 0.5: marker(t, w // 3, 1, d // 2, "guard")
    standing_sign(t, gx + 2, 3, 0, 8, ["TSC SUPPLY", "DEPOT"], "black")
    return t


def medical_station(rng, theme):
    pal = THEMES[theme]
    w, d = rng.randint(11, 15), rng.randint(9, 13)
    t = Template(w + 2, 8, d + 2)
    fls = building(t, 1, 1, w, d, 1, 5, pal, rng, roof_access=False)
    fl = fls[0]
    for _ in range(rng.randint(3, 5)): fl.bed()
    fl.chest("medical"); fl.chest("medical"); fl.bench(); fl.bench()
    fl.console(); fl.lounge(); fl.extras(1)
    fl.marker("scientist"); fl.marker("scientist")
    if rng.random() < 0.5: fl.marker("guard")
    if rng.random() < 0.35: fl.marker("puro")
    # green cross emblem on the roof
    cx, cz = 1 + w // 2, 1 + d // 2
    for i in range(-2, 3):
        t.set(cx + i, 5, cz, "lime_concrete")
        t.set(cx, 5, cz + i, "lime_concrete")
    sign(t, 1 + w // 2 + 1, 3, 0, "north", ["TSC", "FIELD", "MEDICAL", "STATION"], pal["sign_color"])
    return t


def comm_relay(rng, theme):
    pal = THEMES[theme]
    h = rng.randint(18, 24)
    t = Template(11, h + 3, 11)
    shell_room(t, 0, 0, 0, 5, 4, 5, pal, rng=rng)
    door(t, 2, 1, 0, "north", pal["door"])
    fl = Floor(t, 1, 1, 4, 4, 1, pal, rng)
    fl.reserve(2, 1)
    fl.console(); fl.console(); fl.chest("supply"); fl.marker("staff")
    for y in range(0, h):
        for x, z in ((7, 7), (9, 7), (7, 9), (9, 9)):
            t.set(x, y, z, "iron_block" if y % 4 else pal["trim"])
        if y % 4 == 0:
            t.fill(7, y, 7, 9, y, 9, "iron_block")
    t.set(8, h, 8, "iron_block")
    t.set(8, h + 1, 8, "lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    t.set(8, h - 1, 8, "redstone_lamp", {"lit": "false"})
    marker(t, 6, 1, 3, "guard")
    return t


def helipad(rng, theme):
    pal = THEMES[theme]
    t = Template(17, 6, 17)
    t.fill(0, 0, 0, 16, 0, 16, "black_concrete")
    for x in range(17):
        for z in range(17):
            if x in (0, 16) or z in (0, 16):
                t.set(x, 0, z, "yellow_concrete")
    for z in range(4, 13):
        t.set(5, 0, z, "white_concrete"); t.set(11, 0, z, "white_concrete")
    for x in range(5, 12):
        t.set(x, 0, 8, "white_concrete")
    for x, z in ((0, 0), (16, 0), (0, 16), (16, 16)):
        t.set(x, 1, z, pal["light"])
    air_box(t, 0, 1, 1, 16, 5, 15)
    for x, z in ((0, 0), (16, 0), (0, 16), (16, 16)):
        t.set(x, 1, z, pal["light"])
    chest(t, 1, 1, 8, "armory", "east")
    chest(t, 15, 1, 8, "supply", "west")
    for x, z, kind in ((1, 7, "iron_crate"), (1, 9, "cardboard_box"), (15, 7, "box_pile"), (15, 9, "iron_crate"),
                       (2, 14, "cardboard_box"), (14, 2, "speaker")):
        furn(t, x, 1, z, kind, "north", "minecraft:note_block" if kind == "speaker" else EMPTY_BARREL)
    marker(t, 8, 1, 2, "guard"); marker(t, 8, 1, 14, "guard"); marker(t, 3, 1, 3, "staff")
    return t


def quarantine_camp(rng):
    pal = THEMES["white"]
    t = Template(23, 6, 23)
    air_box(t, 0, 1, 0, 22, 5, 22)
    t.set(11, 0, 11, "campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    for i, (ox, oz) in enumerate(((2, 2), (14, 2), (2, 14), (14, 14))):
        color = "white_wool" if i % 2 == 0 else "black_wool"
        for x in range(ox, ox + 7):
            for z in range(oz, oz + 6):
                edge_x = x in (ox, ox + 6)
                if edge_x or z in (oz, oz + 5):
                    t.set(x, 1, z, color)
                    if not edge_x or z in (oz, oz + 5):
                        t.set(x, 2, z, color)
                t.set(x, 3, z, color)
        t.set(ox + 3, 1, oz, "air"); t.set(ox + 3, 2, oz, "air")
        fl = Floor(t, ox + 1, oz + 1, ox + 5, oz + 4, 1, pal, rng)
        fl.reserve(ox + 3, oz + 1)
        fl.bed(); fl.bed()
        fl.crate("medical" if i == 0 else "supply")
        fl.marker(["scientist", "staff", "guard", "staff"][i])
    standing_sign(t, 11, 1, 8, 8, ["QUARANTINE", "ZONE", "Latex exposure", "risk"], "black")
    t.set(11, 0, 8, "spruce_planks")
    marker(t, 11, 1, 19, "latex"); marker(t, 19, 1, 11, "latex")
    if rng.random() < 0.5: marker(t, 5, 1, 11, "puro")
    return t


# =====================================================================================  big ones

BOLT = [  # simple lightning bolt, 5 wide x 9 tall (top row first)
    "..###",
    ".###.",
    ".##..",
    "####.",
    ".####",
    "..##.",
    ".##..",
    ".#...",
    "#....",
]


def tsc_tower(rng):
    """Thunder Science Company Tower: 16 floors + roof, Wolf King on the top floor."""
    pal = dict(THEMES["white"])
    S, F, FH = 25, 16, 7
    H = F * FH + 8
    t = Template(S, H, S)
    # plaza
    t.fill(0, 0, 0, S - 1, 0, S - 1, "smooth_stone")
    for x in range(S):
        t.set(x, 0, 0, "yellow_concrete"); t.set(x, 0, S - 1, "yellow_concrete")
    air_box(t, 0, 1, 0, S - 1, 3, 1)
    x1, x2 = 2, S - 3
    core = (11, 13)
    for f in range(F):
        y0 = f * FH
        t.fill(x1, y0, x1, x2, y0, x2, pal["floor"] if f < F - 1 else "black_concrete")
        for y in range(y0 + 1, y0 + FH):
            for x in range(x1, x2 + 1):
                for z in range(x1, x2 + 1):
                    edge = x in (x1, x2) or z in (x1, x2)
                    if not edge:
                        t.set(x, y, z, "air")
                        continue
                    pillar = (x - x1) % 5 == 0 and (z - x1) % 5 == 0 or (x in (x1, x2) and z in (x1, x2))
                    band = y == y0 + 1 or y == y0 + FH - 1
                    if pillar:
                        t.set(x, y, z, pal["trim"])
                    elif band:
                        t.set(x, y, z, pal["wall"])
                    else:
                        t.set(x, y, z, pal["glass"])
        # ceiling lights
        for x in range(x1 + 3, x2 - 1, 4):
            for z in range(x1 + 3, x2 - 1, 4):
                if not (core[0] - 1 <= x <= core[1] + 1 and core[0] - 1 <= z <= core[1] + 1):
                    t.set(x, y0 + FH, z, pal["light"])
    t.fill(x1, F * FH, x1, x2, F * FH, x2, pal["wall"])
    # ladder core: shaft at (12,12), walls around, opening north each floor
    for y in range(1, F * FH + 1):
        for (x, z) in ((11, 12), (13, 12), (12, 13), (11, 13), (13, 13), (11, 11), (13, 11)):
            t.set(x, y, z, pal["trim"])
        t.set(12, y, 12, "ladder", {"facing": "north", "waterlogged": "false"})
        t.set(12, y, 11, pal["trim"])
    for f in range(F):
        y0 = f * FH
        t.set(12, y0 + 1, 11, "air"); t.set(12, y0 + 2, 11, "air")
        sign(t, 11, y0 + 3, 10, "north", ["FLOOR " + str(f + 1)], "black")
    # entrance
    door(t, 12, 1, x1, "north", "iron_door")
    t.set(11, 1, x1 - 1, "stone_button", {"face": "wall", "facing": "north", "powered": "false"})
    t.set(13, 1, x1 + 1, "stone_button", {"face": "wall", "facing": "south", "powered": "false"})
    sign(t, 10, 3, x1 - 1, "north", ["THUNDER", "SCIENCE", "COMPANY", "TOWER"], "black")
    sign(t, 14, 3, x1 - 1, "north", ["Welcome to", "TSC HQ", "Visitors must", "sign in"], "black")
    # lightning bolt emblem on the north and south facades (floors 13-14)
    for row, line in enumerate(BOLT):
        for col, ch in enumerate(line):
            if ch == "#":
                y = 13 * FH - row
                t.set(10 + col, y, x1, f"{MOD}:tsc_emblem_block")
                t.set(14 - col, y, x2, f"{MOD}:tsc_emblem_block")

    programs = (["lobby", "armory", "office", "office", "lab", "lab", "lab", "medical", "cafeteria",
                 "containment", "containment", "containment", "server", "assimilation", "executive", "arena"])
    for f, prog in enumerate(programs):
        y = f * FH + 1
        fl = Floor(t, x1 + 1, x1 + 1, x2 - 1, x2 - 1, y, pal, rng)
        for x in range(core[0] - 1, core[1] + 2):
            for z in range(core[0] - 2, core[1] + 2):
                fl.reserve(x, z)
        if f == 0:
            for z in range(x1 + 1, core[0] - 1):
                fl.reserve(12, z)
        if prog == "lobby":
            for x in range(8, 17):
                slab_top(t, x, y, 7, pal["slab"]); fl.reserve(x, 7)
            for _ in range(4): fl.plant()
            fl.crate("supply"); fl.console(); fl.lounge(); fl.lounge(); fl.extras(2)
            fl.marker("guard"); fl.marker("guard"); fl.marker("guard"); fl.marker("staff")
        elif prog == "armory":
            for _ in range(4): fl.chest("armory")
            for _ in range(3): fl.console()
            fl.extras(3)
            fl.marker("guard", 2)
        elif prog == "office":
            for _ in range(5): fl.shelf()
            for _ in range(6): fl.console()
            fl.chest("lab"); fl.plant(); fl.plant(); fl.lounge(); fl.extras(2)
            fl.marker("staff"); fl.marker("staff"); fl.marker("scientist")
        elif prog == "lab":
            for _ in range(8): fl.bench()
            fl.console(); fl.console(); fl.chest("lab"); fl.chest("lab")
            fl.pod(); fl.pod(); fl.extras(2)
            fl.marker("scientist"); fl.marker("scientist"); fl.marker("staff")
        elif prog == "medical":
            for _ in range(6): fl.bed()
            fl.chest("medical"); fl.chest("medical"); fl.bench(); fl.bench(); fl.console(); fl.extras(1)
            fl.marker("scientist", 2)
        elif prog == "cafeteria":
            for _ in range(6): fl.lounge()
            for _ in range(2): fl.bench()
            for _ in range(3): fl.crate("supply")
            fl.extras(2)
            fl.marker("staff", 2); fl.marker("guard")
        elif prog == "containment":
            for _ in range(5): fl.pod(broken=(f == 11 and rng.random() < 0.5))
            fl.console(); fl.chest("armory")
            fl.marker("guard"); fl.marker("guard")
            if f == 11: fl.goo(6); fl.marker("latex", 2)
        elif prog == "server":
            for x in range(x1 + 2, x2 - 1, 3):
                for z in range(x1 + 2, x2 - 1):
                    if fl.free(x, z) and z % 6 != 0:
                        t.set(x, y, z, "black_concrete"); t.set(x, y + 1, z, "redstone_lamp", {"lit": "false"})
                        t.set(x, y + 2, z, "black_concrete"); fl.reserve(x, z)
            fl.marker("staff"); fl.chest("lab"); fl.console(); fl.console()
        elif prog == "assimilation":
            for _ in range(4): fl.pod()
            for _ in range(4): fl.bench()
            fl.chest("lab"); fl.marker("puro"); fl.marker("puro"); fl.marker("scientist", 2)
        elif prog == "executive":
            for _ in range(4): fl.shelf()
            fl.chest("vault"); fl.chest("vault"); fl.plant(); fl.plant(); fl.console(); fl.console()
            fl.lounge(); fl.lounge()
            fl.marker("guard", 2); fl.marker("scientist")
        elif prog == "arena":
            for (x, z) in ((x1 + 1, x1 + 1), (x2 - 1, x1 + 1), (x1 + 1, x2 - 1), (x2 - 1, x2 - 1)):
                chest(t, x, y, z, "vault", "south" if z == x1 + 1 else "north")
            for x in range(x1 + 3, x2 - 2, 4):
                for z in range(x1 + 3, x2 - 2, 4):
                    if fl.free(x, z):
                        t.set(x, y, z, "obsidian")
            marker(t, 12, y, 6, "boss:wolf_king")
            sign(t, 11, y + 2, 10, "north", ["WARNING", "Subject:", "WOLF KING", "Do not enter"], "red")
    # roof: helipad + antenna
    ry = F * FH
    for x in range(x1, x2 + 1):
        for z in range(x1, x2 + 1):
            if x in (x1, x2) or z in (x1, x2):
                t.set(x, ry + 1, z, pal["trim"])
            else:
                for yy in range(ry + 1, H):
                    t.set(x, yy, z, "air")
    for z in range(8, 17):
        t.set(8, ry, z, "yellow_concrete"); t.set(16, ry, z, "yellow_concrete")
    for x in range(8, 17):
        t.set(x, ry, 12, "yellow_concrete")
    t.set(12, ry, 12, "ladder", {"facing": "north", "waterlogged": "false"})
    for y in range(ry + 1, H - 1):
        t.set(4, y, 4, "iron_block")
    t.set(4, H - 1, 4, "lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    marker(t, 6, ry + 1, 18, "guard")
    return t


def kaiju_paradise(rng):
    """Cameo: an abandoned tropical latex 'paradise' park with a lagoon; Squid Dog sleeps in the deep tank."""
    S, B0 = 41, 8  # B0 = plaza level (template is sunk 8 blocks)
    H = B0 + 16
    t = Template(S, H, S)
    colors = ["cyan_concrete", "magenta_concrete", "yellow_concrete", "lime_concrete", "light_blue_concrete"]
    # foundation + plaza
    t.fill(0, 0, 0, S - 1, B0 - 1, S - 1, "stone")
    for x in range(S):
        for z in range(S):
            t.set(x, B0, z, colors[((x // 3) + (z // 3)) % len(colors)] if (x + z) % 7 else "smooth_sandstone")
    air_box(t, 0, B0 + 1, 0, S - 1, H - 1, S - 1)
    c = S // 2
    # lagoon
    for x in range(S):
        for z in range(S):
            dist = math.hypot(x - c, z - (c + 4))
            if dist <= 8:
                for y in range(B0 - 3, B0 + 1):
                    t.set(x, y, z, "water", {"level": "0"})
                t.set(x, B0 - 4, z, "prismarine_bricks")
            elif dist <= 9:
                t.set(x, B0, z, "dark_prismarine")
    # deep tank chamber under the lagoon's north rim (Squid Dog)
    tx1, tz1, tx2, tz2 = c - 6, 2, c + 6, 10
    t.fill(tx1 - 1, 0, tz1 - 1, tx2 + 1, B0 - 1, tz2 + 1, "prismarine_bricks")
    air_box(t, tx1, 1, tz1, tx2, B0 - 2, tz2)
    for x in range(tx1, tx2 + 1):
        for z in range(tz1, tz2 + 1):
            t.set(x, 0, z, "dark_prismarine")
            if math.hypot(x - c, z - 6) <= 3:
                t.set(x, 1, z, "water", {"level": "0"})
    for x in range(tx1 + 1, tx2, 4):
        t.set(x, B0 - 1, tz1 + 1, "sea_lantern"); t.set(x, B0 - 1, tz2 - 1, "sea_lantern")
    marker(t, c, 2, 6, "boss:squid_dog")
    chest(t, tx1, 1, tz1, "vault", "south"); chest(t, tx2, 1, tz1, "vault", "south")
    # dome pavilion on top of the tank with ladder down
    for x in range(tx1, tx2 + 1):
        for z in range(tz1, tz2 + 1):
            for y in range(B0 + 1, B0 + 7):
                dist = math.sqrt(((x - c) / 6.5) ** 2 + ((z - 6) / 4.5) ** 2 + ((y - B0) / 6.0) ** 2)
                if 0.85 < dist <= 1.0:
                    t.set(x, y, z, "light_blue_stained_glass" if (x + y) % 3 else "magenta_concrete")
    ladder(t, c, 1, B0, tz2, "north")
    t.set(c, B0 - 1, tz2 + 1, "prismarine_bricks")
    for y in range(1, B0 + 1):
        t.set(c, y, tz2 + 1, "prismarine_bricks")
    for y in (B0 + 1, B0 + 2):
        for x in (c - 1, c, c + 1):
            if t.get(x, y, tz2) and "glass" in t.get(x, y, tz2):
                t.set(x, y, tz2, "air")
    # entrance arch + sign
    for y in range(B0 + 1, B0 + 7):
        t.set(c - 4, y, S - 2, "magenta_concrete"); t.set(c + 4, y, S - 2, "magenta_concrete")
    for x in range(c - 4, c + 5):
        t.set(x, B0 + 7, S - 2, "cyan_concrete")
    for x in range(c - 3, c + 4):
        t.set(x, B0 + 6, S - 2, "yellow_concrete")
        t.set(x, B0 + 5, S - 2, "yellow_concrete")
    sign(t, c - 1, B0 + 5, S - 1, "south", ["~ KAIJU ~", "PARADISE", "Now closed", "for repairs"], "cyan")
    sign(t, c + 1, B0 + 5, S - 1, "south", ["Please do not", "feed the", "latex"], "magenta")
    # booths
    for i, (bx, bz) in enumerate(((3, 22), (3, 30), (32, 22), (32, 30), (3, 14), (32, 14))):
        color = ["red_wool", "blue_wool", "yellow_wool", "lime_wool", "orange_wool", "purple_wool"][i]
        for x in range(bx, bx + 6):
            for z in range(bz, bz + 5):
                t.set(x, B0 + 4, z, color)
                if (x in (bx, bx + 5)) and (z in (bz, bz + 4)):
                    for y in range(B0 + 1, B0 + 4):
                        t.set(x, y, z, "stripped_spruce_log", {"axis": "y"})
        slab_top(t, bx + 1, B0 + 1, bz + 2, "spruce_slab"); slab_top(t, bx + 2, B0 + 1, bz + 2, "spruce_slab")
        chest(t, bx + 3, B0 + 1, bz + 2, "supply", barrel=True)
        # stools at the counter, things left on it, and boxes behind
        stairs = "minecraft:spruce_stairs[facing={opposite},half=bottom]"
        furn(t, bx + 1, B0 + 1, bz + 3, "office_chair", "north", stairs)
        furn(t, bx + 2, B0 + 1, bz + 3, "office_chair", "north", stairs)
        furn(t, bx + 1, B0 + 2, bz + 2, ["canned_peaches", "book_stack", "clipboard"][i % 3], "south", "minecraft:flower_pot")
        furn(t, bx + 4, B0 + 1, bz + 1, ["cardboard_box", "box_pile", "speaker"][i % 3], "south",
             "minecraft:note_block" if i % 3 == 2 else EMPTY_BARREL)
    # palms
    for (px, pz) in ((2, 2), (S - 3, 2), (2, S - 4), (S - 3, S - 4), (12, 34), (28, 34), (10, 12), (30, 12)):
        palm(t, px, B0 + 1, pz, rng.randint(5, 7))
    # inhabitants
    for _ in range(7):
        marker(t, rng.randint(4, S - 5), B0 + 1, rng.randint(12, S - 6), "latex")
    marker(t, 8, B0 + 1, 20, "puro"); marker(t, 33, B0 + 1, 20, "puro")
    marker(t, c, B0 + 1, S - 5, "scientist")
    decay(t, rng, 0.05, keep_names=("minecraft:water", "minecraft:stone", "minecraft:prismarine_bricks"))
    return t


def laminax_underground(rng):
    """Cameo: the abandoned Laminax underground complex, three levels around an atrium. The Elder One waits below."""
    pal = THEMES["bunker"]
    S, LV, LH = 47, 3, 8
    H = LV * LH + 2
    t = Template(S, H, S)
    t.fill(0, 0, 0, S - 1, H - 1, S - 1, "deepslate_bricks")
    c = S // 2
    for lv in range(LV):
        y0 = lv * LH + 1
        # corridors (grid)
        for k in (8, c, S - 9):
            t.fill(2, y0, k - 1, S - 3, y0, k + 1, "polished_deepslate")
            air_box(t, 2, y0 + 1, k - 1, S - 3, y0 + 4, k + 1)
            t.fill(k - 1, y0, 2, k + 1, y0, S - 3, "polished_deepslate")
            air_box(t, k - 1, y0 + 1, 2, k + 1, y0 + 4, S - 3)
            for i in range(4, S - 4, 6):
                t.set(i, y0 + 5, k, "sea_lantern"); t.set(k, y0 + 5, i, "sea_lantern")
        # rooms in the grid cells
        for (rx, rz) in ((10, 10), (c + 2, 10), (10, c + 2), (c + 2, c + 2)):
            w = c - 12
            shell_room(t, rx, y0, rz, rx + w, y0 + 5, rz + w, pal, windows=False, rng=rng)
            for k in range(rx + 3, rx + w - 2):
                t.set(k, y0 + 1, rz, "air"); t.set(k, y0 + 2, rz, "air")
                break
            fl = Floor(t, rx + 1, rz + 1, rx + w - 1, rz + w - 1, y0 + 1, pal, rng)
            kind = rng.choice(["lab", "lab", "dorm", "storage", "containment"])
            if kind == "lab":
                for _ in range(4): fl.bench()
                fl.chest("lab"); fl.console(); fl.extras(1)
            elif kind == "dorm":
                for _ in range(4): fl.bed()
                fl.crate("abandoned"); fl.lounge()
            elif kind == "storage":
                for _ in range(5): fl.crate("abandoned")
                fl.chest("armory"); fl.extras(2)
            else:
                fl.pod(broken=True); fl.pod(broken=True); fl.console()
            fl.goo(5); fl.marker("latex", rng.randint(1, 2))
        # stairs/ladders between levels
        if lv < LV - 1:
            ladder(t, 3, y0 + 1, y0 + LH + 1, 8 - 2, "south")
            t.set(3, y0 + LH, 8 - 2, "ladder", {"facing": "south", "waterlogged": "false"})
            ladder(t, S - 4, y0 + 1, y0 + LH + 1, S - 9 + 2, "north")
    # atrium through all levels, Elder One at the bottom
    for x in range(c - 6, c + 7):
        for z in range(c - 6, c + 7):
            for y in range(2, H - 2):
                t.set(x, y, z, "air")
            t.set(x, 1, z, "sculk" if rng.random() < 0.5 else "polished_deepslate")
    for x in range(c - 6, c + 7):
        for z in (c - 6, c + 6):
            for lv in range(1, LV):
                t.set(x, lv * LH + 1, z, "chain", {"axis": "x", "waterlogged": "false"})
    marker(t, c, 2, c, "boss:elder_one")
    for (x, z) in ((c - 5, c - 5), (c + 5, c - 5), (c - 5, c + 5), (c + 5, c + 5)):
        chest(t, x, 2, z, "vault")
        t.set(x, H - 3, z, "sea_lantern")
    # signs + a lone survivor
    sign(t, 4, 3, 7, "south", ["LAMINAX", "UNDERGROUND", "Sector 7", "ABANDONED"], "yellow")
    sign(t, c - 7, 4, c, "west", ["DANGER", "Do not wake", "THE ELDER", "ONE"], "red")
    marker(t, 5, 2, c, "scientist")
    marker(t, 6, LH + 2, 8, "puro")
    # flooding
    for _ in range(25):
        x, z = rng.randint(3, S - 4), rng.randint(3, S - 4)
        if t.get(x, 1, z) == "minecraft:polished_deepslate" and t.get(x, 2, z) == "minecraft:air":
            t.set(x, 1, z, "water", {"level": "0"})
    decay(t, rng, 0.10, keep_names=("minecraft:deepslate_bricks", "minecraft:water"))
    return t


# =====================================================================================  catalogue

GREEK = ["alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa"]

# (id, builder, placement) placement = land | ocean | underground | flat
CATALOGUE = []


def add(name, fn, place, spacing=38, separation=14):
    CATALOGUE.append((name, fn, place, spacing, separation))


for i, g in enumerate(GREEK):
    add(f"research_outpost_{g}", lambda r, th=("white" if i % 2 == 0 else "black"): research_outpost(r, th), "land")
for i in range(6):
    add(f"guard_watchtower_{i + 1}", lambda r, th=("black" if i % 2 == 0 else "white"): watchtower(r, th), "land")
for i in range(5):
    add(f"security_checkpoint_{i + 1}", lambda r, th=("black" if i % 2 else "white"): checkpoint(r, th), "land")
for i in range(7):
    add(f"research_bunker_{i + 1}", lambda r, n=(2 + i % 3), lh=(i % 3 == 2): bunker(r, "bunker", n, lh), "underground")
for i in range(6):
    add(f"containment_wing_{i + 1}", lambda r: containment_wing(r), "underground")
for i in range(8):
    add(f"abandoned_lab_{i + 1}", lambda r, th=("white" if i % 2 else "black"): research_outpost(r, th, abandoned=True), "land")
for i in range(5):
    add(f"aquatic_research_station_{i + 1}",
        lambda r, b=("tiger_shark" if i == 2 else None): aquatic_station(r, b), "ocean", 34, 12)
for i in range(5):
    add(f"supply_depot_{i + 1}", lambda r, th=("black" if i % 2 else "white"): supply_depot(r, th), "land")
for i in range(4):
    add(f"field_medical_station_{i + 1}", lambda r, th=("white" if i % 2 == 0 else "black"): medical_station(r, th), "land")
for i in range(2):
    add(f"comm_relay_{i + 1}", lambda r, th=("black" if i else "white"): comm_relay(r, th), "land")
add("helipad_1", lambda r: helipad(r, "black"), "land")
add("latex_quarantine_camp_1", lambda r: quarantine_camp(r), "land")
assert len(CATALOGUE) == 60, len(CATALOGUE)

add("thunder_science_tower", tsc_tower, "flat", 56, 22)
add("kaiju_paradise", kaiju_paradise, "kaiju", 60, 24)
add("laminax_underground", laminax_underground, "laminax", 60, 24)


# =====================================================================================  json writers

def wjson(rel, obj):
    p = os.path.join(DATA, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w") as f:
        json.dump(obj, f, indent=2)


def structure_json(name, place):
    base = {
        "type": "minecraft:jigsaw",
        "start_pool": f"{MOD}:{name}/start",
        "size": 1,
        "max_distance_from_center": 80,
        "use_expansion_hack": False,
        "spawn_overrides": {},
    }
    if place in ("land", "flat"):
        base.update(biomes=f"#{MOD}:has_structure/{'land' if place == 'land' else 'flat'}", step="surface_structures",
                    terrain_adaptation="beard_thin" if place == "land" else "beard_box",
                    start_height={"absolute": 0}, project_start_to_heightmap="WORLD_SURFACE_WG")
    elif place == "kaiju":
        base.update(biomes=f"#{MOD}:has_structure/tropical", step="surface_structures", terrain_adaptation="beard_box",
                    start_height={"absolute": -8}, project_start_to_heightmap="WORLD_SURFACE_WG")
    elif place == "ocean":
        base.update(biomes=f"#{MOD}:has_structure/ocean", step="surface_structures", terrain_adaptation="none",
                    start_height={"absolute": 0}, project_start_to_heightmap="OCEAN_FLOOR_WG")
    elif place == "underground":
        base.update(biomes=f"#{MOD}:has_structure/underground", step="underground_structures", terrain_adaptation="none",
                    start_height={"type": "minecraft:uniform", "min_inclusive": {"absolute": -40},
                                  "max_inclusive": {"absolute": 20}})
    elif place == "laminax":
        base.update(biomes=f"#{MOD}:has_structure/underground", step="underground_structures", terrain_adaptation="none",
                    start_height={"type": "minecraft:uniform", "min_inclusive": {"absolute": -50},
                                  "max_inclusive": {"absolute": -25}})
    return base


def main():
    os.makedirs(os.path.join(DATA, "structures"), exist_ok=True)
    stats = []
    for idx, (name, fn, place, spacing, separation) in enumerate(CATALOGUE):
        rng = random.Random(f"tsc-{name}")
        tpl = fn(rng)
        tpl.save(os.path.join(DATA, "structures", name + ".nbt"))
        stats.append((name, tpl.sx, tpl.sy, tpl.sz, len(tpl.blocks)))
        wjson(f"worldgen/structure/{name}.json", structure_json(name, place))
        wjson(f"worldgen/template_pool/{name}/start.json", {
            "fallback": "minecraft:empty",
            "elements": [{"weight": 1, "element": {
                "element_type": "minecraft:single_pool_element",
                "location": f"{MOD}:{name}",
                "projection": "rigid",
                "processors": "minecraft:empty"}}]})
        wjson(f"worldgen/structure_set/{name}.json", {
            "structures": [{"structure": f"{MOD}:{name}", "weight": 1}],
            "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": separation,
                          "salt": 74110000 + idx * 7919}})

    land = ["#minecraft:is_forest", "#minecraft:is_taiga", "#minecraft:is_savanna", "#minecraft:is_jungle",
            "#minecraft:is_badlands", "minecraft:plains", "minecraft:sunflower_plains", "minecraft:desert",
            "minecraft:snowy_plains", "minecraft:meadow", "minecraft:cherry_grove", "minecraft:swamp"]
    flat = ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:desert", "minecraft:savanna",
            "minecraft:snowy_plains", "minecraft:forest", "minecraft:birch_forest", "minecraft:meadow"]
    tropical = ["#minecraft:is_jungle", "#minecraft:is_beach", "minecraft:savanna", "minecraft:desert",
                "minecraft:plains"]
    for tag, values in (("land", land), ("flat", flat), ("tropical", tropical), ("ocean", ["#minecraft:is_ocean"]),
                        ("underground", ["#minecraft:is_overworld"])):
        wjson(f"tags/worldgen/biome/has_structure/{tag}.json", {"replace": False, "values": values})

    with open(os.path.join(ROOT, "structure_list.txt"), "w") as f:
        for s in stats:
            f.write("%-32s %3dx%3dx%3d  %6d blocks\n" % s)
    print(len(stats), "structures written")


if __name__ == "__main__":
    main()
