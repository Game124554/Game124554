package net.realisticoverhaul.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.realisticoverhaul.config.ROConfig;

/** Replaces world-generated bedrock floors/ceilings with normal stone. */
public final class BedrockRemover {
	private BedrockRemover() {}

	public static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
		if (!ROConfig.get().removeBedrockWorldgen) return;
		boolean nether = world.getRegistryKey() == World.NETHER;
		boolean overworld = world.getRegistryKey() == World.OVERWORLD;
		if (!nether && !overworld) return;

		BlockState replacement = nether ? Blocks.NETHERRACK.getDefaultState() : Blocks.DEEPSLATE.getDefaultState();
		int bottom = chunk.getBottomY();
		replaceLayers(chunk, bottom, bottom + 5, replacement);
		if (nether) replaceLayers(chunk, 122, 128, replacement); // Nether roof
	}

	private static void replaceLayers(WorldChunk chunk, int fromY, int toY, BlockState replacement) {
		int startX = chunk.getPos().getStartX();
		int startZ = chunk.getPos().getStartZ();
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int y = fromY; y < toY; y++) {
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					pos.set(startX + x, y, startZ + z);
					if (chunk.getBlockState(pos).isOf(Blocks.BEDROCK)) {
						chunk.setBlockState(pos, replacement, false);
					}
				}
			}
		}
	}
}
