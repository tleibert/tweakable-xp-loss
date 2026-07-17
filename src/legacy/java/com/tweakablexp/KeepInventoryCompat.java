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

    /**
     * Legacy GameRules API: {@code GameRules.Key<BooleanValue>} + {@code getBoolean(Key)}.
     *
     * <p>{@code getGameRules()} lives on {@code Level} in 1.21.1 but moved to {@code ServerLevel}
     * at 1.21.2/1.21.3 (it is gone from {@code Level} there). To stay source- and binary-compatible
     * across the whole legacy era (1.21.1&ndash;1.21.10) we call it on {@code ServerLevel} via a
     * downcast: on 1.21.1 {@code ServerLevel} inherits {@code getGameRules()} from {@code Level};
     * on 1.21.3+ it declares it directly. {@code player.level()} is statically {@code Level}
     * throughout 1.21.1&ndash;1.21.10 (it only becomes covariant {@code ServerLevel} on 1.21.11),
     * so the cast is a legal downcast everywhere in this era, and the resulting bytecode references
     * {@code ServerLevel.getGameRules()} which exists at runtime on every targeted version.
     */
    static boolean keepInventory(ServerPlayer player) {
        return ((ServerLevel) player.level()).getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
    }

    /**
     * Legacy ServerLevel accessor. {@code ServerPlayer#serverLevel()} exists on 1.21.1 but was
     * removed by 1.21.11; its removal point within the legacy era is not pinned down. The downcast
     * {@code (ServerLevel) player.level()} is valid across the whole legacy era ({@code player.level()}
     * is statically {@code Level} through 1.21.10, and a {@code ServerPlayer} is always in a server
     * world at runtime), so we use it for stability instead of the version-gated {@code serverLevel()}.
     */
    static ServerLevel serverLevel(ServerPlayer player) {
        return (ServerLevel) player.level();
    }
}
