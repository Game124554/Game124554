package net.scpfoundation.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * SCP-173. It cannot move while any player is looking at it. Nearby players periodically "blink"
 * (a moment of blindness) and that is when it strikes.
 */
public class Scp173Entity extends HostileEntity {
    private static final TrackedData<Boolean> OBSERVED = DataTracker.registerData(Scp173Entity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private float frozenYaw;
    private float frozenHeadYaw;
    private float frozenBodyYaw;
    private int blinkTimer = 200;

    public Scp173Entity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.moveControl = new StatueMoveControl(this);
        this.experiencePoints = 50;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 250.0)
                .add(EntityAttributes.GENERIC_ARMOR, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 40.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(OBSERVED, false);
    }

    @Override
    public void initGoals() {
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.6));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, false, false, null));
    }

    public boolean isObserved() {
        return this.dataTracker.get(OBSERVED);
    }

    @Override
    public void mobTick() {
        super.mobTick();
        boolean seen = computeObserved();
        if (seen && !isObserved()) {
            frozenYaw = getYaw();
            frozenHeadYaw = getHeadYaw();
            frozenBodyYaw = this.bodyYaw;
        }
        this.dataTracker.set(OBSERVED, seen);
        if (seen) {
            getNavigation().stop();
            setVelocity(0.0, getVelocity().y, 0.0);
        } else if (getTarget() != null && this.age % 6 == 0 && getVelocity().horizontalLengthSquared() > 1.0E-3) {
            playSound(SoundEvents.BLOCK_GRINDSTONE_USE, 0.5f, 0.4f);
        }
        handleBlink();
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient && isObserved()) {
            setYaw(frozenYaw);
            setHeadYaw(frozenHeadYaw);
            this.bodyYaw = frozenBodyYaw;
            setPitch(0.0f);
        }
    }

    private boolean computeObserved() {
        Vec3d head = new Vec3d(getX(), getEyeY(), getZ());
        Vec3d body = new Vec3d(getX(), getBodyY(0.5), getZ());
        boolean dark = getWorld().getLightLevel(getBlockPos().up()) <= 0;
        for (PlayerEntity p : getWorld().getPlayers()) {
            if (p.isSpectator() || !p.isAlive() || p.squaredDistanceTo(this) > 64.0 * 64.0) continue;
            if (p.hasStatusEffect(StatusEffects.BLINDNESS) || p.hasStatusEffect(StatusEffects.DARKNESS)) continue;
            if (dark && !p.hasStatusEffect(StatusEffects.NIGHT_VISION)) continue;
            if (ScpUtil.isLookingAt(p, head, 0.45) || ScpUtil.isLookingAt(p, body, 0.45)) return true;
        }
        return false;
    }

    private void handleBlink() {
        if (--blinkTimer > 0) return;
        blinkTimer = 140 + this.random.nextInt(120);
        for (PlayerEntity p : getWorld().getPlayers()) {
            if (ScpUtil.isTargetablePlayer(p) && p.squaredDistanceTo(this) < 32.0 * 32.0) {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 16, 0, false, false, false));
                p.sendMessage(Text.translatable("message.scpfoundation.blink").formatted(Formatting.GRAY, Formatting.ITALIC), true);
            }
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (isObserved()) return false;
        boolean hit = super.tryAttack(target);
        if (hit) playSound(SoundEvents.BLOCK_BONE_BLOCK_BREAK, 1.6f, 0.6f);
        return hit;
    }

    @Override
    public void takeKnockback(double strength, double x, double z) {
        // It is a block of concrete.
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLOCK_STONE_HIT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.BLOCK_STONE_BREAK;
    }

    /** Stops all movement while observed. */
    static class StatueMoveControl extends MoveControl {
        private final Scp173Entity statue;

        StatueMoveControl(Scp173Entity statue) {
            super(statue);
            this.statue = statue;
        }

        @Override
        public void tick() {
            if (statue.isObserved()) {
                this.state = MoveControl.State.WAIT;
                this.entity.setForwardSpeed(0.0f);
                this.entity.setSidewaysSpeed(0.0f);
                this.entity.setUpwardSpeed(0.0f);
                return;
            }
            super.tick();
        }
    }
}
