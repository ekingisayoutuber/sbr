# Steel Ball Run (Fabric, Minecraft 1.21.1)

Stand users, Spin, and monsters that most people can't see.

## Build

Needs **Java 21**.

```
./gradlew build
```

The mod jar lands in `build/libs/` (use the one without `-sources`). Put it in your
`mods` folder with **Fabric Loader 0.16+** and **Fabric API 0.116.x+1.21.1**.
To test in-dev instead: `./gradlew runClient`.

> This project was written without being able to compile it (the workspace could
> not reach Fabric's servers). If the build reports an error, it will most likely be
> a wrong method name or import for 1.21.1. Paste the error back and it is
> usually a one-line fix.

## Getting started

1. Craft a **Stand Arrow** (arrow + amethyst shard + diamond) and right-click it.
   You get Tusk or D4C at random.
2. Craft a **Steel Ball** (5 iron ingots in a plus shape).

## Controls (rebind under Controls > Steel Ball Run)

| Key | Action |
|-----|--------|
| R | Summon / dismiss Stand |
| G | Cycle Tusk Act / D4C ability |
| V | Use Stand ability |
| B | Spin (hold a Steel Ball) |
| Right-click Steel Ball | Throw it |

## Spin

With a Steel Ball in hand, press B:
- Tap: build Spin charge (and Spin mastery). At 100 you get **Golden Rotation**
  (Strength + Speed).
- Sneak + B: **Spin Heal** (40 charge).
- Sprint + B: **Spin Dash** (30 charge).

Charge slowly drains. Spin mastery is permanent and unlocks Tusk's Acts.

## Tusk

| Act | Unlocks at mastery | Ability |
|-----|-----|---------|
| 1 | 0 | Nail Shot |
| 2 | 15 | Spiral Burst (pierces everything in line) |
| 3 | 40 | Wormhole Nail (goes through walls, Wither) |
| 4 | 80 | Infinite Rotation (needs 100 Spin charge, huge damage) |

## D4C

1. **Dimension Slip**: slip forward through walls (up to 10 blocks).
2. **Love Train**: heavy damage resistance and absorption.
3. **Parallel Swap**: swap places with whatever you are looking at.

## Who can see what

Stand silhouettes, Stand attacks and the phantom monsters below are only ever sent to
players who own a Stand. Everyone else sees nothing. Stand data is also only synced to
its own player, so nobody can tell who is a Stand user.

## Monsters

- **Rotting Horror**: a few zombies/skeletons/husks spawn as 60-HP brutes that cloud
  nearby players' vision with Darkness and play a heartbeat. Everyone sees them.
- **Phantom Stand**: a few spawn fully invisible, fast and hard-hitting. Non-users just
  get hit by something unseen. Stand users see a spectral silhouette.

## Art note

Stands are drawn as particle silhouettes, so there are no custom models. Item textures are
16x16 originals in `assets/steelballrun/textures/item`; swap them for your own art any
time.

## Getting the jar without installing Java

1. Create a new GitHub repository and upload everything in this folder (keep the `.github` folder).
2. Open the repo's **Actions** tab. The `build` workflow runs on every push.
3. When it finishes, open the run and download the `steelballrun-jar` artifact.
4. Unzip it and use the jar that does not end in `-sources.jar`.
