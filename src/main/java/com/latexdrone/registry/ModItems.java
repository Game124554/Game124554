package com.latexdrone.registry;

import com.latexdrone.LatexDroneMod;
import com.latexdrone.content.FormData;
import com.latexdrone.content.LatexForm;
import com.latexdrone.content.ScientistData;
import com.latexdrone.content.ScientistType;
import com.latexdrone.item.*;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
    public static final List<Item> SPAWN_EGGS = new ArrayList<>();

    // ---- 4 weapons ----
    public static final Item LATEX_BLADE = reg("latex_blade", new LatexBladeItem(new Item.Settings()));
    public static final Item SYRINGE_GUN = reg("syringe_gun", new SyringeGunItem(new Item.Settings().maxDamage(300)));
    public static final Item HYPNO_STAFF = reg("hypno_staff", new HypnoStaffItem(new Item.Settings().maxDamage(200).rarity(Rarity.RARE)));
    public static final Item PURIFIER_CANNON = reg("purifier_cannon", new PurifierCannonItem(new Item.Settings().maxDamage(300).rarity(Rarity.UNCOMMON)));

    // ---- materials & tools ----
    public static final Item LATEX_GOO = reg("latex_goo", new Item(new Item.Settings()));
    public static final Item WHITE_LATEX_GOO = reg("white_latex_goo", new Item(new Item.Settings()));
    public static final Item LATEX_ANTIDOTE = reg("latex_antidote", new AntidoteItem(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON)));
    public static final Item LATEX_INJECTOR = reg("latex_injector", new LatexInjectorItem(new Item.Settings().maxCount(16).rarity(Rarity.RARE)));
    public static final Item SYRINGE_DART = reg("syringe_dart", new Item(new Item.Settings()));
    public static final Item PURIFIER_CHARGE = reg("purifier_charge", new Item(new Item.Settings()));
    public static final Item EMPTY_SYRINGE = reg("empty_syringe", new EmptySyringeItem(new Item.Settings().maxCount(16)));
    public static final Item HYPNO_CRYSTAL_SHARD = reg("hypno_crystal_shard", new Item(new Item.Settings()));

    private static Item reg(String name, Item item) {
        return Registry.register(Registries.ITEM, LatexDroneMod.id(name), item);
    }

    public static void init() {}

    /** Must run after ModEntities.init(). Each egg has its own custom texture, so the vanilla tint is white (no tint). */
    public static void initSpawnEggs() {
        for (int i = 0; i < FormData.FORMS.size(); i++) {
            LatexForm f = FormData.FORMS.get(i);
            SPAWN_EGGS.add(reg(f.id() + "_spawn_egg", new SpawnEggItem(ModEntities.LATEX_TYPES.get(i), 0xFFFFFF, 0xFFFFFF, new Item.Settings())));
        }
        for (int i = 0; i < ScientistData.SCIENTISTS.size(); i++) {
            ScientistType s = ScientistData.SCIENTISTS.get(i);
            SPAWN_EGGS.add(reg(s.id() + "_spawn_egg", new SpawnEggItem(ModEntities.SCIENTIST_TYPES.get(i), 0xFFFFFF, 0xFFFFFF, new Item.Settings())));
        }
    }
}
