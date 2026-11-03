package net.realisticoverhaul.world;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.realisticoverhaul.RealisticOverhaul;

public final class ModBlocks {
	public static final Block PUDDLE = new PuddleBlock(AbstractBlock.Settings.create()
			.mapColor(MapColor.WATER_BLUE)
			.replaceable()
			.noCollision()
			.nonOpaque()
			.breakInstantly()
			.dropsNothing()
			.ticksRandomly()
			.sounds(BlockSoundGroup.MUD)
			.pistonBehavior(PistonBehavior.DESTROY));

	private ModBlocks() {}

	public static void register() {
		Registry.register(Registries.BLOCK, new Identifier(RealisticOverhaul.MOD_ID, "puddle"), PUDDLE);
	}
}
