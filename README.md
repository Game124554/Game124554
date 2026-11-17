# Changed: Thunder Science Expansion (Forge 1.20.1)

An add-on for **Changed: Minecraft Mod** (ltxprogrammer) that adds the Thunder Science Company (TSC).

- **Minecraft** 1.20.1, **Forge** 47.x
- **Requires** the Changed mod (1.20.1, v0.15+) for latex, Puro and the latex bosses
- Mod id: `changed_tsc`

## Features

### People: 3 human types × 20 faces × white/black outfits
| Mob | Behaviour |
|---|---|
| **TSC Guard** | Security armor and a pistol, rifle or shotgun. Shoots hostile latex and monsters. Friendly to players unless attacked. |
| **TSC Scientist** | White or black lab coat. Runs from latex. Uses first aid on hurt players, humans and Puro nearby. |
| **Assimilation Staff** | Jumpsuit with a hazard stripe and a Stun Baton or Purifier Blade. Fights latex in melee. |

Each human gets one of 20 face/hair variants and a random white or black outfit, which gives 120 different textures.

### Friendly Puro
Puro never targets players or TSC humans, follows the nearest player, and attacks hostile latex and monsters. TSC humans never shoot him.

- Puro appears in the tower and other structures, or use `/tsc puro` to spawn one where you stand.
- If an installed mod has an entity with `puro` in its id (for example Changed Addon Plus), that entity is used and all of its kind are friendly.
- Otherwise a dark latex wolf from Changed is used. Only that one wolf is friendly: it is named **Puro** and carries the tag `changed_tsc_friendly`. Other dark latex wolves stay hostile.
- You can make any mob friendly yourself with `/tag <mob> add changed_tsc_friendly`.

### Weapons, armor, medical
- **3D guns**: the pistol, assault rifle and shotgun are full 3D models (see `guns_3d.png`), in your hand, in first person, in the inventory, on the ground and in item frames. In third person you hold the gun up and aim it (pistol with straight arms, long guns shouldered), and guards do the same.
- **TSC Pistol / Assault Rifle / Shotgun**: right-click to fire; they use **TSC Bullets** (12 from 1 copper ingot + 1 gunpowder). Bullets do bonus damage to latex.
- **Stun Baton**: slows and weakens.
- **Thunder Purifier Blade**: ×2 damage vs latex. It's crafted from a boss core, a diamond sword and a lightning rod.
- **Security armor** (black) and **Hazmat suit** (white). Wearing the full Hazmat suit halves latex damage.
- **First Aid Kit** (heals 6 hearts, cures harmful effects, gives regeneration) and **Bandage**. Hold right-click to use one on yourself, or right-click another player or mob to treat them.

### Goo infection: Goo Inside Body → Furry Body → Transfur
Each hit from a hostile latex puts **goo inside your body** (+8% per hit, double from bosses, halved by a full Hazmat suit).
| Stage | Effect icon | What happens |
|---|---|---|
| 1 · **Goo Inside Body** (below 34%) | dark drop | Still a human body. Cold goo moves under your skin, with some hunger. Your body slowly fights it off. |
| 2 · **Furry Body** (34–66%) | fur tuft | Fur and goo spread over your arms, legs and torso, and other players can see it. Slowness and weakness. **The goo now spreads by itself.** |
| 3 · **Transfurring** (67–99%) | red ring | Almost fully covered. Slowness II, weakness II, nausea, darkness. It spreads fast. |
| **100%** | | **Transfur.** The mod runs Changed's `/transfur` command for you. If that fails, the fallback is used: by default you die to the goo and a latex appears in your place. |

**Cures:** First Aid Kit (−50%), Bandage (−12%), **Anti-Goo Syringe** (removes all of it, causes nausea). You can also right-click another player or a TSC human to treat them. Scientists treat infected people nearby. First aid also tries to lower Changed's own transfur progress with `/regresstf`.
TSC guards, scientists and staff get infected too. At 100% they turn into a latex.
The goo is black, or white if a white latex infected you. `/tsc goo <targets> [percent]` checks or sets infection.

> **How the form is chosen:** the default command is `transfur {player} {form}`. `{form}` is the form of the latex that infected you; the add-on tries the different ways Changed spells its form ids until one is accepted. To always become one form, type `/transfur ` in game, use tab-complete to see the exact ids, and set `gooInfection.transfurCommand`, for example `transfur {player} changed:form_dark_latex_wolf/male`.

### Paper doll: your body at a glance
A small body figure, in the style of the First Aid mod's health figure, shows where the goo is. See `paper_doll.png` for every state.

| What you see | Meaning |
|---|---|
| Skin-colored part | Human body, clean |
| Skin with a dark (or white) stain | Goo inside that part |
| Black or white part with fur | That part is furry |
| **Paw print** on a hand or foot | The hand or foot has become a paw |
| Ears, tail and paws on the whole doll | You are transfurred (read from the Changed mod) |

The goo starts in the hands, then moves up the arms, into the legs, the body and last the head. The hands are marked as paws the moment the Furry Body stage starts.

- **On the HUD:** the doll appears while there is goo in your body or you are transfurred, with the infection percent under it.
- **Body screen (key G):** a big doll plus three buttons that use a Bandage, First Aid Kit or Anti-Goo Syringe from your inventory to **clear the goo** without holding the item. Hover a body part to read its state ("Right hand: paw").
- The doll also shows the Changed mod's own transfur progress when that is higher than the goo infection.
- With the First Aid mod installed, its health figure sits top-left; this doll sits middle-left by default so both are visible. Position and size are on the **Doll** page of the settings menu.

### Stealth
While **sneaking**, latex only notice you within 4 blocks (less in the dark). A latex that is already chasing you loses track once you sneak far enough away. Bosses ignore stealth by default.

### Bosses: four fights
The Squid Dog and Tiger Shark fights follow how the Changed wiki describes them in the original game. How the Wolf King and The Elder One fight in the original could not be confirmed, so those two use the same style (marked attacks you dodge, one rule to learn).

| Boss | Where | Summon item | How the fight works |
|---|---|---|---|
| **Squid Dog** | Kaiju Paradise (deep tank) | Ink-Stained Beacon | Tentacles chase you: a ring of ink marks the floor, and a second later the tentacle comes down there. **Make it hit itself:** stand next to the Squid Dog when a tentacle lands and it takes the hit and is stunned. At 70% and 35% health other squid dogs arrive with metal plates; while a **shield bearer** lives the boss takes only 20% damage. It holds a captured Tiger Shark and lets it go when enraged. |
| **Tiger Shark** | Aquatic Research Station 3 | Shark Tooth Lure | He calls **two lines of sharks**, one from each side, so don't stand in the middle. Then his fin cuts the water and he **swoops**; every swoop brings one more shark that homes in on you. **After the third swoop he is worn out** for a few seconds and takes double damage. He heals in water and is slower and weaker on dry floor. |
| **Wolf King** | Thunder Science Company Tower (top floor) | Howling Crown | He howls for a pack of dark latex wolves and takes half damage while **two or more wolves** stand with him. Between howls he crouches and **pounces on the marked spot** where you stood. Enraged, he adds a shockwave. |
| **The Elder One** | Laminax Underground (atrium) | Elder Seal | White latex **bursts out of the floor** under every player (white ring, then the hit), a pulse of darkness lifts you off your feet, it regenerates and calls other latex. Enraged, it **absorbs its helpers to heal**, so don't leave them alive. |

Each boss is built on the matching Changed entity, so it uses Changed's own model and texture. The boss bar turns **yellow** while the boss is protected and **green** while it is wide open. Bosses become enraged below 50% health, and drop a **Core**, diamonds, first aid kits and ammo.

### Structures (63)
- **Thunder Science Company Tower**: 16 floors (lobby, armory, offices, labs, medical, cafeteria, containment, server room, assimilation lab, executive vault, Wolf King arena) plus a rooftop helipad, a ladder core and a lightning-bolt emblem. About 120 blocks tall.
- **Kaiju Paradise** (cameo): an abandoned tropical park with a lagoon, booths, palms and a dome. Squid Dog sleeps in the tank below.
- **Laminax Underground** (cameo, abandoned): three flooded levels around an atrium, with The Elder One at the bottom.
- **60 more**: 10 research outposts, 8 abandoned labs, 6 guard watchtowers, 5 security checkpoints, 7 underground research bunkers, 6 containment wings, 5 aquatic research stations, 5 supply depots, 4 field medical stations, 2 comm relays, 1 helipad and 1 latex quarantine camp.

**Furniture from Changed.** The rooms are furnished with the Changed mod's own blocks: lab tables with microscopes, petri dishes, clipboards and book stacks; desks with computers and office chairs; tables with chairs in cafeterias, lobbies and dorms; cardboard boxes, box piles, iron crates and speakers; IV racks beside beds. There are about 1,050 pieces across the 63 structures (231 in the tower).

The structure files don't name Changed's blocks directly. Each piece is a marker ("a computer facing south goes here") that finds the matching Changed block by name when the area loads and places it the way a player would. If Changed has no block for a kind, a vanilla stand-in is used (slab for a table, stairs for a chair, barrel for a box), so rooms are never empty. `/tsc resolve` lists which Changed block each kind found; to pick a different one, edit `furniture.kinds` in the config, for example `computer=changed:computer`.

Use `/locate structure changed_tsc:thunder_science_tower` (or any name in `STRUCTURES.txt`) to find them.
Mobs inside structures appear when a player comes near, using invisible spawn markers.

## Settings menu
Three ways to open the **Thunder Science Company Settings** screen:
- the **TSC Settings** button in the top-left corner of the title screen and the pause menu,
- the settings key (default **O**, change it under Options → Controls → Key Binds),
- **Mods → Changed: Thunder Science Expansion → Config**.

The screen has seven pages of switches and sliders:

| Page | What you can change |
|---|---|
| **Mobs** | No Vanilla Monsters, No Vanilla Bosses, Latex Hunt Humans, Humans Fight Monsters, Scientists Heal, Guns Need Ammo, Changed Furniture, Menu Button |
| **Stealth** | Sneak Stealth on/off, Works On Bosses, notice range, sneak visibility, darkness bonus |
| **Goo** | Goo Infection on/off, symptoms, who can be infected, body overlay, goo per hit, stage thresholds, spread speed |
| **First Aid** | First Aid items on/off, heal amounts, goo removed, and the four **First Aid mod** switches |
| **Puro** | Friendly Puro on/off, follows you, fights for you, fights back |
| **Doll** | Paper doll on HUD, show when clean, mark paws, Changed progress, position, size, open the body screen |
| **Bosses** | Bosses on/off, max minions, health of each boss |

Every change is written to `config/changed_tsc-common.toml`, so the menu and the file always match. In single player the change applies at once. On a multiplayer server the server's config file decides the rules.

## First Aid mod support (optional)
If the **First Aid** mod (mod id `firstaid`) is installed, the First Aid page shows "First Aid mod: installed" and four switches become active:

| Switch | What it does |
|---|---|
| **First Aid Mod Link** | Master switch for everything below. TSC kits heal with the normal heal call, which First Aid spreads over your hurt body parts (if First Aid's own config allows other healing items). |
| **Its Items In Loot** | First Aid's bandages, plasters and morphine appear in TSC structure chests and drop from scientists and bosses. The item list is `firstAidMod.lootItems` in the config. |
| **Morphine Stops Goo** | While First Aid's morphine works, the goo stops spreading and you don't feel its symptoms. |
| **Health: Mod Only** | TSC First Aid Kits and Bandages only remove goo; health is healed only with First Aid's own items. |

The add-on does not need First Aid to run. Without it these four switches are greyed out.

## Settings file — `config/changed_tsc-common.toml`
| Section | Setting | Default |
|---|---|---|
| `vanillaMobs` | `removeVanillaHostiles` (removes zombies, skeletons, creepers…) | `false` |
| | `removeVanillaBosses` (Ender Dragon, Wither, Elder Guardian, Warden) | `false` |
| | `whitelist` | `[]` |
| `stealth` | `enabled`, `sneakNoticeRange`, `sneakVisibility`, `darknessRangeMultiplier`, `worksAgainstBosses` | on, 4, 0.3, 0.6, off |
| `gooInfection` | `enabled`, `infectionPerHit`, stage thresholds, spread/recovery speed, `symptoms`, `bodyOverlay`, cure amounts, `transfurCommand`, `regressCommand`, `transfurFallback` (`kill_and_spawn` / `spawn_only` / `none`) | on, 8, 34/67 … |
| `firstAid` | `enabled`, `kitHeal`, `bandageHeal`, `kitUseTicks`, `cooldownTicks`, `curesHarmfulEffects` | on, 12, 4, 40, 100, on |
| `firstAidMod` | `enabled`, `addItemsToLootAndDrops`, `lootItems`, `morphineHoldsBackGoo`, `healthOnlyFromFirstAidMod` | on, on, bandage/plaster/morphine, on, off |
| `menu` | `showMenuButton` | on |
| `furniture` | `useChangedBlocks`, `kinds` (which Changed block each kind of furniture uses) | on, by name |
| `paperDoll` | `showOnHud`, `showWhenClean`, `position`, `scale`, `markPaws`, `showChangedProgress` | on, off, `middle_left`, 2, on, on |
| `friendlyPuro` | `enabled`, `friendlyLatex`, `friendlySpawnEntity`, `followPlayers`, `fightHostiles`, `retaliateWhenHit` | on, `auto:puro`, Puro or a dark latex wolf, on, on, off |
| `weapons` | damages, cooldowns, `gunsRequireAmmo`, latex multipliers, hazmat reduction | |
| `npcs` | guard damage, scientists heal, latex hunt humans, `structureLatexPool` | |
| `bosses` | per boss: `entity`, `minions`, `health`, `attackDamage` | |

**Entity specs:** `auto:keyword` means "every Changed entity whose id contains the keyword". You can also write an exact id (`changed:some_entity`) or a list of fallbacks such as `auto:squid_dog|minecraft:drowned`. Because entities are matched by keyword, the add-on keeps working if Changed renames its entities.

## Commands (op)
- `/tsc resolve`: shows which Changed entities were found for Puro, each boss, and the structure latex pool, and which Changed block each kind of furniture uses. **Run this first** to check everything is linked up.
- `/tsc boss <squid_dog|tiger_shark|elder_one|wolf_king>`: summons a boss where you stand.
- `/tsc puro`: spawns a friendly Puro where you stand.
- `/tsc goo <targets> [percent]`: shows or sets goo infection (for testing the stages).
- `/tsc reload`: re-scans entities after you edit the config.

## Getting the .jar

### On GitHub (no Java needed on your computer)
The project includes `.github/workflows/build.yml`, which makes GitHub build the jar for you.

**Easiest way, two uploads:**
1. On github.com press **+ → New repository**, give it a name (for example `ChangedTSC`) and create it.
2. In the new repository press **Add file → Upload files**, drop in `ChangedTSC-forge-1.20.1-src.zip`, and press **Commit changes**.
3. Press **Add file → Create new file**. As the file name type `.github/workflows/build.yml`, paste the contents of `build.yml` into the box, and press **Commit changes**.
4. Open the **Actions** tab. A run called "Build mod jar" starts by itself and takes about 5 to 10 minutes the first time.
5. When it shows a green tick, open **Releases → "Changed: Thunder Science Expansion - latest build"** and download `changed_tsc-1.0.0.jar`. (The same jar is also under the run's **Artifacts**.)

If the run shows a red cross, the same Releases entry has `build-log.txt`; the lines with `error:` are what need fixing.

**If you use git or GitHub Desktop:** put the *contents* of this folder (so `gradlew` and `.github` are at the top of the repository) in a repository and push. The same workflow runs on every push.

### On your own computer
You need **JDK 17** (Temurin works) and an internet connection the first time.

```
./gradlew build          (Windows: gradlew.bat build)
```
The mod jar is written to `build/libs/changed_tsc-1.0.0.jar`.

### Installing
Put the jar in `.minecraft/mods` next to the Changed jar, with Forge 1.20.1 (47.x).

To test in a dev client with Changed loaded, uncomment the `runtimeOnly ... curse.maven:changed-minecraft-mod-661527:<fileId>` line in `build.gradle` and run `./gradlew runClient`.

## Project layout
```
src/main/java/com/thunderscience/changedtsc/
  boss/      BossType, BossManager (stats, boss bar, abilities, minions, loot)
  block/     SpawnMarkerBlock(+Entity) – invisible structure spawners
  client/    renderers, TSCSettingsScreen (settings menu), PaperDoll + PaperDollHud + GooBodyScreen (paper doll), keys
  compat/    FirstAidCompat – optional First Aid mod link; ChangedCompat – reads "am I transfurred" from Changed
  config/    TSCConfig – every setting
  entity/    guards, scientists, staff, bullets, follow-player goal
  event/     forge events (stealth, friendly Puro, vanilla removal, damage), /tsc commands
  item/      guns, first aid, blades, armor materials, boss items
  registry/  DeferredRegisters
  util/      EntityResolver (auto: lookups), LatexHelper
src/main/resources/data/changed_tsc/structures/*.nbt   63 structure templates
```

## Regenerating structures / textures
The structures, textures and 3D gun models were generated by the Python scripts in `tools/` (Python 3 + Pillow; the gun script also needs numpy):
```
cd tools && python gen_structures.py ..  &&  python gen_assets.py ..  &&  python gen_gun_models.py ..
```
Each gun is a list of boxes at the top of `gen_gun_models.py`. You can also open `models/item/tsc_pistol.json` (or the rifle or shotgun) in Blockbench to reshape it or to adjust how it sits in the hand.
Change sizes, palettes, loot and spawns there, then re-run to rebuild every `.nbt` and texture.
