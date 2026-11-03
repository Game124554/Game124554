package net.realisticoverhaul.mixin;

import net.minecraft.block.FireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.realisticoverhaul.physics.BlockPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** When fire burns a block away, the blocks it held up can fall. */
@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
	@Inject(method = "trySpawningFire", at = @At("TAIL"))
	private void realisticoverhaul$afterBurn(World world, BlockPos pos, int spreadFactor, Random random, int currentAge, CallbackInfo ci) {
		if (world instanceof ServerWorld sw && !world.getBlockState(pos).isOpaque()) {
			BlockPhysics.queue(sw, pos);
		}
	}
}
