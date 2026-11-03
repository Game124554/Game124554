package com.latexdrone.client;

import com.latexdrone.content.ScientistType;
import com.latexdrone.entity.EvilScientistEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class ScientistRenderer extends BipedEntityRenderer<EvilScientistEntity, LatexHumanoidModel<EvilScientistEntity>> {

    public ScientistRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new LatexHumanoidModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
        this.addFeature(new LatexPartsFeature<>(this, ctx, e -> {
            ScientistType s = e.getScientist();
            return new PartsSpec(s.parts(), s.fur(), s.fur2(), 1f);
        }));
        this.addFeature(new LatexGlowFeature<>(this, e -> new GlowSpec(FormTextures.scientistGlow(e.getScientist().index()), 1f)));
    }

    @Override
    public Identifier getTexture(EvilScientistEntity entity) {
        return FormTextures.scientistSkin(entity.getScientist().index());
    }

    @Override
    protected void scale(EvilScientistEntity entity, MatrixStack matrices, float tickDelta) {
        matrices.scale(0.9375f, 0.9375f, 0.9375f);
    }
}
