package com.tweakablexp;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Handles the XP-retention logic.
 *
 * <h2>Design</h2>
 * <ul>
 *   <li>{@link LivingExperienceDropEvent} suppresses the small vanilla player-death orb drop
 *       ({@code min(level*7, 100)} points) when this mod is active, so retained XP is not double-counted.</li>
 *   <li>{@link Config.AwardMethod#INSTANT INSTANT}: the retained amount is granted directly to the
 *       respawned player in {@link PlayerEvent.Clone}.</li>
 *   <li>{@link Config.AwardMethod#ORBS ORBS}: the retained amount is dropped as experience orbs at the
 *       death location in {@link LivingDeathEvent}, recoverable by returning there (gravestone/corpse
 *       mods that collect nearby orbs will pick it up).</li>
 * </ul>
 *
 * <h2>Compatibility with other death mods</h2>
 * When {@code keepPercentage == 0} or {@code enabled == false}, this mod performs <em>no</em> work and
 * vanilla behavior (including any corpse/gravestone mod handling) is fully preserved. If you use a mod
 * that manages experience on death (storing your XP in a grave and refunding it later), set
 * {@code keepPercentage} to {@code 0} to avoid conflicts.
 *
 * <p>This mod never touches item drops, so it is fully compatible out-of-the-box with corpse/gravestone
 * mods that only handle items.
 */
public final class DeathHandler {
    private DeathHandler() {
    }

    /** True when the mod should actively retain XP for the given player's death. */
    private static boolean activeFor(ServerPlayer player) {
        if (!Config.ENABLED.get()) {
            return false;
        }
        int keep = Config.KEEP_PERCENTAGE.get();
        if (keep <= 0 || keep > 100) {
            return false;
        }
        // keepInventory already preserves XP and suppresses the vanilla drop; nothing to do.
        // The keep-inventory read is delegated to an era-specific KeepInventoryCompat so the
        // shared code never compiles against a particular GameRules API shape.
        return !KeepInventoryCompat.keepInventory(player);
    }

    /** Retained XP points for a given total at the configured percentage. */
    private static int retainedAmount(int totalXp) {
        int keep = Config.KEEP_PERCENTAGE.get();
        return Math.max(0, (int) Math.round((double) totalXp * (double) keep / 100.0));
    }

    /**
     * Suppress the vanilla player-death XP orb drop (which is at most {@code min(level*7, 100)} points)
     * whenever this mod is retaining XP, so the kept portion is not also scattered as orbs.
     *
     * <p>If a corpse/gravestone mod already suppressed the drop via {@code skipDropExperience()}, this
     * event does not fire at all, so we do not interfere.
     */
    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (!activeFor(player)) {
            return;
        }
        event.setDroppedExperience(0);
    }

    /**
     * ORBS mode: drop the retained XP as experience orbs at the death location.
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!activeFor(player) || Config.AWARD_METHOD.get() != Config.AwardMethod.ORBS) {
            return;
        }
        int retained = retainedAmount(player.totalExperience);
        if (retained <= 0) {
            return;
        }
        ServerLevel level = KeepInventoryCompat.serverLevel(player);
        ExperienceOrb.award(level, player.position(), retained);
    }

    /**
     * INSTANT mode: grant the retained XP directly to the respawned player.
     *
     * <p>{@link PlayerEvent.Clone} fires during {@code ServerPlayer#restoreFrom}. On a normal death
     * respawn (keepInventory off), vanilla has not copied XP to the new player, so it starts at 0 and we
     * grant the retained amount here. The dead player's {@code totalExperience} is still the full
     * pre-death value at this point.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        Player newPlayer = event.getEntity();
        if (!(newPlayer instanceof ServerPlayer sp)) {
            return;
        }
        if (!activeFor(sp) || Config.AWARD_METHOD.get() != Config.AwardMethod.INSTANT) {
            return;
        }
        int retained = retainedAmount(event.getOriginal().totalExperience);
        if (retained <= 0) {
            return;
        }
        newPlayer.giveExperiencePoints(retained);
    }
}
