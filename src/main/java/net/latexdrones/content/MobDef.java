package net.latexdrones.content;

import net.latexdrones.LatexDrones;
import net.latexdrones.entity.LatexDroneEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/** Static description of one of the mod's mobs. One {@link LatexDroneEntity} class serves all of them. */
public final class MobDef {
    // ---- model extras (bit flags) - must match tools/content.py
    public static final int EARS_POINTY = 1;
    public static final int EARS_LONG = 1 << 1;
    public static final int EARS_ROUND = 1 << 2;
    public static final int HORNS = 1 << 3;
    public static final int SNOUT = 1 << 4;
    public static final int BEAK = 1 << 5;
    public static final int VISOR = 1 << 6;
    public static final int ANTENNA = 1 << 7;
    public static final int FIN = 1 << 8;
    public static final int TAIL_BUSHY = 1 << 9;
    public static final int TAIL_THIN = 1 << 10;
    public static final int TAIL_SHORT = 1 << 11;
    public static final int TAIL_THICK = 1 << 12;
    public static final int WINGS = 1 << 13;

    // ---- ranged attack kinds
    public static final int DART_NONE = 0;
    public static final int DART_LATEX = 1;
    public static final int DART_TRANQ = 2;
    public static final int DART_GOO = 3;
    public static final int DART_HYPNO = 4;

    public final String name;
    public final Archetype archetype;
    public final float width;
    public final float height;
    public final float scale;
    public final double health;
    public final double damage;
    public final double speed;
    public final double armor;
    /** Infection added to a player per melee hit (0..1000, 1000 = 100%). */
    public final int infect;
    public final int flags;
    public final boolean ranged;
    public final boolean leaper;
    public final boolean scientist;
    public final boolean fireImmune;
    /** Hits (or beams) from this mob hypnotise the target. */
    public final boolean hypno;
    /** Item id (path only) held in the main hand, or "". */
    public final String heldItem;
    public final int spawnWeight;
    public final int maxGroup;
    public final int dartKind;
    public final int color;
    public final int glowColor;

    /** Humanoid-layout texture (used by humanoid mobs and for the ears/tail of transformed players). */
    public final ResourceLocation formTexture;
    /** Texture the mob itself renders with. */
    public final ResourceLocation mobTexture;
    /** Player-skin-layout texture used when a player has been turned into this form. */
    public final ResourceLocation playerTexture;

    /** Filled in by ModEntities when the entity type is registered. */
    public EntityType<LatexDroneEntity> type;

    public MobDef(String name, Archetype archetype, float width, float height, float scale, double health, double damage,
                  double speed, double armor, int infect, int flags, boolean ranged, boolean leaper, boolean scientist,
                  boolean fireImmune, boolean hypno, String heldItem, int spawnWeight, int maxGroup, int dartKind,
                  int color, int glowColor) {
        this.name = name;
        this.archetype = archetype;
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.armor = armor;
        this.infect = infect;
        this.flags = flags;
        this.ranged = ranged;
        this.leaper = leaper;
        this.scientist = scientist;
        this.fireImmune = fireImmune;
        this.hypno = hypno;
        this.heldItem = heldItem;
        this.spawnWeight = spawnWeight;
        this.maxGroup = maxGroup;
        this.dartKind = dartKind;
        this.color = color;
        this.glowColor = glowColor;
        this.formTexture = new ResourceLocation(LatexDrones.MOD_ID, "textures/entity/form/" + name + ".png");
        this.playerTexture = new ResourceLocation(LatexDrones.MOD_ID, "textures/entity/player/" + name + ".png");
        this.mobTexture = archetype == Archetype.HUMANOID
                ? this.formTexture
                : new ResourceLocation(LatexDrones.MOD_ID, "textures/entity/mob/" + name + ".png");
    }

    public boolean has(int flag) {
        return (this.flags & flag) != 0;
    }

    /** Tough mobs: rarer natural spawns, better loot, more XP. */
    public boolean isBig() {
        return this.health >= 40.0D;
    }
}
