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
| Blocks | **61** | latex, goo, infected terrain, crystals, lab building set, machines |
| Weapons | **9** | 4 melee, 5 ranged |
| Structures | **19** | labs, hives, towers, a bunker, a cave nest ... |
| Sound effects | 20 events / 22 files | all synthesised for this mod |
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

### Drone life: orders, ranks and sleep

* The scientists stop hunting you - you work for them now. **Right-click a scientist** to get an **order**
  (idle drones near a scientist also get called over). The order and your progress show at the top of the screen.
* Orders: collect Raw Latex, bring Iron Ingots, convert villagers or zombies, eliminate intruders,
  recharge on a charging pad, guard an area. Collect orders are handed in by right-clicking a scientist;
  the others finish by themselves.
* Every finished order pays XP, emeralds and sometimes a Drone Core. Ranks: **Drone -> Loyal Drone (3 orders)
  -> Elite Drone (6) -> Commander Unit (10)**, each rank adds a heart.
* Hitting a scientist counts as disobedience: you get hypnotised and re-programmed.
* **Sleep:** right-click a **Drone Charging Pad** to go into **standby mode**. The screen powers down, you heal
  and refuel, and if everyone else is asleep (or in standby) the night passes. Sneak to wake up; taking damage
  wakes you too.
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
```

## Getting the .jar

You need the mod compiled once. Pick whichever is easiest:

**A. Let GitHub build it (nothing to install)**
1. Create a free GitHub account and a new repository, upload this whole folder.
2. Open the repository's **Actions** tab - the "Build mod" run starts by itself.
3. When it is green, open the run and download **latexdrones-jar**.

**B. Build on your PC**
1. Install Java 17 or 21 (for example from adoptium.net).
2. In this folder run `gradlew build` (Windows) or `./gradlew build` (macOS / Linux). The first run downloads Minecraft and takes a while.
3. The mod is `build/libs/latexdrones-1.0.0+mc1.20.1.jar`.

**C. IntelliJ IDEA** - open the folder, let Gradle import, run the `build` task (or `runClient` to launch the game directly).

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
