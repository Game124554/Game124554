package net.latexdrones.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.latexdrones.client.model.AnthroModel;
import net.latexdrones.client.model.ModModelLayers;
import net.latexdrones.content.MobDef;
import net.latexdrones.infection.InfectionData;
import net.latexdrones.registry.ModEntities;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * Player render layer that shows the infection on the body:
 * <ul>
 *   <li>while infected the latex skin fades in over the real skin (up to about half visible),</li>
 *   <li>during the transformation it takes over completely,</li>
 *   <li>once transformed it covers the player and adds the form's ears / tail / visor / wings.</li>
 * </ul>
 * The skin itself is also swapped by AbstractClientPlayerMixin (that is what makes the first-person arm match).
 */
public class LatexFormLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final AnthroModel<AbstractClientPlayer> extras;

    public LatexFormLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        this.extras = new AnthroModel<>(models.bakeLayer(ModModelLayers.ANTHRO));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        InfectionData.Entry e = InfectionData.CLIENT.get(player.getUUID());
        if (e == null || player.isInvisible()) {
            return;
        }
        float alpha;
        if (e.transformed) {
            alpha = 1.0F;
        } else if (e.transformTicks > 0) {
            float t = Mth.clamp((e.transformTicks + partialTick) / InfectionData.TRANSFORM_TICKS, 0.0F, 1.0F);
            alpha = 0.55F + 0.45F * Mth.clamp(t * 1.25F, 0.0F, 1.0F);
        } else {
            alpha = 0.55F * e.infection / InfectionData.MAX;
        }
        if (alpha < 0.03F) {
            return;
        }
        MobDef def = ModEntities.form(e.form);
        if (def == null) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = this.getParentModel();
        if (alpha >= 0.999F) {
            VertexConsumer solid = buffer.getBuffer(RenderType.entityCutoutNoCull(def.playerTexture));
            model.renderToBuffer(poseStack, solid, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            VertexConsumer fading = buffer.getBuffer(RenderType.entityTranslucent(def.playerTexture));
            model.renderToBuffer(poseStack, fading, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);
        }
        if (alpha > 0.85F && def.flags != 0) {
            model.copyPropertiesTo(this.extras);
            this.extras.flags = def.flags;
            this.extras.animateExtras(ageInTicks, limbSwingAmount);
            VertexConsumer parts = buffer.getBuffer(RenderType.entityCutoutNoCull(def.formTexture));
            this.extras.renderExtras(poseStack, parts, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
