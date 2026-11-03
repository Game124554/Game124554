package net.realisticoverhaul.physics;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.realisticoverhaul.config.ROConfig;

/** Glass breaks from fast projectiles, hard landings and sprinting into it. */
public final class GlassPhysics {
	private GlassPhysics() {}

	public static boolean isGlass(BlockState state) {
		return Registries.BLOCK.getId(state.getBlock()).getPath().contains("glass");
	}

	public static void tickSprinting(ServerWorld world) {
		ROConfig cfg = ROConfig.get();
		if (!cfg.glassBreaking || !cfg.sprintBreaksGlass) return;
		for (ServerPlayerEntity p : world.getPlayers()) {
			if (!p.isSprinting() || !p.horizontalCollision || p.isSpectator()) continue;
			Direction facing = p.getHorizontalFacing();
			BlockPos front = p.getBlockPos().offset(facing);
			for (int y = 0; y <= 1; y++) {
				BlockPos pos = front.up(y);
				if (isGlass(world.getBlockState(pos))) {
					world.breakBlock(pos, false, p);
					BlockPhysics.queue(world, pos);
				}
			}
		}
	}
}
