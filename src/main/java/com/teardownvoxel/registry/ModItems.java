package com.teardownvoxel.registry;

import com.teardownvoxel.TeardownVoxel;
import com.teardownvoxel.item.BlowtorchItem;
import com.teardownvoxel.item.PipeBombItem;
import com.teardownvoxel.item.SledgehammerItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

public final class ModItems {
    public static final Item SLEDGEHAMMER = new SledgehammerItem(new Item.Settings().maxDamage(750).rarity(Rarity.UNCOMMON));
    public static final Item PIPE_BOMB = new PipeBombItem(new Item.Settings().maxCount(16));
    public static final Item BLOWTORCH = new BlowtorchItem(new Item.Settings().maxDamage(400));
    public static final Item NITRO_CHARGE = new BlockItem(ModBlocks.NITRO_CHARGE, new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON));
    public static final Item CHARRED_PLANKS = new BlockItem(ModBlocks.CHARRED_PLANKS, new Item.Settings());
    public static final Item CHARRED_LOG = new BlockItem(ModBlocks.CHARRED_LOG, new Item.Settings());

    public static final ItemGroup GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(SLEDGEHAMMER))
            .displayName(Text.translatable("itemGroup.teardown_voxel"))
            .entries((context, entries) -> {
                entries.add(SLEDGEHAMMER);
                entries.add(BLOWTORCH);
                entries.add(PIPE_BOMB);
                entries.add(NITRO_CHARGE);
                entries.add(CHARRED_PLANKS);
                entries.add(CHARRED_LOG);
            })
            .build();

    public static void register() {
        Registry.register(Registries.ITEM, TeardownVoxel.id("sledgehammer"), SLEDGEHAMMER);
        Registry.register(Registries.ITEM, TeardownVoxel.id("pipe_bomb"), PIPE_BOMB);
        Registry.register(Registries.ITEM, TeardownVoxel.id("blowtorch"), BLOWTORCH);
        Registry.register(Registries.ITEM, TeardownVoxel.id("nitro_charge"), NITRO_CHARGE);
        Registry.register(Registries.ITEM, TeardownVoxel.id("charred_planks"), CHARRED_PLANKS);
        Registry.register(Registries.ITEM, TeardownVoxel.id("charred_log"), CHARRED_LOG);
        Registry.register(Registries.ITEM_GROUP, TeardownVoxel.id("main"), GROUP);
    }

    private ModItems() {
    }
}
