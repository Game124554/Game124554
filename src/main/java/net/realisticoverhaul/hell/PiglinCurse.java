package net.realisticoverhaul.hell;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.realisticoverhaul.config.ROConfig;
import net.realisticoverhaul.net.ModNet;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * When you die you wake up in the Nether as a Piglin.
 * Piglins treat you as one of their own. Leave the Nether to become human again.
 */
public final class PiglinCurse {
	public static final String TAG = "realisticoverhaul_piglin";
	private static Set<UUID> lastSent = new HashSet<>();

	private PiglinCurse() {}

	public static boolean isPiglin(Entity e) {
		return e instanceof PlayerEntity p && p.getCommandTags().contains(TAG);
	}

	public static void onRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
		ROConfig cfg = ROConfig.get();
		if (alive || !cfg.deathToNether) return;
		MinecraftServer server = newPlayer.getServer();
		if (server == null) return;
		ServerWorld nether = server.getWorld(World.NETHER);
		if (nether == null) return;

		boolean diedInNether = oldPlayer.getWorld().getRegistryKey() == World.NETHER;
		double scale = diedInNether ? 1.0 : 0.125;
		int cx = (int) Math.floor(oldPlayer.getX() * scale);
		int cz = (int) Math.floor(oldPlayer.getZ() * scale);

		BlockPos safe = findSafe(nether, cx, cz);
		if (safe == null) safe = buildPlatform(nether, cx, 70, cz);

		newPlayer.teleport(nether, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, newPlayer.getYaw(), newPlayer.getPitch());

		if (cfg.piglinForm) {
			newPlayer.addCommandTag(TAG);
			newPlayer.sendMessage(Text.literal("You died... and woke up in Hell as a Piglin. Escape the Nether to become human again.")
					.formatted(Formatting.DARK_RED), false);
		} else {
			newPlayer.sendMessage(Text.literal("You died... and woke up in Hell.").formatted(Formatting.DARK_RED), false);
		}
	}

	public static void onChangeWorld(ServerPlayerEntity player, ServerWorld origin, ServerWorld destination) {
		if (destination.getRegistryKey() != World.NETHER && player.getCommandTags().contains(TAG)) {
			player.removeCommandTag(TAG);
			player.sendMessage(Text.literal("You escaped Hell and are human again.").formatted(Formatting.GOLD), false);
		}
	}

	/** Keep every client up to date about who looks like a Piglin. */
	public static void tick(MinecraftServer server) {
		if (server.getTicks() % 10 != 0) return;
		Set<UUID> now = current(server);
		if (!now.equals(lastSent)) {
			lastSent = now;
			for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) ModNet.sendPiglins(p, now);
		}
	}

	public static void sendTo(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		if (server != null) ModNet.sendPiglins(player, current(server));
	}

	private static Set<UUID> current(MinecraftServer server) {
		Set<UUID> set = new HashSet<>();
		if (!ROConfig.get().piglinForm) return set;
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			if (p.getCommandTags().contains(TAG)) set.add(p.getUuid());
		}
		return set;
	}

	private static BlockPos findSafe(ServerWorld w, int cx, int cz) {
		for (int r = 0; r <= 24; r += 4) {
			for (int dx = -r; dx <= r; dx += 4) {
				for (int dz = -r; dz <= r; dz += 4) {
					if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) continue;
					int x = cx + dx;
					int z = cz + dz;
					for (int y = 100; y >= 32; y--) {
						BlockPos p = new BlockPos(x, y, z);
						if (!w.getBlockState(p).isAir() || !w.getBlockState(p.up()).isAir()) continue;
						BlockPos down = p.down();
						BlockState below = w.getBlockState(down);
						if (below.isSolidBlock(w, down) && below.getFluidState().isEmpty() && !below.isOf(Blocks.MAGMA_BLOCK)) {
							return p;
						}
					}
				}
			}
		}
		return null;
	}

	private static BlockPos buildPlatform(ServerWorld w, int x, int y, int z) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				w.setBlockState(new BlockPos(x + dx, y - 1, z + dz), Blocks.NETHERRACK.getDefaultState());
				for (int dy = 0; dy < 3; dy++) w.setBlockState(new BlockPos(x + dx, y + dy, z + dz), Blocks.AIR.getDefaultState());
			}
		}
		return new BlockPos(x, y, z);
	}
}
