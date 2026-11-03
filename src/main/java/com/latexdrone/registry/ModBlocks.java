package com.latexdrone.registry;

import com.latexdrone.LatexDroneMod;
import com.latexdrone.block.*;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

import java.util.ArrayList;
import java.util.List;

public final class ModBlocks {
    public static final List<Block> ALL = new ArrayList<>();

    // ---- latex blocks (10) ----
    public static final Block DARK_LATEX_BLOCK = latex("dark_latex_block", MapColor.BLACK);
    public static final Block WHITE_LATEX_BLOCK = latex("white_latex_block", MapColor.WHITE);
    public static final Block PURPLE_LATEX_BLOCK = latex("purple_latex_block", MapColor.PURPLE);
    public static final Block BLUE_LATEX_BLOCK = latex("blue_latex_block", MapColor.BLUE);
    public static final Block RED_LATEX_BLOCK = latex("red_latex_block", MapColor.RED);
    public static final Block GREEN_LATEX_BLOCK = latex("green_latex_block", MapColor.GREEN);
    public static final Block PINK_LATEX_BLOCK = latex("pink_latex_block", MapColor.PINK);
    public static final Block ORANGE_LATEX_BLOCK = latex("orange_latex_block", MapColor.ORANGE);
    public static final Block CYAN_LATEX_BLOCK = latex("cyan_latex_block", MapColor.CYAN);
    public static final Block GOLD_LATEX_BLOCK = latex("gold_latex_block", MapColor.GOLD);

    // ---- hazards (4) ----
    public static final Block DARK_LATEX_PUDDLE = reg("dark_latex_puddle", new LatexPuddleBlock(
            AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(0.2f).sounds(BlockSoundGroup.HONEY).pistonBehavior(PistonBehavior.DESTROY), 0));
    public static final Block WHITE_LATEX_PUDDLE = reg("white_latex_puddle", new LatexPuddleBlock(
            AbstractBlock.Settings.create().mapColor(MapColor.WHITE).strength(0.2f).sounds(BlockSoundGroup.HONEY).pistonBehavior(PistonBehavior.DESTROY), 1));
    public static final Block LATEX_CRYSTAL = reg("latex_crystal", new LatexCrystalBlock(
            AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(0.5f).noCollision().nonOpaque()
                    .sounds(BlockSoundGroup.AMETHYST_CLUSTER).luminance(s -> 3).pistonBehavior(PistonBehavior.DESTROY), 15));
    public static final Block WHITE_LATEX_CRYSTAL = reg("white_latex_crystal", new LatexCrystalBlock(
            AbstractBlock.Settings.create().mapColor(MapColor.WHITE).strength(0.5f).noCollision().nonOpaque()
                    .sounds(BlockSoundGroup.AMETHYST_CLUSTER).luminance(s -> 3).pistonBehavior(PistonBehavior.DESTROY), 16));

    // ---- infected nature (6) ----
    public static final Block INFECTED_DIRT = reg("infected_dirt", new Block(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(0.6f).sounds(BlockSoundGroup.MUD)));
    public static final Block INFECTED_STONE = reg("infected_stone", new Block(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).requiresTool().strength(1.5f, 6f)));
    public static final Block INFECTED_LOG = reg("infected_log", new PillarBlock(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(2f).sounds(BlockSoundGroup.WOOD)));
    public static final Block INFECTED_PLANKS = reg("infected_planks", new Block(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(2f, 3f).sounds(BlockSoundGroup.WOOD)));
    public static final Block LATEX_BRICKS = reg("latex_bricks", new Block(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).requiresTool().strength(2f, 6f)));
    public static final Block CRACKED_LATEX_BRICKS = reg("cracked_latex_bricks", new Block(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).requiresTool().strength(2f, 6f)));

    // ---- laboratory (20) ----
    public static final Block LAB_TILE = metal("lab_tile", MapColor.WHITE);
    public static final Block DARK_LAB_TILE = metal("dark_lab_tile", MapColor.GRAY);
    public static final Block LAB_WALL = metal("lab_wall", MapColor.WHITE);
    public static final Block LAB_WALL_STRIPED = metal("lab_wall_striped", MapColor.WHITE);
    public static final Block LAB_FLOOR = metal("lab_floor", MapColor.LIGHT_GRAY);
    public static final Block LAB_CEILING_LIGHT = reg("lab_ceiling_light", new Block(AbstractBlock.Settings.create().mapColor(MapColor.WHITE).strength(1f).sounds(BlockSoundGroup.GLASS).luminance(s -> 15)));
    public static final Block REINFORCED_LAB_GLASS = reg("reinforced_lab_glass", new GlassBlock(AbstractBlock.Settings.create().strength(3f, 12f).sounds(BlockSoundGroup.GLASS).nonOpaque()
            .allowsSpawning((s, w, p, t) -> false).solidBlock((s, w, p) -> false).suffocates((s, w, p) -> false).blockVision((s, w, p) -> false)));
    public static final Block CONTAINMENT_TANK = reg("containment_tank", new GlassBlock(AbstractBlock.Settings.create().strength(3f, 12f).sounds(BlockSoundGroup.GLASS).nonOpaque().luminance(s -> 7)
            .allowsSpawning((s, w, p, t) -> false).solidBlock((s, w, p) -> false).suffocates((s, w, p) -> false).blockVision((s, w, p) -> false)));
    public static final Block LAB_VENT = metal("lab_vent", MapColor.GRAY);
    public static final Block HAZARD_BLOCK = metal("hazard_block", MapColor.YELLOW);
    public static final Block STEEL_PANEL = metal("steel_panel", MapColor.IRON_GRAY);
    public static final Block RUSTED_STEEL_PANEL = metal("rusted_steel_panel", MapColor.BROWN);
    public static final Block METAL_GRATE = reg("metal_grate", new GlassBlock(AbstractBlock.Settings.create().mapColor(MapColor.IRON_GRAY).strength(3f, 6f).sounds(BlockSoundGroup.METAL).nonOpaque()
            .allowsSpawning((s, w, p, t) -> false).solidBlock((s, w, p) -> false).suffocates((s, w, p) -> false).blockVision((s, w, p) -> false)));
    public static final Block LAB_CRATE = reg("lab_crate", new Block(AbstractBlock.Settings.create().mapColor(MapColor.OAK_TAN).strength(2f).sounds(BlockSoundGroup.WOOD)));
    public static final Block WARNING_LIGHT = reg("warning_light", new Block(AbstractBlock.Settings.create().mapColor(MapColor.RED).strength(1f).sounds(BlockSoundGroup.GLASS).luminance(s -> 12)));
    public static final Block LAB_COMPUTER = reg("lab_computer", new FacingLabBlock(AbstractBlock.Settings.create().mapColor(MapColor.GRAY).strength(2f).sounds(BlockSoundGroup.METAL).luminance(s -> 6)));
    public static final Block SERVER_RACK = reg("server_rack", new FacingLabBlock(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(3f).sounds(BlockSoundGroup.METAL).luminance(s -> 4)));
    public static final Block HYPNO_SCREEN = reg("hypno_screen", new HypnoScreenBlock(AbstractBlock.Settings.create().mapColor(MapColor.PURPLE).strength(2f).sounds(BlockSoundGroup.METAL).luminance(s -> 10)));
    public static final Block DRONE_CONVERSION_POD = reg("drone_conversion_pod", new ConversionPodBlock(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(4f, 12f).sounds(BlockSoundGroup.METAL).luminance(s -> 8), 0));
    public static final Block LATEX_GLOW_LAMP = reg("latex_glow_lamp", new Block(AbstractBlock.Settings.create().mapColor(MapColor.CYAN).strength(1f).sounds(BlockSoundGroup.SLIME).luminance(s -> 15)));

    private static Block latex(String name, MapColor color) {
        return reg(name, new Block(AbstractBlock.Settings.create().mapColor(color).strength(0.8f).sounds(BlockSoundGroup.SLIME)
                .velocityMultiplier(0.85f).jumpVelocityMultiplier(0.9f)));
    }

    private static Block metal(String name, MapColor color) {
        return reg(name, new Block(AbstractBlock.Settings.create().mapColor(color).requiresTool().strength(3f, 6f).sounds(BlockSoundGroup.METAL)));
    }

    private static Block reg(String name, Block block) {
        Registry.register(Registries.BLOCK, LatexDroneMod.id(name), block);
        Registry.register(Registries.ITEM, LatexDroneMod.id(name), new BlockItem(block, new Item.Settings()));
        ALL.add(block);
        return block;
    }

    public static void init() {}
}
