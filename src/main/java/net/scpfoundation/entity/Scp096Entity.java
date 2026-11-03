package net.scpfoundation.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * SCP-096. Docile until a player looks at its face (through glass counts). It then screams for a few seconds
 * and hunts that player relentlessly, smashing through walls, until the player is dead.
 */
public class Scp096Entity extends HostileEntity {
    public static final int CALM = 0;
    public static final int TRIGGERED = 1;
    public static final int ENRAGED = 2;
    private static final TrackedData<Integer> STATE = DataTracker.registerData(Scp096Entity.class, TrackedDataHandlerRegistry.INTEGER);

    @Nullable
    private UUID victim;
    private int stateTimer;

    public Scp096Entity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 60;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 400.0)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 30.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 128.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(STATE, CALM);
    }

    @Override
    public void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.5));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    public int getState() {
        return this.dataTracker.get(STATE);
    }

    private void setState(int state) {
        this.dataTracker.set(STATE, state);
        getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(state == ENRAGED ? 0.48 : 0.2);
    }

    @Override
    public void mobTick() {
        super.mobTick();
        switch (getState()) {
            case CALM -> {
                setTarget(null);
                if (this.age % 4 == 0) {
                    for (PlayerEntity p : getWorld().getPlayers()) {
                        if (ScpUtil.isTargetablePlayer(p) && p.squaredDistanceTo(this) < 48.0 * 48.0 && seesFace(p)) {
                            trigger(p);
                            break;
                        }
                    }
                }
                if (this.age % 40 == 0 && getHealth() < getMaxHealth()) heal(2.0f);
            }
            case TRIGGERED -> {
                getNavigation().stop();
                if (--stateTimer % 20 == 0) playSound(SoundEvents.ENTITY_GHAST_SCREAM, 2.5f, 0.55f);
                if (stateTimer <= 0) setState(ENRAGED);
            }
            case ENRAGED -> {
                PlayerEntity target = victim == null ? null : getWorld().getPlayerByUuid(victim);
                if (target == null || !ScpUtil.isTargetablePlayer(target)) {
                    calmDown();
                    return;
                }
                setTarget(target);
                if (this.horizontalCollision) ScpUtil.smashAhead(this, 0, 3);
                if (this.age % 30 == 0) playSound(SoundEvents.ENTITY_GHAST_SCREAM, 2.0f, 0.5f);
                // If it gets left far behind, it simply catches up.
                if (squaredDistanceTo(target) > 80.0 * 80.0) {
                    Vec3d behind = target.getPos().subtract(target.getRotationVector().multiply(12.0));
                    teleport(behind.x, target.getY(), behind.z, false);
                }
            }
            default -> setState(CALM);
        }
    }

    private boolean seesFace(PlayerEntity p) {
        Vec3d face = new Vec3d(getX(), getEyeY(), getZ());
        if (!ScpUtil.isLookingAt(p, face, 0.97)) return false;
        // The player must see the front of its head, not the back.
        Vec3d toPlayer = p.getEyePos().subtract(face).normalize();
        return getRotationVec(1.0f).normalize().dotProduct(toPlayer) > 0.2;
    }

    private void trigger(PlayerEntity p) {
        victim = p.getUuid();
        stateTimer = 100;
        setState(TRIGGERED);
        p.sendMessage(Text.translatable("message.scpfoundation.096_triggered").formatted(Formatting.DARK_RED, Formatting.BOLD), true);
    }

    private void calmDown() {
        victim = null;
        setTarget(null);
        setState(CALM);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Attacking it while calm only angers it at the attacker after it has seen them; mostly it ignores pain.
        return super.damage(source, getState() == ENRAGED ? amount * 0.3f : amount);
    }

    @Override
    public SoundEvent getAmbientSound() {
        return getState() == CALM ? SoundEvents.ENTITY_GHAST_AMBIENT : null;
    }

    @Override
    public float getSoundVolume() {
        return 0.6f;
    }

    @Override
    public float getSoundPitch() {
        return 0.5f;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_GHAST_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_GHAST_DEATH;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Rage", getState());
        nbt.putInt("RageTimer", stateTimer);
        if (victim != null) nbt.putUuid("Victim", victim);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        stateTimer = nbt.getInt("RageTimer");
        victim = nbt.containsUuid("Victim") ? nbt.getUuid("Victim") : null;
        setState(nbt.getInt("Rage"));
    }
}
