# Tweakable XP Loss

A small **server-side** [NeoForge](https://neoforged.net/) mod that lets you configure the percentage of
experience points a player keeps after death, overriding the default vanilla XP loss.

Players do **not** need this mod installed on their client — it runs entirely on the server.

## Supported versions

The mod ships as **three jars**, each compiled against its era's Minecraft/NeoForge APIs. Pick the one that
covers your Minecraft version:

| Jar | Minecraft | NeoForge |
|-----|-----------|----------|
| `tweakable_xp_loss-<version>-mc1.21.1.jar` | 1.21.1 – 1.21.10 | 21.1.x – 21.10.x |
| `tweakable_xp_loss-<version>-mc1.21.11.jar` | 1.21.11 | 21.11.x |
| `tweakable_xp_loss-<version>-mc26.2.jar` | 26.2 | 26.2.x-beta |

The 26.2 jar is published as a **beta** version: Minecraft 26.2 is a stable release, but NeoForge for 26.2
is itself still beta-only at the time of writing (no stable 26.2.x build yet). The mod's GameRules API on
26.x is identical to 1.21.11's, so the mod code is unchanged — only the compile target and Java toolchain
(25 vs 21) differ. It will flip to a release version once NeoForge 26.2 ships a stable build.

> **Why multiple jars?** Minecraft 1.21.11 restructured the `GameRules` API (the `keepInventory` gamerule moved
> package, changed type, and changed its accessor), and MC 26.x is a new major on top of that. Rather than rely
> on runtime reflection, each jar calls its era's API directly — compile-checked and obviously correct.
> See [`docs/PORTING.md`](docs/PORTING.md).

## Config

On first launch the mod creates `config/tweakable_xp_loss-common.toml`:

```toml
#Master switch. If false, the mod does nothing (pure vanilla XP loss).
enabled = true

#Percentage of experience points a player keeps through death (0-100).
# 100 = keep all XP (no loss).
# 0   = vanilla behavior (lose all XP).
# Range: 0 ~ 100
keepPercentage = 100

#How the kept XP is returned to the player:
# INSTANT = granted directly to the player on respawn (no orbs).
# ORBS    = dropped as experience orbs at the death location (recoverable; gravestone/corpse mods may collect them).
#Allowed Values: INSTANT, ORBS
awardMethod = "INSTANT"
```

Edit the file and run `/reload` (or restart the server) to apply changes.

### What "keepPercentage" means

It is a percentage of the player's **total accumulated experience points** (the raw XP behind the levels bar),
not a percentage of levels. For example, `keepPercentage = 50` means a player respawns with half of the XP
points they had before dying.

## How it works (under the hood)

1. When a player dies, vanilla normally scatters a small amount of XP as orbs at the death point
   (`min(level * 7, 100)` points) and resets the player's XP to 0 on respawn.
2. This mod:
   - suppresses that small vanilla orb drop (via `LivingExperienceDropEvent`) so retained XP isn't
     double-counted, **only when active**;
   - returns the retained portion either directly on respawn (`INSTANT`, via `PlayerEvent.Clone`) or as orbs
     at the death location (`ORBS`, via `LivingDeathEvent`).

## Compatibility with other death mods (CorPse, gravestones, …)

- This mod **never touches item drops**, so it is fully compatible out-of-the-box with corpse/gravestone
  mods that only handle items.
- When `keepPercentage = 0` (or `enabled = false`), this mod performs **no work at all** and vanilla
  behavior is fully preserved, so any other mod's XP handling is untouched.
- If you use a mod that **manages experience on death** (it stores your full XP in a grave and refunds it
  later), set `keepPercentage = 0` to avoid this mod double-awarding XP.

## Notes

- The mod is a no-op when the `keepInventory` gamerule is enabled (vanilla already preserves XP then).
- Builds with no client-side code; drop the built jar into the server's `mods/` folder.

## Building

Build the jar for a specific era (defaults to `legacy`):

```bash
./gradlew build                # legacy jar  -> build/libs/tweakable_xp_loss-<ver>-mc1.21.1.jar
./gradlew build -Pera=modern   # modern jar  -> build/libs/tweakable_xp_loss-<ver>-mc1.21.11.jar
./gradlew build -Pera=next     # next jar    -> build/libs/tweakable_xp_loss-<ver>-mc26.2.jar (Java 25)
```

Java 21 is required for the 1.21.x eras and Java 25 for the 26.x era; Gradle auto-downloads the needed
toolchain if it is not installed.

### Continuous integration & publishing

- **`build` workflow** (`.github/workflows/ci.yml`) compiles the mod against every targeted
  Minecraft/NeoForge version on each push/PR — the cross-minor jobs act as a tripwire for API drift.
- **`release` workflow** (`.github/workflows/release.yml`) triggers on a `v*` git tag: it builds all three
  jars, publishes each as a separate [Modrinth](https://modrinth.com/) version (via
  [Minotaur](https://github.com/modrinth/minotaur)), and attaches them to a GitHub Release.
  Requires a `MODRINTH_TOKEN` repository secret and an existing Modrinth project (slug `tweakable-xp-loss`).
