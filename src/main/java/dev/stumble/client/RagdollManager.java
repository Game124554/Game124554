package dev.stumble.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.stumble.StumbleConfig;
import dev.stumble.common.LevelEnvironment;
import dev.stumble.common.Net;
import dev.stumble.physics.Ragdoll;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Owns every body on this game: dead ones, mobs the server knocked down, other players who went
 * down, and you. Everything here runs on the client thread.
 */
public final class RagdollManager {
    private static final List<Corpse> CORPSES = new ArrayList<>();
    /** Entity id -> the body currently standing in for that entity. */
    private static final Int2ObjectOpenHashMap<Corpse> BOUND = new Int2ObjectOpenHashMap<>();
    /** START packets for entities this game has not been told about yet: id -> {body, ticks waited}. */
    private static final Int2ObjectOpenHashMap<Object[]> PENDING = new Int2ObjectOpenHashMap<>();
    private static final LevelEnvironment ENV = new LevelEnvironment();
    /** Recent explosions as {x, y, z, power, tick}; damage packets often arrive without a position. */
    private static final List<double[]> BLASTS = new ArrayList<>();
    private static long clock;
    private static ClientLevel lastLevel;
    /** Your own body while you are a ragdoll. */
    private static Corpse local;
    private static Vec3 lastVelocity = Vec3.ZERO;

    private RagdollManager() {
    }

    /** Should the vanilla model of this entity be skipped because a ragdoll is drawn in its place? */
    public static boolean hides(Entity entity) {
        if (BOUND.isEmpty()) {
            return false;
        }
        Corpse c = BOUND.get(entity.getId());
        return c != null && c.owner == entity;
    }

    /** You are a ragdoll right now (getting up included), so you cannot move or act. */
    public static boolean isLocalDown(LivingEntity entity) {
        Corpse c = local;
        return c != null && c.owner == entity;
    }

    // ------------------------------------------------------------------ tick

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null) {
            clear(mc);
            lastLevel = null;
            return;
        }
        if (level != lastLevel) {
            clear(mc);
            lastLevel = level;
        }
        if (mc.isPaused()) {
            return;
        }
        StumbleConfig cfg = StumbleConfig.get();
        clock++;
        ENV.level = level;

        if (cfg.enabled) {
            for (Entity e : level.entitiesForRendering()) {
                if (e instanceof LivingEntity living && living.isDeadOrDying() && living.deathTime <= 5
                        && !BOUND.containsKey(e.getId())) {
                    spawnCorpse(level, living, cfg);
                }
            }
        }

        LocalPlayer player = mc.player;
        if (player != null) {
            if (local == null && cfg.enabled && cfg.players) {
                checkFall(player, cfg);
            }
            lastVelocity = player.getDeltaMovement();
        }

        retryPending(level);

        Iterator<Corpse> it = CORPSES.iterator();
        while (it.hasNext()) {
            Corpse c = it.next();
            boolean remove = false;

            if (c.owner != null) {
                Entity now = level.getEntity(c.owner.getId());
                LivingEntity owner = (LivingEntity) c.owner;
                if (c.owner.isRemoved() || now != c.owner) {
                    unbind(c);                          // the entity is gone
                    remove = c.alive;                   // a dead body stays; a living one has nothing to stand in for
                } else if (c.alive && owner.isDeadOrDying()) {
                    die(mc, c, cfg);                    // killed while down: carry straight on as a corpse
                } else if (!c.alive && !owner.isDeadOrDying()) {
                    unbind(c);                          // it was not dead after all (totem, another mod)
                    remove = true;
                }
            }

            c.age++;
            c.body.tick(ENV);
            if (!c.alive) {
                kick(level, c);
            }
            thud(level, c, cfg);

            if (c.alive && !remove) {
                if (c.body.isUp()) {
                    remove = true;                      // standing again: hand back to the normal model
                } else if (c.local && player != null && c.owner == player) {
                    driveLocal(mc, c, player);
                } else if (c.owner != null) {
                    followEntity(c, (LivingEntity) c.owner);
                }
            }

            if (!c.alive && c.age >= c.lifetime) {
                remove = true;
            }
            if (c.body.x[Ragdoll.CHEST * 3 + 1] < level.getMinBuildHeight() - 32) {
                remove = true;
            }
            if (remove) {
                release(mc, c);
                it.remove();
            }
        }
    }

    private static void release(Minecraft mc, Corpse c) {
        unbind(c);
        if (c == local) {
            local = null;
            restoreCamera(mc, c);
        }
    }

    private static void die(Minecraft mc, Corpse c, StumbleConfig cfg) {
        c.alive = false;
        c.body.kill();
        c.age = 0;
        c.lifetime = cfg.lifetimeSeconds * 20;
        if (c == local) {
            local = null;
            c.local = false;
            restoreCamera(mc, c);
        }
    }

    private static void unbind(Corpse c) {
        if (c.owner != null) {
            if (BOUND.get(c.owner.getId()) == c) {
                BOUND.remove(c.owner.getId());
            }
            c.owner = null;
        }
    }

    private static void clear(Minecraft mc) {
        if (local != null) {
            restoreCamera(mc, local);
        }
        local = null;
        CORPSES.clear();
        BOUND.clear();
        PENDING.clear();
        BLASTS.clear();
        ENV.level = null;
    }

    /** Make room: drop the oldest dead body. Living ones are never dropped. */
    private static void makeRoom(StumbleConfig cfg) {
        int dead = 0;
        for (Corpse c : CORPSES) {
            if (!c.alive) {
                dead++;
            }
        }
        Iterator<Corpse> it = CORPSES.iterator();
        while (dead >= cfg.maxRagdolls && it.hasNext()) {
            Corpse c = it.next();
            if (!c.alive) {
                unbind(c);
                it.remove();
                dead--;
            }
        }
    }

    // ------------------------------------------------------------------ you

    /** Fall far enough, or get thrown hard enough, and you go down. */
    private static void checkFall(LocalPlayer p, StumbleConfig cfg) {
        if (!p.isAlive() || p.isSpectator() || p.getAbilities().flying || p.isFallFlying() || p.isPassenger()
                || p.isInWater() || p.isInLava() || p.onClimbable() || p.isSleeping() || p.isAutoSpinAttack()
                || p.hasEffect(MobEffects.SLOW_FALLING) || p.hasEffect(MobEffects.LEVITATION)) {
            return;
        }
        Vec3 v = p.getDeltaMovement();
        if (cfg.fallRagdoll && p.fallDistance > cfg.fallRagdollHeight && v.y < -0.5) {
            goDown(p, Ragdoll.Mode.FALL, 0, 0, 0);
            return;
        }
        if (cfg.knockbackRagdoll) {
            double jx = v.x - lastVelocity.x, jz = v.z - lastVelocity.z, jy = v.y - lastVelocity.y;
            if (Math.sqrt(jx * jx + jz * jz) > cfg.knockbackRagdollSpeed) {
                goDown(p, jy > 0.25 ? Ragdoll.Mode.BLAST : Ragdoll.Mode.FALL, 0, 0, 0);
            }
        }
    }

    /** The server says someone pushed you. */
    public static void knockedDown(int mode, double jx, double jy, double jz) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !p.isAlive()) {
            return;
        }
        if (local != null && local.owner == p) {
            local.body.impulse(jx, jy, jz);
            return;
        }
        StumbleConfig cfg = StumbleConfig.get();
        if (cfg.enabled && cfg.players && !p.isPassenger() && !p.getAbilities().flying && !p.isSpectator()) {
            goDown(p, Ragdoll.Mode.of(mode), jx, jy, jz);
        } else {
            p.push(jx, jy + 0.1, jz);       // no ragdoll wanted: an ordinary shove
        }
    }

    private static void goDown(LocalPlayer p, Ragdoll.Mode mode, double jx, double jy, double jz) {
        Minecraft mc = Minecraft.getInstance();
        BodyType type = BodyType.of(p);
        ResourceLocation texture = type == null ? null : textureOf(p);
        if (texture == null || BOUND.containsKey(p.getId())) {
            return;
        }
        long seed = p.getRandom().nextLong();
        float scale = BodyType.scaleOf(p);
        Ragdoll body = new Ragdoll(scale, type.thinLimbs, seed);
        float yaw = p.yBodyRot;
        body.place(p.getX(), p.getY(), p.getZ(), yaw, false, false);
        Vec3 v = p.getDeltaMovement();
        body.launch(v.x, v.y, v.z, jx, jy, jz, mode, true);

        Corpse c = new Corpse(body, type, texture, scale, Integer.MAX_VALUE, p);
        c.alive = true;
        c.local = true;
        CORPSES.add(c);
        BOUND.put(p.getId(), c);
        local = c;
        p.setSprinting(false);

        if (StumbleConfig.get().ragdollCamera && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            c.savedCamera = CameraType.FIRST_PERSON;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        if (ClientPlayNetworking.canSend(Net.SELF_START)) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            Net.writeBody(buf, seed, mode.ordinal(), v.x, v.y, v.z, jx, jy, jz, yaw);
            ClientPlayNetworking.send(Net.SELF_START, buf);
        }
    }

    /** Your player is carried along under your body; vanilla movement does the collision. */
    private static void driveLocal(Minecraft mc, Corpse c, LocalPlayer p) {
        Ragdoll body = c.body;
        p.setSprinting(false);
        if (c.gettingUp) {
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(0, Math.min(0, v.y), 0);
            return;
        }
        if (!c.fallSent && body.state() != Ragdoll.State.STAGGER && body.state() != Ragdoll.State.RECOVERED) {
            c.fallSent = true;
            if (ClientPlayNetworking.canSend(Net.SELF_FALL)) {
                ClientPlayNetworking.send(Net.SELF_FALL, PacketByteBufs.create());
            }
        }
        // if the player got stuck where the body could go (under a low ceiling), bring the body back
        double ex = p.getX() - body.rootX(), ez = p.getZ() - body.rootZ();
        if (ex * ex + ez * ez > 1.0) {
            body.shift(ex * 0.3, 0, ez * 0.3);
        }
        p.setDeltaMovement(clamp(body.rootX() - p.getX(), 1.5), clamp(body.rootY() - p.getY(), 1.5),
                clamp(body.rootZ() - p.getZ(), 1.5));

        int kind = body.getUpKind();
        if (kind != Ragdoll.NOT_READY) {
            float yaw = body.headingYaw();
            int ticks = kind == Ragdoll.STANDING ? 5 : 20;
            body.beginGetUp(p.getX(), p.getY(), p.getZ(), yaw, ticks);
            c.gettingUp = true;
            p.setYBodyRot(yaw);
            p.setDeltaMovement(0, Math.min(0, p.getDeltaMovement().y), 0);
            if (ClientPlayNetworking.canSend(Net.SELF_END)) {
                FriendlyByteBuf buf = PacketByteBufs.create();
                buf.writeFloat(yaw);
                buf.writeByte(ticks);
                ClientPlayNetworking.send(Net.SELF_END, buf);
            }
        }
    }

    private static void restoreCamera(Minecraft mc, Corpse c) {
        if (c.savedCamera != null) {
            if (mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
                mc.options.setCameraType(c.savedCamera);
            }
            c.savedCamera = null;
        }
    }

    // ------------------------------------------------------------------ others (server news)

    /** Keep someone else's body on top of where the server says that entity is. */
    private static void followEntity(Corpse c, LivingEntity e) {
        if (c.gettingUp) {
            return;
        }
        Ragdoll body = c.body;
        double ex = e.getX() - body.rootX(), ez = e.getZ() - body.rootZ(), ey = e.getY() - body.rootY();
        body.shift(ex * 0.15, Math.abs(ey) > 0.4 ? ey * 0.15 : 0, ez * 0.15);
        if (c.age > 30 * 20) {
            // never heard the end of it: get up where the entity is
            body.beginGetUp(e.getX(), e.getY(), e.getZ(), e.yBodyRot, 20);
            c.gettingUp = true;
        }
    }

    public static void remoteStart(int id, Net.Body info) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        StumbleConfig cfg = StumbleConfig.get();
        if (level == null || !cfg.enabled) {
            return;
        }
        Entity found = level.getEntity(id);
        if (found == null) {
            PENDING.put(id, new Object[]{info, 0});     // the entity itself has not arrived yet
            return;
        }
        if (!(found instanceof LivingEntity e) || e == mc.player || !e.isAlive() || BOUND.containsKey(id)) {
            return;
        }
        BodyType type = BodyType.of(e);
        if (type == null || (e instanceof Player ? !cfg.players : !cfg.mobs)) {
            return;
        }
        ResourceLocation texture = textureOf(e);
        if (texture == null) {
            return;
        }
        float scale = BodyType.scaleOf(e);
        Ragdoll body = new Ragdoll(scale, type.thinLimbs, info.seed());
        body.place(e.getX(), e.getY(), e.getZ(), info.yaw(), false, type == BodyType.ZOMBIE);
        body.launch(info.vx(), info.vy(), info.vz(), info.jx(), info.jy(), info.jz(), Ragdoll.Mode.of(info.mode()), true);
        Corpse c = new Corpse(body, type, texture, scale, Integer.MAX_VALUE, e);
        c.alive = true;
        CORPSES.add(c);
        BOUND.put(id, c);
    }

    private static void retryPending(ClientLevel level) {
        if (PENDING.isEmpty()) {
            return;
        }
        for (int id : PENDING.keySet().toIntArray()) {
            Object[] p = PENDING.get(id);
            int waited = (Integer) p[1];
            if (level.getEntity(id) != null) {
                PENDING.remove(id);
                remoteStart(id, (Net.Body) p[0]);
            } else if (waited > 40) {
                PENDING.remove(id);
            } else {
                p[1] = waited + 1;
            }
        }
    }

    private static Corpse living(int id) {
        Corpse c = BOUND.get(id);
        return c != null && c.alive && !c.local ? c : null;
    }

    public static void remoteFall(int id) {
        Corpse c = living(id);
        if (c != null) {
            c.body.forceFall();
        }
    }

    public static void remoteImpulse(int id, double jx, double jy, double jz) {
        Corpse c = living(id);
        if (c != null) {
            c.body.impulse(jx, jy, jz);
        }
    }

    public static void remoteEnd(int id, double x, double y, double z, float yaw, int ticks) {
        PENDING.remove(id);
        Corpse c = living(id);
        if (c == null || c.gettingUp) {
            return;
        }
        // our copy may have gone down where the server's caught itself: never leap up in a blink
        int t = c.body.isLying() ? Math.max(ticks, 20) : Math.max(ticks, 1);
        c.body.beginGetUp(x, y, z, yaw, t);
        c.gettingUp = true;
    }

    // ------------------------------------------------------------------ dead bodies

    /** Called for every explosion the server tells us about. */
    public static void onExplosion(double x, double y, double z, float power) {
        if (BLASTS.size() >= 32) {
            BLASTS.remove(0);
        }
        BLASTS.add(new double[]{x, y, z, power, clock});
        double radius = power * 2.0 + 1.0;
        double strength = 0.5 * StumbleConfig.get().force;
        for (Corpse c : CORPSES) {
            if (!c.alive) {
                c.body.blast(x, y, z, radius, strength);   // living bodies get their shove from the server
            }
        }
    }

    /** The push key also shoves dead bodies lying in front of you. */
    public static void kickBodies(Player player, Vec3 look) {
        double hx = look.x, hz = look.z, hl = Math.sqrt(hx * hx + hz * hz);
        if (hl < 1e-3) {
            return;
        }
        hx /= hl;
        hz /= hl;
        double power = 0.3 * StumbleConfig.get().pushStrength * (player.isSprinting() ? 1.5 : 1.0);
        for (Corpse c : CORPSES) {
            if (c.alive) {
                continue;
            }
            double ex = c.body.rootX() - player.getX(), ez = c.body.rootZ() - player.getZ();
            double ey = c.body.rootY() - player.getY();
            double d = Math.sqrt(ex * ex + ez * ez);
            if (d > 2.2 || Math.abs(ey) > 1.5 || (d > 0.3 && (ex * hx + ez * hz) / d < 0.4)) {
                continue;
            }
            c.body.impulse(hx * power, 0.12, hz * power);
        }
    }

    /** The explosion that most plausibly just killed something standing at (x, y, z), or null. */
    private static double[] recentBlast(double x, double y, double z) {
        double[] best = null;
        double bestD = 14 * 14;
        for (double[] b : BLASTS) {
            if (clock - b[4] > 10) {
                continue;
            }
            double d = (b[0] - x) * (b[0] - x) + (b[1] - y) * (b[1] - y) + (b[2] - z) * (b[2] - z);
            if (d < bestD) {
                bestD = d;
                best = b;
            }
        }
        return best;
    }

    private static void spawnCorpse(ClientLevel level, LivingEntity living, StumbleConfig cfg) {
        BodyType type = BodyType.of(living);
        if (type == null || living.isBaby() || living.isInvisible() || living.isSpectator()) {
            return;
        }
        if (living instanceof Player ? !cfg.players : !cfg.mobs) {
            return;
        }
        ResourceLocation texture = textureOf(living);
        if (texture == null) {
            return;
        }
        makeRoom(cfg);

        float scale = BodyType.scaleOf(living);
        Ragdoll body = new Ragdoll(scale, type.thinLimbs, level.random.nextLong());
        Pose pose = living.getPose();
        boolean prone = pose == Pose.SWIMMING || pose == Pose.FALL_FLYING || pose == Pose.SLEEPING;
        body.place(living.getX(), living.getY(), living.getZ(), living.yBodyRot, prone, type == BodyType.ZOMBIE);

        // what the entity was already doing
        double vx = clamp(living.getX() - living.xo, 1.2);
        double vy = clamp(living.getY() - living.yo, 1.2);
        double vz = clamp(living.getZ() - living.zo, 1.2);

        // what killed it
        double jx = 0, jy = 0, jz = 0;
        Ragdoll.Mode mode = prone ? Ragdoll.Mode.LIMP : Ragdoll.Mode.STAGGER;
        DamageSource source = living.getLastDamageSource();
        if (source != null) {
            boolean blast = source.is(DamageTypeTags.IS_EXPLOSION);
            double power = blast ? 0.55 : source.is(DamageTypeTags.IS_PROJECTILE) ? 0.38 : 0.28;
            if (blast) {
                mode = Ragdoll.Mode.BLAST;
            }
            if (source.is(DamageTypeTags.IS_FALL)) {
                mode = Ragdoll.Mode.LIMP;
                power = 0;
            }
            Vec3 from = source.getSourcePosition();
            if (blast) {
                // the TNT or creeper is usually gone by now, so trust the explosion packet instead
                double[] b = recentBlast(living.getX(), living.getY(), living.getZ());
                if (b != null) {
                    from = new Vec3(b[0], b[1], b[2]);
                }
            }
            power *= cfg.force;
            if (power > 0) {
                double ox, oz, len;
                if (from != null) {
                    ox = living.getX() - from.x;
                    oz = living.getZ() - from.z;
                    len = Math.sqrt(ox * ox + oz * oz);
                } else {
                    // no idea where it came from: pick a direction rather than drop straight down
                    double a = level.random.nextDouble() * Math.PI * 2;
                    ox = Math.cos(a);
                    oz = Math.sin(a);
                    len = 1;
                    if (!blast) {
                        power *= 0.4;
                    }
                }
                if (blast) {
                    power *= Math.max(0.35, Math.min(1.0, 1.4 - len / 6.0));   // weaker further out
                }
                if (len > 1e-3) {
                    jx = ox / len * power;
                    jz = oz / len * power;
                }
                jy = blast ? power * 0.7 : power * 0.2;
            }
        }

        body.launch(vx, vy, vz, jx, jy, jz, mode, false);

        Corpse corpse = new Corpse(body, type, texture, scale, cfg.lifetimeSeconds * 20, living);
        CORPSES.add(corpse);
        BOUND.put(living.getId(), corpse);
    }

    private static ResourceLocation textureOf(LivingEntity living) {
        try {
            EntityRenderer<? super LivingEntity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(living);
            return renderer.getTextureLocation(living);
        } catch (RuntimeException e) {
            return null;        // some other mod's renderer did not like being asked; leave vanilla alone
        }
    }

    private static double clamp(double v, double limit) {
        return v > limit ? limit : (v < -limit ? -limit : v);
    }

    /** Living things walking through a dead body push its limbs around. */
    private static void kick(ClientLevel level, Corpse c) {
        double[] x = c.body.x;
        double minX = x[0], minY = x[1], minZ = x[2], maxX = x[0], maxY = x[1], maxZ = x[2];
        for (int i = 3; i < x.length; i += 3) {
            minX = Math.min(minX, x[i]); maxX = Math.max(maxX, x[i]);
            minY = Math.min(minY, x[i + 1]); maxY = Math.max(maxY, x[i + 1]);
            minZ = Math.min(minZ, x[i + 2]); maxZ = Math.max(maxZ, x[i + 2]);
        }
        AABB around = new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(0.1);
        for (Entity e : level.getEntities((Entity) null, around)) {
            if (e == c.owner || !(e instanceof LivingEntity living) || living.isDeadOrDying() || e.isSpectator()) {
                continue;
            }
            double mx = e.getX() - e.xo, mz = e.getZ() - e.zo;
            if (mx * mx + mz * mz < 0.03 * 0.03) {
                continue;
            }
            AABB b = e.getBoundingBox();
            c.body.nudge(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, mx, mz);
        }
    }

    private static void thud(ClientLevel level, Corpse c, StumbleConfig cfg) {
        double impact = c.body.consumeImpact();
        if (!cfg.impactSounds || impact < 0.28) {
            return;
        }
        double[] x = c.body.x;
        float volume = (float) Math.max(0.2, Math.min(1.0, impact * 0.9));
        float pitch = 0.8f + level.random.nextFloat() * 0.3f;
        level.playLocalSound(x[Ragdoll.CHEST * 3], x[Ragdoll.CHEST * 3 + 1], x[Ragdoll.CHEST * 3 + 2],
                impact > 0.7 ? SoundEvents.PLAYER_BIG_FALL : SoundEvents.PLAYER_SMALL_FALL,
                SoundSource.NEUTRAL, volume, pitch, false);
    }

    // ------------------------------------------------------------------ render

    public static void render(WorldRenderContext ctx) {
        if (CORPSES.isEmpty()) {
            return;
        }
        MultiBufferSource buffers = ctx.consumers();
        ClientLevel level = Minecraft.getInstance().level;
        if (buffers == null || level == null) {
            return;
        }
        PoseStack pose = ctx.matrixStack();
        Vec3 cam = ctx.camera().getPosition();
        Entity viewer = ctx.camera().getEntity();
        boolean firstPerson = !ctx.camera().isDetached();
        float partial = ctx.tickDelta();
        for (Corpse c : CORPSES) {
            if (firstPerson && c.owner != null && c.owner == viewer) {
                continue;       // do not fill your own view with the inside of your head
            }
            RagdollRenderer.render(c, pose, buffers, cam, partial, level);
        }
    }
}
