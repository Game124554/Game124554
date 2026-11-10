"""Writes blockstates, models, lang, loot tables, recipes, tags, ore worldgen and the
generated Java tables (MobDefs.java, ModBlocks.java).  Run:  python3 tools/gen_assets.py
"""
import json
import os

import content as C

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", C.MODID)
DATA = os.path.join(RES, "data")
JAVA = os.path.join(ROOT, "src", "main", "java", "net", "latexdrones")
M = C.MODID
COUNT = {"json": 0}


def wj(obj, *path):
    p = os.path.join(*path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w") as f:
        json.dump(obj, f, indent=2)
    COUNT["json"] += 1


def bt(n):
    return f"{M}:block/{n}"


SPECIAL_TEX = {
    "infected_grass": dict(top="infected_grass_top", side="infected_grass_side", bottom="infected_dirt"),
    "lab_table": dict(top="lab_table_top", side="lab_table_side", bottom="lab_ceiling"),
    "conversion_chamber": dict(top="conversion_chamber_top", side="conversion_chamber_side", bottom="lab_vent"),
    "drone_charging_pad": dict(top="drone_charging_pad_top", side="drone_charging_pad_side", bottom="lab_vent"),
    "latex_gas_vent": dict(top="latex_gas_vent_top", side="latex_gas_vent_side", bottom="lab_vent"),
    "decontamination_block": dict(top="decontamination_block_top", side="decontamination_block_side", bottom="lab_vent"),
    "infected_log": dict(end="infected_log_top", side="infected_log"),
    "lab_pillar": dict(end="lab_pillar_top", side="lab_pillar"),
    "containment_tank": dict(end="containment_tank_top", side="containment_tank"),
}
USED_TEX = set()


def tx(n):
    USED_TEX.add(n)
    return bt(n)


# ------------------------------------------------------------------ blockstates + models
def stairs_state(name):
    yaw = {"east": 0, "south": 90, "west": 180, "north": 270}
    variants = {}
    for half in ("bottom", "top"):
        for shape in ("straight", "outer_right", "outer_left", "inner_right", "inner_left"):
            for facing, y0 in yaw.items():
                model = name if shape == "straight" else (name + ("_outer" if shape.startswith("outer") else "_inner"))
                if half == "bottom":
                    y = y0 if (shape == "straight" or shape.endswith("right")) else (y0 - 90) % 360
                else:
                    y = (y0 + 90) % 360 if shape.endswith("right") else y0
                v = {"model": bt(model)}
                if half == "top":
                    v["x"] = 180
                if y:
                    v["y"] = y
                if "x" in v or "y" in v:
                    v["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = v
    return {"variants": variants}


def gen_blocks():
    bs = os.path.join(ASSETS, "blockstates")
    bm = os.path.join(ASSETS, "models", "block")
    im = os.path.join(ASSETS, "models", "item")
    for b in C.BLOCKS:
        n, model = b["name"], b["model"]
        item_model = {"parent": bt(n)}
        if model == "cube":
            wj({"parent": "minecraft:block/cube_all", "textures": {"all": tx(n)}}, bm, n + ".json")
            state = {"variants": {"": {"model": bt(n)}}}
        elif model == "bottom_top":
            t = SPECIAL_TEX[n]
            wj({"parent": "minecraft:block/cube_bottom_top",
                "textures": {"top": tx(t["top"]), "bottom": tx(t["bottom"]), "side": tx(t["side"])}}, bm, n + ".json")
            state = {"variants": {"": {"model": bt(n)}}}
        elif model == "column":
            t = SPECIAL_TEX[n]
            wj({"parent": "minecraft:block/cube_column", "textures": {"end": tx(t["end"]), "side": tx(t["side"])}}, bm, n + ".json")
            state = {"variants": {"": {"model": bt(n)}}}
        elif model == "pillar":
            t = SPECIAL_TEX[n]
            tex = {"end": tx(t["end"]), "side": tx(t["side"])}
            wj({"parent": "minecraft:block/cube_column", "textures": tex}, bm, n + ".json")
            wj({"parent": "minecraft:block/cube_column_horizontal", "textures": tex}, bm, n + "_horizontal.json")
            state = {"variants": {
                "axis=y": {"model": bt(n)},
                "axis=z": {"model": bt(n + "_horizontal"), "x": 90},
                "axis=x": {"model": bt(n + "_horizontal"), "x": 90, "y": 90}}}
        elif model == "orientable":
            wj({"parent": "minecraft:block/orientable",
                "textures": {"top": tx(n + "_top"), "front": tx(n + "_front"), "side": tx(n + "_side")}}, bm, n + ".json")
            state = {"variants": {
                "facing=north": {"model": bt(n)}, "facing=east": {"model": bt(n), "y": 90},
                "facing=south": {"model": bt(n), "y": 180}, "facing=west": {"model": bt(n), "y": 270}}}
        elif model == "cross":
            wj({"parent": "minecraft:block/cross", "textures": {"cross": tx(n)}}, bm, n + ".json")
            state = {"variants": {"": {"model": bt(n)}}}
            item_model = {"parent": "minecraft:item/generated", "textures": {"layer0": bt(n)}}
        elif model == "carpet":
            wj({"parent": "minecraft:block/carpet", "textures": {"wool": tx(n)}}, bm, n + ".json")
            state = {"variants": {"": {"model": bt(n)}}}
        elif model == "goo":
            def cube(a, z, cull):
                faces = {}
                for d in ("down", "up", "north", "south", "west", "east"):
                    faces[d] = {"uv": [a, a, z, z], "texture": "#texture"}
                    if cull:
                        faces[d]["cullface"] = d
                return {"from": [a, a, a], "to": [z, z, z], "faces": faces}
            wj({"parent": "minecraft:block/block", "textures": {"particle": tx(n), "texture": tx(n)},
                "elements": [cube(3, 13, False), cube(0, 16, True)]}, bm, n + ".json")
            state = {"variants": {"": {"model": bt(n)}}}
        elif model == "slab":
            base = b["base"]
            tex = {"bottom": tx(base), "top": tx(base), "side": tx(base)}
            wj({"parent": "minecraft:block/slab", "textures": tex}, bm, n + ".json")
            wj({"parent": "minecraft:block/slab_top", "textures": tex}, bm, n + "_top.json")
            state = {"variants": {"type=bottom": {"model": bt(n)}, "type=top": {"model": bt(n + "_top")},
                                  "type=double": {"model": bt(base)}}}
        elif model == "stairs":
            base = b["base"]
            tex = {"bottom": tx(base), "top": tx(base), "side": tx(base)}
            wj({"parent": "minecraft:block/stairs", "textures": tex}, bm, n + ".json")
            wj({"parent": "minecraft:block/inner_stairs", "textures": tex}, bm, n + "_inner.json")
            wj({"parent": "minecraft:block/outer_stairs", "textures": tex}, bm, n + "_outer.json")
            state = stairs_state(n)
        else:
            raise ValueError(model)
        wj(state, bs, n + ".json")
        wj(item_model, im, n + ".json")
    # every referenced texture must exist
    for t in sorted(USED_TEX):
        assert os.path.exists(os.path.join(ASSETS, "textures", "block", t + ".png")), "missing texture " + t


def gen_items():
    im = os.path.join(ASSETS, "models", "item")
    for n, _t in C.WEAPONS + C.MISC_ITEMS:
        parent = "minecraft:item/handheld" if n in C.HANDHELD else "minecraft:item/generated"
        assert os.path.exists(os.path.join(ASSETS, "textures", "item", n + ".png")), n
        wj({"parent": parent, "textures": {"layer0": f"{M}:item/{n}"}}, im, n + ".json")
    for m in C.MOBS:
        wj({"parent": "minecraft:item/generated", "textures": {"layer0": f"{M}:item/egg/{m['name']}"}},
           im, m["name"] + "_spawn_egg.json")


# ------------------------------------------------------------------ lang
TIPS = {
    "stun_baton": "Stuns whatever it hits",
    "tesla_prod": "Arcs lightning to nearby enemies",
    "bone_saw": "Fast, leaves wounds that keep hurting",
    "latex_blade": "Infects on hit - anything it kills rises as a drone",
    "syringe_gun": "Fires Latex Darts or Antidote Darts (off-hand first)",
    "tranq_rifle": "Fires Tranquilizer Darts - long range sedation",
    "goo_launcher": "Lobs Goo Canisters that splash infection",
    "purifier_flamethrower": "Burns latex. Drones hate it",
    "hypno_ray": "Hypnotises the creature you aim at",
    "latex_syringe": "Use: inject yourself (+35% infection). Use on a mob: convert it",
    "antidote_syringe": "Use: -50% infection and a minute of immunity",
    "cure_serum": "Use: turns a transformed player human again",
    "latex_dart": "Ammo for the Syringe Gun",
    "tranq_dart": "Ammo for the Tranquilizer Rifle",
    "antidote_dart": "Syringe Gun ammo - cures players, melts drones",
    "goo_canister": "Ammo for the Goo Launcher",
    "raw_latex": "Still twitching",
    "drone_core": "The control unit of a drone",
}


def gen_lang():
    L = {}
    for b in C.BLOCKS:
        L[f"block.{M}.{b['name']}"] = b["title"]
    for n, t in C.WEAPONS + C.MISC_ITEMS:
        L[f"item.{M}.{n}"] = t
        L[f"item.{M}.{n}.tip"] = TIPS[n]
    for m in C.MOBS:
        L[f"entity.{M}.{m['name']}"] = m["title"]
        L[f"item.{M}.{m['name']}_spawn_egg"] = m["title"] + " Spawn Egg"
        L[f"form.{M}.{m['name']}"] = m["title"]
    L[f"entity.{M}.dart"] = "Dart"
    L[f"effect.{M}.latex_infection"] = "Latex Infection"
    L[f"effect.{M}.hypnosis"] = "Hypnosis"
    L[f"effect.{M}.inoculated"] = "Inoculated"
    L[f"itemGroup.{M}.blocks"] = "Latex Drones: Blocks"
    L[f"itemGroup.{M}.items"] = "Latex Drones: Weapons & Items"
    L[f"itemGroup.{M}.mobs"] = "Latex Drones: Spawn Eggs"
    for ev, sub, _v in C.SOUNDS:
        L[f"subtitles.{M}.{ev}"] = sub
    L.update({
        f"message.{M}.infected": "You have been infected with latex!",
        f"message.{M}.stage1": "Your skin feels strangely tight...",
        f"message.{M}.stage2": "Glossy black patches are spreading over you...",
        f"message.{M}.stage3": "You can barely think... the hive is calling.",
        f"message.{M}.transform_start": "THE LATEX IS TAKING OVER!",
        f"message.{M}.transformed": "Assimilation complete. You are now: %s",
        f"message.{M}.cured": "The latex melts away. You are human again.",
        f"message.{M}.antidote": "The antidote burns the infection back.",
        f"message.{M}.antidote_late": "Too late for an antidote - only a Cure Serum can help now.",
        f"message.{M}.not_infected": "You are not infected.",
        f"message.{M}.no_ammo": "Out of ammo!",
        f"message.{M}.hypnotized": "You can't look away...",
        f"message.{M}.assimilated_death": "The drones did not let you go. You wake up as one of them.",
        f"message.{M}.chamber": "The chamber seals around you...",
        f"message.{M}.alarm": "INTRUDER ALERT!",
        f"hud.{M}.infection": "LATEX INFECTION",
        f"hud.{M}.unit": "UNIT #%s",
        f"commands.{M}.infect": "Set infection of %s to %s%%",
        f"commands.{M}.transform": "Transformed %s into %s",
        f"commands.{M}.cure": "Cured %s",
        f"commands.{M}.unknown_form": "Unknown form: %s",
    })
    with open(os.path.join(ROOT, "tools", "lang_extra.json")) as f:
        L.update(json.load(f))   # drone orders / standby texts
    wj(L, ASSETS, "lang", "en_us.json")


# ------------------------------------------------------------------ loot tables
def item_entry(name, lo=None, hi=None, weight=None, extra=None):
    e = {"type": "minecraft:item", "name": name}
    fns = []
    if lo is not None:
        fns.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
    if extra:
        fns += extra
    if fns:
        e["functions"] = fns
    if weight is not None:
        e["weight"] = weight
    return e


def gen_loot():
    lt = os.path.join(DATA, M, "loot_tables")
    for b in C.BLOCKS:
        if b["drop"]:
            item, lo, hi = b["drop"]
            entry = item_entry(f"{M}:{item}", lo, hi, extra=[{"function": "minecraft:explosion_decay"}]) if hi > lo else item_entry(f"{M}:{item}")
            pools = [{"rolls": 1, "entries": [entry]}]
            if b["name"] == "latex_core":
                pools.append({"rolls": 1, "entries": [item_entry(f"{M}:raw_latex", 2, 5)]})
        else:
            pools = [{"rolls": 1, "entries": [item_entry(f"{M}:{b['name']}")],
                      "conditions": [{"condition": "minecraft:survives_explosion"}]}]
        wj({"type": "minecraft:block", "pools": pools}, lt, "blocks", b["name"] + ".json")

    looting = {"function": "minecraft:looting_enchant", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}
    for m in C.MOBS:
        big = m["role"] in ("elite", "boss", "giant", "beast_big", "blob_big", "hover_big")
        pools = [{"rolls": 1, "entries": [item_entry(f"{M}:raw_latex", 1 if big else 0, 4 if big else 2, extra=[looting])]}]
        core = {"rolls": 1, "entries": [item_entry(f"{M}:drone_core")]}
        if not big:
            core["conditions"] = [{"condition": "minecraft:killed_by_player"},
                                  {"condition": "minecraft:random_chance_with_looting", "chance": 0.08, "looting_multiplier": 0.02}]
        pools.append(core)
        if m["scientist"]:
            pools.append({"rolls": 1, "entries": [
                item_entry(f"{M}:antidote_syringe", 1, 2, weight=3), item_entry(f"{M}:latex_dart", 1, 4, weight=3),
                item_entry(f"{M}:tranq_dart", 1, 4, weight=2), item_entry(f"{M}:cure_serum", weight=1),
                item_entry("minecraft:emerald", 1, 3, weight=2)]})
        if m["name"] == "latex_hive_queen":
            pools.append({"rolls": 1, "entries": [item_entry(f"{M}:cure_serum", 2, 3)]})
            pools.append({"rolls": 1, "entries": [item_entry(f"{M}:latex_core")]})
        wj({"type": "minecraft:entity", "pools": pools}, lt, "entities", m["name"] + ".json")

    def chest(name, rolls, entries):
        wj({"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]},
                                                    "entries": entries}]}, lt, "chests", name + ".json")
    chest("lab_common", (3, 6), [
        item_entry(f"{M}:raw_latex", 1, 4, 10), item_entry(f"{M}:latex_dart", 2, 6, 8), item_entry(f"{M}:tranq_dart", 2, 5, 6),
        item_entry(f"{M}:antidote_syringe", 1, 2, 7), item_entry(f"{M}:goo_canister", 1, 4, 5), item_entry(f"{M}:latex_syringe", 1, 1, 4),
        item_entry("minecraft:iron_ingot", 1, 4, 8), item_entry("minecraft:redstone", 2, 6, 6), item_entry("minecraft:glass_bottle", 1, 3, 6),
        item_entry("minecraft:bread", 1, 3, 6), item_entry("minecraft:paper", 1, 5, 5), item_entry(f"{M}:lab_light", 1, 2, 3)])
    chest("lab_rare", (4, 7), [
        item_entry(f"{M}:cure_serum", 1, 1, 5), item_entry(f"{M}:antidote_syringe", 1, 3, 8), item_entry(f"{M}:antidote_dart", 2, 6, 6),
        item_entry(f"{M}:drone_core", 1, 2, 6), item_entry(f"{M}:stun_baton", weight=3), item_entry(f"{M}:syringe_gun", weight=3),
        item_entry(f"{M}:tranq_rifle", weight=2), item_entry(f"{M}:tesla_prod", weight=2), item_entry(f"{M}:hypno_ray", weight=1),
        item_entry(f"{M}:purifier_flamethrower", weight=2), item_entry(f"{M}:goo_launcher", weight=2), item_entry(f"{M}:bone_saw", weight=3),
        item_entry("minecraft:diamond", 1, 2, 3), item_entry("minecraft:golden_apple", 1, 1, 3), item_entry("minecraft:iron_ingot", 2, 6, 8)])
    chest("hive", (3, 6), [
        item_entry(f"{M}:raw_latex", 2, 8, 12), item_entry(f"{M}:drone_core", 1, 2, 5), item_entry(f"{M}:latex_blade", weight=2),
        item_entry(f"{M}:black_latex_goo", 1, 4, 6), item_entry(f"{M}:latex_crystal_black", 1, 3, 5), item_entry(f"{M}:latex_syringe", 1, 2, 5),
        item_entry("minecraft:slime_ball", 1, 5, 6), item_entry("minecraft:bone", 1, 4, 5), item_entry("minecraft:ender_pearl", 1, 2, 2)])


# ------------------------------------------------------------------ recipes
def gen_recipes():
    rd = os.path.join(DATA, M, "recipes")
    mc = lambda n: "minecraft:" + n
    md = lambda n: f"{M}:{n}"

    def res(item, count):
        r = {"item": item}
        if count != 1:
            r["count"] = count
        return r

    def shaped(name, pattern, key, result, count=1, cat="misc"):
        wj({"type": "minecraft:crafting_shaped", "category": cat, "pattern": pattern,
            "key": {k: {"item": v} for k, v in key.items()}, "result": res(result, count)}, rd, name + ".json")

    def shapeless(name, ings, result, count=1, cat="misc"):
        wj({"type": "minecraft:crafting_shapeless", "category": cat, "ingredients": [{"item": i} for i in ings],
            "result": res(result, count)}, rd, name + ".json")

    def cut(src, result, count=1):
        wj({"type": "minecraft:stonecutting", "ingredient": {"item": md(src)}, "result": md(result), "count": count},
           rd, f"{result}_from_{src}_stonecutting.json")

    B = "building"
    shaped("raw_latex_block", ["###", "###", "###"], {"#": md("raw_latex")}, md("raw_latex_block"), 1, B)
    shapeless("raw_latex_from_block", [md("raw_latex_block")], md("raw_latex"), 9)
    shaped("black_latex_block", ["##", "##"], {"#": md("raw_latex")}, md("black_latex_block"), 2, B)
    for cid, _n, _mc, _h in C.LATEX_COLORS[1:]:
        shapeless(f"{cid}_latex_block", [md("black_latex_block"), mc(f"{cid}_dye")], md(f"{cid}_latex_block"), 1, B)
    for c in ("black", "white"):
        shapeless(f"{c}_latex_goo", [md(f"{c}_latex_block"), mc("slime_ball")], md(f"{c}_latex_goo"), 2, B)
        shaped(f"{c}_latex_puddle", ["##"], {"#": md(f"{c}_latex_goo")}, md(f"{c}_latex_puddle"), 3, B)
    shaped("lab_wall", ["SSS", "SIS", "SSS"], {"S": mc("smooth_stone"), "I": mc("iron_ingot")}, md("lab_wall"), 8, B)
    for r in ("lab_wall_striped", "lab_tile", "lab_tile_dark", "lab_tile_checker", "lab_ceiling", "lab_pillar",
              "lab_vent", "lab_hazard_block", "lab_wall_stairs"):
        cut("lab_wall", r)
    cut("lab_wall", "lab_wall_slab", 2)
    cut("lab_tile", "lab_tile_slab", 2)
    cut("lab_tile", "lab_tile_stairs")
    shapeless("lab_light", [md("lab_wall"), mc("glowstone_dust")], md("lab_light"), 1, B)
    shapeless("lab_emergency_light", [md("lab_light"), mc("red_dye")], md("lab_emergency_light"), 1, B)
    shaped("lab_glass", ["GGG", "GIG", "GGG"], {"G": mc("glass"), "I": mc("iron_nugget")}, md("lab_glass"), 8, B)
    shapeless("lab_reinforced_glass", [md("lab_glass"), md("lab_glass"), mc("iron_bars")], md("lab_reinforced_glass"), 2, B)
    shaped("lab_computer", ["III", "GRG", "III"], {"I": mc("iron_ingot"), "G": mc("glass_pane"), "R": mc("redstone")}, md("lab_computer"))
    shaped("lab_server_rack", ["IRI", "IRI", "IRI"], {"I": mc("iron_ingot"), "R": mc("redstone")}, md("lab_server_rack"))
    shaped("lab_crate", ["NNN", "NCN", "NNN"], {"N": mc("iron_nugget"), "C": mc("chest")}, md("lab_crate"), 2)
    shaped("lab_shelf", ["WWW", "BBB", "WWW"], {"W": md("lab_wall"), "B": mc("glass_bottle")}, md("lab_shelf"), 2)
    shaped("lab_table", ["III", "W W", "W W"], {"I": mc("iron_ingot"), "W": md("lab_wall")}, md("lab_table"), 2)
    shaped("containment_tank", ["IGI", "GLG", "IGI"], {"I": mc("iron_ingot"), "G": md("lab_glass"), "L": md("raw_latex_block")}, md("containment_tank"))
    shaped("conversion_chamber", ["IDI", "GCG", "ILI"], {"I": mc("iron_ingot"), "D": mc("diamond"), "G": md("lab_reinforced_glass"),
                                                         "C": md("drone_core"), "L": md("raw_latex_block")}, md("conversion_chamber"))
    shaped("drone_charging_pad", ["RCR", "III"], {"R": mc("redstone"), "C": md("drone_core"), "I": mc("iron_ingot")}, md("drone_charging_pad"), 2)
    shaped("hypno_screen", ["IGI", "GEG", "IRI"], {"I": mc("iron_ingot"), "G": md("lab_glass"), "E": mc("ender_eye"), "R": mc("redstone")}, md("hypno_screen"))
    shaped("latex_gas_vent", ["IVI", "ILI", "III"], {"I": mc("iron_ingot"), "V": md("lab_vent"), "L": md("raw_latex_block")}, md("latex_gas_vent"))
    shaped("decontamination_block", ["IVI", "IAI", "III"], {"I": mc("iron_ingot"), "V": md("lab_vent"), "A": md("antidote_syringe")}, md("decontamination_block"))
    shaped("alarm_block", ["IRI", "RNR", "IRI"], {"I": mc("iron_ingot"), "R": mc("redstone"), "N": mc("note_block")}, md("alarm_block"))
    E = "equipment"
    shaped("stun_baton", ["  R", " I ", "S  "], {"R": mc("redstone_block"), "I": mc("iron_ingot"), "S": mc("stick")}, md("stun_baton"), 1, E)
    shaped("tesla_prod", ["  L", " I ", "S  "], {"L": mc("lightning_rod"), "I": mc("iron_ingot"), "S": mc("stick")}, md("tesla_prod"), 1, E)
    shaped("bone_saw", ["I", "I", "B"], {"I": mc("iron_ingot"), "B": mc("bone")}, md("bone_saw"), 1, E)
    shaped("latex_blade", ["L", "L", "S"], {"L": md("raw_latex_block"), "S": mc("stick")}, md("latex_blade"), 1, E)
    shaped("syringe_gun", ["IIG", " S "], {"I": mc("iron_ingot"), "G": mc("glass"), "S": mc("stick")}, md("syringe_gun"), 1, E)
    shaped("tranq_rifle", ["III", "PS "], {"I": mc("iron_ingot"), "P": mc("glass_pane"), "S": mc("stick")}, md("tranq_rifle"), 1, E)
    shaped("goo_launcher", ["III", "LBI"], {"I": mc("iron_ingot"), "L": md("raw_latex_block"), "B": mc("bucket")}, md("goo_launcher"), 1, E)
    shaped("purifier_flamethrower", ["IIF", "BS "], {"I": mc("iron_ingot"), "F": mc("flint_and_steel"), "B": mc("blaze_powder"), "S": mc("stick")}, md("purifier_flamethrower"), 1, E)
    shaped("hypno_ray", ["IAE", " S "], {"I": mc("iron_ingot"), "A": mc("amethyst_shard"), "E": mc("ender_eye"), "S": mc("stick")}, md("hypno_ray"), 1, E)
    shaped("latex_dart", ["N", "L", "F"], {"N": mc("iron_nugget"), "L": md("raw_latex"), "F": mc("feather")}, md("latex_dart"), 4, E)
    shaped("tranq_dart", ["N", "E", "F"], {"N": mc("iron_nugget"), "E": mc("fermented_spider_eye"), "F": mc("feather")}, md("tranq_dart"), 4, E)
    shaped("antidote_dart", ["N", "A", "F"], {"N": mc("iron_nugget"), "A": md("antidote_syringe"), "F": mc("feather")}, md("antidote_dart"), 4, E)
    shapeless("goo_canister", [md("raw_latex"), md("raw_latex"), mc("slime_ball"), mc("iron_nugget")], md("goo_canister"), 4, E)
    shapeless("latex_syringe", [mc("glass_bottle"), md("raw_latex"), mc("iron_nugget")], md("latex_syringe"))
    shapeless("antidote_syringe", [mc("glass_bottle"), mc("honey_bottle"), mc("glistering_melon_slice"), mc("iron_nugget")], md("antidote_syringe"), 2)
    shapeless("cure_serum", [md("antidote_syringe"), mc("golden_apple"), md("drone_core")], md("cure_serum"))


# ------------------------------------------------------------------ tags + ore worldgen
def gen_tags_worldgen():
    for tool in ("pickaxe", "shovel", "axe"):
        vals = [f"{M}:{b['name']}" for b in C.BLOCKS if b["tool"] == tool]
        wj({"replace": False, "values": vals}, DATA, "minecraft", "tags", "blocks", "mineable", tool + ".json")
    for kind, tag in (("slab", "slabs"), ("stairs", "stairs")):
        vals = [f"{M}:{b['name']}" for b in C.BLOCKS if b["model"] == kind]
        wj({"replace": False, "values": vals}, DATA, "minecraft", "tags", "blocks", tag + ".json")
        wj({"replace": False, "values": vals}, DATA, "minecraft", "tags", "items", tag + ".json")
    wj({"replace": False, "values": ["minecraft:" + b for b in C.LAND_BIOMES]},
       DATA, M, "tags", "worldgen", "biome", "has_structure", "land.json")
    wj({"replace": False, "values": ["#minecraft:is_overworld"]},
       DATA, M, "tags", "worldgen", "biome", "has_structure", "underground.json")
    wj({"type": "minecraft:ore", "config": {"discard_chance_on_air_exposure": 0.0, "size": 8, "targets": [
        {"state": {"Name": f"{M}:latex_ore"}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}},
        {"state": {"Name": f"{M}:deepslate_latex_ore"}, "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}}]}},
       DATA, M, "worldgen", "configured_feature", "latex_ore.json")
    wj({"feature": f"{M}:latex_ore", "placement": [
        {"type": "minecraft:count", "count": 9}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": -48}, "max_inclusive": {"absolute": 72}}},
        {"type": "minecraft:biome"}]},
       DATA, M, "worldgen", "placed_feature", "latex_ore.json")


# ------------------------------------------------------------------ generated Java
def jbool(v):
    return "true" if v else "false"


def gen_java():
    lines = []
    for m in C.MOBS:
        lines.append(
            '        new MobDef("{name}", {arch}, {w}F, {h}F, {sc}F, {hp}, {dmg}, {spd}, {armor}, {inf}, 0x{flags:04X}, '
            '{ranged}, {leaper}, {sci}, {fire}, {hypno}, "{held}", {weight}, {grp}, {dart}, 0x{c:06X}, 0x{g:06X})'.format(
                name=m["name"], arch=m["arch"], w=m["w"], h=m["h"], sc=m["sc"], hp=float(m["hp"]), dmg=float(m["dmg"]),
                spd=m["spd"], armor=float(m["armor"]), inf=m["inf"], flags=m["flags"], ranged=jbool(m["ranged"]),
                leaper=jbool(m["leaper"]), sci=jbool(m["scientist"]), fire=jbool(m["fire"]), hypno=jbool(m["hypno"]),
                held=m["held"], weight=m["weight"], grp=m["grp"], dart=m["dart"], c=m["pal"][0], g=m["pal"][2]))
    src = """package net.latexdrones.content;

import java.util.List;

import static net.latexdrones.content.Archetype.BEAST;
import static net.latexdrones.content.Archetype.BLOB;
import static net.latexdrones.content.Archetype.HOVER;
import static net.latexdrones.content.Archetype.HUMANOID;

/** GENERATED by tools/gen_assets.py from tools/content.py - edit the table there, not here. */
public final class MobDefs {
    private MobDefs() {
    }

    /** All %d mobs of the mod (6 scientists first). */
    public static final List<MobDef> ALL = List.of(
%s
    );
}
""" % (len(C.MOBS), ",\n".join(lines))
    p = os.path.join(JAVA, "content", "MobDefs.java")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w") as f:
        f.write(src)

    layer = {None: 0, "cutout": 1, "translucent": 2}
    blines = []
    for b in C.BLOCKS:
        blines.append('    public static final Block %s = register("%s", %s, %d);' % (
            b["name"].upper(), b["name"], b["java"], layer[b["layer"]]))
    with open(os.path.join(ROOT, "tools", "ModBlocks.java.template")) as f:
        tpl = f.read()
    src = tpl.replace("/*BLOCKS*/", "\n".join(blines)).replace("/*COUNT*/", str(len(C.BLOCKS)))
    p = os.path.join(JAVA, "registry", "ModBlocks.java")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w") as f:
        f.write(src)


def main():
    gen_blocks()
    gen_items()
    gen_lang()
    gen_loot()
    gen_recipes()
    gen_tags_worldgen()
    gen_java()
    print("json files written:", COUNT["json"])


if __name__ == "__main__":
    main()
