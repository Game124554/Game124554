package net.realisticoverhaul.net;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.realisticoverhaul.RealisticOverhaul;
import net.realisticoverhaul.config.ROConfig;

import java.util.Collection;
import java.util.UUID;

public final class ModNet {
	public static final Identifier SHAKE = new Identifier(RealisticOverhaul.MOD_ID, "shake");
	public static final Identifier PIGLINS = new Identifier(RealisticOverhaul.MOD_ID, "piglins");
	public static final Identifier SETTINGS = new Identifier(RealisticOverhaul.MOD_ID, "settings");
	private static Boolean lastStepAssist;

	private ModNet() {}

	public static void sendShake(ServerPlayerEntity player, float intensity) {
		if (!ServerPlayNetworking.canSend(player, SHAKE)) return;
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeFloat(intensity);
		ServerPlayNetworking.send(player, SHAKE, buf);
	}

	/** Shake every player near a point. Strength fades with distance. */
	public static void shakeNear(ServerWorld world, Vec3d center, double radius, float intensity) {
		for (ServerPlayerEntity p : world.getPlayers()) {
			double d = p.getPos().distanceTo(center);
			if (d > radius) continue;
			float s = (float) (intensity * (1.0 - d / radius));
			if (s > 0.02f) sendShake(p, s);
		}
	}

	public static void sendPiglins(ServerPlayerEntity player, Collection<UUID> uuids) {
		if (!ServerPlayNetworking.canSend(player, PIGLINS)) return;
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(uuids.size());
		for (UUID u : uuids) buf.writeUuid(u);
		ServerPlayNetworking.send(player, PIGLINS, buf);
	}

	/** Server settings the client must mirror (step assist). */
	public static void sendSettings(ServerPlayerEntity player) {
		if (!ServerPlayNetworking.canSend(player, SETTINGS)) return;
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(ROConfig.get().stepAssist);
		ServerPlayNetworking.send(player, SETTINGS, buf);
	}

	/** Re-send the settings when they change (e.g. from the settings screen in singleplayer). */
	public static void tickSettings(MinecraftServer server) {
		boolean now = ROConfig.get().stepAssist;
		if (lastStepAssist == null || lastStepAssist != now) {
			lastStepAssist = now;
			for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) sendSettings(p);
		}
	}

	public static void resetSettings() {
		lastStepAssist = null;
	}
}
