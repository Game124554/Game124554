package com.latexdrone.entity;

import com.latexdrone.LatexDroneMod;
import com.latexdrone.content.ScientistType;
import com.latexdrone.infection.InfectionManager;
import com.latexdrone.registry.ModEffects;
import com.latexdrone.registry.ModEntities;
import com.latexdrone.registry.ModSounds;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Evil anthro scientist in a lab coat. Carries a weapon; every hit injects the target
 * with latex serum (infection) and a hypnotic sedative (Hypnosis effect).
 * Leaves players who are already latex alone - they are a "success".
 */
public class EvilScientistEntity extends HostileEntity implements RangedAttackMob {

    public EvilScientistEntity(EntityType<? extends EvilScientistEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 15;
    }

    public ScientistType getScientist() {
        return ModEntities.scientistOf(this.getType());
    }

    public static DefaultAttributeContainer.Builder createAttributes(ScientistType s) {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, s.health())
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, s.ranged() ? 3.0 : 6.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27)
                .add(EntityAttributes.GENERIC_ARMOR, 3.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        if (getScientist().ranged()) {
            this.goalSelector.add(2, new ProjectileAttackGoal(this, 1.0, 35, 15.0f));
        } else {
            this.goalSelector.add(2, new MeleeAttackGoal(this, 1.2, false));
        }
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this, EvilScientistEntity.class, LatexMobEntity.class));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, e -> !InfectionManager.isLatex(e)));
    }

    private ItemStack weaponStack() {
        Item item = Registries.ITEM.get(LatexDroneMod.id(getScientist().weapon()));
        return new ItemStack(item);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        this.equipStack(EquipmentSlot.MAINHAND, weaponStack());
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.1f);
        return data;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) {
            if (this.age < 3 && this.getMainHandStack().isEmpty()) {
                this.equipStack(EquipmentSlot.MAINHAND, weaponStack());
                this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.1f);
            }
            this.setAttacking(this.getTarget() != null);
        }
    }

    /** Called by melee hits and darts: the injection. */
    public static void inject(LivingEntity target, int hypnoTicks, float infection) {
        target.addStatusEffect(new StatusEffectInstance(ModEffects.HYPNOSIS, hypnoTicks, 0));
        if (target instanceof ServerPlayerEntity player) {
            InfectionManager.addInfection(player, infection, -1);
        }
        target.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.SYRINGE_INJECT,
                target.getSoundCategory(), 1.0f, 1.0f);
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof LivingEntity living && !InfectionManager.isLatex(living)) {
            inject(living, 200, 18.0f);
        }
        return hit;
    }

    @Override
    public void attack(LivingEntity target, float pullProgress) {
        SyringeDartEntity dart = new SyringeDartEntity(this.getWorld(), this);
        double dx = target.getX() - this.getX();
        double dy = target.getBodyY(0.3333) - dart.getY();
        double dz = target.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        dart.setVelocity(dx, dy + dist * 0.15, dz, 1.7f, 5.0f);
        this.playSound(ModSounds.SYRINGE_SHOOT, 1.0f, 0.9f + random.nextFloat() * 0.2f);
        this.getWorld().spawnEntity(dart);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SCIENTIST_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SCIENTIST_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SCIENTIST_DEATH;
    }
}
