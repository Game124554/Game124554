package net.scpfoundation.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * SCP-650 "Startling Statue": a black statue that never moves while watched - but every so often, when you turn
 * around, it is standing right behind you. It never attacks.
 */
public class Scp650Entity extends PathAwareEntity {
    private int jumpCooldown = 200;

    public Scp650Entity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 200.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public void mobTick() {
        super.mobTick();
        if (--jumpCooldown > 0) return;
        jumpCooldown = 140 + this.random.nextInt(220);
        PlayerEntity p = getWorld().getClosestPlayer(this, 24.0);
        if (p == null || !ScpUtil.isTargetablePlayer(p)) return;
        if (ScpUtil.isLookingAt(p, getEyePos(), 0.5)) return; // never while observed
        Vec3d back = p.getRotationVector().multiply(-1.0, 0.0, -1.0);
        if (back.lengthSquared() < 1.0E-4) return;
        back = back.normalize().multiply(1.6);
        BlockPos pos = BlockPos.ofFloored(p.getX() + back.x, p.getY(), p.getZ() + back.z);
        if (!getWorld().isAir(pos) || !getWorld().isAir(pos.up())) return;
        refreshPositionAndAngles(p.getX() + back.x, p.getY(), p.getZ() + back.z, p.getYaw(), 0.0f);
        setHeadYaw(p.getYaw());
        this.bodyYaw = p.getYaw();
        getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_STONE_PLACE, SoundCategory.HOSTILE, 0.6f, 0.5f);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 40, 0, true, false, false));
    }

    @Override
    public void takeKnockback(double strength, double x, double z) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public net.minecraft.sound.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLOCK_STONE_HIT;
    }

    @Override
    public net.minecraft.sound.SoundEvent getDeathSound() {
        return SoundEvents.BLOCK_STONE_BREAK;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }
}
