package com.mandelahorror;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

/**
 * Settings file: .minecraft/config/mandelahorror.properties
 * (created on first launch with every option and its explanation; edit it and restart).
 */
public final class HorrorConfig {

    /** One option. Use get() to read it. */
    public static final class Value<T> {
        private final String key;
        private final String comment;
        private final T defaultValue;
        private final Function<String, T> parser;
        private final Function<T, String> writer;
        private T value;

        private Value(String key, T def, String comment, Function<String, T> parser, Function<T, String> writer) {
            this.key = key;
            this.defaultValue = def;
            this.value = def;
            this.comment = comment;
            this.parser = parser;
            this.writer = writer;
            ALL.add(this);
        }

        public T get() {
            return this.value;
        }

        private void read(Properties p) {
            String raw = p.getProperty(this.key);
            if (raw == null) return;
            try {
                T parsed = this.parser.apply(raw.trim());
                this.value = parsed != null ? parsed : this.defaultValue;
            } catch (Exception e) {
                MandelaHorror.LOGGER.warn("Bad value for {} in mandelahorror.properties: {}", this.key, raw);
                this.value = this.defaultValue;
            }
        }
    }

    private static final List<Value<?>> ALL = new ArrayList<>();

    // ---------- intro ----------
    public static final Value<Boolean> INTRO_EVERY_JOIN = bool("intro.everyJoin", false,
            "Show the emergency broadcast screen every time you join (false = only the first time in each world).");
    public static final Value<Integer> GRACE_SECONDS = integer("intro.graceSeconds", 120, 0, 3600,
            "Seconds after joining before any haunting can start.");

    // ---------- fake players ----------
    public static final Value<Boolean> FAKE_JOINS = bool("fakePlayers.enabled", true,
            "Fake players 'join the game', talk in chat and walk around as mimics.");
    public static final Value<Integer> FAKE_JOIN_MIN_SECONDS = integer("fakePlayers.minSecondsBetween", 360, 30, 36000, "");
    public static final Value<Integer> FAKE_JOIN_MAX_SECONDS = integer("fakePlayers.maxSecondsBetween", 900, 30, 36000, "");
    public static final Value<Double> OWN_NAME_CHANCE = decimal("fakePlayers.ownNameChance", 0.35, 0.0, 1.0,
            "Chance (0-1) that the fake player uses YOUR name and YOUR skin.");
    public static final Value<Boolean> FAKE_TAB_LIST = bool("fakePlayers.showInTabList", true,
            "Also add the fake player to the TAB player list.");
    public static final Value<List<String>> FAKE_NAMES = list("fakePlayers.names",
            List.of("Mark_H", "adam2009", "SarahPlays", "jonny_k", "Thomas", "Gabriel", "caesar_x", "evelyn",
                    "dan_miner", "henry", "lucy_b", "Isaac", "Martha", "peter", "ruth"),
            "Names used by fake players, separated by commas (max 16 letters each).");

    // ---------- the Alternate ----------
    public static final Value<Boolean> ALTERNATE_SPAWNS = bool("alternate.enabled", true,
            "The tall Alternate appears near players.");
    public static final Value<Boolean> ALTERNATE_NIGHT_ONLY = bool("alternate.nightOnly", true,
            "Only at night (or underground / in dimensions without a day cycle).");
    public static final Value<Integer> ALTERNATE_MIN_SECONDS = integer("alternate.minSecondsBetween", 420, 30, 36000, "");
    public static final Value<Integer> ALTERNATE_MAX_SECONDS = integer("alternate.maxSecondsBetween", 1200, 30, 36000, "");
    public static final Value<Boolean> ALTERNATE_VOICE = bool("alternate.voice", true,
            "The Alternate plays its voice clip when it appears, when you look at it, and before the self-stab.");

    // ---------- M.A.D. ----------
    public static final Value<Boolean> MAD_BOSS_BAR = bool("mad.bossBar", true,
            "Show the M.A.D. boss bar when an Alternate is close.");
    public static final Value<Double> INFLUENCE_SPEED = decimal("mad.influenceSpeed", 1.0, 0.1, 10.0,
            "How fast the Alternate's influence grows (1.0 = default). Looking at it makes it grow much faster.");
    public static final Value<Boolean> NIGHT_BROADCASTS = bool("mad.nightBroadcasts", true,
            "M.A.D. advisory message every nightfall.");

    // ---------- self-stab ----------
    public static final Value<Boolean> SELF_STAB = bool("selfStab.enabled", true,
            "When the M.A.D. bar fills, the Alternate makes you stab yourself with a sword, pickaxe or axe from your hotbar.");
    public static final Value<Double> SELF_STAB_MULTIPLIER = decimal("selfStab.damageMultiplier", 1.25, 0.0, 10.0,
            "Damage = weapon attack damage x this value.");
    public static final Value<Boolean> SELF_STAB_CAN_KILL = bool("selfStab.canKill", true,
            "If false, the stab always leaves you with at least half a heart.");

    // ---------- client ----------
    public static final Value<Boolean> TEXT_TO_SPEECH = bool("client.textToSpeech", true,
            "The Alternate and the broadcast speak out loud with your system's text-to-speech voice.");
    public static final Value<Boolean> STATIC_OVERLAY = bool("client.staticOverlay", true,
            "VHS static on screen when an Alternate is near.");
    public static final Value<Boolean> ALLOW_SKIP_INTRO = bool("client.allowSkipIntro", true,
            "Allow ESC to skip the emergency broadcast intro.");
    public static final Value<Boolean> SKIN_GLITCH = bool("client.skinGlitch", true,
            "Player skins can randomly swap during haunting events.");

    // ---------- VHS camera filter ----------
    public static final Value<Boolean> VHS_FILTER = bool("vhs.enabled", true,
            "Old VHS camera look: scanlines, noise, tracking lines, PLAY label and date stamp. Toggle in game with the V key.");
    public static final Value<Boolean> VHS_BLACK_AND_WHITE = bool("vhs.blackAndWhite", true,
            "true = black and white, false = faded VHS colors.");
    public static final Value<Integer> VHS_YEAR = integer("vhs.year", 1998, 0, 9999,
            "Year shown in the date stamp. 0 = the real current year.");
    public static final Value<Boolean> VHS_REAL_TIME = bool("vhs.realTime", false,
            "Clock in the date stamp: false = Minecraft world time, true = your real clock.");

    // ------------------------------------------------------------------ load / save

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("mandelahorror.properties");
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                p.load(r);
            } catch (IOException e) {
                MandelaHorror.LOGGER.warn("Could not read {}", file, e);
            }
        }
        for (Value<?> v : ALL) v.read(p);
        save(file);
    }

    private static void save(Path file) {
        StringBuilder sb = new StringBuilder("# Mandela Catalogue Horror settings. Edit, save, and restart the game.\n");
        String lastGroup = "";
        for (Value<?> v : ALL) {
            String group = v.key.substring(0, v.key.indexOf('.'));
            if (!group.equals(lastGroup)) {
                sb.append("\n# ---- ").append(group).append(" ----\n");
                lastGroup = group;
            }
            if (!v.comment.isEmpty()) sb.append("# ").append(v.comment).append('\n');
            sb.append(v.key).append(" = ").append(write(v)).append('\n');
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            MandelaHorror.LOGGER.warn("Could not write {}", file, e);
        }
    }

    private static <T> String write(Value<T> v) {
        return v.writer.apply(v.value);
    }

    // ------------------------------------------------------------------ builders

    private static Value<Boolean> bool(String key, boolean def, String comment) {
        return new Value<>(key, def, comment, s -> {
            if (s.equalsIgnoreCase("true")) return true;
            if (s.equalsIgnoreCase("false")) return false;
            return null;
        }, String::valueOf);
    }

    private static Value<Integer> integer(String key, int def, int min, int max, String comment) {
        String c = (comment.isEmpty() ? "" : comment + " ") + "(" + min + " to " + max + ")";
        return new Value<>(key, def, c, s -> Math.max(min, Math.min(max, Integer.parseInt(s))), String::valueOf);
    }

    private static Value<Double> decimal(String key, double def, double min, double max, String comment) {
        String c = comment + " (" + min + " to " + max + ")";
        return new Value<>(key, def, c, s -> Math.max(min, Math.min(max, Double.parseDouble(s))), String::valueOf);
    }

    private static Value<List<String>> list(String key, List<String> def, String comment) {
        return new Value<>(key, def, comment, s -> {
            List<String> out = new ArrayList<>();
            for (String part : Arrays.asList(s.split(","))) {
                String n = part.trim();
                if (!n.isEmpty() && n.length() <= 16) out.add(n);
            }
            return out.isEmpty() ? null : List.copyOf(out);
        }, l -> String.join(", ", l));
    }

    private HorrorConfig() {}
}
