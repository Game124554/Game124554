package com.latexdrone.block;

import com.latexdrone.infection.InfectionManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** Sharp latex crystal. Cuts and infects anything walking through it. */
public class LatexCrystalBlock extends Block {
    private static final VoxelShape SHAPE = Block.createCuboidShape(3, 0, 3, 13, 13, 13);
    private final int formIndex;

    public LatexCrystalBlock(Settings settings, int formIndex) {
        super(settings);
        this.formIndex = formIndex;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (world.isClient || !(entity instanceof LivingEntity living) || InfectionManager.isLatex(living)) return;
        if (living.age % 20 == 0) {
            living.damage(world.getDamageSources().cactus(), 1.0f);
            if (living instanceof ServerPlayerEntity player) InfectionManager.addInfection(player, 3.0f, formIndex);
        }
    }
}
