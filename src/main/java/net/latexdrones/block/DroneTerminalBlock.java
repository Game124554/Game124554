package net.latexdrones.block;

import net.latexdrones.drone.DroneLife;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Drone Terminal: right-click opens the hive terminal screen (orders, report, refuse, standby). Drones only. */
public class DroneTerminalBlock extends FacingBlock {
    public DroneTerminalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            DroneLife.openTerminal(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
