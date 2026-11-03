package com.latexdrone.registry;

import com.latexdrone.LatexDroneMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModItemGroup {
    public static ItemGroup MAIN;

    public static void init() {
        MAIN = Registry.register(Registries.ITEM_GROUP, LatexDroneMod.id("main"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModItems.LATEX_INJECTOR))
                .displayName(Text.translatable("itemGroup.latexdrone.main"))
                .entries((context, entries) -> {
                    entries.add(ModItems.LATEX_BLADE);
                    entries.add(ModItems.SYRINGE_GUN);
                    entries.add(ModItems.HYPNO_STAFF);
                    entries.add(ModItems.PURIFIER_CANNON);
                    entries.add(ModItems.LATEX_ANTIDOTE);
                    entries.add(ModItems.LATEX_INJECTOR);
                    entries.add(ModItems.EMPTY_SYRINGE);
                    entries.add(ModItems.SYRINGE_DART);
                    entries.add(ModItems.PURIFIER_CHARGE);
                    entries.add(ModItems.LATEX_GOO);
                    entries.add(ModItems.WHITE_LATEX_GOO);
                    entries.add(ModItems.HYPNO_CRYSTAL_SHARD);
                    for (Item egg : ModItems.SPAWN_EGGS) entries.add(egg);
                    for (Block block : ModBlocks.ALL) entries.add(block);
                })
                .build());
    }
}
