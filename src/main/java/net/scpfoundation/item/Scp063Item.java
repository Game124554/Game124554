package net.scpfoundation.item;

import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** SCP-063: destroys organic matter almost instantly; useless against anything else. */
public class Scp063Item extends Item {
    public Scp063Item(Settings settings) {
        super(settings);
    }

    public static boolean isOrganic(BlockState state) {
        return state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.LEAVES) || state.isIn(BlockTags.WOOL)
                || state.isIn(BlockTags.PLANKS) || state.isIn(BlockTags.WART_BLOCKS) || state.isIn(BlockTags.FLOWERS)
                || state.isIn(BlockTags.CROPS) || state.isIn(BlockTags.SAPLINGS) || state.isIn(BlockTags.WOOL_CARPETS)
                || state.isIn(BlockTags.BEDS) || state.isIn(BlockTags.CORAL_BLOCKS) || state.isIn(BlockTags.MINEABLE_WITH_HOE);
    }

    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        return isOrganic(state) ? 60.0f : 1.0f;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.scpfoundation.scp_063").formatted(Formatting.GRAY));
    }
}
