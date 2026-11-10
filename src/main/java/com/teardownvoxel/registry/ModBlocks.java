package com.teardownvoxel.registry;

import com.teardownvoxel.TeardownVoxel;
import com.teardownvoxel.block.NitroChargeBlock;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

public final class ModBlocks {
    public static final Block NITRO_CHARGE = new NitroChargeBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.RED)
            .strength(0.4f)
            .sounds(BlockSoundGroup.GRASS)
            .nonOpaque()
            .pistonBehavior(PistonBehavior.DESTROY));

    public static final Block CHARRED_PLANKS = new Block(AbstractBlock.Settings.create()
            .mapColor(MapColor.BLACK)
            .strength(1.2f, 2.0f)
            .sounds(BlockSoundGroup.WOOD)
            .burnable());

    public static final Block CHARRED_LOG = new PillarBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.BLACK)
            .strength(1.4f)
            .sounds(BlockSoundGroup.WOOD)
            .burnable());

    public static void register() {
        Registry.register(Registries.BLOCK, TeardownVoxel.id("nitro_charge"), NITRO_CHARGE);
        Registry.register(Registries.BLOCK, TeardownVoxel.id("charred_planks"), CHARRED_PLANKS);
        Registry.register(Registries.BLOCK, TeardownVoxel.id("charred_log"), CHARRED_LOG);

        FlammableBlockRegistry.getDefaultInstance().add(CHARRED_PLANKS, 5, 20);
        FlammableBlockRegistry.getDefaultInstance().add(CHARRED_LOG, 5, 5);
    }

    private ModBlocks() {
    }
}
