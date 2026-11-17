package com.thunderscience.changedtsc.registry;

import com.thunderscience.changedtsc.ChangedTSC;
import com.thunderscience.changedtsc.block.SpawnMarkerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ChangedTSC.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<SpawnMarkerBlockEntity>> SPAWN_MARKER = BLOCK_ENTITIES.register("spawn_marker",
            () -> BlockEntityType.Builder.of(SpawnMarkerBlockEntity::new, ModBlocks.SPAWN_MARKER.get()).build(null));

    private ModBlockEntities() {}
}
