package net.realisticoverhaul.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.realisticoverhaul.config.ROConfig;
import net.realisticoverhaul.physics.BlockPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** When fire burns a block away, the blocks it held up can fall. */
@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
	@Inject(method = "scheduledTick", at = @At("TAIL"))
	private void realisticoverhaul$afterBurn(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
		if (!ROConfig.get().blockPhysics) return;
		BlockPhysics.queue(world, pos);
		for (Direction d : Direction.values()) {
			BlockPos n = pos.offset(d);
			if (world.getBlockState(n).isOf(Blocks.FIRE)) BlockPhysics.queue(world, n);
		}
	}
}
