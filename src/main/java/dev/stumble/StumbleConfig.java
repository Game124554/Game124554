package dev.stumble;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Settings, stored as config/stumble.json. Edit the file and restart the game.
 * On a multiplayer server, the server's copy decides pushing and mob knockdowns;
 * each player's own copy decides what they see and whether they ragdoll themselves.
 */
public final class StumbleConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static StumbleConfig instance = new StumbleConfig();

    /** Master switch for everything this mod draws. */
    public boolean enabled = true;
    /** Ragdoll players (you included, visible in third person). */
    public boolean players = true;
    /** Ragdoll zombies, husks, drowned, skeletons, strays and wither skeletons. */
    public boolean mobs = true;
    /** How many dead bodies may exist at once; the oldest is removed first. */
    public int maxRagdolls = 24;
    /** How long a dead body stays before fading out. */
    public int lifetimeSeconds = 45;
    /** Multiplier on how hard killing blows and explosions throw bodies. */
    public double force = 1.0;
    /** Thud when a body hits the ground. */
    public boolean impactSounds = true;

    /** The push key works (server side). */
    public boolean push = true;
    /** Multiplier on how hard a push is. Sprinting pushes harder. */
    public double pushStrength = 1.0;
    /** Pushing other players knocks them about too (server side). */
    public boolean pushPlayers = true;
    /** Walking or running into a mob bumps it: walking makes it stumble, running can knock it down (server side). */
    public boolean bump = true;
    /** Multiplier on how hard a bump is. */
    public double bumpStrength = 1.0;
    /** Living mobs can be knocked down: by pushes, big knockback and long falls (server side). */
    public boolean mobKnockdowns = true;
    /** Falling further than this many blocks turns you (and mobs) into a ragdoll mid-air. */
    public double fallRagdollHeight = 4.0;
    /** Falling ragdolls on or off. */
    public boolean fallRagdoll = true;
    /** Being thrown hard (explosions, strong knockback) ragdolls you and mobs. */
    public boolean knockbackRagdoll = true;
    /** How sudden a change of speed (blocks per tick) counts as being thrown. */
    public double knockbackRagdollSpeed = 0.65;
    /** Switch to third person while you are a ragdoll, and back when you are up. */
    public boolean ragdollCamera = true;

    public static StumbleConfig get() {
        return instance;
    }

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("stumble.json");
        try {
            if (Files.exists(file)) {
                try (Reader in = Files.newBufferedReader(file)) {
                    StumbleConfig read = GSON.fromJson(in, StumbleConfig.class);
                    if (read != null) {
                        instance = read;
                    }
                }
            }
            StumbleConfig c = instance;
            c.maxRagdolls = Math.max(1, Math.min(128, c.maxRagdolls));
            c.lifetimeSeconds = Math.max(2, Math.min(600, c.lifetimeSeconds));
            c.force = clamp(c.force, 0, 4);
            c.pushStrength = clamp(c.pushStrength, 0, 4);
            c.bumpStrength = clamp(c.bumpStrength, 0, 4);
            c.fallRagdollHeight = clamp(c.fallRagdollHeight, 1.5, 64);
            c.knockbackRagdollSpeed = clamp(c.knockbackRagdollSpeed, 0.2, 5);
            try (Writer out = Files.newBufferedWriter(file)) {
                GSON.toJson(instance, out);
            }
        } catch (Exception e) {
            // a broken config file should never stop the game from starting
            System.err.println("[Stumble] Could not read or write " + file + ": " + e);
            instance = new StumbleConfig();
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Double.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v));
    }
}
