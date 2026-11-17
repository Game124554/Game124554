package dev.stumble.mixin;

import dev.stumble.client.PushAnimation;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Poses a player's arms for the push. Runs at the end of HumanoidModel.setupAnim; the player
 * model copies the arms onto its sleeve layer after this, so sleeves follow.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void stumble$push(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                              float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player) {
            HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
            PushAnimation.apply(entity, ageInTicks, model.rightArm, model.leftArm);
        }
    }
}
