package net.latexdrones.block;

import net.latexdrones.entity.LatexDroneEntity;
import net.latexdrones.infection.InfectionData;
import net.latexdrones.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/** Lab alarm: right-click to sound it - every drone and scientist within 24 blocks comes for you. */
public class AlarmBlock extends Block {
    public AlarmBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            level.playSound(null, pos, ModSounds.ALARM, SoundSource.BLOCKS, 3.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.latexdrones.alarm"), true);
            if (!player.isCreative() && !player.isSpectator()) {
                boolean transformed = InfectionData.isTransformed(player);
                for (LatexDroneEntity drone : level.getEntitiesOfClass(LatexDroneEntity.class, new AABB(pos).inflate(24.0D))) {
                    if (!transformed || drone.def().scientist) {
                        drone.setTarget(player);
                    }
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
