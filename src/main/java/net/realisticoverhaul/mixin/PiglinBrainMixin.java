package net.realisticoverhaul.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PiglinBrain;
import net.realisticoverhaul.hell.PiglinCurse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Piglins see a cursed player as one of their own (same as wearing gold). */
@Mixin(PiglinBrain.class)
public abstract class PiglinBrainMixin {
	@Inject(method = "wearsGoldArmor", at = @At("HEAD"), cancellable = true)
	private static void realisticoverhaul$piglinPlayer(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		if (PiglinCurse.isPiglin(entity)) cir.setReturnValue(true);
	}
}
