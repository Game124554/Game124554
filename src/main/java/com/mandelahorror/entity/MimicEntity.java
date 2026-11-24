package com.mandelahorror.entity;

import com.mandelahorror.horror.FakeJoins;
import com.mandelahorror.horror.HorrorUtil;
import com.mandelahorror.horror.Lines;
import com.mandelahorror.horror.Speech;
import com.mandelahorror.network.FxPacket;
import com.mandelahorror.network.ModNetwork;
import com.mandelahorror.registry.ModEntities;
import com.mandelahorror.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * A "player" that isn't one. Wears a random default skin (or YOUR skin), has a normal name tag,
 * follows you at a distance and freezes when you look at it.
 * Get too close, hit it, or stare too long and it becomes an Alternate.
 */
public class MimicEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> DATA_NAME =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SKIN =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_COPY =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    @Nullable private UUID haunted;
    private boolean sessionBound;
    private boolean transforming;
    private int lifeTicks = 20 * 240;
    private int watchedTicks;

    public MimicEntity(EntityType<? extends MimicEntity> type, Level level) {
        super(type, level);
        for (EquipmentSlot slot : EquipmentSlot.values()) this.setDropChance(slot, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_NAME, "");
        this.entityData.define(DATA_SKIN, Optional.empty());
        this.entityData.define(DATA_COPY, Optional.empty());
    }

    /**
     * @param skinSeed   random UUID that picks one of the default skins (Steve, Alex, Zuri, Sunny...)
     * @param copyTarget if set, the mimic wears THIS player's real skin
     */
    public void setup(String name, UUID skinSeed, @Nullable UUID copyTarget, @Nullable UUID haunted, boolean sessionBound) {
        this.entityData.set(DATA_NAME, name);
        this.entityData.set(DATA_SKIN, Optional.of(skinSeed));
        this.entityData.set(DATA_COPY, Optional.ofNullable(copyTarget));
        this.haunted = haunted;
        this.sessionBound = sessionBound;
        this.setCustomName(Component.literal(name));
        this.setCustomNameVisible(true);
    }

    public String getMimicName() { return this.entityData.get(DATA_NAME); }
    public UUID getSkinSeed() { return this.entityData.get(DATA_SKIN).orElse(this.getUUID()); }
    @Nullable public UUID getCopyTarget() { return this.entityData.get(DATA_COPY).orElse(null); }

    @Nullable
    public Player getHauntedPlayer() {
        return this.haunted == null ? null : this.level().getPlayerByUUID(this.haunted);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FollowHauntedGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || this.transforming) return;

        // spawned by egg or /summon: pick a random identity
        if (this.getMimicName().isEmpty()) {
            this.setup(Lines.randomName(this.random, ""), UUID.randomUUID(), null, null, false);
        }

        if (--this.lifeTicks <= 0 || (this.sessionBound && this.tickCount % 20 == 0 && !FakeJoins.isActive(this.getSkinSeed()))) {
            vanish();
            return;
        }

        Player target = this.getHauntedPlayer();
        if (target == null) {
            Player nearest = this.level().getNearestPlayer(this, 32.0);
            if (nearest != null && !nearest.isSpectator()) this.haunted = nearest.getUUID();
            return;
        }
        if (!target.isAlive() || target.isSpectator()) return;

        double dist = this.distanceTo(target);
        if (HorrorUtil.isLookingAt(target, this, 0.9)) this.watchedTicks++;
        else this.watchedTicks = Math.max(0, this.watchedTicks - 2);

        if (dist < 3.5 || this.watchedTicks > 20 * 8) {
            transform(target);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (!this.level().isClientSide && !this.transforming && source.getEntity() instanceof Player player) {
            transform(player);
            return false;
        }
        return super.hurt(source, amount);
    }

    /** Reveals itself: becomes an Alternate. */
    public void transform(Player player) {
        if (this.transforming || !(this.level() instanceof ServerLevel level)) return;
        this.transforming = true;

        AlternateEntity alt = ModEntities.ALTERNATE.create(level);
        boolean voice = false;
        if (alt != null) {
            alt.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            alt.setNatural(true);
            alt.setTarget(player);
            level.addFreshEntity(alt);
            voice = alt.speakVoice();
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.STATIC.get(), SoundSource.HOSTILE, 1.5F, 0.8F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(0.5), this.getZ(), 40, 0.3, 0.8, 0.3, 0.02);
        if (player instanceof ServerPlayer sp) {
            ModNetwork.sendFx(sp, FxPacket.STATIC_BURST, 25);
            Speech.alternateSays(sp, Lines.reveal(sp.getGameProfile().getName(), this.random), !voice);
        }
        if (level.getServer() != null) FakeJoins.endSession(level.getServer(), this.getSkinSeed());
        this.discard();
    }

    private void vanish() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(0.5), this.getZ(), 20, 0.3, 0.8, 0.3, 0.01);
            if (this.sessionBound && level.getServer() != null) FakeJoins.endSession(level.getServer(), this.getSkinSeed());
        }
        this.discard();
    }

    // ------------------------------------------------------------------ misc

    @Override
    public boolean removeWhenFarAway(double distance) { return true; }

    @Override
    protected SoundEvent getAmbientSound() { return null; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PLAYER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("MimicName", this.getMimicName());
        tag.putUUID("SkinSeed", this.getSkinSeed());
        UUID copy = this.getCopyTarget();
        if (copy != null) tag.putUUID("CopyTarget", copy);
        if (this.haunted != null) tag.putUUID("Haunted", this.haunted);
        tag.putBoolean("SessionBound", this.sessionBound);
        tag.putInt("LifeTicks", this.lifeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        String name = tag.getString("MimicName");
        UUID seed = tag.hasUUID("SkinSeed") ? tag.getUUID("SkinSeed") : UUID.randomUUID();
        UUID copy = tag.hasUUID("CopyTarget") ? tag.getUUID("CopyTarget") : null;
        UUID haunt = tag.hasUUID("Haunted") ? tag.getUUID("Haunted") : null;
        if (!name.isEmpty()) this.setup(name, seed, copy, haunt, tag.getBoolean("SessionBound"));
        if (tag.contains("LifeTicks")) this.lifeTicks = tag.getInt("LifeTicks");
    }

    /** Keeps 5-12 blocks away. Freezes like a statue while you look at it. */
    static class FollowHauntedGoal extends Goal {
        private final MimicEntity mob;

        FollowHauntedGoal(MimicEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player p = this.mob.getHauntedPlayer();
            return p != null && p.isAlive() && !p.isSpectator() && this.mob.distanceTo(p) < 48F;
        }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void stop() { this.mob.getNavigation().stop(); }

        @Override
        public void tick() {
            Player p = this.mob.getHauntedPlayer();
            if (p == null) return;
            this.mob.getLookControl().setLookAt(p, 30.0F, 30.0F);
            double dist = this.mob.distanceTo(p);
            if (HorrorUtil.isLookingAt(p, this.mob, 0.85)) {
                this.mob.getNavigation().stop();
            } else if (dist > 12) {
                this.mob.getNavigation().moveTo(p, 1.0);
            } else if (dist > 5) {
                this.mob.getNavigation().moveTo(p, 0.6);
            } else {
                this.mob.getNavigation().stop();
            }
        }
    }
}
