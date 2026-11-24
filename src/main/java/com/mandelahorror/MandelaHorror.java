package com.mandelahorror;

import com.mandelahorror.horror.HorrorCommands;
import com.mandelahorror.horror.HorrorDirector;
import com.mandelahorror.network.ModNetwork;
import com.mandelahorror.registry.ModEntities;
import com.mandelahorror.registry.ModItems;
import com.mandelahorror.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/**
 * Mandela Catalogue Horror - a fan-made analog horror mod for Fabric 1.20.1.
 */
public class MandelaHorror implements ModInitializer {
    public static final String MODID = "mandelahorror";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        HorrorConfig.load();

        ModSounds.register();
        ModEntities.register();
        ModItems.register();

        ModNetwork.registerServerReceivers();
        HorrorDirector.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                HorrorCommands.register(dispatcher));
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
