package com.latexdrone.client;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

import java.util.function.Function;

/** Glowing drone visors / eyes / goggles that shine in the dark. */
public class LatexGlowFeature<T extends LivingEntity, M extends BipedEntityModel<T>> extends FeatureRenderer<T, M> {
    private final Function<T, GlowSpec> specFn;

    public LatexGlowFeature(FeatureRendererContext<T, M> context, Function<T, GlowSpec> specFn) {
        super(context);
        this.specFn = specFn;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle,
                       float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (entity.isInvisible()) return;
        GlowSpec spec = specFn.apply(entity);
        if (spec == null || spec.strength() <= 0.02f) return;
        float pulse = spec.strength() * (0.85f + 0.15f * (float) Math.sin(animationProgress * 0.1f));
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEyes(spec.texture()));
        this.getContextModel().render(matrices, vc, 0xF00000, OverlayTexture.DEFAULT_UV, pulse, pulse, pulse, 1.0f);
    }
}
