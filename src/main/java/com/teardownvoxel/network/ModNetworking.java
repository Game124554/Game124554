package com.teardownvoxel.network;

import com.teardownvoxel.TeardownVoxel;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class ModNetworking {
    public static final Identifier SCREEN_SHAKE = TeardownVoxel.id("screen_shake");

    private ModNetworking() {
    }

    /**
     * Sends a camera shake to every player within {@code radius}, with intensity falling off linearly
     * with distance.
     */
    public static void sendShake(ServerWorld world, Vec3d origin, float intensity, double radius, int durationTicks) {
        double r2 = radius * radius;
        for (ServerPlayerEntity player : world.getPlayers()) {
            double d2 = player.squaredDistanceTo(origin);
            if (d2 > r2) continue;
            float scaled = (float) (intensity * (1.0 - Math.sqrt(d2) / radius));
            if (scaled < 0.05f) continue;
            if (!ServerPlayNetworking.canSend(player, SCREEN_SHAKE)) continue;
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeFloat(scaled);
            buf.writeVarInt(durationTicks);
            ServerPlayNetworking.send(player, SCREEN_SHAKE, buf);
        }
    }
}
