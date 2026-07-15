package com.tweakablexp;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TweakableXpLoss.MODID)
public final class TweakableXpLoss {
    public static final String MODID = "tweakable_xp_loss";

    public TweakableXpLoss(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        NeoForge.EVENT_BUS.register(DeathHandler.class);
    }
}
