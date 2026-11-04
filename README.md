# SCP Foundation — Fabric mod for Minecraft Java 1.20.1

*Secure. Contain. Protect.*

## What's inside

### The SCP Archive: every number from SCP-001 to SCP-9999
- A creative tab called **SCP Archive (001-9999)** holds a personnel file for each of the 9,999 SCP numbers.
- Right-click a file to read it in chat. Each file has a clickable link to the full article on the SCP Wiki.
- About 80 well-known SCPs have a nickname, an object class and a short summary written for this mod. The other files are marked `[REDACTED]` and still link to the wiki.
- A **Classified Document** (found in loot chests) decrypts into a random SCP file when you use it.
- Commands:
  - `/scp info <n>` prints a file.
  - `/scp list` lists the anomalies that exist in the world.
  - `/scp give <n>` gives you a file (operators only).

### Playable SCPs (each one has its own behaviour)
| SCP | What it does in-game |
|---|---|
| **173** | Can't move while any player is looking at it, even through containment glass. Nearby players blink every few seconds, and that's when it moves. Its hits do very heavy damage. |
| **096** | Docile until you look at its face. Then it screams for 5 seconds and hunts you down, smashing through walls, until you're dead. |
| **049** | Slow and talks to you in chat. Its touch withers. Anything it kills (players, villagers, illagers) rises again as **SCP-049-2**. |
| **106** | Drifts straight through walls toward you, leaving corrosion behind. Its grab can pull you into the **Pocket Dimension**. |
| **682** | A huge reptile that regenerates, adapts to each damage type you use on it, smashes walls, and comes back to life the first time it dies. |
| **939** | Blind, so it hunts by sound: sneak to stay hidden. It imitates other players' voices in chat. |
| **999** | Friendly orange blob. It follows you and heals you; right-click it for a hug. |
| **131** | Eye pods (orange A and blue B) that follow you and make nearby hostile mobs glow. |
| **529** | Josie the half-cat. Feed her fish. |
| **1048** | Builder Bear. Hurt it and it builds hostile little copies of itself. |
| **018** | A super ball you can throw. It speeds up with every bounce and breaks glass. |
| **066** | Eric's Toy: a ball of yarn that keeps asking for "Eric" and sometimes lets out a deafening blast of sound. |
| **1471** | The "MalO ver1.0.0" phone. Use it to install the app, and from then on a skull-faced figure (SCP-1471-A) keeps appearing behind you. Use the phone to take photos. Uninstalling always fails; only SCP-500 removes it. |
| **012** | A Bad Composition: an unfinished score that pulls nearby players in and hurts them. Wear a carved pumpkin as a blindfold to resist it. |
| **2521** | Type "2521" in chat and something takes an item stack from your inventory. |
| **008** | The zombie plague. SCP-049-2 bites and zombie players spread it. Infected players get fever stages, and when it runs out they turn into SCP-049-2. SCP-500 cures it. |
| **035** | The possessive mask. Wear it for strength, resistance and haste, and hostile mobs back off. But it whispers to you, rots you slowly, and can't be taken off (SCP-500 pries it loose). |
| **963 / 178 / 2295** | An amulet that cheats death once every 5 minutes; 3-D glasses that make every living thing glow; a patchwork bear that heals hurt players and sews out SCP-008. |
| **457** | The Burning Man: a living fire that sets you and the ground ablaze. It heals in flames and is hurt by water. |
| **017** | The Shadow Person: fast and deadly in the dark, but bright light burns it away. |
| **966** | Sleep Killers: invisible unless you have night vision (SCP-178, SCP-035 or a potion). Their claws drain you. |
| **650** | The Startling Statue: when nobody's watching, it appears right behind you. |
| **073 / 076** | Cain reflects any damage back at the attacker, and plants wither around him. Able is a regenerating warrior armed with a netherite sword. |
| **079 / 261 / 610** | Old AI (insults you and plays with iron doors); a vending machine that sells snacks from other worlds; a flesh growth that slowly spreads. |
| **148 / 109** | A Telekill helmet that blocks SCP-012, SCP-1471 and SCP-939's voices; a canteen that never runs dry. |
| **1025 / 006** | An encyclopedia that gives you the disease you read about, and bottled water that heals. |

### SCP items and blocks
- **SCP-914**: sneak + use to change the setting (Rough / Coarse / 1:1 / Fine / Very Fine). Use it while holding an item to refine the whole stack. It upgrades keycards, ores, tools, armour and more.
- **SCP-294**: pay with an iron nugget to get a random drink. Rename the nugget in an anvil ("coffee", "tea", "liquid courage", "antimatter"…) to order a specific drink.
- **SCP-330**: take only two pieces of candy. A third piece costs you the use of your hands (SCP-500 cures it).
- **SCP-458**: a pizza box that never runs out.
- **SCP-1162**: put an item in and pull out a random one. Reaching in empty-handed hurts.
- **SCP-500**: cures every effect and restores full health.
- **SCP-207**: stacking speed boost that slowly poisons you.
- **SCP-005**: opens any door, including iron doors and keycard readers.
- **SCP-427**: heals you, but too many uses have side effects.
- **SCP-714**: hold it in your off-hand for immunity to poison and wither, at the cost of drowsiness.
- **SCP-268**: wear it and you become invisible, and mobs lose interest in you.
- **SCP-063**: breaks wood, leaves, wool and plants instantly.
- **Keycards, Level 1–5 plus O5**, and **Keycard Readers, Level 1–5**. A reader opens nearby iron doors for 4 seconds. You can craft a Level 1 card (paper + iron nugget + redstone); SCP-914 on Fine upgrades cards.
- Building blocks: Reinforced Concrete, Facility Wall, Facility Floor, Hazard Stripes, Containment Glass, Foundation Emblem.

### Zombie mode: play as SCP-049-2
Two game rules (both on by default):
- `/gamerule scpDeadPlayersRise true|false`: when a player dies, an SCP-049-2 with their name rises where they fell.
- `/gamerule scpPlayableZombies true|false`: if you're killed by SCP-049, SCP-049-2 or a zombie player, or die from SCP-008, you respawn where you died **as SCP-049-2**.
  - As a zombie you wear a rotting face, get Strength, Resistance and Night Vision, and never get hungry. The undead and SCP-049 leave you alone.
  - Every hit you land infects your target with SCP-008, and villagers you kill rise as SCP-049-2.
  - SCP-500 turns you back into a human.
- Operator commands: `/scp zombify [player]` and `/scp cure [player]`.

### SCP-106's Pocket Dimension
A dark, endless corroded floor that slowly withers you. Walk more than 40 blocks from the centre: 60% of the time you escape back near where you were taken, otherwise the corridor loops you back to the middle.

### 99 generated structures
All of them are single-piece jigsaw structures that spawn on land in the Overworld. They're split into 11 structure sets, so you'll find roughly one every 150–250 blocks.

- **25 containment and anomaly sites**: cells for 173, 096, 049, 106, 682, 939, 999, 131, 529 and 1048 (each SCP is inside); rooms for 914, 294, 330, 458 and 1162; secure lockers for 500, 207, 018, 005, 427, 714, 268 and 063; the SCP-087 stairwell (about 44 blocks deep, with something waiting at the bottom); and the SCP-3008 furniture store.
- **74 Foundation facilities and hostile-group sites**:
  - Research labs, archives, armories, medical bays and D-Class barracks
  - Heavy containment zones (random hostile SCPs in cells) and light containment zones (safe SCPs and item lockers)
  - Checkpoints and Gates A/B guarded by iron golems; helipads, watchtowers and outposts
  - Underground bunkers reached by a ladder shaft, plus the O5 Council bunker
  - Ruined sites, Chaos Insurgency camps (pillagers), GOC outposts (vindicators), Serpent's Hand hideouts
  - MTF bases (Epsilon-11, Nu-7, Alpha-1), comms relays and power stations

Find them with `/locate structure scpfoundation:<name>`, `/locate structure #scpfoundation:containment`, `/locate structure #scpfoundation:facilities` or `/locate structure #scpfoundation:all`.

Chests use 18 custom loot tables: keycards, documents, SCP items, weapons and medical supplies.

---

## Download
A ready-built jar is published as a GitHub Release on every push. Each build is first boot-tested on a real dedicated server.
https://github.com/Game124554/Game124554/releases/tag/scp-foundation-latest

## Building the mod (.jar)

You need **Java 17** (or newer) and an internet connection.

```bash
# Windows
gradlew.bat build
# macOS / Linux
./gradlew build
```

The jar ends up in `build/libs/scp-foundation-1.0.0.jar`. Copy it to `.minecraft/mods/` along with **Fabric Loader 0.14.21+** and **Fabric API (0.92.x for 1.20.1)**.

No Java setup? Push the folder to a GitHub repository. The workflow in `.github/workflows/build.yml` builds the jar automatically; download it from the **Actions** tab.

To test in development: `./gradlew runClient`

> Testing tip: SCP-096 and SCP-173 ignore players in Creative mode, so test them in Survival.

## Regenerating art and structures
All textures, models, the 99 structure `.nbt` files, the loot tables and the world-gen JSON are generated by Python scripts. You need Python 3 with Pillow.
```bash
python3 tools/gen_assets.py          # textures, models, blockstates, lang, recipes, Java model data
cd tools && python3 gen_structures.py # 99 structures + loot tables + worldgen + pocket dimension
python3 check_nbt.py && python3 check_resources.py   # validation
```
To change the structures, edit the catalogue in `build_all()` in `gen_structures.py`.

## Licence and attribution
The SCP Foundation concepts and names come from the **SCP Wiki** (https://scp-wiki.wikidot.com). They're licensed under **Creative Commons Attribution-ShareAlike 3.0** (CC BY-SA 3.0), so this mod is released under the same licence. See `ATTRIBUTION.md`. All textures and models in this mod are original pixel art, and all summaries are original paraphrases.
