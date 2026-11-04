package net.scpfoundation;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import net.scpfoundation.registry.ModBlockEntities;
import net.scpfoundation.registry.ModBlocks;
import net.scpfoundation.registry.ModCommands;
import net.scpfoundation.registry.ModEffects;
import net.scpfoundation.world.ZombiePlayers;
import net.scpfoundation.registry.ModEntities;
import net.scpfoundation.registry.ModEvents;
import net.scpfoundation.registry.ModItemGroups;
import net.scpfoundation.registry.ModItems;
import net.scpfoundation.world.PocketDimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SCP Foundation mod for Minecraft 1.20.1 (Fabric).
 * SCP Foundation concepts come from the SCP Wiki (https://scp-wiki.wikidot.com), licensed CC BY-SA 3.0.
 */
public class ScpFoundation implements ModInitializer {
    public static final String MOD_ID = "scpfoundation";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModEffects.register();
        ModBlocks.register();
        ModEntities.register();
        ModItems.register();
        ModBlockEntities.register();
        ModItemGroups.register();
        ModCommands.register();
        ModEvents.register();
        PocketDimension.register();
        ZombiePlayers.register();
        LOGGER.info("SCP Foundation initialised: Secure. Contain. Protect.");
    }
}
