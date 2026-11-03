package net.scpfoundation.registry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.scpfoundation.ScpFoundation;
import net.scpfoundation.block.Scp330BlockEntity;

public final class ModBlockEntities {
    public static final BlockEntityType<Scp330BlockEntity> SCP_330 = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            ScpFoundation.id("scp_330"), FabricBlockEntityTypeBuilder.create(Scp330BlockEntity::new, ModBlocks.SCP_330).build());

    private ModBlockEntities() {}

    public static void register() {
        // Static initialisation registers everything.
    }
}
