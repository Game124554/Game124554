package net.latexdrones.block;

import net.latexdrones.entity.LatexDroneEntity;
import net.latexdrones.infection.InfectionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Heals drones - and players who have become drones - standing on it. */
public class ChargingPadBlock extends Block {
    public ChargingPadBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel server && level.getGameTime() % 20L == 0L && entity instanceof LivingEntity living) {
            boolean drone = living instanceof LatexDroneEntity
                    || (living instanceof ServerPlayer player && InfectionData.isTransformed(player));
            if (drone && living.getHealth() < living.getMaxHealth()) {
                living.heal(2.0F);
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, living.getX(), living.getY() + 0.5D, living.getZ(), 8, 0.3D, 0.5D, 0.3D, 0.02D);
            }
        }
        super.stepOn(level, pos, state, entity);
    }
}
