package net.scpfoundation.registry;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.GlassBlock;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Rarity;
import net.scpfoundation.ScpFoundation;
import net.scpfoundation.block.KeycardReaderBlock;
import net.scpfoundation.block.Scp1162Block;
import net.scpfoundation.block.Scp294Block;
import net.scpfoundation.block.Scp330Block;
import net.scpfoundation.block.Scp458Block;
import net.scpfoundation.block.Scp914Block;

import java.util.ArrayList;
import java.util.List;

public final class ModBlocks {
    public static final List<Block> ALL = new ArrayList<>();

    public static final Block REINFORCED_CONCRETE = register("reinforced_concrete",
            new Block(AbstractBlock.Settings.create().mapColor(MapColor.GRAY).strength(25.0f, 1200.0f).requiresTool().sounds(BlockSoundGroup.STONE)), Rarity.COMMON);
    public static final Block FACILITY_WALL = register("facility_wall",
            new Block(AbstractBlock.Settings.create().mapColor(MapColor.WHITE).strength(4.0f, 9.0f).requiresTool().sounds(BlockSoundGroup.METAL)), Rarity.COMMON);
    public static final Block FACILITY_FLOOR = register("facility_floor",
            new Block(AbstractBlock.Settings.create().mapColor(MapColor.LIGHT_GRAY).strength(3.0f, 9.0f).requiresTool().sounds(BlockSoundGroup.STONE)), Rarity.COMMON);
    public static final Block HAZARD_BLOCK = register("hazard_block",
            new Block(AbstractBlock.Settings.create().mapColor(MapColor.YELLOW).strength(4.0f, 9.0f).requiresTool().sounds(BlockSoundGroup.METAL)), Rarity.COMMON);
    public static final Block CONTAINMENT_GLASS = register("containment_glass",
            new GlassBlock(AbstractBlock.Settings.copy(Blocks.GLASS).strength(10.0f, 1200.0f)), Rarity.COMMON);
    public static final Block FOUNDATION_EMBLEM = register("foundation_emblem",
            new Block(AbstractBlock.Settings.create().mapColor(MapColor.WHITE).strength(4.0f, 9.0f).requiresTool().sounds(BlockSoundGroup.METAL)), Rarity.UNCOMMON);
    public static final Block CORRODED_BLOCK = register("corroded_block",
            new Block(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(2.0f, 6.0f).sounds(BlockSoundGroup.MUD)), Rarity.UNCOMMON);

    public static final Block KEYCARD_READER_1 = reader(1);
    public static final Block KEYCARD_READER_2 = reader(2);
    public static final Block KEYCARD_READER_3 = reader(3);
    public static final Block KEYCARD_READER_4 = reader(4);
    public static final Block KEYCARD_READER_5 = reader(5);

    public static final Block SCP_914 = register("scp_914",
            new Scp914Block(AbstractBlock.Settings.create().mapColor(MapColor.ORANGE).strength(-1.0f, 3600000.0f).sounds(BlockSoundGroup.METAL)), Rarity.EPIC);
    public static final Block SCP_294 = register("scp_294",
            new Scp294Block(AbstractBlock.Settings.create().mapColor(MapColor.WHITE).strength(-1.0f, 3600000.0f).sounds(BlockSoundGroup.METAL)), Rarity.EPIC);
    public static final Block SCP_330 = register("scp_330",
            new Scp330Block(AbstractBlock.Settings.create().mapColor(MapColor.WHITE).strength(1.5f, 6.0f).nonOpaque().sounds(BlockSoundGroup.GLASS)), Rarity.EPIC);
    public static final Block SCP_458 = register("scp_458",
            new Scp458Block(AbstractBlock.Settings.create().mapColor(MapColor.BROWN).strength(0.8f).nonOpaque().sounds(BlockSoundGroup.WOOD)), Rarity.EPIC);
    public static final Block SCP_1162 = register("scp_1162",
            new Scp1162Block(AbstractBlock.Settings.create().mapColor(MapColor.GRAY).strength(-1.0f, 3600000.0f).sounds(BlockSoundGroup.STONE)), Rarity.EPIC);

    private ModBlocks() {}

    private static Block reader(int level) {
        return register("keycard_reader_" + level,
                new KeycardReaderBlock(level, AbstractBlock.Settings.create().mapColor(MapColor.GRAY).strength(25.0f, 1200.0f).requiresTool().sounds(BlockSoundGroup.METAL)),
                level >= 4 ? Rarity.RARE : Rarity.UNCOMMON);
    }

    private static Block register(String name, Block block, Rarity rarity) {
        Registry.register(Registries.BLOCK, ScpFoundation.id(name), block);
        Registry.register(Registries.ITEM, ScpFoundation.id(name), new BlockItem(block, new Item.Settings().rarity(rarity)));
        ALL.add(block);
        return block;
    }

    public static void register() {
        // Static initialisation registers everything.
    }
}
