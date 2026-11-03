package net.scpfoundation.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** SCP-1162: reach in holding something and you pull out something else. Reach in empty-handed and get hurt. */
public class Scp1162Block extends HorizontalFacingBlock {
    public Scp1162Block(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (world.isClient) return ActionResult.SUCCESS;
        ItemStack held = player.getMainHandStack();
        if (held.isEmpty()) {
            player.damage(world.getDamageSources().generic(), 4.0f);
            player.sendMessage(Text.translatable("message.scpfoundation.1162_hurt").formatted(Formatting.DARK_RED), true);
            return ActionResult.CONSUME;
        }
        if (!player.getAbilities().creativeMode) held.decrement(1);
        player.getInventory().offerOrDrop(Scp914Recipes.anomaly(1, world));
        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 1.0f, 0.7f);
        player.sendMessage(Text.translatable("message.scpfoundation.1162_trade").formatted(Formatting.GRAY), true);
        return ActionResult.CONSUME;
    }
}
