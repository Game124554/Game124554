package com.latexdrone.registry;

import com.latexdrone.LatexDroneMod;
import com.latexdrone.effect.HypnosisEffect;
import com.latexdrone.effect.LatexInfectionEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEffects {
    public static final StatusEffect HYPNOSIS = Registry.register(Registries.STATUS_EFFECT, LatexDroneMod.id("hypnosis"), new HypnosisEffect());
    public static final StatusEffect LATEX_INFECTION = Registry.register(Registries.STATUS_EFFECT, LatexDroneMod.id("latex_infection"), new LatexInfectionEffect());

    public static void init() {}
}
