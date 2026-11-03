package net.realisticoverhaul.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.realisticoverhaul.config.ROConfig;

/** Client visuals: hellish fog/embers, steam where water meets lava, damage shake. */
public final class ClientEffects {
	/** 0 = normal, 1 = full hell look. Smoothed. */
	public static float hellIntensity;
	private static float lastHealth = -1f;

	private ClientEffects() {}

	public static void tick(MinecraftClient client) {
		ClientWorld world = client.world;
		ClientPlayerEntity player = client.player;
		if (world == null || player == null) {
			hellIntensity = 0f;
			lastHealth = -1f;
			CameraShake.reset();
			return;
		}
		if (client.isPaused()) return;
		ROConfig cfg = ROConfig.get();

		CameraShake.tick();
		float health = player.getHealth();
		if (lastHealth >= 0f && health < lastHealth - 0.01f) {
			CameraShake.add(Math.min(0.7f, (lastHealth - health) * 0.07f + 0.12f));
		}
		lastHealth = health;

		float target = 0f;
		if (cfg.hellVisuals) {
			if (world.getRegistryKey() == World.NETHER) {
				target = 1f;
			} else if (world.getRegistryKey() == World.OVERWORLD) {
				target = (float) MathHelper.clamp((cfg.hellStartY - player.getY()) / 40.0, 0.0, 1.0);
			}
		}
		hellIntensity += (target - hellIntensity) * 0.05f;

		Random r = world.random;
		if (hellIntensity > 0.02f) spawnHellParticles(world, player, r, cfg);
		if (cfg.steamParticles) spawnSteam(world, player, r, cfg);
	}

	private static void spawnHellParticles(ClientWorld world, ClientPlayerEntity player, Random r, ROConfig cfg) {
		int n = Math.round(cfg.particleDensity * hellIntensity);
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int i = 0; i < n; i++) {
			double x = player.getX() + (r.nextDouble() - 0.5) * 28;
			double y = player.getY() + (r.nextDouble() * 14) - 4;
			double z = player.getZ() + (r.nextDouble() - 0.5) * 28;
			pos.set(x, y, z);
			if (!world.getBlockState(pos).isAir()) continue;
			float pick = r.nextFloat();
			if (pick < 0.5f) {
				world.addParticle(ParticleTypes.ASH, x, y, z, 0, -0.02, 0);
			} else if (pick < 0.8f) {
				// rising embers
				world.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, (r.nextDouble() - 0.5) * 0.01, 0.02 + r.nextDouble() * 0.02, (r.nextDouble() - 0.5) * 0.01);
			} else if (pick < 0.95f) {
				world.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.01, 0);
			} else {
				world.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
			}
		}
	}

	private static void spawnSteam(ClientWorld world, ClientPlayerEntity player, Random r, ROConfig cfg) {
		int tries = cfg.particleDensity * 4;
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int i = 0; i < tries; i++) {
			pos.set(player.getBlockX() + r.nextInt(25) - 12, player.getBlockY() + r.nextInt(13) - 6, player.getBlockZ() + r.nextInt(25) - 12);
			if (!world.getFluidState(pos).isIn(FluidTags.WATER)) continue;
			boolean nearLava = false;
			for (Direction d : Direction.values()) {
				if (world.getFluidState(pos.offset(d)).isIn(FluidTags.LAVA)) {
					nearLava = true;
					break;
				}
			}
			if (!nearLava) continue;
			double x = pos.getX() + r.nextDouble();
			double y = pos.getY() + 1.0;
			double z = pos.getZ() + r.nextDouble();
			world.addParticle(ParticleTypes.CLOUD, x, y, z, 0, 0.08, 0);
			world.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.05, 0);
			if (r.nextInt(8) == 0) {
				world.playSound(x, y, z, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.3f, 1.6f + r.nextFloat() * 0.4f, false);
			}
		}
	}
}
