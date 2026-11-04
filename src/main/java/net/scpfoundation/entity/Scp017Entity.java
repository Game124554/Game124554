package net.scpfoundation.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** SCP-017 "Shadow Person": a living shadow. Fast and deadly in darkness; bright light burns it away. */
public class Scp017Entity extends HostileEntity {
    public Scp017Entity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 30;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.36)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override
    public void initGoals() {
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, false, false,
                living -> living.getWorld().getLightLevel(living.getBlockPos()) <= 9));
    }

    @Override
    public void mobTick() {
        super.mobTick();
        if (this.age % 20 != 0) return;
        int light = getWorld().getLightLevel(getBlockPos().up());
        if (light > 10) {
            damage(getDamageSources().magic(), 4.0f);
            ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getBodyY(0.5), getZ(), 8, 0.3, 0.6, 0.3, 0.01);
        } else if (getHealth() < getMaxHealth()) {
            heal(2.0f);
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0), this);
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 100, 0), this);
        }
        return hit;
    }

    @Override
    public SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_PHANTOM_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PHANTOM_DEATH;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }
}
