# Stumble — GTA IV style ragdolls for Minecraft 1.20.1 (Fabric)

Bodies don't just fall over; they react.

- **Push (R key).** Your character brings both hands up to the chest and shoves with both arms;
  other players see it too. Whoever is in front of you gets shoved: from standing they usually
  stumble a few steps and catch their balance, while a running shove (sprint + R) knocks them
  flat about two times in three. Once down they lie there for a moment, then get back up through
  a crouch. Pushing a body that is already on the ground kicks it. Dead bodies can be kicked too.
- **Walk into people.** Walking into a zombie or skeleton bumps it and it stumbles out of your
  way. Sprinting into one knocks it down about a third of the time, and a sprint-jump into one
  always does. Sneak past to avoid bumping.
- **You fall, you ragdoll.** Drop more than 4 blocks, or get thrown by an explosion or a big hit,
  and you go limp in mid-air: arms flailing, bracing for the landing. The camera switches to third
  person, you can't move or act until your body comes to rest, then you get up and the camera
  switches back. Mobs do the same.
- **Bodies float.** Bodies in water float at the surface with arms and legs hanging, and drift
  with the current. In lava they sink in further.
- **Deaths.** When a player or humanoid mob dies, the body staggers, tries to keep its balance,
  braces, and goes limp instead of tipping over and turning red. Arrows hit harder than fists;
  explosions launch bodies, including ones already lying on the ground.
- Bodies collide with blocks (stairs, ledges, walls), have elbows and knees with joint limits,
  and can be shoved around by walking through them. Dead bodies fade after 45 seconds.

Works on players (classic and slim skins), zombies, husks, drowned, skeletons, strays and
wither skeletons. Other mobs get an ordinary shove when pushed and keep their vanilla death.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft **1.20.1**.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) for 1.20.1 in your `mods` folder.
3. Put `stumble-1.2.0.jar` in the same `mods` folder.

**Single player:** everything works.

**Multiplayer:** install it on the server *and* on each player's game for pushing, mob knockdowns,
and seeing each other ragdoll. With the mod only on your own game you still get death ragdolls,
your own fall ragdoll, floating bodies and the push animation for yourself, but pushing and
bumping won't do anything to living things.

Change the push key in Options → Controls → Key Binds → "Stumble (ragdolls)".

## Build the jar

You need a computer with internet and Java 17 or newer (Java 21 is fine).

    ./gradlew build          (Windows: gradlew.bat build)

The mod is then at `build/libs/stumble-1.2.0.jar`. The first build downloads Minecraft and
takes a few minutes.

No computer handy? Upload this folder to a new GitHub repository. The included workflow
builds it automatically: open the repository's **Actions** tab, open the latest run, and
download `stumble-mod`.

## Settings

`config/stumble.json` is created on first launch. Edit it and restart the game.

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch for everything drawn |
| `players` | `true` | Ragdoll players, you included |
| `mobs` | `true` | Ragdoll the supported mobs |
| `maxRagdolls` | `24` | Dead bodies allowed at once (oldest goes first) |
| `lifetimeSeconds` | `45` | How long a dead body stays |
| `force` | `1.0` | How hard killing blows and explosions throw bodies |
| `impactSounds` | `true` | Thud when a body lands |
| `push` | `true` | The push key works |
| `pushStrength` | `1.0` | How hard you push (sprinting pushes 1.6× harder) |
| `pushPlayers` | `true` | Pushing other players knocks them about |
| `bump` | `true` | Walking or running into mobs bumps them |
| `bumpStrength` | `1.0` | How hard bumps are |
| `mobKnockdowns` | `true` | Living mobs can be knocked down by pushes, big hits and falls |
| `fallRagdoll` | `true` | Long falls turn you and mobs into ragdolls |
| `fallRagdollHeight` | `4.0` | How many blocks of falling that takes |
| `knockbackRagdoll` | `true` | Being thrown hard ragdolls you and mobs |
| `knockbackRagdollSpeed` | `0.65` | How sudden a change of speed counts as "thrown" (lower = more often) |
| `ragdollCamera` | `true` | Third person while you are a ragdoll |

On a server, the server's file decides `push`, `pushStrength`, `pushPlayers`, `bump`,
`bumpStrength` and `mobKnockdowns`; each player's own file decides the rest for them.

## Known limits

- Armour and held items are not drawn on bodies.
- Baby mobs, villagers, piglins, endermen and other shapes are not ragdolled.
- A downed player's hitbox stays standing-sized, so in a one-block gap the body may slide
  further than the player can follow; the body is then pulled back to the player.
- If a mob is knocked into water it gets up and swims rather than staying limp.

## How it is put together

- `physics/Ragdoll.java` – the whole simulation, with no Minecraft imports: 14 particles,
  distance and hinge constraints, sphere-versus-box collision, buoyancy, and the brain:
  stagger → catch balance or fall → brace → limp → get up.
- `server/ServerRagdolls.java` – pushes, and knocked-down mobs: the server runs the body and
  drags the mob along under it with ordinary movement, so it stays where the body is.
- `client/RagdollManager.java` – death bodies, your own ragdoll (it drives your player and
  locks your controls), and copies of other people's and mobs' bodies.
- `client/RagdollModel.java`, `client/RagdollRenderer.java` – the vanilla humanoid cut at
  elbows and knees, drawn with the entity's own texture.
- `client/PushAnimation.java` – the two-handed shove, blended over the normal player pose.
- `mixin/` – hides the vanilla model while a ragdoll stands in for it, stops a downed entity
  from walking or thinking, poses the arms for the push, and listens for explosions.
