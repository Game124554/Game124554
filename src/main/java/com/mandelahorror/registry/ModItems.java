package com.mandelahorror.registry;

import com.mandelahorror.MandelaHorror;
import com.mandelahorror.item.StrangePaperItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public final class ModItems {

    public static final Item ALTERNATE_SPAWN_EGG = register("alternate_spawn_egg",
            new SpawnEggItem(ModEntities.ALTERNATE, 0x050505, 0xF4F4F4, new Item.Properties()));

    public static final Item MIMIC_SPAWN_EGG = register("mimic_spawn_egg",
            new SpawnEggItem(ModEntities.MIMIC, 0x2F6FAD, 0x111111, new Item.Properties()));

    /** Paper you can draw and write on. Crafted from paper + coal/charcoal. */
    public static final Item STRANGE_PAPER = register("strange_paper",
            new StrangePaperItem(new Item.Properties().stacksTo(1)));

    private static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, MandelaHorror.id(name), item);
    }

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
            entries.accept(ALTERNATE_SPAWN_EGG);
            entries.accept(MIMIC_SPAWN_EGG);
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries ->
                entries.accept(STRANGE_PAPER));
    }

    private ModItems() {}
}
