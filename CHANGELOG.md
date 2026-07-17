# Changelog

All notable changes to **Tweakable XP Loss** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html) (with a per-era Minecraft
classifier suffix on each release artifact).

## [1.1.0] - 2026-07-15

### Added
- **Multi-version support.** The mod now ships as two jars, each compiled against its era's
  direct GameRules API:
  - `tweakable_xp_loss-1.1.0-mc1.21.1.jar` — Minecraft **1.21.1–1.21.10** (NeoForge 21.1.x–21.10.x).
  - `tweakable_xp_loss-1.1.0-mc1.21.11.jar` — Minecraft **1.21.11** (NeoForge 21.11.x).
  Pick the jar whose range covers your Minecraft version. See the README for details.
- Version metadata (`mods.toml`) now declares the correct per-era Minecraft and NeoForge ranges.

### Changed
- `mod_version` bumped `1.0.0` → `1.1.0`.
- The keep-inventory gamerule read and the `ServerLevel` accessor were extracted into an
  era-specific `KeepInventoryCompat` seam, so the shared death/respawn logic is identical
  across both jars. This is "Path C" of the multi-version plan (see `docs/PORTING.md`).
- The legacy seam reads the gamerule via `((ServerLevel) player.level()).getGameRules()` rather
  than `player.level().getGameRules()`: `getGameRules()` moved from `Level` to `ServerLevel` at
  the 1.21.2/1.21.3 boundary, so calling it on `ServerLevel` is what makes one legacy jar
  binary-compatible across 1.21.1–1.21.10. The CI tripwire matrix recompiles the legacy source
  against 1.21.3/1.21.4/1.21.5/1.21.8/1.21.10 on every push to guard this.

### Notes
- Minecraft 26.x (NeoForge 26.x) is **not yet targeted** — it is still in beta and requires
  Java 25. Its GameRules API is identical to 1.21.11, so a third jar (or widening the modern
  jar's range) is straightforward once 26.x goes stable.

## [1.0.0] - initial release

- Server-side NeoForge mod for Minecraft 1.21.1: configurable percentage of XP kept through
  death, delivered either instantly on respawn (`INSTANT`) or as experience orbs at the death
  location (`ORBS`). File config at `config/tweakable_xp_loss-common.toml`.
