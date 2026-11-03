# Realistic Overhaul (Fabric 1.20.1)

Needs **Fabric Loader 0.14.21+** and **Fabric API** for 1.20.1.
In game, press **O** to open the settings. Settings are saved in `config/realisticoverhaul.json`.

## Features
| Feature | What it does |
|---|---|
| Block collapse physics | Break, burn or blow up a block, and any block it was holding up falls. A block stays up only if something is under it or at least 2 solid blocks touch its sides. Falling blocks hurt mobs. Trees fall when you cut the trunk. |
| Breakable glass | Fast arrows, tridents and snowballs shatter glass. Landing on glass from 4+ blocks smashes through it, and so does sprinting into it. |
| Camera shake | From explosions (stronger when you are closer), from taking damage and from blocks collapsing near you. |
| Corpses / ragdoll | Dead mobs are thrown back from the hit, then lie on the ground as corpses (60 s by default). Walk into a corpse to push it, or hit it to kick it. |
| Rain puddles | Puddles of water form during rain, mostly in low spots. They dry up when the rain stops, splash when you walk through them, and put out fire on you. |
| Snow | Snow builds up in layers while it snows (up to 4 layers by default, 8 max). |
| Water and lava | Lava flows fast and far everywhere, the way it does in the Nether. Steam rises where water touches lava. |
| Bedrock | Bedrock can be broken (it is as hard as obsidian). Bedrock is also replaced in newly generated chunks: deepslate in the Overworld, netherrack in the Nether. Mining blocks at the very bottom never drops them into the void. |
| Hell visuals | The Nether and deep Overworld caves (below Y=0) get red, thick, smoky fog, plus falling ash and rising embers. |
| Death → Nether → Piglin | When you die you respawn in the Nether, and other players see you with the Piglin model. Piglins treat you as one of their own. Leave the Nether through a portal to become human again. |
| Performance limits | The LOW / MEDIUM / HIGH / ULTRA preset sets how many physics checks run per tick, the maximum number of corpses, the particle amount and how often weather updates. |

## Not included (needs other mods or real 3D artwork)
- **NoCubes-style smooth terrain** needs a complete replacement of the terrain mesh renderer, which is a large project of its own.
- **Realistic 3D mob models** need models drawn by an artist. On 1.20.1 Fabric you can use *Fresh Animations* (a resource pack) with *Entity Model Features* and *Entity Texture Features*.
- **True bone ragdolls**: corpses here are thrown and fall over as one piece. The individual limbs do not move separately.

## How to get the .jar
**Option A, GitHub (no setup needed):** upload this folder to a new GitHub repository. The included workflow (`.github/workflows/build.yml`) builds the mod automatically. Open the **Actions** tab, select the finished run, and download `realistic-overhaul-jar`.

**Option B, your PC:** install Java 17 or 21, open a terminal in this folder and run:
```
gradlew build        (Windows)
./gradlew build      (Mac/Linux)
```
The jar appears in `build/libs/realistic-overhaul-1.0.0.jar`. Put it in `.minecraft/mods` together with Fabric API.

## Notes
- The mod must be installed on the server and on every client. Physics, corpses, puddles and the Piglin curse run on the server. Fog, particles, camera shake and the Piglin model run on the client.
- If a feature is too heavy, set Performance to LOW or switch the feature off with the O key.
