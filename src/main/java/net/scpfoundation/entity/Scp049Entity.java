package net.scpfoundation.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/** SCP-049. Calm, slow and talkative. Its touch withers; whatever it kills rises as SCP-049-2 (see ModEvents). */
public class Scp049Entity extends HostileEntity {
    private int talkCooldown = 100;

    public Scp049Entity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 40;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 120.0)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    public void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false,
                living -> !(living instanceof PlayerEntity p && net.scpfoundation.world.ZombiePlayers.isZombie(p))));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, false));
    }

    @Override
    public void mobTick() {
        super.mobTick();
        if (--talkCooldown <= 0) {
            talkCooldown = 300 + this.random.nextInt(300);
            LivingEntity target = getTarget();
            if (target instanceof PlayerEntity player && player.squaredDistanceTo(this) < 20.0 * 20.0) {
                Text line = Text.literal("SCP-049: ").formatted(Formatting.DARK_GRAY)
                        .append(Text.translatable("message.scpfoundation.049." + this.random.nextInt(4)).formatted(Formatting.GRAY, Formatting.ITALIC));
                player.sendMessage(line, false);
            }
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1), this);
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), this);
        }
        return hit;
    }

    @Override
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WITCH_AMBIENT;
    }

    @Override
    public float getSoundPitch() {
        return 0.6f;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_WITCH_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WITCH_DEATH;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }
}
