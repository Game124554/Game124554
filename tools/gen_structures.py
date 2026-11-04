"""Generates the 99 SCP Foundation world structures (NBT templates + worldgen JSON + loot tables).

Run from the project root:  python3 tools/gen_structures.py
Every structure is a single-piece jigsaw structure so it can be found with
/locate structure scpfoundation:<name>   or   /locate structure #scpfoundation:all
"""
import json
import os
import random
import zlib

from nbtio import Byte, Compound, Double, Int, List, Str, write_gz, to_tag, INT, DOUBLE, COMPOUND, STRING

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODID = "scpfoundation"
DATA = os.path.join(ROOT, "src", "main", "resources", "data", MODID)
DATA_VERSION = 3465  # Minecraft 1.20.1
M = MODID


def p(*parts):
    path = os.path.join(*parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    return path


def write_json(path, obj):
    with open(p(path), "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)


# --------------------------------------------------------------------------------------
# Palette shortcuts
# --------------------------------------------------------------------------------------
AIR = "minecraft:air"
CONC = f"{M}:reinforced_concrete"
WALL = f"{M}:facility_wall"
FLOOR = f"{M}:facility_floor"
HAZ = f"{M}:hazard_block"
GLASS = f"{M}:containment_glass"
EMBLEM = f"{M}:foundation_emblem"
LAMP = "minecraft:sea_lantern"


def reader(level, facing): return f"{M}:keycard_reader_{level}[facing={facing}]"


OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}


class T:
    """A structure template under construction. y=0 is the ground/floor layer."""

    def __init__(self, sx, sy, sz):
        self.size = (sx, sy, sz)
        self.blocks = {}
        self.entities = []

    def inb(self, x, y, z):
        return 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]

    def set(self, x, y, z, state, nbt=None):
        if self.inb(x, y, z):
            self.blocks[(x, y, z)] = (state, nbt)

    def get(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return b[0] if b else None

    def fill(self, x1, y1, z1, x2, y2, z2, state):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, state)

    def box(self, x1, y1, z1, x2, y2, z2, wall, floor=None, roof=None, inside=AIR):
        """Hollow room: floor at y1, roof at y2, walls between."""
        self.fill(x1, y1, z1, x2, y2, z2, wall)
        self.fill(x1 + 1, y1 + 1, z1 + 1, x2 - 1, y2 - 1, z2 - 1, inside)
        if floor:
            self.fill(x1, y1, z1, x2, y1, z2, floor)
        if roof:
            self.fill(x1, y2, z1, x2, y2, z2, roof)

    def chest(self, x, y, z, loot, facing="north", block="chest"):
        if block == "barrel":
            self.set(x, y, z, f"minecraft:barrel[facing=up,open=false]", {"id": "minecraft:barrel", "LootTable": f"{M}:{loot}"})
        else:
            self.set(x, y, z, f"minecraft:chest[facing={facing},type=single,waterlogged=false]", {"id": "minecraft:chest", "LootTable": f"{M}:{loot}"})

    def entity(self, x, y, z, eid, name=None, extra=None):
        nbt = {"id": eid, "PersistenceRequired": Byte(1)}
        if name:
            nbt["CustomName"] = json.dumps({"text": name})
        if extra:
            nbt.update(extra)
        self.entities.append(((x + 0.5, float(y), z + 0.5), (x, y, z), nbt))

    def sign(self, x, y, z, facing, lines, wood="dark_oak", color="white"):
        msgs = [json.dumps({"text": l}) for l in (lines + ["", "", "", ""])[:4]]
        empty = [json.dumps({"text": ""})] * 4
        self.set(x, y, z, f"minecraft:{wood}_wall_sign[facing={facing},waterlogged=false]", {
            "id": "minecraft:sign",
            "front_text": {"messages": List(STRING, [Str(m) for m in msgs]), "color": color, "has_glowing_text": Byte(1)},
            "back_text": {"messages": List(STRING, [Str(m) for m in empty]), "color": "black", "has_glowing_text": Byte(0)},
            "is_waxed": Byte(1)})

    def iron_door(self, x, y, z, facing, hinge="left"):
        self.set(x, y, z, f"minecraft:iron_door[facing={facing},half=lower,hinge={hinge},open=false,powered=false]")
        self.set(x, y + 1, z, f"minecraft:iron_door[facing={facing},half=upper,hinge={hinge},open=false,powered=false]")

    def save(self, name):
        palette, index = [], {}
        blocks = []
        for (x, y, z), (state, nbt) in sorted(self.blocks.items()):
            if state not in index:
                index[state] = len(palette)
                if "[" in state:
                    nm, props = state[:-1].split("[", 1)
                    pd = dict(kv.split("=") for kv in props.split(","))
                    palette.append(Compound({"Name": Str(nm), "Properties": Compound({k: Str(v) for k, v in pd.items()})}))
                else:
                    palette.append(Compound({"Name": Str(state)}))
            entry = {"pos": List(INT, [Int(x), Int(y), Int(z)]), "state": Int(index[state])}
            if nbt is not None:
                entry["nbt"] = to_tag(nbt)
            blocks.append(Compound(entry))
        ents = []
        for pos, bpos, nbt in self.entities:
            ents.append(Compound({"pos": List(DOUBLE, [Double(v) for v in pos]),
                                  "blockPos": List(INT, [Int(v) for v in bpos]),
                                  "nbt": to_tag(nbt)}))
        root = Compound({
            "DataVersion": Int(DATA_VERSION),
            "size": List(INT, [Int(v) for v in self.size]),
            "palette": List(COMPOUND, palette),
            "blocks": List(COMPOUND, blocks),
            "entities": List(COMPOUND, ents),
        })
        write_gz(p(DATA, "structures", f"{name}.nbt"), root)
        return len(self.blocks)


# --------------------------------------------------------------------------------------
# Furnishing helpers (interior bounds are inclusive; y=1 is the first air layer)
# --------------------------------------------------------------------------------------
def lights(t, x0, x1, z0, z1, y, step=4):
    for x in range(x0 + 1, x1, step):
        for z in range(z0 + 1, z1, step):
            t.set(x, y, z, LAMP)


def furnish(t, kind, x0, x1, z0, z1, door, rng, h, cell_mob=None, cell_level=3):
    """Furnish a room interior.  door = (x, z) of the inside cell in front of the doorway."""
    keep = {door, (door[0], door[1] + 1), (door[0], door[1] - 1)}

    def ok(x, z): return (x, z) not in keep and x0 <= x <= x1 and z0 <= z <= z1

    far_z = z0 if abs(door[1] - z0) > abs(door[1] - z1) else z1
    fz_face = "south" if far_z == z0 else "north"
    if kind == "office":
        for x in range(x0, x1 + 1, 3):
            if ok(x, far_z):
                t.set(x, 1, far_z, "minecraft:bookshelf")
        for x in range(x0 + 1, x1, 3):
            mz = (z0 + z1) // 2
            if ok(x, mz):
                t.set(x, 1, mz, "minecraft:spruce_slab[type=top,waterlogged=false]")
                if ok(x, mz + (1 if fz_face == "north" else -1)):
                    t.set(x, 1, mz + (1 if fz_face == "north" else -1), f"minecraft:spruce_stairs[facing={fz_face},half=bottom,shape=straight,waterlogged=false]")
        if ok(x1, far_z):
            t.chest(x1, 1, far_z, "chests/research", fz_face)
        t.set(x0, 1, (z0 + z1) // 2, "minecraft:lectern[facing=east,has_book=false,powered=false]") if ok(x0, (z0 + z1) // 2) else None
    elif kind == "lab":
        for x in range(x0, x1 + 1):
            if ok(x, far_z):
                t.set(x, 1, far_z, rng.choice(["minecraft:smooth_stone", "minecraft:white_concrete"]))
                if rng.random() < 0.35:
                    t.set(x, 2, far_z, "minecraft:brewing_stand[has_bottle_0=false,has_bottle_1=false,has_bottle_2=false]")
        if ok(x0, (z0 + z1) // 2):
            t.set(x0, 1, (z0 + z1) // 2, "minecraft:cauldron")
        if ok(x1, (z0 + z1) // 2):
            t.chest(x1, 1, (z0 + z1) // 2, "chests/research", "west")
        if ok((x0 + x1) // 2, (z0 + z1) // 2):
            t.set((x0 + x1) // 2, 1, (z0 + z1) // 2, "minecraft:crafting_table")
    elif kind == "archive":
        for x in range(x0, x1 + 1, 2):
            for z in range(z0, z1 + 1):
                if ok(x, z) and x not in (door[0],) and z != (z0 + z1) // 2 + (0 if (z1 - z0) < 3 else 0):
                    if (z - z0) % 2 == 0:
                        t.set(x, 1, z, "minecraft:bookshelf")
                        t.set(x, 2, z, "minecraft:bookshelf" if rng.random() < 0.7 else AIR)
                    elif rng.random() < 0.5:
                        t.chest(x, 1, z, "chests/archive", block="barrel")
    elif kind == "armory":
        for x in range(x0, x1 + 1, 2):
            if ok(x, far_z):
                t.chest(x, 1, far_z, "chests/armory", fz_face)
        if ok(x0, (z0 + z1) // 2):
            t.set(x0, 1, (z0 + z1) // 2, "minecraft:smithing_table")
        if ok(x1, (z0 + z1) // 2):
            t.set(x1, 1, (z0 + z1) // 2, "minecraft:anvil[facing=north]")
        for x in range(x0 + 1, x1, 2):
            if ok(x, far_z):
                t.set(x, 2, far_z, "minecraft:iron_bars[east=true,north=false,south=false,waterlogged=false,west=true]")
    elif kind == "medical":
        bed_dir = "north" if far_z == z0 else "south"
        for x in range(x0, x1 + 1, 2):
            head = far_z
            foot = far_z + (1 if bed_dir == "north" else -1)
            if ok(x, head) and ok(x, foot):
                t.set(x, 1, head, f"minecraft:white_bed[facing={bed_dir},occupied=false,part=head]")
                t.set(x, 1, foot, f"minecraft:white_bed[facing={bed_dir},occupied=false,part=foot]")
        if ok(x1, door[1]) and x1 != door[0]:
            t.chest(x1, 1, door[1], "chests/medical", "west")
        if ok(x0, door[1]) and x0 != door[0]:
            t.set(x0, 1, door[1], "minecraft:brewing_stand[has_bottle_0=false,has_bottle_1=false,has_bottle_2=false]")
    elif kind == "barracks":
        bed_dir = "north" if far_z == z0 else "south"
        for x in range(x0, x1 + 1, 2):
            head, foot = far_z, far_z + (1 if bed_dir == "north" else -1)
            if ok(x, head) and ok(x, foot):
                t.set(x, 1, head, f"minecraft:orange_bed[facing={bed_dir},occupied=false,part=head]")
                t.set(x, 1, foot, f"minecraft:orange_bed[facing={bed_dir},occupied=false,part=foot]")
        if ok(x1, door[1]) and x1 != door[0]:
            t.chest(x1, 1, door[1], "chests/facility_common", "west")
    elif kind == "storage":
        for x in range(x0, x1 + 1):
            for z in (z0, z1):
                if ok(x, z) and rng.random() < 0.55:
                    if rng.random() < 0.3:
                        t.chest(x, 1, z, "chests/facility_common", block="barrel")
                    else:
                        t.set(x, 1, z, rng.choice(["minecraft:barrel[facing=up,open=false]", "minecraft:spruce_planks", "minecraft:hay_block[axis=y]"]))
                    if rng.random() < 0.4:
                        t.set(x, 2, z, "minecraft:barrel[facing=up,open=false]")
    elif kind == "server":
        for x in range(x0, x1 + 1, 2):
            for z in range(z0, z1 + 1):
                if ok(x, z) and z != door[1]:
                    t.set(x, 1, z, "minecraft:black_concrete")
                    t.set(x, 2, z, rng.choice(["minecraft:observer[facing=north,powered=false]", "minecraft:black_concrete", "minecraft:target[power=0]"]))
        if ok(x1, door[1]) and x1 != door[0]:
            t.chest(x1, 1, door[1], "chests/research", "west")
    elif kind == "breakroom":
        mx, mz = (x0 + x1) // 2, (z0 + z1) // 2
        if ok(mx, mz):
            t.set(mx, 1, mz, "minecraft:oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]")
            t.set(mx, 2, mz, "minecraft:cake[bites=0]") if False else t.set(mx, 2, mz, "minecraft:oak_pressure_plate[powered=false]")
        for x in range(x0, x1 + 1):
            if ok(x, far_z):
                t.set(x, 1, far_z, rng.choice(["minecraft:smoker[facing=" + fz_face + ",lit=false]", "minecraft:white_concrete", "minecraft:crafting_table"]))
        if ok(x1, mz):
            t.chest(x1, 1, mz, "chests/facility_common", "west")
    elif kind == "security":
        for x in range(x0, x1 + 1):
            if ok(x, far_z):
                t.set(x, 1, far_z, "minecraft:gray_concrete")
                t.set(x, 2, far_z, "minecraft:tinted_glass" if x % 2 else "minecraft:redstone_lamp[lit=false]")
        if ok(x0, (z0 + z1) // 2):
            t.chest(x0, 1, (z0 + z1) // 2, "chests/armory", "east")
    elif kind == "generator":
        for x in range(x0, x1 + 1, 2):
            for z in range(z0, z1 + 1, 2):
                if ok(x, z):
                    t.set(x, 0, z, HAZ)
                    t.set(x, 1, z, rng.choice(["minecraft:blast_furnace[facing=north,lit=false]", "minecraft:furnace[facing=north,lit=false]", "minecraft:redstone_block"]))
                    if rng.random() < 0.5:
                        t.set(x, 2, z, "minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]")
    elif kind == "cell":
        # containment cell: hazard floor, the SCP placed inside
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                t.set(x, 0, z, HAZ if (x + z) % 2 == 0 else CONC)
        if cell_mob:
            t.entity((x0 + x1) // 2, 1, far_z if abs(far_z - door[1]) > 1 else (z0 + z1) // 2, f"{M}:{cell_mob}")
    elif kind.startswith("anomaly:"):
        block = kind.split(":")[1]
        mx = (x0 + x1) // 2
        t.set(mx, 1, far_z, f"{M}:{block}[facing={fz_face}]")
        for x in range(x0, x1 + 1):
            t.set(x, 0, far_z, HAZ)
    elif kind.startswith("locker:"):
        item = kind.split(":")[1]
        mx, mz = (x0 + x1) // 2, far_z
        t.set(mx, 1, mz, "minecraft:polished_andesite")
        t.chest(mx, 2, mz, f"chests/vault/{item}", fz_face)


# --------------------------------------------------------------------------------------
# Generic facility building (corridor plan)
# --------------------------------------------------------------------------------------
def facility(name, seed, rooms, W=None, D=17, H=4, entrance=1, sign=None, palette=None,
             cell_mobs=None, door_levels=None, second_exit=False, margin=2, base_y=0, t=None, sy=None):
    rng = random.Random(seed)
    W = W or rng.randint(22, 34)
    wall, floor, roof = palette or (WALL, FLOOR, CONC)
    if t is None:
        t = T(W + margin * 2, (sy or H + 3), D + margin * 2)
    ox, oz, y0 = margin, margin, base_y
    x1, z1 = ox + W - 1, oz + D - 1
    # shell
    t.box(ox, y0, oz, x1, y0 + H + 1, z1, wall, floor, roof)
    # clear the area above the roof line and in front of doors (cuts into hills)
    cz = oz + D // 2  # corridor centre
    c0, c1 = cz - 1, cz + 1
    # interior walls separating corridor
    t.fill(ox + 1, y0 + 1, c0 - 1, x1 - 1, y0 + H, c0 - 1, wall)
    t.fill(ox + 1, y0 + 1, c1 + 1, x1 - 1, y0 + H, c1 + 1, wall)
    lights(t, ox, x1, c0 - 1, c1 + 1, y0 + H + 1, 4)
    # rooms
    xs = ox + 1
    room_idx = 0
    mobs = list(cell_mobs or [])
    while xs < x1 - 3:
        rw = rng.randint(5, 8)
        xe = min(xs + rw - 1, x1 - 1)
        if x1 - 1 - xe < 4:
            xe = x1 - 1
        for side in ("north", "south"):
            if side == "north":
                rz0, rz1, wall_z, inside_z = oz + 1, c0 - 2, c0 - 1, c0 - 2
            else:
                rz0, rz1, wall_z, inside_z = c1 + 2, z1 - 1, c1 + 1, c1 + 2
            kind = rooms[room_idx % len(rooms)]
            room_idx += 1
            dx = (xs + xe) // 2
            # partition wall at room end
            if xe < x1 - 1:
                t.fill(xe + 1, y0 + 1, rz0, xe + 1, y0 + H, rz1, wall)
            # door
            level = None
            if kind == "cell":
                level = cell_level_for(rng, door_levels)
            elif door_levels and kind in door_levels:
                level = door_levels[kind]
            door_face = "south" if side == "north" else "north"  # direction pointing into the corridor
            if level:
                t.iron_door(dx, y0 + 1, wall_z, door_face)
                t.set(dx + 1, y0 + 2, wall_z, reader(min(level, 5), door_face))
                t.set(dx - 1, y0 + 2, wall_z + (-1 if side == "north" else 1),
                      f"minecraft:stone_button[face=wall,facing={OPP[door_face]},powered=false]") if kind != "cell" else None
            else:
                t.set(dx, y0 + 1, wall_z, AIR)
                t.set(dx, y0 + 2, wall_z, AIR)
            if kind == "cell":
                # observation window
                for wx in range(xs, xe + 1):
                    if abs(wx - dx) >= 2:
                        t.set(wx, y0 + 2, wall_z, GLASS)
            lights(t, xs - 1, xe + 1, rz0 - 1, rz1 + 1, y0 + H + 1, 3)
            mob = mobs.pop(0) if (kind == "cell" and mobs) else None
            furnish_offset(t, kind, xs, xe, rz0, rz1, (dx, inside_z), rng, H, y0, mob)
        xs = xe + 2
    # entrance at west wall
    if entrance == 0:
        for z in (c0, cz, c1):
            t.fill(ox, y0 + 1, z, ox, y0 + 3, z, AIR)
    else:
        t.iron_door(ox, y0 + 1, cz, "west")
        t.set(ox, y0 + 2, cz + 1, reader(entrance, "west"))
        t.set(ox + 1, y0 + 2, cz - 1, "minecraft:stone_button[face=wall,facing=east,powered=false]")
    # path & clearing outside entrance
    for x in range(0, ox):
        for z in (c0, cz, c1):
            t.set(x, y0, z, "minecraft:gravel" if base_y == 0 else CONC)
            t.fill(x, y0 + 1, z, x, y0 + 3, z, AIR)
    if sign and ox >= 1:
        t.sign(ox - 1, y0 + 3, cz, "west", sign)
        t.set(ox, y0 + 3, cz, EMBLEM)
    if second_exit:
        t.set(x1, y0 + 1, cz, AIR)
        t.set(x1, y0 + 2, cz, AIR)
    return t


def cell_level_for(rng, door_levels):
    if door_levels and "cell" in door_levels:
        return door_levels["cell"]
    return rng.choice([3, 3, 4])


def furnish_offset(t, kind, x0, x1, z0, z1, door, rng, h, y0, mob):
    if y0 == 0:
        furnish(t, kind, x0, x1, z0, z1, door, rng, h, mob)
        return
    # furnish into a temp template then shift up by y0
    tmp = T(t.size[0], h + 3, t.size[2])
    for (x, y, z), v in t.blocks.items():
        if y0 <= y < y0 + h + 3:
            tmp.blocks[(x, y - y0, z)] = v
    snap = dict(tmp.blocks)
    furnish(tmp, kind, x0, x1, z0, z1, door, rng, h, mob)
    for k, v in tmp.blocks.items():
        if snap.get(k) is not v:
            t.blocks[(k[0], k[1] + y0, k[2])] = v
    for pos, bpos, nbt in tmp.entities:
        t.entities.append(((pos[0], pos[1] + y0, pos[2]), (bpos[0], bpos[1] + y0, bpos[2]), nbt))


# --------------------------------------------------------------------------------------
# Specific structure builders
# --------------------------------------------------------------------------------------
def containment_cell(name, mob, size=(9, 5, 9), level=3, extra=None, glass=True, dark=False, seed=0):
    """A standalone containment unit: observation corridor + sealed cell."""
    cw, ch, cd = size
    W, D = cw + 2, cd + 6
    t = T(W + 4, ch + 3, D + 4)
    ox, oz = 2, 2
    x1, z1 = ox + W - 1, oz + D - 1
    t.box(ox, 0, oz, x1, ch + 1, z1, CONC, FLOOR, CONC)
    # cell wall separating observation corridor (south part) from cell (north part)
    wz = oz + cd + 1
    t.fill(ox + 1, 1, wz, x1 - 1, ch, wz, CONC)
    for x in range(ox + 1, x1):
        for z in range(oz + 1, wz):
            t.set(x, 0, z, HAZ if (x + z) % 2 == 0 else CONC)
    dx = ox + W // 2
    t.iron_door(dx, 1, wz, "south")
    t.set(dx + 1, 2, wz, reader(level, "south"))
    if glass:
        for x in range(ox + 1, x1):
            if abs(x - dx) >= 2:
                t.set(x, 2, wz, GLASS)
                if ch >= 4:
                    t.set(x, 3, wz, GLASS)
    if not dark:
        lights(t, ox, x1, oz, wz, ch + 1, 3)
    lights(t, ox, x1, wz, z1, ch + 1, 3)
    # entrance from the south
    t.iron_door(dx, 1, z1, "south")
    t.set(dx - 1, 2, z1, reader(1, "south"))
    t.set(dx + 1, 2, z1 - 1, "minecraft:stone_button[face=wall,facing=north,powered=false]")
    t.sign(dx, 3, z1 + 1, "south", [name.upper().replace("_", "-").replace("CELL-", "").replace("SCP-0", "SCP-0"), "CONTAINMENT", "LEVEL %d" % level])
    for z in range(z1 + 1, t.size[2]):
        t.fill(dx - 1, 1, z, dx + 1, 3, z, AIR)
        t.fill(dx - 1, 0, z, dx + 1, 0, z, "minecraft:gravel")
    t.chest(x1 - 1, 1, z1 - 1, "chests/research", "north")
    t.set(ox + 1, 1, z1 - 1, "minecraft:lectern[facing=north,has_book=false,powered=false]")
    if mob:
        mobs = mob if isinstance(mob, list) else [mob]
        for i, m in enumerate(mobs):
            t.entity(dx + (i - len(mobs) // 2) * 2, 1, oz + cd // 2 + 1, f"{M}:{m}")
    if extra:
        extra(t, ox, oz, x1, wz)
    return t


def scp_room(name, block, level, decorate=None, size=(11, 5, 11), seed=0):
    W, H, D = size
    t = T(W + 4, H + 3, D + 4)
    ox, oz = 2, 2
    x1, z1 = ox + W - 1, oz + D - 1
    t.box(ox, 0, oz, x1, H + 1, z1, WALL, FLOOR, CONC)
    lights(t, ox, x1, oz, z1, H + 1, 3)
    cx, cz = ox + W // 2, oz + D // 2
    if block:
        t.set(cx, 1, oz + 2, block)
    dx = cx
    t.iron_door(dx, 1, z1, "south")
    t.set(dx - 1, 2, z1, reader(level, "south"))
    t.set(dx + 1, 2, z1 - 1, "minecraft:stone_button[face=wall,facing=north,powered=false]")
    t.sign(dx, 3, z1 + 1, "south", [name.upper().replace("_", " "), "AUTHORIZED", "PERSONNEL ONLY"])
    for z in range(z1 + 1, t.size[2]):
        t.fill(dx - 1, 1, z, dx + 1, 3, z, AIR)
        t.fill(dx - 1, 0, z, dx + 1, 0, z, "minecraft:gravel")
    if decorate:
        decorate(t, ox, oz, x1, z1, cx, cz)
    return t


def vault(item, level, seed=0):
    rng = random.Random(seed)
    W = rng.choice([7, 9])
    t = T(W + 4, 7, W + 4)
    ox = oz = 2
    x1 = z1 = ox + W - 1
    t.box(ox, 0, oz, x1, 5, z1, CONC, FLOOR, CONC)
    cx = ox + W // 2
    t.set(cx, 5, cx, LAMP)
    t.set(cx, 1, oz + 2, "minecraft:polished_blackstone")
    t.chest(cx, 2, oz + 2, f"chests/vault/{item}", "south")
    for x in (ox + 1, x1 - 1):
        for z in (oz + 1, z1 - 1):
            t.fill(x, 1, z, x, 4, z, "minecraft:iron_bars[east=false,north=true,south=true,waterlogged=false,west=false]")
    t.fill(ox + 1, 0, oz + 1, x1 - 1, 0, z1 - 1, HAZ)
    t.fill(cx - 1, 0, oz + 3, cx + 1, 0, z1 - 1, FLOOR)
    t.iron_door(cx, 1, z1, "south")
    t.set(cx - 1, 2, z1, reader(level, "south"))
    t.set(cx + 1, 2, z1 - 1, "minecraft:stone_button[face=wall,facing=north,powered=false]")
    t.sign(cx, 3, z1 + 1, "south", ["SECURE LOCKER", item.upper().replace("_", "-"), "SAFE CLASS"])
    for z in range(z1 + 1, t.size[2]):
        t.fill(cx - 1, 1, z, cx + 1, 3, z, AIR)
    return t


def stairwell_087():
    depth = 44
    t = T(13, depth + 8, 13)
    top = depth
    # entrance hut on the surface
    t.box(2, top, 2, 10, top + 5, 10, CONC, FLOOR, CONC)
    t.set(6, top + 5, 6, LAMP)
    t.iron_door(6, top + 1, 10, "south")
    t.set(5, top + 2, 10, reader(2, "south"))
    t.set(7, top + 2, 9, "minecraft:stone_button[face=wall,facing=north,powered=false]")
    t.sign(6, top + 3, 11, "south", ["SCP-087", "STAIRWELL", "LEVEL 2"])
    # shaft
    t.fill(2, 0, 2, 10, top - 1, 10, CONC)
    t.fill(3, 1, 3, 9, top, 9, AIR)
    t.fill(3, top, 3, 9, top, 9, AIR)
    t.fill(5, top, 5, 7, top, 7, "minecraft:iron_bars[east=true,north=true,south=true,waterlogged=false,west=true]")
    # spiral around the 7x7 inner ring, one block down per step
    ring = []
    for x in range(3, 9): ring.append((x, 3, "east"))
    for z in range(3, 9): ring.append((9, z, "south"))
    for x in range(9, 3, -1): ring.append((x, 9, "west"))
    for z in range(9, 3, -1): ring.append((3, z, "north"))
    y = top - 1
    i = 0
    while y >= 1:
        x, z, d = ring[i % len(ring)]
        corner = (x, z) in [(3, 3), (9, 3), (9, 9), (3, 9)]
        state = "minecraft:stone_bricks" if corner else f"minecraft:stone_brick_stairs[facing={OPP[d]},half=bottom,shape=straight,waterlogged=false]"
        t.set(x, y, z, state)
        if i % 13 == 0:
            t.set(x, y + 3, z, "minecraft:soul_lantern[hanging=true,waterlogged=false]") if False else None
        i += 1
        y -= 1
    # landing at the bottom with something waiting in the dark
    t.fill(3, 0, 3, 9, 0, 9, "minecraft:cracked_stone_bricks")
    t.fill(4, 1, 4, 8, 1, 8, AIR)
    t.chest(8, 1, 8, "chests/o5_vault", "north")
    t.set(4, 1, 8, "minecraft:cobweb")
    t.entity(6, 1, 6, f"{M}:scp_939")
    return t, depth


def bunker(name, seed, depth, rooms, door_levels=None, sign=None, cell_mobs=None, loot_vault=None):
    rng = random.Random(seed)
    W = rng.randint(20, 28)
    D = 17
    H = 4
    t = T(W + 4, depth + 8, D + 4)
    facility(name, seed, rooms, W=W, D=D, H=H, entrance=0, margin=2, t=t, door_levels=door_levels, cell_mobs=cell_mobs)
    # seal the west entrance underground (bunker is accessed via shaft)
    cz = 2 + D // 2
    for z in (cz - 1, cz, cz + 1):
        t.fill(2, 1, z, 2, 3, z, CONC)
        t.fill(0, 0, z, 1, 3, z, CONC)
    # shaft from corridor up to a surface hut
    sx, sz = 4, cz
    hut_y = depth
    t.fill(sx - 1, H + 1, sz - 1, sx + 1, hut_y - 1, sz + 1, CONC)
    t.fill(sx, 1, sz, sx, hut_y + 1, sz, AIR)
    for y in range(1, hut_y + 1):
        t.set(sx, y, sz, "minecraft:ladder[facing=north,waterlogged=false]")
    t.set(sx, y, sz + 1, CONC) if False else None
    for y in range(H + 1, hut_y):
        t.set(sx, y, sz + 1, CONC)
    # hut
    t.box(sx - 2, hut_y, sz - 2, sx + 3, hut_y + 4, sz + 2, CONC, FLOOR, CONC)
    t.set(sx, hut_y, sz, "minecraft:ladder[facing=north,waterlogged=false]")
    t.set(sx, hut_y, sz + 1, CONC)
    t.set(sx + 2, hut_y + 4, sz, LAMP)
    t.iron_door(sx + 3, hut_y + 1, sz, "east")
    t.set(sx + 3, hut_y + 2, sz + 1, reader(rng.choice([1, 2]), "east"))
    t.set(sx + 2, hut_y + 2, sz - 1, "minecraft:stone_button[face=wall,facing=west,powered=false]")
    if sign:
        t.sign(sx + 4, hut_y + 3, sz, "east", sign)
    if loot_vault:
        t.chest(sx + 2, hut_y + 1, sz + 1, loot_vault, "west")
    return t


def camp_ci(seed):
    rng = random.Random(seed)
    t = T(25, 8, 25)
    t.fill(0, 0, 0, 24, 0, 24, "minecraft:coarse_dirt")
    for _ in range(80):
        t.set(rng.randrange(25), 0, rng.randrange(25), rng.choice(["minecraft:grass_block[snowy=false]", "minecraft:gravel", "minecraft:dirt_path"]))
    t.fill(0, 1, 0, 24, 5, 24, AIR)
    tents = [(3, 3), (14, 3), (3, 14), (15, 15)][: rng.randint(3, 4)]
    for (tx, tz) in tents:
        wool = rng.choice(["minecraft:green_wool", "minecraft:black_wool", "minecraft:brown_wool"])
        for i in range(3):
            t.fill(tx + i, 1 + i, tz, tx + i, 1 + i, tz + 5, wool)
            t.fill(tx + 6 - i, 1 + i, tz, tx + 6 - i, 1 + i, tz + 5, wool)
        t.fill(tx + 3, 4, tz, tx + 3, 4, tz + 5, wool)
        t.chest(tx + 3, 1, tz + 4, "chests/insurgency", "north")
        t.set(tx + 2, 1, tz + 4, "minecraft:black_bed[facing=north,occupied=false,part=foot]")
        t.set(tx + 2, 1, tz + 3, "minecraft:black_bed[facing=north,occupied=false,part=head]")
    t.set(11, 1, 11, "minecraft:campfire[facing=north,lit=true,signal_fire=false,waterlogged=false]")
    for (x, z) in [(10, 11), (12, 11), (11, 10), (11, 12)]:
        t.set(x, 1, z, "minecraft:spruce_slab[type=bottom,waterlogged=false]")
    for i in range(rng.randint(2, 4)):
        t.set(20 + (i % 2), 1, 9 + i, "minecraft:tnt[unstable=false]")
    t.chest(21, 1, 6, "chests/insurgency", block="barrel")
    t.chest(6, 1, 21, "chests/insurgency", block="barrel")
    t.set(12, 1, 2, "minecraft:white_banner[rotation=0]") if False else None
    for i in range(rng.randint(2, 4)):
        t.entity(9 + i * 2, 1, 13, "minecraft:pillager", "Chaos Insurgent")
    return t


def goc_outpost(seed):
    rng = random.Random(seed)
    t = T(21, 9, 21)
    t.fill(2, 0, 2, 18, 0, 18, "minecraft:packed_mud")
    t.fill(2, 1, 2, 18, 6, 18, AIR)
    # sandbag ring
    for x in range(2, 19):
        for z in (2, 18):
            if x not in (9, 10, 11):
                t.fill(x, 1, z, x, 2, z, "minecraft:mud_bricks")
    for z in range(2, 19):
        for x in (2, 18):
            t.fill(x, 1, z, x, 2, z, "minecraft:mud_bricks")
    # bunker
    t.box(6, 0, 6, 14, 5, 13, "minecraft:deepslate_bricks", "minecraft:polished_deepslate", "minecraft:deepslate_tiles")
    t.set(10, 1, 13, AIR); t.set(10, 2, 13, AIR)
    t.set(10, 5, 9, LAMP)
    t.chest(7, 1, 7, "chests/goc", "south")
    t.chest(13, 1, 7, "chests/goc", "south")
    t.set(10, 1, 7, "minecraft:crafting_table")
    t.sign(11, 3, 14, "south", ["GLOBAL OCCULT", "COALITION", "FORWARD POST"], wood="spruce")
    t.fill(16, 1, 15, 16, 7, 15, "minecraft:spruce_log[axis=y]")
    t.set(16, 8, 15, "minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]")
    for i in range(rng.randint(2, 3)):
        t.entity(8 + i * 2, 1, 16, "minecraft:vindicator", "GOC Operative")
    return t


def serpents_hand(seed):
    rng = random.Random(seed)
    t = T(17, 10, 15)
    t.fill(1, 0, 1, 15, 0, 13, "minecraft:spruce_planks")
    t.fill(1, 1, 1, 15, 7, 13, AIR)
    t.box(2, 0, 2, 14, 5, 12, "minecraft:spruce_log[axis=y]", "minecraft:spruce_planks", "minecraft:dark_oak_planks")
    t.fill(3, 1, 2, 13, 4, 2, "minecraft:spruce_planks")
    t.fill(3, 1, 12, 13, 4, 12, "minecraft:spruce_planks")
    t.fill(2, 1, 3, 2, 4, 11, "minecraft:spruce_planks")
    t.fill(14, 1, 3, 14, 4, 11, "minecraft:spruce_planks")
    for x in range(3, 14):
        if x != 8:
            t.fill(x, 1, 3, x, 3, 3, "minecraft:bookshelf")
    t.set(8, 1, 12, AIR); t.set(8, 2, 12, AIR)
    t.set(8, 1, 6, "minecraft:enchanting_table")
    t.set(4, 1, 9, "minecraft:lectern[facing=east,has_book=false,powered=false]")
    t.chest(12, 1, 10, "chests/serpents_hand", "west")
    t.chest(12, 1, 5, "chests/serpents_hand", "west")
    t.set(8, 4, 8, "minecraft:lantern[hanging=true,waterlogged=false]")
    t.set(5, 4, 6, "minecraft:lantern[hanging=true,waterlogged=false]")
    t.set(11, 4, 6, "minecraft:lantern[hanging=true,waterlogged=false]")
    t.sign(9, 3, 13, "south", ["THE LIBRARY", "IS OPEN", "TO ALL"], wood="spruce", color="lime")
    for x in range(2, 15):
        t.set(x, 6, 7, "minecraft:dark_oak_planks")
    return t


def checkpoint(seed):
    rng = random.Random(seed)
    L = rng.randint(17, 25)
    t = T(L, 7, 11)
    for x in range(L):
        t.fill(x, 0, 0, x, 0, 10, "minecraft:gray_concrete" if 3 <= 4 + 0 else CONC)
        t.fill(x, 1, 0, x, 5, 10, AIR)
    for x in range(L):
        if not (L // 2 - 2 <= x <= L // 2 + 2):
            t.fill(x, 1, 5, x, 3, 5, CONC)
            t.set(x, 4, 5, "minecraft:iron_bars[east=true,north=false,south=false,waterlogged=false,west=true]")
    for x in range(L // 2 - 2, L // 2 + 3):
        t.set(x, 0, 5, HAZ)
    # gate bar
    t.fill(L // 2 - 2, 3, 5, L // 2 + 2, 3, 5, "minecraft:polished_andesite_wall[east=low,north=none,south=none,up=false,waterlogged=false,west=low]")
    # guard house
    gx = 1
    t.box(gx, 0, 6, gx + 5, 4, 10, WALL, FLOOR, CONC)
    t.set(gx + 3, 1, 6, AIR); t.set(gx + 3, 2, 6, AIR)
    t.set(gx + 1, 2, 10, GLASS); t.set(gx + 4, 2, 10, GLASS)
    t.set(gx + 2, 4, 8, LAMP)
    t.chest(gx + 1, 1, 9, "chests/armory", "east")
    t.set(gx + 4, 1, 9, "minecraft:lectern[facing=west,has_book=false,powered=false]")
    t.sign(L // 2, 4, 4, "north", ["FOUNDATION", "CHECKPOINT", "SHOW YOUR", "CLEARANCE"])
    t.entity(L // 2, 1, 7, "minecraft:iron_golem", "Foundation Security", {"PlayerCreated": Byte(1)})
    return t


def gate(seed, letter):
    rng = random.Random(seed)
    t = T(35, 12, 21)
    t.fill(0, 0, 0, 34, 0, 20, "minecraft:gray_concrete")
    t.fill(0, 1, 0, 34, 10, 20, AIR)
    # big wall with gate
    t.fill(0, 1, 9, 34, 9, 11, CONC)
    t.fill(14, 1, 9, 20, 6, 11, AIR)
    t.fill(14, 7, 9, 20, 7, 9, HAZ)
    for x in range(14, 21):
        t.set(x, 6, 10, "minecraft:iron_bars[east=true,north=false,south=false,waterlogged=false,west=true]")
    t.fill(2, 10, 9, 32, 10, 11, "minecraft:polished_andesite")
    for x in range(2, 33, 4):
        t.set(x, 9, 8, LAMP)
    t.set(17, 8, 8, EMBLEM)
    t.sign(17, 8, 7, "north", [f"GATE {letter}", "SITE-19", "SURFACE ACCESS"]) if False else t.sign(16, 8, 8, "north", [f"GATE {letter}", "SITE-19", "SURFACE ACCESS"])
    # guard towers
    for gx in (3, 27):
        t.box(gx, 0, 13, gx + 5, 5, 18, CONC, FLOOR, CONC)
        t.set(gx + 2, 1, 13, AIR); t.set(gx + 2, 2, 13, AIR)
        t.chest(gx + 4, 1, 17, "chests/armory", "west")
        t.set(gx + 2, 5, 15, LAMP)
    # helipad
    t.fill(10, 0, 1, 24, 0, 7, "minecraft:black_concrete")
    for z in range(2, 7):
        t.set(14, 0, z, "minecraft:yellow_concrete"); t.set(20, 0, z, "minecraft:yellow_concrete")
    for x in range(14, 21):
        t.set(x, 0, 4, "minecraft:yellow_concrete")
    for (x, z) in [(10, 1), (24, 1), (10, 7), (24, 7)]:
        t.set(x, 0, z, "minecraft:redstone_lamp[lit=false]")
    t.entity(17, 1, 13, "minecraft:iron_golem", "Foundation Security", {"PlayerCreated": Byte(1)})
    t.entity(17, 1, 5, "minecraft:iron_golem", "Foundation Security", {"PlayerCreated": Byte(1)})
    return t


def helipad(seed):
    t = T(17, 6, 17)
    t.fill(1, 0, 1, 15, 0, 15, "minecraft:black_concrete")
    t.fill(1, 1, 1, 15, 4, 15, AIR)
    for i in range(1, 16):
        for c in [(i, 1), (i, 15), (1, i), (15, i)]:
            t.set(c[0], 0, c[1], "minecraft:yellow_concrete" if i % 2 else "minecraft:white_concrete")
    for z in range(5, 12):
        t.set(5, 0, z, "minecraft:white_concrete"); t.set(11, 0, z, "minecraft:white_concrete")
    for x in range(5, 12):
        t.set(x, 0, 8, "minecraft:white_concrete")
    for (x, z) in [(1, 1), (15, 1), (1, 15), (15, 15)]:
        t.set(x, 1, z, "minecraft:end_rod[facing=up]")
    t.box(12, 0, 12, 16, 4, 16, WALL, FLOOR, CONC) if False else None
    t.chest(14, 1, 3, "chests/armory", "west")
    t.chest(2, 1, 13, "chests/facility_common", "east")
    t.sign(8, 1, 0, "north", ["SITE HELIPAD", "KEEP CLEAR"]) if False else None
    return t


def watchtower(seed):
    rng = random.Random(seed)
    h = rng.randint(11, 15)
    t = T(9, h + 6, 9)
    for (x, z) in [(2, 2), (6, 2), (2, 6), (6, 6)]:
        t.fill(x, 0, z, x, h, z, CONC)
    t.fill(1, h, 1, 7, h, 7, "minecraft:polished_andesite")
    t.fill(1, h + 1, 1, 7, h + 1, 7, "minecraft:iron_bars[east=true,north=true,south=true,waterlogged=false,west=true]")
    t.fill(2, h + 1, 2, 6, h + 3, 6, AIR)
    t.fill(1, h + 4, 1, 7, h + 4, 7, CONC)
    t.set(4, h + 4, 4, LAMP)
    for y in range(1, h + 1):
        t.set(4, y, 3, "minecraft:ladder[facing=south,waterlogged=false]") if False else None
    t.fill(4, 0, 2, 4, h - 1, 2, CONC)
    for y in range(0, h + 1):
        t.set(4, y, 3, "minecraft:ladder[facing=south,waterlogged=false]")
    t.set(4, h, 3, AIR)
    t.set(4, h, 3, "minecraft:ladder[facing=south,waterlogged=false]")
    t.chest(6, h + 1, 6, "chests/armory", "north") if False else t.chest(5, h + 1, 5, "chests/armory", "north")
    t.set(3, h + 1, 5, "minecraft:bell[attachment=floor,facing=north,powered=false]")
    return t


def comms_relay(seed):
    rng = random.Random(seed)
    h = rng.randint(16, 22)
    t = T(13, h + 3, 11)
    t.box(1, 0, 1, 7, 4, 7, WALL, FLOOR, CONC)
    t.set(4, 1, 7, AIR); t.set(4, 2, 7, AIR)
    t.set(4, 4, 4, LAMP)
    t.set(2, 1, 2, "minecraft:observer[facing=north,powered=false]")
    t.set(3, 1, 2, "minecraft:target[power=0]")
    t.set(5, 1, 2, "minecraft:observer[facing=north,powered=false]")
    t.chest(6, 1, 3, "chests/research", "west")
    t.sign(4, 3, 8, "south", ["COMMS RELAY", "DO NOT", "TOUCH"])
    for y in range(0, h):
        t.set(10, y, 5, "minecraft:iron_bars[east=true,north=true,south=true,waterlogged=false,west=true]" if y % 4 else "minecraft:iron_block")
    t.set(10, h, 5, "minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]")
    t.set(10, h + 1, 5, "minecraft:end_rod[facing=up]")
    return t


def ruin(t, seed, intensity=0.3):
    rng = random.Random(seed)
    keys = list(t.blocks.keys())
    for k in keys:
        st, nbt = t.blocks[k]
        if st in (WALL, CONC, FLOOR) and rng.random() < intensity * (0.5 + k[1] / 6.0):
            if k[1] >= 3 and rng.random() < 0.7:
                t.blocks[k] = (AIR, None)
            else:
                t.blocks[k] = (rng.choice(["minecraft:cracked_stone_bricks", "minecraft:mossy_cobblestone", "minecraft:gravel", "minecraft:cobblestone", CONC]), None)
        elif st == LAMP:
            t.blocks[k] = ("minecraft:cracked_stone_bricks", None)
        elif st == AIR and k[1] in (1, 2, 3) and rng.random() < 0.02:
            t.blocks[k] = ("minecraft:cobweb", None)
        elif st.startswith("minecraft:iron_door") and rng.random() < 0.6:
            t.blocks[k] = (AIR, None)
        elif "keycard_reader" in st and rng.random() < 0.5:
            t.blocks[k] = ("minecraft:cracked_stone_bricks", None)
    return t


def store_3008(seed):
    rng = random.Random(seed)
    W, D, H = 40, 40, 8
    t = T(W + 4, H + 3, D + 4)
    t.box(2, 0, 2, W + 1, H + 1, D + 1, "minecraft:light_blue_concrete", "minecraft:smooth_stone", "minecraft:white_concrete")
    lights(t, 2, W + 1, 2, D + 1, H + 1, 5)
    for x in range(5, W - 1, 6):
        for z in range(5, D - 1, 6):
            kind = rng.random()
            if kind < 0.45:  # shelving aisle
                for dz in range(0, 4):
                    t.fill(x, 1, z + dz, x, rng.randint(3, 5), z + dz, rng.choice(["minecraft:barrel[facing=up,open=false]", "minecraft:spruce_planks", "minecraft:birch_planks"]))
                if rng.random() < 0.3:
                    t.chest(x + 1, 1, z + 1, "chests/facility_common", block="barrel")
            elif kind < 0.7:  # showroom bed
                color = rng.choice(["white", "light_gray", "yellow", "blue"])
                t.set(x, 1, z, f"minecraft:{color}_bed[facing=north,occupied=false,part=head]")
                t.set(x, 1, z + 1, f"minecraft:{color}_bed[facing=north,occupied=false,part=foot]")
                t.set(x + 1, 1, z, "minecraft:birch_trapdoor[facing=north,half=top,open=false,powered=false,waterlogged=false]")
            elif kind < 0.9:  # table set
                t.set(x, 1, z, "minecraft:birch_fence[east=false,north=false,south=false,waterlogged=false,west=false]")
                t.set(x, 2, z, "minecraft:birch_pressure_plate[powered=false]")
                t.set(x + 1, 1, z, "minecraft:birch_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]")
                t.set(x - 1, 1, z, "minecraft:birch_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]")
            else:
                t.chest(x, 1, z, "chests/facility_common", "north")
    # entrance
    for z in (21, 22, 23):
        t.fill(2, 1, z, 2, 3, z, AIR)
    t.sign(1, 4, 22, "west", ["SCP-3008", "FURNITURE STORE", "OPEN 24/7"], wood="birch", color="yellow")
    return t


# --------------------------------------------------------------------------------------
# Loot tables
# --------------------------------------------------------------------------------------
def item(name, w=10, mn=1, mx=1, extra_fn=None):
    e = {"type": "minecraft:item", "name": name, "weight": w}
    fns = []
    if mx > 1:
        fns.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": mn, "max": mx}})
    if extra_fn:
        fns.append(extra_fn)
    if fns:
        e["functions"] = fns
    return e


def table(path, pools):
    write_json(os.path.join(DATA, "loot_tables", path + ".json"), {"type": "minecraft:chest", "pools": pools})


def pool(mn, mx, entries):
    return {"rolls": {"type": "minecraft:uniform", "min": mn, "max": mx}, "entries": entries}


def loot_tables():
    doc = item(f"{M}:scp_document", 20, 1, 3)
    table("chests/facility_common", [pool(3, 6, [
        item("minecraft:bread", 15, 1, 3), item("minecraft:paper", 12, 1, 5), item("minecraft:iron_nugget", 10, 1, 6),
        item("minecraft:torch", 10, 2, 8), item("minecraft:coal", 8, 1, 4), item(f"{M}:keycard_level_1", 8), item(f"{M}:keycard_level_2", 3),
        doc, item("minecraft:glass_bottle", 6, 1, 3), item(f"{M}:scp_458_pizza", 4, 1, 2)])])
    table("chests/research", [pool(3, 6, [
        doc, item("minecraft:book", 10, 1, 3), item("minecraft:paper", 10, 2, 6), item("minecraft:redstone", 8, 1, 6),
        item(f"{M}:keycard_level_2", 8), item(f"{M}:keycard_level_3", 4), item("minecraft:glass_bottle", 6, 1, 3),
        item("minecraft:experience_bottle", 4, 1, 3), item(f"{M}:scp_500", 1), item(f"{M}:scp_207", 2), item(f"{M}:scp_294_cup", 2), item(f"{M}:scp_1471", 1), item(f"{M}:scp_008", 1), item(f"{M}:scp_178", 1)])])
    table("chests/archive", [pool(2, 5, [
        item(f"{M}:scp_document", 40, 1, 5), item("minecraft:book", 10, 1, 3), item("minecraft:writable_book", 4),
        item(f"{M}:keycard_level_2", 6), item(f"{M}:keycard_level_3", 4), item(f"{M}:keycard_level_4", 1), item(f"{M}:scp_1025", 2)])])
    table("chests/armory", [pool(3, 6, [
        item("minecraft:iron_sword", 8), item("minecraft:crossbow", 6), item("minecraft:arrow", 12, 4, 16), item("minecraft:shield", 6),
        item("minecraft:iron_helmet", 5), item("minecraft:iron_chestplate", 4), item("minecraft:iron_leggings", 4), item("minecraft:iron_boots", 5),
        item(f"{M}:keycard_level_3", 6), item(f"{M}:keycard_level_4", 3), item("minecraft:cooked_beef", 8, 1, 4),
        item(f"{M}:scp_018", 1)])])
    table("chests/medical", [pool(2, 5, [
        item("minecraft:golden_apple", 4), item("minecraft:bread", 10, 1, 3), item("minecraft:glistering_melon_slice", 4, 1, 2),
        item("minecraft:potion", 10, extra_fn={"function": "minecraft:set_potion", "id": "minecraft:healing"}),
        item("minecraft:potion", 6, extra_fn={"function": "minecraft:set_potion", "id": "minecraft:regeneration"}),
        item("minecraft:milk_bucket", 4), item(f"{M}:scp_500", 2), item(f"{M}:scp_006", 3, 1, 2), item(f"{M}:keycard_level_2", 5)])])
    table("chests/o5_vault", [pool(3, 5, [
        item(f"{M}:keycard_o5", 4), item(f"{M}:keycard_level_5", 6), item("minecraft:diamond", 10, 1, 4), item("minecraft:emerald", 10, 2, 6),
        item("minecraft:netherite_scrap", 3), item(f"{M}:scp_500", 4), item(f"{M}:scp_005", 2), item(f"{M}:scp_714", 2),
        item(f"{M}:scp_427", 2), item(f"{M}:scp_268", 2), item(f"{M}:scp_document", 10, 2, 6), item(f"{M}:scp_035", 2), item(f"{M}:scp_963", 2)])])
    table("chests/insurgency", [pool(3, 6, [
        item("minecraft:crossbow", 6), item("minecraft:arrow", 10, 4, 12), item("minecraft:tnt", 5, 1, 3), item("minecraft:gold_ingot", 8, 1, 4),
        item("minecraft:emerald", 6, 1, 3), item(f"{M}:keycard_level_3", 4), item(f"{M}:keycard_level_4", 2), item(f"{M}:scp_207", 3),
        item(f"{M}:scp_document", 8, 1, 2), item("minecraft:cooked_porkchop", 8, 1, 4), item(f"{M}:scp_1471", 2), item(f"{M}:scp_008", 2)])])
    table("chests/goc", [pool(3, 6, [
        item("minecraft:iron_chestplate", 5), item("minecraft:iron_helmet", 5), item("minecraft:arrow", 10, 4, 16), item("minecraft:bow", 6),
        item("minecraft:tnt", 6, 1, 4), item("minecraft:emerald", 8, 1, 4), item("minecraft:gunpowder", 8, 2, 6),
        item(f"{M}:scp_document", 6, 1, 2), item("minecraft:golden_carrot", 6, 1, 4)])])
    table("chests/serpents_hand", [pool(3, 6, [
        item("minecraft:book", 12, 1, 4), item("minecraft:book", 6, extra_fn={"function": "minecraft:enchant_randomly"}),
        item("minecraft:ender_pearl", 6, 1, 3), item("minecraft:lapis_lazuli", 10, 2, 8), item("minecraft:amethyst_shard", 8, 1, 6),
        item(f"{M}:scp_document", 10, 1, 4), item(f"{M}:scp_427", 1), item(f"{M}:scp_714", 1), item("minecraft:experience_bottle", 6, 1, 4), item(f"{M}:scp_035", 1), item(f"{M}:scp_963", 1)])])
    for it in ["scp_500", "scp_207", "scp_018", "scp_005", "scp_427", "scp_714", "scp_268", "scp_063"]:
        cnt = 3 if it in ("scp_500", "scp_207") else 1
        table(f"chests/vault/{it}", [
            {"rolls": 1, "entries": [item(f"{M}:{it}", 1, 1, cnt)]},
            pool(1, 3, [item(f"{M}:scp_document", 10, 1, 2), item("minecraft:paper", 10, 1, 4), item(f"{M}:keycard_level_3", 3)])])


# --------------------------------------------------------------------------------------
# Structure catalogue (99 entries)
# --------------------------------------------------------------------------------------
STRUCTURES = []  # (name, template, kind)  kind: surface | underground(depth)


def add(name, t, underground=0):
    STRUCTURES.append((name, t, underground))


def build_all():
    # ---- 25 containment / anomaly sites -------------------------------------------
    add("cell_scp_173", containment_cell("cell_scp_173", "scp_173", (9, 5, 9), 3))
    add("cell_scp_096", containment_cell("cell_scp_096", "scp_096", (11, 6, 11), 4, glass=False))
    add("cell_scp_049", containment_cell("cell_scp_049", "scp_049", (9, 5, 9), 4))
    add("cell_scp_106", containment_cell("cell_scp_106", "scp_106", (9, 5, 9), 5, dark=True))
    add("cell_scp_682", containment_cell("cell_scp_682", "scp_682", (17, 8, 15), 5))
    add("cell_scp_939", containment_cell("cell_scp_939", ["scp_939", "scp_939"], (13, 5, 11), 4, dark=True))
    add("cell_scp_999", containment_cell("cell_scp_999", "scp_999", (9, 5, 9), 1))
    add("cell_scp_131", containment_cell("cell_scp_131", ["scp_131", "scp_131"], (7, 4, 7), 1))
    add("cell_scp_529", containment_cell("cell_scp_529", "scp_529", (7, 4, 7), 1))
    add("cell_scp_1048", containment_cell("cell_scp_1048", "scp_1048", (7, 4, 7), 3))

    def d914(t, ox, oz, x1, z1, cx, cz):
        t.set(cx, 1, oz + 2, f"{M}:scp_914[facing=south,setting=2]")
        for dx in (-3, 3):
            t.box(cx + dx - 1, 0, oz + 1, cx + dx + 1, 4, oz + 3, CONC, HAZ, CONC)
            t.set(cx + dx, 1, oz + 3, AIR); t.set(cx + dx, 2, oz + 3, AIR)
        t.chest(ox + 1, 1, z1 - 2, "chests/research", "east")
    add("chamber_scp_914", scp_room("scp_914", None, 2, d914, (13, 5, 11)))

    def d294(t, ox, oz, x1, z1, cx, cz):
        t.set(cx, 1, oz + 1, f"{M}:scp_294[facing=south]")
        for x in range(ox + 2, x1 - 1, 3):
            t.set(x, 1, cz, "minecraft:oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]")
            t.set(x, 2, cz, "minecraft:oak_pressure_plate[powered=false]")
        t.chest(x1 - 1, 1, oz + 1, "chests/facility_common", "west")
    add("breakroom_scp_294", scp_room("scp_294", None, 1, d294))

    def d330(t, ox, oz, x1, z1, cx, cz):
        t.set(cx, 1, cz, f"{M}:scp_330")
        t.sign(cx, 2, oz + 1, "south", ["TAKE ONLY TWO"], wood="birch", color="black") if False else None
    add("room_scp_330", scp_room("scp_330", None, 2, d330, (9, 4, 9)))
    add("room_scp_458", scp_room("scp_458", f"{M}:scp_458[facing=south]", 1, None, (9, 4, 9)))
    add("room_scp_1162", scp_room("scp_1162", f"{M}:scp_1162[facing=south]", 2, None, (9, 4, 9)))
    for i, (it, lvl) in enumerate([("scp_500", 4), ("scp_207", 2), ("scp_018", 3), ("scp_005", 5), ("scp_427", 3), ("scp_714", 3), ("scp_268", 3), ("scp_063", 2)]):
        add(f"vault_{it}", vault(it, lvl, seed=100 + i))
    t087, d087 = stairwell_087()
    add("scp_087_stairwell", t087, d087)
    add("scp_3008_store", store_3008(3008))

    # ---- 74 Foundation facilities and hostile-group sites --------------------------
    def site(n): return f"SITE-{n}"
    hostile = ["scp_173", "scp_096", "scp_049", "scp_106", "scp_939", "scp_049_2", "scp_049_2"]
    safe = ["scp_999", "scp_131", "scp_529", "scp_1048", "scp_066", "scp_2295"]
    lockers = ["locker:scp_500", "locker:scp_207", "locker:scp_018", "locker:scp_427", "locker:scp_714", "locker:scp_063"]

    for i, n in enumerate([19, 17, 43, 62, 77]):
        add(f"site_{n}_research_lab", facility("lab", 1000 + n, ["lab", "office", "lab", "server", "storage", "breakroom"],
                                               sign=[site(n), "RESEARCH", "DIVISION"], door_levels={"server": 3, "lab": 2}, second_exit=i % 2 == 0))
    for n in [19, 13, 81, 120]:
        add(f"site_{n}_archive", facility("archive", 2000 + n, ["archive", "anomaly:scp_012" if n == 13 else "archive", "office", "archive"],
                                          sign=[site(n), "RECORDS", "ARCHIVE"], entrance=2, door_levels={"archive": 3}))
    for n in [11, 28, 36, 64]:
        add(f"site_{n}_armory", facility("armory", 3000 + n, ["armory", "security", "armory", "storage"],
                                         sign=[site(n), "ARMORY", "LEVEL 3"], entrance=2, door_levels={"armory": 3}, palette=(CONC, FLOOR, CONC)))
    for n in [17, 23, 88, 91]:
        add(f"site_{n}_medical_bay", facility("medical", 4000 + n, ["medical", "medical", "storage", "office"],
                                              sign=[site(n), "MEDICAL", "BAY"], palette=("minecraft:white_concrete", FLOOR, CONC)))
    for n in [19, 6, 103, 54]:
        add(f"site_{n}_dclass_barracks", facility("barracks", 5000 + n, ["barracks", "barracks", "breakroom", "barracks", "security"],
                                                  sign=[site(n), "D-CLASS", "HOUSING"], palette=("minecraft:orange_terracotta", FLOOR, CONC), door_levels={"security": 2}))
    for i, n in enumerate([19, 17, 13, 66, 99]):
        rng = random.Random(6000 + n)
        mobs = [rng.choice(hostile) for _ in range(6)]
        add(f"site_{n}_heavy_containment", facility("hcz", 6000 + n, ["cell", "cell", "security", "cell"],
                                                     sign=[site(n), "HEAVY", "CONTAINMENT", "ZONE"], entrance=3, cell_mobs=mobs,
                                                     palette=(CONC, CONC, CONC), door_levels={"security": 3}, W=rng.randint(26, 34)))
    for n in [19, 17, 11, 25, 40]:
        rng = random.Random(7000 + n)
        mobs = [rng.choice(safe) for _ in range(6)]
        rooms = ["cell", rng.choice(lockers), "cell", rng.choice(lockers), "office"]
        add(f"site_{n}_light_containment", facility("lcz", 7000 + n, rooms, sign=[site(n), "LIGHT", "CONTAINMENT", "ZONE"],
                                                    entrance=2, cell_mobs=mobs, door_levels={"cell": 2, **{l: 3 for l in lockers}}))
    for i in range(5):
        add(f"checkpoint_{chr(ord('a') + i)}", checkpoint(8000 + i))
    add("gate_a", gate(8100, "A"))
    add("gate_b", gate(8101, "B"))
    add("helipad_alpha", helipad(8200))
    add("helipad_omega", helipad(8201))
    for i in range(3):
        add(f"watchtower_{i + 1}", watchtower(8300 + i))
    for n in [14, 33, 51, 72]:
        add(f"outpost_{n}", facility("outpost", 8400 + n, ["office", "storage", "breakroom", "security", "lab"],
                                     sign=[f"OUTPOST-{n}", "SCP FOUNDATION"], W=random.Random(n).randint(18, 24), D=15))
    for i, n in enumerate([12, 31, 47, 68, 85]):
        add(f"area_{n}_bunker", bunker(f"area_{n}", 8500 + n, random.Random(n).randint(18, 28),
                                       ["storage", "armory", "office", "barracks", "server"], door_levels={"armory": 3},
                                       sign=[f"AREA-{n}", "UNDERGROUND", "FACILITY"]))
    add("o5_council_bunker", bunker("o5", 8600, 34, ["office", "archive", "security", "cell", "locker:scp_005", "storage"],
                                    door_levels={"office": 5, "archive": 5, "security": 4, "cell": 5, "locker:scp_005": 5, "storage": 4},
                                    cell_mobs=["scp_682"] if False else ["scp_999"], sign=["O5 COUNCIL", "[REDACTED]"], loot_vault="chests/o5_vault"))
    for i, n in enumerate([4, 8, 26, 59]):
        t = facility("ruin", 8700 + n, ["lab", "cell", "storage", "office"], sign=[site(n), "DECOMMISSIONED"],
                     cell_mobs=["scp_049_2", "scp_049_2", "scp_939"], entrance=0)
        add(f"ruined_site_{n}", ruin(t, 8700 + n, 0.25 + i * 0.05))
    for i in range(4):
        add(f"chaos_insurgency_camp_{i + 1}", camp_ci(8800 + i))
    for i in range(3):
        add(f"goc_outpost_{i + 1}", goc_outpost(8900 + i))
    for i in range(2):
        add(f"serpents_hand_hideout_{i + 1}", serpents_hand(9000 + i))
    for n, unit in [(1, "EPSILON-11"), (2, "NU-7"), (3, "ALPHA-1")]:
        add(f"mtf_base_{unit.lower().replace('-', '_')}", facility("mtf", 9100 + n, ["armory", "barracks", "security", "office", "armory"],
                                                                     sign=["MOBILE TASK", "FORCE", unit], entrance=3,
                                                                     palette=("minecraft:gray_concrete", FLOOR, CONC), door_levels={"armory": 4}))
    for i in range(3):
        add(f"comms_relay_{i + 1}", comms_relay(9200 + i))
    for n in [9, 21]:
        add(f"site_{n}_power_station", facility("power", 9300 + n, ["generator", "generator", "storage", "security"],
                                                sign=[site(n), "POWER", "STATION", "HIGH VOLTAGE"], palette=(CONC, CONC, CONC), door_levels={"generator": 2}))


SURFACE_BIOMES = ["plains", "sunflower_plains", "snowy_plains", "desert", "savanna", "savanna_plateau", "forest",
                  "flower_forest", "birch_forest", "old_growth_birch_forest", "dark_forest", "taiga", "snowy_taiga",
                  "old_growth_pine_taiga", "old_growth_spruce_taiga", "meadow", "cherry_grove", "badlands",
                  "wooded_badlands", "swamp", "sparse_jungle", "windswept_savanna", "ice_spikes", "mushroom_fields"]


def worldgen():
    write_json(os.path.join(DATA, "tags", "worldgen", "biome", "has_structure", "foundation_site.json"),
               {"replace": False, "values": [f"minecraft:{b}" for b in SURFACE_BIOMES]})
    names = []
    for idx, (name, t, depth) in enumerate(STRUCTURES):
        t.save(name)
        names.append(name)
        write_json(os.path.join(DATA, "worldgen", "template_pool", f"{name}.json"), {
            "name": f"{M}:{name}",
            "fallback": "minecraft:empty",
            "elements": [{"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"{M}:{name}",
                                                   "projection": "rigid", "processors": "minecraft:empty"}}]})
        s = {
            "type": "minecraft:jigsaw",
            "biomes": f"#{M}:has_structure/foundation_site",
            "step": "surface_structures" if not depth else "underground_structures",
            "spawn_overrides": {},
            "terrain_adaptation": "beard_thin" if not depth else "none",
            "start_pool": f"{M}:{name}",
            "size": 1,
            "start_height": {"absolute": 0 if not depth else -depth},
            "project_start_to_heightmap": "WORLD_SURFACE_WG",
            "max_distance_from_center": 80,
            "use_expansion_hack": False,
        }
        write_json(os.path.join(DATA, "worldgen", "structure", f"{name}.json"), s)
    # group into structure sets of 9
    for g in range(0, len(names), 9):
        group = names[g:g + 9]
        write_json(os.path.join(DATA, "worldgen", "structure_set", f"foundation_sites_{g // 9 + 1:02d}.json"), {
            "structures": [{"structure": f"{M}:{n}", "weight": 1} for n in group],
            "placement": {"type": "minecraft:random_spread", "spacing": 44, "separation": 14, "salt": 731500 + g * 7919}})
    write_json(os.path.join(DATA, "tags", "worldgen", "structure", "all.json"), {"replace": False, "values": [f"{M}:{n}" for n in names]})
    write_json(os.path.join(DATA, "tags", "worldgen", "structure", "containment.json"),
               {"replace": False, "values": [f"{M}:{n}" for n in names if n.startswith(("cell_", "chamber_", "room_", "vault_", "breakroom_", "scp_"))]})
    write_json(os.path.join(DATA, "tags", "worldgen", "structure", "facilities.json"),
               {"replace": False, "values": [f"{M}:{n}" for n in names if not n.startswith(("cell_", "chamber_", "room_", "vault_", "breakroom_", "scp_"))]})
    return names


def dimension():
    write_json(os.path.join(DATA, "dimension_type", "pocket_dimension.json"), {
        "ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": False, "has_ceiling": False,
        "ambient_light": 0.0, "fixed_time": 18000, "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0,
        "piglin_safe": False, "bed_works": False, "respawn_anchor_works": False, "has_raids": False,
        "logical_height": 256, "min_y": 0, "height": 256, "infiniburn": "#minecraft:infiniburn_overworld",
        "effects": "minecraft:the_end"})
    write_json(os.path.join(DATA, "dimension", "pocket_dimension.json"), {
        "type": f"{M}:pocket_dimension",
        "generator": {"type": "minecraft:flat", "settings": {
            "biome": "minecraft:the_void", "lakes": False, "features": False,
            "layers": [{"block": "minecraft:bedrock", "height": 1}, {"block": f"{M}:corroded_block", "height": 3}],
            "structure_overrides": []}}})


if __name__ == "__main__":
    loot_tables()
    build_all()
    names = worldgen()
    dimension()
    assert len(names) == 99, len(names)
    assert len(set(names)) == 99
    print(f"{len(names)} structures generated")
