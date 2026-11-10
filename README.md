# Latex Drone Infection

A Fabric mod for **Minecraft Java Edition 1.20.1**. A latex outbreak spreads through the world:
drones, goo creatures and evil anthro scientists hunt you, and if the infection wins **you become one of them**.

> **Status:** compiles on GitHub Actions; the ready jar is in `dist/`. Not yet play-tested in the game.

![The 100 mobs](docs/mobs.png)

## What is in the mod

| | Count | Notes |
|---|---|---|
| Mobs | **100** | 6 scientists, 20 drone units, 30 latex animals, 17 goo people & specials, 11 feral beasts, 8 goo blobs, 8 hover drones |
| Spawn eggs | 100 | each with its own painted texture |
| Blocks | **100** | 16 latex colours, goo, puddles, latex bricks, infected terrain, crystals, lab building set, machines, drone terminal, sleep pod |
| Weapons | **9** | 4 melee, 5 ranged |
| Structures | **28** | labs, hives, towers, a bunker, a cave nest, drone barracks, factory, director's office, hypno garden, observation dome ... |
| Sound effects | 24 events / 26 files | all synthesised for this mod |
| Status effects | 3 | Latex Infection, Hypnosis, Inoculated |

### The infection (it infects *you*)

* Hits from latex mobs, darts, latex blocks, goo, puddles and gas vents fill your **infection meter** (top of the screen).
* Once infected it keeps growing by itself (about 0.4% per second). Black goo creeps in from the edges of your
  screen, your skin starts turning glossy, you hear your heartbeat.
* At **100%** the **transformation** plays (7 seconds): you are rooted to the spot, clutch your head, throw your
  arms wide, latex climbs up your body and runs down your screen - then you wake up as the creature that infected you.
* **Killed by a latex mob?** You respawn as one of them.

### Being one of them

* Your **skin is replaced** by the form's skin, with its ears / tail / visor / wings (other players see it too).
* Drones and latex creatures ignore you. Scientists do not - they want their specimen back.
* +4 hearts, +2 attack damage, +12% speed, night vision. Charging pads heal you.
* Your punches infect other players. Antidote darts and decontamination pads hurt you.
* Only a **Cure Serum** turns you human again.

### Drone life: you are a drone now

* **Orders.** Scientists near you hand out jobs (or right-click one, or use a **Drone Terminal**). Every order
  has a **time limit** and shows at the top of the screen:
  clean latex spills, bring Raw Latex, deliver Iron Ingots, patrol to a marked point, guard an area,
  eliminate intruders, convert villagers.
* **Cleaning.** A cleaning order spills latex puddles around you. Break them, or use the **Drone Mop**
  (right-click the ground, cleans everything within 2 blocks).
* **Obedience (0-100%).** Finishing orders raises it and pays XP, emeralds, latex, darts or a Drone Core.
  **Disobeying** (letting the timer run out, refusing an order, hitting a scientist) lowers it: you are
  *corrected* (shock + hypnosis) and the scientists turn on you for a while.
  Below 25% you are a **ROGUE UNIT** and every drone hunts you; from 75% you are **LOYAL** and nearby
  drones attack whoever hurts you.
* **Ranks:** Drone -> Loyal Drone (3 orders) -> Elite Drone (6) -> Commander Unit (10); each rank adds a heart.
* **Energy and sleep.** Your battery drains slowly (about 50 minutes). Below 20% you get slow, at 0% weak.
  Right-click a **Drone Sleep Pod** or **Charging Pad** (or press *Enter Standby* at a terminal next to one)
  to go into **standby**: the screen powers down, energy, health and hunger refill, and at night the night
  passes once everyone is resting. Sneak to wake up; taking damage wakes you too.
* **Drone Terminal (UI block).** Right-click it to open the hive terminal: unit number, rank, energy,
  obedience and current order, with buttons **Request Task**, **Report** (hand in items), **Refuse Task**
  and **Enter Standby**. Humans get *ACCESS DENIED*.
* Whatever a drone player kills that can be converted (villagers, illagers, zombies) rises as a new drone.

### The six scientists

Anthro animals in lab coats, each carrying a weapon. Their attacks **inject** you (+30% infection) and **hypnotise** you:
your screen swirls, you are slowed, dragged towards them and your camera turns to face them.

| Scientist | Weapon | Attack |
|---|---|---|
| Dr. Lupin (wolf) | Latex Syringe | melee injection |
| Dr. Vex (fox) | Syringe Gun | latex darts |
| Dr. Whisker (cat) | Hypno Ray | hypnosis beam |
| Dr. Clover (rabbit) | Tranquilizer Rifle | sedating darts |
| Dr. Corvin (raven) | Goo Launcher | goo that splashes |
| Dr. Scale (lizard) | Stun Baton | melee |

### Weapons

| Weapon | What it does |
|---|---|
| Stun Baton | stuns (slowness + weakness) |
| Tesla Prod | lightning arcs to nearby mobs |
| Bone Saw | very fast, causes a lingering wound |
| Latex Blade | infects players; anything convertible it kills rises as a drone |
| Syringe Gun | fires Latex Darts (infect / convert villagers) or Antidote Darts (cure players, melt drones) |
| Tranquilizer Rifle | long range, heavy slowness |
| Goo Launcher | goo canisters: area infection + puddle |
| Purifier Flamethrower | cone of fire, burns latex creatures and latex blocks |
| Hypno Ray | hypnotises what you aim at (mobs forget their target) |

Other items: Latex Syringe (infect yourself or convert a mob), **Antidote Syringe** (-50% infection, 1 minute immune),
**Cure Serum**, darts, goo canisters, Raw Latex, Drone Core.

### Useful blocks

Conversion Chamber (right-click = instant transformation), Decontamination Pad (washes infection off),
Drone Charging Pad, Hypno Screen (hypnotises nearby players), Latex Gas Vent, Lab Alarm (calls every drone nearby),
Latex Hive Core (infects the ground around it and grows drones).

### Commands (operators)

```
/latex infect <player> <0-100>
/latex transform <player> [form]     form = any mob id, e.g. latex_wolf_black
/latex cure <player>
/latex obedience <player> <0-100>
/latex task <player>                 give a drone player an order right away
```

## Getting the .jar

**Download it:** the GitHub repository builds the mod automatically on every change and puts the jar in
`dist/` on the `latex-drone-mod` branch:
https://github.com/Game124554/game124554/tree/latex-drone-mod/dist

**Or build it yourself:** install Java 17 or 21, then run `gradlew build` (Windows) or `./gradlew build`
(macOS / Linux) in this folder. The jar ends up in `build/libs/`.

### Installing

1. Install **Fabric Loader** for Minecraft 1.20.1 (fabricmc.net).
2. Put the mod jar **and** [Fabric API](https://modrinth.com/mod/fabric-api) for 1.20.1 into your `mods` folder.

## If the build fails

Because the code has never met a compiler, a mistyped Minecraft method name somewhere is likely.
The build log prints the file and line (`error: cannot find symbol ...`). Send that log back and the fix
is usually one line. What *was* checked: Java syntax, imports, every JSON file, that every registered block /
item / mob has its model, texture, loot table and translation, and that all 19 structure files load.

## Project layout

```
src/main/java/net/latexdrones/     the code (about 3,900 lines)
  content/MobDefs.java             GENERATED table of the 100 mobs
  registry/ModBlocks.java          GENERATED list of the 61 blocks
  entity/LatexDroneEntity.java     the one entity class behind every mob
  infection/                       infection meter, transformation, cure, sync, commands
  client/                          models, renderers, HUD overlays, player layer
  mixin/client/                    skin replacement + transformation pose
src/main/resources/                textures, models, sounds, loot, recipes, structures, worldgen
tools/                             Python generators - edit tools/content.py, then run:
                                   gen_textures.py, gen_sounds.py, gen_assets.py, gen_structures.py
```

To add or change a mob, block or structure, edit `tools/content.py` (or the builder in `gen_structures.py`)
and run the generators again; they need Python 3 with Pillow and numpy, and ffmpeg for the sounds.

## License

MIT - see `LICENSE`.
