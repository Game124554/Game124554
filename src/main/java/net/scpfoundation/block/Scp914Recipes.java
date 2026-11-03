package net.scpfoundation.block;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.OperatorOnlyBlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.scpfoundation.data.ScpDatabase;
import net.scpfoundation.item.ScpDocumentItem;
import net.scpfoundation.registry.ModItems;

import java.util.List;
import java.util.Optional;

/**
 * SCP-914 refining rules.
 * Rough = two steps down a chain (or rubble), Coarse = one step down, 1:1 = swap for something of the same kind,
 * Fine = one step up, Very Fine = two steps up with a chance of something anomalous.
 */
public final class Scp914Recipes {
    private static List<List<Item>> chains;

    private static final List<TagKey<Item>> SAME_KIND = List.of(
            ItemTags.WOOL, ItemTags.LOGS, ItemTags.PLANKS, ItemTags.FLOWERS, ItemTags.SAPLINGS, ItemTags.WOOL_CARPETS,
            ItemTags.TERRACOTTA, ItemTags.BEDS, ItemTags.BOATS, ItemTags.MUSIC_DISCS, ItemTags.FISHES, ItemTags.LEAVES);

    private Scp914Recipes() {}

    private static List<List<Item>> chains() {
        if (chains == null) {
            chains = List.of(
                    List.of(ModItems.KEYCARD_LEVEL_1, ModItems.KEYCARD_LEVEL_2, ModItems.KEYCARD_LEVEL_3, ModItems.KEYCARD_LEVEL_4, ModItems.KEYCARD_LEVEL_5, ModItems.KEYCARD_O5),
                    List.of(Items.COAL, Items.COPPER_INGOT, Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.NETHERITE_SCRAP),
                    List.of(Items.IRON_NUGGET, Items.IRON_INGOT, Items.IRON_BLOCK),
                    List.of(Items.GOLD_NUGGET, Items.GOLD_INGOT, Items.GOLD_BLOCK),
                    List.of(Items.GRAVEL, Items.COBBLESTONE, Items.STONE, Items.STONE_BRICKS, Items.CHISELED_STONE_BRICKS),
                    List.of(Items.SAND, Items.SANDSTONE, Items.SMOOTH_SANDSTONE, Items.CUT_SANDSTONE),
                    List.of(Items.DIRT, Items.COARSE_DIRT, Items.GRASS_BLOCK, Items.MOSS_BLOCK),
                    List.of(Items.STICK, Items.OAK_PLANKS, Items.OAK_LOG),
                    List.of(Items.ROTTEN_FLESH, Items.LEATHER, Items.RABBIT_HIDE),
                    List.of(Items.WHEAT_SEEDS, Items.WHEAT, Items.BREAD, Items.CAKE),
                    List.of(Items.APPLE, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE),
                    List.of(Items.PAPER, Items.BOOK, Items.WRITABLE_BOOK),
                    List.of(Items.GLASS_BOTTLE, Items.HONEY_BOTTLE, Items.EXPERIENCE_BOTTLE),
                    List.of(Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD),
                    List.of(Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE),
                    List.of(Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE),
                    List.of(Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET),
                    List.of(Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE),
                    List.of(Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS),
                    List.of(Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS),
                    List.of(Items.REDSTONE, Items.GLOWSTONE_DUST, Items.BLAZE_POWDER),
                    List.of(Items.STRING, Items.COBWEB),
                    List.of(Items.BONE_MEAL, Items.BONE, Items.BONE_BLOCK),
                    List.of(Items.ENDER_PEARL, Items.ENDER_EYE));
        }
        return chains;
    }

    public static ItemStack refine(ItemStack input, int setting, World world) {
        Random random = world.getRandom();
        int count = input.getCount();
        Item item = input.getItem();

        if (item == ModItems.SCP_DOCUMENT) {
            if (setting <= 1) return new ItemStack(Items.PAPER, count);
            ItemStack doc = ScpDocumentItem.create(ScpDatabase.MIN + random.nextInt(ScpDatabase.MAX));
            doc.setCount(count);
            return doc;
        }

        if (setting == 4 && random.nextFloat() < 0.12f) {
            return anomaly(count, world);
        }

        for (List<Item> chain : chains()) {
            int idx = chain.indexOf(item);
            if (idx < 0) continue;
            int delta = switch (setting) {
                case 0 -> -2;
                case 1 -> -1;
                case 3 -> 1;
                case 4 -> 2;
                default -> 0;
            };
            if (delta == 0) return swap(input, random);
            int next = idx + delta;
            if (next < 0) return rubble(item, count);
            Item out = chain.get(Math.min(next, chain.size() - 1));
            return new ItemStack(out, Math.min(count, out.getMaxCount()));
        }

        return switch (setting) {
            case 0, 1 -> rubble(item, count);
            case 2 -> swap(input, random);
            default -> input.copy();
        };
    }

    private static ItemStack rubble(Item item, int count) {
        return new ItemStack(item instanceof BlockItem ? Items.GRAVEL : Items.GUNPOWDER, count);
    }

    private static ItemStack swap(ItemStack input, Random random) {
        for (TagKey<Item> tag : SAME_KIND) {
            if (!input.isIn(tag)) continue;
            Optional<RegistryEntry<Item>> pick = Registries.ITEM.getEntryList(tag).flatMap(list -> list.getRandom(random));
            if (pick.isPresent()) {
                Item out = pick.get().value();
                return new ItemStack(out, Math.min(input.getCount(), out.getMaxCount()));
            }
        }
        return input.copy();
    }

    /** A random, survival-legal item: used by Very Fine mishaps and by SCP-1162. */
    public static ItemStack anomaly(int count, World world) {
        Random random = world.getRandom();
        for (int attempt = 0; attempt < 32; attempt++) {
            Optional<RegistryEntry.Reference<Item>> pick = Registries.ITEM.getRandom(random);
            if (pick.isEmpty()) continue;
            Item out = pick.get().value();
            if (out == Items.AIR || out instanceof OperatorOnlyBlockItem || out == Items.BEDROCK || out == Items.BARRIER
                    || out == Items.LIGHT || out == Items.STRUCTURE_VOID || out == Items.DEBUG_STICK || out == Items.KNOWLEDGE_BOOK
                    || out == Items.END_PORTAL_FRAME || out == Items.SPAWNER || out == Items.REINFORCED_DEEPSLATE
                    || !out.isEnabled(world.getEnabledFeatures())) {
                continue;
            }
            return new ItemStack(out, Math.max(1, Math.min(count, out.getMaxCount())));
        }
        return new ItemStack(Items.GRAVEL, count);
    }
}
