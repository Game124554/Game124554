package com.teardownvoxel.world;

import com.teardownvoxel.registry.ModParticles;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * Delayed server tasks (chain detonations) and lingering effect emitters (smoke columns).
 */
public final class ServerScheduler {
    private static final Map<ServerWorld, List<Task>> TASKS = new WeakHashMap<>();
    private static final Map<ServerWorld, List<SmokeEmitter>> SMOKE = new WeakHashMap<>();

    private record Task(long runAt, Consumer<ServerWorld> action) {
    }

    private static final class SmokeEmitter {
        final Vec3d pos;
        final double radius;
        int ticksLeft;
        final int total;

        SmokeEmitter(Vec3d pos, double radius, int ticks) {
            this.pos = pos;
            this.radius = radius;
            this.ticksLeft = ticks;
            this.total = ticks;
        }
    }

    private ServerScheduler() {
    }

    public static void schedule(ServerWorld world, int delayTicks, Consumer<ServerWorld> action) {
        TASKS.computeIfAbsent(world, w -> new ArrayList<>()).add(new Task(world.getTime() + Math.max(1, delayTicks), action));
    }

    public static void addSmoke(ServerWorld world, Vec3d pos, double radius, int ticks) {
        if (ticks <= 0) return;
        SMOKE.computeIfAbsent(world, w -> new ArrayList<>()).add(new SmokeEmitter(pos, radius, ticks));
    }

    public static void clear() {
        TASKS.clear();
        SMOKE.clear();
    }

    public static void tick(ServerWorld world) {
        List<Task> tasks = TASKS.get(world);
        if (tasks != null && !tasks.isEmpty()) {
            long now = world.getTime();
            List<Task> due = new ArrayList<>();
            for (Iterator<Task> it = tasks.iterator(); it.hasNext(); ) {
                Task t = it.next();
                if (t.runAt <= now) {
                    due.add(t);
                    it.remove();
                }
            }
            // Run after iterating: tasks may schedule more tasks.
            for (Task t : due) t.action.accept(world);
        }

        List<SmokeEmitter> smoke = SMOKE.get(world);
        if (smoke != null && !smoke.isEmpty()) {
            Random r = world.random;
            for (Iterator<SmokeEmitter> it = smoke.iterator(); it.hasNext(); ) {
                SmokeEmitter s = it.next();
                if (--s.ticksLeft <= 0) {
                    it.remove();
                    continue;
                }
                if (s.ticksLeft % 3 != 0) continue;
                float strength = (float) s.ticksLeft / s.total;
                int puffs = 1 + (int) (strength * 3);
                for (int i = 0; i < puffs; i++) {
                    double x = s.pos.x + (r.nextDouble() - 0.5) * 2 * s.radius;
                    double y = s.pos.y + r.nextDouble() * 0.6;
                    double z = s.pos.z + (r.nextDouble() - 0.5) * 2 * s.radius;
                    // count = 0 => (dx,dy,dz) is the particle velocity.
                    world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0,
                            (r.nextDouble() - 0.5) * 0.02, 0.03 + r.nextDouble() * 0.03, (r.nextDouble() - 0.5) * 0.02, 1.0);
                }
                if (r.nextInt(3) == 0) {
                    world.spawnParticles(ModParticles.DUST_CLOUD, s.pos.x, s.pos.y + 0.3, s.pos.z, 1, s.radius * 0.6, 0.2, s.radius * 0.6, 0.01);
                }
            }
        }
    }
}
