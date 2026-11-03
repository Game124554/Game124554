package net.scpfoundation.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** SCP-999: a friendly blob that follows people around and makes them feel better. Right-click for a hug. */
public class Scp999Entity extends PathAwareEntity {
    public Scp999Entity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    @Override
    public void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    @Override
    public void mobTick() {
        super.mobTick();
        if (this.age % 20 == 0) {
            PlayerEntity p = getWorld().getClosestPlayer(this, 14.0);
            if (p != null && !p.isSpectator() && squaredDistanceTo(p) > 9.0) {
                getNavigation().startMovingTo(p, 1.0);
            }
        }
        if (this.age % 40 == 0) {
            for (PlayerEntity p : getWorld().getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(6.0), pl -> !pl.isSpectator())) {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0, true, true, true));
            }
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!getWorld().isClient) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 600, 0));
            player.removeStatusEffect(StatusEffects.WEAKNESS);
            player.removeStatusEffect(StatusEffects.SLOWNESS);
            ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.HEART, getX(), getY() + 1.0, getZ(), 6, 0.4, 0.3, 0.4, 0.0);
            playSound(SoundEvents.ENTITY_SLIME_SQUISH, 1.0f, 1.5f);
        }
        return ActionResult.success(getWorld().isClient);
    }

    @Override
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_SLIME_SQUISH_SMALL;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_SLIME_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_SLIME_DEATH;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }
}
