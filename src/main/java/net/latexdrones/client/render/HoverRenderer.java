package net.latexdrones.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.latexdrones.client.model.HoverModel;
import net.latexdrones.client.model.ModModelLayers;
import net.latexdrones.entity.LatexDroneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class HoverRenderer extends MobRenderer<LatexDroneEntity, HoverModel<LatexDroneEntity>> {
    public HoverRenderer(EntityRendererProvider.Context context) {
        super(context, new HoverModel<>(context.bakeLayer(ModModelLayers.HOVER)), 0.4F);
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
