package com.mandelahorror.entity;

import com.mandelahorror.HorrorConfig;
import com.mandelahorror.horror.HorrorUtil;
import com.mandelahorror.horror.Lines;
import com.mandelahorror.horror.Speech;
import com.mandelahorror.item.PaperData;
import com.mandelahorror.network.FxPacket;
import com.mandelahorror.network.ModNetwork;
import com.mandelahorror.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * The Alternate: very tall, pitch black, white staring eyes.
 * - Freezes and stares back when you look at it (that fills the M.A.D. bar fast).
 * - Creeps closer and teleports behind you when you don't.
 * - Talks to you (subtitles + text-to-speech).
 */
public class AlternateEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Boolean> STARING =
            SynchedEntityData.defineId(AlternateEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.alternate.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.alternate.walk");
    private static final RawAnimation STARE = RawAnimation.begin().thenLoop("animation.alternate.stare");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.alternate.attack");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean natural;
    private int lifeTicks = 20 * 60 * 6;
    private int speechCooldown = 60;
    private int teleportCooldown = 300;
    private int voiceCooldown = 0;
    private boolean spokeOnSpawn;
    private boolean wasStaring;

    public AlternateEntity(EntityType<? extends AlternateEntity> type, Level level) {
        super(type, level);
        this.xpReward = 25;
        this.setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STARING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new StalkGoal(this));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public boolean isStaring() { return this.entityData.get(STARING); }
    public void setStaring(boolean staring) { this.entityData.set(STARING, staring); }
    public void setNatural(boolean natural) { this.natural = natural; }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;
        ServerLevel level = (ServerLevel) this.level();

        if (this.natural) {
            if (--this.lifeTicks <= 0) { vanish(); return; }
            boolean daylight = level.isDay() && !level.dimensionType().hasFixedTime() && level.canSeeSky(this.blockPosition());
            if (HorrorConfig.ALTERNATE_NIGHT_ONLY.get() && daylight && this.tickCount % 40 == 0 && !isWatched()) {
                vanish();
                return;
            }
        }

        if (this.voiceCooldown > 0) this.voiceCooldown--;

        LivingEntity target = this.getTarget();
        if (target instanceof ServerPlayer player && player.isAlive()) {
            String name = player.getGameProfile().getName();

            // voice clip: when it first appears, when you start looking at it, and now and then
            boolean staring = this.isStaring();
            if (this.distanceTo(player) < 36F) {
                if (!this.spokeOnSpawn && this.tickCount > 10) {
                    this.spokeOnSpawn = true;
                    speakVoice();
                } else if (staring && !this.wasStaring) {
                    speakVoice();
                } else if (this.random.nextInt(900) == 0) {
                    speakVoice();
                }
            }
            this.wasStaring = staring;

            if (--this.speechCooldown <= 0 && this.distanceTo(player) < 28F) {
                this.speechCooldown = 220 + this.random.nextInt(320);
                Speech.alternateSays(player, Lines.alternate(name, this.random));
            }
            if (--this.teleportCooldown <= 0) {
                this.teleportCooldown = 300 + this.random.nextInt(400);
                boolean unseen = !HorrorUtil.isLookingAt(player, this, 0.5);
                if (unseen && (this.distanceTo(player) > 20F || this.random.nextFloat() < 0.35F)) {
                    teleportBehind(player);
                }
            }
        }
    }

    /**
     * Plays the Alternate's voice clip from where it stands (heard up to ~48 blocks).
     * Has its own cooldown so the 7-second clip never overlaps itself.
     * @return true if it played
     */
    public boolean speakVoice() {
        if (this.voiceCooldown > 0 || this.isSilent() || !HorrorConfig.ALTERNATE_VOICE.get()) return false;
        this.playSound(ModSounds.ALTERNATE_VOICE.get(), 1.5F, 1.0F);
        this.voiceCooldown = 20 * 25 + this.random.nextInt(20 * 30);
        this.speechCooldown = Math.max(this.speechCooldown, 180); // no text-to-speech on top of it
        return true;
    }

    /** The Alternate drops a note with a drawing and a message when a player kills it. */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (source.getEntity() instanceof Player killer) {
            this.spawnAtLocation(PaperData.alternateNote(killer.getGameProfile().getName()));
        }
    }

    private boolean isWatched() {
        for (Player p : this.level().players()) {
            if (p.distanceTo(this) < 64F && HorrorUtil.isLookingAt(p, this, 0.6)) return true;
        }
        return false;
    }

    /** Disappears in a puff of static. */
    public void vanish() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(0.5), this.getZ(), 30, 0.3, 1.0, 0.3, 0.02);
            this.playSound(ModSounds.STATIC.get(), 1.0F, 0.6F);
        }
        this.discard();
    }

    public boolean teleportBehind(Player player) {
        if (!(this.level() instanceof ServerLevel level)) return false;
        BlockPos pos = HorrorUtil.findSpotAround(level, player, this.getType(), 4, 9, true);
        if (pos == null) return false;
        level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(0.5), this.getZ(), 20, 0.3, 1.0, 0.3, 0.01);
        this.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        this.getNavigation().stop();
        this.playSound(ModSounds.WHISPER.get(), 1.0F, 0.8F + this.random.nextFloat() * 0.3F);
        return true;
    }

    /** Jump scare: right in front of your face. */
    public void appearInFrontOf(ServerPlayer player) {
        ServerLevel level = (ServerLevel) this.level();
        BlockPos pos = HorrorUtil.findSpotInFront(level, player, this.getType(), 2.5);
        if (pos == null) return;
        this.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        this.getLookControl().setLookAt(player, 180F, 180F);
        this.playSound(ModSounds.STATIC.get(), 2.0F, 0.5F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide && this.isAlive()
                && source.getEntity() instanceof ServerPlayer player && this.random.nextFloat() < 0.4F) {
            if (this.random.nextBoolean()) Speech.alternateSays(player, Lines.hurt(player.getGameProfile().getName()));
            ModNetwork.sendFx(player, FxPacket.STATIC_BURST, 10);
            teleportBehind(player);
        }
        return result;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        this.triggerAnim("attack", "attack");
        return super.doHurtTarget(target);
    }

    // ------------------------------------------------------------------ sounds

    @Override
    protected SoundEvent getAmbientSound() { return ModSounds.WHISPER.get(); }

    @Override
    public int getAmbientSoundInterval() { return 200; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.STATIC.get(); }

    @Override
    protected SoundEvent getDeathSound() { return ModSounds.STATIC.get(); }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // it makes no sound when it walks
    }

    @Override
    protected float getSoundVolume() { return 0.8F; }

    // ------------------------------------------------------------------ save

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Natural", this.natural);
        tag.putInt("LifeTicks", this.lifeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.natural = tag.getBoolean("Natural");
        if (tag.contains("LifeTicks")) this.lifeTicks = tag.getInt("LifeTicks");
    }

    // ------------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 6, this::mainAnimation));
        controllers.add(new AnimationController<>(this, "attack", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    private PlayState mainAnimation(AnimationState<AlternateEntity> state) {
        if (this.isStaring()) return state.setAndContinue(STARE);
        if (state.isMoving()) return state.setAndContinue(WALK);
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ------------------------------------------------------------------ AI

    /** Stand still while watched; creep closer while not. */
    static class StalkGoal extends Goal {
        private final AlternateEntity mob;
        private int attackCooldown;

        StalkGoal(AlternateEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = this.mob.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void stop() {
            this.mob.setStaring(false);
            this.mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity t = this.mob.getTarget();
            if (t == null) return;
            this.mob.getLookControl().setLookAt(t, 30.0F, 30.0F);
            double dist = this.mob.distanceTo(t);
            boolean watched = t instanceof Player p && HorrorUtil.isLookingAt(p, this.mob, 0.94);
            this.mob.setStaring(watched);

            if (watched) {
                this.mob.getNavigation().stop();
                return;
            }
            if (dist > 2.6) {
                this.mob.getNavigation().moveTo(t, dist > 16 ? 1.15 : 0.8);
            } else {
                this.mob.getNavigation().stop();
                if (--this.attackCooldown <= 0) {
                    this.attackCooldown = 40;
                    this.mob.doHurtTarget(t);
                }
            }
        }
    }
}
