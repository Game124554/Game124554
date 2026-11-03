package com.latexdrone.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;

/** During the transformation the latex skin fades in over the player's own skin. */
public class TransformOverlayFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    public TransformOverlayFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        ClientLatexData.State s = ClientLatexData.get(player.getUuid());
        if (s == null || s.transformed || s.transformTicks <= 0 || player.isInvisible()) return;
        float p = s.progress(tickDelta);
        // goo creeps in, with a flickering surge near the end
        float alpha = Math.min(1f, p * 1.1f + (p > 0.6f ? 0.15f * (float) Math.sin(animationProgress * 1.3f) : 0f));
        if (alpha <= 0.02f) return;
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(FormTextures.skin(s.pendingForm)));
        this.getContextModel().render(matrices, vc, light, LivingEntityRenderer.getOverlay(player, 0f), 1f, 1f, 1f, Math.max(0f, alpha));
    }
}
