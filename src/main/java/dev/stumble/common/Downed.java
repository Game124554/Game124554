package dev.stumble.common;

import dev.stumble.server.ServerRagdolls;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;

/** Is this living entity currently a ragdoll, and so unable to move or act by itself? */
public final class Downed {
    /** Set by the client half: true for the local player while they are a ragdoll. */
    public static volatile Predicate<LivingEntity> client = e -> false;

    private Downed() {
    }

    public static boolean isDown(LivingEntity entity) {
        return entity.level().isClientSide ? client.test(entity) : ServerRagdolls.isDown(entity);
    }
}
