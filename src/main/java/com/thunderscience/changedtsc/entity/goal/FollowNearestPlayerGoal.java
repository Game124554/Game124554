package com.thunderscience.changedtsc.entity.goal;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/** Lets a friendly Puro tag along with the closest player. */
public class FollowNearestPlayerGoal extends Goal {
    private final PathfinderMob mob;
    private final double speed;
    private final float startDistance;
    private final float stopDistance;
    private Player player;
    private int recalc;

    public FollowNearestPlayerGoal(PathfinderMob mob, double speed, float startDistance, float stopDistance) {
        this.mob = mob;
        this.speed = speed;
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getTarget() != null) return false;
        Player nearest = this.mob.level().getNearestPlayer(this.mob, 16.0D);
        if (nearest == null || nearest.isSpectator()) return false;
        if (this.mob.distanceToSqr(nearest) < this.startDistance * this.startDistance) return false;
        this.player = nearest;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.player != null && this.player.isAlive() && this.mob.getTarget() == null
                && this.mob.distanceToSqr(this.player) > this.stopDistance * this.stopDistance
                && this.mob.distanceToSqr(this.player) < 24.0D * 24.0D;
    }

    @Override
    public void start() {
        this.recalc = 0;
    }

    @Override
    public void stop() {
        this.player = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.player == null) return;
        this.mob.getLookControl().setLookAt(this.player, 10.0F, (float) this.mob.getMaxHeadXRot());
        if (--this.recalc <= 0) {
            this.recalc = 10;
            this.mob.getNavigation().moveTo(this.player, this.speed);
        }
    }
}
