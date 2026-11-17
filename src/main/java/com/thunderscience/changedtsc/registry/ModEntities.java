package com.thunderscience.changedtsc.registry;

import com.thunderscience.changedtsc.ChangedTSC;
import com.thunderscience.changedtsc.entity.BulletEntity;
import com.thunderscience.changedtsc.entity.TSCGuardEntity;
import com.thunderscience.changedtsc.entity.TSCScientistEntity;
import com.thunderscience.changedtsc.entity.TSCStaffEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ChangedTSC.MODID);

    public static final RegistryObject<EntityType<TSCGuardEntity>> GUARD = ENTITIES.register("tsc_guard",
            () -> EntityType.Builder.<TSCGuardEntity>of(TSCGuardEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(10).build(ChangedTSC.MODID + ":tsc_guard"));

    public static final RegistryObject<EntityType<TSCScientistEntity>> SCIENTIST = ENTITIES.register("tsc_scientist",
            () -> EntityType.Builder.<TSCScientistEntity>of(TSCScientistEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(10).build(ChangedTSC.MODID + ":tsc_scientist"));

    public static final RegistryObject<EntityType<TSCStaffEntity>> STAFF = ENTITIES.register("assimilation_staff",
            () -> EntityType.Builder.<TSCStaffEntity>of(TSCStaffEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(10).build(ChangedTSC.MODID + ":assimilation_staff"));

    public static final RegistryObject<EntityType<BulletEntity>> BULLET = ENTITIES.register("bullet",
            () -> EntityType.Builder.<BulletEntity>of(BulletEntity::new, MobCategory.MISC)
                    .sized(0.15F, 0.15F).clientTrackingRange(4).updateInterval(5).build(ChangedTSC.MODID + ":bullet"));

    private ModEntities() {}
}
