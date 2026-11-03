package com.latexdrone;

import com.latexdrone.command.LatexCommand;
import com.latexdrone.infection.InfectionManager;
import com.latexdrone.registry.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LatexDroneMod implements ModInitializer {
    public static final String MODID = "latexdrone";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static Identifier id(String path) {
        return new Identifier(MODID, path);
    }

    @Override
    public void onInitialize() {
        ModSounds.init();
        ModEffects.init();
        ModEntities.init();
        ModBlocks.init();
        ModItems.init();
        ModItems.initSpawnEggs();
        ModItemGroup.init();
        ModEntities.initSpawns();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                InfectionManager.tick(player);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> InfectionManager.onJoin(handler.player));
        ServerPlayerEvents.COPY_FROM.register(InfectionManager::copyFrom);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> InfectionManager.sync(newPlayer));
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> LatexCommand.register(dispatcher));

        LOGGER.info("Latex Drone Infection loaded: {} latex forms, {} scientists", ModEntities.LATEX_TYPES.size(), ModEntities.SCIENTIST_TYPES.size());
    }
}
