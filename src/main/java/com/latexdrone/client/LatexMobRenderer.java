package com.latexdrone.client;

import com.latexdrone.content.LatexForm;
import com.latexdrone.entity.LatexMobEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class LatexMobRenderer extends BipedEntityRenderer<LatexMobEntity, LatexHumanoidModel<LatexMobEntity>> {

    public LatexMobRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new LatexHumanoidModel<>(ctx.getPart(EntityModelLayers.PLAYER), true), 0.5f);
        this.addFeature(new LatexPartsFeature<>(this, ctx, e -> {
            LatexForm f = e.getForm();
            return new PartsSpec(f.parts(), f.primary(), f.secondary(), 1f);
        }));
        this.addFeature(new LatexGlowFeature<>(this, e -> new GlowSpec(FormTextures.glow(e.getForm().index()), 1f)));
    }

    @Override
    public Identifier getTexture(LatexMobEntity entity) {
        return FormTextures.skin(entity.getForm().index());
    }

    @Override
    protected void scale(LatexMobEntity entity, MatrixStack matrices, float tickDelta) {
        float s = entity.getForm().scale() * 0.9375f;
        this.shadowRadius = 0.5f * entity.getForm().scale();
        matrices.scale(s, s, s);
    }
}
