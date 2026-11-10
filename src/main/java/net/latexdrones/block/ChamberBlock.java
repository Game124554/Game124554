package net.latexdrones.block;

import net.latexdrones.infection.InfectionData;
import net.latexdrones.infection.InfectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Conversion chamber: right-click it and the transformation starts at once; standing on it infects quickly. */
public class ChamberBlock extends Block {
    public ChamberBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && !InfectionData.isTransformed(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable("message.latexdrones.chamber"), true);
            InfectionManager.forceTransform(serverPlayer, "");
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        InfectionManager.touch(entity, level, 150);
        super.stepOn(level, pos, state, entity);
    }
}
