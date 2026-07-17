package com.tweakablexp;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;

/**
 * Era-specific bridges to two GameRules/level accessors that moved between MC 1.21.x eras.
 * This variant is compiled against NeoForge 21.1.x (MC 1.21.1&ndash;1.21.10).
 *
 * <p>The modern-era twin (NeoForge 21.11.x / MC 1.21.11+) lives in {@code src/modern/java}.
 * Both share the package + class name so the shared {@link DeathHandler} calls a single,
 * version-stable seam; exactly one is compiled into a given jar (Gradle {@code -Pera}).
 */
final class KeepInventoryCompat {
    private KeepInventoryCompat() {
    }

    /** Legacy GameRules API: {@code GameRules.Key<BooleanValue>} + {@code getBoolean(Key)}. */
    static boolean keepInventory(ServerPlayer player) {
        return player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
    }

    /** Legacy ServerLevel accessor: {@code ServerPlayer#serverLevel()}. */
    static ServerLevel serverLevel(ServerPlayer player) {
        return player.serverLevel();
    }
}
