package com.thunderscience.changedtsc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thunderscience.changedtsc.ChangedTSC;
import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.network.ClientGooData;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Draws goo and fur spreading over a human body, on the outer skin layer (hat / jacket / sleeves / pants):
 * stage 1 = goo creeping up the hands, stage 2 = furry goo over arms, legs and torso, stage 3 = nearly covered.
 */
public class GooLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation[][] TEXTURES = new ResourceLocation[2][4];

    static {
        for (int stage = 1; stage <= 3; stage++) {
            TEXTURES[0][stage] = new ResourceLocation(ChangedTSC.MODID, "textures/entity/goo/goo_black_" + stage + ".png");
            TEXTURES[1][stage] = new ResourceLocation(ChangedTSC.MODID, "textures/entity/goo/goo_white_" + stage + ".png");
        }
    }

    public GooLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible() || !TSCConfig.GOO_BODY_OVERLAY.get()) return;
        int stage = ClientGooData.stage(entity);
        if (stage <= 0) return;
        ResourceLocation texture = TEXTURES[ClientGooData.isWhite(entity) ? 1 : 0][stage];

        M model = this.getParentModel();
        // the goo lives on the outer layer, so make sure it is drawn even if the player hid it in skin settings
        boolean hat = model.hat.visible;
        model.hat.visible = true;
        boolean[] saved = null;
        if (model instanceof PlayerModel<?> pm) {
            saved = new boolean[]{pm.jacket.visible, pm.leftSleeve.visible, pm.rightSleeve.visible,
                    pm.leftPants.visible, pm.rightPants.visible};
            pm.jacket.visible = pm.leftSleeve.visible = pm.rightSleeve.visible = true;
            pm.leftPants.visible = pm.rightPants.visible = true;
        }

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
        model.renderToBuffer(poseStack, consumer, packedLight, LivingEntityRenderer.getOverlayCoords(entity, 0.0F),
                1.0F, 1.0F, 1.0F, 1.0F);

        model.hat.visible = hat;
        if (saved != null && model instanceof PlayerModel<?> pm) {
            pm.jacket.visible = saved[0];
            pm.leftSleeve.visible = saved[1];
            pm.rightSleeve.visible = saved[2];
            pm.leftPants.visible = saved[3];
            pm.rightPants.visible = saved[4];
        }
    }
}
