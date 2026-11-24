# Mandela Catalogue Horror (fan mod) — Fabric 1.20.1

A fan-made analog horror mod inspired by *The Mandela Catalogue*. Not affiliated with the original series.

## What's in it

| Feature | What happens |
|---|---|
| **Start screen** | The first time you join a world: color bars + emergency tone, then an **EMERGENCY ALERT SYSTEM** broadcast that types itself out and is **read aloud (text-to-speech)**, ending with *"hello, YourName."* |
| **Fake player joins** | `Name joined the game` (yellow, identical to vanilla), the name appears in the **TAB list**, and it talks in chat (`<Name> turn around`). If you answer in chat, it repeats you. Later: `Name left the game`. |
| **Your name** | Sometimes the fake player uses **YOUR name and YOUR skin** and holds the same item you hold. |
| **Mimic + skin randomizer** | The fake player walks around as a "player" with a random default skin (Steve, Alex, Ari, Kai, Noor, Sunny, Zuri, Efe, Makena) or your skin. It freezes when you look at it. Get close, hit it, or stare too long, and it turns into an Alternate. A **skin glitch** event makes every player's skin swap randomly for a few seconds. |
| **The Alternate (GeckoLib)** | Very tall, pitch black, long arms and fingers, **glowing white eyes with tiny black pupils**. It stares back and tilts its head when you look at it, creeps closer when you don't, teleports behind you, and **talks** (big shaky subtitles + text-to-speech voice). |
| **M.A.D. boss bar** | A red bar fills while an Alternate is near. Looking at it makes it fill MUCH faster: `M.A.D. // SIGNAL DETECTED` → `DO NOT LOOK AT IT` → `IT IS TALKING TO YOU - LOOK AWAY`. VHS static appears on screen as it gets closer. |
| **Self-stab** | When the bar is full, your hand moves on its own: it selects a **sword, pickaxe or axe** from your hotbar, your camera is forced down, you say `my hands won't stop`... and you **stab yourself** (damage = weapon damage × 1.25, ignores armor). **Escape:** switch to another hotbar slot before the stab lands. No weapon in the hotbar = jump scare instead. |
| **Night broadcasts** | Every nightfall, a M.A.D. advisory appears at the top of the screen. |
| **Alternate voice** | Your clip (`sounds/alternate_voice.ogg`) plays from the Alternate's position: when it first appears, every time you start looking at it, sometimes at random, when a mimic reveals itself, and right before the self-stab. Heard up to ~48 blocks. Turn off with `voice = false` in the common config. |
| **Strange Paper** | Right-click to open a paper screen: draw with **Pencil**, **Red Pen** or **Eraser** (3 brush sizes, right-click erases), and **Write Text** anywhere (click the paper, type, Enter = new line, right-click a text to delete it). **Undo** (Ctrl+Z), **Clear**, **Done**. Saved inside the item, and the tooltip shows what you wrote. The world keeps running while you draw... and sometimes words appear that you didn't write. |
| **Alternate's note** | Killing an Alternate drops a Strange Paper with two staring eyes, a red grin and a message with your name. |

## Build the .jar

You need a **JDK 17 or newer** installed.

Windows:
```
gradlew.bat build
```
Mac / Linux:
```
./gradlew build
```
The mod is in `build/libs/mandelahorror-fabric-1.0.0.jar`.

To test from the project directly: `gradlew runClient`

## Install

1. Install **Fabric Loader** for Minecraft **1.20.1** (0.16 or newer) from fabricmc.net.
2. Put these in your `mods` folder:
   - `mandelahorror-fabric-1.0.0.jar` (this mod)
   - **Fabric API** for 1.20.1 (e.g. `fabric-api-0.92.6+1.20.1.jar`)
   - **GeckoLib** for **Fabric 1.20.1**, version **4.4.9 or newer** (e.g. `geckolib-fabric-1.20.1-4.4.9.jar`)

## Test commands (need cheats / op)

| Command | Does |
|---|---|
| `/mandela intro` | Play the start screen again |
| `/mandela fakejoin` | A fake player joins now |
| `/mandela alternate` | Spawn the Alternate behind you |
| `/mandela stab` | Self-stab event now (Survival mode + sword/pickaxe/axe in hotbar) |
| `/mandela glitch` | Skin randomizer glitch for 10 seconds |
| `/mandela broadcast` | M.A.D. advisory |
| `/mandela voice` | Play the Alternate's voice clip |
| `/mandela note` | Get the Alternate's note |

There are also spawn eggs in the Spawn Eggs creative tab, and Strange Paper in Tools & Utilities.

**Crafting Strange Paper:** Paper + Coal (or Charcoal), shapeless.

**Change the voice clip:** replace `src/main/resources/assets/mandelahorror/sounds/alternate_voice.ogg` with any **mono .ogg** file and rebuild.

## Settings

`config/mandelahorror.properties` is created the first time you start the game. Every option has a comment:
grace time, how often fake players join, chance they use your name, TAB list on/off, Alternate night-only,
Alternate voice, M.A.D. bar, influence speed, self-stab on/off, damage multiplier, `canKill`,
text-to-speech, static overlay, skip intro with ESC, skin glitch. Edit it, save, restart.

Text-to-speech uses Minecraft's own narrator library, so it works even with the in-game Narrator option off. On Linux it needs `flite` installed.

## Project layout

```
src/main/java/com/mandelahorror/
  MandelaHorror.java        main mod class (Fabric ModInitializer)
  HorrorConfig.java         settings (config/mandelahorror.properties)
  entity/                   AlternateEntity (GeckoLib), MimicEntity
  item/                     StrangePaperItem, PaperData (drawing + text saved in NBT)
  horror/                   director, fake joins, self-stab, lines, commands, per-world save data
  network/                  packets (intro, speech, screen effects)
  client/                   MandelaHorrorClient (client entrypoint), intro screen, paper screen,
                            HUD overlay, TTS, renderers, client networking
  mixin/                    camera shake (GameRenderer) + skin randomizer glitch (PlayerRenderer)
src/main/resources/assets/mandelahorror/
  geo/alternate.geo.json                  GeckoLib model (open in Blockbench to edit)
  animations/alternate.animation.json     idle / walk / stare / attack
  textures/entity/alternate.png (+ _glowmask.png for the glowing eyes)
  sounds/                                 static, emergency tone, drone, whisper, beep, stab, heartbeat, alternate_voice
  textures/item/strange_paper.png
```

## Build the .jar on GitHub (no Java needed)

1. Create a new repository on github.com.
2. Upload **everything** in this folder, including the `.github` folder, `gradlew` and `gradle/wrapper/gradle-wrapper.jar`.
3. Open the **Actions** tab. The "Build mod jar" run starts by itself (or press **Run workflow**).
4. When it shows a green check, open the run and download **mod-jar** at the bottom. Unzip it: that's your mod `.jar`.

If the run shows a red X, open it, copy the error from the "Build" step, and send it to me.
