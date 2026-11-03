package net.realisticoverhaul.physics;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.realisticoverhaul.config.ROConfig;
import net.realisticoverhaul.net.ModNet;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Realistic collapse: when a block is broken, burnt or blown up, the blocks around it
 * are checked. A block with nothing under it and too few solid blocks beside it falls.
 * Work is spread over ticks and limited by the performance preset.
 */
public final class BlockPhysics {
	private static final Direction[] CHECK = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
	private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
	private static final int MAX_QUEUE = 20000;

	private static final Map<RegistryKey<World>, ArrayDeque<BlockPos>> QUEUES = new HashMap<>();
	private static final Map<RegistryKey<World>, LongOpenHashSet> QUEUED = new HashMap<>();

	private BlockPhysics() {}

	public static void queue(ServerWorld world, BlockPos pos) {
		if (!ROConfig.get().blockPhysics) return;
		ArrayDeque<BlockPos> q = QUEUES.computeIfAbsent(world.getRegistryKey(), k -> new ArrayDeque<>());
		LongOpenHashSet set = QUEUED.computeIfAbsent(world.getRegistryKey(), k -> new LongOpenHashSet());
		if (q.size() >= MAX_QUEUE) return;
		if (set.add(pos.asLong())) q.add(pos.toImmutable());
	}

	public static void clear() {
		QUEUES.clear();
		QUEUED.clear();
	}

	public static void tick(ServerWorld world) {
		ArrayDeque<BlockPos> q = QUEUES.get(world.getRegistryKey());
		if (q == null || q.isEmpty()) return;
		LongOpenHashSet set = QUEUED.get(world.getRegistryKey());
		ROConfig cfg = ROConfig.get();
		if (!cfg.blockPhysics) {
			q.clear();
			set.clear();
			return;
		}

		int budget = Math.max(1, cfg.physicsUpdatesPerTick);
		int fell = 0;
		BlockPos last = null;

		while (budget-- > 0 && !q.isEmpty()) {
			BlockPos origin = q.poll();
			set.remove(origin.asLong());
			for (Direction d : CHECK) {
				BlockPos p = origin.offset(d);
				if (!world.isChunkLoaded(p)) continue;
				BlockState s = world.getBlockState(p);
				if (shouldFall(world, p, s, cfg)) {
					FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, p, s);
					if (cfg.collapseHurtsEntities) fb.setHurtEntities(2.0f, 40);
					fell++;
					last = p;
					queue(world, p); // chain reaction
				}
			}
		}

		if (fell > 0 && last != null && ROConfig.get().cameraShake) {
			ModNet.shakeNear(world, Vec3d.ofCenter(last), 20, Math.min(0.6f, fell * 0.08f));
		}
	}

	public static boolean shouldFall(ServerWorld world, BlockPos pos, BlockState state, ROConfig cfg) {
		if (state.isAir() || !state.getFluidState().isEmpty()) return false;
		if (state.hasBlockEntity()) return false;
		if (state.getBlock() instanceof FallingBlock) return false; // vanilla already handles sand/gravel
		if (state.isIn(BlockTags.LEAVES)) return false;
		if (state.isIn(BlockTags.LOGS) && !cfg.treesFall) return false;
		if (state.isOf(Blocks.BEDROCK) || state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.CRYING_OBSIDIAN)
				|| state.isOf(Blocks.END_PORTAL_FRAME) || state.isOf(Blocks.REINFORCED_DEEPSLATE)) return false;
		float hardness = state.getHardness(world, pos);
		if (hardness < 0 || hardness >= 50) return false;
		if (!state.isFullCube(world, pos)) return false;
		if (pos.getY() <= world.getBottomY()) return false; // never drop blocks into the void

		BlockPos belowPos = pos.down();
		BlockState below = world.getBlockState(belowPos);
		boolean belowEmpty = below.isAir() || below.isReplaceable() || !below.getFluidState().isEmpty() && !below.isSolidBlock(world, belowPos);
		if (!belowEmpty) return false;

		int sides = 0;
		for (Direction d : SIDES) {
			BlockPos n = pos.offset(d);
			if (world.getBlockState(n).isSolidBlock(world, n)) sides++;
		}
		return sides < cfg.sideSupportNeeded;
	}
}
