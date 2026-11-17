package dev.stumble.common;

import net.minecraft.world.entity.EntityType;

/** The entity types this mod can turn into ragdolls, and their sizes. Safe on a dedicated server. */
public final class Humanoids {
    private Humanoids() {
    }

    public static boolean supported(EntityType<?> type) {
        return type == EntityType.PLAYER || type == EntityType.ZOMBIE || type == EntityType.HUSK
                || type == EntityType.DROWNED || type == EntityType.SKELETON || type == EntityType.STRAY
                || type == EntityType.WITHER_SKELETON;
    }

    /** Vanilla renders these at different sizes from the same 32 pixel tall model. */
    public static float scale(EntityType<?> type) {
        if (type == EntityType.PLAYER) {
            return 0.9375f;
        }
        if (type == EntityType.HUSK) {
            return 1.0625f;
        }
        if (type == EntityType.WITHER_SKELETON) {
            return 1.2f;
        }
        return 1.0f;
    }

    public static boolean thinLimbs(EntityType<?> type) {
        return type == EntityType.SKELETON || type == EntityType.STRAY || type == EntityType.WITHER_SKELETON;
    }

    /** Zombies walk with their arms out. */
    public static boolean armsForward(EntityType<?> type) {
        return type == EntityType.ZOMBIE || type == EntityType.HUSK || type == EntityType.DROWNED;
    }
}
