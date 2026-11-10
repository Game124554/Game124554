package com.teardownvoxel.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

/**
 * Tiny rigid-body integrator shared by debris, falling structure chunks and pipe bombs:
 * gravity, air drag, swept world collision (via Entity#move), restitution and ground friction.
 */
public final class SimplePhysics {
    private SimplePhysics() {
    }

    /**
     * Integrates one tick.
     *
     * @return the downward speed the body had when it hit the ground this tick (0 if it did not land)
     */
    public static double step(Entity e, double gravity, double drag, double bounciness, double groundFriction) {
        Vec3d v = e.getVelocity();
        if (!e.hasNoGravity()) {
            v = v.add(0.0, -gravity, 0.0);
        }
        Vec3d before = e.getPos();
        e.move(MovementType.SELF, v);
        Vec3d moved = e.getPos().subtract(before);

        double vx = v.x;
        double vy = v.y;
        double vz = v.z;
        double impact = 0.0;

        if (e.horizontalCollision) {
            if (Math.abs(moved.x - v.x) > 1.0E-4) vx = -v.x * bounciness;
            if (Math.abs(moved.z - v.z) > 1.0E-4) vz = -v.z * bounciness;
        }
        if (e.verticalCollision) {
            if (v.y < 0) {
                impact = -v.y;
                vy = impact > 0.12 ? impact * bounciness : 0.0;
            } else {
                vy = -v.y * bounciness * 0.5;
            }
        }

        if (e.isOnGround()) {
            vx *= groundFriction;
            vz *= groundFriction;
        }
        vx *= drag;
        vy *= drag;
        vz *= drag;

        if (Math.abs(vx) < 1.0E-3) vx = 0;
        if (Math.abs(vz) < 1.0E-3) vz = 0;
        e.setVelocity(vx, vy, vz);
        return impact;
    }
}
