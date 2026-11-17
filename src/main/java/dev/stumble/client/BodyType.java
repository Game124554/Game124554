package dev.stumble.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/** The humanoid texture layouts the ragdoll model knows how to draw. */
public enum BodyType {
    PLAYER(64, 64, true, false, false),
    PLAYER_SLIM(64, 64, true, true, false),
    ZOMBIE(64, 64, false, false, false),
    SKELETON(64, 32, false, false, true);

    public final int texWidth, texHeight;
    /** Has the second skin layer and separate left-limb textures. */
    public final boolean player;
    public final boolean slimArms;
    public final boolean thinLimbs;

    BodyType(int texWidth, int texHeight, boolean player, boolean slimArms, boolean thinLimbs) {
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.player = player;
        this.slimArms = slimArms;
        this.thinLimbs = thinLimbs;
    }

    /** The body type for this entity, or null if it is not one the mod can ragdoll. */
    public static BodyType of(LivingEntity entity) {
        if (entity instanceof AbstractClientPlayer player) {
            return "slim".equals(player.getModelName()) ? PLAYER_SLIM : PLAYER;
        }
        EntityType<?> type = entity.getType();
        if (type == EntityType.ZOMBIE || type == EntityType.HUSK || type == EntityType.DROWNED) {
            return ZOMBIE;
        }
        if (type == EntityType.SKELETON || type == EntityType.STRAY || type == EntityType.WITHER_SKELETON) {
            return SKELETON;
        }
        return null;
    }

    /** Vanilla renders these entities at different sizes from the same 32 pixel tall model. */
    public static float scaleOf(LivingEntity entity) {
        return dev.stumble.common.Humanoids.scale(entity.getType());
    }
}
