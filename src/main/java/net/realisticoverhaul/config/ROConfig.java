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
import java.util.ArrayList;
import java.util.List;

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

	// ---- Smooth terrain (NoCubes style) ----
	/** Client: draw natural terrain as smooth slopes instead of cubes. */
	public boolean smoothTerrain = true;
	/** Server: players walk up 1-block steps without jumping. */
	public boolean stepAssist = true;
	/** Client: the camera glides up steps instead of snapping. */
	public boolean smoothStepCamera = true;
	/** Blocks drawn smooth. Block ids, or #tags such as "#minecraft:dirt". */
	public List<String> smoothBlocks = defaultSmoothBlocks();

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
	/** Client: corpse arms, legs and head flop and swing separately. */
	public boolean ragdollLimbs = true;

	// ---- Mobs ----
	public boolean blood = true;
	/** Mobs under 30% health move slower. */
	public boolean woundedMobs = true;
	/** Client: zombies hunch and limp, skeletons and endermen twitch, creepers breathe, spiders fidget. */
	public boolean creepyMobs = true;
	/** Client: zombies and skeletons have glowing eyes in the dark. */
	public boolean glowingEyes = true;

	// ---- Weather ----
	public boolean puddles = true;
	public float puddleChance = 0.08f;
	public boolean snowAccumulation = true;
	public int maxSnowLayers = 6;
	/** Snow builds dunes and piles up against walls. */
	public boolean snowDrifts = true;
	/** Client: snow layers are drawn as one smooth, wavy snow surface. */
	public boolean smoothSnow = true;
	/** Client: wind blows snow around in snowy places. */
	public boolean blowingSnow = true;

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

	public static List<String> defaultSmoothBlocks() {
		List<String> l = new ArrayList<>(List.of(
				"#minecraft:base_stone_overworld", "#minecraft:base_stone_nether", "#minecraft:dirt",
				"#minecraft:sand", "#minecraft:terracotta", "#minecraft:nylium",
				"#minecraft:coal_ores", "#minecraft:iron_ores", "#minecraft:copper_ores", "#minecraft:gold_ores",
				"#minecraft:redstone_ores", "#minecraft:emerald_ores", "#minecraft:lapis_ores", "#minecraft:diamond_ores",
				"minecraft:stone", "minecraft:granite", "minecraft:diorite", "minecraft:andesite", "minecraft:deepslate",
				"minecraft:tuff", "minecraft:calcite", "minecraft:dripstone_block", "minecraft:smooth_basalt",
				"minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:rooted_dirt",
				"minecraft:podzol", "minecraft:mycelium", "minecraft:mud", "minecraft:clay", "minecraft:moss_block",
				"minecraft:gravel", "minecraft:sand", "minecraft:red_sand", "minecraft:sandstone", "minecraft:red_sandstone",
				"minecraft:terracotta", "minecraft:snow_block", "minecraft:packed_ice", "minecraft:blue_ice",
				"minecraft:netherrack", "minecraft:soul_soil", "minecraft:basalt", "minecraft:blackstone",
				"minecraft:magma_block", "minecraft:crimson_nylium", "minecraft:warped_nylium", "minecraft:end_stone",
				"minecraft:nether_gold_ore", "minecraft:nether_quartz_ore"));
		return l;
	}

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
					if (loaded.smoothBlocks == null) loaded.smoothBlocks = defaultSmoothBlocks();
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
