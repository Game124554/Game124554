"""Builds the 7 structure .nbt templates + jigsaw worldgen JSON (structure, template pool, structure set, biome tag)."""
import os, sys, gzip, struct, json, math, random
sys.path.insert(0, os.path.dirname(__file__))
from spec import MODID, STRUCTURES

R = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/data", MODID)
DATA_VERSION = 3465  # 1.20.1


# ------------------------------------------------------------------ minimal NBT writer
class Byte(int): pass
class Short(int): pass
class Int(int): pass
class Double(float): pass
class Float(float): pass
class String(str): pass
class Compound(dict): pass
class List(list):
    def __init__(self, tag, items):
        super().__init__(items)
        self.tag = tag

TAG = {Byte: 1, Short: 2, Int: 3, Float: 5, Double: 6, String: 8, List: 9, Compound: 10}


def tag_id(v):
    return TAG[type(v)]


def w_payload(out, v):
    t = type(v)
    if t is Byte: out += struct.pack(">b", v)
    elif t is Short: out += struct.pack(">h", v)
    elif t is Int: out += struct.pack(">i", v)
    elif t is Float: out += struct.pack(">f", v)
    elif t is Double: out += struct.pack(">d", v)
    elif t is String:
        b = v.encode("utf-8"); out += struct.pack(">H", len(b)) + b
    elif t is List:
        out += struct.pack(">b", TAG[v.tag] if len(v) else 0) + struct.pack(">i", len(v))
        for item in v: w_payload(out, item)
    elif t is Compound:
        for k, item in v.items():
            out += struct.pack(">b", tag_id(item))
            kb = k.encode(); out += struct.pack(">H", len(kb)) + kb
            w_payload(out, item)
        out += b"\x00"
    else:
        raise TypeError(t)


def write_nbt(path, root):
    out = bytearray()
    out += b"\x0a" + struct.pack(">H", 0)
    w_payload(out, root)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with gzip.open(path, "wb") as fh:
        fh.write(bytes(out))


# ------------------------------------------------------------------ structure builder
def L(n): return f"{MODID}:{n}"


class Struct:
    def __init__(self, sx, sy, sz, seed):
        self.size = (sx, sy, sz)
        self.blocks = {}
        self.entities = []
        self.rnd = random.Random(seed)

    def set(self, x, y, z, name, props=None, nbt=None):
        if 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]:
            self.blocks[(x, y, z)] = (name, tuple(sorted((props or {}).items())), nbt)

    def get(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return b[0] if b else None

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, props)

    def box(self, x0, y0, z0, x1, y1, z1, wall, floor=None, roof=None, air=True):
        if air:
            self.fill(x0, y0, z0, x1, y1, z1, "minecraft:air")
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    if x in (x0, x1) or z in (z0, z1):
                        self.set(x, y, z, wall)
        if floor: self.fill(x0, y0, z0, x1, y0, z1, floor)
        if roof: self.fill(x0, y1, z0, x1, y1, z1, roof)

    def chest(self, x, y, z, table, facing="north"):
        self.set(x, y, z, "minecraft:chest", {"facing": facing, "type": "single", "waterlogged": "false"},
                 Compound({"id": String("minecraft:chest"), "LootTable": String(L(f"chests/{table}"))}))

    def spawner(self, x, y, z, mob):
        self.set(x, y, z, "minecraft:spawner", None, Compound({
            "id": String("minecraft:mob_spawner"),
            "SpawnData": Compound({"entity": Compound({"id": String(L(mob))})}),
            "Delay": Short(20), "MinSpawnDelay": Short(240), "MaxSpawnDelay": Short(700), "SpawnCount": Short(2),
            "MaxNearbyEntities": Short(4), "RequiredPlayerRange": Short(16), "SpawnRange": Short(4)}))

    def mob(self, x, y, z, mob):
        self.entities.append((x, y, z, L(mob)))

    def save(self, name):
        palette, pal_index, blocks = [], {}, []
        for (pos, (bname, props, nbt)) in sorted(self.blocks.items()):
            key = (bname, props)
            if key not in pal_index:
                pal_index[key] = len(palette)
                entry = Compound({"Name": String(bname)})
                if props:
                    entry["Properties"] = Compound({k: String(v) for k, v in props})
                palette.append(entry)
            b = Compound({"pos": List(Int, [Int(pos[0]), Int(pos[1]), Int(pos[2])]), "state": Int(pal_index[key])})
            if nbt is not None:
                b["nbt"] = nbt
            blocks.append(b)
        ents = []
        for (x, y, z, eid) in self.entities:
            ents.append(Compound({
                "pos": List(Double, [Double(x + 0.5), Double(y), Double(z + 0.5)]),
                "blockPos": List(Int, [Int(x), Int(y), Int(z)]),
                "nbt": Compound({"id": String(eid), "PersistenceRequired": Byte(1)})}))
        root = Compound({
            "DataVersion": Int(DATA_VERSION),
            "size": List(Int, [Int(v) for v in self.size]),
            "palette": List(Compound, palette),
            "blocks": List(Compound, blocks),
            "entities": List(Compound, ents)})
        write_nbt(f"{R}/structures/{name}.nbt", root)
        return len(blocks), len(ents)


FACE = lambda f: {"facing": f}


def latex_splash(s, cx, y, cz, r, blocks, puddle, crystal, rnd):
    """organic patch of latex: blocks on the floor, puddles on top, crystals sprouting"""
    for x in range(cx - r, cx + r + 1):
        for z in range(cz - r, cz + r + 1):
            d = math.hypot(x - cx, z - cz)
            if d <= r - 0.3 * rnd.random():
                if d < r * 0.6 and rnd.random() < 0.7:
                    s.set(x, y, z, blocks)
                if s.get(x, y + 1, z) in (None, "minecraft:air") and rnd.random() < 0.45:
                    s.set(x, y + 1, z, puddle if rnd.random() < 0.8 else crystal)


# ------------------------------------------------------------------ 1. abandoned lab
def abandoned_lab():
    s = Struct(17, 8, 15, 1); r = s.rnd
    s.fill(0, 0, 0, 16, 0, 14, L("lab_floor"))
    for x in range(1, 16):
        for z in range(1, 14):
            if (x + z) % 2 == 0: s.set(x, 0, z, L("lab_tile"))
    s.box(0, 1, 0, 16, 6, 14, L("lab_wall"))
    s.fill(0, 3, 0, 16, 3, 14, L("lab_wall_striped")); s.fill(1, 3, 1, 15, 3, 13, "minecraft:air")
    s.fill(0, 7, 0, 16, 7, 14, L("steel_panel"))
    for (x, z) in ((4, 4), (12, 4), (4, 10), (12, 10), (8, 7)):
        s.set(x, 7, z, L("lab_ceiling_light"))
    for x in (3, 4, 12, 13):
        s.fill(x, 2, 0, x, 4, 0, L("reinforced_lab_glass")); s.fill(x, 2, 14, x, 4, 14, L("reinforced_lab_glass"))
    s.fill(7, 1, 0, 9, 3, 0, "minecraft:air")                       # entrance
    s.set(6, 4, 0, L("warning_light")); s.set(10, 4, 0, L("warning_light"))
    # interior wall dividing a containment room
    s.fill(11, 1, 1, 11, 5, 13, L("lab_wall")); s.fill(11, 1, 6, 11, 2, 8, "minecraft:air")
    s.fill(11, 3, 6, 11, 3, 8, L("reinforced_lab_glass"))
    # containment tanks
    for z in (2, 5, 9, 12):
        s.fill(14, 1, z, 14, 3, z, L("containment_tank")); s.set(14, 4, z, L("steel_panel"))
    s.set(14, 1, 5, "minecraft:air"); s.set(14, 2, 5, "minecraft:air")      # a broken tank...
    latex_splash(s, 13, 0, 6, 3, L("dark_latex_block"), L("dark_latex_puddle"), L("latex_crystal"), r)
    # workstations
    for z in (2, 4):
        s.set(1, 1, z, L("lab_computer"), FACE("east"))
    s.set(1, 1, 6, L("hypno_screen"), FACE("east"))
    for z in (9, 10, 11, 12):
        s.set(1, 1, z, L("server_rack"), FACE("east")); s.set(1, 2, z, L("server_rack"), FACE("east"))
    s.fill(4, 1, 6, 7, 1, 8, L("steel_panel"))                          # lab bench
    s.set(5, 2, 7, L("lab_computer"), FACE("south"))
    s.set(3, 1, 13, L("lab_crate")); s.set(4, 1, 13, L("lab_crate")); s.set(3, 2, 13, L("lab_crate"))
    s.chest(6, 1, 13, "lab_loot", "north")
    s.set(9, 1, 13, L("drone_conversion_pod"), FACE("north"))
    s.spawner(13, 1, 11, "dark_latex_drone")
    s.set(2, 6, 13, L("lab_vent")); s.set(14, 6, 1, L("lab_vent"))
    s.mob(8, 1, 4, "dr_vulpine")
    s.mob(13, 1, 3, "dark_latex_drone"); s.mob(13, 1, 8, "purple_latex_drone")
    return s


# ------------------------------------------------------------------ 2. latex nest
def latex_nest():
    s = Struct(15, 8, 15, 2); r = s.rnd
    c = 7
    for x in range(15):
        for z in range(15):
            d = math.hypot(x - c, z - c)
            if d > 7.2: continue
            s.set(x, 0, z, L("infected_dirt") if r.random() < 0.6 else L("infected_stone"))
            hgt = int(max(0, 4.5 - abs(d - 5.0) * 1.6))          # ring-shaped mound
            for y in range(1, hgt + 1):
                s.set(x, y, z, L("dark_latex_block") if r.random() < 0.85 else L("latex_bricks"))
            if d < 3.5:
                s.set(x, 0, z, L("dark_latex_block"))
                for y in range(1, 6): s.set(x, y, z, "minecraft:air")
                if r.random() < 0.5: s.set(x, 1, z, L("dark_latex_puddle"))
            elif hgt and r.random() < 0.25:
                s.set(x, hgt + 1, z, L("latex_crystal"))
    s.fill(c, 1, 0, c, 3, 3, "minecraft:air"); s.fill(c - 1, 1, 0, c + 1, 2, 3, "minecraft:air")  # tunnel in
    s.spawner(c, 0, c, "dark_latex_wolf")
    s.set(c + 2, 0, c - 1, L("latex_glow_lamp"))
    s.chest(c - 2, 1, c + 1, "nest_loot", "east")
    s.mob(c + 1, 1, c + 1, "dark_latex_wolf"); s.mob(c - 1, 1, c - 1, "latex_pup"); s.mob(c + 2, 1, c, "latex_pup")
    return s


# ------------------------------------------------------------------ 3. containment facility (underground)
def containment_facility():
    s = Struct(21, 8, 21, 3); r = s.rnd
    s.box(0, 0, 0, 20, 7, 20, L("steel_panel"), floor=L("dark_lab_tile"), roof=L("steel_panel"))
    s.fill(1, 0, 9, 19, 0, 11, L("hazard_block"))                     # corridor stripe
    for x in range(2, 19, 4):
        s.set(x, 7, 10, L("lab_ceiling_light"))
    # cells on both sides of the corridor
    contained = ["latex_tiger", "latex_shark", "purple_latex_dragon", "latex_snow_leopard", "white_latex_fox", "latex_bat"]
    k = 0
    for side in (0, 1):
        zs, ze, zf = (1, 7, 8) if side == 0 else (13, 19, 12)
        for x0 in (1, 8, 14):
            x1 = min(x0 + 5, 19)
            s.fill(x0, 1, zs, x0, 6, ze, L("steel_panel")) if x0 != 1 else None
            s.fill(x1, 1, zs, x1, 6, ze, L("steel_panel")) if x1 != 19 else None
            s.fill(x0 + 1, 1, zf, x1 - 1, 4, zf, L("metal_grate") if k % 2 else L("reinforced_lab_glass"))
            s.fill(x0 + 1, 5, zf, x1 - 1, 6, zf, L("steel_panel"))
            cx, cz = (x0 + x1) // 2, (zs + ze) // 2
            s.set(cx, 6, cz, L("warning_light"))
            if r.random() < 0.6:
                s.set(cx + 1, 1, cz + 1, L("white_latex_puddle") if k % 2 else L("dark_latex_puddle"))
            s.mob(cx, 1, cz, contained[k % len(contained)])
            k += 1
    # the control room / scientists' office at the east end of the corridor
    s.fill(19, 1, 9, 19, 3, 11, L("lab_wall"))
    s.set(19, 1, 9, L("lab_computer"), FACE("west")); s.set(19, 1, 10, L("hypno_screen"), FACE("west"))
    s.set(19, 1, 11, L("lab_computer"), FACE("west")); s.set(19, 2, 10, L("hypno_screen"), FACE("west"))
    s.set(1, 1, 9, L("drone_conversion_pod"), FACE("east")); s.set(1, 1, 11, L("drone_conversion_pod"), FACE("east"))
    s.set(1, 1, 10, L("server_rack"), FACE("east")); s.set(1, 2, 10, L("server_rack"), FACE("east"))
    s.chest(18, 1, 9, "lab_loot", "west"); s.chest(2, 1, 9, "lab_loot", "east")
    s.spawner(10, 0, 10, "white_latex_drone")
    s.set(10, 3, 0, "minecraft:air"); s.set(10, 4, 0, "minecraft:air")   # vent shaft hint
    s.mob(15, 1, 10, "dr_ursus"); s.mob(5, 1, 10, "dr_lupa")
    return s


# ------------------------------------------------------------------ 4. drone hive tower
def drone_hive():
    s = Struct(13, 18, 13, 4); r = s.rnd
    c = 6
    for y in range(0, 17):
        for x in range(13):
            for z in range(13):
                d = math.hypot(x - c, z - c)
                rad = 6 - (y // 6) * 0.6
                if d <= rad:
                    if d > rad - 1.1:
                        s.set(x, y, z, L("white_latex_block") if r.random() < 0.92 else L("white_latex_crystal") if y % 6 else L("latex_glow_lamp"))
                    elif y % 6 == 0:
                        s.set(x, y, z, L("white_latex_block") if d > 1.5 or y == 0 else "minecraft:air")
                    else:
                        s.set(x, y, z, "minecraft:air")
    # visor windows: glowing bands
    for y in (3, 9, 15):
        for x in range(13):
            for z in range(13):
                if s.get(x, y, z) == L("white_latex_block"):
                    s.set(x, y, z, L("cyan_latex_block") if (x + z) % 3 else L("latex_glow_lamp"))
    s.fill(c, 1, 0, c, 2, 1, "minecraft:air"); s.fill(c - 1, 1, 0, c + 1, 2, 1, "minecraft:air")
    s.set(c, 17, c, L("latex_glow_lamp"))
    # levels connected by a central shaft with ladders
    for y in range(1, 17):
        s.set(c, y, c + 1, L("white_latex_block"))
        s.set(c, y, c, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
    s.spawner(c + 3, 1, c, "white_latex_drone")
    s.set(c - 3, 13, c, L("drone_conversion_pod"), FACE("east")); s.set(c + 3, 13, c, L("hypno_screen"), FACE("west"))
    s.chest(c, 13, c - 3, "lab_loot", "south")
    s.mob(c + 2, 1, c + 2, "white_latex_drone"); s.mob(c - 2, 7, c - 2, "drone_scout")
    s.mob(c + 2, 7, c - 2, "white_latex_drone"); s.mob(c - 2, 13, c + 2, "hypno_drone")
    return s


# ------------------------------------------------------------------ 5. research outpost
def research_outpost():
    s = Struct(9, 6, 9, 5)
    s.box(0, 0, 0, 8, 5, 8, L("steel_panel"), floor=L("lab_tile"), roof=L("rusted_steel_panel"))
    s.fill(0, 1, 0, 8, 1, 8, L("rusted_steel_panel")); s.fill(1, 1, 1, 7, 1, 7, "minecraft:air")
    s.fill(4, 1, 0, 4, 2, 0, "minecraft:air")
    s.set(2, 3, 0, L("reinforced_lab_glass")); s.set(6, 3, 0, L("reinforced_lab_glass"))
    s.set(0, 3, 4, L("reinforced_lab_glass")); s.set(8, 3, 4, L("reinforced_lab_glass"))
    s.set(4, 5, 4, L("lab_ceiling_light"))
    s.set(1, 1, 7, L("lab_computer"), FACE("south")); s.set(2, 1, 7, L("server_rack"), FACE("south"))
    s.set(7, 1, 7, L("lab_crate")); s.set(7, 2, 7, L("lab_crate")); s.set(7, 1, 6, L("lab_crate"))
    s.chest(6, 1, 7, "lab_loot", "north")
    s.set(1, 1, 1, L("containment_tank")); s.set(1, 2, 1, L("containment_tank"))
    s.set(5, 4, 0, L("warning_light"))
    s.mob(4, 1, 4, "dr_vermin")
    return s


# ------------------------------------------------------------------ 6. infected ruins
def infected_ruins():
    s = Struct(15, 7, 15, 6); r = s.rnd
    s.fill(0, 0, 0, 14, 0, 14, L("infected_stone"))
    for x in range(15):
        for z in range(15):
            if r.random() < 0.3: s.set(x, 0, z, L("infected_dirt"))
    # broken house walls
    for (x0, z0, x1, z1) in ((1, 1, 7, 6), (8, 8, 13, 13)):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    hgt = r.randint(1, 5)
                    for y in range(1, hgt + 1):
                        s.set(x, y, z, L("cracked_latex_bricks") if r.random() < 0.5 else L("latex_bricks"))
                else:
                    s.set(x, 0, z, L("infected_planks"))
                    for y in range(1, 5): s.set(x, y, z, "minecraft:air")
        for y in range(1, 6):
            s.set(x0, y, z0, L("infected_log"), {"axis": "y"}); s.set(x1, y, z1, L("infected_log"), {"axis": "y"})
    latex_splash(s, 10, 0, 3, 3, L("dark_latex_block"), L("dark_latex_puddle"), L("latex_crystal"), r)
    latex_splash(s, 4, 0, 11, 2, L("white_latex_block"), L("white_latex_puddle"), L("white_latex_crystal"), r)
    s.chest(3, 1, 3, "nest_loot", "south")
    s.spawner(10, 1, 10, "latex_goo_crawler")
    s.mob(5, 1, 9, "latex_goo_blob"); s.mob(11, 1, 5, "latex_goo_crawler"); s.mob(4, 1, 4, "latex_goo_crawler")
    return s


# ------------------------------------------------------------------ 7. hypno tower
def hypno_tower():
    s = Struct(9, 21, 9, 7)
    s.fill(0, 0, 0, 8, 0, 8, L("dark_lab_tile"))
    s.box(1, 1, 1, 7, 15, 7, L("dark_lab_tile"))
    for y in range(1, 16, 3):
        s.fill(1, y, 1, 7, y, 1, L("purple_latex_block")); s.fill(1, y, 7, 7, y, 7, L("purple_latex_block"))
    s.fill(4, 1, 7, 4, 2, 7, "minecraft:air")
    # ladder up the inside of the north wall
    for y in range(1, 16):
        s.set(4, y, 2, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    s.fill(2, 8, 2, 6, 8, 6, L("metal_grate")); s.set(4, 8, 2, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    # top observation room
    s.fill(0, 15, 0, 8, 15, 8, L("steel_panel"))
    s.set(4, 15, 2, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    s.set(4, 15, 1, L("steel_panel"))
    s.fill(4, 15, 1, 4, 15, 1, L("steel_panel"))
    s.box(0, 16, 0, 8, 19, 8, L("reinforced_lab_glass"), roof=L("steel_panel"))
    s.fill(0, 16, 0, 8, 16, 8, L("steel_panel")); s.fill(1, 16, 1, 7, 16, 7, "minecraft:air")
    s.fill(1, 15, 1, 7, 15, 7, L("dark_lab_tile"))
    s.set(4, 15, 2, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    # giant hypno screens facing all four directions
    s.set(4, 18, 0, L("hypno_screen"), FACE("north")); s.set(4, 18, 8, L("hypno_screen"), FACE("south"))
    s.set(0, 18, 4, L("hypno_screen"), FACE("west")); s.set(8, 18, 4, L("hypno_screen"), FACE("east"))
    s.set(4, 20, 4, L("warning_light")); s.set(4, 19, 4, L("lab_ceiling_light"))
    s.set(6, 16, 6, L("lab_computer"), FACE("north")); s.chest(2, 16, 6, "lab_loot", "east")
    s.spawner(6, 1, 3, "hypno_drone")
    s.mob(5, 16, 4, "dr_corvax"); s.mob(3, 1, 5, "hypno_drone"); s.mob(5, 1, 5, "hypno_drone")
    return s


BUILDERS = {"abandoned_lab": abandoned_lab, "latex_nest": latex_nest, "containment_facility": containment_facility,
            "drone_hive": drone_hive, "research_outpost": research_outpost, "infected_ruins": infected_ruins,
            "hypno_tower": hypno_tower}

# spacing, separation, salt, underground, biomes
PLACEMENT = {
    "abandoned_lab": (36, 12, 74821013, False, ["#minecraft:is_forest", "#minecraft:is_taiga", "minecraft:plains", "minecraft:meadow"]),
    "latex_nest": (28, 8, 74821014, False, ["#minecraft:is_forest", "minecraft:dark_forest", "minecraft:swamp", "minecraft:plains", "#minecraft:is_taiga"]),
    "containment_facility": (48, 16, 74821015, True, ["#minecraft:is_overworld"]),
    "drone_hive": (40, 14, 74821016, False, ["minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:ice_spikes", "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow"]),
    "research_outpost": (24, 8, 74821017, False, ["#minecraft:is_savanna", "minecraft:desert", "minecraft:plains", "#minecraft:is_forest", "#minecraft:is_taiga"]),
    "infected_ruins": (32, 10, 74821018, False, ["minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:dark_forest", "#minecraft:is_forest", "minecraft:plains"]),
    "hypno_tower": (44, 14, 74821019, False, ["minecraft:plains", "minecraft:desert", "#minecraft:is_savanna", "minecraft:meadow", "#minecraft:is_badlands"]),
}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=2)


def main():
    assert list(BUILDERS) == STRUCTURES
    for name, build in BUILDERS.items():
        s = build()
        nb, ne = s.save(name)
        spacing, sep, salt, under, biomes = PLACEMENT[name]
        st = {"type": "minecraft:jigsaw", "biomes": f"#{MODID}:has_structure/{name}", "spawn_overrides": {},
              "start_pool": f"{MODID}:{name}/start", "size": 1, "max_distance_from_center": 80, "use_expansion_hack": False}
        if under:
            st.update({"step": "underground_structures", "terrain_adaptation": "none",
                       "start_height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": -32}, "max_inclusive": {"absolute": 16}}})
        else:
            st.update({"step": "surface_structures", "terrain_adaptation": "beard_thin",
                       "start_height": {"absolute": 0}, "project_start_to_heightmap": "WORLD_SURFACE_WG"})
        write_json(f"{R}/worldgen/structure/{name}.json", st)
        write_json(f"{R}/worldgen/template_pool/{name}/start.json", {"fallback": "minecraft:empty", "elements": [{
            "weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"{MODID}:{name}",
                                     "projection": "rigid", "processors": "minecraft:empty"}}]})
        write_json(f"{R}/worldgen/structure_set/{name}.json", {
            "structures": [{"structure": f"{MODID}:{name}", "weight": 1}],
            "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": sep, "salt": salt}})
        write_json(f"{R}/tags/worldgen/biome/has_structure/{name}.json", {"replace": False, "values": biomes})
        print(f"{name}: {s.size} {nb} blocks, {ne} mobs")


if __name__ == "__main__":
    main()
