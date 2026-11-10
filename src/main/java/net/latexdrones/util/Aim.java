package net.latexdrones.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Small aiming helpers for beam / cone weapons. */
public final class Aim {
    private Aim() {
    }

    /**
     * The closest living entity the shooter is looking at.
     *
     * @param range  maximum distance in blocks
     * @param minDot how tight the aim must be (1 = dead centre, 0.97 is roughly a 14 degree cone)
     */
    public static LivingEntity pick(LivingEntity shooter, double range, double minDot) {
        Vec3 eye = shooter.getEyePosition();
        Vec3 look = shooter.getLookAngle();
        LivingEntity best = null;
        double bestDist = range * range;
        for (LivingEntity e : shooter.level().getEntitiesOfClass(LivingEntity.class, shooter.getBoundingBox().inflate(range),
                x -> x != shooter && x.isAlive() && !x.isSpectator())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d2 = to.lengthSqr();
            if (d2 > bestDist || d2 < 1.0E-4D) {
                continue;
            }
            if (to.normalize().dot(look) < minDot) {
                continue;
            }
            if (!shooter.hasLineOfSight(e)) {
                continue;
            }
            best = e;
            bestDist = d2;
        }
        return best;
    }
}
