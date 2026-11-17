package com.thunderscience.changedtsc.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/** Client-only start-up. Only ever touched through DistExecutor, so a dedicated server never loads it. */
public final class ClientInit {
    private ClientInit() {}

    /** Makes the "Config" button in the Mods list open the TSC settings screen. */
    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new TSCSettingsScreen(parent)));
    }
}
