package net.scpfoundation.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** SCP-131-A / SCP-131-B: tiny eye pods that follow players and point out hostiles (they glow). */
public class Scp131Entity extends PathAwareEntity {
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(Scp131Entity.class, TrackedDataHandlerRegistry.INTEGER);

    public Scp131Entity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        if (!world.isClient) this.dataTracker.set(VARIANT, this.random.nextInt(2));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 16.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32);
    }

    @Override
    public void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, 0);
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
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
            PlayerEntity p = getWorld().getClosestPlayer(this, 16.0);
            if (p != null && !p.isSpectator() && squaredDistanceTo(p) > 6.0) {
                getNavigation().startMovingTo(p, 1.1);
            }
        }
        if (this.age % 40 == 0) {
            for (HostileEntity h : getWorld().getEntitiesByClass(HostileEntity.class, getBoundingBox().expand(14.0), e -> true)) {
                h.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 60, 0, true, false, false));
            }
        }
    }

    @Override
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    public float getSoundPitch() {
        return 1.4f;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", getVariant());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Variant")) this.dataTracker.set(VARIANT, nbt.getInt("Variant") & 1);
    }
}
