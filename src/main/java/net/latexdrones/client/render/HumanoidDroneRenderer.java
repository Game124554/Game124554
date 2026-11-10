package net.latexdrones.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.latexdrones.client.model.AnthroModel;
import net.latexdrones.client.model.ModModelLayers;
import net.latexdrones.entity.LatexDroneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renderer for every two-legged mob (drones, latex animals, scientists). Held weapons come from HumanoidMobRenderer. */
public class HumanoidDroneRenderer extends HumanoidMobRenderer<LatexDroneEntity, AnthroModel<LatexDroneEntity>> {
    public HumanoidDroneRenderer(EntityRendererProvider.Context context) {
        super(context, new AnthroModel<>(context.bakeLayer(ModModelLayers.ANTHRO)), 0.5F);
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
