package net.realisticoverhaul.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.realisticoverhaul.physics.StepAssist;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Players walk up 1-block steps like slopes (pairs with smooth terrain). */
@Mixin(Entity.class)
public abstract class EntityStepMixin {
	@Inject(method = "getStepHeight", at = @At("RETURN"), cancellable = true)
	private void realisticoverhaul$stepAssist(CallbackInfoReturnable<Float> cir) {
		Entity self = (Entity) (Object) this;
		if (self instanceof PlayerEntity && StepAssist.active(self.getWorld().isClient)) {
			if (cir.getReturnValueF() < 1.0f) cir.setReturnValue(1.0f);
		}
	}
}
