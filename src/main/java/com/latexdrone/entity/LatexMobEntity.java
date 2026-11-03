package com.latexdrone.entity;

import com.latexdrone.content.LatexForm;
import com.latexdrone.infection.InfectionManager;
import com.latexdrone.registry.ModEntities;
import com.latexdrone.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Vector3f;

/** All 50 latex creatures share this class; their stats/looks come from their LatexForm. */
public class LatexMobEntity extends HostileEntity {

    public LatexMobEntity(EntityType<? extends LatexMobEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 6;
    }

    public LatexForm getForm() {
        return ModEntities.formOf(this.getType());
    }

    public static DefaultAttributeContainer.Builder createAttributes(LatexForm form) {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, form.health())
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, form.damage())
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, form.speed())
                .add(EntityAttributes.GENERIC_ARMOR, form.armor())
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, form.scale() >= 1.3f ? 0.6 : 0.1);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.9));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this, LatexMobEntity.class, EvilScientistEntity.class).setGroupRevenge());
        // Latex creatures leave fellow latex (transformed players) alone.
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, e -> !InfectionManager.isLatex(e)));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, 10, true, false, null));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, IronGolemEntity.class, 10, true, false, null));
    }

    @Override
    public void tick() {
        super.tick();
        World world = this.getWorld();
        if (!world.isClient) {
            this.setAttacking(this.getTarget() != null);
        } else if (this.random.nextFloat() < 0.04f) {
            int c = getForm().primary();
            Vector3f color = new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f);
            world.addParticle(new DustParticleEffect(color, 1.2f), getParticleX(0.5), getRandomBodyY(), getParticleZ(0.5), 0, -0.05, 0);
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof ServerPlayerEntity player) {
            InfectionManager.addInfection(player, getForm().infect(), getForm().index());
            this.playSound(ModSounds.INFECT_HIT, 1.0f, 0.9f + random.nextFloat() * 0.2f);
        }
        return hit;
    }

    /** Villagers killed by latex become latex. */
    @Override
    public boolean onKilledOther(ServerWorld world, LivingEntity other) {
        boolean result = super.onKilledOther(world, other);
        if (other instanceof MerchantEntity) {
            Entity created = this.getType().create(world);
            if (created instanceof LatexMobEntity convert) {
                convert.refreshPositionAndAngles(other.getX(), other.getY(), other.getZ(), other.getYaw(), other.getPitch());
                convert.initialize(world, world.getLocalDifficulty(convert.getBlockPos()), SpawnReason.CONVERSION, null, null);
                convert.setPersistent();
                world.spawnEntity(convert);
                world.spawnParticles(ParticleTypes.SQUID_INK, other.getX(), other.getBodyY(0.5), other.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
                convert.playSound(ModSounds.TRANSFORM, 1.0f, 1.2f);
            }
        }
        return result;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return switch (getForm().sound()) {
            case "drone" -> ModSounds.DRONE_AMBIENT;
            case "goo" -> ModSounds.GOO_AMBIENT;
            default -> ModSounds.LATEX_AMBIENT;
        };
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return "drone".equals(getForm().sound()) ? ModSounds.DRONE_HURT : ModSounds.LATEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.LATEX_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.BLOCK_SLIME_BLOCK_STEP, 0.25f, 1.0f / getForm().scale());
    }

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() / (float) Math.sqrt(getForm().scale());
    }
}
