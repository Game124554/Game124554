package dev.stumble.mixin;

import dev.stumble.common.Downed;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A ragdolled entity is immobile: no AI, no walking, no jumping. Vanilla then still moves it by its
 * velocity, which is how the ragdoll drags the entity along with it.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "isImmobile", at = @At("HEAD"), cancellable = true)
    private void stumble$downed(CallbackInfoReturnable<Boolean> cir) {
        if (Downed.isDown((LivingEntity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
