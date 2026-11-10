package com.teardownvoxel.world;

import com.teardownvoxel.config.TVConfig;
import com.teardownvoxel.entity.VoxelDebrisEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Spawns micro-debris while keeping a per-dimension budget so a big explosion can't tank TPS.
 */
public final class DebrisSpawner {
    private static final Map<ServerWorld, int[]> ACTIVE = new WeakHashMap<>();

    private DebrisSpawner() {
    }

    /** Re-counts live debris once per second (cheap, and self-correcting for chunk unloads). */
    public static void tickBudget(ServerWorld world) {
        if (world.getTime() % 20 != 0) return;
        int count = 0;
        for (Entity e : world.iterateEntities()) {
            if (e instanceof VoxelDebrisEntity) count++;
        }
        ACTIVE.computeIfAbsent(world, w -> new int[1])[0] = count;
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static int remainingBudget(ServerWorld world) {
        int active = ACTIVE.computeIfAbsent(world, w -> new int[1])[0];
        return Math.max(0, TVConfig.get().maxActiveDebris - active);
    }

    public static boolean canShatter(BlockState state) {
        // Fluids report INVISIBLE, so this also skips water/lava.
        return !state.isAir() && state.getRenderType() != BlockRenderType.INVISIBLE;
    }

    /**
     * Breaks {@code state} at {@code pos} into {@code count} small cubes.
     *
     * @param impulse base velocity added to every fragment (e.g. blast direction)
     * @param spread  random velocity magnitude
     * @return number of fragments actually spawned
     */
    public static int shatter(ServerWorld world, BlockPos pos, BlockState state, Vec3d impulse, int count, double spread) {
        if (!canShatter(state) || count <= 0) return 0;
        int[] active = ACTIVE.computeIfAbsent(world, w -> new int[1]);
        TVConfig cfg = TVConfig.get();
        Random r = world.random;
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            if (active[0] >= cfg.maxActiveDebris) break;
            float scale = cfg.debrisMinScale + r.nextFloat() * (cfg.debrisMaxScale - cfg.debrisMinScale);
            double half = scale / 2.0;
            double x = pos.getX() + half + r.nextDouble() * (1.0 - scale);
            double y = pos.getY() + r.nextDouble() * (1.0 - scale);
            double z = pos.getZ() + half + r.nextDouble() * (1.0 - scale);
            VoxelDebrisEntity debris = VoxelDebrisEntity.create(world, state, x, y, z, scale);
            Vec3d v = impulse.add((r.nextDouble() - 0.5) * 2 * spread, r.nextDouble() * spread * 1.2, (r.nextDouble() - 0.5) * 2 * spread);
            debris.setVelocity(v);
            if (world.spawnEntity(debris)) {
                active[0]++;
                spawned++;
            }
        }
        return spawned;
    }
}
