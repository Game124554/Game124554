package com.mandelahorror.client;

import com.mandelahorror.MandelaHorror;
import com.mandelahorror.entity.AlternateEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class AlternateModel extends GeoModel<AlternateEntity> {
    private static final ResourceLocation MODEL = MandelaHorror.id("geo/alternate.geo.json");
    private static final ResourceLocation TEXTURE = MandelaHorror.id("textures/entity/alternate.png");
    private static final ResourceLocation ANIMATIONS = MandelaHorror.id("animations/alternate.animation.json");

    @Override
    public ResourceLocation getModelResource(AlternateEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AlternateEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(AlternateEntity animatable) {
        return ANIMATIONS;
    }

    /** The head follows you. (Tilt on Z comes from the animations.) */
    @Override
    public void setCustomAnimations(AlternateEntity animatable, long instanceId, AnimationState<AlternateEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        CoreGeoBone head = this.getAnimationProcessor().getBone("head");
        if (head == null) return;
        EntityModelData data = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (data == null) return;
        head.setRotX(data.headPitch() * Mth.DEG_TO_RAD);
        head.setRotY(data.netHeadYaw() * Mth.DEG_TO_RAD);
    }
}
