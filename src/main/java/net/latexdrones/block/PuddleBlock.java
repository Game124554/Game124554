package net.latexdrones.block;

import net.latexdrones.infection.InfectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** A thin latex puddle (carpet shaped) that infects whoever walks through it. */
public class PuddleBlock extends CarpetBlock {
    private final int power;

    public PuddleBlock(BlockBehaviour.Properties properties, int power) {
        super(properties);
        this.power = power;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        InfectionManager.touch(entity, level, this.power);
    }
}
