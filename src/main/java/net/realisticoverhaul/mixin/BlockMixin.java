package net.realisticoverhaul.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.realisticoverhaul.config.ROConfig;
import net.realisticoverhaul.physics.BlockPhysics;
import net.realisticoverhaul.physics.GlassPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Landing hard on glass smashes through it. */
@Mixin(Block.class)
public abstract class BlockMixin {
	@Inject(method = "onLandedUpon", at = @At("HEAD"), cancellable = true)
	private void realisticoverhaul$smashGlass(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance, CallbackInfo ci) {
		ROConfig cfg = ROConfig.get();
		if (world.isClient || !cfg.glassBreaking) return;
		if (fallDistance >= cfg.glassBreakFallDistance && GlassPhysics.isGlass(state)) {
			world.breakBlock(pos, false, entity);
			if (world instanceof ServerWorld sw) BlockPhysics.queue(sw, pos);
			ci.cancel();
		}
	}
}
