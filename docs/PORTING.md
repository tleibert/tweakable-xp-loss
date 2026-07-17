# Multi-version support plan & record (1.21.1 → newest, up to MC 26)

Status: **implemented (Path C — two jars, direct API per era).** Covers Minecraft **1.21.1–1.21.11**
via two jars; MC 26.x is deferred until it leaves beta.

> **Heads-up on versioning:** Mojang dropped the `1.` prefix for its newest major —
> Minecraft is now versioned `26.x` (newest betas: `26.2`). NeoForge mirrors this with a `26.x` line.
> The newest **stable** NeoForge is still `21.11.x` (MC 1.21.11).

## Scope

NeoForge release series after 1.21.1 (pulled from `maven.neoforged.net` `releases`).
NeoForge version `21.minor` == MC `1.21.minor`.

| MC        | NeoForge series | status  | notes                                   |
|-----------|-----------------|---------|-----------------------------------------|
| 1.21.1    | 21.1.x          | ✅ jar   | legacy era compile target               |
| 1.21.3    | 21.3.x          | ✅ tripwire | source-compiled in CI; covered by legacy jar |
| 1.21.4    | 21.4.x          | ✅ tripwire | source-compiled in CI; covered by legacy jar |
| 1.21.5    | 21.5.x          | ✅ tripwire | source-compiled in CI; covered by legacy jar |
| 1.21.8    | 21.8.x          | ✅ tripwire | source-compiled in CI; covered by legacy jar |
| 1.21.10   | 21.10.x         | ✅ tripwire | source-compiled in CI; covered by legacy jar |
| 1.21.11   | 21.11.x         | ✅ jar   | modern era compile target               |
| 1.21.2 / 1.21.6 / 1.21.7 / 1.21.9 | — | ❌ | no **stable** NeoForge (beta-only); skipped |
| 26.1 / 26.2 | 26.x beta        | ⏸ later | MC's new major; **beta only**. Needs Java 25. APIs match 1.21.11 (verified). Add when stable. |

The **legacy jar** (`*-mc1.21.1.jar`, compiled against NeoForge 21.1.235) declares
`minecraft_version_range=[1.21.1,1.21.11)` and `neo_version_range=[21.1,21.11)` and is published for
MC 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.8, 1.21.10. The **modern jar** (`*-mc1.21.11.jar`, compiled
against NeoForge 21.11.44) declares `[1.21.11,1.22)` / `[21.11,)` and is published for MC 1.21.11.

## API audit — verified against decompiled sources

Every API the mod touches was checked against the real Mojang-mapped source (decompiled via the
`neoformruntime` pipeline) at **1.21.1**, **1.21.11**, and **26.2**. Two surfaces moved between eras;
everything else is stable across the whole range.

### Seam 1 — `keepInventory` gamerule

| | 1.21.1 (legacy) | 1.21.11 / 26.2 (modern) |
|---|---|---|
| holder class | `net.minecraft.world.level.GameRules` | `net.minecraft.world.level.gamerules.GameRules` |
| `getGameRules()` declared on | `Level` | **`ServerLevel` only** (not `Level`) |
| `player.level()` static return | `Level` (from `Entity`) | `ServerLevel` (covariant override on `ServerPlayer`) |
| constant | `GameRules.RULE_KEEPINVENTORY` : `Key<BooleanValue>` | `GameRules.KEEP_INVENTORY` : `GameRule<Boolean>` |
| registered id string | `"keepInventory"` | `"keep_inventory"` (snake_case) |
| accessor | `getBoolean(Key)` → `boolean` | `get(GameRule)` → `Boolean` (raw value) |
| `Key` / `Value` / `BooleanValue` / `getRule` | present | **removed** (rules are now registry-backed `GameRule<T>` in `BuiltInRegistries.GAME_RULE`) |

> **Correction of the earlier audit:** this is *not* a "3-line rename." 1.21.11 replaced the entire
> `Key`/`Value` wrapper structure with a registry-backed `GameRule<T>` whose `get()` returns the value
> directly, moved the class to a new package, moved `getGameRules()` off `Level` onto `ServerLevel`
> (compensated by `ServerPlayer.level()` becoming covariant), and snake_cased the id. A single
> reflection chain targeting `getRule(Key)`→`Value#get()` (the old Path B design) does **not** work on
> 1.21.11 — those methods no longer exist. Verified directly from source.

### Seam 2 — `ServerLevel` accessor

| | 1.21.1 (legacy) | 1.21.11 / 26.2 (modern) |
|---|---|---|
| `ServerPlayer.serverLevel()` | `ServerLevel` ✓ | **removed** |
| `ServerPlayer.level()` | `Level` (inherited) | `ServerLevel` (covariant override) |

Used by the ORBS spawn (`ExperienceOrb.award(ServerLevel, Vec3, int)`).

### Stable across the whole range

`Player#giveExperiencePoints(int)`, `Player.totalExperience` field, `ExperienceOrb.award(ServerLevel,
Vec3, int)`, `LivingExperienceDropEvent` + `setDroppedExperience`, `PlayerEvent.Clone` +
`isWasDeath()` + `getOriginal()`, `LivingDeathEvent`, `NeoForge.EVENT_BUS` + `@SubscribeEvent` +
`ModConfigSpec`, `Player#position()`. 26.2's `GameRules.java` differs from 1.21.11's only in
unrelated constructors/codec helpers — every seam above is identical.

## Strategic decision: Path C (implemented)

Two jars, each compiled against its era's **direct** GameRules/level API. No reflection, no runtime
era-detection — each jar is compile-checked against the API it ships for.

- The shared death/respawn logic lives in `src/main/java` (`DeathHandler`, `Config`,
  `TweakableXpLoss`) and never references `GameRules` or `serverLevel()` directly.
- The two era-specific seams (`keepInventory(ServerPlayer)`, `serverLevel(ServerPlayer)`) live in a
  per-era `KeepInventoryCompat` class with the same package + FQN:
  - `src/legacy/java/.../KeepInventoryCompat.java` — `RULE_KEEPINVENTORY` + `getBoolean(...)`;
    `player.serverLevel()`.
  - `src/modern/java/.../KeepInventoryCompat.java` — `KEEP_INVENTORY` + `get(...)`; `player.level()`.
- The Gradle build is parameterized by `-Pera=legacy|modern` (default `legacy`): it selects the
  NeoForge version, the `mods.toml` version ranges, the jar classifier, and which era source dir is
  folded into `sourceSets.main`. Exactly one `KeepInventoryCompat` is compiled per jar.

### Why C over the reflection approach (Path B)

Path B (one jar, runtime reflection over the gamerule) was the original plan. Source verification
against 1.21.11 and 26.2 changed the recommendation:

- The GameRules restructure is bigger than the earlier audit implied (see "Correction" above). A
  version-proof reflection helper would need **two branches** (old `getRule`/`Value#get` chain vs new
  `get(GameRule)`), era-detection, and reflective entry through `player.level().getClass()` because
  `Level.getGameRules()` no longer exists on 1.21.11. ~40–50 lines on a death-time path.
- Reflection breakage is **runtime** — the CI compile matrix does not catch it; only per-version
  smoke-tests would. C's per-era direct API is **compile-verified per era by construction**, so the
  CI matrix genuinely catches drift before release.
- C's per-era code is 3 lines × 2 methods, arguably simpler than the 2-branch reflection. The cost is
  two publish artifacts per release — acceptable for a 3-class mod with a single version-sensitive
  seam.

## Concrete steps (done)

1. **Extract the era seam** — `KeepInventoryCompat` (legacy + modern variants); `DeathHandler.activeFor`
   now takes `ServerPlayer` and delegates both seams. `onExperienceDrop` binds `instanceof ServerPlayer`
   (server-side deaths only); `onPlayerClone` binds `ServerPlayer sp`. ✅
2. **Parameterize the build** — `build.gradle` `eraConfig` map + `-Pera`; per-era `mods.toml` ranges via
   the existing `generateModMetadata` template; `jar { archiveClassifier = cfg.classifier }`. ✅
3. **Bump version** — `mod_version` 1.0.0 → 1.1.0; `CHANGELOG.md` added. ✅
4. **CI matrix** — `.github/workflows/ci.yml` compiles legacy against 21.1.235 + tripwires
   21.3.97/21.4.157/21.5.98/21.8.54/21.10.64, and modern against 21.11.44. ✅
5. **Publish pipeline** — `.github/workflows/release.yml` on `v*` tags runs `./gradlew modrinth` per era
   (Minotaur 2.9.0), each publishing a distinct Modrinth version, then creates a GitHub Release with
   both jars. ✅
6. **Verify** — both eras compile; bytecode inspected (`javap`) per jar to confirm each references its
   era's `GameRules` class/field/accessor; both boot a dedicated server (1.21.1 + 1.21.11) with the mod
   loaded, config generated, no errors. ✅
7. **Docs** — README + this file updated. ✅

## MC 26.x — beta now, ready when stable

NeoForge 26.x targets MC 26.1/26.2 and is **beta only** (`26.2.0.19-beta` latest). It requires
**Java 25** (FancyModLoader 11 needs JVM 25+), unlike the 1.21.x line (Java 21). Verified against the
26.2 decompiled source: every seam above is **identical to 1.21.11** — the new major adds *no* breakage
beyond the 1.21.11 refactor. So when 26.x goes stable:

- Add a third era to `eraConfig` (e.g. `next`) targeting `26.x` with Java 25 toolchain, or widen the
  modern jar's range if cross-major binary compat holds (needs a smoke-test — *not* assumed).
- Add a CI row and a `modrinth` publish for the new era.
- No `DeathHandler` or `KeepInventoryCompat` logic change expected (the modern `KeepInventoryCompat`
  already uses the 1.21.11-shaped API that 26.x shares).

## Build-side facts

- `net.neoforged.moddev` plugin: **2.0.141** (works for all 1.21.x targets; 26.x also resolves with it
  given a Java 25 toolchain).
- Minotaur (Modrinth publish): **2.9.0**, plugin id `com.modrinth.minotaur`.
- NeoForge versions: 21.1.235 / 21.3.97 / 21.4.157 / 21.5.98 / 21.8.54 / 21.10.64 / 21.11.44 / 26.2.0.19-beta.

## Risks / watch-items

- **Cross-minor binary compat (legacy jar):** the legacy jar is compiled against 21.1.235 but claims
  1.21.1–1.21.10. This rests on the non-GameRules APIs keeping identical descriptors across those
  minors (audited stable) and the legacy GameRules API being present through 1.21.10 (it is — the break
  is 1.21.11-only). The CI tripwires recompile the source against each minor as a regression guard;
  if a future point release shifts a descriptor, the tripwire fails loudly.
- **1.21.11 is very new.** Pin the modern era to 21.11.44 and re-audit if point releases shift the
  GameRules API again.
- **Grave-mod interaction is unchanged** from 1.0.0: set `keepPercentage = 0` if an XP-managing grave
  mod refunds XP, to avoid double-awarding. The `totalExperience`-based skip from the earlier Path B
  draft was *not* adopted (Path C keeps the explicit keepInventory check at both death and respawn).
