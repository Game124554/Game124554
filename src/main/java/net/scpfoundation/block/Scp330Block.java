package net.scpfoundation.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.scpfoundation.registry.ModItems;
import org.jetbrains.annotations.Nullable;

/** SCP-330: everyone may take two pieces of candy. Take a third and you lose the use of your hands. */
public class Scp330Block extends Block implements BlockEntityProvider {
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(6, 0, 6, 10, 9, 10),
            Block.createCuboidShape(3, 9, 3, 13, 13, 13));

    public Scp330Block(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new Scp330BlockEntity(pos, state);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(world.getBlockEntity(pos) instanceof Scp330BlockEntity bowl)) return ActionResult.PASS;
        int taken = bowl.take(player.getUuid());
        if (taken <= 2) {
            player.getInventory().offerOrDrop(new ItemStack(ModItems.SCP_330_CANDY));
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.6f, 1.4f);
            player.sendMessage(Text.translatable("message.scpfoundation.330_ok", taken).formatted(Formatting.LIGHT_PURPLE), true);
        } else {
            player.damage(world.getDamageSources().magic(), 8.0f);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 300, 4));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20 * 300, 3));
            player.dropItem(player.getMainHandStack().copy(), true);
            player.getMainHandStack().setCount(0);
            world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.PLAYERS, 1.0f, 0.5f);
            ((ServerWorld) world).spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, net.minecraft.block.Blocks.REDSTONE_BLOCK.getDefaultState()),
                    player.getX(), player.getBodyY(0.5), player.getZ(), 30, 0.3, 0.3, 0.3, 0.1);
            player.sendMessage(Text.translatable("message.scpfoundation.330_punish").formatted(Formatting.DARK_RED, Formatting.BOLD), true);
        }
        return ActionResult.CONSUME;
    }
}
