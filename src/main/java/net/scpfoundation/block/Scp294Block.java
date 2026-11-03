package net.scpfoundation.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import net.scpfoundation.item.Scp294CupItem;

/**
 * SCP-294. Use it while holding an iron nugget (the "coin"). A plain nugget buys a random drink;
 * rename the nugget in an anvil ("coffee", "tea", "liquid courage", ...) to order that drink.
 */
public class Scp294Block extends HorizontalFacingBlock {
    public Scp294Block(Settings settings) {
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
        ItemStack coin = player.getMainHandStack();
        if (!coin.isOf(Items.IRON_NUGGET)) {
            player.sendMessage(Text.translatable("message.scpfoundation.294_coin").formatted(Formatting.YELLOW), true);
            return ActionResult.CONSUME;
        }
        Scp294CupItem.Drink drink;
        if (coin.hasCustomName()) {
            drink = Scp294CupItem.Drink.match(coin.getName().getString());
            if (drink == null) {
                player.sendMessage(Text.translatable("message.scpfoundation.294_unknown").formatted(Formatting.RED), true);
                world.playSound(null, pos, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 1.0f, 1.0f);
                return ActionResult.CONSUME;
            }
        } else {
            drink = Scp294CupItem.Drink.random(world.getRandom());
        }
        if (!player.getAbilities().creativeMode) coin.decrement(1);
        player.getInventory().offerOrDrop(Scp294CupItem.create(drink));
        world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 1.0f, 1.0f);
        player.sendMessage(Text.translatable("message.scpfoundation.294_dispensed",
                Text.translatable("drink.scpfoundation." + drink.id)).formatted(Formatting.AQUA), true);
        return ActionResult.CONSUME;
    }
}
