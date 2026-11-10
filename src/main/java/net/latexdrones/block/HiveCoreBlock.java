package net.latexdrones.block;

import net.latexdrones.content.MobDef;
import net.latexdrones.entity.LatexDroneEntity;
import net.latexdrones.registry.ModBlocks;
import net.latexdrones.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The heart of a hive. On random ticks it infects the ground within two blocks and, while a player is
 * near, grows new drone units (never more than four at a time around one core).
 */
public class HiveCoreBlock extends Block {
    public HiveCoreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // spread: the radius is fixed, so the infection can never run away across the world
        for (int i = 0; i < 3; i++) {
            BlockPos target = pos.offset(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
            BlockState there = level.getBlockState(target);
            Block infected = null;
            if (there.is(Blocks.GRASS_BLOCK)) {
                infected = ModBlocks.INFECTED_GRASS;
            } else if (there.is(Blocks.DIRT) || there.is(Blocks.COARSE_DIRT)) {
                infected = ModBlocks.INFECTED_DIRT;
            } else if (there.is(Blocks.STONE)) {
                infected = ModBlocks.INFECTED_STONE;
            } else if (there.is(Blocks.COBBLESTONE)) {
                infected = ModBlocks.INFECTED_COBBLESTONE;
            } else if (there.is(Blocks.SAND)) {
                infected = ModBlocks.INFECTED_SAND;
            }
            if (infected != null) {
                level.setBlockAndUpdate(target, infected.defaultBlockState());
            }
        }

        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        if (level.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 24.0D, true) == null) {
            return;
        }
        if (level.getEntitiesOfClass(LatexDroneEntity.class, new AABB(pos).inflate(12.0D)).size() >= 4) {
            return;
        }
        MobDef def = ModEntities.form("drone_unit");
        for (int attempt = 0; attempt < 8; attempt++) {
            BlockPos spot = pos.offset(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
            BlockPos below = spot.below();
            if (level.getBlockState(spot).isAir() && level.getBlockState(spot.above()).isAir()
                    && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                LatexDroneEntity drone = def.type.create(level);
                if (drone != null) {
                    drone.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
                    level.addFreshEntity(drone);
                    level.sendParticles(ParticleTypes.SQUID_INK, spot.getX() + 0.5D, spot.getY() + 1.0D, spot.getZ() + 0.5D, 12, 0.3D, 0.5D, 0.3D, 0.02D);
                }
                return;
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.WITCH, pos.getX() + random.nextDouble(), pos.getY() + 1.05D, pos.getZ() + random.nextDouble(), 0.0D, 0.02D, 0.0D);
        }
    }
}
