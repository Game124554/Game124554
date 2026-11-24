package com.mandelahorror.registry;

import com.mandelahorror.MandelaHorror;
import com.mandelahorror.entity.AlternateEntity;
import com.mandelahorror.entity.MimicEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {

    /** The tall dark Alternate with white eyes (GeckoLib model). */
    public static final EntityType<AlternateEntity> ALTERNATE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            MandelaHorror.id("alternate"),
            EntityType.Builder.of(AlternateEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.95F)
                    .clientTrackingRange(10)
                    .fireImmune()
                    .build(MandelaHorror.MODID + ":alternate"));

    /** A fake "player" wearing a random skin (or yours). Turns into an Alternate. */
    public static final EntityType<MimicEntity> MIMIC = Registry.register(BuiltInRegistries.ENTITY_TYPE,
            MandelaHorror.id("mimic"),
            EntityType.Builder.of(MimicEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build(MandelaHorror.MODID + ":mimic"));

    public static void register() {
        FabricDefaultAttributeRegistry.register(ALTERNATE, AlternateEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MIMIC, MimicEntity.createAttributes());
    }

    private ModEntities() {}
}
