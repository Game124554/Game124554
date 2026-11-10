package net.latexdrones.block;

import net.latexdrones.drone.DroneLife;
import net.latexdrones.infection.InfectionData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Drone Sleep Pod: a glass capsule. A drone right-clicks it to step inside and sleep (standby):
 * the battery refills, health and hunger recover, and the night passes once everybody rests.
 */
public class DronePodBlock extends Block {
    public DronePodBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** No collision, so the sleeping drone can stand inside the capsule. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (InfectionData.isTransformed(serverPlayer)) {
                // stand in the lower pod block
                BlockPos floor = level.getBlockState(pos.below()).is(this) ? pos.below() : pos;
                DroneLife.enterStandbyAt(serverPlayer, floor);
            } else {
                serverPlayer.displayClientMessage(Component.translatable("message.latexdrones.access_denied"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
