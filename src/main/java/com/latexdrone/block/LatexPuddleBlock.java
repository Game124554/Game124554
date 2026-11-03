package com.latexdrone.block;

import com.latexdrone.infection.InfectionManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.CarpetBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Thin latex puddle. Standing in it slowly infects you with its latex type. */
public class LatexPuddleBlock extends CarpetBlock {
    private final int formIndex;

    public LatexPuddleBlock(Settings settings, int formIndex) {
        super(settings);
        this.formIndex = formIndex;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof ServerPlayerEntity player && player.age % 20 == 0) {
            InfectionManager.addInfection(player, 4.0f, formIndex);
        }
        entity.slowMovement(state, new net.minecraft.util.math.Vec3d(0.8, 1.0, 0.8));
    }
}
