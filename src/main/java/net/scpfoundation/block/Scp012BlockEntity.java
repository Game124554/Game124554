package net.scpfoundation.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.scpfoundation.registry.ModBlockEntities;

/** Exists only so SCP-012 can tick. */
public class Scp012BlockEntity extends BlockEntity {
    public Scp012BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCP_012, pos, state);
    }
}
