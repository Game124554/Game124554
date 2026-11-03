package com.latexdrone.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/** Display-only effect: shows the infection stage in the inventory/HUD. The real logic is in InfectionManager. */
public class LatexInfectionEffect extends StatusEffect {
    public LatexInfectionEffect() {
        super(StatusEffectCategory.HARMFUL, 0x1A1A22);
    }
}
