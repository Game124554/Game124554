package net.scpfoundation.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** SCP-005: opens any door (including iron doors), trapdoor or fence gate. Keycard readers treat it as O5 clearance. */
public class Scp005Item extends Item {
    public Scp005Item(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock door) {
            if (!world.isClient) {
                door.setOpen(context.getPlayer(), world, state, pos, !state.get(DoorBlock.OPEN));
            }
            return ActionResult.success(world.isClient);
        }
        if (state.contains(Properties.OPEN)) {
            if (!world.isClient) {
                world.setBlockState(pos, state.cycle(Properties.OPEN), Block.NOTIFY_ALL);
                world.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
            }
            return ActionResult.success(world.isClient);
        }
        return ActionResult.PASS;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.scpfoundation.scp_005").formatted(Formatting.GRAY));
    }
}
