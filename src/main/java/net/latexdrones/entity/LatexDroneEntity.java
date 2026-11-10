package net.latexdrones.entity;

import net.latexdrones.LatexDrones;
import net.latexdrones.content.Archetype;
import net.latexdrones.content.MobDef;
import net.latexdrones.content.MobDefs;
import net.latexdrones.infection.DroneTasks;
import net.latexdrones.infection.InfectionData;
import net.latexdrones.infection.InfectionManager;
import net.latexdrones.registry.ModEffects;
import net.latexdrones.registry.ModEntities;
import net.latexdrones.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The one entity class behind all 100 mobs. Stats, looks and behaviour come from the {@link MobDef}
 * registered for the entity type (drone units, latex animals, goo blobs, hover drones and the six scientists).
 */
public class LatexDroneEntity extends Monster implements RangedAttackMob {
    // NOTE: deliberately no initialiser - it is filled lazily while the super constructor is still running.
    private MobDef def;

    public LatexDroneEntity(EntityType<? extends LatexDroneEntity> type, Level level) {
        super(type, level);
        MobDef d = this.def();
        this.xpReward = d.health >= 100.0D ? 50 : (d.isBig() ? 12 : 5);
    }

    public MobDef def() {
        MobDef d = this.def;
        if (d == null) {
            d = ModEntities.BY_TYPE.get(this.getType());
            if (d == null) {
                d = MobDefs.ALL.get(6);
            }
            this.def = d;
        }
        return d;
    }

    public static AttributeSupplier.Builder createAttributes(MobDef def) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, def.health)
                .add(Attributes.ATTACK_DAMAGE, def.damage)
                .add(Attributes.MOVEMENT_SPEED, def.speed)
                .add(Attributes.ARMOR, def.armor)
                .add(Attributes.FOLLOW_RANGE, def.scientist ? 28.0D : 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, def.isBig() ? 0.5D : 0.0D);
    }

    // ------------------------------------------------------------------ AI
    @Override
    protected void registerGoals() {
        MobDef d = this.def();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        if (d.leaper) {
            this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.42F));
        }
        if (d.ranged) {
            this.goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0D, d.scientist ? 45 : 55, d.scientist ? 14.0F : 12.0F));
        } else {
            this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, false));
        }
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, LatexDroneEntity.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::wantsToAttack));
        if (!d.scientist) {
            this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
        }
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    /** Transformed players are part of the hive: drones leave them alone and scientists give them orders instead. */
    private boolean wantsToAttack(LivingEntity target) {
        return target instanceof Player player && !InfectionData.isTransformed(player);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.def().scientist && hand == InteractionHand.MAIN_HAND && InfectionData.isTransformed(player)) {
            if (player instanceof ServerPlayer drone) {
                DroneTasks.talk(drone, this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void customServerAiStep() {
        MobDef d = this.def();
        LivingEntity target = this.getTarget();
        if (this.tickCount % 20 == 0 && target instanceof Player player
                && InfectionData.isTransformed(player) && this.getLastHurtByMob() != player) {
            this.setTarget(null);
            target = null;
        }
        if (d.archetype == Archetype.HOVER && target != null
                && target.getEyeY() > this.getEyeY() + 0.5D && this.canAttack(target)) {
            // blaze-style lift so hover drones can follow a target upwards
            Vec3 v = this.getDeltaMovement();
            this.setDeltaMovement(v.add(0.0D, (0.3D - v.y) * 0.3D, 0.0D));
            this.hasImpulse = true;
        }
        super.customServerAiStep();
    }

    @Override
    public void aiStep() {
        if (this.def().archetype == Archetype.HOVER) {
            Vec3 v = this.getDeltaMovement();
            if (!this.onGround() && v.y < 0.0D) {
                this.setDeltaMovement(v.multiply(1.0D, 0.6D, 1.0D));
            }
        }
        super.aiStep();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount == 2) {
            MobDef d = this.def();
            if (!d.heldItem.isEmpty() && this.getMainHandItem().isEmpty()) {
                Item item = BuiltInRegistries.ITEM.get(LatexDrones.id(d.heldItem));
                if (item != Items.AIR) {
                    this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
                    this.setDropChance(EquipmentSlot.MAINHAND, 0.06F);
                }
            }
        }
    }

    // ------------------------------------------------------------------ attacks
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !this.level().isClientSide) {
            MobDef d = this.def();
            if (target instanceof ServerPlayer player) {
                InfectionManager.addInfection(player, d.infect, d.scientist ? "" : d.name);
                if (d.scientist) {
                    this.playSound(ModSounds.INJECT, 1.0F, 1.0F);
                }
                if (d.hypno) {
                    player.addEffect(new MobEffectInstance(ModEffects.HYPNOSIS, d.scientist ? 160 : 120, 0));
                }
            } else if (target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            if (d.fireImmune && d.archetype == Archetype.BEAST) {
                target.setSecondsOnFire(4);
            }
        }
        return hit;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        MobDef d = this.def();
        Level level = this.level();
        if (d.dartKind == MobDef.DART_HYPNO) {
            // no projectile: a hypnotic beam that needs a clear line of sight
            if (!this.hasLineOfSight(target)) {
                return;
            }
            target.addEffect(new MobEffectInstance(ModEffects.HYPNOSIS, 140, 0));
            this.playSound(ModSounds.HYPNOSIS, 1.0F, 1.2F);
            if (level instanceof ServerLevel server) {
                Vec3 from = this.getEyePosition();
                Vec3 step = target.getEyePosition().subtract(from).scale(1.0D / 12.0D);
                for (int i = 1; i <= 12; i++) {
                    Vec3 p = from.add(step.scale(i));
                    server.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                }
            }
            return;
        }
        DartEntity dart = new DartEntity(level, this);
        dart.setItem(new ItemStack(DartEntity.itemFor(d.dartKind)));
        dart.setSource(d.scientist ? "" : d.name, d.hypno ? 100 : 0);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333D) - dart.getY();
        double dz = target.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        dart.shoot(dx, dy + dist * 0.12D, dz, 1.5F, 6.0F);
        this.playSound(d.dartKind == MobDef.DART_GOO ? ModSounds.GOO_SPLAT : ModSounds.DART_SHOOT, 1.0F,
                0.9F + this.random.nextFloat() * 0.2F);
        level.addFreshEntity(dart);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        Archetype a = this.def().archetype;
        if (a == Archetype.HOVER || a == Archetype.BLOB) {
            return false;
        }
        return super.causeFallDamage(fallDistance, multiplier, source);
    }

    // ------------------------------------------------------------------ spawning
    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
        if (reason == MobSpawnType.NATURAL) {
            BlockPos pos = this.blockPosition();
            BlockPos below = pos.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                return false;
            }
            if (level instanceof ServerLevelAccessor server && !Monster.isDarkEnoughToSpawn(server, pos, this.random)) {
                return false;
            }
            if (this.def().isBig() && this.random.nextInt(4) != 0) {
                return false; // tough mobs are rarer
            }
        }
        return super.checkSpawnRules(level, reason);
    }

    // ------------------------------------------------------------------ sounds
    @Override
    protected SoundEvent getAmbientSound() {
        MobDef d = this.def();
        if (d.scientist) {
            return ModSounds.SCIENTIST_LAUGH;
        }
        switch (d.archetype) {
            case BEAST:
                return ModSounds.BEAST_GROWL;
            case BLOB:
                return ModSounds.BLOB_SQUISH;
            case HOVER:
                return ModSounds.HOVER_BUZZ;
            default:
                return ModSounds.DRONE_AMBIENT;
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.def().scientist ? ModSounds.SCIENTIST_HURT : ModSounds.DRONE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.def().scientist ? ModSounds.SCIENTIST_HURT : ModSounds.DRONE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        MobDef d = this.def();
        if (d.archetype == Archetype.HOVER) {
            return;
        }
        if (d.scientist) {
            this.playSound(SoundEvents.ZOMBIE_STEP, 0.15F, 1.0F);
        } else if (d.archetype == Archetype.BLOB) {
            this.playSound(ModSounds.BLOB_SQUISH, 0.2F, 1.0F);
        } else {
            this.playSound(ModSounds.DRONE_STEP, 0.2F, 1.0F);
        }
    }
}
