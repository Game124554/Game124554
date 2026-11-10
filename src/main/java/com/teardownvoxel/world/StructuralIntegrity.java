package com.teardownvoxel.world;

import com.teardownvoxel.config.TVConfig;
import com.teardownvoxel.entity.FallingStructureEntity;
import com.teardownvoxel.network.ModNetworking;
import com.teardownvoxel.registry.ModParticles;
import com.teardownvoxel.registry.ModTags;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Teardown-style structural integrity.
 * <p>
 * When blocks are destroyed, every solid neighbour becomes the seed of a breadth-first search through
 * connected load-bearing blocks. If the search reaches natural ground ({@link ModTags#FOUNDATION}),
 * unbreakable blocks (bedrock), the world floor, an unloaded chunk, or exceeds
 * {@link TVConfig#maxStructureSearch} blocks, the structure is supported. Otherwise the whole island
 * lost its support and is converted into {@link FallingStructureEntity} chunks that fall and shatter.
 * <p>
 * Searches run asynchronously in the sense that matters for Minecraft: they are time-sliced across
 * server ticks with a node budget ({@link TVConfig#bfsNodesPerTick}) on the server thread, so world
 * access stays thread-safe and a huge check never causes a lag spike.
 */
public final class StructuralIntegrity {
    private static final Map<ServerWorld, Deque<Search>> QUEUES = new WeakHashMap<>();
    private static final Direction[] DIRS = Direction.values();

    private StructuralIntegrity() {
    }

    private static final class Search {
        final long seed;
        final LongOpenHashSet visited = new LongOpenHashSet();
        final LongArrayFIFOQueue frontier = new LongArrayFIFOQueue();
        final LongSet batchGrounded;
        boolean started;

        Search(long seed, LongSet batchGrounded) {
            this.seed = seed;
            this.batchGrounded = batchGrounded;
        }
    }

    private enum Result { RUNNING, SUPPORTED, UNSUPPORTED }

    public static void clear() {
        QUEUES.clear();
    }

    /** Queue support checks for everything touching the destroyed positions. */
    public static void onBlocksDestroyed(ServerWorld world, Collection<BlockPos> destroyed) {
        if (!TVConfig.get().structuralCollapse || destroyed.isEmpty()) return;
        LongOpenHashSet destroyedSet = new LongOpenHashSet();
        for (BlockPos p : destroyed) destroyedSet.add(p.asLong());

        LongOpenHashSet seeds = new LongOpenHashSet();
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (BlockPos p : destroyed) {
            for (Direction d : DIRS) {
                m.set(p, d);
                long key = m.asLong();
                if (destroyedSet.contains(key) || seeds.contains(key)) continue;
                if (!world.isChunkLoaded(m)) continue;
                BlockState s = world.getBlockState(m);
                if (isStructural(world, m, s) && !isFoundation(world, m, s)) {
                    seeds.add(key);
                }
            }
        }
        if (seeds.isEmpty()) return;

        // Shared between the searches of one event so a supported region is only walked once.
        LongOpenHashSet batchGrounded = new LongOpenHashSet();
        Deque<Search> queue = QUEUES.computeIfAbsent(world, w -> new ArrayDeque<>());
        for (long seed : seeds) queue.add(new Search(seed, batchGrounded));
    }

    public static void tick(ServerWorld world) {
        Deque<Search> queue = QUEUES.get(world);
        if (queue == null || queue.isEmpty()) return;
        int budget = TVConfig.get().bfsNodesPerTick;
        while (budget > 0 && !queue.isEmpty()) {
            Search s = queue.peekFirst();
            int[] used = new int[1];
            Result r = step(world, s, budget, used);
            budget -= Math.max(1, used[0]);
            if (r != Result.RUNNING) {
                queue.pollFirst();
                if (r == Result.SUPPORTED) {
                    s.batchGrounded.addAll(s.visited);
                } else {
                    collapse(world, s.visited);
                }
            }
        }
    }

    private static Result step(ServerWorld world, Search s, int budget, int[] used) {
        TVConfig cfg = TVConfig.get();
        BlockPos.Mutable m = new BlockPos.Mutable();
        if (!s.started) {
            s.started = true;
            if (s.batchGrounded.contains(s.seed)) return Result.SUPPORTED;
            m.set(s.seed);
            BlockState seedState = world.getBlockState(m);
            // Seed vanished or changed since it was queued (e.g. already collapsed by a sibling search).
            if (!isStructural(world, m, seedState)) return Result.SUPPORTED;
            s.visited.add(s.seed);
            s.frontier.enqueue(s.seed);
        }

        int bottom = world.getBottomY();
        while (!s.frontier.isEmpty()) {
            if (used[0]++ >= budget) return Result.RUNNING;
            long cur = s.frontier.dequeueLong();
            int cx = BlockPos.unpackLongX(cur), cy = BlockPos.unpackLongY(cur), cz = BlockPos.unpackLongZ(cur);
            if (cy <= bottom) return Result.SUPPORTED;

            for (Direction d : DIRS) {
                m.set(cx + d.getOffsetX(), cy + d.getOffsetY(), cz + d.getOffsetZ());
                long key = m.asLong();
                if (s.visited.contains(key)) continue;
                if (s.batchGrounded.contains(key)) return Result.SUPPORTED;
                if (!world.isChunkLoaded(m)) return Result.SUPPORTED;
                BlockState st = world.getBlockState(m);
                if (!isStructural(world, m, st)) continue;
                if (isFoundation(world, m, st)) return Result.SUPPORTED;
                s.visited.add(key);
                if (s.visited.size() > cfg.maxStructureSearch) return Result.SUPPORTED;
                s.frontier.enqueue(key);
            }
        }
        return Result.UNSUPPORTED;
    }

    /** Blocks that can carry load. Air, fluids, plants, leaves and non-colliding blocks cannot. */
    public static boolean isStructural(ServerWorld world, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getBlock() instanceof FluidBlock) return false;
        if (state.isIn(ModTags.NON_STRUCTURAL)) return false;
        return !state.getCollisionShape(world, pos).isEmpty();
    }

    /** Natural ground / bedrock / the world floor. */
    public static boolean isFoundation(ServerWorld world, BlockPos pos, BlockState state) {
        if (pos.getY() <= world.getBottomY()) return true;
        if (state.isIn(ModTags.FOUNDATION)) return true;
        return state.getHardness(world, pos) < 0; // bedrock, barriers, end portal frames...
    }

    private static void collapse(ServerWorld world, LongOpenHashSet island) {
        List<BlockPos> blocks = new ArrayList<>(island.size());
        for (long l : island) blocks.add(BlockPos.fromLong(l));
        blocks.sort(Comparator.comparingInt(BlockPos::getY));
        if (blocks.isEmpty()) return;

        int minY = blocks.get(0).getY();
        int baseDelay = 3 + world.random.nextInt(6);
        double sx = 0, sy = 0, sz = 0;
        int converted = 0;

        for (BlockPos pos : blocks) {
            BlockState state = world.getBlockState(pos);
            if (!isStructural(world, pos, state)) continue; // changed since the search ran
            sx += pos.getX();
            sy += pos.getY();
            sz += pos.getZ();
            converted++;

            if (state.hasBlockEntity()) {
                // Containers etc. break normally so their contents are never lost.
                world.breakBlock(pos, true);
                continue;
            }
            int delay = baseDelay + (pos.getY() - minY) / 3;
            FallingStructureEntity falling = FallingStructureEntity.create(world, pos, state, delay);
            world.setBlockState(pos, state.getFluidState().getBlockState(), Block.NOTIFY_ALL);
            world.spawnEntity(falling);
        }
        if (converted == 0) return;

        Vec3d center = new Vec3d(sx / converted + 0.5, sy / converted + 0.5, sz / converted + 0.5);
        float size = Math.min(1f, converted / 64f);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.BLOCKS,
                1.0f + size, 0.5f);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS,
                1.5f, 0.6f);
        world.spawnParticles(ModParticles.DUST_CLOUD, center.x, center.y, center.z, 6 + (int) (size * 20),
                1.0 + size * 3, 0.6, 1.0 + size * 3, 0.02);
        world.spawnParticles(ModParticles.PEBBLE, center.x, center.y, center.z, 20 + (int) (size * 40),
                1.0 + size * 3, 0.5, 1.0 + size * 3, 0.0);
        ModNetworking.sendShake(world, center, 1.0f + size * 3.0f, 40.0 + size * 40.0, 18);
    }
}
