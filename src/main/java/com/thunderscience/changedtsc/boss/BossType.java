package com.thunderscience.changedtsc.boss;

import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;

import java.util.Locale;
import java.util.Optional;

/**
 * The four Thunder Science bosses. Each boss is built on top of an entity from the Changed mod
 * (looked up by keyword at runtime, so it uses Changed's own model and texture), then boosted
 * with boss stats, a boss bar and special abilities. Every default here can be changed in the config.
 */
public enum BossType {
    SQUID_DOG("squid_dog",
            "auto:squid_dog|minecraft:drowned",
            "auto:tiger_shark|auto:shark|minecraft:drowned",
            300.0, 9.0, BossEvent.BossBarColor.WHITE),
    TIGER_SHARK("tiger_shark",
            "auto:tiger_shark|auto:shark|minecraft:drowned",
            "auto:shark|minecraft:drowned",
            250.0, 10.0, BossEvent.BossBarColor.BLUE),
    ELDER_ONE("elder_one",
            "auto:pure_white_latex_wolf|auto:white_latex_wolf|auto:white_latex|auto:behemoth|minecraft:wither_skeleton",
            "pool",
            420.0, 12.0, BossEvent.BossBarColor.PURPLE),
    WOLF_KING("wolf_king",
            "auto:dark_latex_wolf|auto:dark_latex|minecraft:wither_skeleton",
            "auto:dark_latex_wolf|auto:dark_latex|minecraft:wolf",
            500.0, 14.0, BossEvent.BossBarColor.RED);

    private final String id;
    private final String defaultEntity;
    private final String defaultMinions;
    private final double defaultHealth;
    private final double defaultDamage;
    private final BossEvent.BossBarColor color;

    BossType(String id, String defaultEntity, String defaultMinions, double defaultHealth, double defaultDamage,
             BossEvent.BossBarColor color) {
        this.id = id;
        this.defaultEntity = defaultEntity;
        this.defaultMinions = defaultMinions;
        this.defaultHealth = defaultHealth;
        this.defaultDamage = defaultDamage;
        this.color = color;
    }

    public String id() { return id; }
    public String defaultEntity() { return defaultEntity; }
    public String defaultMinions() { return defaultMinions; }
    public double defaultHealth() { return defaultHealth; }
    public double defaultDamage() { return defaultDamage; }
    public BossEvent.BossBarColor color() { return color; }

    public Component displayName() {
        return Component.translatable("boss.changed_tsc." + id);
    }

    public static Optional<BossType> byId(String id) {
        String lower = id.toLowerCase(Locale.ROOT);
        for (BossType t : values()) {
            if (t.id.equals(lower)) return Optional.of(t);
        }
        return Optional.empty();
    }
}
