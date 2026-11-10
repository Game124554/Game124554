package net.latexdrones.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vent leaking latex gas. The gas cloud is only particles here; players standing within about three
 * blocks are infected by InfectionManager#scanSurroundings.
 */
public class GasVentBlock extends Block {
    public GasVentBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockState(pos.above()).isAir()) {
            for (int i = 0; i < 2; i++) {
                level.addParticle(ParticleTypes.SNEEZE, pos.getX() + 0.2D + random.nextDouble() * 0.6D, pos.getY() + 1.05D,
                        pos.getZ() + 0.2D + random.nextDouble() * 0.6D, (random.nextDouble() - 0.5D) * 0.04D, 0.06D,
                        (random.nextDouble() - 0.5D) * 0.04D);
            }
        }
    }
}
