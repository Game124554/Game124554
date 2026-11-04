package net.scpfoundation.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.scpfoundation.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * SCP-079 "Old AI": a sentient computer on obsolete hardware. When people are around it insults them and plays
 * with the facility's iron doors.
 */
public class Scp079Block extends HorizontalFacingBlock implements BlockEntityProvider {
    private static final int LINES = 6;

    public Scp079Block(Settings settings) {
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

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new Scp079BlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient || type != ModBlockEntities.SCP_079) return null;
        return (w, pos, s, be) -> serverTick(w, pos);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (!world.isClient) speak(world, pos, player);
        return ActionResult.success(world.isClient);
    }

    private static void speak(World world, BlockPos pos, PlayerEntity player) {
        player.sendMessage(Text.literal("SCP-079: ").formatted(Formatting.DARK_GREEN)
                .append(Text.translatable("message.scpfoundation.079." + world.getRandom().nextInt(LINES)).formatted(Formatting.GREEN)), false);
        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.BLOCKS, 0.8f, 0.5f + world.getRandom().nextFloat());
    }

    private static void serverTick(World world, BlockPos pos) {
        if (world.getTime() % 100 != 0 || world.getRandom().nextInt(3) != 0) return;
        List<PlayerEntity> near = world.getEntitiesByClass(PlayerEntity.class, new Box(pos).expand(16.0), p -> !p.isSpectator());
        if (near.isEmpty()) return;
        if (world.getRandom().nextBoolean()) {
            speak(world, pos, near.get(world.getRandom().nextInt(near.size())));
            return;
        }
        List<BlockPos> doors = new ArrayList<>();
        for (BlockPos p : BlockPos.iterate(pos.add(-12, -3, -12), pos.add(12, 3, 12))) {
            BlockState s = world.getBlockState(p);
            if (s.getBlock() instanceof DoorBlock && s.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER) doors.add(p.toImmutable());
        }
        if (doors.isEmpty()) return;
        BlockPos d = doors.get(world.getRandom().nextInt(doors.size()));
        BlockState s = world.getBlockState(d);
        ((DoorBlock) s.getBlock()).setOpen(null, world, s, d, !s.get(DoorBlock.OPEN));
    }
}
