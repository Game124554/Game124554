package net.scpfoundation.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * SCP-682. Enormous, hostile to all life, regenerates constantly and adapts: every hit of the same damage type
 * does less than the last. The first time it would die, it regenerates completely.
 */
public class Scp682Entity extends HostileEntity {
    private final Map<String, Integer> adaptations = new HashMap<>();
    private boolean revived;

    public Scp682Entity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 250;
        setStepHeight(1.5f);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 600.0)
                .add(EntityAttributes.GENERIC_ARMOR, 16.0)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 8.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 28.0)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 2.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.6));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, true));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, AnimalEntity.class, true));
    }

    @Override
    public void mobTick() {
        super.mobTick();
        if (this.age % 20 == 0 && getHealth() < getMaxHealth()) heal(2.0f);
        if (getTarget() != null && this.horizontalCollision) ScpUtil.smashAhead(this, 1, 3);
        if (this.age % 200 == 0 && getTarget() != null) playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 3.0f, 0.6f);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.damage(source, amount);
        if (getWorld().isClient) return false;
        String key = source.getName();
        int seen = adaptations.getOrDefault(key, 0);
        float reduced = amount * Math.max(0.1f, 1.0f - 0.12f * seen);
        adaptations.put(key, seen + 1);
        if (!revived && reduced >= getHealth() && !isInvulnerableTo(source)) {
            revived = true;
            setHealth(getMaxHealth());
            addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 3));
            addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 600, 1));
            playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.7f);
            return false;
        }
        return super.damage(source, reduced);
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 1), this);
        }
        return hit;
    }

    @Override
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_RAVAGER_AMBIENT;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_RAVAGER_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_RAVAGER_DEATH;
    }

    @Override
    public float getSoundPitch() {
        return 0.5f;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Revived", revived);
        NbtCompound ad = new NbtCompound();
        adaptations.forEach(ad::putInt);
        nbt.put("Adaptations", ad);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        revived = nbt.getBoolean("Revived");
        adaptations.clear();
        NbtCompound ad = nbt.getCompound("Adaptations");
        for (String k : ad.getKeys()) adaptations.put(k, ad.getInt(k));
    }
}
