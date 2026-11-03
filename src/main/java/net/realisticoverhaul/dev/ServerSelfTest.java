package net.realisticoverhaul.dev;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.realisticoverhaul.RealisticOverhaul;
import net.realisticoverhaul.physics.BlockPhysics;
import org.spongepowered.asm.mixin.MixinEnvironment;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Development self-test, only active when the environment variable RO_SELFTEST=server.
 * Used by the GitHub Actions build: renders a terrain height map, runs a few physics /
 * corpse checks, forces every mixin to apply, then stops the server.
 */
public final class ServerSelfTest {
	private static final boolean ON = "server".equals(System.getenv("RO_SELFTEST"));
	private static final List<String> RESULTS = new ArrayList<>();
	private static int tick = -1;
	private static BlockPos base;
	private static ZombieEntity zombie;
	private static CowEntity cow;

	private ServerSelfTest() {}

	public static void init() {
		if (!ON) return;
		ServerLifecycleEvents.SERVER_STARTED.register(ServerSelfTest::onStarted);
		ServerTickEvents.END_SERVER_TICK.register(ServerSelfTest::onTick);
	}

	private static void log(String s) {
		RealisticOverhaul.LOGGER.info("[SELFTEST] " + s);
		RESULTS.add(s);
	}

	private static String tag(MinecraftServer server) {
		String t = System.getenv("RO_TAG");
		if (t != null && !t.isBlank()) return t;
		return server.getDataPackManager().getEnabledNames().contains("realisticoverhaul:realistic_terrain") ? "realistic" : "vanilla";
	}

	private static File dir(MinecraftServer server) {
		File d = new File(server.getRunDirectory(), "selftest");
		d.mkdirs();
		return d;
	}

	private static void onStarted(MinecraftServer server) {
		log("Enabled data packs: " + server.getDataPackManager().getEnabledNames());
		try {
			terrainMap(server);
		} catch (Throwable t) {
			log("FAIL terrain map: " + t);
			RealisticOverhaul.LOGGER.error("terrain map", t);
		}
		tick = 0;
	}

	private static void onTick(MinecraftServer server) {
		if (tick < 0) return;
		tick++;
		ServerWorld w = server.getOverworld();
		try {
			if (tick == 5) {
				BlockPos spawn = w.getSpawnPos();
				// Far enough from spawn to stay out of the client screenshots, close enough to keep ticking.
				base = new BlockPos(spawn.getX() + 100, 240, spawn.getZ() + 100);
				// A floating stone column on one support block.
				w.setBlockState(base, Blocks.STONE.getDefaultState());
				for (int i = 1; i <= 3; i++) w.setBlockState(base.up(i), Blocks.COBBLESTONE.getDefaultState());
				// A platform for the corpse test.
				for (int dx = 3; dx <= 9; dx++) {
					for (int dz = -2; dz <= 2; dz++) w.setBlockState(base.add(dx, 0, dz), Blocks.STONE.getDefaultState());
				}
				zombie = EntityType.ZOMBIE.create(w);
				zombie.refreshPositionAndAngles(base.getX() + 5.5, base.getY() + 1, base.getZ() + 0.5, 0, 0);
				w.spawnEntity(zombie);
				cow = EntityType.COW.create(w);
				cow.refreshPositionAndAngles(base.getX() + 8.5, base.getY() + 1, base.getZ() + 0.5, 0, 0);
				w.spawnEntity(cow);
			} else if (tick == 10) {
				w.breakBlock(base, false);
				BlockPhysics.queue(w, base);
				zombie.damage(w.getDamageSources().generic(), 1000f);
				cow.setHealth(2f);
			} else if (tick == 60) {
				boolean columnGone = w.getBlockState(base.up(3)).isAir();
				int falling = w.getEntitiesByClass(FallingBlockEntity.class, new Box(base).expand(3, 260, 3), e -> true).size();
				boolean landedBelow = false;
				int top = w.getTopY(Heightmap.Type.MOTION_BLOCKING, base.getX(), base.getZ());
				if (w.getBlockState(new BlockPos(base.getX(), top - 1, base.getZ())).isOf(Blocks.COBBLESTONE)) landedBelow = true;
				log((columnGone ? "PASS" : "FAIL") + " collapse: column fell (falling=" + falling + ", landedOnGround=" + landedBelow + ")");
				log((zombie.isDead() && !zombie.isRemoved() ? "PASS" : "FAIL") + " corpse stays after death (deathTime=" + zombie.deathTime + ")");
				var speed = cow.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
				boolean slowed = speed != null && speed.getValue() < speed.getBaseValue() * 0.9;
				log((slowed ? "PASS" : "FAIL") + " wounded cow moves slower (" + (speed == null ? "?" : speed.getValue()) + ")");
				// Explosion with block physics + debris.
				BlockPos boom = base.add(-10, 0, 0);
				for (int dx = -2; dx <= 2; dx++)
					for (int dy = -2; dy <= 2; dy++)
						for (int dz = -2; dz <= 2; dz++) w.setBlockState(boom.add(dx, dy, dz), Blocks.STONE.getDefaultState());
				w.createExplosion(null, boom.getX() + 0.5, boom.getY() + 2.5, boom.getZ() + 0.5, 3f, World.ExplosionSourceType.TNT);
				log("PASS explosion ran");
			} else if (tick == 100) {
				ServerWorld nether = server.getWorld(World.NETHER);
				log("Nether loaded: " + (nether != null));
				MixinEnvironment.getCurrentEnvironment().audit();
				log("PASS mixin audit (server side)");
				finish(server);
			}
		} catch (Throwable t) {
			log("FAIL at tick " + tick + ": " + t);
			RealisticOverhaul.LOGGER.error("selftest", t);
			finish(server);
		}
	}

	private static void finish(MinecraftServer server) {
		tick = -1;
		try {
			Files.write(new File(dir(server), "server_results_" + tag(server) + ".txt").toPath(), RESULTS, StandardCharsets.UTF_8);
		} catch (Exception e) {
			RealisticOverhaul.LOGGER.error("write results", e);
		}
		RealisticOverhaul.LOGGER.info("[SELFTEST] SERVER DONE");
		server.stop(false);
	}

	/** Height map of a 3840 x 3840 block area around spawn, as a shaded PNG. */
	private static void terrainMap(MinecraftServer server) throws Exception {
		ServerWorld w = server.getOverworld();
		ChunkGenerator gen = w.getChunkManager().getChunkGenerator();
		NoiseConfig noise = w.getChunkManager().getNoiseConfig();
		int n = 160, step = 24;
		int cx = w.getSpawnPos().getX(), cz = w.getSpawnPos().getZ();
		int[] h = new int[n * n];
		long t0 = System.currentTimeMillis();
		for (int j = 0; j < n; j++) {
			for (int i = 0; i < n; i++) {
				int x = cx + (i - n / 2) * step;
				int z = cz + (j - n / 2) * step;
				h[j * n + i] = gen.getHeight(x, z, Heightmap.Type.OCEAN_FLOOR_WG, w, noise);
			}
		}
		int sea = gen.getSeaLevel();
		BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
		for (int j = 0; j < n; j++) {
			for (int i = 0; i < n; i++) {
				int y = h[j * n + i];
				int l = h[j * n + Math.max(0, i - 1)], r = h[j * n + Math.min(n - 1, i + 1)];
				int u = h[Math.max(0, j - 1) * n + i], d = h[Math.min(n - 1, j + 1) * n + i];
				float shade = clamp(1f + ((l - r) + (u - d)) * 0.012f, 0.55f, 1.35f);
				int[] c = color(y, sea);
				if (y >= sea) {
					c[0] = (int) clamp(c[0] * shade, 0, 255);
					c[1] = (int) clamp(c[1] * shade, 0, 255);
					c[2] = (int) clamp(c[2] * shade, 0, 255);
				}
				img.setRGB(i, j, (c[0] << 16) | (c[1] << 8) | c[2]);
			}
		}
		String name = "terrain_" + tag(server);
		ImageIO.write(img, "png", new File(dir(server), name + ".png"));

		int[] sorted = h.clone();
		Arrays.sort(sorted);
		long ocean = Arrays.stream(h).filter(v -> v < sea).count();
		long above150 = Arrays.stream(h).filter(v -> v > 150).count();
		long above200 = Arrays.stream(h).filter(v -> v > 200).count();
		long above250 = Arrays.stream(h).filter(v -> v > 250).count();
		double mean = Arrays.stream(h).average().orElse(0);
		log(String.format("%s: %d columns in %d ms | min %d p10 %d p50 %d p90 %d p99 %d max %d | mean %.1f | ocean %.1f%% | >150 %.2f%% | >200 %.2f%% | >250 %.2f%%",
				name, h.length, System.currentTimeMillis() - t0, sorted[0], sorted[h.length / 10], sorted[h.length / 2],
				sorted[h.length * 9 / 10], sorted[h.length * 99 / 100], sorted[h.length - 1], mean,
				100.0 * ocean / h.length, 100.0 * above150 / h.length, 100.0 * above200 / h.length, 100.0 * above250 / h.length));
	}

	private static int[] color(int y, int sea) {
		if (y < sea) {
			float t = clamp((sea - y) / 45f, 0f, 1f);
			return new int[]{(int) (70 - 50 * t), (int) (125 - 85 * t), (int) (205 - 95 * t)};
		}
		float[][] stops = {{sea, 88, 150, 64}, {sea + 40, 125, 165, 80}, {sea + 85, 140, 125, 90}, {sea + 135, 128, 128, 128}, {sea + 185, 245, 245, 250}};
		for (int k = 1; k < stops.length; k++) {
			if (y <= stops[k][0]) {
				float t = (y - stops[k - 1][0]) / (stops[k][0] - stops[k - 1][0]);
				return new int[]{(int) lerp(stops[k - 1][1], stops[k][1], t), (int) lerp(stops[k - 1][2], stops[k][2], t), (int) lerp(stops[k - 1][3], stops[k][3], t)};
			}
		}
		return new int[]{245, 245, 250};
	}

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	private static float clamp(float v, float lo, float hi) {
		return Math.max(lo, Math.min(hi, v));
	}
}
