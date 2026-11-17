package dev.stumble.client;

import dev.stumble.StumbleConfig;
import dev.stumble.common.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** The push key: shove whoever is in front of you. */
public final class Pushing {
    private static final double REACH = 3.5;
    private static int cooldown;

    private Pushing() {
    }

    public static void tick() {
        if (cooldown > 0) {
            cooldown--;
        }
    }

    public static void push(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null || player.isSpectator() || !player.isAlive()
                || RagdollManager.isLocalDown(player) || cooldown > 0 || !StumbleConfig.get().push) {
            return;
        }
        cooldown = 10;
        // two-handed shove on your model (third person, and for everyone else via the server);
        // in first person the hand swing shows it, without sending the vanilla swing to the server
        PushAnimation.start(player);
        player.swing(InteractionHand.MAIN_HAND, false);
        Vec3 look = player.getLookAngle();
        RagdollManager.kickBodies(player, look);

        LivingEntity target = target(mc, player, look);
        if (ClientPlayNetworking.canSend(Net.PUSH)) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeInt(target == null ? -1 : target.getId());
            ClientPlayNetworking.send(Net.PUSH, buf);
        } else if (target != null) {
            player.displayClientMessage(Component.translatable("stumble.push.needs_server"), true);
        }
    }

    // ------------------------------------------------------------------ walking into people

    private static final double BUMP_MIN_SPEED = 0.12;      // sneaking is about 0.065 blocks a tick, walking 0.22
    private static final java.util.Map<Integer, Long> LAST_BUMP = new java.util.HashMap<>();
    private static long clock;

    /** Called every tick: if you are moving into a mob that can stumble, tell the server. */
    public static void bumps(Minecraft mc) {
        clock++;
        LocalPlayer p = mc.player;
        StumbleConfig cfg = StumbleConfig.get();
        if (p == null || mc.level == null || !cfg.bump || !p.isAlive() || p.isSpectator() || p.isPassenger()
                || RagdollManager.isLocalDown(p) || !ClientPlayNetworking.canSend(Net.BUMP)) {
            return;
        }
        double mx = p.getX() - p.xo, mz = p.getZ() - p.zo;
        double speed = Math.sqrt(mx * mx + mz * mz);
        if (speed < BUMP_MIN_SPEED || speed > 1.0) {
            return;
        }
        double dx = mx / speed, dz = mz / speed;
        net.minecraft.world.phys.AABB reach = p.getBoundingBox().expandTowards(dx * 0.35, 0, dz * 0.35).inflate(0.05);
        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, reach,
                e -> e != p && e.isAlive() && !(e instanceof net.minecraft.world.entity.player.Player)
                        && dev.stumble.common.Humanoids.supported(e.getType()) && !e.isBaby())) {
            double tx = e.getX() - p.getX(), tz = e.getZ() - p.getZ();
            double tl = Math.sqrt(tx * tx + tz * tz);
            if (tl < 1e-3 || (tx * dx + tz * dz) / tl < 0.3) {
                continue;               // only what is in front of you, not what you brush past
            }
            if (clock - LAST_BUMP.getOrDefault(e.getId(), -1000L) < 15) {
                continue;
            }
            if (LAST_BUMP.size() > 256) {
                LAST_BUMP.clear();
            }
            LAST_BUMP.put(e.getId(), clock);
            FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeInt(e.getId());
            buf.writeFloat((float) speed);
            ClientPlayNetworking.send(Net.BUMP, buf);
        }
    }

    /** What the crosshair is on, or else the nearest living thing roughly in front of you. */
    private static LivingEntity target(Minecraft mc, LocalPlayer player, Vec3 look) {
        if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living
                && living.isAlive() && living.distanceToSqr(player) < REACH * REACH) {
            return living;
        }
        Vec3 eye = player.getEyePosition();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(REACH),
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
            double d = to.length();
            if (d > REACH || d < 1e-3) {
                continue;
            }
            double facing = to.scale(1 / d).dot(look);
            if (facing < 0.7) {
                continue;
            }
            double score = d * (2 - facing);
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }
}
