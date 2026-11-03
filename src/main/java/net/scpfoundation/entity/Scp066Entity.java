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
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * SCP-066 "Eric's Toy": a small, mobile mass of braided yarn. It keeps asking for "Eric" and every so often
 * emits a deafening burst of sound that hurts and disorients everyone nearby.
 */
public class Scp066Entity extends PathAwareEntity {
    private int noiseCooldown = 400;
    private int askCooldown = 60;

    public Scp066Entity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22);
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
        PlayerEntity near = getWorld().getClosestPlayer(this, 10.0);
        if (near == null || near.isSpectator()) return;
        if (--askCooldown <= 0 && squaredDistanceTo(near) < 36.0) {
            askCooldown = 120 + this.random.nextInt(120);
            near.sendMessage(Text.literal("SCP-066: ").formatted(Formatting.DARK_GRAY)
                    .append(Text.translatable("message.scpfoundation.066_eric").formatted(Formatting.GRAY, Formatting.ITALIC)), false);
            playSound(SoundEvents.ENTITY_ALLAY_ITEM_GIVEN, 1.0f, 0.5f);
        }
        if (--noiseCooldown <= 0) {
            noiseCooldown = 600 + this.random.nextInt(400);
            blast();
        }
    }

    private void blast() {
        playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 4.0f, 0.7f);
        if (getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.NOTE, getX(), getY() + 0.6, getZ(), 30, 1.5, 1.0, 1.5, 1.0);
        }
        for (PlayerEntity p : getWorld().getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(10.0), ScpUtil::isTargetablePlayer)) {
            p.damage(getDamageSources().sonicBoom(this), 4.0f);
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1));
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!getWorld().isClient) {
            player.sendMessage(Text.literal("SCP-066: ").formatted(Formatting.DARK_GRAY)
                    .append(Text.translatable("message.scpfoundation.066_eric").formatted(Formatting.GRAY, Formatting.ITALIC)), false);
            if (this.random.nextInt(4) == 0) noiseCooldown = 20;
        }
        return ActionResult.success(getWorld().isClient);
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
