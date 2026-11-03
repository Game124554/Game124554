package com.latexdrone.registry;

import com.latexdrone.LatexDroneMod;
import com.latexdrone.content.FormData;
import com.latexdrone.content.LatexForm;
import com.latexdrone.content.ScientistData;
import com.latexdrone.content.ScientistType;
import com.latexdrone.entity.EvilScientistEntity;
import com.latexdrone.entity.LatexMobEntity;
import com.latexdrone.entity.PurifierShotEntity;
import com.latexdrone.entity.SyringeDartEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModEntities {
    public static final List<EntityType<LatexMobEntity>> LATEX_TYPES = new ArrayList<>();
    public static final List<EntityType<EvilScientistEntity>> SCIENTIST_TYPES = new ArrayList<>();
    private static final Map<EntityType<?>, LatexForm> FORM_BY_TYPE = new HashMap<>();
    private static final Map<EntityType<?>, ScientistType> SCIENTIST_BY_TYPE = new HashMap<>();

    public static EntityType<SyringeDartEntity> SYRINGE_DART;
    public static EntityType<PurifierShotEntity> PURIFIER_SHOT;

    public static LatexForm formOf(EntityType<?> type) {
        LatexForm f = FORM_BY_TYPE.get(type);
        return f != null ? f : FormData.FORMS.get(0);
    }

    public static ScientistType scientistOf(EntityType<?> type) {
        ScientistType s = SCIENTIST_BY_TYPE.get(type);
        return s != null ? s : ScientistData.SCIENTISTS.get(0);
    }

    public static EntityType<LatexMobEntity> latexType(int formIndex) {
        return LATEX_TYPES.get(Math.max(0, Math.min(formIndex, LATEX_TYPES.size() - 1)));
    }

    public static void init() {
        for (LatexForm form : FormData.FORMS) {
            EntityType<LatexMobEntity> type = Registry.register(Registries.ENTITY_TYPE, LatexDroneMod.id(form.id()),
                    FabricEntityTypeBuilder.<LatexMobEntity>createMob()
                            .spawnGroup(SpawnGroup.MONSTER)
                            .entityFactory(LatexMobEntity::new)
                            .dimensions(EntityDimensions.fixed(0.6f * form.scale(), 1.95f * form.scale()))
                            .trackRangeBlocks(64)
                            .spawnRestriction(SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark)
                            .defaultAttributes(() -> LatexMobEntity.createAttributes(form))
                            .build());
            LATEX_TYPES.add(type);
            FORM_BY_TYPE.put(type, form);
        }
        for (ScientistType sci : ScientistData.SCIENTISTS) {
            EntityType<EvilScientistEntity> type = Registry.register(Registries.ENTITY_TYPE, LatexDroneMod.id(sci.id()),
                    FabricEntityTypeBuilder.<EvilScientistEntity>createMob()
                            .spawnGroup(SpawnGroup.MONSTER)
                            .entityFactory(EvilScientistEntity::new)
                            .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                            .trackRangeBlocks(64)
                            .spawnRestriction(SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark)
                            .defaultAttributes(() -> EvilScientistEntity.createAttributes(sci))
                            .build());
            SCIENTIST_TYPES.add(type);
            SCIENTIST_BY_TYPE.put(type, sci);
        }
        SYRINGE_DART = Registry.register(Registries.ENTITY_TYPE, LatexDroneMod.id("syringe_dart"),
                FabricEntityTypeBuilder.<SyringeDartEntity>create(SpawnGroup.MISC, SyringeDartEntity::new)
                        .dimensions(EntityDimensions.fixed(0.25f, 0.25f)).trackRangeBlocks(4).trackedUpdateRate(20).build());
        PURIFIER_SHOT = Registry.register(Registries.ENTITY_TYPE, LatexDroneMod.id("purifier_shot"),
                FabricEntityTypeBuilder.<PurifierShotEntity>create(SpawnGroup.MISC, PurifierShotEntity::new)
                        .dimensions(EntityDimensions.fixed(0.3f, 0.3f)).trackRangeBlocks(4).trackedUpdateRate(10).build());
    }

    /** Natural spawning in the Overworld at night / in the dark. */
    public static void initSpawns() {
        for (int i = 0; i < LATEX_TYPES.size(); i++) {
            LatexForm form = FormData.FORMS.get(i);
            int max = form.scale() >= 1.3f ? 1 : 2;
            BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, LATEX_TYPES.get(i), form.weight(), 1, max);
        }
        for (EntityType<EvilScientistEntity> type : SCIENTIST_TYPES) {
            BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, type, 1, 1, 1);
        }
    }
}
