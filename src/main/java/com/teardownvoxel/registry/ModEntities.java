package com.teardownvoxel.registry;

import com.teardownvoxel.TeardownVoxel;
import com.teardownvoxel.entity.FallingStructureEntity;
import com.teardownvoxel.entity.PipeBombEntity;
import com.teardownvoxel.entity.VoxelDebrisEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
    public static final EntityType<VoxelDebrisEntity> VOXEL_DEBRIS = FabricEntityTypeBuilder
            .<VoxelDebrisEntity>create(SpawnGroup.MISC, VoxelDebrisEntity::new)
            .dimensions(EntityDimensions.changing(0.4f, 0.4f))
            .trackRangeBlocks(64)
            .trackedUpdateRate(2)
            .forceTrackedVelocityUpdates(true)
            .disableSaving()
            .build();

    public static final EntityType<FallingStructureEntity> FALLING_STRUCTURE = FabricEntityTypeBuilder
            .<FallingStructureEntity>create(SpawnGroup.MISC, FallingStructureEntity::new)
            .dimensions(EntityDimensions.fixed(0.98f, 0.98f))
            .trackRangeBlocks(96)
            .trackedUpdateRate(2)
            .forceTrackedVelocityUpdates(true)
            .build();

    public static final EntityType<PipeBombEntity> PIPE_BOMB = FabricEntityTypeBuilder
            .<PipeBombEntity>create(SpawnGroup.MISC, PipeBombEntity::new)
            .dimensions(EntityDimensions.fixed(0.3f, 0.3f))
            .trackRangeBlocks(64)
            .trackedUpdateRate(2)
            .forceTrackedVelocityUpdates(true)
            .build();

    public static void register() {
        Registry.register(Registries.ENTITY_TYPE, TeardownVoxel.id("voxel_debris"), VOXEL_DEBRIS);
        Registry.register(Registries.ENTITY_TYPE, TeardownVoxel.id("falling_structure"), FALLING_STRUCTURE);
        Registry.register(Registries.ENTITY_TYPE, TeardownVoxel.id("pipe_bomb"), PIPE_BOMB);
    }

    private ModEntities() {
    }
}
