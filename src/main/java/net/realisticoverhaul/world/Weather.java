package net.realisticoverhaul.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.realisticoverhaul.config.ROConfig;

/** Rain puddles and deeper snow build-up near players. */
public final class Weather {
	private static final int RADIUS = 28;

	private Weather() {}

	public static void tick(ServerWorld world) {
		ROConfig cfg = ROConfig.get();
		if (!cfg.puddles && !cfg.snowAccumulation) return;
		if (!world.isRaining()) return;
		Random random = world.random;

		for (ServerPlayerEntity player : world.getPlayers()) {
			for (int i = 0; i < cfg.weatherAttemptsPerTick; i++) {
				int x = player.getBlockX() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
				int z = player.getBlockZ() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
				if (!world.isChunkLoaded(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z))) continue;
				BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, new BlockPos(x, 0, z));
				Biome biome = world.getBiome(top).value();
				Biome.Precipitation precip = biome.getPrecipitation(top);
				if (precip == Biome.Precipitation.SNOW) {
					if (cfg.snowAccumulation && random.nextFloat() < 0.5f) addSnow(world, top, cfg);
				} else if (precip == Biome.Precipitation.RAIN) {
					if (cfg.puddles) tryPuddle(world, top, random, cfg);
				}
			}
		}
	}

	private static void tryPuddle(ServerWorld world, BlockPos top, Random random, ROConfig cfg) {
		if (!world.getBlockState(top).isAir()) return;
		if (!world.hasRain(top)) return;
		BlockPos below = top.down();
		BlockState belowState = world.getBlockState(below);
		if (!belowState.isSideSolidFullSquare(world, below, Direction.UP)) return;
		if (belowState.isIn(BlockTags.LEAVES)) return;

		// Puddles prefer low spots (blocks beside them that are higher).
		float chance = cfg.puddleChance;
		for (Direction d : Direction.Type.HORIZONTAL) {
			if (world.getBlockState(top.offset(d)).isSolidBlock(world, top.offset(d))) {
				chance *= 2.0f;
			}
		}
		if (random.nextFloat() < chance) {
			world.setBlockState(top, ModBlocks.PUDDLE.getDefaultState(), Block.NOTIFY_ALL);
		}
	}

	private static void addSnow(ServerWorld world, BlockPos top, ROConfig cfg) {
		if (!world.isSkyVisible(top)) return;
		int max = Math.max(1, Math.min(8, cfg.maxSnowLayers));
		BlockPos target = top;
		BlockState at = world.getBlockState(top);
		if (!at.isOf(Blocks.SNOW)) {
			BlockState down = world.getBlockState(top.down());
			if (down.isOf(Blocks.SNOW)) {
				target = top.down();
				at = down;
			}
		}
		if (at.isOf(Blocks.SNOW)) {
			int layers = at.get(SnowBlock.LAYERS);
			if (layers < max) world.setBlockState(target, at.with(SnowBlock.LAYERS, layers + 1), Block.NOTIFY_ALL);
		} else if (at.isAir() && Blocks.SNOW.getDefaultState().canPlaceAt(world, top)) {
			world.setBlockState(top, Blocks.SNOW.getDefaultState(), Block.NOTIFY_ALL);
		}
	}
}
