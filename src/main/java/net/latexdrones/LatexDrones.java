package net.latexdrones;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.latexdrones.infection.InfectionManager;
import net.latexdrones.infection.ModCommands;
import net.latexdrones.registry.ModBlocks;
import net.latexdrones.registry.ModEffects;
import net.latexdrones.registry.ModEntities;
import net.latexdrones.registry.ModItems;
import net.latexdrones.registry.ModSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Latex Drone Infection - main (common) entry point.
 *
 * <p>Content tables live in tools/content.py; {@code MobDefs} and {@code ModBlocks} are generated from it.
 */
public class LatexDrones implements ModInitializer {
    public static final String MOD_ID = "latexdrones";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModSounds.init();
        ModEffects.init();
        ModBlocks.init();
        ModEntities.init();
        ModItems.init();
        InfectionManager.init();
        ModCommands.init();

        // latex ore (data/latexdrones/worldgen/placed_feature/latex_ore.json)
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, id("latex_ore")));

        LOGGER.info("Latex Drone Infection loaded: {} mobs, {} blocks.", ModEntities.BY_NAME.size(), ModBlocks.ALL.size());
    }
}
