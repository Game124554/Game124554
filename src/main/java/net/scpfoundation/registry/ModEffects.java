package net.scpfoundation.registry;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.scpfoundation.ScpFoundation;
import net.scpfoundation.effect.Scp008InfectionEffect;

public final class ModEffects {
    public static final StatusEffect SCP_008_INFECTION = Registry.register(Registries.STATUS_EFFECT,
            ScpFoundation.id("scp_008_infection"), new Scp008InfectionEffect());

    private ModEffects() {}

    public static void register() {
        // Static initialisation registers everything.
    }
}
