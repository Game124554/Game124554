"""Single source of truth for the mod's content.

Everything else (textures, JSON assets, structures, the generated Java tables)
is produced from the tables in this file by the other scripts in tools/.
"""

MODID = "latexdrones"

# ---------------------------------------------------------------- model extras
(EARS_POINTY, EARS_LONG, EARS_ROUND, HORNS, SNOUT, BEAK, VISOR, ANTENNA, FIN,
 TAIL_BUSHY, TAIL_THIN, TAIL_SHORT, TAIL_THICK, WINGS) = [1 << i for i in range(14)]

FLAG_NAMES = ["EARS_POINTY", "EARS_LONG", "EARS_ROUND", "HORNS", "SNOUT", "BEAK", "VISOR",
              "ANTENNA", "FIN", "TAIL_BUSHY", "TAIL_THIN", "TAIL_SHORT", "TAIL_THICK", "WINGS"]

# dart kinds (ranged attacks)
DART_NONE, DART_LATEX, DART_TRANQ, DART_GOO, DART_HYPNO = 0, 1, 2, 3, 4

# ---------------------------------------------------------------- colours
K = 0x16161A; W = 0xE8E8EE; G = 0x6B6F78; DG = 0x3A3D45; R = 0xC0262D; B = 0x2456C8
GR = 0x2E9E4F; P = 0x7A35C9; O = 0xE0762A; Y = 0xE8C42E; PK = 0xF08AB8; C = 0x2EC4C9
GOLD = 0xD9A520; NAVY = 0x1C2A5C; TEAL = 0x1F8F8A; CRIM = 0x8C1220; BR = 0x7A5232
CREAM = 0xEADFC8

# ---------------------------------------------------------------- mob roles
# hp, dmg, spd, armor, infect (0..1000 per hit), width, height, render scale, spawn weight, max group
ROLES = {
    "basic":     dict(hp=20, dmg=3, spd=0.25, armor=0, inf=80,  w=0.6, h=1.95, sc=1.0, weight=2, grp=3),
    "fast":      dict(hp=16, dmg=3, spd=0.33, armor=0, inf=70,  w=0.6, h=1.95, sc=1.0, weight=2, grp=2),
    "soldier":   dict(hp=26, dmg=5, spd=0.26, armor=4, inf=90,  w=0.6, h=1.95, sc=1.0, weight=1, grp=2),
    "heavy":     dict(hp=44, dmg=7, spd=0.21, armor=8, inf=110, w=0.8, h=2.35, sc=1.2, weight=1, grp=1),
    "ranged":    dict(hp=18, dmg=2, spd=0.25, armor=0, inf=60,  w=0.6, h=1.95, sc=1.0, weight=1, grp=1, ranged=True),
    "leaper":    dict(hp=18, dmg=4, spd=0.31, armor=0, inf=90,  w=0.6, h=1.95, sc=1.0, weight=1, grp=2, leaper=True),
    "elite":     dict(hp=60, dmg=8, spd=0.27, armor=8, inf=150, w=0.7, h=2.2,  sc=1.13, weight=1, grp=1),
    "boss":      dict(hp=150, dmg=11, spd=0.26, armor=12, inf=250, w=0.9, h=2.9, sc=1.5, weight=0, grp=1),
    "giant":     dict(hp=90, dmg=10, spd=0.19, armor=10, inf=120, w=1.0, h=3.1, sc=1.6, weight=1, grp=1),
    "small":     dict(hp=10, dmg=2, spd=0.34, armor=0, inf=60,  w=0.45, h=1.25, sc=0.65, weight=1, grp=3),
    "tall":      dict(hp=24, dmg=5, spd=0.32, armor=0, inf=120, w=0.55, h=2.5, sc=1.28, weight=1, grp=1),
    "scientist": dict(hp=30, dmg=3, spd=0.27, armor=2, inf=300, w=0.6, h=1.95, sc=1.0, weight=1, grp=1),
    "beast":     dict(hp=18, dmg=4, spd=0.33, armor=0, inf=80,  w=0.9, h=1.0,  sc=1.0, weight=2, grp=3, leaper=True),
    "beast_big": dict(hp=40, dmg=7, spd=0.30, armor=4, inf=110, w=1.2, h=1.4,  sc=1.4, weight=1, grp=1, leaper=True),
    "blob":      dict(hp=14, dmg=2, spd=0.20, armor=0, inf=120, w=0.8, h=0.75, sc=1.0, weight=2, grp=3),
    "blob_big":  dict(hp=50, dmg=5, spd=0.18, armor=2, inf=180, w=1.6, h=1.5,  sc=2.0, weight=1, grp=1),
    "hover":     dict(hp=14, dmg=2, spd=0.28, armor=2, inf=60,  w=0.8, h=0.9,  sc=1.0, weight=1, grp=2),
    "hover_big": dict(hp=40, dmg=4, spd=0.24, armor=6, inf=90,  w=1.2, h=1.3,  sc=1.5, weight=1, grp=1),
}

MOBS = []


def mob(name, title, role, pal, face="visor", pattern="drone", flags=0, arch="HUMANOID", **over):
    d = dict(ROLES[role])
    d.update(name=name, title=title, role=role, pal=pal, face=face, pattern=pattern, flags=flags,
             arch=arch, ranged=d.get("ranged", False), leaper=d.get("leaper", False),
             scientist=(role == "scientist"), fire=False, hypno=False, held="", dart=DART_NONE,
             coat=None)
    d.update(over)
    if d["ranged"] and d["dart"] == DART_NONE:
        d["dart"] = DART_LATEX
    MOBS.append(d)


# ---- A. the six evil anthro scientists (lab coat + weapon) ---------------------------
SCI = "scientist"
mob("scientist_wolf", "Dr. Lupin, Wolf Scientist", SCI, (0x8A8F98, 0xDADDE4, 0xFFD040), "animal", "coat",
    EARS_POINTY | SNOUT | TAIL_BUSHY | VISOR, held="latex_syringe", hypno=True, coat=0x2E8F8A)
mob("scientist_fox", "Dr. Vex, Fox Scientist", SCI, (0xE0762A, 0xF4EDE0, 0x40FF80), "animal", "coat",
    EARS_POINTY | SNOUT | TAIL_BUSHY, held="syringe_gun", ranged=True, dart=DART_LATEX, hypno=True, coat=0x7A35C9)
mob("scientist_cat", "Dr. Whisker, Cat Scientist", SCI, (0x2A2A30, 0xE8E8EE, 0x9CFF40), "animal", "coat",
    EARS_POINTY | TAIL_THIN | VISOR, held="hypno_ray", ranged=True, dart=DART_HYPNO, hypno=True, coat=0xC0262D)
mob("scientist_rabbit", "Dr. Clover, Rabbit Scientist", SCI, (0xF0F0F4, 0xF8C8D8, 0xFF5080), "animal", "coat",
    EARS_LONG | TAIL_SHORT, held="tranq_rifle", ranged=True, dart=DART_TRANQ, hypno=True, coat=0x2456C8)
mob("scientist_raven", "Dr. Corvin, Raven Scientist", SCI, (0x1A1A22, 0x3A3D55, 0xFF9020), "beak", "coat",
    BEAK | WINGS | TAIL_SHORT, held="goo_launcher", ranged=True, dart=DART_GOO, hypno=True, coat=0x3A3D45)
mob("scientist_lizard", "Dr. Scale, Lizard Scientist", SCI, (0x2E9E4F, 0xD8E8A0, 0xFFE040), "animal", "coat",
    TAIL_THICK | FIN | VISOR, held="stun_baton", hypno=True, coat=0xE8C42E)

# ---- B. latex drone units (visor + antenna humanoids) ---------------------------------
V, A = VISOR, ANTENNA
mob("drone_unit", "Latex Drone Unit", "basic", (K, G, 0x35E0FF), "visor", "drone", V)
mob("drone_worker", "Worker Drone", "basic", (G, Y, 0xFFD23A), "visor", "hazard", V)
mob("drone_scout", "Scout Drone", "fast", (NAVY, C, 0x5FF0FF), "visor", "stripe", V | A)
mob("drone_soldier", "Soldier Drone", "soldier", (K, R, 0xFF3030), "visor", "plates", V)
mob("drone_guard", "Guard Drone", "soldier", (W, B, 0x3A8CFF), "visor", "plates", V)
mob("drone_enforcer", "Enforcer Drone", "heavy", (K, P, 0xC050FF), "visor", "plates", V | HORNS)
mob("drone_medic", "Injector Drone", "ranged", (W, R, 0xFF5050), "visor", "cross", V, held="syringe_gun", dart=DART_LATEX)
mob("drone_sniper", "Sniper Drone", "ranged", (0x1F3D2A, GR, 0x60FF80), "visor", "stripe", V | A, held="tranq_rifle", dart=DART_TRANQ)
mob("drone_heavy", "Heavy Drone", "heavy", (DG, O, 0xFFA040), "visor", "plates", V)
mob("drone_commander", "Commander Drone", "elite", (K, GOLD, 0xFFE060), "visor", "royal", V | A | HORNS)
mob("drone_infiltrator", "Infiltrator Drone", "leaper", (0x2A1840, P, 0xB070FF), "visor", "stripe", V)
mob("drone_seeker", "Seeker Drone", "fast", (K, O, 0xFF8A20), "visor", "drone", V | A)
mob("drone_collector", "Collector Drone", "basic", (TEAL, W, 0x80FFF0), "visor", "drone", V)
mob("drone_sentinel", "Sentinel Drone", "soldier", (W, GOLD, 0xFFE890), "visor", "royal", V)
mob("drone_brute", "Brute Drone", "heavy", (CRIM, K, 0xFF6060), "visor", "plates", V | HORNS)
mob("drone_technician", "Technician Drone", "basic", (Y, K, 0x40FF90), "visor", "hazard", V | A, held="stun_baton")
mob("drone_hunter", "Hunter Drone", "leaper", (CRIM, O, 0xFFB040), "visor", "stripe", V)
mob("drone_warden", "Warden Drone", "heavy", (NAVY, W, 0xA0C8FF), "visor", "plates", V, held="stun_baton")
mob("drone_prototype", "Prototype Drone", "basic", (G, C, 0xFF40FF), "visor", "veins", V | A)
mob("drone_overseer", "Overseer Drone", "elite", (K, PK, 0xFF40C0), "visor", "royal", V | A, hypno=True)

# ---- C. latex animals (anthro humanoids with ears / tails) ----------------------------
WOLF = EARS_POINTY | SNOUT | TAIL_BUSHY
CAT = EARS_POINTY | TAIL_THIN
BIGCAT = EARS_ROUND | SNOUT | TAIL_THIN
SHARK = FIN | TAIL_THICK
DRAGON = HORNS | SNOUT | TAIL_THICK | WINGS
RABBIT = EARS_LONG | TAIL_SHORT
BEAR = EARS_ROUND | SNOUT | TAIL_SHORT
mob("latex_wolf_black", "Black Latex Wolf", "basic", (K, DG, 0xFFFFFF), "animal", "belly", WOLF)
mob("latex_wolf_white", "White Latex Wolf", "basic", (W, 0xC8C8D4, 0x40B0FF), "animal", "belly", WOLF)
mob("latex_wolf_gray", "Gray Latex Wolf", "soldier", (G, W, 0xFFD040), "animal", "belly", WOLF)
mob("latex_fox_orange", "Latex Fox", "fast", (O, W, 0x40FF80), "animal", "belly", WOLF)
mob("latex_fox_arctic", "Arctic Latex Fox", "fast", (W, 0xB8D8F0, 0x60C0FF), "animal", "belly", WOLF)
mob("latex_fox_shadow", "Shadow Latex Fox", "leaper", (0x241A2E, P, 0xD060FF), "animal", "belly", WOLF)
mob("latex_cat_black", "Black Latex Cat", "fast", (K, P, 0x9CFF40), "animal", "plain", CAT)
mob("latex_cat_white", "White Latex Cat", "fast", (W, PK, 0x40D0FF), "animal", "plain", CAT)
mob("latex_cat_calico", "Calico Latex Cat", "basic", (CREAM, O, 0x50E060), "animal", "spots", CAT)
mob("latex_tiger", "Latex Tiger", "soldier", (O, K, 0xFFE040), "animal", "tiger", BIGCAT)
mob("latex_snow_leopard", "Latex Snow Leopard", "leaper", (0xD8DCE4, DG, 0x70D0FF), "animal", "spots", EARS_ROUND | SNOUT | TAIL_BUSHY)
mob("latex_panther", "Latex Panther", "leaper", (0x101018, 0x2A2A3A, 0xFFD000), "animal", "plain", BIGCAT)
mob("latex_shark_blue", "Latex Shark", "soldier", (0x2B5C8F, W, 0xFFFFFF), "eyes", "belly", SHARK)
mob("latex_shark_tiger", "Tiger Latex Shark", "soldier", (0x5A6B78, CREAM, 0xFF4040), "eyes", "tiger", SHARK)
mob("latex_orca", "Latex Orca", "heavy", (K, W, 0x80E0FF), "eyes", "belly", SHARK)
mob("latex_dragon_red", "Red Latex Dragon", "elite", (R, GOLD, 0xFFE040), "animal", "belly", DRAGON, fire=True)
mob("latex_dragon_black", "Black Latex Dragon", "elite", (K, P, 0xD040FF), "animal", "belly", DRAGON, fire=True)
mob("latex_dragon_azure", "Azure Latex Dragon", "soldier", (0x2A78D8, W, 0x80FFFF), "animal", "belly", DRAGON)
mob("latex_rabbit_white", "White Latex Rabbit", "leaper", (W, PK, 0xFF5080), "animal", "belly", RABBIT)
mob("latex_rabbit_pink", "Pink Latex Rabbit", "leaper", (PK, W, 0xFFFFFF), "animal", "belly", RABBIT)
mob("latex_hare_brown", "Latex Hare", "fast", (BR, CREAM, 0x40FF60), "animal", "belly", RABBIT)
mob("latex_bat", "Latex Bat", "fast", (0x30263A, P, 0xFF3060), "eyes", "plain", EARS_POINTY | WINGS)
mob("latex_raven", "Latex Raven", "basic", (0x15151C, NAVY, 0xFF9020), "beak", "plain", BEAK | WINGS | TAIL_SHORT)
mob("latex_owl", "Latex Owl", "basic", (BR, CREAM, 0xFFD030), "beak", "spots", BEAK | WINGS | EARS_POINTY)
mob("latex_lizard_green", "Latex Lizard", "basic", (GR, Y, 0xFFE040), "eyes", "belly", TAIL_THICK | FIN)
mob("latex_gecko_yellow", "Latex Gecko", "small", (Y, O, 0x40FFE0), "eyes", "spots", TAIL_THICK)
mob("latex_cobra", "Latex Cobra", "ranged", (0x3A4A20, Y, 0xFF3030), "eyes", "belly", FIN | TAIL_THICK, dart=DART_GOO)
mob("latex_bear_brown", "Latex Bear", "heavy", (BR, CREAM, 0xFFFFFF), "animal", "belly", BEAR)
mob("latex_panda", "Latex Panda", "heavy", (W, K, 0x80FF80), "animal", "limbs", BEAR)
mob("latex_polar_bear", "Polar Latex Bear", "heavy", (0xF4F4F8, 0xC8D8E8, 0x60B0FF), "animal", "belly", BEAR)

# ---- D. goo people and specials ------------------------------------------------------
mob("goo_walker_black", "Black Goo Walker", "basic", (K, DG, 0xFFFFFF), "eyes", "goo")
mob("goo_walker_white", "White Goo Walker", "basic", (W, G, 0x30A0FF), "eyes", "goo")
mob("goo_walker_green", "Toxic Goo Walker", "basic", (0x3CC850, 0x1F7A30, 0xE8FF60), "eyes", "goo")
mob("goo_walker_pink", "Pink Goo Walker", "basic", (PK, 0xC04888, 0xFFFFFF), "eyes", "goo")
mob("latex_stalker", "Latex Stalker", "tall", (0x0C0C10, 0x202028, 0xFF2020), "blank", "plain")
mob("latex_mimic", "Latex Mimic", "basic", (G, 0x9AA0AA, 0x40FFB0), "eyes", "veins")
mob("latex_knight", "Latex Knight", "soldier", (0x9AA2B0, DG, 0x60D0FF), "visor", "plates", V | FIN, held="latex_blade")
mob("latex_siren", "Latex Siren", "ranged", (TEAL, PK, 0xFF80E0), "eyes", "belly", FIN | TAIL_THICK, dart=DART_HYPNO, hypno=True)
mob("latex_hypnotist", "Latex Hypnotist", "ranged", (P, PK, 0xFF60FF), "spiral", "royal", HORNS, dart=DART_HYPNO, hypno=True, weight=1)
mob("latex_queen_guard", "Queen's Guard", "elite", (K, GOLD, 0xFFD040), "visor", "royal", V | HORNS)
mob("latex_hive_queen", "Latex Hive Queen", "boss", (0x3A1458, GOLD, 0xFF50D0), "eyes", "royal", HORNS | WINGS | TAIL_THICK, hypno=True)
mob("latex_ghost", "Pale Latex Ghost", "fast", (0xDFE8F4, 0xA8B8D0, 0x40E0FF), "eyes", "goo")
mob("latex_imp", "Latex Imp", "small", (R, K, 0xFFE040), "eyes", "plain", HORNS | TAIL_THIN | WINGS)
mob("latex_golem", "Latex Golem", "giant", (0x4A4E58, 0x2A2C34, 0xFF7030), "eyes", "plates")
mob("latex_assimilator", "Assimilator", "elite", (K, GR, 0x50FF50), "visor", "veins", V | A, inf=250)
mob("latex_crystal_guardian", "Crystal Guardian", "soldier", (0x1E3A4A, C, 0x80FFFF), "eyes", "crystal", HORNS | FIN)
mob("latex_deer", "Latex Deer", "fast", (BR, CREAM, 0xFFFFFF), "animal", "belly", HORNS | EARS_POINTY | SNOUT | TAIL_SHORT)

# ---- E. feral beasts (quadruped model) -----------------------------------------------
BF = EARS_POINTY | SNOUT | TAIL_BUSHY          # extras a player gets from this form
mob("latex_hound_black", "Black Latex Hound", "beast", (K, DG, 0xFF3030), "animal", "belly", BF, "BEAST")
mob("latex_hound_white", "White Latex Hound", "beast", (W, G, 0x40C0FF), "animal", "belly", BF, "BEAST")
mob("latex_hound_red", "Crimson Latex Hound", "beast", (CRIM, K, 0xFFD040), "animal", "belly", BF, "BEAST")
mob("latex_feral_cat", "Feral Latex Cat", "beast", (0x241A2E, P, 0x9CFF40), "animal", "plain", CAT, "BEAST", spd=0.36)
mob("latex_feral_tiger", "Feral Latex Tiger", "beast", (O, K, 0xFFE040), "animal", "tiger", BIGCAT, "BEAST", hp=28, dmg=6)
mob("latex_crawler", "Latex Crawler", "beast", (G, DG, 0x40FFB0), "eyes", "veins", TAIL_THIN, "BEAST")
mob("latex_boar", "Latex Boar", "beast", (BR, CREAM, 0xFF5030), "animal", "plain", EARS_ROUND | SNOUT | TAIL_SHORT, "BEAST", hp=30, spd=0.28, leaper=False)
mob("latex_lynx", "Latex Lynx", "beast", (CREAM, BR, 0x60E060), "animal", "spots", EARS_POINTY | SNOUT | TAIL_SHORT, "BEAST")
mob("latex_jackal", "Latex Jackal", "beast", (0xB89050, K, 0xFF9020), "animal", "belly", BF, "BEAST")
mob("latex_direwolf", "Latex Direwolf", "beast_big", (DG, W, 0x80D0FF), "animal", "belly", BF, "BEAST")
mob("latex_hellhound", "Latex Hellhound", "beast", (0x2A1010, O, 0xFFA020), "animal", "veins", BF | HORNS, "BEAST", fire=True, hp=26, dmg=6, weight=1)

# ---- F. goo blobs --------------------------------------------------------------------
mob("goo_blob_black", "Black Goo Blob", "blob", (K, DG, 0xFFFFFF), "eyes", "goo", 0, "BLOB")
mob("goo_blob_white", "White Goo Blob", "blob", (W, G, 0x30A0FF), "eyes", "goo", 0, "BLOB")
mob("goo_blob_green", "Green Goo Blob", "blob", (0x3CC850, 0x1F7A30, 0xE8FF60), "eyes", "goo", 0, "BLOB")
mob("goo_blob_purple", "Purple Goo Blob", "blob", (P, 0x4A1F80, 0xFF90FF), "eyes", "goo", 0, "BLOB")
mob("goo_blob_orange", "Orange Goo Blob", "blob", (O, 0x9A4A10, 0xFFF0A0), "eyes", "goo", 0, "BLOB")
mob("goo_blob_blue", "Blue Goo Blob", "blob", (B, NAVY, 0xA0E0FF), "eyes", "goo", 0, "BLOB")
mob("toxic_blob", "Spitting Toxic Blob", "blob", (0x9ACD32, 0x4A6A10, 0xFFFF60), "eyes", "goo", 0, "BLOB", ranged=True, dart=DART_GOO, weight=2)
mob("giant_goo_blob", "Giant Goo Blob", "blob_big", (0x1E1E26, P, 0xD070FF), "eyes", "goo", 0, "BLOB")

# ---- G. mechanical hover drones ------------------------------------------------------
HF = VISOR | ANTENNA
mob("hover_drone_scout", "Hover Scout", "hover", (0x8A929E, C, 0x40F0FF), "visor", "drone", HF, "HOVER", spd=0.34)
mob("hover_drone_injector", "Hover Injector", "hover", (0xD8DCE2, R, 0xFF4040), "visor", "cross", HF, "HOVER", ranged=True, dart=DART_LATEX)
mob("hover_drone_sprayer", "Hover Goo Sprayer", "hover", (0x3A3D45, GR, 0x70FF50), "visor", "hazard", HF, "HOVER", ranged=True, dart=DART_GOO)
mob("hover_drone_hypno", "Hover Hypno-Emitter", "hover", (0x4A2A78, PK, 0xFF60FF), "visor", "royal", HF, "HOVER", ranged=True, dart=DART_HYPNO, hypno=True, weight=1)
mob("hover_drone_sentry", "Hover Sentry", "hover", (0x2A2C34, O, 0xFFA030), "visor", "plates", HF, "HOVER", hp=22, dmg=4, armor=6)
mob("hover_drone_carrier", "Hover Carrier", "hover_big", (0x5A6070, Y, 0xFFE040), "visor", "hazard", HF, "HOVER")
mob("hover_drone_stinger", "Hover Stinger", "hover", (K, P, 0xC060FF), "visor", "stripe", HF, "HOVER", spd=0.36, inf=140)
mob("hover_drone_alpha", "Hover Alpha", "hover_big", (K, GOLD, 0xFFD040), "visor", "royal", HF, "HOVER", ranged=True, dart=DART_TRANQ, hp=50)

assert len(MOBS) == 100, len(MOBS)
assert len({m["name"] for m in MOBS}) == 100
assert sum(1 for m in MOBS if m["scientist"]) == 6

# ---------------------------------------------------------------- blocks (61)
BLOCKS = []


def block(name, title, java, model="cube", layer=None, tool=None, drop=None, base=None, light=0):
    BLOCKS.append(dict(name=name, title=title, java=java, model=model, layer=layer, tool=tool,
                       drop=drop, base=base))


LATEX_COLORS = [
    ("black", "Black", "COLOR_BLACK", 0x16161A), ("white", "White", "SNOW", 0xE8E8EE),
    ("gray", "Gray", "COLOR_GRAY", 0x6B6F78), ("red", "Red", "COLOR_RED", 0xC0262D),
    ("blue", "Blue", "COLOR_BLUE", 0x2456C8), ("green", "Green", "COLOR_GREEN", 0x2E9E4F),
    ("purple", "Purple", "COLOR_PURPLE", 0x7A35C9), ("orange", "Orange", "COLOR_ORANGE", 0xE0762A),
    ("yellow", "Yellow", "COLOR_YELLOW", 0xE8C42E), ("pink", "Pink", "COLOR_PINK", 0xF08AB8),
    ("cyan", "Cyan", "COLOR_CYAN", 0x2EC4C9),
    ("brown", "Brown", "COLOR_BROWN", 0x7A5232), ("lime", "Lime", "COLOR_LIGHT_GREEN", 0x86D12E),
    ("magenta", "Magenta", "COLOR_MAGENTA", 0xC24FC8), ("light_blue", "Light Blue", "COLOR_LIGHT_BLUE", 0x5AA8E8),
    ("light_gray", "Light Gray", "COLOR_LIGHT_GRAY", 0xA8ACB4),
]
for cid, cname, mc, _hex in LATEX_COLORS:
    block(f"{cid}_latex_block", f"{cname} Latex Block", f"new InfectiousBlock(latex(MapColor.{mc}), 15)")
block("black_latex_goo", "Black Latex Goo", "new GooBlock(goo(MapColor.COLOR_BLACK), 30)", "goo", "translucent")
block("white_latex_goo", "White Latex Goo", "new GooBlock(goo(MapColor.SNOW), 30)", "goo", "translucent")
block("black_latex_puddle", "Black Latex Puddle", "new PuddleBlock(puddle(MapColor.COLOR_BLACK), 15)", "carpet")
block("white_latex_puddle", "White Latex Puddle", "new PuddleBlock(puddle(MapColor.SNOW), 15)", "carpet")
block("latex_core", "Latex Hive Core", "new HiveCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(5.0F, 12.0F).sound(SoundType.SLIME_BLOCK).lightLevel(s -> 12).randomTicks())",
      drop=("drone_core", 1, 1))
block("infected_dirt", "Infected Dirt", "new InfectiousBlock(earth(MapColor.DIRT, SoundType.GRAVEL, 0.6F), 6)", tool="shovel")
block("infected_grass", "Infected Grass Block", "new InfectiousBlock(earth(MapColor.GRASS, SoundType.GRASS, 0.7F), 6)", "bottom_top", tool="shovel")
block("infected_stone", "Infected Stone", "new InfectiousBlock(rock(MapColor.STONE, 1.8F), 5)", tool="pickaxe")
block("infected_cobblestone", "Infected Cobblestone", "new InfectiousBlock(rock(MapColor.STONE, 2.0F), 5)", tool="pickaxe")
block("infected_sand", "Infected Sand", "new InfectiousBlock(earth(MapColor.SAND, SoundType.SAND, 0.6F), 6)", tool="shovel")
block("infected_log", "Infected Log", "new RotatedPillarBlock(wood(MapColor.WOOD))", "pillar", tool="axe")
block("infected_planks", "Infected Planks", "new Block(wood(MapColor.WOOD))", tool="axe")
block("infected_leaves", "Infected Leaves", "new Block(see(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.3F).sound(SoundType.GRASS).noOcclusion()))", layer="cutout")
block("latex_crystal_black", "Black Latex Crystal", "new LatexPlantBlock(crystal(MapColor.COLOR_BLACK), 0)", "cross", "cutout")
block("latex_crystal_white", "White Latex Crystal", "new LatexPlantBlock(crystal(MapColor.SNOW), 0)", "cross", "cutout")
block("latex_crystal_green", "Toxic Latex Crystal", "new LatexPlantBlock(crystal(MapColor.COLOR_GREEN), 0)", "cross", "cutout")
block("latex_sprout", "Latex Sprout", "new LatexPlantBlock(plant(MapColor.COLOR_BLACK), 8)", "cross", "cutout")
block("latex_tendrils", "Latex Tendrils", "new LatexPlantBlock(plant(MapColor.COLOR_BLACK), 12)", "cross", "cutout")
block("latex_ore", "Latex Ore", "new Block(rock(MapColor.STONE, 3.0F))", tool="pickaxe", drop=("raw_latex", 1, 3))
block("deepslate_latex_ore", "Deepslate Latex Ore", "new Block(rock(MapColor.DEEPSLATE, 4.5F).sound(SoundType.DEEPSLATE))", tool="pickaxe", drop=("raw_latex", 1, 3))
block("raw_latex_block", "Block of Raw Latex", "new InfectiousBlock(latex(MapColor.COLOR_BLACK).strength(1.5F), 10)")

block("lab_wall", "Lab Wall", "new Block(lab())", tool="pickaxe")
block("lab_wall_striped", "Striped Lab Wall", "new Block(lab())", tool="pickaxe")
block("lab_tile", "Lab Tile", "new Block(lab())", tool="pickaxe")
block("lab_tile_dark", "Dark Lab Tile", "new Block(lab())", tool="pickaxe")
block("lab_tile_checker", "Checkered Lab Tile", "new Block(lab())", tool="pickaxe")
block("lab_ceiling", "Lab Ceiling Panel", "new Block(lab())", tool="pickaxe")
block("lab_light", "Lab Light", "new Block(lab().lightLevel(s -> 15))", tool="pickaxe")
block("lab_emergency_light", "Emergency Light", "new Block(lab().lightLevel(s -> 10))", tool="pickaxe")
block("lab_glass", "Lab Glass", "new GlassBlock(glass(0.4F, 0.4F))", layer="translucent")
block("lab_reinforced_glass", "Reinforced Lab Glass", "new GlassBlock(glass(6.0F, 1200.0F))", layer="translucent", tool="pickaxe")
block("lab_vent", "Lab Vent", "new Block(metal())", tool="pickaxe")
block("lab_hazard_block", "Hazard Block", "new Block(metal())", tool="pickaxe")
block("lab_pillar", "Lab Pillar", "new RotatedPillarBlock(lab())", "pillar", tool="pickaxe")
block("lab_wall_slab", "Lab Wall Slab", "new SlabBlock(lab())", "slab", tool="pickaxe", base="lab_wall")
block("lab_tile_slab", "Lab Tile Slab", "new SlabBlock(lab())", "slab", tool="pickaxe", base="lab_tile")
block("lab_wall_stairs", "Lab Wall Stairs", "new ModStairBlock(LAB_WALL.defaultBlockState(), lab())", "stairs", tool="pickaxe", base="lab_wall")
block("lab_tile_stairs", "Lab Tile Stairs", "new ModStairBlock(LAB_TILE.defaultBlockState(), lab())", "stairs", tool="pickaxe", base="lab_tile")
block("lab_computer", "Lab Computer", "new FacingBlock(metal().lightLevel(s -> 7))", "orientable", tool="pickaxe")
block("lab_server_rack", "Server Rack", "new FacingBlock(metal().lightLevel(s -> 4))", "orientable", tool="pickaxe")
block("lab_crate", "Lab Crate", "new Block(wood(MapColor.COLOR_LIGHT_GRAY))", tool="axe")
block("lab_shelf", "Specimen Shelf", "new FacingBlock(lab())", "orientable", tool="pickaxe")
block("lab_table", "Lab Table", "new Block(metal())", "bottom_top", tool="pickaxe")
block("containment_tank", "Containment Tank", "new Block(metal().lightLevel(s -> 6))", "column", tool="pickaxe")
block("conversion_chamber", "Conversion Chamber", "new ChamberBlock(metal().lightLevel(s -> 10))", "bottom_top", tool="pickaxe")
block("drone_charging_pad", "Drone Charging Pad", "new ChargingPadBlock(metal().lightLevel(s -> 8))", "bottom_top", tool="pickaxe")
block("hypno_screen", "Hypno Screen", "new FacingBlock(metal().lightLevel(s -> 12))", "orientable", tool="pickaxe")
block("latex_gas_vent", "Latex Gas Vent", "new GasVentBlock(metal())", "bottom_top", tool="pickaxe")
block("decontamination_block", "Decontamination Pad", "new DeconBlock(metal().lightLevel(s -> 6))", "bottom_top", tool="pickaxe")
block("alarm_block", "Lab Alarm", "new AlarmBlock(metal().lightLevel(s -> 9))", tool="pickaxe")

# ---- expansion: goo colours, latex masonry, more infected terrain, lab set, drone life blocks
block("purple_latex_goo", "Purple Latex Goo", "new GooBlock(goo(MapColor.COLOR_PURPLE), 30)", "goo", "translucent")
block("green_latex_goo", "Toxic Latex Goo", "new GooBlock(goo(MapColor.COLOR_GREEN), 30)", "goo", "translucent")
block("purple_latex_puddle", "Purple Latex Puddle", "new PuddleBlock(puddle(MapColor.COLOR_PURPLE), 15)", "carpet")
block("green_latex_puddle", "Toxic Latex Puddle", "new PuddleBlock(puddle(MapColor.COLOR_GREEN), 15)", "carpet")
block("latex_bricks", "Latex Bricks", "new Block(rock(MapColor.COLOR_BLACK, 2.0F))", tool="pickaxe")
block("latex_brick_slab", "Latex Brick Slab", "new SlabBlock(rock(MapColor.COLOR_BLACK, 2.0F))", "slab", tool="pickaxe", base="latex_bricks")
block("latex_brick_stairs", "Latex Brick Stairs", "new ModStairBlock(LATEX_BRICKS.defaultBlockState(), rock(MapColor.COLOR_BLACK, 2.0F))", "stairs", tool="pickaxe", base="latex_bricks")
block("polished_latex", "Polished Latex", "new Block(rock(MapColor.COLOR_BLACK, 2.0F))", tool="pickaxe")
block("chiseled_latex", "Chiseled Latex", "new Block(rock(MapColor.COLOR_BLACK, 2.0F).lightLevel(s -> 5))", tool="pickaxe")
block("infected_gravel", "Infected Gravel", "new InfectiousBlock(earth(MapColor.STONE, SoundType.GRAVEL, 0.6F), 6)", tool="shovel")
block("infected_deepslate", "Infected Deepslate", "new InfectiousBlock(rock(MapColor.DEEPSLATE, 3.0F).sound(SoundType.DEEPSLATE), 5)", tool="pickaxe")
block("infected_planks_slab", "Infected Plank Slab", "new SlabBlock(wood(MapColor.WOOD))", "slab", tool="axe", base="infected_planks")
block("infected_planks_stairs", "Infected Plank Stairs", "new ModStairBlock(INFECTED_PLANKS.defaultBlockState(), wood(MapColor.WOOD))", "stairs", tool="axe", base="infected_planks")
block("glowing_latex_pod", "Glowing Latex Pod", "new LatexPlantBlock(crystal(MapColor.COLOR_PURPLE).lightLevel(s -> 12), 0)", "cross", "cutout")
block("lab_steel_plate", "Steel Plating", "new Block(metal())", tool="pickaxe")
block("lab_steel_slab", "Steel Plating Slab", "new SlabBlock(metal())", "slab", tool="pickaxe", base="lab_steel_plate")
block("lab_steel_stairs", "Steel Plating Stairs", "new ModStairBlock(LAB_STEEL_PLATE.defaultBlockState(), metal())", "stairs", tool="pickaxe", base="lab_steel_plate")
block("lab_dark_wall", "Dark Lab Wall", "new Block(lab())", tool="pickaxe")
block("lab_dark_wall_slab", "Dark Lab Wall Slab", "new SlabBlock(lab())", "slab", tool="pickaxe", base="lab_dark_wall")
block("lab_dark_wall_stairs", "Dark Lab Wall Stairs", "new ModStairBlock(LAB_DARK_WALL.defaultBlockState(), lab())", "stairs", tool="pickaxe", base="lab_dark_wall")
block("lab_floor_grate", "Floor Grate", "new GlassBlock(see(metal().noOcclusion()))", layer="cutout", tool="pickaxe")
block("lab_monitor", "Lab Monitor", "new FacingBlock(metal().lightLevel(s -> 8))", "orientable", tool="pickaxe")
block("lab_cabinet", "Lab Cabinet", "new FacingBlock(metal())", "orientable", tool="pickaxe")
block("specimen_freezer", "Specimen Freezer", "new FacingBlock(metal().lightLevel(s -> 4))", "orientable", tool="pickaxe")
block("research_desk", "Research Desk", "new FacingBlock(lab())", "orientable", tool="pickaxe")
block("lab_generator", "Lab Generator", "new FacingBlock(metal().lightLevel(s -> 9))", "orientable", tool="pickaxe")
block("lab_pipe", "Lab Pipe", "new RotatedPillarBlock(metal())", "pillar", tool="pickaxe")
block("toxic_tank", "Toxic Tank", "new Block(metal().lightLevel(s -> 8))", "column", tool="pickaxe")
block("drone_terminal", "Drone Terminal", "new DroneTerminalBlock(metal().lightLevel(s -> 10))", "orientable", tool="pickaxe")
block("drone_pod", "Drone Sleep Pod", "new DronePodBlock(metal().lightLevel(s -> 7).noOcclusion())", "column", "translucent", tool="pickaxe")
block("drone_fabricator", "Drone Fabricator", "new FacingBlock(metal().lightLevel(s -> 6))", "orientable", tool="pickaxe")
block("drone_beacon", "Drone Beacon", "new Block(metal().lightLevel(s -> 15))", tool="pickaxe")
block("hypno_pylon", "Hypno Pylon", "new Block(metal().lightLevel(s -> 11))", "column", tool="pickaxe")
block("drone_locker", "Drone Locker", "new FacingBlock(metal())", "orientable", tool="pickaxe")

assert len(BLOCKS) == 100, len(BLOCKS)

# ---------------------------------------------------------------- items
WEAPONS = [  # exactly nine
    ("stun_baton", "Stun Baton"), ("tesla_prod", "Tesla Prod"), ("bone_saw", "Bone Saw"),
    ("latex_blade", "Latex Blade"), ("syringe_gun", "Syringe Gun"), ("tranq_rifle", "Tranquilizer Rifle"),
    ("goo_launcher", "Goo Launcher"), ("purifier_flamethrower", "Purifier Flamethrower"),
    ("hypno_ray", "Hypno Ray"),
]
HANDHELD = {"stun_baton", "tesla_prod", "bone_saw", "latex_blade", "syringe_gun", "tranq_rifle",
            "goo_launcher", "purifier_flamethrower", "hypno_ray", "drone_mop"}
MISC_ITEMS = [
    ("raw_latex", "Raw Latex"), ("drone_core", "Drone Core"),
    ("latex_dart", "Latex Dart"), ("tranq_dart", "Tranquilizer Dart"), ("antidote_dart", "Antidote Dart"),
    ("goo_canister", "Goo Canister"),
    ("drone_mop", "Drone Mop"),
    ("latex_syringe", "Latex Syringe"), ("antidote_syringe", "Antidote Syringe"), ("cure_serum", "Cure Serum"),
]
assert len(WEAPONS) == 9

# ---------------------------------------------------------------- sounds
SOUNDS = [  # (event, subtitle, variants)
    ("drone_ambient", "Drone hums", 2), ("drone_hurt", "Drone glitches", 1), ("drone_death", "Drone shuts down", 1),
    ("drone_step", "Latex squelches", 2), ("beast_growl", "Latex beast growls", 1), ("blob_squish", "Goo squishes", 1),
    ("hover_buzz", "Hover drone buzzes", 1), ("scientist_laugh", "Scientist chuckles", 1),
    ("scientist_hurt", "Scientist grunts", 1), ("inject", "Syringe injects", 1), ("dart_shoot", "Dart fires", 1),
    ("goo_splat", "Goo splats", 1), ("flamethrower", "Flamethrower roars", 1), ("zap", "Electricity zaps", 1),
    ("transform_start", "Latex spreads", 1), ("transform_complete", "Assimilation complete", 1),
    ("hypnosis", "Hypnotic hum", 1), ("alarm", "Alarm blares", 1), ("heartbeat", "Infection pulses", 1),
    ("cure", "Infection fades", 1),
    ("terminal_beep", "Terminal beeps", 1), ("task_complete", "Task complete", 1), ("standby", "Drone powers down", 1),
    ("correction", "Unit corrected", 1),
]

# ---------------------------------------------------------------- structures (19)
# (name, title, kind, spacing, separation) -- kind: surface | buried | underground
STRUCTURES = [
    ("abandoned_lab", "Abandoned Lab", "surface", 42, 14),
    ("research_lab", "Research Lab", "surface", 62, 20),
    ("containment_facility", "Containment Facility", "surface", 73, 22),
    ("latex_pit", "Latex Pit", "surface", 36, 11),
    ("black_latex_hive", "Black Latex Hive", "surface", 56, 17),
    ("white_latex_hive", "White Latex Hive", "surface", 64, 17),
    ("drone_outpost", "Drone Outpost", "surface", 48, 14),
    ("crashed_transport", "Crashed Transport", "surface", 39, 11),
    ("conversion_shrine", "Conversion Shrine", "surface", 59, 17),
    ("infected_house", "Infected House", "surface", 42, 14),
    ("latex_spire", "Latex Spire", "surface", 45, 14),
    ("hypno_tower", "Hypno Tower", "surface", 67, 20),
    ("scientist_camp", "Scientist Camp", "surface", 50, 14),
    ("charging_station", "Drone Charging Station", "surface", 53, 14),
    ("quarantine_checkpoint", "Quarantine Checkpoint", "surface", 48, 14),
    ("vent_field", "Gas Vent Field", "surface", 50, 14),
    ("signal_tower", "Signal Tower", "surface", 56, 17),
    ("underground_bunker", "Underground Bunker", "buried", 50, 17),
    ("latex_cave_nest", "Latex Cave Nest", "underground", 34, 11),
    ("drone_barracks", "Drone Barracks", "surface", 50, 17),
    ("processing_center", "Processing Center", "surface", 62, 20),
    ("contaminated_storage", "Contaminated Storage", "surface", 44, 14),
    ("directors_office", "Director's Office", "surface", 76, 24),
    ("drone_factory", "Drone Factory", "surface", 64, 20),
    ("hypno_garden", "Hypno Garden", "surface", 52, 17),
    ("latex_monolith", "Latex Monolith", "surface", 46, 14),
    ("assimilated_farm", "Assimilated Farm", "surface", 48, 15),
    ("observation_dome", "Observation Dome", "surface", 58, 18),
]
assert len(STRUCTURES) == 28

LAND_BIOMES = [
    "plains", "sunflower_plains", "snowy_plains", "desert", "swamp", "forest", "flower_forest",
    "birch_forest", "dark_forest", "old_growth_birch_forest", "old_growth_pine_taiga",
    "old_growth_spruce_taiga", "taiga", "snowy_taiga", "savanna", "savanna_plateau",
    "windswept_hills", "windswept_gravelly_hills", "windswept_forest", "windswept_savanna",
    "jungle", "sparse_jungle", "bamboo_jungle", "badlands", "eroded_badlands", "wooded_badlands",
    "meadow", "cherry_grove", "grove", "snowy_slopes", "stony_peaks",
]
