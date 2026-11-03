package com.latexdrone.client;

import com.latexdrone.content.LatexForm;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

import java.util.function.Function;

/** Draws and animates ears / snout / horns / tail / fin / wings, tinted to the creature's colours. */
public class LatexPartsFeature<T extends LivingEntity, M extends BipedEntityModel<T>> extends FeatureRenderer<T, M> {
    private final Function<T, PartsSpec> specFn;
    private final ModelPart earL, earR, roundL, roundR, longL, longR, snout, hornL, hornR;
    private final ModelPart tail, tailTip, fin, wingL, wingR;

    public LatexPartsFeature(FeatureRendererContext<T, M> context, EntityRendererFactory.Context ctx, Function<T, PartsSpec> specFn) {
        super(context);
        this.specFn = specFn;
        ModelPart root = ctx.getPart(LatexPartsModel.LAYER);
        ModelPart head = root.getChild("head_parts");
        ModelPart body = root.getChild("body_parts");
        earL = head.getChild("ear_left");
        earR = head.getChild("ear_right");
        roundL = head.getChild("ear_round_left");
        roundR = head.getChild("ear_round_right");
        longL = head.getChild("long_ear_left");
        longR = head.getChild("long_ear_right");
        snout = head.getChild("snout");
        hornL = head.getChild("horn_left");
        hornR = head.getChild("horn_right");
        tail = body.getChild("tail");
        tailTip = tail.getChild("tail_tip");
        fin = body.getChild("fin");
        wingL = body.getChild("wing_left");
        wingR = body.getChild("wing_right");
    }

    private static boolean has(int flags, int f) {
        return (flags & f) != 0;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle,
                       float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (entity.isInvisible()) return;
        PartsSpec spec = specFn.apply(entity);
        if (spec == null || spec.flags() == 0 || spec.grow() <= 0.02f) return;
        int flags = spec.flags();
        float grow = spec.grow();

        // ---- animation ----
        float sway = MathHelper.sin(animationProgress * 0.12f) * 0.25f + MathHelper.sin(limbAngle * 0.6662f) * limbDistance * 0.5f;
        tail.yaw = sway;
        tail.pitch = 0.55f + MathHelper.sin(animationProgress * 0.07f) * 0.08f - limbDistance * 0.3f;
        tailTip.yaw = sway * 0.8f;
        tailTip.pitch = -0.25f;
        float flap = MathHelper.sin(animationProgress * 0.25f) * (entity.isOnGround() ? 0.15f : 0.6f);
        wingL.yaw = 0.6f + flap;
        wingR.yaw = -0.6f - flap;
        float twitch = MathHelper.sin(animationProgress * 0.05f) > 0.95f ? 0.25f : 0f;
        earL.roll = 0.3f + twitch;
        earR.roll = -0.3f - twitch;

        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(FormTextures.PARTS));
        int overlay = LivingEntityRenderer.getOverlay(entity, 0.0f);
        float r1 = ((spec.color() >> 16) & 255) / 255f, g1 = ((spec.color() >> 8) & 255) / 255f, b1 = (spec.color() & 255) / 255f;
        float r2 = ((spec.color2() >> 16) & 255) / 255f, g2 = ((spec.color2() >> 8) & 255) / 255f, b2 = (spec.color2() & 255) / 255f;

        // head-attached parts
        matrices.push();
        this.getContextModel().head.rotate(matrices);
        matrices.scale(grow, grow, grow);
        if (has(flags, LatexForm.EARS)) {
            earL.render(matrices, vc, light, overlay, r1, g1, b1, 1f);
            earR.render(matrices, vc, light, overlay, r1, g1, b1, 1f);
        }
        if (has(flags, LatexForm.EARS_ROUND)) {
            roundL.render(matrices, vc, light, overlay, r1, g1, b1, 1f);
            roundR.render(matrices, vc, light, overlay, r1, g1, b1, 1f);
        }
        if (has(flags, LatexForm.LONG_EARS)) {
            longL.render(matrices, vc, light, overlay, r1, g1, b1, 1f);
            longR.render(matrices, vc, light, overlay, r1, g1, b1, 1f);
        }
        if (has(flags, LatexForm.SNOUT)) snout.render(matrices, vc, light, overlay, r1, g1, b1, 1f);
        if (has(flags, LatexForm.HORNS)) {
            hornL.render(matrices, vc, light, overlay, r2, g2, b2, 1f);
            hornR.render(matrices, vc, light, overlay, r2, g2, b2, 1f);
        }
        matrices.pop();

        // body-attached parts
        matrices.push();
        this.getContextModel().body.rotate(matrices);
        matrices.scale(grow, grow, grow);
        if (has(flags, LatexForm.TAIL)) tail.render(matrices, vc, light, overlay, r1, g1, b1, 1f);
        if (has(flags, LatexForm.FIN)) fin.render(matrices, vc, light, overlay, r1, g1, b1, 1f);
        if (has(flags, LatexForm.WINGS)) {
            wingL.render(matrices, vc, light, overlay, r2, g2, b2, 1f);
            wingR.render(matrices, vc, light, overlay, r2, g2, b2, 1f);
        }
        matrices.pop();
    }
}
