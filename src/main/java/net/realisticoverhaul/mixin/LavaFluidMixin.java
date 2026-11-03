package net.realisticoverhaul.mixin;

import net.minecraft.fluid.LavaFluid;
import net.minecraft.world.WorldView;
import net.realisticoverhaul.config.ROConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lava flows fast and far everywhere, like in the Nether. */
@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin {
	@Inject(method = "getFlowSpeed", at = @At("HEAD"), cancellable = true)
	private void realisticoverhaul$flowSpeed(WorldView world, CallbackInfoReturnable<Integer> cir) {
		if (ROConfig.get().fastLava) cir.setReturnValue(4);
	}

	@Inject(method = "getLevelDecreasePerBlock", at = @At("HEAD"), cancellable = true)
	private void realisticoverhaul$levelDecrease(WorldView world, CallbackInfoReturnable<Integer> cir) {
		if (ROConfig.get().fastLava) cir.setReturnValue(1);
	}

	@Inject(method = "getTickRate", at = @At("HEAD"), cancellable = true)
	private void realisticoverhaul$tickRate(WorldView world, CallbackInfoReturnable<Integer> cir) {
		if (ROConfig.get().fastLava) cir.setReturnValue(10);
	}
}
