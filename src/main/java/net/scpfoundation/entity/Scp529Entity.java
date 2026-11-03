package net.scpfoundation.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** SCP-529 "Josie": a perfectly ordinary, friendly cat - apart from the missing back half. Loves fish. */
public class Scp529Entity extends PathAwareEntity {
    public Scp529Entity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 12.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3);
    }

    @Override
    public void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.4));
        this.goalSelector.add(3, new TemptGoal(this, 1.0, Ingredient.ofItems(Items.COD, Items.SALMON, Items.COOKED_COD, Items.COOKED_SALMON), false));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.isOf(Items.COD) || stack.isOf(Items.SALMON) || stack.isOf(Items.COOKED_COD) || stack.isOf(Items.COOKED_SALMON)) {
            if (!getWorld().isClient) {
                if (!player.getAbilities().creativeMode) stack.decrement(1);
                heal(4.0f);
                ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.HEART, getX(), getY() + 0.7, getZ(), 4, 0.3, 0.2, 0.3, 0.0);
                playSound(SoundEvents.ENTITY_CAT_EAT, 1.0f, 1.0f);
            }
            return ActionResult.success(getWorld().isClient);
        }
        if (!getWorld().isClient) playSound(SoundEvents.ENTITY_CAT_PURR, 1.0f, 1.0f);
        return ActionResult.success(getWorld().isClient);
    }

    @Override
    public SoundEvent getAmbientSound() {
        return this.random.nextInt(3) == 0 ? SoundEvents.ENTITY_CAT_PURREOW : SoundEvents.ENTITY_CAT_AMBIENT;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_CAT_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_CAT_DEATH;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }
}
