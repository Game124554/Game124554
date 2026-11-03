package com.latexdrone.effect;

import com.latexdrone.entity.EvilScientistEntity;
import com.latexdrone.infection.InfectionManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Hypnosis: slows you, slowly raises latex infection ("accept the latex...") and, on the client,
 * draws a spinning spiral and pulls your view toward the nearest scientist.
 * Hypnotized mobs forget their target and stand still.
 */
public class HypnosisEffect extends StatusEffect {
    public HypnosisEffect() {
        super(StatusEffectCategory.HARMFUL, 0xC23DFF);
        addAttributeModifier(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED,
                "7107DE5E-7CE8-4030-940E-514C1F160890", -0.30, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                "8a2c4a11-3b7e-4c55-9f1d-2b6d0c7a9e01", -0.25, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (entity.getWorld().isClient) return;
        if (entity instanceof MobEntity mob && !(entity instanceof EvilScientistEntity)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        if (entity instanceof ServerPlayerEntity player && player.age % 40 == 0) {
            InfectionManager.addInfection(player, 1.5f + amplifier, -1);
        }
    }
}
