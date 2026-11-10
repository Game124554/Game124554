package net.latexdrones.mixin.client;

import net.latexdrones.client.TransformAnimation;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the "turning into a drone" pose on top of the vanilla player animation. Cosmetic only, hence require = 0. */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void latexdrones$transformPose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                           float netHeadYaw, float headPitch, CallbackInfo ci) {
        TransformAnimation.apply((PlayerModel<?>) (Object) this, entity, ageInTicks);
    }
}
