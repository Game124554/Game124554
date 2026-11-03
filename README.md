# Realistic Overhaul 2.0 (Fabric 1.20.1)

One mod that makes Minecraft Java 1.20.1 look and behave more like real life.

**Download:** the `.jar` is on the [Releases page](https://github.com/Game124554/Game124554/releases) (tag `realistic-overhaul-v2.0.0`).
Put it in `.minecraft/mods` together with **Fabric API** for 1.20.1 (Fabric Loader 0.14.21 or newer).
Install it on the server too when you play multiplayer.

**Settings:** press **O** in game, or open **Options → Realistic Overhaul...**. Everything can be switched off, and the
**Performance** preset (LOW / MEDIUM / HIGH / ULTRA) limits how much work the physics, corpses, particles and weather do.
Settings are saved in `config/realisticoverhaul.json`.

## Features

| Feature | What it does |
|---|---|
| **Smooth terrain (NoCubes style)** | Stone, dirt, grass, sand, gravel, ores, netherrack and other natural blocks are drawn as smooth slopes instead of cubes. Flat ground stays flat. Lighting and grass color are smooth too. Edit the `smoothBlocks` list in the config to choose blocks (ids or `#tags`). |
| **Walk up slopes** | You walk up 1-block steps without jumping, and the camera glides up instead of snapping. |
| **Realistic terrain generation** | New worlds get bigger continents and longer mountain ranges (about 1.7x the size), hills and mountains about 60% taller with sharper peaks (the height limit for terrain is raised from Y 256 to Y 300), deeper oceans and wider river valleys. This is a built-in data pack called *Realistic Terrain*. It is on by default; turn it off under **Data Packs** when creating a world. Old chunks are not changed. |
| **Block collapse physics** | Break, burn or blow up a block, and the blocks it was holding up fall. A block stays up only if something is under it or at least 2 solid blocks touch its sides. Falling blocks hurt mobs. Trees fall when you cut the trunk. |
| **Breakable glass** | Fast arrows, tridents and snowballs shatter glass. Landing on glass from 4+ blocks smashes through it, and so does sprinting into it. |
| **Camera shake** | From explosions (stronger when closer), taking damage and blocks collapsing near you. |
| **Corpses + ragdoll** | Dead mobs are thrown back by the hit and stay on the ground (60 s by default, up to 10 minutes). Their head, arms and legs flop separately, and swing again when a corpse is kicked, pushed or lands. Walk into a corpse to push it, hit it to kick it. |
| **Realistic mobs** | Hits splatter (red for most mobs, bone for skeletons, slime for slimes...). Mobs under 30% health limp and move 35% slower. |
| **Rain puddles** | Puddles form while it rains, mostly in low spots, and dry up afterwards. |
| **Snow** | Snow builds up in layers while it snows (up to 4 by default, 8 max). |
| **Water and lava** | Lava flows fast and far everywhere. Steam rises where water touches lava. |
| **Bedrock removed** | Bedrock is replaced in new chunks (deepslate in the Overworld, netherrack in the Nether) and existing bedrock can be mined. Blocks at the very bottom never drop into the void. |
| **Hell** | The Nether and deep Overworld caves (below Y=0) get thick red smoky fog, falling ash and rising embers. |
| **Death → Nether → Piglin** | When you die you wake up in the Nether. Other players see you as a Piglin, and Piglins treat you as one of their own. Leave the Nether through a portal to become human again. |

## Good to know
- **Smooth terrain uses the Fabric Rendering API.** It works with plain Fabric API. With **Sodium** you also need **Indium**, otherwise terrain stays cubic (everything else still works). The Sodium + Indium combination has not been tested yet.
- Smooth terrain changes only how blocks **look**. You still collide with full blocks, so on a slope you may stand slightly above or below the surface.
- **Realistic 3D mob models** need artwork, which this mod does not include. On 1.20.1 Fabric you can add the *Fresh Animations* resource pack with *Entity Model Features* and *Entity Texture Features*. The ragdolls work with those models too.
- Ragdolls work on humanoids, animals with four legs, and most single-model mobs (creepers, spiders, villagers, illagers, golems). Other mobs just fall over.

## Build it yourself
Install Java 17 or 21, open a terminal in this folder and run `./gradlew build` (or `gradlew build` on Windows).
The jar appears in `build/libs/`. Every push to this branch is also built by GitHub Actions (`.github/workflows/build.yml`),
which boots a test server and a test client and saves screenshots to the `ci-results` branch.

`tools/gen_terrain.py` regenerates the realistic terrain data pack from the vanilla 1.20.1 world-gen files; change the
numbers at the top of the script to tune it.
