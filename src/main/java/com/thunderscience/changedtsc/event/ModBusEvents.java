package com.thunderscience.changedtsc.event;

import com.thunderscience.changedtsc.ChangedTSC;
import com.thunderscience.changedtsc.entity.TSCGuardEntity;
import com.thunderscience.changedtsc.entity.TSCScientistEntity;
import com.thunderscience.changedtsc.entity.TSCStaffEntity;
import com.thunderscience.changedtsc.registry.ModEntities;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ChangedTSC.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModBusEvents {
    private ModBusEvents() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GUARD.get(), TSCGuardEntity.createAttributes().build());
        event.put(ModEntities.SCIENTIST.get(), TSCScientistEntity.createAttributes().build());
        event.put(ModEntities.STAFF.get(), TSCStaffEntity.createAttributes().build());
    }
}
