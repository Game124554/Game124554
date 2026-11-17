package com.thunderscience.changedtsc.registry;

import com.thunderscience.changedtsc.ChangedTSC;
import com.thunderscience.changedtsc.block.SpawnMarkerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ChangedTSC.MODID);

    public static final RegistryObject<Block> LAB_TILE_WHITE = BLOCKS.register("lab_tile_white",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(2.5F, 6.0F).sound(SoundType.STONE)));
    public static final RegistryObject<Block> LAB_TILE_BLACK = BLOCKS.register("lab_tile_black",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.5F, 6.0F).sound(SoundType.STONE)));
    public static final RegistryObject<Block> TSC_EMBLEM_BLOCK = BLOCKS.register("tsc_emblem_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(3.0F, 6.0F)
                    .sound(SoundType.METAL).lightLevel(state -> 10)));

    /** Invisible marker used inside structures; spawns NPCs / latex / bosses when a player comes near. */
    public static final RegistryObject<Block> SPAWN_MARKER = BLOCKS.register("spawn_marker",
            () -> new SpawnMarkerBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion().noLootTable()
                    .replaceable().strength(-1.0F, 3600000.0F)));

    private ModBlocks() {}
}
