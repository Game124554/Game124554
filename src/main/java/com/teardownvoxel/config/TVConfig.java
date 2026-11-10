package com.teardownvoxel.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.teardownvoxel.TeardownVoxel;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple JSON config stored at config/teardown_voxel.json.
 * Every field has a sensible default; unknown/missing fields fall back to the defaults.
 */
public class TVConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static TVConfig instance = new TVConfig();

    // ---- Micro-debris ----
    /** Lifetime of a VoxelDebrisEntity in ticks (300 = 15 seconds). */
    public int debrisLifetimeTicks = 300;
    /** Smallest / largest debris cube size relative to a full block. */
    public float debrisMinScale = 0.25f;
    public float debrisMaxScale = 0.5f;
    /** Hard cap on live debris entities per dimension (protects server TPS). */
    public int maxActiveDebris = 800;
    /** Max debris spawned by a single explosion. */
    public int maxDebrisPerExplosion = 160;
    /** Debris cubes spawned per shattered block (min..max). */
    public int debrisPerBlockMin = 2;
    public int debrisPerBlockMax = 4;
    /** Debris restitution (0 = no bounce, 1 = perfect bounce) and ground friction. */
    public float debrisBounciness = 0.35f;
    public float debrisGroundFriction = 0.72f;
    /** Spawn debris when vanilla explosions (TNT, creepers, fireballs...) destroy blocks. */
    public boolean vanillaExplosionDebris = true;
    /** Spawn a few chips when a player mines a block with a pickaxe/axe/shovel/hoe. */
    public boolean toolBreakDebris = true;

    // ---- Progressive damage ----
    /** Block "hit points" = max(0.3, hardness) * this value. */
    public float hitPointsPerHardness = 6.0f;
    /** Damage per block of projectile speed (arrows, tridents...). */
    public float projectileDamageMultiplier = 0.6f;
    /** Projectiles can chip and shatter blocks. */
    public boolean projectileBlockDamage = true;
    /** Damage per tick dealt by the blowtorch to metal blocks / doors. */
    public float blowtorchMetalDamagePerTick = 1.6f;
    /** Damage per tick dealt by the blowtorch to anything else. */
    public float blowtorchOtherDamagePerTick = 0.15f;

    // ---- Structural integrity ----
    public boolean structuralCollapse = true;
    /** Max blocks explored per connectivity search before a structure is assumed supported. */
    public int maxStructureSearch = 256;
    /** BFS node budget per server tick (searches are spread over ticks so they never stall the server). */
    public int bfsNodesPerTick = 2048;
    /** Run collapse checks when a player breaks a block by hand. */
    public boolean collapseOnPlayerBreak = true;
    /** Chance a collapsing block drops its item when it shatters on impact. */
    public float shatterDropChance = 0.6f;
    /** Chance a collapsing block survives the impact and is placed back as rubble. */
    public float rubbleChance = 0.2f;

    // ---- Tools / explosives ----
    /** Sledgehammer instantly smashes blocks up to this hardness (harder blocks take progressive damage). */
    public float sledgehammerMaxHardness = 3.0f;
    public int sledgehammerCooldownTicks = 18;
    public boolean sledgehammerDropsItems = false;
    public float pipeBombPower = 3.2f;
    public float nitroPower = 5.0f;
    public int pipeBombFuseTicks = 50;
    /** Lingering smoke duration after mod explosives, in ticks. */
    public int smokeDurationTicks = 240;
    /** Explosives char surrounding blocks (stone -> cobble/blackstone, wood -> charred). */
    public boolean charring = true;

    // ---- Client feedback ----
    public float screenShakeMultiplier = 1.0f;

    public static TVConfig get() {
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(TeardownVoxel.MOD_ID + ".json");
        try {
            if (Files.exists(path)) {
                try (Reader r = Files.newBufferedReader(path)) {
                    TVConfig loaded = GSON.fromJson(r, TVConfig.class);
                    if (loaded != null) instance = loaded;
                }
            }
            instance.sanitize();
            try (Writer w = Files.newBufferedWriter(path)) {
                GSON.toJson(instance, w);
            }
        } catch (Exception e) {
            TeardownVoxel.LOGGER.error("Failed to load config {}, using defaults", path, e);
            instance = new TVConfig();
        }
    }

    private void sanitize() {
        debrisMinScale = Math.max(0.05f, Math.min(debrisMinScale, 1f));
        debrisMaxScale = Math.max(debrisMinScale, Math.min(debrisMaxScale, 1f));
        debrisPerBlockMin = Math.max(0, debrisPerBlockMin);
        debrisPerBlockMax = Math.max(debrisPerBlockMin, debrisPerBlockMax);
        maxStructureSearch = Math.max(8, Math.min(maxStructureSearch, 32768));
        bfsNodesPerTick = Math.max(64, bfsNodesPerTick);
        hitPointsPerHardness = Math.max(0.1f, hitPointsPerHardness);
        debrisLifetimeTicks = Math.max(20, debrisLifetimeTicks);
    }
}
