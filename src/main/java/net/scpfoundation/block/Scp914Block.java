package net.scpfoundation.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** SCP-914. Sneak-use cycles the setting; using it with an item in hand refines the whole stack. */
public class Scp914Block extends HorizontalFacingBlock {
    public static final IntProperty SETTING = IntProperty.of("setting", 0, 4);

    public Scp914Block(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(SETTING, 2));
    }

    @Override
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, SETTING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    public static Text settingName(int setting) {
        return Text.translatable("scp914.setting." + setting);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (world.isClient) return ActionResult.SUCCESS;
        int setting = state.get(SETTING);
        if (player.isSneaking()) {
            int next = (setting + 1) % 5;
            world.setBlockState(pos, state.with(SETTING, next), Block.NOTIFY_ALL);
            world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 1.0f, 0.6f + next * 0.15f);
            player.sendMessage(Text.translatable("message.scpfoundation.914_setting", settingName(next)).formatted(Formatting.GOLD), true);
            return ActionResult.CONSUME;
        }
        ItemStack held = player.getMainHandStack();
        if (held.isEmpty()) {
            player.sendMessage(Text.translatable("message.scpfoundation.914_setting", settingName(setting)).formatted(Formatting.GOLD), true);
            return ActionResult.CONSUME;
        }
        ItemStack result = Scp914Recipes.refine(held, setting, world);
        Text before = held.getName();
        player.setStackInHand(hand, result);
        world.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 1.0f, 0.5f);
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.5f, 0.6f);
        ((ServerWorld) world).spawnParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.02);
        player.sendMessage(Text.translatable("message.scpfoundation.914_refined", settingName(setting), before,
                result.isEmpty() ? Text.literal("-") : result.getName()).formatted(Formatting.GOLD), true);
        return ActionResult.CONSUME;
    }
}
