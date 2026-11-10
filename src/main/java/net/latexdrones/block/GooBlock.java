package net.latexdrones.block;

import net.latexdrones.infection.InfectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Sticky, see-through latex goo: slows movement, weakens jumps and infects on contact. */
public class GooBlock extends HalfTransparentBlock {
    // slightly smaller than a full cube so that entities touching it count as "inside"
    private static final VoxelShape COLLISION = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 15.0D, 15.0D);
    private final int power;

    public GooBlock(BlockBehaviour.Properties properties, int power) {
        super(properties);
        this.power = power;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        InfectionManager.touch(entity, level, this.power);
    }
}
