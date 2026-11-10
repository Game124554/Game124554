package net.latexdrones.registry;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.latexdrones.LatexDrones;
import net.latexdrones.content.MobDef;
import net.latexdrones.content.MobDefs;
import net.latexdrones.entity.DartEntity;
import net.latexdrones.entity.LatexDroneEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Registers the 100 mobs (one entity type each, all backed by {@link LatexDroneEntity}) and the dart projectile. */
public final class ModEntities {
    private ModEntities() {
    }

    public static final Map<EntityType<?>, MobDef> BY_TYPE = new IdentityHashMap<>();
    public static final Map<String, MobDef> BY_NAME = new LinkedHashMap<>();
    public static EntityType<DartEntity> DART;

    public static void init() {
        for (MobDef def : MobDefs.ALL) {
            FabricEntityTypeBuilder<LatexDroneEntity> builder = FabricEntityTypeBuilder
                    .<LatexDroneEntity>create(MobCategory.MONSTER, LatexDroneEntity::new)
                    .dimensions(EntityDimensions.scalable(def.width, def.height))
                    .trackRangeChunks(8);
            if (def.fireImmune) {
                builder = builder.fireImmune();
            }
            EntityType<LatexDroneEntity> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, LatexDrones.id(def.name), builder.build());
            def.type = type;
            BY_TYPE.put(type, def);
            BY_NAME.put(def.name, def);
            FabricDefaultAttributeRegistry.register(type, LatexDroneEntity.createAttributes(def));
            if (def.spawnWeight > 0) {
                // spawn conditions (darkness, solid ground) are enforced in LatexDroneEntity#checkSpawnRules
                BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, type,
                        def.spawnWeight, 1, def.maxGroup);
            }
        }
        DART = Registry.register(BuiltInRegistries.ENTITY_TYPE, LatexDrones.id("dart"),
                FabricEntityTypeBuilder.<DartEntity>create(MobCategory.MISC, DartEntity::new)
                        .dimensions(EntityDimensions.fixed(0.25F, 0.25F))
                        .trackRangeChunks(4)
                        .trackedUpdateRate(10)
                        .build());
    }

    /** Definition for a form name, falling back to the basic drone. */
    public static MobDef form(String name) {
        MobDef def = BY_NAME.get(name);
        return def != null ? def : BY_NAME.get("drone_unit");
    }
}
