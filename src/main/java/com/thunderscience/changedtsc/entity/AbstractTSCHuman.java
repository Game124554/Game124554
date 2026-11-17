package com.thunderscience.changedtsc.entity;

import com.thunderscience.changedtsc.util.LatexHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * Base for all Thunder Science Company humans. 20 face/hair variants, and a random
 * white or black outfit. Friendly to players unless attacked.
 */
public abstract class AbstractTSCHuman extends PathfinderMob {
    public static final int VARIANTS = 20;
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(AbstractTSCHuman.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DARK_OUTFIT =
            SynchedEntityData.defineId(AbstractTSCHuman.class, EntityDataSerializers.BOOLEAN);

    protected AbstractTSCHuman(EntityType<? extends AbstractTSCHuman> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false);
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
        }
    }

    /** "guard", "scientist" or "staff" – used to pick the texture. */
    public abstract String textureKind();

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, 0);
        this.entityData.define(DARK_OUTFIT, false);
    }

    /** Shared goals. Subclasses add their own attack / flee goals. */
    protected void addCommonGoals(boolean fights) {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        if (fights) {
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this, AbstractTSCHuman.class).setAlertOthers());
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                    LatexHelper::isHumanEnemy));
        }
    }

    public int getVariant() {
        return Math.floorMod(this.entityData.get(VARIANT), VARIANTS);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, Math.floorMod(variant, VARIANTS));
    }

    public boolean isDarkOutfit() {
        return this.entityData.get(DARK_OUTFIT);
    }

    public void setDarkOutfit(boolean dark) {
        this.entityData.set(DARK_OUTFIT, dark);
    }

    @Override
    @Nullable
    @SuppressWarnings("deprecation")
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        this.setVariant(this.random.nextInt(VARIANTS));
        this.setDarkOutfit(this.random.nextBoolean());
        this.populateDefaultEquipmentSlots(this.random, difficulty);
        this.setPersistenceRequired();
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant());
        tag.putBoolean("DarkOutfit", this.isDarkOutfit());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(tag.getInt("Variant"));
        this.setDarkOutfit(tag.getBoolean("DarkOutfit"));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
