package net.scpfoundation.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
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
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.scpfoundation.item.KeycardItem;

/**
 * Opens every door within two blocks for four seconds when used with a keycard of sufficient clearance.
 */
public class KeycardReaderBlock extends HorizontalFacingBlock {
    private static final int OPEN_TICKS = 80;
    private final int level;

    public KeycardReaderBlock(int level, Settings settings) {
        super(settings);
        this.level = level;
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    public int getLevel() {
        return level;
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
        ItemStack stack = player.getStackInHand(hand);
        int clearance = KeycardItem.levelOf(stack);
        if (clearance <= 0) {
            if (hand == Hand.MAIN_HAND && KeycardItem.levelOf(player.getOffHandStack()) <= 0) {
                if (!world.isClient) {
                    player.sendMessage(Text.translatable("message.scpfoundation.reader_need", level).formatted(Formatting.RED), true);
                    world.playSound(null, pos, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 0.8f, 1.4f);
                }
                return ActionResult.success(world.isClient);
            }
            return ActionResult.PASS;
        }
        if (world.isClient) return ActionResult.SUCCESS;
        if (clearance < level) {
            player.sendMessage(Text.translatable("message.scpfoundation.reader_denied", clearance, level).formatted(Formatting.RED), true);
            world.playSound(null, pos, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 0.8f, 1.4f);
            return ActionResult.CONSUME;
        }
        player.sendMessage(Text.translatable("message.scpfoundation.reader_granted").formatted(Formatting.GREEN), true);
        world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 0.6f, 1.8f);
        setDoors(world, pos, true);
        world.scheduleBlockTick(pos, this, OPEN_TICKS);
        return ActionResult.CONSUME;
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        setDoors(world, pos, false);
    }

    public static void setDoors(World world, BlockPos center, boolean open) {
        for (BlockPos p : BlockPos.iterate(center.add(-2, -2, -2), center.add(2, 1, 2))) {
            BlockState s = world.getBlockState(p);
            if (s.getBlock() instanceof DoorBlock door && s.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER && s.get(DoorBlock.OPEN) != open) {
                door.setOpen(null, world, s, p.toImmutable(), open);
            }
        }
    }
}
