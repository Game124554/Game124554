package com.teardownvoxel.world;

import com.teardownvoxel.config.TVConfig;
import com.teardownvoxel.registry.ModTags;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.block.BlockState;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Progressive block damage: projectiles, sledgehammer hits on hard blocks and the blowtorch
 * accumulate damage on a block (shown with the vanilla crack overlay). When the block runs out of
 * hit points it shatters into voxel debris instead of just vanishing.
 */
public final class VoxelDamage {
    private static final Map<ServerWorld, Long2ObjectMap<Entry>> DAMAGE = new WeakHashMap<>();
    private static final int FORGET_AFTER_TICKS = 600;

    private static final class Entry {
        float damage;
        long lastHit;
        int stage = -1;
    }

    private VoxelDamage() {
    }

    public static void clear() {
        DAMAGE.clear();
    }

    public static float hitPoints(ServerWorld world, BlockPos pos, BlockState state) {
        float hardness = state.getHardness(world, pos);
        if (hardness < 0 || state.isIn(ModTags.INDESTRUCTIBLE)) return Float.POSITIVE_INFINITY;
        float hp = Math.max(0.3f, hardness) * TVConfig.get().hitPointsPerHardness;
        if (state.isIn(ModTags.FRAGILE)) hp *= 0.2f;
        return hp;
    }

    /** Stable fake "breaker id" for the crack overlay of a position. */
    private static int breakerId(BlockPos pos) {
        return -1000 - (int) (pos.asLong() & 0x3FFFFFFF);
    }

    /**
     * Adds damage to a block.
     *
     * @param impulse velocity given to the debris if the block breaks
     * @return true if the block shattered
     */
    public static boolean damage(ServerWorld world, BlockPos pos, float amount, Vec3d impulse) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof net.minecraft.block.FluidBlock) return false;
        float hp = hitPoints(world, pos, state);
        if (Float.isInfinite(hp)) return false;

        Long2ObjectMap<Entry> map = DAMAGE.computeIfAbsent(world, w -> new Long2ObjectOpenHashMap<>());
        long key = pos.asLong();
        Entry e = map.computeIfAbsent(key, k -> new Entry());
        e.damage += amount;
        e.lastHit = world.getTime();

        if (e.damage >= hp) {
            map.remove(key);
            world.setBlockBreakingInfo(breakerId(pos), pos, -1);
            shatterBlock(world, pos, state, impulse);
            return true;
        }

        int stage = Math.min(9, (int) (e.damage / hp * 10f));
        if (stage != e.stage) {
            e.stage = stage;
            world.setBlockBreakingInfo(breakerId(pos), pos, stage);
            // Chip off a crumb on every new crack stage.
            Vec3d c = Vec3d.ofCenter(pos);
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), c.x, c.y, c.z, 6, 0.3, 0.3, 0.3, 0.05);
            if (stage % 3 == 0) DebrisSpawner.shatter(world, pos, state, impulse.multiply(0.5), 1, 0.1);
        }
        return false;
    }

    /** Destroys a block as debris (no item drops), then checks structural support around it. */
    public static void shatterBlock(ServerWorld world, BlockPos pos, BlockState state, Vec3d impulse) {
        TVConfig cfg = TVConfig.get();
        int count = cfg.debrisPerBlockMin + world.random.nextInt(cfg.debrisPerBlockMax - cfg.debrisPerBlockMin + 1);
        world.breakBlock(pos, false);
        DebrisSpawner.shatter(world, pos, state, impulse, count, 0.15);
        StructuralIntegrity.onBlocksDestroyed(world, List.of(pos.toImmutable()));
    }

    public static void clearAt(ServerWorld world, BlockPos pos) {
        Long2ObjectMap<Entry> map = DAMAGE.get(world);
        if (map != null && map.remove(pos.asLong()) != null) {
            world.setBlockBreakingInfo(breakerId(pos), pos, -1);
        }
    }

    /** Forgets stale damage and clears its crack overlay. */
    public static void tick(ServerWorld world) {
        if (world.getTime() % 40 != 0) return;
        Long2ObjectMap<Entry> map = DAMAGE.get(world);
        if (map == null || map.isEmpty()) return;
        long now = world.getTime();
        ObjectIterator<Long2ObjectMap.Entry<Entry>> it = map.long2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            Long2ObjectMap.Entry<Entry> en = it.next();
            BlockPos pos = BlockPos.fromLong(en.getLongKey());
            if (now - en.getValue().lastHit > FORGET_AFTER_TICKS || world.getBlockState(pos).isAir()) {
                world.setBlockBreakingInfo(breakerId(pos), pos, -1);
                it.remove();
            }
        }
    }
}
