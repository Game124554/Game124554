package com.thunderscience.changedtsc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thunderscience.changedtsc.ChangedTSC;
import com.thunderscience.changedtsc.entity.AbstractTSCHuman;
import com.thunderscience.changedtsc.item.GunItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** Player-shaped renderer; picks one of 20 faces x 2 outfit colors per human type. */
public class TSCHumanRenderer extends HumanoidMobRenderer<AbstractTSCHuman, PlayerModel<AbstractTSCHuman>> {
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

    public TSCHumanRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new GooLayer<>(this));
    }

    /** Guards aim their guns the same way players do. */
    @Override
    public void render(AbstractTSCHuman entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        PlayerModel<AbstractTSCHuman> model = this.getModel();
        HumanoidModel.ArmPose main = poseFor(entity.getMainHandItem());
        HumanoidModel.ArmPose off = main.isTwoHanded() ? HumanoidModel.ArmPose.EMPTY : poseFor(entity.getOffhandItem());
        if (entity.getMainArm() == HumanoidArm.RIGHT) {
            model.rightArmPose = main;
            model.leftArmPose = off;
        } else {
            model.leftArmPose = main;
            model.rightArmPose = off;
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static HumanoidModel.ArmPose poseFor(ItemStack stack) {
        if (stack.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
        if (stack.getItem() instanceof GunItem gun) {
            return gun.kind() == GunItem.Kind.PISTOL ? HumanoidModel.ArmPose.BOW_AND_ARROW : HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        return HumanoidModel.ArmPose.ITEM;
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractTSCHuman entity) {
        String key = entity.textureKind() + "_" + (entity.isDarkOutfit() ? "black" : "white") + "_" + entity.getVariant();
        return TEXTURES.computeIfAbsent(key,
                k -> new ResourceLocation(ChangedTSC.MODID, "textures/entity/human/" + k + ".png"));
    }
}
