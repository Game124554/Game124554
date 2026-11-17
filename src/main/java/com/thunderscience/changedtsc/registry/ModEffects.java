package com.thunderscience.changedtsc.registry;

import com.thunderscience.changedtsc.ChangedTSC;
import com.thunderscience.changedtsc.effect.GooStageEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ChangedTSC.MODID);

    /** Stage 1 – human body, goo moving inside. */
    public static final RegistryObject<MobEffect> GOO_INSIDE = EFFECTS.register("goo_inside", () -> new GooStageEffect(0x2B2B33));
    /** Stage 2 – fur and goo spreading over the body. */
    public static final RegistryObject<MobEffect> FURRY_BODY = EFFECTS.register("furry_body", () -> new GooStageEffect(0x5A5A66));
    /** Stage 3 – about to transfur. */
    public static final RegistryObject<MobEffect> TRANSFURRING = EFFECTS.register("transfurring", () -> new GooStageEffect(0x0B0B0F));

    private ModEffects() {}
}
