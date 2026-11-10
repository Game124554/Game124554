package net.latexdrones.registry;

import net.latexdrones.LatexDrones;
import net.latexdrones.effect.HypnosisEffect;
import net.latexdrones.effect.SimpleEffect;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class ModEffects {
    private ModEffects() {
    }

    /** Indicator only - the real infection meter lives in InfectionData. */
    public static final MobEffect LATEX_INFECTION = register("latex_infection", new SimpleEffect(MobEffectCategory.HARMFUL, 0x1A1A22));
    /** Drags you towards the nearest hypnotist and blurs your screen. */
    public static final MobEffect HYPNOSIS = register("hypnosis", new HypnosisEffect());
    /** Granted by antidotes: infection gain is cut to a quarter and stops growing on its own. */
    public static final MobEffect INOCULATED = register("inoculated", new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x46D7F5));

    private static MobEffect register(String name, MobEffect effect) {
        return Registry.register(BuiltInRegistries.MOB_EFFECT, LatexDrones.id(name), effect);
    }

    /** Forces the static initialiser to run. */
    public static void init() {
    }
}
