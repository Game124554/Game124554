package net.latexdrones.item;

import net.latexdrones.drone.DroneLife;
import net.latexdrones.drone.DroneTask;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Drone Mop: right-click the ground to clean every latex spill (puddles, goo, growths) within two blocks. */
public class MopItem extends Item {
    public MopItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        BlockPos center = context.getClickedPos();
        int cleaned = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 2, 2))) {
            if (DroneLife.isSpill(server.getBlockState(pos).getBlock())) {
                server.destroyBlock(pos.immutable(), false);
                server.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5D, pos.getY() + 0.3D, pos.getZ() + 0.5D, 6, 0.3D, 0.1D, 0.3D, 0.0D);
                cleaned++;
            }
        }
        if (cleaned == 0) {
            return InteractionResult.PASS;
        }
        server.playSound(null, center, SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.6F, 1.4F);
        if (player != null) {
            context.getItemInHand().hurtAndBreak(1, player, p -> p.broadcastBreakEvent(context.getHand()));
            if (player instanceof ServerPlayer serverPlayer) {
                DroneLife.credit(serverPlayer, DroneTask.CLEAN, cleaned);
            }
        }
        return InteractionResult.CONSUME;
    }
}
