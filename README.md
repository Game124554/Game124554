# Latex Drone Infection — Fabric mod for Minecraft Java 1.20.1

Latex creatures, drones and evil anthro scientists. Get hit, get infected, get hypnotized, and become one of them.

## What's inside

| | Count | Notes |
|---|---|---|
| Latex mobs | **50** | 15 drones (glowing visors) + 35 latex beasts (wolves, foxes, cats, sharks, orcas, dragons, tiger, bat, bear, goo blobs, a 1.8× Goo Giant, Dark Latex Alpha…). Each has its own skin, glow layer, stats, animated ears/tail/wings/fins/horns. |
| Evil scientists | **6** | Anthro fox, bear, raven, wolf, cat and rat in lab coats with goggles. They carry a weapon (Syringe Gun, Latex Blade or Hypno Staff). Every hit **injects** you: Hypnosis + latex infection. |
| Blocks | **40** | 10 latex blocks, puddles & crystals that infect you, infected dirt/stone/log/planks, latex bricks, 20 lab blocks (tiles, walls, lights, glass, containment tank, vents, hazard, computer, server rack, **Hypno Screen**, **Drone Conversion Pod**, …). |
| Weapons | **4** | Latex Blade, Syringe Gun, Hypno Staff, Purifier Cannon. |
| Spawn eggs | **56** | Every egg has its own hand-styled texture. |
| Sound effects | 18 (24 files) | Gurgles, drone beeps, scientist chuckles, syringe inject/shoot, hypno hum, transformation, heartbeat, purifier zap… |
| Structures | **7** | Abandoned Lab, Latex Nest, Containment Facility (underground), Drone Hive, Research Outpost, Infected Ruins, Hypno Tower — with mobs, spawners and loot chests. |

## Infection → transformation

* Latex mobs, puddles, crystals and scientist injections raise your **Latex Infection** meter (top-left of the screen).
* 0–30 % fades on its own · 30–60 % slowness + heartbeat · 60 %+ it grows by itself, nausea, latex creeps over your screen.
* At 100 % you **transform** (5 s animation): your body squashes and shudders, the latex skin fades in over yours, ears/tail/wings grow out, eyes start to glow, goo drips down your screen.
* After that **your skin is replaced** with that creature's (also in first person and for other players), you get its perks (night vision + speed / jump / water / strength / resistance / slow-fall / haste) and **latex mobs and scientists ignore you**.
* You become the creature that infected you. Use an **Empty Syringe** on any latex mob to fill a **Latex Injector** with that form, or step into a **Drone Conversion Pod**.
* **Latex Antidote** (or `/latex cure`) turns you back into a human.

## Hypnosis

Scientist injections, syringe darts, Hypno Staff and Hypno Screens hypnotize. You get a spinning spiral overlay, whispers ("Obey…", "Accept the latex…"), your view is pulled toward the nearest scientist, you're slowed, and infection slowly rises. Hypnotized mobs freeze and forget their target.

## Commands (op)

`/latex transform <form>` · `/latex cure` · `/latex infect <amount>` · `/latex forms`

## Building the .jar

You need **Java 17+**. In this folder:

```
./gradlew build          (Windows: gradlew.bat build)
```

The mod is then at `build/libs/latexdrone-1.0.0.jar`. Put it in `.minecraft/mods` together with **Fabric API** (for 1.20.1) and launch with Fabric Loader 1.20.1.

**No PC?** Upload this folder to a new GitHub repository. The included workflow (`.github/workflows/build.yml`) builds it automatically; download the jar from the repository's **Actions** tab → latest run → *latexdrone-mod-jar*.

## Changing content

Everything is generated from `tools/spec.py` (forms, colours, stats, scientists, blocks). After editing, run:

```
python3 tools/gen_java.py && python3 tools/gen_skins.py && python3 tools/gen_icons.py \
 && python3 tools/gen_sounds.py && python3 tools/gen_data.py && python3 tools/gen_structures.py
```

(Needs Python 3 with Pillow + numpy, and ffmpeg for the sounds.)
