package net.latexdrones.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.latexdrones.client.model.BeastModel;
import net.latexdrones.client.model.ModModelLayers;
import net.latexdrones.entity.LatexDroneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BeastRenderer extends MobRenderer<LatexDroneEntity, BeastModel<LatexDroneEntity>> {
    public BeastRenderer(EntityRendererProvider.Context context) {
        super(context, new BeastModel<>(context.bakeLayer(ModModelLayers.BEAST)), 0.55F);
    }

    @Override
    public ResourceLocation getTextureLocation(LatexDroneEntity entity) {
        return entity.def().mobTexture;
    }

    @Override
    protected void scale(LatexDroneEntity entity, PoseStack poseStack, float partialTick) {
        float s = entity.def().scale;
        poseStack.scale(s, s, s);
    }
}
