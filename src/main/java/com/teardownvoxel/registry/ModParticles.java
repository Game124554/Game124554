package com.teardownvoxel.registry;

import com.teardownvoxel.TeardownVoxel;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModParticles {
    public static final DefaultParticleType SPARK = FabricParticleTypes.simple(true);
    public static final DefaultParticleType DUST_CLOUD = FabricParticleTypes.simple(true);
    public static final DefaultParticleType PEBBLE = FabricParticleTypes.simple(false);

    public static void register() {
        Registry.register(Registries.PARTICLE_TYPE, TeardownVoxel.id("spark"), SPARK);
        Registry.register(Registries.PARTICLE_TYPE, TeardownVoxel.id("dust_cloud"), DUST_CLOUD);
        Registry.register(Registries.PARTICLE_TYPE, TeardownVoxel.id("pebble"), PEBBLE);
    }

    private ModParticles() {
    }
}
