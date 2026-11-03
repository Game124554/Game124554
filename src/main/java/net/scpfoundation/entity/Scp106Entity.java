package net.scpfoundation.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.scpfoundation.world.PocketDimension;

/**
 * SCP-106. While hunting it drifts straight through solid matter toward its prey, leaving corrosion behind.
 * Its grip drags players into the pocket dimension.
 */
public class Scp106Entity extends HostileEntity {
    private int attackCooldown;

    public Scp106Entity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 60;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 300.0)
                .add(EntityAttributes.GENERIC_ARMOR, 10.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public void initGoals() {
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.6));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, false, false, null));
    }

    @Override
    public void tick() {
        if (!getWorld().isClient) {
            LivingEntity target = getTarget();
            boolean hunting = target != null && target.isAlive() && target.getWorld() == getWorld()
                    && squaredDistanceTo(target) < 48.0 * 48.0
                    && !(target instanceof PlayerEntity p && !ScpUtil.isTargetablePlayer(p));
            this.noClip = hunting;
            setNoGravity(hunting);
            if (hunting) {
                getNavigation().stop();
                Vec3d to = new Vec3d(target.getX() - getX(), target.getY() - getY(), target.getZ() - getZ());
                double len = to.length();
                setVelocity(len > 0.05 ? to.multiply(0.14 / len) : Vec3d.ZERO);
                getLookControl().lookAt(target, 30.0f, 30.0f);
                setYaw((float) (Math.toDegrees(Math.atan2(to.z, to.x))) - 90.0f);
                this.bodyYaw = getYaw();
                if (len < 1.8 && attackCooldown <= 0) {
                    tryAttack(target);
                    attackCooldown = 30;
                }
                if (this.age % 3 == 0) {
                    ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.1, getZ(), 2, 0.3, 0.05, 0.3, 0.0);
                }
                if (this.age % 50 == 0) playSound(SoundEvents.BLOCK_SCULK_SPREAD, 1.5f, 0.5f);
            }
            if (attackCooldown > 0) attackCooldown--;
        }
        super.tick();
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 0), this);
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 2), this);
            if (target instanceof ServerPlayerEntity player && this.random.nextFloat() < 0.4f) {
                PocketDimension.send(player);
                setTarget(null);
            }
        }
        return hit;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isOf(DamageTypes.IN_WALL) || source.isOf(DamageTypes.FALL)) return false;
        if (source.isIn(DamageTypeTags.IS_FIRE)) amount *= 3.0f; // the only thing it seems to fear
        return super.damage(source, amount);
    }

    @Override
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WARDEN_AMBIENT;
    }

    @Override
    public float getSoundPitch() {
        return 0.6f;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_WARDEN_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WARDEN_DEATH;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }
}
