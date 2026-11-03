package net.scpfoundation.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.scpfoundation.ScpFoundation;
import net.scpfoundation.data.ScpDatabase;
import net.scpfoundation.item.ScpDocumentItem;

public final class ModItemGroups {
    public static final ItemGroup MAIN = Registry.register(Registries.ITEM_GROUP, ScpFoundation.id("main"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModBlocks.FOUNDATION_EMBLEM))
                    .displayName(Text.translatable("itemGroup.scpfoundation.main"))
                    .entries((context, entries) -> {
                        for (Block block : ModBlocks.ALL) entries.add(block);
                        for (Item item : ModItems.ALL) {
                            if (item != ModItems.SCP_DOCUMENT) entries.add(item);
                        }
                        entries.add(new ItemStack(ModItems.SCP_DOCUMENT));
                    })
                    .build());

    /** Every SCP number from 001 to 9999 as a personnel file. */
    public static final ItemGroup ARCHIVE = Registry.register(Registries.ITEM_GROUP, ScpFoundation.id("archive"),
            FabricItemGroup.builder()
                    .icon(() -> ScpDocumentItem.create(173))
                    .displayName(Text.translatable("itemGroup.scpfoundation.archive"))
                    .entries((context, entries) -> {
                        for (int n = ScpDatabase.MIN; n <= ScpDatabase.MAX; n++) {
                            // Only the catalogued files go into the creative search index; the rest stay in this tab.
                            entries.add(ScpDocumentItem.create(n), ScpDatabase.isKnown(n)
                                    ? ItemGroup.StackVisibility.PARENT_AND_SEARCH_TABS
                                    : ItemGroup.StackVisibility.PARENT_TAB_ONLY);
                        }
                    })
                    .build());

    private ModItemGroups() {}

    public static void register() {
        // Static initialisation registers everything.
    }
}
