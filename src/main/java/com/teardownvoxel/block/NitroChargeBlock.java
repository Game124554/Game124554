package com.teardownvoxel.block;

import com.teardownvoxel.config.TVConfig;
import com.teardownvoxel.explosion.ShockwaveExplosion;
import com.teardownvoxel.world.ServerScheduler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Placeable, shock-sensitive nitroglycerin charge. Detonates (almost) instantly from redstone,
 * flint and steel / fire charges, projectile hits, heavy landings on top of it, and other explosions.
 */
public class NitroChargeBlock extends Block {
    private static final VoxelShape SHAPE = Block.createCuboidShape(3, 0, 3, 13, 9, 13);

    public NitroChargeBlock(Settings settings) {
        super(settings);
    }

    public static void detonate(World world, BlockPos pos, @Nullable Entity cause) {
        if (!(world instanceof ServerWorld sw)) return;
        if (!(world.getBlockState(pos).getBlock() instanceof NitroChargeBlock)) return;
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        ShockwaveExplosion.detonate(sw, Vec3d.ofCenter(pos), TVConfig.get().nitroPower, cause, null);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(state.getBlock()) && world.isReceivingRedstonePower(pos)) {
            detonate(world, pos, null);
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (world.isReceivingRedstonePower(pos)) {
            detonate(world, pos, null);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(Items.FLINT_AND_STEEL) && !stack.isOf(Items.FIRE_CHARGE)) {
            return super.onUse(state, world, pos, player, hand, hit);
        }
        if (!world.isClient) {
            world.playSound(null, pos, SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1.0f, 1.6f);
            ((ServerWorld) world).spawnParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 8, 0.1, 0.1, 0.1, 0.02);
            // Short fuse so the player has a moment to step back.
            ServerScheduler.schedule((ServerWorld) world, 12, w -> detonate(w, pos, player));
            if (!player.isCreative()) {
                if (stack.isOf(Items.FLINT_AND_STEEL)) {
                    stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
                } else {
                    stack.decrement(1);
                }
            }
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
        if (!world.isClient && projectile.getVelocity().lengthSquared() > 0.25) {
            detonate(world, hit.getBlockPos(), projectile.getOwner());
        }
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!world.isClient && fallDistance > 2.5f) {
            detonate(world, pos, entity);
        }
        super.onLandedUpon(world, state, pos, entity, fallDistance);
    }

    @Override
    public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
        if (world instanceof ServerWorld sw) {
            Vec3d c = Vec3d.ofCenter(pos);
            ServerScheduler.schedule(sw, 2 + sw.random.nextInt(4),
                    w -> ShockwaveExplosion.detonate(w, c, TVConfig.get().nitroPower, explosion.getCausingEntity(), null));
        }
    }

    @Override
    public boolean shouldDropItemsOnExplosion(Explosion explosion) {
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        tooltip.add(Text.translatable("block.teardown_voxel.nitro_charge.tooltip").formatted(Formatting.GRAY));
    }
}
