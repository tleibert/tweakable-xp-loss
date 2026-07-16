# Multi-version support plan (1.21.1 → newest, up to MC 26)

Status: **proposed, not yet implemented.** Current mod targets MC 1.21.1 / NeoForge 21.1.x only.
This file records the audit + plan so the work can be picked up later. No code changes yet.

> **Heads-up on versioning:** Mojang dropped the `1.` prefix for its newest major —
> Minecraft is now versioned `26.x` (newest releases: `26.2`, `26.1.2`, `26.1.1`, `26.1`, then
> `1.21.11`). NeoForge mirrors this with a `26.x` line. So "newer than 1.2x" = the 26.x
> series, which is currently **beta/alpha only**; the newest **stable** NeoForge is still
> `21.11.x` (MC 1.21.11).

## Scope

NeoForge release series after 1.21.1 (pulled from `maven.neoforged.net` `releases`).
NeoForge version `major.minor` == MC `1.21.minor.patch`.

| MC        | NeoForge series | target | notes                                   |
|-----------|-----------------|--------|-----------------------------------------|
| 1.21.1    | 21.1.x          | ✅ now  | current baseline (jar already ships)    |
| 1.21.3    | 21.3.x          | ✅      | metadata-only port                      |
| 1.21.4    | 21.4.x          | ✅      | metadata-only port                      |
| 1.21.5    | 21.5.x          | ✅      | metadata-only port                      |
| 1.21.8    | 21.8.x          | ✅      | metadata-only port                      |
| 1.21.10   | 21.10.x         | ✅      | 2 builds; minor                         |
| 1.21.11   | 21.11.x         | ✅      | newest; **one code change needed**      |
| 1.21.2 / 1.21.6 / 1.21.7 / 1.21.9 | — | ❌ | no **stable** NeoForge (beta-only); skipped |
| 26.1 / 26.2 | 26.1.0.0-alpha… / 26.2.0.x-beta | ⏸ later | MC's new major (post-1.21). NeoForge 26.x is **beta/alpha only** right now (140 builds, latest `26.2.0.9-beta`). API already audited = identical to 1.21.11 (see below). Add when it goes stable. |

## API audit (the foundation)

Verified against NeoForge patches/sources + Mojang mappings at 1.21.1, 1.21.4, 1.21.11.
Every API the mod touches is stable across the whole range **except one**:

| API used by the mod                                            | 1.21.1 | 1.21.4 | 1.21.11 |
|----------------------------------------------------------------|:---:|:---:|:---:|
| `Player#giveExperiencePoints(int)`                             | ✅ | ✅ | ✅ |
| `Player.totalExperience` field                                 | ✅ | ✅ | ✅ |
| `ExperienceOrb.award(ServerLevel, Vec3, int)`                  | ✅ | ✅ | ✅ |
| `LivingExperienceDropEvent` + `setDroppedExperience`           | ✅ | ✅ | ✅ |
| `PlayerEvent.Clone` + `isWasDeath()` + `getOriginal()`         | ✅ | ✅ | ✅ |
| `LivingDeathEvent` (fired via `CommonHooks.onLivingDeath` at start of `Player.die`) | ✅ | ✅ | ✅ |
| `NeoForge.EVENT_BUS` + `@SubscribeEvent` + `ModConfigSpec`     | ✅ | ✅ | ✅ |
| **`GameRules.RULE_KEEPINVENTORY`**                            | ✅ | ✅ | ❌ **renamed** |
| **26.x (beta)** — same APIs as 1.21.11                       | — | — | ✅ matches 1.21.11 |

### The sole breakage: 1.21.11 `GameRules` refactor

- constant `RULE_KEEPINVENTORY` → **`KEEP_INVENTORY`** (1.21.10 still has the old name; rename is 1.21.11-only).
- class package `net.minecraft.world.level.GameRules` → **`net.minecraft.world.level.gamerules.GameRules`**.
- accessor `getBoolean(GameRules.Key<Boolean>)` → **`get(GameRule<Boolean>)`** (returns `Boolean` directly).

Consequence: the package move means a 1.21.1-compiled jar that references `GameRules`
would `NoClassDefFoundError` on 1.21.11. **The GameRules reference is the one thing
standing between "1.21.1-only" and "one jar for all 1.21.x".**

The keep-inventory gamerule is registered by the **stable string id `"keepInventory"`**
in every version — that's the seam a version-proof design can use.

## Strategic decision: Path B (recommended)

Eliminate the direct `GameRules` reference so one source compiles once and runs on
1.21.1 through 1.21.11.

- **At respawn (`PlayerEvent.Clone`):** replace the keep-inventory gamerule check with
  "skip if the new player already has XP" (`newPlayer.totalExperience > 0`). This is
  version-proof *and* strictly more compatible — it also auto-skips when an XP-managing
  grave mod already restored XP before our handler, removing the double-award footgun.
  (XP-copy happens before `Clone` is posted, so this read is safe.)
- **At death (`LivingDeathEvent` ORBS spawn, `LivingExperienceDropEvent` suppression):**
  add a tiny `keepInventory(Player)` helper resolved by the stable string id
  `"keepInventory"` (reflection, death-time only, perf-irrelevant). The drop-suppression
  is harmless either way (no-op in keep-inventory), but the ORBS spawn must be gated so
  it doesn't over-grant in keep-inventory mode.
- Widen `mods.toml` ranges to cover the whole span.
- Keep a CI matrix that *compiles* against each version as a regression tripwire, but
  publish the single jar built against 1.21.1.

Path A (per-version branches) was considered and rejected: N branches for a 3-class mod
where only one line differs is more overhead than a sliver of reflection on a non-hot path.

## Concrete steps

1. **Remove the compile-time GameRules dependency**
   - Clone-time gate → `newPlayer.totalExperience > 0` skip.
   - Death-time gate → `keepInventory(Player)` helper via string id `"keepInventory"`
     (reflection). Verify the lookup path against the 1.21.11 decompiled `GameRules`
     source during implementation, and confirm it also works on 1.21.1.

2. **Widen version metadata**
   - `mods.toml`: `minecraft_version_range` → a range spanning 1.21.1–1.21.11 (or `[1.21.1,)`);
     `neo_version_range` stays `[21.1,)` (already loose).
   - Bump `mod_version` (e.g. 1.1.0) to signal multi-version support.

3. **CI build matrix (GitHub Actions)**
   - One job per target MC version (1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.8, 1.21.10,
     1.21.11), each overriding `minecraft_version`/`neo_version`/`parchment_*` and
     running `./gradlew build`.
   - Purpose: catch future API drift so the single-jar claim stays true.
   - Publish the 1.21.1-built jar as the release artifact; the other jobs are
     compile-only checks.

4. **Smoke-test the single jar across versions**
   - Boot `runServer` against 1.21.1, 1.21.11, and one mid-range (e.g. 1.21.4):
     confirm mod loads, TOML generates, and a `/kill` round-trip keeps XP at
     `keepPercentage = 100`. Validates the "one jar, many versions" claim empirically.

5. **Refresh README + compat notes**
   - State supported MC range and the one-jar-for-all behavior.
   - Update the grave-mod interaction note: the new `totalExperience > 0` skip means
     XP-managing grave mods no longer strictly require `keepPercentage = 0` at respawn
     (the mod yields if XP was already restored) — keep the escape hatch documented anyway.

6. **MC 26.x — beta now, ready when stable**
   - NeoForge 26.x (targets MC 26.1/26.2, Mojang's new major) is currently beta/alpha
     only (`26.2.0.9-beta` latest). **Already audited:** its XP APIs (`giveExperiencePoints`,
     `totalExperience`, `ExperienceOrb.award`) and the renamed `GameRules.MOB_DROPS` /
     `KEEP_INVENTORY` are **identical to 1.21.11** — i.e. the new major adds *no* breakage
     beyond the 1.21.11 GameRules refactor. So the Path B jar (once GameRules is decoupled)
     covers 26.x too.
   - When 26.x goes stable: add a CI matrix row, smoke-test `runServer` against it, widen
     `minecraft_version_range` to include `26.x`. No code change expected.

## Build-side facts (already confirmed)

- `net.neoforged.moddev` plugin: latest **2.0.141** (works for all target versions).
- MDK templates exist per MC version at `github.com/NeoForgeMDKs/MDK-1.21.x-ModDevGradle`
  (and `MDK-26.x-ModDevGradle` for the new major).
- NeoForge userdev jars publish per-version `patches/` (used for the audit above),
  including the 26.x betas.

## Risks / watch-items

- **Reflection target stability:** depends on the string id `"keepInventory"` and the
  `GameRules` accessor shape staying reachable. Mitigated by the CI matrix recompiling
  against each version; if a future version reshapes the accessor, the matrix fails loudly
  before release.
- **Cross-version byte compat:** rests on the non-GameRules classes keeping their packages
  and method descriptors (audited stable). The CI matrix is the ongoing guarantee.
- **Clone-ordering vs grave mods:** the new `totalExperience > 0` skip assumes vanilla/another
  mod has already set XP by the time `Clone` fires (true — XP-copy happens before `Clone` is
  posted). Worth a comment + a test alongside any grave mod the user runs.
- **1.21.11 is very new (1 build).** Pin the CI to 21.11.42 and treat it as moving; re-audit
  if point releases shift the GameRules API again.
