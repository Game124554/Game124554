package net.scpfoundation.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
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
import net.scpfoundation.registry.ModItems;

/** SCP-261 "Pan-dimensional Vending": pay an iron nugget, get a snack from who-knows-where. */
public class Scp261Block extends HorizontalFacingBlock {
    public Scp261Block(Settings settings) {
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

    private static Item[] snacks() {
        return new Item[]{Items.COOKIE, Items.PUMPKIN_PIE, Items.GOLDEN_CARROT, Items.BAKED_POTATO, Items.COOKED_SALMON,
                Items.HONEY_BOTTLE, Items.BEETROOT_SOUP, Items.RABBIT_STEW, Items.SWEET_BERRIES, Items.GLOW_BERRIES,
                Items.CHORUS_FRUIT, Items.MELON_SLICE, Items.DRIED_KELP, Items.SPIDER_EYE, Items.ROTTEN_FLESH,
                Items.GOLDEN_APPLE, Items.CAKE, ModItems.SCP_330_CANDY, ModItems.SCP_458_PIZZA, ModItems.SCP_207};
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (world.isClient) return ActionResult.SUCCESS;
        ItemStack coin = player.getMainHandStack();
        if (!coin.isOf(Items.IRON_NUGGET)) {
            player.sendMessage(Text.translatable("message.scpfoundation.261_coin").formatted(Formatting.YELLOW), true);
            return ActionResult.CONSUME;
        }
        if (!player.getAbilities().creativeMode) coin.decrement(1);
        Item[] all = snacks();
        Item snack = all[world.getRandom().nextInt(all.length)];
        player.getInventory().offerOrDrop(new ItemStack(snack));
        world.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 1.0f, 0.8f);
        player.sendMessage(Text.translatable("message.scpfoundation.261_snack", new ItemStack(snack).getName()).formatted(Formatting.AQUA), true);
        return ActionResult.CONSUME;
    }
}
