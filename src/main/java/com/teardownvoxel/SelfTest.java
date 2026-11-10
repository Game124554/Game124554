package com.teardownvoxel;

import com.teardownvoxel.entity.FallingStructureEntity;
import com.teardownvoxel.entity.VoxelDebrisEntity;
import com.teardownvoxel.explosion.ShockwaveExplosion;
import com.teardownvoxel.registry.ModBlocks;
import com.teardownvoxel.world.ServerScheduler;
import com.teardownvoxel.world.VoxelDamage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * Headless smoke test, only active with -Dteardown_voxel.selftest=true (used by CI on a dedicated server).
 * Exercises collapse, shockwave, vanilla-explosion hook, projectile hook and nitro, then stops the server.
 */
public final class SelfTest {
    private static int maxFalling, maxDebris;
    private static boolean failed;

    private SelfTest() {
    }

    public static void init() {
        if (!Boolean.getBoolean("teardown_voxel.selftest")) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                MixinEnvironment.getCurrentEnvironment().audit();
                TeardownVoxel.LOGGER.info("SELFTEST mixin audit OK");
            } catch (Throwable t) {
                fail("mixin audit", t);
            }
            ServerWorld world = server.getOverworld();
            ServerScheduler.schedule(world, 20, w -> run(server, w));
        });
    }

    private static void fail(String what, Throwable t) {
        failed = true;
        TeardownVoxel.LOGGER.error("SELFTEST FAIL: {}", what, t);
    }

    private static BlockPos ground(ServerWorld w, int x, int z) {
        w.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, w.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z), z);
    }

    private static void run(MinecraftServer server, ServerWorld w) {
        try {
            // 1. Collapse: 4 plank pillars (height 4) + stone-brick roof; then cut the pillars.
            BlockPos o = ground(w, 0, 0);
            for (int dx = 0; dx < 5; dx++)
                for (int dz = 0; dz < 5; dz++)
                    w.setBlockState(o.add(dx, -1, dz), Blocks.STONE.getDefaultState(), Block.NOTIFY_ALL);
            int[][] corners = {{0, 0}, {4, 0}, {0, 4}, {4, 4}};
            for (int[] c : corners)
                for (int y = 0; y < 4; y++)
                    w.setBlockState(o.add(c[0], y, c[1]), Blocks.OAK_PLANKS.getDefaultState(), Block.NOTIFY_ALL);
            for (int dx = 0; dx < 5; dx++)
                for (int dz = 0; dz < 5; dz++)
                    w.setBlockState(o.add(dx, 4, dz), Blocks.STONE_BRICKS.getDefaultState(), Block.NOTIFY_ALL);
            for (int[] c : corners) {
                BlockPos p = o.add(c[0], 1, c[1]);
                VoxelDamage.shatterBlock(w, p, w.getBlockState(p), Vec3d.ZERO);
            }

            // 2. Shockwave explosion on a stone pad.
            BlockPos s = ground(w, 40, 0);
            for (int dx = -3; dx <= 3; dx++)
                for (int dy = -3; dy <= 0; dy++)
                    for (int dz = -3; dz <= 3; dz++)
                        w.setBlockState(s.add(dx, dy, dz), Blocks.STONE.getDefaultState(), Block.NOTIFY_ALL);
            ShockwaveExplosion.detonate(w, Vec3d.ofCenter(s.up()), 3.2f, null, null);

            // 3. Vanilla TNT -> ExplosionMixin.
            BlockPos t = ground(w, 0, 40);
            TntEntity tnt = new TntEntity(w, t.getX() + 0.5, t.getY(), t.getZ() + 0.5, null);
            tnt.setFuse(5);
            w.spawnEntity(tnt);

            // 4. Arrow into glass -> PersistentProjectileEntityMixin.
            BlockPos g = ground(w, 40, 40);
            w.setBlockState(g, Blocks.GLASS.getDefaultState(), Block.NOTIFY_ALL);
            ArrowEntity arrow = new ArrowEntity(w, g.getX() + 0.5, g.getY() + 4, g.getZ() + 0.5);
            arrow.setVelocity(0, -3.0, 0);
            w.spawnEntity(arrow);

            // 5. Nitro charge triggered by a redstone block.
            BlockPos n = ground(w, -40, 0);
            w.setBlockState(n, ModBlocks.NITRO_CHARGE.getDefaultState(), Block.NOTIFY_ALL);
            w.setBlockState(n.east(), Blocks.REDSTONE_BLOCK.getDefaultState(), Block.NOTIFY_ALL);

            // Sample entity counts for a few seconds, then report.
            for (int i = 1; i <= 8; i++) {
                ServerScheduler.schedule(w, i * 10, SelfTest::sample);
            }
            ServerScheduler.schedule(w, 100, ww -> report(server, ww, o, g, n));
        } catch (Throwable e) {
            fail("scenario setup", e);
            server.stop(false);
        }
    }

    private static void sample(ServerWorld w) {
        int falling = 0, debris = 0;
        for (Entity e : w.iterateEntities()) {
            if (e instanceof FallingStructureEntity) falling++;
            if (e instanceof VoxelDebrisEntity) debris++;
        }
        maxFalling = Math.max(maxFalling, falling);
        maxDebris = Math.max(maxDebris, debris);
        TeardownVoxel.LOGGER.info("SELFTEST sample: falling={} debris={}", falling, debris);
    }

    private static void report(MinecraftServer server, ServerWorld w, BlockPos o, BlockPos g, BlockPos n) {
        boolean roofGone = w.getBlockState(o.add(2, 4, 2)).isAir();
        boolean glassBroken = !w.getBlockState(g).isOf(Blocks.GLASS);
        boolean nitroGone = !w.getBlockState(n).isOf(ModBlocks.NITRO_CHARGE);
        TeardownVoxel.LOGGER.info("SELFTEST result: maxFalling={} maxDebris={} roofCollapsed={} glassBroken={} nitroDetonated={}",
                maxFalling, maxDebris, roofGone, glassBroken, nitroGone);
        boolean ok = !failed && maxFalling > 0 && maxDebris > 0 && roofGone && glassBroken && nitroGone;
        TeardownVoxel.LOGGER.info(ok ? "SELFTEST PASS" : "SELFTEST FAIL");
        server.stop(false);
    }
}
