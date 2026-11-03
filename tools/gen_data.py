"""Lang, blockstates, block/item models, loot tables, recipes, tags."""
import os, sys, json
sys.path.insert(0, os.path.dirname(__file__))
from spec import FORMS, SCIENTISTS, BLOCKS, WEAPONS, ITEMS, SOUNDS, MODID

R = os.path.join(os.path.dirname(__file__), "..", "src/main/resources")
A, D = f"{R}/assets/{MODID}", f"{R}/data/{MODID}"


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=2)


M = lambda p: f"{MODID}:{p}"

# ---------------------------------------------------------------- blockstates + models
for (bid, name, kind, arg) in BLOCKS:
    if kind == "pillar":
        w(f"{A}/blockstates/{bid}.json", {"variants": {
            "axis=y": {"model": M(f"block/{bid}")},
            "axis=z": {"model": M(f"block/{bid}"), "x": 90},
            "axis=x": {"model": M(f"block/{bid}"), "x": 90, "y": 90}}})
        w(f"{A}/models/block/{bid}.json", {"parent": "minecraft:block/cube_column",
                                           "textures": {"end": M(f"block/{bid}_top"), "side": M(f"block/{bid}")}})
    elif kind == "facing":
        w(f"{A}/blockstates/{bid}.json", {"variants": {
            "facing=north": {"model": M(f"block/{bid}")},
            "facing=east": {"model": M(f"block/{bid}"), "y": 90},
            "facing=south": {"model": M(f"block/{bid}"), "y": 180},
            "facing=west": {"model": M(f"block/{bid}"), "y": 270}}})
        w(f"{A}/models/block/{bid}.json", {"parent": "minecraft:block/orientable", "textures": {
            "top": M("block/machine_top"), "front": M(f"block/{bid}_front"), "side": M("block/machine_side")}})
    else:
        w(f"{A}/blockstates/{bid}.json", {"variants": {"": {"model": M(f"block/{bid}")}}})
        if kind == "puddle":
            w(f"{A}/models/block/{bid}.json", {"parent": "minecraft:block/carpet", "textures": {"wool": M(f"block/{bid}")}})
        elif kind == "crystal":
            w(f"{A}/models/block/{bid}.json", {"parent": "minecraft:block/cross",
                                               "textures": {"cross": M(f"block/{bid}")}})
        else:
            w(f"{A}/models/block/{bid}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": M(f"block/{bid}")}})
    if kind == "crystal":
        w(f"{A}/models/item/{bid}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M(f"block/{bid}")}})
    else:
        w(f"{A}/models/item/{bid}.json", {"parent": M(f"block/{bid}")})
    # loot: drops itself
    w(f"{D}/loot_tables/blocks/{bid}.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": M(bid)}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

# ---------------------------------------------------------------- item models
for (iid, _) in WEAPONS:
    w(f"{A}/models/item/{iid}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": M(f"item/{iid}")}})
for (iid, _) in ITEMS:
    w(f"{A}/models/item/{iid}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M(f"item/{iid}")}})
for e in [f["id"] for f in FORMS] + [s["id"] for s in SCIENTISTS]:
    w(f"{A}/models/item/{e}_spawn_egg.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M(f"item/{e}_spawn_egg")}})

# ---------------------------------------------------------------- lang
lang = {"itemGroup.latexdrone.main": "Latex Drone Infection"}
for f in FORMS:
    lang[f"entity.{MODID}.{f['id']}"] = f["name"]
    lang[f"item.{MODID}.{f['id']}_spawn_egg"] = f"{f['name']} Spawn Egg"
for s in SCIENTISTS:
    lang[f"entity.{MODID}.{s['id']}"] = s["name"]
    lang[f"item.{MODID}.{s['id']}_spawn_egg"] = f"{s['name']} Spawn Egg"
for (bid, name, _, _) in BLOCKS:
    lang[f"block.{MODID}.{bid}"] = name
for (iid, name) in WEAPONS + ITEMS:
    lang[f"item.{MODID}.{iid}"] = name
lang.update({
    f"entity.{MODID}.syringe_dart": "Syringe Dart",
    f"entity.{MODID}.purifier_shot": "Purifier Shot",
    f"effect.{MODID}.hypnosis": "Hypnosis",
    f"effect.{MODID}.latex_infection": "Latex Infection",
    f"message.{MODID}.transforming": "The latex is taking over...",
    f"message.{MODID}.transformed": "You have become a %s. Latex creatures now see you as one of them.",
    f"message.{MODID}.cured": "The antidote burns the latex away. You are human again.",
    f"message.{MODID}.already_latex": "You are already one of them.",
    f"message.{MODID}.hypno.1": "Obey...",
    f"message.{MODID}.hypno.2": "Relax... don't fight it...",
    f"message.{MODID}.hypno.3": "Accept the latex...",
    f"message.{MODID}.hypno.4": "You want to be a drone...",
    f"message.{MODID}.hypno.5": "Look at the doctor...",
    f"message.{MODID}.hypno.6": "Sink deeper...",
    f"hud.{MODID}.infection": "Latex Infection: %s%%",
    f"hud.{MODID}.form": "Form: %s",
    f"hud.{MODID}.transforming": "TRANSFORMING",
    f"tooltip.{MODID}.latex_blade": "Coats targets in sticky latex",
    f"tooltip.{MODID}.syringe_gun": "Fires darts of latex serum and hypnotic",
    f"tooltip.{MODID}.hypno_staff": "Hypnotizes everything in front of you",
    f"tooltip.{MODID}.purifier_cannon": "Melts latex creatures, cleanses infection",
    f"tooltip.{MODID}.latex_antidote": "Cures infection, hypnosis and latex transformation",
    f"tooltip.{MODID}.injector_form": "Becomes: %s",
    f"tooltip.{MODID}.injector_random": "Inject to become a random latex creature",
    f"tooltip.{MODID}.empty_syringe": "Use on a latex creature to take a sample",
})
SUB = {"latex_ambient": "Latex creature gurgles", "latex_hurt": "Latex creature hurts", "latex_death": "Latex creature melts",
       "drone_ambient": "Drone beeps", "drone_hurt": "Drone glitches", "goo_ambient": "Goo bubbles",
       "scientist_ambient": "Scientist chuckles", "scientist_hurt": "Scientist hurts", "scientist_death": "Scientist dies",
       "syringe_inject": "Syringe injects", "syringe_shoot": "Syringe fires", "hypno_hum": "Hypnotic hum",
       "transform": "Latex transformation", "heartbeat": "Heartbeat", "purifier_shot": "Purifier fires",
       "blade_swing": "Latex blade slashes", "pod_activate": "Conversion pod activates", "infect_hit": "Latex splatters"}
for k in SOUNDS:
    lang[f"subtitles.{MODID}.{k}"] = SUB[k]
w(f"{A}/lang/en_us.json", lang)

# ---------------------------------------------------------------- entity loot
def item_entry(name, mn, mx, looting=True, chance=None):
    e = {"type": "minecraft:item", "name": name,
         "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": mn, "max": mx}}]}
    if looting:
        e["functions"].append({"function": "minecraft:looting_enchant", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}})
    pool = {"rolls": 1, "entries": [e]}
    if chance:
        pool["conditions"] = [{"condition": "minecraft:killed_by_player"},
                              {"condition": "minecraft:random_chance_with_looting", "chance": chance, "looting_multiplier": 0.02}]
    return pool


for f in FORMS:
    goo = M("white_latex_goo") if sum(f["primary"]) / 3 > 140 else M("latex_goo")
    w(f"{D}/loot_tables/entities/{f['id']}.json", {"type": "minecraft:entity", "pools": [
        item_entry(goo, 0, 2), item_entry(M("empty_syringe"), 1, 1, False, 0.04)]})
for s in SCIENTISTS:
    w(f"{D}/loot_tables/entities/{s['id']}.json", {"type": "minecraft:entity", "pools": [
        item_entry(M("syringe_dart"), 1, 3), item_entry(M("empty_syringe"), 0, 1),
        item_entry(M("latex_antidote"), 1, 1, False, 0.12), item_entry(M("hypno_crystal_shard"), 1, 1, False, 0.25),
        item_entry(M("latex_injector"), 1, 1, False, 0.05)]})

# chest loot for structures
def chest(entries, rolls):
    return {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]},
            "entries": [{"type": "minecraft:item", "name": n, "weight": wt,
                         "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": a, "max": b}}]}
                        for (n, wt, a, b) in entries]}]}


w(f"{D}/loot_tables/chests/lab_loot.json", chest([
    (M("latex_antidote"), 8, 1, 2), (M("empty_syringe"), 12, 1, 3), (M("syringe_dart"), 12, 2, 6),
    (M("latex_injector"), 4, 1, 1), (M("hypno_crystal_shard"), 6, 1, 3), (M("purifier_charge"), 8, 2, 5),
    (M("syringe_gun"), 2, 1, 1), (M("purifier_cannon"), 1, 1, 1), (M("hypno_staff"), 1, 1, 1),
    ("minecraft:iron_ingot", 10, 1, 4), ("minecraft:redstone", 10, 2, 6), ("minecraft:glass_bottle", 8, 1, 3),
    ("minecraft:paper", 8, 1, 4), ("minecraft:gold_ingot", 4, 1, 2), ("minecraft:diamond", 1, 1, 1)], (4, 8)))
w(f"{D}/loot_tables/chests/nest_loot.json", chest([
    (M("latex_goo"), 20, 2, 8), (M("white_latex_goo"), 10, 1, 5), (M("latex_injector"), 3, 1, 1),
    (M("latex_blade"), 2, 1, 1), ("minecraft:bone", 10, 1, 4), ("minecraft:rotten_flesh", 10, 1, 5),
    ("minecraft:iron_nugget", 8, 2, 8), ("minecraft:emerald", 3, 1, 2)], (3, 7)))

# ---------------------------------------------------------------- recipes
def shaped(name, pattern, key, result, count=1):
    w(f"{D}/recipes/{name}.json", {"type": "minecraft:crafting_shaped", "pattern": pattern,
                                    "key": {k: {"item": v} for k, v in key.items()}, "result": {"item": result, "count": count}})


def shapeless(name, items, result, count=1):
    w(f"{D}/recipes/{name}.json", {"type": "minecraft:crafting_shapeless",
                                    "ingredients": [{"item": i} for i in items], "result": {"item": result, "count": count}})


shaped("latex_blade", ["G", "G", "S"], {"G": M("latex_goo"), "S": "minecraft:iron_ingot"}, M("latex_blade"))
shaped("syringe_gun", ["IIE", " RI"], {"I": "minecraft:iron_ingot", "E": M("empty_syringe"), "R": "minecraft:redstone"}, M("syringe_gun"))
shaped("hypno_staff", ["  H", " S ", "S  "], {"H": M("hypno_crystal_shard"), "S": "minecraft:stick"}, M("hypno_staff"))
shaped("purifier_cannon", ["IGI", "DCD", " I "], {"I": "minecraft:iron_ingot", "G": "minecraft:glass", "D": "minecraft:diamond", "C": M("purifier_charge")}, M("purifier_cannon"))
shaped("empty_syringe", ["G", "N"], {"G": "minecraft:glass_pane", "N": "minecraft:iron_nugget"}, M("empty_syringe"), 2)
shapeless("syringe_dart", [M("empty_syringe"), "minecraft:arrow"], M("syringe_dart"), 4)
shapeless("purifier_charge", ["minecraft:glowstone_dust", "minecraft:prismarine_crystals", "minecraft:snowball"], M("purifier_charge"), 4)
shapeless("latex_antidote", ["minecraft:glass_bottle", "minecraft:glistering_melon_slice", "minecraft:sugar", M("purifier_charge")], M("latex_antidote"))
shapeless("latex_injector", [M("empty_syringe"), M("latex_goo")], M("latex_injector"))
shapeless("hypno_crystal_shard", ["minecraft:amethyst_shard", "minecraft:redstone", M("latex_goo")], M("hypno_crystal_shard"))
shaped("dark_latex_block", ["GGG", "GGG", "GGG"], {"G": M("latex_goo")}, M("dark_latex_block"))
shaped("white_latex_block", ["GGG", "GGG", "GGG"], {"G": M("white_latex_goo")}, M("white_latex_block"))
shapeless("latex_goo_from_block", [M("dark_latex_block")], M("latex_goo"), 9)
shapeless("white_latex_goo_from_block", [M("white_latex_block")], M("white_latex_goo"), 9)
for color in ("purple", "blue", "red", "green", "pink", "orange", "cyan"):
    shapeless(f"{color}_latex_block", [M("white_latex_block"), f"minecraft:{color}_dye"], M(f"{color}_latex_block"))
shapeless("gold_latex_block", [M("white_latex_block"), "minecraft:gold_ingot"], M("gold_latex_block"))
shaped("dark_latex_puddle", ["GG"], {"G": M("latex_goo")}, M("dark_latex_puddle"), 3)
shaped("white_latex_puddle", ["GG"], {"G": M("white_latex_goo")}, M("white_latex_puddle"), 3)
shaped("latex_bricks", ["SG", "GS"], {"S": "minecraft:stone_bricks", "G": M("latex_goo")}, M("latex_bricks"), 4)
shaped("lab_tile", ["QQ", "QQ"], {"Q": "minecraft:white_concrete"}, M("lab_tile"), 4)
shaped("dark_lab_tile", ["QQ", "QQ"], {"Q": "minecraft:gray_concrete"}, M("dark_lab_tile"), 4)
shaped("lab_wall", ["QI", "IQ"], {"Q": "minecraft:white_concrete", "I": "minecraft:iron_nugget"}, M("lab_wall"), 4)
shapeless("lab_wall_striped", [M("lab_wall"), "minecraft:cyan_dye"], M("lab_wall_striped"))
shaped("lab_floor", ["QQ", "QQ"], {"Q": "minecraft:light_gray_concrete"}, M("lab_floor"), 4)
shaped("lab_ceiling_light", ["III", "IGI"], {"I": "minecraft:iron_ingot", "G": "minecraft:glowstone"}, M("lab_ceiling_light"), 2)
shaped("reinforced_lab_glass", ["IGI"], {"I": "minecraft:iron_nugget", "G": "minecraft:glass"}, M("reinforced_lab_glass"), 2)
shaped("containment_tank", ["GGG", "GLG", "GGG"], {"G": M("reinforced_lab_glass"), "L": M("latex_goo")}, M("containment_tank"), 8)
shaped("steel_panel", ["II", "II"], {"I": "minecraft:iron_ingot"}, M("steel_panel"), 8)
shapeless("rusted_steel_panel", [M("steel_panel"), "minecraft:water_bucket"], M("rusted_steel_panel"))
shaped("lab_vent", ["I I", "III", "I I"], {"I": "minecraft:iron_bars"}, M("lab_vent"), 4)
shaped("metal_grate", ["B B", " B ", "B B"], {"B": "minecraft:iron_bars"}, M("metal_grate"), 4)
shapeless("hazard_block", [M("steel_panel"), "minecraft:yellow_dye", "minecraft:black_dye"], M("hazard_block"))
shaped("lab_crate", ["PIP", "P P", "PIP"], {"P": "#minecraft:planks", "I": "minecraft:iron_ingot"}, M("lab_crate"))
shaped("warning_light", ["R", "G"], {"R": "minecraft:redstone_lamp", "G": "minecraft:red_stained_glass"}, M("warning_light"))
shaped("lab_computer", ["III", "IGI", "IRI"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass_pane", "R": "minecraft:redstone"}, M("lab_computer"))
shaped("server_rack", ["IRI", "IRI", "IRI"], {"I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, M("server_rack"))
shaped("hypno_screen", ["III", "IHI", "III"], {"I": "minecraft:iron_ingot", "H": M("hypno_crystal_shard")}, M("hypno_screen"))
shaped("drone_conversion_pod", ["IGI", "GLG", "IRI"], {"I": "minecraft:iron_block", "G": M("reinforced_lab_glass"), "L": M("latex_injector"), "R": "minecraft:redstone_block"}, M("drone_conversion_pod"))
shaped("latex_glow_lamp", ["GLG"], {"G": M("latex_goo"), "L": "minecraft:glowstone"}, M("latex_glow_lamp"), 2)
# "#minecraft:planks" needs tag syntax
w(f"{D}/recipes/lab_crate.json", {"type": "minecraft:crafting_shaped", "pattern": ["PIP", "P P", "PIP"],
    "key": {"P": {"tag": "minecraft:planks"}, "I": {"item": "minecraft:iron_ingot"}}, "result": {"item": M("lab_crate"), "count": 1}})

# ---------------------------------------------------------------- tags
TAGS = f"{R}/data/minecraft/tags/blocks"
pick, axe, shovel = [], [], []
for (bid, name, kind, arg) in BLOCKS:
    if bid in ("infected_log", "infected_planks", "lab_crate"):
        axe.append(M(bid))
    elif kind in ("latex", "puddle") or bid in ("infected_dirt", "latex_glow_lamp"):
        shovel.append(M(bid))
    else:
        pick.append(M(bid))
w(f"{TAGS}/mineable/pickaxe.json", {"replace": False, "values": pick})
w(f"{TAGS}/mineable/axe.json", {"replace": False, "values": axe})
w(f"{TAGS}/mineable/shovel.json", {"replace": False, "values": shovel})
print("data done:", len(lang), "lang keys")
