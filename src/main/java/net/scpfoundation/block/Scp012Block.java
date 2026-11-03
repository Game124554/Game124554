package net.scpfoundation.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.Items;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.scpfoundation.entity.ScpUtil;
import net.scpfoundation.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

/**
 * SCP-012 "A Bad Composition": an unfinished score on a stand. Players nearby are drawn toward it and hurt
 * themselves trying to complete it. Wearing a carved pumpkin (a blindfold) protects you.
 */
public class Scp012Block extends HorizontalFacingBlock implements BlockEntityProvider {
    private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 14, 14);
    private static final double RANGE = 7.0;

    public Scp012Block(Settings settings) {
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
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new Scp012BlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient || type != ModBlockEntities.SCP_012) return null;
        return (w, pos, s, be) -> serverTick(w, pos);
    }

    private static void serverTick(World world, BlockPos pos) {
        if (world.getTime() % 5 != 0) return;
        Vec3d center = Vec3d.ofCenter(pos);
        for (PlayerEntity p : world.getEntitiesByClass(PlayerEntity.class, new Box(pos).expand(RANGE), ScpUtil::isTargetablePlayer)) {
            if (p.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.CARVED_PUMPKIN)) continue;
            Vec3d to = center.subtract(p.getPos());
            double dist = Math.sqrt(to.x * to.x + to.z * to.z);
            if (dist > RANGE) continue;
            if (dist > 1.4) {
                p.addVelocity(to.x / dist * 0.12, 0.0, to.z / dist * 0.12);
                p.velocityModified = true;
                if (world.getTime() % 100 == 0) {
                    p.sendMessage(Text.translatable("message.scpfoundation.012_compelled").formatted(Formatting.DARK_RED, Formatting.ITALIC), true);
                }
            } else if (world.getTime() % 40 == 0) {
                p.damage(world.getDamageSources().magic(), 2.0f);
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0));
                p.sendMessage(Text.translatable("message.scpfoundation.012_writing").formatted(Formatting.DARK_RED), true);
            }
        }
    }
}
