package com.thunderscience.changedtsc.compat;

import com.thunderscience.changedtsc.config.TSCConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional compatibility with the First Aid mod (mod id "firstaid": ichttt's First Aid and its ports).
 *
 * Nothing here links against First Aid's classes. Items and effects are looked up by id, so the
 * add-on loads and works with or without that mod, and with any of its versions.
 *
 * What it does when First Aid is installed and "firstAidMod.enabled" is on:
 *   - TSC First Aid Kits / Bandages heal through the normal heal() call, which First Aid spreads
 *     over the hurt body parts (or, with "healthOnlyFromFirstAidMod", they only treat goo);
 *   - First Aid's bandages, plasters and morphine are added to TSC chests, scientist and boss drops;
 *   - First Aid's morphine effect holds the goo infection back while it lasts.
 */
public final class FirstAidCompat {
    public static final String MODID = "firstaid";

    private FirstAidCompat() {}

    public static boolean isLoaded() {
        ModList list = ModList.get();
        return list != null && list.isLoaded(MODID);
    }

    /** First Aid installed and compatibility switched on. */
    public static boolean active() {
        return isLoaded() && TSCConfig.FA_MOD_COMPAT.get();
    }

    public static boolean healthOnlyFromMod() {
        return active() && TSCConfig.FA_MOD_ONLY.get();
    }

    /** The First Aid mod items named in the config that really exist. */
    public static List<Item> lootItems() {
        List<Item> out = new ArrayList<>();
        if (!isLoaded()) return out;
        for (String s : TSCConfig.FA_MOD_LOOT_ITEMS.get()) {
            ResourceLocation id = ResourceLocation.tryParse(s.trim());
            if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) continue;
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item != null) out.add(item);
        }
        return out;
    }

    /** One random First Aid mod item for a mob drop, or an empty stack. */
    public static ItemStack randomDrop(RandomSource random) {
        if (!active() || !TSCConfig.FA_MOD_LOOT.get()) return ItemStack.EMPTY;
        List<Item> items = lootItems();
        if (items.isEmpty()) return ItemStack.EMPTY;
        return new ItemStack(items.get(random.nextInt(items.size())), 1 + random.nextInt(2));
    }

    /** Adds an extra pool with First Aid mod items to a TSC chest loot table. */
    public static void addToChestLoot(LootTable table) {
        if (!active() || !TSCConfig.FA_MOD_LOOT.get()) return;
        List<Item> items = lootItems();
        if (items.isEmpty()) return;
        LootPool.Builder pool = LootPool.lootPool()
                .name("changed_tsc_firstaid")
                .setRolls(UniformGenerator.between(0.0F, 2.0F));
        for (Item item : items) {
            pool.add(LootItem.lootTableItem(item)
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))));
        }
        table.addPool(pool.build());
    }

    /** True while the entity is under a First Aid mod effect with "morphine" in its id. */
    public static boolean morphineActive(LivingEntity e) {
        if (!active() || !TSCConfig.FA_MOD_MORPHINE.get()) return false;
        for (MobEffectInstance inst : e.getActiveEffects()) {
            ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(inst.getEffect());
            if (id != null && MODID.equals(id.getNamespace()) && id.getPath().contains("morphine")) return true;
        }
        return false;
    }
}
