package net.latexdrones.block;

import net.latexdrones.infection.InfectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** A solid block that infects players who walk on it (sneak to stay safe). */
public class InfectiousBlock extends Block {
    private final int power;

    public InfectiousBlock(BlockBehaviour.Properties properties, int power) {
        super(properties);
        this.power = power;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        InfectionManager.touch(entity, level, this.power);
        super.stepOn(level, pos, state, entity);
    }
}
