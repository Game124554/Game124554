package dev.stumble.server;

import dev.stumble.StumbleConfig;
import dev.stumble.common.Humanoids;
import dev.stumble.common.LevelEnvironment;
import dev.stumble.common.Net;
import dev.stumble.physics.Ragdoll;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.server.PlayerLookup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The server half. Knocked-down mobs are simulated here, and the mob is dragged along with its
 * body; nearby games run their own copy from the same seed for the picture. Players' ragdolls are
 * run by their own game; the server only passes the news on.
 */
public final class ServerRagdolls {
    /** Ticks a get-up takes from lying down, and from having caught one's balance. */
    public static final int GET_UP_LYING = 20, GET_UP_STANDING = 5;
    private static final int PUSH_COOLDOWN = 10;
    private static final double PUSH_REACH = 4.0;

    private static final class Down {
        final LivingEntity entity;
        final Ragdoll body;
        final long seed;
        double sentX, sentZ;
        int age;
        int getUp = -1;
        boolean fellSent;

        Down(LivingEntity entity, Ragdoll body, long seed) {
            this.entity = entity;
            this.body = body;
            this.seed = seed;
        }
    }

    private static final Map<LivingEntity, Down> DOWN = new IdentityHashMap<>();
    /** Players who told us they are a ragdoll right now, with their seed (for late viewers). */
    private static final Map<UUID, Long> DOWN_PLAYERS = new HashMap<>();
    private static final Map<UUID, Long> LAST_PUSH = new HashMap<>();
    /** Entity id -> game time it was last bumped, so walking into someone is one bump, not twenty. */
    private static final Map<Integer, Long> LAST_BUMP = new HashMap<>();
    private static final int BUMP_COOLDOWN = 15;
    private static final LevelEnvironment ENV = new LevelEnvironment();

    private ServerRagdolls() {
    }

    public static boolean isDown(LivingEntity entity) {
        return !DOWN.isEmpty() && DOWN.containsKey(entity);
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(ServerRagdolls::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            DOWN.clear();
            DOWN_PLAYERS.clear();
            LAST_PUSH.clear();
            LAST_BUMP.clear();
            ENV.level = null;
        });

        ServerPlayNetworking.registerGlobalReceiver(Net.PUSH, (server, player, handler, buf, sender) -> {
            int target = buf.readInt();
            server.execute(() -> push(player, target));
        });
        ServerPlayNetworking.registerGlobalReceiver(Net.BUMP, (server, player, handler, buf, sender) -> {
            int target = buf.readInt();
            float speed = buf.readFloat();
            server.execute(() -> bump(player, target, speed));
        });
        ServerPlayNetworking.registerGlobalReceiver(Net.SELF_START, (server, player, handler, buf, sender) -> {
            Net.Body body = Net.Body.read(buf).sane();
            server.execute(() -> {
                DOWN_PLAYERS.put(player.getUUID(), body.seed());
                tell(player, Net.START, () -> Net.start(player.getId(), body.seed(), body.mode(),
                        body.vx(), body.vy(), body.vz(), body.jx(), body.jy(), body.jz(), body.yaw()));
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(Net.SELF_FALL, (server, player, handler, buf, sender) ->
                server.execute(() -> tell(player, Net.FALL, () -> Net.id(player.getId()))));
        ServerPlayNetworking.registerGlobalReceiver(Net.SELF_END, (server, player, handler, buf, sender) -> {
            float yaw = buf.readFloat();
            int ticks = Math.max(1, Math.min(40, buf.readByte()));
            float y = Float.isFinite(yaw) ? yaw : player.getYRot();
            server.execute(() -> {
                DOWN_PLAYERS.remove(player.getUUID());
                tell(player, Net.END, () -> Net.end(player.getId(), player.getX(), player.getY(), player.getZ(), y, ticks));
            });
        });

        // someone who comes into view of a body that is already down sees it collapse
        EntityTrackingEvents.START_TRACKING.register((entity, viewer) -> {
            Long seed = null;
            float yaw = 0;
            if (entity instanceof LivingEntity living) {
                Down d = DOWN.get(living);
                if (d != null && d.getUp < 0) {
                    seed = d.seed;
                    yaw = living.yBodyRot;
                }
            }
            if (seed == null && entity instanceof Player p) {
                seed = DOWN_PLAYERS.get(p.getUUID());
                yaw = p.yBodyRot;
            }
            if (seed != null && ServerPlayNetworking.canSend(viewer, Net.START)) {
                ServerPlayNetworking.send(viewer, Net.START,
                        Net.start(entity.getId(), seed, Ragdoll.Mode.LIMP.ordinal(), 0, 0, 0, 0, 0, 0, yaw));
            }
        });
    }

    // ------------------------------------------------------------------ pushing

    private static void push(ServerPlayer player, int targetId) {
        StumbleConfig cfg = StumbleConfig.get();
        if (!cfg.push || !player.isAlive() || player.isSpectator()) {
            return;
        }
        long now = player.level().getGameTime();
        Long last = LAST_PUSH.get(player.getUUID());
        if (last != null && now - last < PUSH_COOLDOWN && now >= last) {
            return;
        }
        LAST_PUSH.put(player.getUUID(), now);
        // everyone around sees the shove, whether or not it lands on anyone
        tell(player, Net.PUSH_ANIM, () -> Net.id(player.getId()));

        Entity found = targetId < 0 ? null : player.level().getEntity(targetId);
        if (!(found instanceof LivingEntity target) || target == player || !target.isAlive() || target.isSpectator()) {
            return;
        }
        if (player.distanceToSqr(target) > PUSH_REACH * PUSH_REACH || !player.hasLineOfSight(target)) {
            return;
        }

        double dx = target.getX() - player.getX(), dz = target.getZ() - player.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-3) {
            Vec3 look = player.getLookAngle();
            dx = look.x;
            dz = look.z;
            len = Math.max(1e-3, Math.sqrt(dx * dx + dz * dz));
        }
        dx /= len;
        dz /= len;
        // a shove from standing is usually caught with a stumble; a running shove usually knocks them down
        double power = 0.32 * cfg.pushStrength * (player.isSprinting() ? 1.6 : 1.0);
        double jx = dx * power, jy = 0.05, jz = dz * power;

        player.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.PLAYERS, 0.8f, 0.9f + player.getRandom().nextFloat() * 0.2f);
        player.causeFoodExhaustion(0.05f);

        if (target instanceof ServerPlayer other) {
            if (!cfg.pushPlayers) {
                return;
            }
            if (ServerPlayNetworking.canSend(other, Net.KNOCKDOWN)) {
                ServerPlayNetworking.send(other, Net.KNOCKDOWN, Net.knockdown(Ragdoll.Mode.STAGGER.ordinal(), jx, jy, jz));
            } else {
                other.knockback(power * 1.5, -dx, -dz);
                other.hurtMarked = true;
            }
            return;
        }
        if (cfg.mobKnockdowns && canKnockDown(target)) {
            knockDown(target, Ragdoll.Mode.STAGGER, jx, jy, jz);
        } else {
            target.knockback(power * 1.5, -dx, -dz);
            target.hurtMarked = true;
        }
    }

    /** The player walked or ran into a mob: it stumbles, and if they were running it may go down. */
    private static void bump(ServerPlayer player, int targetId, float reported) {
        StumbleConfig cfg = StumbleConfig.get();
        if (!cfg.bump || !cfg.mobKnockdowns || !player.isAlive() || player.isSpectator()) {
            return;
        }
        Entity found = player.level().getEntity(targetId);
        if (!(found instanceof LivingEntity target) || !canKnockDown(target) || player.distanceToSqr(target) > 2.5 * 2.5) {
            return;
        }
        long now = player.level().getGameTime();
        Long last = LAST_BUMP.get(targetId);
        if (last != null && now - last < BUMP_COOLDOWN && now >= last) {
            return;
        }
        if (LAST_BUMP.size() > 512) {
            LAST_BUMP.clear();
        }
        LAST_BUMP.put(targetId, now);

        double speed = Float.isFinite(reported) ? Math.max(0, Math.min(0.6, reported)) : 0;
        double dx = target.getX() - player.getX(), dz = target.getZ() - player.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-3) {
            return;
        }
        // walking (about 0.22 blocks a tick) is a nudge they nearly always catch; sprinting into them
        // sometimes floors them; a sprint-jump into them usually does
        double power = Math.max(0.06, Math.min(0.6, (speed - 0.08) * 1.9)) * cfg.bumpStrength;
        knockDown(target, Ragdoll.Mode.STAGGER, dx / len * power, 0.02, dz / len * power);
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE,
                SoundSource.PLAYERS, 0.5f, 0.8f + player.getRandom().nextFloat() * 0.3f);
    }

    private static boolean canKnockDown(LivingEntity e) {
        return Humanoids.supported(e.getType()) && !(e instanceof Player) && !e.isBaby() && !e.isPassenger()
                && !e.isVehicle() && e.isAlive();
    }

    // ------------------------------------------------------------------ knocking down

    /** Turn a living mob into a ragdoll, or shove the ragdoll it already is. */
    public static void knockDown(LivingEntity e, Ragdoll.Mode mode, double jx, double jy, double jz) {
        Down d = DOWN.get(e);
        if (d != null) {
            if (d.getUp < 0) {
                d.body.impulse(jx, jy, jz);
                tell(e, Net.IMPULSE, () -> Net.vec(e.getId(), jx, jy, jz));
            }
            return;
        }
        long seed = e.getRandom().nextLong();
        Ragdoll body = new Ragdoll(Humanoids.scale(e.getType()), Humanoids.thinLimbs(e.getType()), seed);
        float yaw = e.yBodyRot;
        body.place(e.getX(), e.getY(), e.getZ(), yaw, false, Humanoids.armsForward(e.getType()));
        Vec3 v = e.getDeltaMovement();
        body.launch(v.x, v.y, v.z, jx, jy, jz, mode, true);
        DOWN.put(e, new Down(e, body, seed));
        if (e instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        tell(e, Net.START, () -> Net.start(e.getId(), seed, mode.ordinal(), v.x, v.y, v.z, jx, jy, jz, yaw));
    }

    private static void tick(ServerLevel level) {
        StumbleConfig cfg = StumbleConfig.get();
        ENV.level = level;

        if (cfg.mobKnockdowns && (cfg.knockbackRagdoll || cfg.fallRagdoll)) {
            List<LivingEntity> thrown = null;
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof LivingEntity e) || !canKnockDown(e) || DOWN.containsKey(e)
                        || e.isInWater() || e.isInLava()) {
                    continue;
                }
                Vec3 v = e.getDeltaMovement();
                boolean flung = cfg.knockbackRagdoll && Math.sqrt(v.x * v.x + v.z * v.z) > cfg.knockbackRagdollSpeed;
                boolean falling = cfg.fallRagdoll && e.fallDistance > cfg.fallRagdollHeight && v.y < -0.5;
                if (flung || falling) {
                    if (thrown == null) {
                        thrown = new ArrayList<>();
                    }
                    thrown.add(e);
                }
            }
            if (thrown != null) {
                for (LivingEntity e : thrown) {
                    boolean up = e.getDeltaMovement().y > 0.25;
                    knockDown(e, up ? Ragdoll.Mode.BLAST : Ragdoll.Mode.FALL, 0, 0, 0);
                }
            }
        }

        if (DOWN.isEmpty()) {
            return;
        }
        for (Down d : new ArrayList<>(DOWN.values())) {
            LivingEntity e = d.entity;
            if (e.level() != level) {
                if (e.isRemoved()) {
                    DOWN.remove(e);
                }
                continue;
            }
            if (e.isRemoved() || !e.isAlive()) {
                DOWN.remove(e);         // dead: every game turns its copy into a corpse by itself
                continue;
            }
            d.age++;
            if (d.getUp >= 0) {
                e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
                if (--d.getUp <= 0) {
                    DOWN.remove(e);     // back on its feet; its AI takes over again
                }
                continue;
            }

            // knockback that landed since last tick (a hit, an explosion) shoves the body
            Vec3 now = e.getDeltaMovement();
            double kx = now.x - d.sentX, kz = now.z - d.sentZ;
            if (Math.sqrt(kx * kx + kz * kz) > 0.25 + Math.sqrt(d.sentX * d.sentX + d.sentZ * d.sentZ)) {
                double jy = Math.max(0, now.y) * 0.5;
                d.body.impulse(kx * 0.8, jy, kz * 0.8);
                double fx = kx * 0.8, fz = kz * 0.8;
                tell(e, Net.IMPULSE, () -> Net.vec(e.getId(), fx, jy, fz));
            }

            d.body.tick(ENV);
            Ragdoll.State state = d.body.state();
            if (!d.fellSent && state != Ragdoll.State.STAGGER && state != Ragdoll.State.RECOVERED) {
                d.fellSent = true;
                tell(e, Net.FALL, () -> Net.id(e.getId()));
            }

            // drag the mob along under the body; vanilla movement does the collision
            double mx = clamp(d.body.rootX() - e.getX()), my = clamp(d.body.rootY() - e.getY()),
                    mz = clamp(d.body.rootZ() - e.getZ());
            e.setDeltaMovement(mx, my, mz);
            d.sentX = mx;
            d.sentZ = mz;

            int kind = d.body.getUpKind();
            if (kind != Ragdoll.NOT_READY || d.age > 600) {
                float yaw = d.body.headingYaw();
                int ticks = kind == Ragdoll.STANDING ? GET_UP_STANDING : GET_UP_LYING;
                e.setYRot(yaw);
                e.setYBodyRot(yaw);
                e.setYHeadRot(yaw);
                e.setDeltaMovement(Vec3.ZERO);
                d.getUp = ticks;
                tell(e, Net.END, () -> Net.end(e.getId(), e.getX(), e.getY(), e.getZ(), yaw, ticks));
            }
        }
    }

    private static double clamp(double v) {
        return Math.max(-1.5, Math.min(1.5, v));
    }

    /** Send a packet to everyone who can see the entity and has this mod. */
    private static void tell(Entity about, ResourceLocation channel, java.util.function.Supplier<FriendlyByteBuf> packet) {
        for (ServerPlayer viewer : PlayerLookup.tracking(about)) {
            if (viewer != about && ServerPlayNetworking.canSend(viewer, channel)) {
                ServerPlayNetworking.send(viewer, channel, packet.get());
            }
        }
    }
}
