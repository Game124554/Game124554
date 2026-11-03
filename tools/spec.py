"""Single source of truth for generated content (forms, scientists, blocks, items, sounds)."""

MODID = "latexdrone"

# part flags (must match PartsSpec.java)
EARS, EARS_ROUND, SNOUT, TAIL, HORNS, FIN, WINGS, LONG_EARS = 1, 2, 4, 8, 16, 32, 64, 128

def h(x):
    return tuple(int(x[i:i + 2], 16) for i in (0, 2, 4))

# id, display name, primary, secondary, glow, style, parts, scale, hp, dmg, speed, armor, infect, ability, sound, weight
_F = [
    # ---------- drones ----------
    ("dark_latex_drone", "Dark Latex Drone", "151519", "2c2c36", "33e6ff", "drone", 0, 1.0, 24, 4, 0.26, 2, 12, "resist", "drone", 6),
    ("white_latex_drone", "White Latex Drone", "e8e8ee", "b9bccb", "3f8cff", "drone", 0, 1.0, 24, 4, 0.26, 2, 12, "speed", "drone", 5),
    ("purple_latex_drone", "Purple Latex Drone", "5b2a86", "34184f", "ff5cf0", "drone", 0, 1.0, 24, 4, 0.26, 2, 12, "jump", "drone", 4),
    ("red_latex_drone", "Red Latex Drone", "8f1520", "4c0b11", "ffcf33", "drone", 0, 1.0, 26, 5, 0.26, 2, 12, "strength", "drone", 3),
    ("blue_latex_drone", "Blue Latex Drone", "1f3f9a", "122259", "7ff6ff", "drone", 0, 1.0, 24, 4, 0.26, 2, 12, "water", "drone", 4),
    ("green_latex_drone", "Green Latex Drone", "1f7a32", "0f4019", "c6ff4a", "drone", 0, 1.0, 24, 4, 0.26, 2, 12, "haste", "drone", 3),
    ("gold_latex_drone", "Gold Latex Drone", "c79a1c", "7a5b0c", "ffffff", "drone", 0, 1.0, 28, 4, 0.26, 4, 12, "resist", "drone", 2),
    ("pink_latex_drone", "Pink Latex Drone", "e05aa0", "8f2a5e", "ffffff", "drone", 0, 1.0, 22, 4, 0.28, 2, 12, "jump", "drone", 3),
    ("chrome_latex_drone", "Chrome Latex Drone", "a9b0bd", "5d6370", "ff3b3b", "drone", 0, 1.0, 30, 5, 0.25, 6, 12, "resist", "drone", 2),
    ("hypno_drone", "Hypno Drone", "1a0b24", "6a1fa8", "ff3df5", "hypno", 0, 1.0, 26, 3, 0.26, 2, 16, "none", "drone", 3),
    ("elite_drone_commander", "Elite Drone Commander", "0d0d10", "8a0f1b", "ff2a2a", "drone", HORNS, 1.2, 50, 8, 0.27, 8, 20, "strength", "drone", 1),
    ("drone_scout", "Drone Scout", "202028", "3a3a48", "7dff6a", "drone", 0, 0.8, 16, 3, 0.34, 0, 10, "speed", "drone", 4),
    ("heavy_drone", "Heavy Drone", "26262e", "4a4a55", "ffa21f", "drone", 0, 1.4, 60, 9, 0.20, 10, 18, "resist", "drone", 1),
    ("orange_latex_drone", "Orange Latex Drone", "d9661a", "7a340a", "fff27a", "drone", 0, 1.0, 24, 4, 0.26, 2, 12, "haste", "drone", 3),
    ("cyan_latex_drone", "Cyan Latex Drone", "1aa6b3", "0b5961", "ffffff", "drone", 0, 1.0, 24, 4, 0.26, 2, 12, "water", "drone", 3),
    # ---------- beasts ----------
    ("dark_latex_wolf", "Dark Latex Wolf", "141418", "34343e", "e9f2ff", "beast", EARS | SNOUT | TAIL, 1.0, 28, 5, 0.29, 2, 14, "speed", "beast", 5),
    ("white_latex_wolf", "White Latex Wolf", "ececf2", "c4c6d4", "1c1c22", "beast", EARS | SNOUT | TAIL, 1.0, 28, 5, 0.29, 2, 14, "speed", "beast", 4),
    ("dark_latex_fox", "Dark Latex Fox", "18181d", "3b3b47", "ff8a2a", "beast", EARS | SNOUT | TAIL, 0.95, 24, 4, 0.31, 1, 13, "speed", "beast", 4),
    ("white_latex_fox", "White Latex Fox", "f2f2f6", "cfd2de", "ff7a1a", "beast", EARS | SNOUT | TAIL, 0.95, 24, 4, 0.31, 1, 13, "speed", "beast", 3),
    ("black_latex_cat", "Black Latex Cat", "121216", "2a2a33", "b6ff3a", "beast", EARS | TAIL, 0.9, 22, 4, 0.31, 1, 12, "jump", "beast", 4),
    ("white_latex_cat", "White Latex Cat", "efeff4", "c9cbd8", "3ac8ff", "beast", EARS | TAIL, 0.9, 22, 4, 0.31, 1, 12, "jump", "beast", 3),
    ("latex_shark", "Latex Shark", "3f6fa8", "dfe8f2", "ffffff", "twotone", FIN | TAIL | SNOUT, 1.1, 32, 6, 0.27, 2, 14, "water", "beast", 2),
    ("latex_orca", "Latex Orca", "111114", "f4f4f6", "7fd8ff", "twotone", FIN | TAIL | SNOUT, 1.2, 38, 6, 0.26, 3, 14, "water", "beast", 2),
    ("purple_latex_dragon", "Purple Latex Dragon", "4a1f73", "8e5bc2", "ffd23d", "beast", HORNS | WINGS | TAIL | SNOUT, 1.15, 40, 7, 0.27, 4, 15, "slowfall", "beast", 2),
    ("red_latex_dragon", "Red Latex Dragon", "7d1012", "d4493a", "ffde59", "beast", HORNS | WINGS | TAIL | SNOUT, 1.15, 40, 7, 0.27, 4, 15, "strength", "beast", 2),
    ("crystal_latex_dragon", "Crystal Latex Dragon", "2a4f7a", "8fd8ff", "ffffff", "crystal", HORNS | WINGS | TAIL | SNOUT, 1.15, 42, 7, 0.27, 6, 15, "resist", "beast", 1),
    ("latex_snow_leopard", "Latex Snow Leopard", "d7dbe2", "4a4d57", "6fe0ff", "spotted", EARS | TAIL | SNOUT, 1.0, 26, 5, 0.31, 1, 13, "jump", "beast", 3),
    ("latex_tiger", "Latex Tiger", "d9741c", "1a1a1a", "fff27a", "striped", EARS | TAIL | SNOUT, 1.1, 34, 6, 0.29, 2, 14, "strength", "beast", 2),
    ("latex_deer", "Latex Deer", "7a4b26", "d9b48c", "9dff7a", "spotted", EARS | HORNS | SNOUT | TAIL, 1.0, 24, 4, 0.30, 1, 12, "jump", "beast", 3),
    ("latex_rabbit", "Latex Rabbit", "e6e2ea", "f2b6cf", "ff4fa3", "beast", LONG_EARS | TAIL, 0.8, 18, 3, 0.33, 0, 11, "jump", "beast", 4),
    ("latex_bat", "Latex Bat", "2a1f2e", "5a3d63", "ff3355", "beast", EARS | WINGS, 0.85, 20, 4, 0.30, 0, 12, "slowfall", "beast", 3),
    ("latex_lizard", "Latex Lizard", "2f7a2a", "a4d15a", "ffef4a", "spotted", TAIL | SNOUT, 1.0, 24, 4, 0.29, 3, 12, "haste", "beast", 3),
    ("latex_octo_beast", "Latex Octo Beast", "1b2f6b", "4fd1ff", "ffffff", "goo", EARS | TAIL, 1.0, 30, 5, 0.27, 2, 14, "water", "goo", 2),
    ("latex_bear", "Latex Bear", "4a2d1a", "6b4428", "ffcc33", "beast", EARS_ROUND | SNOUT, 1.3, 50, 8, 0.24, 4, 16, "strength", "beast", 2),
    ("latex_hyena", "Latex Hyena", "a8834f", "3a2a1a", "ff5a2a", "spotted", EARS | SNOUT | TAIL, 1.0, 26, 5, 0.30, 1, 13, "speed", "beast", 3),
    ("latex_raccoon", "Latex Raccoon", "6d6f78", "1c1c22", "8cff8c", "striped", EARS | SNOUT | TAIL, 0.85, 20, 3, 0.31, 0, 11, "haste", "beast", 3),
    ("latex_panther", "Latex Panther", "0b0b0e", "1d1d26", "ffd400", "beast", EARS | TAIL | SNOUT, 1.05, 30, 6, 0.32, 2, 14, "speed", "beast", 3),
    ("latex_lion", "Latex Lion", "c9952e", "7a4a12", "ff7a1a", "beast", EARS_ROUND | TAIL | SNOUT, 1.15, 38, 7, 0.28, 3, 15, "strength", "beast", 2),
    ("latex_stallion", "Latex Stallion", "3a2414", "171009", "6ac8ff", "beast", EARS | SNOUT | TAIL, 1.15, 34, 6, 0.33, 2, 13, "speed", "beast", 2),
    ("latex_crow", "Latex Crow", "0f0f14", "2a2a3a", "a07cff", "beast", WINGS | SNOUT, 0.9, 20, 4, 0.30, 0, 12, "slowfall", "beast", 3),
    ("latex_goo_blob", "Latex Goo Blob", "101014", "2a2a33", "ffffff", "goo", 0, 0.75, 16, 3, 0.22, 0, 10, "none", "goo", 5),
    ("latex_goo_crawler", "Latex Goo Crawler", "1a1a20", "3d3d4a", "d6ff3a", "goo", TAIL, 0.7, 14, 3, 0.30, 0, 10, "speed", "goo", 4),
    ("latex_goo_giant", "Latex Goo Giant", "0c0c10", "25252e", "ff2a6a", "goo", 0, 1.8, 90, 11, 0.21, 6, 25, "resist", "goo", 1),
    ("latex_pup", "Latex Pup", "18181c", "34343e", "8fe9ff", "beast", EARS | SNOUT | TAIL, 0.6, 12, 2, 0.32, 0, 8, "speed", "beast", 4),
    ("latex_kitsune", "Latex Kitsune", "f4f2f6", "f2c94c", "ffb02a", "beast", EARS | SNOUT | TAIL, 1.0, 30, 5, 0.31, 2, 14, "jump", "beast", 2),
    ("latex_jackal", "Latex Jackal", "101010", "c9a227", "33e6ff", "twotone", EARS | SNOUT | TAIL, 1.05, 30, 6, 0.30, 3, 14, "haste", "beast", 2),
    ("latex_moth", "Latex Moth", "8c8478", "e8dcc4", "fff27a", "spotted", EARS | WINGS, 0.9, 20, 3, 0.29, 0, 11, "slowfall", "beast", 3),
    ("latex_serpent", "Latex Serpent", "1f5a3a", "8ed15a", "ffef4a", "striped", TAIL | SNOUT, 1.0, 26, 5, 0.28, 2, 13, "water", "beast", 2),
    ("frost_latex_wolf", "Frost Latex Wolf", "a9d9f2", "e8f8ff", "2a7aff", "crystal", EARS | SNOUT | TAIL, 1.05, 30, 6, 0.30, 2, 14, "speed", "beast", 2),
    ("dark_latex_alpha", "Dark Latex Alpha", "0a0a0d", "c41a2a", "ff2a2a", "beast", EARS | SNOUT | TAIL | HORNS, 1.35, 80, 10, 0.30, 8, 22, "strength", "beast", 1),
]

FORMS = []
for f in _F:
    (fid, name, p, s, g, style, parts, scale, hp, dmg, spd, armor, inf, ability, sound, weight) = f
    FORMS.append(dict(id=fid, name=name, primary=h(p), secondary=h(s), glow=h(g), style=style, parts=parts,
                      scale=scale, hp=hp, dmg=dmg, speed=spd, armor=armor, infect=inf, ability=ability,
                      sound=sound, weight=weight))
assert len(FORMS) == 50, len(FORMS)
assert len({f["id"] for f in FORMS}) == 50

# Evil scientists: id, name, species, fur, fur2, eye/goggle glow, parts, weapon, ranged, hp
_S = [
    ("dr_vulpine", "Dr. Vulpine", "fox", "d9661a", "f2efe9", "7dff6a", EARS | SNOUT | TAIL, "syringe_gun", True, 30),
    ("dr_ursus", "Dr. Ursus", "bear", "5a3a22", "7a5434", "ff3b3b", EARS_ROUND | SNOUT, "latex_blade", False, 44),
    ("dr_corvax", "Dr. Corvax", "raven", "1a1a22", "3a3a4a", "c06bff", WINGS | SNOUT, "hypno_staff", True, 28),
    ("dr_lupa", "Dr. Lupa", "wolf", "6f737d", "c9ccd4", "33e6ff", EARS | SNOUT | TAIL, "syringe_gun", True, 32),
    ("dr_felina", "Dr. Felina", "cat", "efe9e1", "3a3a3a", "ff5cf0", EARS | TAIL, "latex_blade", False, 30),
    ("dr_vermin", "Dr. Vermin", "rat", "8a8a90", "e6a3b4", "b6ff3a", EARS_ROUND | SNOUT | TAIL, "syringe_gun", True, 26),
]
SCIENTISTS = [dict(id=a, name=b, species=c, fur=h(d), fur2=h(e), glow=h(f), parts=g, weapon=w, ranged=r, hp=hp)
              for (a, b, c, d, e, f, g, w, r, hp) in _S]

# Blocks: id, name, kind, texture-style args
# kinds: cube, latex, puddle, crystal, pillar, glass, facing, light
BLOCKS = [
    ("dark_latex_block", "Dark Latex Block", "latex", "151519"),
    ("white_latex_block", "White Latex Block", "latex", "e8e8ee"),
    ("purple_latex_block", "Purple Latex Block", "latex", "5b2a86"),
    ("blue_latex_block", "Blue Latex Block", "latex", "1f3f9a"),
    ("red_latex_block", "Red Latex Block", "latex", "8f1520"),
    ("green_latex_block", "Green Latex Block", "latex", "1f7a32"),
    ("pink_latex_block", "Pink Latex Block", "latex", "e05aa0"),
    ("orange_latex_block", "Orange Latex Block", "latex", "d9661a"),
    ("cyan_latex_block", "Cyan Latex Block", "latex", "1aa6b3"),
    ("gold_latex_block", "Gold Latex Block", "latex", "c79a1c"),
    ("dark_latex_puddle", "Dark Latex Puddle", "puddle", "151519"),
    ("white_latex_puddle", "White Latex Puddle", "puddle", "e8e8ee"),
    ("latex_crystal", "Dark Latex Crystal", "crystal", "1b1b24"),
    ("white_latex_crystal", "White Latex Crystal", "crystal", "dfe6f2"),
    ("infected_dirt", "Infected Dirt", "cube", "infected_dirt"),
    ("infected_stone", "Infected Stone", "cube", "infected_stone"),
    ("infected_log", "Infected Log", "pillar", "infected_log"),
    ("infected_planks", "Infected Planks", "cube", "infected_planks"),
    ("latex_bricks", "Latex Bricks", "cube", "latex_bricks"),
    ("cracked_latex_bricks", "Cracked Latex Bricks", "cube", "cracked_latex_bricks"),
    ("lab_tile", "Lab Tile", "cube", "lab_tile"),
    ("dark_lab_tile", "Dark Lab Tile", "cube", "dark_lab_tile"),
    ("lab_wall", "Lab Wall", "cube", "lab_wall"),
    ("lab_wall_striped", "Striped Lab Wall", "cube", "lab_wall_striped"),
    ("lab_floor", "Lab Floor", "cube", "lab_floor"),
    ("lab_ceiling_light", "Lab Ceiling Light", "light", "lab_ceiling_light"),
    ("reinforced_lab_glass", "Reinforced Lab Glass", "glass", "reinforced_lab_glass"),
    ("containment_tank", "Containment Tank", "glass", "containment_tank"),
    ("lab_vent", "Lab Vent", "cube", "lab_vent"),
    ("hazard_block", "Hazard Block", "cube", "hazard_block"),
    ("steel_panel", "Steel Panel", "cube", "steel_panel"),
    ("rusted_steel_panel", "Rusted Steel Panel", "cube", "rusted_steel_panel"),
    ("metal_grate", "Metal Grate", "glass", "metal_grate"),
    ("lab_crate", "Lab Crate", "cube", "lab_crate"),
    ("warning_light", "Warning Light", "light", "warning_light"),
    ("lab_computer", "Lab Computer", "facing", "lab_computer"),
    ("server_rack", "Server Rack", "facing", "server_rack"),
    ("hypno_screen", "Hypno Screen", "facing", "hypno_screen"),
    ("drone_conversion_pod", "Drone Conversion Pod", "facing", "drone_conversion_pod"),
    ("latex_glow_lamp", "Latex Glow Lamp", "light", "latex_glow_lamp"),
]
assert len(BLOCKS) == 40

WEAPONS = [("latex_blade", "Latex Blade"), ("syringe_gun", "Syringe Gun"),
           ("hypno_staff", "Hypno Staff"), ("purifier_cannon", "Purifier Cannon")]
ITEMS = [("latex_goo", "Dark Latex Goo"), ("white_latex_goo", "White Latex Goo"),
         ("latex_antidote", "Latex Antidote"), ("latex_injector", "Latex Injector"),
         ("syringe_dart", "Syringe Dart"), ("purifier_charge", "Purifier Charge"),
         ("empty_syringe", "Empty Syringe"), ("hypno_crystal_shard", "Hypno Crystal Shard")]

SOUNDS = ["latex_ambient", "latex_hurt", "latex_death", "drone_ambient", "drone_hurt", "goo_ambient",
          "scientist_ambient", "scientist_hurt", "scientist_death", "syringe_inject", "syringe_shoot",
          "hypno_hum", "transform", "heartbeat", "purifier_shot", "blade_swing", "pod_activate", "infect_hit"]

STRUCTURES = ["abandoned_lab", "latex_nest", "containment_facility", "drone_hive",
              "research_outpost", "infected_ruins", "hypno_tower"]
