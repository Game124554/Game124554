package net.scpfoundation.entity;

import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

/** SCP-457 "Burning Man": a living fire. Sets everything it touches alight, heals in flames, hurt by water. */
public class Scp457Entity extends HostileEntity {
    public Scp457Entity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 40;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 100.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override
    public void initGoals() {
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void mobTick() {
        super.mobTick();
        ServerWorld world = (ServerWorld) getWorld();
        if (this.age % 2 == 0) {
            world.spawnParticles(ParticleTypes.FLAME, getX(), getBodyY(0.5), getZ(), 3, 0.25, 0.6, 0.25, 0.01);
        }
        if (this.age % 20 == 0) {
            if (isOnFire() || isInLava()) heal(4.0f);
            GameRules rules = world.getGameRules();
            if (rules.getBoolean(GameRules.DO_MOB_GRIEFING) && rules.getBoolean(GameRules.DO_FIRE_TICK)) {
                BlockPos pos = getBlockPos().add(this.random.nextInt(3) - 1, 0, this.random.nextInt(3) - 1);
                if (world.isAir(pos) && Blocks.FIRE.getDefaultState().canPlaceAt(world, pos)) {
                    world.setBlockState(pos, AbstractFireBlock.getState(world, pos));
                }
            }
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit) target.setOnFireFor(8);
        return hit;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isIn(DamageTypeTags.IS_FIRE)) return false;
        return super.damage(source, amount);
    }

    @Override
    public boolean hurtByWater() {
        return true;
    }

    @Override
    public boolean isOnFire() {
        return true;
    }

    @Override
    public float getBrightnessAtEyes() {
        return 1.0f;
    }

    @Override
    public SoundEvent getAmbientSound() {
        return SoundEvents.BLOCK_FIRE_AMBIENT;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_BLAZE_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.BLOCK_FIRE_EXTINGUISH;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }
}
