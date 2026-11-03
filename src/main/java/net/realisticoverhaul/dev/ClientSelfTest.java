package net.realisticoverhaul.dev;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.realisticoverhaul.RealisticOverhaul;
import net.realisticoverhaul.client.ConfigScreen;
import net.realisticoverhaul.client.smooth.SmoothTerrain;
import net.realisticoverhaul.config.ROConfig;
import net.realisticoverhaul.mixin.client.IntegratedServerLoaderInvoker;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Development self-test, only active when RO_SELFTEST=client (GitHub Actions, virtual screen).
 * Opens the "selftest" world, takes screenshots with smooth terrain on and off, of corpses,
 * and of the Nether after dying as a Piglin, forces every mixin to apply, then quits.
 */
public final class ClientSelfTest {
	private static final boolean ON = "client".equals(System.getenv("RO_SELFTEST"));
	private static final List<String> RESULTS = new ArrayList<>();
	private static final int TIMEOUT_TICKS = 20 * 60 * 14;

	private static int phase;
	private static int phaseTicks;
	private static int totalTicks;
	private static int nightShotAt = -1;
	private static volatile double[] farView;
	private static volatile double[] nearView;
	private static volatile double[] mobSpot;

	private ClientSelfTest() {}

	public static void init() {
		if (!ON) return;
		ClientTickEvents.END_CLIENT_TICK.register(ClientSelfTest::tick);
	}

	private static void log(String s) {
		RealisticOverhaul.LOGGER.info("[SELFTEST] " + s);
		RESULTS.add(s);
	}

	private static void next() {
		phase++;
		phaseTicks = 0;
		log("phase " + phase + " at tick " + totalTicks);
	}

	private static void cmd(MinecraftClient client, String command) {
		MinecraftServer server = client.getServer();
		if (server == null) return;
		server.execute(() -> server.getCommandManager().executeWithPrefix(server.getCommandSource(), command));
	}

	private static void tp(MinecraftClient client, double[] v) {
		cmd(client, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f %.1f %.1f", v[0], v[1], v[2], v[3], v[4]));
	}

	/** Ready to take a picture: chunks are built (or we waited long enough). */
	private static boolean ready(MinecraftClient client, int minTicks) {
		if (phaseTicks < minTicks) return false;
		return client.worldRenderer.isTerrainRenderComplete() || phaseTicks > minTicks + 900;
	}

	private static void shot(MinecraftClient client, String name) {
		ScreenshotRecorder.saveScreenshot(client.runDirectory, name + ".png", client.getFramebuffer(),
				msg -> RealisticOverhaul.LOGGER.info("[SELFTEST] " + msg.getString()));
		log("screenshot " + name + " (chunks: " + client.worldRenderer.getChunksDebugString() + ", fps " + client.getCurrentFps() + ")");
	}

	private static void tick(MinecraftClient client) {
		if (phase < 0) return;
		totalTicks++;
		phaseTicks++;
		try {
			if (totalTicks > TIMEOUT_TICKS) {
				log("FAIL timeout in phase " + phase);
				finish(client);
				return;
			}
			switch (phase) {
				case 0 -> {
					if (client.currentScreen instanceof TitleScreen && client.getOverlay() == null && phaseTicks > 40) {
						log("Opening world 'selftest'");
						((IntegratedServerLoaderInvoker) client.createIntegratedServerLoader())
								.realisticoverhaul$start(client.currentScreen, "selftest", false, false);
						next();
					}
				}
				case 1 -> {
					if (client.world != null && client.player != null && client.getServer() != null && phaseTicks > 20) {
						client.options.hudHidden = true;
						client.options.getViewDistance().setValue(8);
						for (String c : new String[]{"gamemode spectator @a", "gamerule doDaylightCycle false", "gamerule doWeatherCycle false",
								"gamerule doMobSpawning false", "time set 5000", "weather clear", "difficulty peaceful"}) cmd(client, c);
						MinecraftServer server = client.getServer();
						server.execute(() -> pickViews(server.getOverworld()));
						next();
					}
				}
				case 2 -> {
					if (farView != null && phaseTicks > 10) {
						log(String.format("views: far %s near %s", java.util.Arrays.toString(farView), java.util.Arrays.toString(nearView)));
						tp(client, farView);
						next();
					}
				}
				case 3 -> {
					if (ready(client, 200)) {
						shot(client, "1_smooth_far");
						tp(client, nearView);
						next();
					}
				}
				case 4 -> {
					if (ready(client, 140)) {
						shot(client, "2_smooth_near");
						ROConfig.get().smoothTerrain = false;
						SmoothTerrain.reload();
						SmoothTerrain.rebuildChunks();
						next();
					}
				}
				case 5 -> {
					if (ready(client, 140)) {
						shot(client, "3_cubes_near");
						tp(client, farView);
						next();
					}
				}
				case 6 -> {
					if (ready(client, 140)) {
						shot(client, "4_cubes_far");
						ROConfig.get().smoothTerrain = true;
						SmoothTerrain.reload();
						SmoothTerrain.rebuildChunks();
						// Corpse test on a platform in the open air, so trees and water are out of the way.
						double[] m = mobSpot;
						int px = (int) Math.floor(m[0]), pz = (int) Math.floor(m[2]), py = (int) m[1] + 20;
						cmd(client, String.format(java.util.Locale.ROOT, "fill %d %d %d %d %d %d minecraft:smooth_stone", px - 4, py, pz - 4, px + 4, py, pz + 5));
						String[] mobs = {"zombie", "pig", "skeleton", "creeper", "villager", "cow"};
						for (int i = 0; i < mobs.length; i++) {
							double ox = (i % 3 - 1) * 2.4, oz = (i / 3) * 2.8;
							cmd(client, String.format(java.util.Locale.ROOT, "summon %s %.1f %d %.1f {NoAI:1b}", mobs[i], px + 0.5 + ox, py + 1, pz + 0.5 + oz));
						}
						tp(client, new double[]{px - 4.0, py + 4.2, pz - 3.5, -45, 40});
						next();
					}
				}
				case 7 -> {
					if (phaseTicks == 40) {
						cmd(client, "kill @e[type=!player]");
					}
					if (phaseTicks == 75) shot(client, "5a_corpses_falling");
					if (phaseTicks > 160 && ready(client, 160)) {
						shot(client, "5b_corpses");
						phase = 19;
						next(); // -> 20
					}
				}
				case 20 -> { // rain puddles
					int[] b = base();
					int qx = b[0] + 16, py = b[1], pz = b[2];
					if (phaseTicks == 1) {
						cmd(client, f("fill %d %d %d %d %d %d minecraft:smooth_stone", qx, py, pz - 5, qx + 11, py, pz + 7));
						cmd(client, f("fill %d %d %d %d %d %d realisticoverhaul:puddle", qx + 2, py + 1, pz - 1, qx + 6, py + 1, pz + 3));
						cmd(client, f("fill %d %d %d %d %d %d realisticoverhaul:puddle", qx + 5, py + 1, pz + 2, qx + 9, py + 1, pz + 5));
						cmd(client, f("setblock %d %d %d realisticoverhaul:puddle", qx + 2, py + 1, pz + 5));
						cmd(client, f("setblock %d %d %d realisticoverhaul:puddle", qx + 9, py + 1, pz - 2));
						cmd(client, "weather rain");
						tp(client, new double[]{qx + 5.5, py + 4.5, pz - 5.0, 0, 48});
					}
					if (phaseTicks > 120 && ready(client, 120)) {
						shot(client, "9_puddles_rain");
						next();
					}
				}
				case 21 -> { // snow drifts
					int[] b = base();
					int sx = b[0] + 34, py = b[1], pz = b[2];
					if (phaseTicks == 1) {
						cmd(client, "weather clear");
						cmd(client, f("fill %d %d %d %d %d %d minecraft:smooth_stone", sx - 1, py, pz - 5, sx + 10, py, pz + 9));
						cmd(client, f("fill %d %d %d %d %d %d minecraft:stone", sx + 9, py + 1, pz - 5, sx + 10, py + 2, pz + 9));
						for (int x = 0; x <= 8; x++) {
							for (int z = -4; z <= 8; z++) {
								int layers = (int) Math.round(3 + 2.5 * Math.sin(x * 0.7) * Math.cos(z * 0.5) + (x >= 7 ? 2 : 0));
								layers = Math.max(1, Math.min(8, layers));
								cmd(client, f("setblock %d %d %d minecraft:snow[layers=%d]", sx + x, py + 1, pz + z, layers));
							}
						}
						tp(client, new double[]{sx + 4.5, py + 4.2, pz - 7.5, 0, 33});
					}
					if (phaseTicks == 160) {
						shot(client, "10a_smooth_snow");
						ROConfig.get().smoothSnow = false;
						SmoothTerrain.rebuildChunks();
					}
					if (phaseTicks > 300 && ready(client, 300)) {
						shot(client, "10b_snow_layers_vanilla");
						ROConfig.get().smoothSnow = true;
						SmoothTerrain.rebuildChunks();
						next();
					}
				}
				case 22 -> { // creepy mobs at night
					int[] b = base();
					int nx = b[0] + 52, py = b[1], pz = b[2];
					if (phaseTicks == 1) {
						cmd(client, "difficulty easy");
						cmd(client, "time set 18000");
						cmd(client, f("fill %d %d %d %d %d %d minecraft:smooth_stone", nx - 1, py, pz - 2, nx + 13, py, pz + 10));
						String[] mobs = {"zombie", "skeleton", "husk", "enderman", "creeper", "spider"};
						for (int i = 0; i < mobs.length; i++) {
							cmd(client, f("summon minecraft:%s %d.5 %d %d.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f]}", mobs[i], nx + 1 + i * 2, py + 1, pz + 1));
						}
						tp(client, new double[]{nx + 6.5, py + 2.8, pz + 8.5, 180, 12});
						client.options.getGamma().setValue(1.0);
					}
					if (nightShotAt < 0 && phaseTicks > 160 && ready(client, 160)) {
						shot(client, "11_night_mobs");
						cmd(client, "time set 12900");
						nightShotAt = phaseTicks;
					}
					if (nightShotAt > 0 && phaseTicks == nightShotAt + 40) {
						shot(client, "12_dusk_mobs");
						cmd(client, "kill @e[type=!player]");
						cmd(client, "difficulty peaceful");
						cmd(client, "time set 5000");
						client.options.getGamma().setValue(0.5);
						client.setScreen(new ConfigScreen(null));
						phase = 7;
						next(); // -> 8
					}
				}
				case 8 -> {
					if (phaseTicks == 30) {
						shot(client, "7_settings_screen");
						client.setScreen(new OptionsScreen(null, client.options));
					}
					if (phaseTicks == 60) {
						shot(client, "8_options_button");
						client.setScreen(null);
						cmd(client, "gamemode survival @a");
						next();
					}
				}
				case 9 -> {
					if (phaseTicks == 20) cmd(client, "kill @a");
					if (phaseTicks > 40 && client.currentScreen instanceof DeathScreen) {
						client.player.requestRespawn();
						client.setScreen(null);
						log("respawn requested");
						next();
					}
					if (phaseTicks > 400) {
						log("FAIL no death screen");
						next();
					}
				}
				case 10 -> {
					if (phaseTicks == 60) {
						client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
						client.options.hudHidden = true;
					}
					if (ready(client, 260)) {
						String dim = client.world.getRegistryKey().getValue().toString();
						log((dim.equals("minecraft:the_nether") ? "PASS" : "FAIL") + " respawned in " + dim);
						shot(client, "6_nether_piglin");
						next();
					}
				}
				case 11 -> {
					MixinEnvironment.getCurrentEnvironment().audit();
					log("PASS mixin audit (client side)");
					finish(client);
				}
				default -> {
				}
			}
		} catch (Throwable t) {
			log("FAIL in phase " + phase + ": " + t);
			RealisticOverhaul.LOGGER.error("[SELFTEST] client", t);
			finish(client);
		}
	}

	private static String f(String fmt, Object... args) {
		return String.format(java.util.Locale.ROOT, fmt, args);
	}

	/** Corner of the test platforms (above the mob spot). */
	private static int[] base() {
		double[] m = mobSpot;
		return new int[]{(int) Math.floor(m[0]), (int) m[1] + 20, (int) Math.floor(m[2])};
	}

	/** Server thread: find a hill near spawn and camera spots looking at it. */
	private static void pickViews(ServerWorld w) {
		int sx = w.getSpawnPos().getX(), sz = w.getSpawnPos().getZ();
		int bestX = sx, bestZ = sz, bestY = Integer.MIN_VALUE;
		for (int dx = -80; dx <= 80; dx += 4) {
			for (int dz = -80; dz <= 80; dz += 4) {
				int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, sx + dx, sz + dz);
				if (y > bestY) {
					bestY = y;
					bestX = sx + dx;
					bestZ = sz + dz;
				}
			}
		}
		// Far view: 40 blocks south-west of the hill top, above it, looking at it.
		double fx = bestX - 34, fz = bestZ - 34;
		double fy = Math.max(bestY, w.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) fx, (int) fz)) + 18;
		farView = view(fx, fy, fz, bestX, bestY, bestZ);
		// Near view: on the slope, a few blocks above the ground.
		int nx = bestX - 14, nz = bestZ - 14;
		double ny = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, nx, nz) + 2.6;
		nearView = view(nx, ny, nz, bestX, Math.min(bestY, ny + 6), bestZ);
		// Mob spot: dry, fairly flat ground near the near view.
		int mx = nx, mz = nz;
		search:
		for (int r = 0; r <= 24; r += 2) {
			for (int dx = -r; dx <= r; dx += 2) {
				for (int dz = -r; dz <= r; dz += 2) {
					int x = nx + dx, z = nz + dz;
					int top = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
					BlockPos ground = new BlockPos(x, top - 1, z);
					if (!w.getFluidState(ground).isEmpty() || !w.getFluidState(ground.up()).isEmpty()) continue;
					int flat = 0;
					for (int ox = -2; ox <= 2; ox += 2)
						for (int oz = -2; oz <= 2; oz += 2)
							if (Math.abs(w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x + ox, z + oz) - top) <= 1) flat++;
					if (flat >= 8) {
						mx = x;
						mz = z;
						break search;
					}
				}
			}
		}
		mobSpot = new double[]{mx + 0.5, w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, mx, mz), mz + 0.5};
	}

	private static double[] view(double x, double y, double z, double tx, double ty, double tz) {
		double dx = tx - x, dz = tz - z, dy = ty - y;
		float yaw = (float) (MathHelper.atan2(dz, dx) * 57.2957795 - 90.0);
		float pitch = (float) -(MathHelper.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 57.2957795);
		return new double[]{x + 0.5, y, z + 0.5, yaw, MathHelper.clamp(pitch, -10f, 60f)};
	}

	private static void finish(MinecraftClient client) {
		phase = -1;
		try {
			File dir = new File(client.runDirectory, "selftest");
			dir.mkdirs();
			Files.write(new File(dir, "client_results.txt").toPath(), RESULTS, StandardCharsets.UTF_8);
		} catch (Exception e) {
			RealisticOverhaul.LOGGER.error("write results", e);
		}
		RealisticOverhaul.LOGGER.info("[SELFTEST] CLIENT DONE");
		client.scheduleStop();
	}
}
