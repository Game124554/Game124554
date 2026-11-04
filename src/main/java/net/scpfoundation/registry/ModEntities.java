package net.scpfoundation.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.scpfoundation.ScpFoundation;
import net.scpfoundation.entity.Scp018Entity;
import net.scpfoundation.entity.Scp066Entity;
import net.scpfoundation.entity.Scp1471Entity;
import net.scpfoundation.entity.Scp2295Entity;
import net.scpfoundation.entity.Scp0492Entity;
import net.scpfoundation.entity.Scp049Entity;
import net.scpfoundation.entity.Scp096Entity;
import net.scpfoundation.entity.Scp1048Entity;
import net.scpfoundation.entity.Scp106Entity;
import net.scpfoundation.entity.Scp131Entity;
import net.scpfoundation.entity.Scp173Entity;
import net.scpfoundation.entity.Scp529Entity;
import net.scpfoundation.entity.Scp682Entity;
import net.scpfoundation.entity.Scp939Entity;
import net.scpfoundation.entity.Scp999Entity;

public final class ModEntities {
    public static final EntityType<Scp173Entity> SCP_173 = register("scp_173",
            EntityType.Builder.<Scp173Entity>create(Scp173Entity::new, SpawnGroup.MONSTER).setDimensions(0.7f, 2.3f).maxTrackingRange(10).makeFireImmune());
    public static final EntityType<Scp096Entity> SCP_096 = register("scp_096",
            EntityType.Builder.<Scp096Entity>create(Scp096Entity::new, SpawnGroup.MONSTER).setDimensions(0.6f, 2.6f).maxTrackingRange(10));
    public static final EntityType<Scp049Entity> SCP_049 = register("scp_049",
            EntityType.Builder.<Scp049Entity>create(Scp049Entity::new, SpawnGroup.MONSTER).setDimensions(0.6f, 1.95f).maxTrackingRange(8));
    public static final EntityType<Scp0492Entity> SCP_049_2 = register("scp_049_2",
            EntityType.Builder.<Scp0492Entity>create(Scp0492Entity::new, SpawnGroup.MONSTER).setDimensions(0.6f, 1.95f).maxTrackingRange(8));
    public static final EntityType<Scp106Entity> SCP_106 = register("scp_106",
            EntityType.Builder.<Scp106Entity>create(Scp106Entity::new, SpawnGroup.MONSTER).setDimensions(0.6f, 2.1f).maxTrackingRange(10).makeFireImmune());
    public static final EntityType<Scp682Entity> SCP_682 = register("scp_682",
            EntityType.Builder.<Scp682Entity>create(Scp682Entity::new, SpawnGroup.MONSTER).setDimensions(2.6f, 2.2f).maxTrackingRange(12).makeFireImmune());
    public static final EntityType<Scp939Entity> SCP_939 = register("scp_939",
            EntityType.Builder.<Scp939Entity>create(Scp939Entity::new, SpawnGroup.MONSTER).setDimensions(1.2f, 1.6f).maxTrackingRange(10));
    public static final EntityType<Scp999Entity> SCP_999 = register("scp_999",
            EntityType.Builder.<Scp999Entity>create(Scp999Entity::new, SpawnGroup.CREATURE).setDimensions(0.9f, 0.95f).maxTrackingRange(8));
    public static final EntityType<Scp131Entity> SCP_131 = register("scp_131",
            EntityType.Builder.<Scp131Entity>create(Scp131Entity::new, SpawnGroup.CREATURE).setDimensions(0.45f, 0.7f).maxTrackingRange(8));
    public static final EntityType<Scp529Entity> SCP_529 = register("scp_529",
            EntityType.Builder.<Scp529Entity>create(Scp529Entity::new, SpawnGroup.CREATURE).setDimensions(0.5f, 0.6f).maxTrackingRange(8));
    public static final EntityType<Scp1048Entity> SCP_1048 = register("scp_1048",
            EntityType.Builder.<Scp1048Entity>create(Scp1048Entity::new, SpawnGroup.CREATURE).setDimensions(0.5f, 0.8f).maxTrackingRange(8));
    public static final EntityType<Scp066Entity> SCP_066 = register("scp_066",
            EntityType.Builder.<Scp066Entity>create(Scp066Entity::new, SpawnGroup.CREATURE).setDimensions(0.6f, 0.6f).maxTrackingRange(8));
    public static final EntityType<Scp1471Entity> SCP_1471_A = register("scp_1471_a",
            EntityType.Builder.<Scp1471Entity>create(Scp1471Entity::new, SpawnGroup.MISC).setDimensions(0.7f, 2.6f).maxTrackingRange(10));
    public static final EntityType<Scp2295Entity> SCP_2295 = register("scp_2295",
            EntityType.Builder.<Scp2295Entity>create(Scp2295Entity::new, SpawnGroup.CREATURE).setDimensions(0.5f, 0.8f).maxTrackingRange(8));
    public static final EntityType<Scp018Entity> SCP_018 = register("scp_018",
            EntityType.Builder.<Scp018Entity>create(Scp018Entity::new, SpawnGroup.MISC).setDimensions(0.25f, 0.25f).maxTrackingRange(6).trackingTickInterval(2));

    private ModEntities() {}

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        return Registry.register(Registries.ENTITY_TYPE, ScpFoundation.id(name), builder.build(name));
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(SCP_173, Scp173Entity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCP_096, Scp096Entity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCP_049, Scp049Entity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCP_049_2, Scp0492Entity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCP_106, Scp106Entity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCP_682, Scp682Entity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCP_939, Scp939Entity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCP_999, Scp999Entity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCP_131, Scp131Entity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCP_529, Scp529Entity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCP_1048, Scp1048Entity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCP_066, Scp066Entity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCP_2295, Scp2295Entity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCP_1471_A, Scp1471Entity.createAttributes());
    }
}
