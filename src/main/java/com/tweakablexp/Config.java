package com.tweakablexp;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Config for Tweakable XP Loss.
 *
 * <p>NeoForge persists this as a TOML file at {@code config/tweakable_xp_loss-common.toml}.
 * Editing that file (and running {@code /reload} or restarting) is the supported way to tweak values.
 *
 * <h2>Compatibility with other death mods (CorPse, gravestones, ...)</h2>
 * If you run a mod that already manages experience on death (it stores your XP in a grave/corpse and refunds
 * it later), set {@link #ENABLED} to {@code false} (or {@link #KEEP_PERCENTAGE} to {@code 0}) so this mod
 * becomes a complete no-op and does not double-award experience. When {@code keepPercentage == 0} this mod
 * performs no work and vanilla behavior is fully preserved.
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Master switch. If false, the mod does nothing (pure vanilla XP loss).")
            .define("enabled", true);

    public static final ModConfigSpec.IntValue KEEP_PERCENTAGE = BUILDER
            .comment(
                    "Percentage of experience points a player keeps through death (0-100).",
                    " 100 = keep all XP (no loss).",
                    " 0   = vanilla behavior (lose all XP).",
                    "Interacting mods that manage XP on death should set this to 0 to avoid conflicts."
            )
            .defineInRange("keepPercentage", 100, 0, 100);

    public static final ModConfigSpec.EnumValue<AwardMethod> AWARD_METHOD = BUILDER
            .comment(
                    "How the kept XP is returned to the player:",
                    " INSTANT = granted directly to the player on respawn (no orbs).",
                    " ORBS    = dropped as experience orbs at the death location (recoverable; gravestone/corpse mods may collect them)."
            )
            .defineEnum("awardMethod", AwardMethod.INSTANT);

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** How the retained XP is delivered back to the player. */
    public enum AwardMethod {
        /** Grant the retained XP directly to the respawned player. No orbs are spawned for the retained portion. */
        INSTANT,
        /** Spawn experience orbs carrying the retained XP at the death location. */
        ORBS
    }

    private Config() {
    }
}
