package com.mandelahorror.mixin;

import com.mandelahorror.client.ClientHorror;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Camera shake during the self-stab (added right after the vanilla "hurt" camera tilt). */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "bobHurt", at = @At("TAIL"))
    private void mandelahorror$shake(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        float s = ClientHorror.shakeStrength();
        if (s <= 0F) return;
        double t = ClientHorror.ticks + partialTick;
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (Math.sin(t * 2.1) * 5.0 * s)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (Math.cos(t * 4.3) * 1.5 * s)));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (Math.sin(t * 3.7) * 1.5 * s)));
    }
}
