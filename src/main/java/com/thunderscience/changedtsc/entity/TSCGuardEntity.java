package com.thunderscience.changedtsc.entity;

import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Armed security guard. Shoots hostile latex and monsters on sight. */
public class TSCGuardEntity extends AbstractTSCHuman implements RangedAttackMob {

    public TSCGuardEntity(EntityType<? extends TSCGuardEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    public String textureKind() {
        return "guard";
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0D, 14, 18.0F));
        addCommonGoals(true);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        int roll = random.nextInt(10);
        ItemStack gun = new ItemStack(roll < 5 ? ModItems.PISTOL.get() : roll < 8 ? ModItems.RIFLE.get() : ModItems.SHOTGUN.get());
        this.setItemSlot(EquipmentSlot.MAINHAND, gun);
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.GUARD_HELMET.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.GUARD_CHESTPLATE.get()));
        if (random.nextBoolean()) this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.GUARD_LEGGINGS.get()));
        if (random.nextBoolean()) this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.GUARD_BOOTS.get()));
        for (EquipmentSlot slot : EquipmentSlot.values()) this.setDropChance(slot, 0.06F);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        BulletEntity bullet = new BulletEntity(this.level(), this);
        bullet.setDamage(TSCConfig.GUARD_BULLET_DAMAGE.get().floatValue());
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.5D) - bullet.getY();
        double dz = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float inaccuracy = (float) (12 - this.level().getDifficulty().getId() * 3);
        bullet.shoot(dx, dy + horizontal * 0.02D, dz, 3.2F, inaccuracy);
        this.playSound(SoundEvents.FIREWORK_ROCKET_BLAST, 1.0F, 1.6F + this.random.nextFloat() * 0.2F);
        this.level().addFreshEntity(bullet);
    }
}
