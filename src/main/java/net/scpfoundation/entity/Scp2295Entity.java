package net.scpfoundation.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.scpfoundation.registry.ModEffects;

/**
 * SCP-2295 "The Bear with a Heart of Patchwork": a stitched-together teddy bear that walks to injured people and
 * patches them up. It can even sew out an SCP-008 infection.
 */
public class Scp2295Entity extends PathAwareEntity {
    private int healCooldown;

    public Scp2295Entity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28);
    }

    @Override
    public void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.7));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    @Override
    public void mobTick() {
        super.mobTick();
        if (healCooldown > 0) {
            healCooldown--;
            return;
        }
        if (this.age % 20 != 0) return;
        PlayerEntity patient = null;
        for (PlayerEntity p : getWorld().getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(12.0), pl -> pl.isAlive() && !pl.isSpectator())) {
            if (p.getHealth() < p.getMaxHealth() * 0.6f || p.hasStatusEffect(ModEffects.SCP_008_INFECTION)) {
                patient = p;
                break;
            }
        }
        if (patient == null) return;
        if (squaredDistanceTo(patient) > 4.0) {
            getNavigation().startMovingTo(patient, 1.2);
            return;
        }
        patient.addStatusEffect(new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 1, 1));
        patient.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 0));
        patient.removeStatusEffect(ModEffects.SCP_008_INFECTION);
        patient.sendMessage(Text.translatable("message.scpfoundation.2295_heal").formatted(Formatting.LIGHT_PURPLE), true);
        ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.HEART, patient.getX(), patient.getY() + 1.5, patient.getZ(), 6, 0.4, 0.3, 0.4, 0.0);
        playSound(SoundEvents.ENTITY_SHEEP_SHEAR, 1.0f, 1.3f);
        healCooldown = 20 * 45;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLOCK_WOOL_HIT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.BLOCK_WOOL_BREAK;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }
}
