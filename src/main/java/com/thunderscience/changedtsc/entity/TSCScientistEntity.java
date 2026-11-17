package com.thunderscience.changedtsc.entity;

import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.infection.GooInfection;
import com.thunderscience.changedtsc.item.FirstAidKitItem;
import com.thunderscience.changedtsc.registry.ModItems;
import com.thunderscience.changedtsc.util.LatexHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** Scientist: runs from latex and patches up nearby players, humans and friendly Puro. */
public class TSCScientistEntity extends AbstractTSCHuman {

    public TSCScientistEntity(EntityType<? extends TSCScientistEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    public String textureKind() {
        return "scientist";
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, LivingEntity.class, 12.0F, 1.0D, 1.35D,
                LatexHelper::isHostileLatex));
        addCommonGoals(false);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.FIRST_AID_KIT.get()));
        if (random.nextInt(3) == 0) this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.HAZMAT_HELMET.get()));
        this.setDropChance(EquipmentSlot.OFFHAND, 0.25F);
        this.setDropChance(EquipmentSlot.HEAD, 0.08F);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || this.tickCount % 60 != 0 || !TSCConfig.SCIENTISTS_HEAL.get()) return;

        if (this.getHealth() < this.getMaxHealth()) this.heal(2.0F);

        List<LivingEntity> patients = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(5.0D),
                e -> e != this && e.isAlive()
                        && (e instanceof Player || e instanceof AbstractTSCHuman || LatexHelper.isFriendlyLatex(e))
                        && (e.getHealth() < e.getMaxHealth() * 0.6F || GooInfection.get(e) > 0.0F)
                        && this.getLastHurtByMob() != e);
        if (!patients.isEmpty()) {
            LivingEntity patient = patients.get(0);
            this.getLookControl().setLookAt(patient, 30.0F, 30.0F);
            this.swing(InteractionHand.OFF_HAND);
            FirstAidKitItem.applyTreatment(patient, GooInfection.stage(patient) < 2);
        }
    }
}
