package com.mandelahorror.registry;

import com.mandelahorror.MandelaHorror;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/** Damage type is data-driven: data/mandelahorror/damage_type/self_stab.json */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> SELF_STAB =
            ResourceKey.create(Registries.DAMAGE_TYPE, MandelaHorror.id("self_stab"));

    public static DamageSource selfStab(Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SELF_STAB));
    }

    private ModDamageTypes() {}
}
