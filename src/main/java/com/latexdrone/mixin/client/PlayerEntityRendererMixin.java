package com.latexdrone.mixin.client;

import com.latexdrone.client.ClientLatexData;
import com.latexdrone.content.LatexForm;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Transformation animation: the body bulges, squashes and shudders while the latex takes over. */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Inject(method = "scale(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;F)V",
            at = @At("TAIL"))
    private void latexdrone$transformWobble(AbstractClientPlayerEntity player, MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        ClientLatexData.State s = ClientLatexData.get(player.getUuid());
        if (s == null) return;
        if (s.transformed) {
            float fs = MathHelper.clamp(LatexForm.get(s.form).scale(), 0.85f, 1.15f);
            matrices.scale(fs, fs, fs);
            return;
        }
        if (s.transformTicks <= 0) return;
        float p = s.progress(tickDelta);
        float t = player.age + tickDelta;
        float amp = 0.12f * MathHelper.sin(p * (float) Math.PI);
        float sq = MathHelper.sin(t * 0.9f) * amp;
        float targetScale = MathHelper.clamp(LatexForm.get(s.pendingForm).scale(), 0.85f, 1.15f);
        float base = MathHelper.lerp(p, 1f, targetScale);
        matrices.scale(base * (1f + sq), base * (1f - sq * 0.8f), base * (1f + sq));
        float jitter = 0.02f * p;
        matrices.translate((player.getRandom().nextFloat() - 0.5f) * jitter, 0, (player.getRandom().nextFloat() - 0.5f) * jitter);
    }
}
