package com.teardownvoxel.explosion;

import com.teardownvoxel.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

/**
 * Scorch rules for blast-adjacent blocks: stone turns to cobble / blackstone, bricks crack,
 * wood chars (or carbonises to coal), soil burns, foliage and wool burn away.
 */
public final class Charring {
    private Charring() {
    }

    /** @return the scorched state, or null if the block is unaffected. AIR means "burned away". */
    @Nullable
    public static BlockState charred(BlockState s, Random r) {
        Block b = s.getBlock();

        if (b == Blocks.STONE || b == Blocks.SMOOTH_STONE) return r.nextFloat() < 0.65f ? Blocks.COBBLESTONE.getDefaultState() : Blocks.BLACKSTONE.getDefaultState();
        if (b == Blocks.ANDESITE || b == Blocks.DIORITE || b == Blocks.GRANITE || b == Blocks.TUFF)
            return r.nextFloat() < 0.5f ? Blocks.COBBLESTONE.getDefaultState() : Blocks.BLACKSTONE.getDefaultState();
        if (b == Blocks.COBBLESTONE || b == Blocks.MOSSY_COBBLESTONE) return r.nextFloat() < 0.5f ? Blocks.BLACKSTONE.getDefaultState() : null;
        if (b == Blocks.STONE_BRICKS || b == Blocks.MOSSY_STONE_BRICKS) return Blocks.CRACKED_STONE_BRICKS.getDefaultState();
        if (b == Blocks.DEEPSLATE) return Blocks.COBBLED_DEEPSLATE.getDefaultState();
        if (b == Blocks.DEEPSLATE_BRICKS) return Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState();
        if (b == Blocks.DEEPSLATE_TILES) return Blocks.CRACKED_DEEPSLATE_TILES.getDefaultState();
        if (b == Blocks.NETHER_BRICKS) return Blocks.CRACKED_NETHER_BRICKS.getDefaultState();
        if (b == Blocks.POLISHED_BLACKSTONE_BRICKS) return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        if (b == Blocks.SANDSTONE || b == Blocks.CUT_SANDSTONE) return Blocks.SMOOTH_SANDSTONE.getDefaultState();
        if (b == Blocks.GRASS_BLOCK || b == Blocks.PODZOL || b == Blocks.MYCELIUM)
            return r.nextFloat() < 0.6f ? Blocks.DIRT.getDefaultState() : Blocks.COARSE_DIRT.getDefaultState();
        if (b == Blocks.DIRT) return r.nextFloat() < 0.5f ? Blocks.COARSE_DIRT.getDefaultState() : null;
        if (b == Blocks.SAND && r.nextFloat() < 0.08f) return Blocks.GLASS.getDefaultState();

        if (b == ModBlocks.CHARRED_LOG || b == ModBlocks.CHARRED_PLANKS) return r.nextFloat() < 0.15f ? Blocks.AIR.getDefaultState() : null;
        if (s.isIn(BlockTags.LOGS)) {
            if (r.nextFloat() < 0.15f) return Blocks.COAL_BLOCK.getDefaultState();
            BlockState charred = ModBlocks.CHARRED_LOG.getDefaultState();
            if (s.contains(Properties.AXIS)) charred = charred.with(Properties.AXIS, s.get(Properties.AXIS));
            return charred;
        }
        if (s.isIn(BlockTags.PLANKS)) return ModBlocks.CHARRED_PLANKS.getDefaultState();
        if (s.isIn(BlockTags.LEAVES) || s.isIn(BlockTags.WOOL) || s.isIn(BlockTags.WOOL_CARPETS) || s.isReplaceable())
            return Blocks.AIR.getDefaultState();
        return null;
    }
}
