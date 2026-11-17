package com.thunderscience.changedtsc;

import com.mojang.logging.LogUtils;
import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.registry.ModBlockEntities;
import com.thunderscience.changedtsc.registry.ModBlocks;
import com.thunderscience.changedtsc.network.TSCNetwork;
import com.thunderscience.changedtsc.registry.ModCreativeTabs;
import com.thunderscience.changedtsc.registry.ModEffects;
import com.thunderscience.changedtsc.registry.ModEntities;
import com.thunderscience.changedtsc.registry.ModItems;
import com.thunderscience.changedtsc.util.EntityResolver;
import com.thunderscience.changedtsc.client.ClientInit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Changed: Thunder Science Expansion.
 * An add-on for "Changed: Minecraft Mod" (ltxprogrammer) that adds the Thunder Science Company:
 * human guards / scientists / assimilation staff, guns, armor, first aid, sneaking stealth,
 * friendly Puro, four latex bosses and 63 world-gen structures.
 */
@Mod(ChangedTSC.MODID)
public class ChangedTSC {
    public static final String MODID = "changed_tsc";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ChangedTSC() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        TSCNetwork.register();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TSCConfig.SPEC, "changed_tsc-common.toml");
        // "Config" button in the Mods list opens the TSC settings screen (client only)
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientInit::registerConfigScreen);
        modBus.addListener(this::onConfigLoad);
        modBus.addListener(this::onConfigReload);
    }

    private void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == TSCConfig.SPEC) EntityResolver.invalidate();
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == TSCConfig.SPEC) EntityResolver.invalidate();
    }
}
