# Tweakable XP Loss

A small **server-side** [NeoForge](https://neoforged.net/) mod for **Minecraft 1.21.1** that lets you configure
the percentage of experience points a player keeps after death, overriding the default vanilla XP loss.

Players do **not** need this mod installed on their client — it runs entirely on the server.

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

```bash
./gradlew build
```

The mod jar is written to `build/libs/tweakable_xp_loss-1.0.0.jar`.
