package net.scpfoundation.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

/**
 * SCP-008 infection. Causes fever-like symptoms; what happens when it runs its course is handled
 * in {@link net.scpfoundation.world.ZombiePlayers} (the victim turns into SCP-049-2). Cured by SCP-500.
 */
public class Scp008InfectionEffect extends StatusEffect {
    public Scp008InfectionEffect() {
        super(StatusEffectCategory.HARMFUL, 0x4E6B3A);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration % 100 == 0;
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (entity.getWorld().isClient) return;
        if (entity instanceof PlayerEntity player) player.addExhaustion(3.0f);
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 80, 0, true, false, false));
        if (entity.getHealth() > 4.0f) entity.damage(entity.getDamageSources().magic(), 1.0f);
    }
}
