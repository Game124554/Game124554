package net.latexdrones.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.latexdrones.client.model.BlobModel;
import net.latexdrones.client.model.ModModelLayers;
import net.latexdrones.entity.LatexDroneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class BlobRenderer extends MobRenderer<LatexDroneEntity, BlobModel<LatexDroneEntity>> {
    public BlobRenderer(EntityRendererProvider.Context context) {
        super(context, new BlobModel<>(context.bakeLayer(ModModelLayers.BLOB)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(LatexDroneEntity entity) {
        return entity.def().mobTexture;
    }

    @Override
    protected void scale(LatexDroneEntity entity, PoseStack poseStack, float partialTick) {
        // squash and stretch around the feet so the blob wobbles like jelly
        float s = entity.def().scale;
        float wobble = Mth.sin((entity.tickCount + partialTick) * 0.25F) * 0.07F;
        poseStack.scale(s * (1.0F - wobble * 0.6F), s * (1.0F + wobble), s * (1.0F - wobble * 0.6F));
    }
}
