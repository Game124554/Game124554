# Teardown Voxel Physics (`teardown_voxel`)

Teardown-style destruction for **Minecraft 1.20.1 (Fabric)**: blocks shatter into physical voxel debris, buildings that lose their supports collapse and smash on impact, and you get a sledgehammer, pipe bombs, nitro charges and a blowtorch to do it with.

**Requirements:** Minecraft 1.20.1, Fabric Loader ≥ 0.15, Fabric API, Java 17. Install on the client and on the server (both sides need it).

**Download:** grab `teardown_voxel-<version>.jar` from the [Releases](../../releases) page (built by GitHub Actions on every push to `main`).

## Features

### Progressive voxel damage and micro-debris
- Blocks destroyed by explosions, projectiles, the sledgehammer or the blowtorch break into `VoxelDebrisEntity` cubes (0.25–0.5 of a block). Each cube is textured with a random fragment of the original block's texture (biome-tinted for leaves and grass).
- Debris has gravity, air drag, bounce and ground friction, and collides with the world. Players can kick it around, and fast debris hurts mobs it hits. It settles flat onto a face and shrinks away after 15 s.
- **Progressive damage:** arrows and tridents, sledgehammer hits on hard blocks and the blowtorch build up damage on a block, shown with the vanilla crack overlay. When a block's hit points run out it shatters. Glass, ice and other fragile blocks break from one arrow, and the arrow keeps flying through.
- Mining with a pickaxe, axe, shovel or hoe knocks off a couple of chips.
- A global debris budget (`maxActiveDebris`) protects TPS.

### Structural integrity and collapse
- Whenever blocks are destroyed (explosions, tools, hand mining), every solid neighbour seeds a **BFS connectivity search** (default limit **256 blocks**).
- A structure counts as supported if it reaches natural ground (`#teardown_voxel:foundation`: stone, dirt, sand, ores, bedrock…), an unbreakable block, the world floor or an unloaded chunk, or if it's bigger than the search limit.
- Unsupported islands become `FallingStructureEntity` clusters. They creak and shed dust for a few ticks, fall together, crush whatever is underneath, and **shatter into debris on impact**. Some blocks drop their item and some stay behind as rubble. Containers break normally so you never lose their contents.
- Searches are time-sliced across server ticks (`bfsNodesPerTick`). That's the safe way to run them "asynchronously" in Minecraft: world access stays on the server thread and a big check never causes a lag spike.
- Leaves, plants, ladders and similar blocks aren't load-bearing (`#teardown_voxel:non_structural`). For example, cutting the bottom log of a tree drops the trunk.

### Tools and explosives
| Item | Use |
|---|---|
| **Sledgehammer** | 3D-modelled heavy tool. Right-click a block to smash a **3×3×1** area perpendicular to the face you hit, throwing fragments along your look direction. Blocks up to hardness 3 (wood, stone, bricks, deepslate) break instantly; harder ones crack progressively. Also a slow, heavy melee weapon (9 dmg). |
| **Pipe Bomb** | Right-click to throw it; it bounces with real physics while the fuse burns. Sneak + right-click a block to plant one with a longer fuse. |
| **Nitro Charge** | Placeable and shock-sensitive. Set it off with redstone, flint & steel or a fire charge, a projectile hit, a heavy landing on top of it, or any other explosion (chain reactions!). |
| **Blowtorch** | Hold right-click. It cuts through iron bars, doors, trapdoors and metal blocks (`#teardown_voxel:torchable`) in about a second with a spark shower, scorches other blocks slowly, and sets things in front of the nozzle on fire. |

The pipe bomb and nitro use a custom **raycasted shockwave**. Rays march outward and lose energy with distance and block blast resistance. Blocks a ray passes through are shredded into flying debris, and the crater walls where rays stop get **charred**: stone → cobblestone / blackstone, bricks → cracked bricks, logs → charred logs or coal blocks, planks → charred planks, grass → dirt, and leaves and wool burn away. The blast also leaves **thick lingering smoke** and a little fire.

Vanilla explosions (TNT, creepers, beds, fireballs, crystals) also throw debris, shake the screen and trigger collapse checks.

### Screen shake and audio-visual feedback
- Camera shake on explosions, structural collapses, heavy impacts and hammer blows, scaled by distance. It moves only the render camera, so your aim stays where it was.
- Custom particles: **sparks** (glowing, bouncing, cooling from white to orange), billowing **dust clouds**, and falling **pebbles**. Collapses also spawn falling-dust and block-break particles.

## Recipes
- **Sledgehammer:** `I B I / _ S _ / _ S _` (iron ingot, iron block, stick)
- **Pipe Bomb ×2:** `_ String _ / Iron Gunpowder Iron / _ Iron _`
- **Nitro Charge ×2:** `G B G / B TNT B / G B G` (gunpowder, blaze powder, TNT)
- **Blowtorch:** `_ Flint&Steel _ / Iron FireCharge Iron / _ Iron _`

## Configuration
`config/teardown_voxel.json` is created on first launch. Notable options:

| Option | Default | Meaning |
|---|---|---|
| `debrisLifetimeTicks` | 300 | Debris lifetime (15 s) |
| `debrisMinScale` / `debrisMaxScale` | 0.25 / 0.5 | Debris cube size |
| `maxActiveDebris` | 800 | Debris cap per dimension |
| `maxDebrisPerExplosion` | 160 | Debris cap per blast |
| `maxStructureSearch` | 256 | BFS limit before a structure is assumed supported |
| `bfsNodesPerTick` | 2048 | BFS work per server tick |
| `collapseOnPlayerBreak` | true | Hand mining can trigger collapses |
| `shatterDropChance` / `rubbleChance` | 0.6 / 0.2 | What happens to collapsed blocks on impact |
| `sledgehammerMaxHardness` | 3.0 | Instant-smash hardness limit |
| `pipeBombPower` / `nitroPower` | 3.2 / 5.0 | Explosive strength |
| `charring` | true | Scorch crater walls |
| `screenShakeMultiplier` | 1.0 | Set to 0 to disable shake (client) |

Block behaviour is driven by tags that datapacks can override: `teardown_voxel:foundation`, `non_structural`, `fragile`, `torchable` and `indestructible`.

## Building from source
```
./gradlew build
```
The jar lands in `build/libs/`. CI (`.github/workflows/build.yml`) builds every push and attaches the jar to the GitHub release for the current `mod_version`.

## License
MIT
