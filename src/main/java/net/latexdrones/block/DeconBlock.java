package net.latexdrones.block;

import net.latexdrones.entity.LatexDroneEntity;
import net.latexdrones.infection.InfectionData;
import net.latexdrones.infection.InfectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Decontamination pad: washes infection off players standing on it and burns drones (and drone players). */
public class DeconBlock extends Block {
    public DeconBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel server && level.getGameTime() % 10L == 0L) {
            boolean worked = false;
            if (entity instanceof ServerPlayer player) {
                if (InfectionData.isTransformed(player)) {
                    player.hurt(level.damageSources().magic(), 1.0F);
                    worked = true;
                } else {
                    worked = InfectionManager.reduce(player, 40);
                }
            } else if (entity instanceof LatexDroneEntity drone && !drone.def().scientist) {
                drone.hurt(level.damageSources().magic(), 2.0F);
                worked = true;
            }
            if (worked) {
                server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, 6, 0.3D, 0.4D, 0.3D, 0.01D);
            }
        }
        super.stepOn(level, pos, state, entity);
    }
}
