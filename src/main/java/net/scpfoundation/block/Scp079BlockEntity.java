package net.scpfoundation.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.scpfoundation.registry.ModBlockEntities;

/** Exists only so SCP-079 can tick. */
public class Scp079BlockEntity extends BlockEntity {
    public Scp079BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCP_079, pos, state);
    }
}
