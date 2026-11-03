package net.realisticoverhaul.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.realisticoverhaul.RealisticOverhaul;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * All settings. Saved to config/realisticoverhaul.json.
 * Edit in game with the O key, or edit the JSON file.
 */
public class ROConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ROConfig instance = new ROConfig();

	public static ROConfig get() {
		return instance;
	}

	// ---- Performance ----
	public PerformancePreset performancePreset = PerformancePreset.MEDIUM;
	public int physicsUpdatesPerTick = 32;
	public int maxCorpses = 16;
	public int particleDensity = 6;
	public int weatherAttemptsPerTick = 3;

	// ---- Block physics (collapse / falling blocks) ----
	public boolean blockPhysics = true;
	public boolean treesFall = true;
	/** A block with nothing under it stays up if at least this many solid blocks touch its sides. */
	public int sideSupportNeeded = 2;
	public boolean collapseHurtsEntities = true;

	// ---- Glass ----
	public boolean glassBreaking = true;
	public double glassBreakSpeed = 1.2;
	public float glassBreakFallDistance = 4.0f;
	public boolean sprintBreaksGlass = true;

	// ---- Camera ----
	public boolean cameraShake = true;
	public int cameraShakeStrength = 100;

	// ---- Corpses / ragdoll ----
	public boolean corpses = true;
	public int corpseSeconds = 60;
	public float ragdollImpulse = 0.45f;

	// ---- Weather ----
	public boolean puddles = true;
	public float puddleChance = 0.08f;
	public boolean snowAccumulation = true;
	public int maxSnowLayers = 4;

	// ---- Water & lava ----
	public boolean fastLava = true;
	public boolean steamParticles = true;

	// ---- Bedrock ----
	public boolean bedrockBreakable = true;
	public boolean removeBedrockWorldgen = true;

	// ---- Hell ----
	public boolean hellVisuals = true;
	/** Below this Y in the Overworld, caves start to look like hell. */
	public int hellStartY = 0;
	public boolean deathToNether = true;
	public boolean piglinForm = true;

	public void applyPreset() {
		performancePreset.applyTo(this);
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("realisticoverhaul.json");
	}

	public static void load() {
		Path p = path();
		if (Files.exists(p)) {
			try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
				ROConfig loaded = GSON.fromJson(r, ROConfig.class);
				if (loaded != null) {
					if (loaded.performancePreset == null) loaded.performancePreset = PerformancePreset.MEDIUM;
					instance = loaded;
				}
			} catch (Exception e) {
				RealisticOverhaul.LOGGER.error("Could not read config, using defaults", e);
			}
		}
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(path().getParent());
			try (Writer w = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
				GSON.toJson(instance, w);
			}
		} catch (Exception e) {
			RealisticOverhaul.LOGGER.error("Could not save config", e);
		}
	}
}
