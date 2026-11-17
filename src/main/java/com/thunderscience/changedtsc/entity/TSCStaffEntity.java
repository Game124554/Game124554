package com.thunderscience.changedtsc.entity;

import com.thunderscience.changedtsc.registry.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Assimilation Staff: lab workers in white or black who fight latex up close with batons and blades. */
public class TSCStaffEntity extends AbstractTSCHuman {

    public TSCStaffEntity(EntityType<? extends TSCStaffEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    public String textureKind() {
        return "staff";
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        addCommonGoals(true);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(random.nextInt(4) == 0
                ? ModItems.PURIFIER_BLADE.get() : ModItems.STUN_BATON.get()));
        if (random.nextBoolean()) this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.HAZMAT_HELMET.get()));
        if (random.nextBoolean()) this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.HAZMAT_CHESTPLATE.get()));
        if (random.nextInt(3) == 0) this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.HAZMAT_LEGGINGS.get()));
        if (random.nextInt(3) == 0) this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.HAZMAT_BOOTS.get()));
        for (EquipmentSlot slot : EquipmentSlot.values()) this.setDropChance(slot, 0.06F);
    }
}
