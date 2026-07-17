package com.tweakablexp;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Era-specific bridges to two GameRules/level accessors that moved between MC 1.21.x eras.
 * This variant is compiled against NeoForge 21.11.x (MC 1.21.11+).
 *
 * <p>On 1.21.11 the GameRules API was restructured: rules are registry-backed
 * {@code GameRule<T>} constants (id snake_case {@code "keep_inventory"}), and
 * {@code GameRules#get(GameRule)} returns the value directly (the old {@code Key}/{@code Value}
 * wrappers, {@code getRule}, and {@code getBoolean} were removed). {@code ServerPlayer#serverLevel()}
 * was also removed &mdash; {@code ServerPlayer#level()} now returns {@code ServerLevel} covariantly,
 * which is how the ServerLevel is obtained here.
 *
 * <p>The legacy-era twin (NeoForge 21.1.x / MC 1.21.1&ndash;1.21.10) lives in {@code src/legacy/java}.
 * Both share the package + class name so the shared {@link DeathHandler} calls a single,
 * version-stable seam; exactly one is compiled into a given jar (Gradle {@code -Pera}).
 */
final class KeepInventoryCompat {
    private KeepInventoryCompat() {
    }

    /** Modern GameRules API: {@code GameRule<Boolean>} + {@code get(GameRule)} (returns Boolean). */
    static boolean keepInventory(ServerPlayer player) {
        return player.level().getGameRules().get(GameRules.KEEP_INVENTORY);
    }

    /** Modern ServerLevel accessor: {@code ServerPlayer#level()} (covariant, returns ServerLevel). */
    static ServerLevel serverLevel(ServerPlayer player) {
        return player.level();
    }
}
