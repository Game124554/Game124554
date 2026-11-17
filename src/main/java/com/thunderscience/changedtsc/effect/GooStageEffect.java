package com.thunderscience.changedtsc.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Display-only effect for one goo infection stage (Goo Inside Body / Furry Body / Transfurring).
 * All the real logic lives in GooInfection; this just shows the stage in the HUD and inventory.
 */
public class GooStageEffect extends MobEffect {
    public GooStageEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }
}
