package com.latexdrone.client;

import com.latexdrone.entity.EvilScientistEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Player-shaped model for latex creatures and scientists.
 * Latex creatures reach forward with both arms when hunting (zombie-style lunge);
 * scientists aim their syringe guns; everything wobbles a little like goo.
 */
public class LatexHumanoidModel<T extends LivingEntity> extends PlayerEntityModel<T> {
    private final boolean zombieArms;

    public LatexHumanoidModel(ModelPart root, boolean zombieArms) {
        super(root, false);
        this.zombieArms = zombieArms;
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        if (!zombieArms && entity instanceof MobEntity mob) {
            boolean ranged = entity instanceof EvilScientistEntity sci && sci.getScientist().ranged();
            boolean hasItem = !entity.getMainHandStack().isEmpty();
            this.rightArmPose = (mob.isAttacking() && ranged) ? BipedEntityModel.ArmPose.BOW_AND_ARROW
                    : (hasItem ? BipedEntityModel.ArmPose.ITEM : BipedEntityModel.ArmPose.EMPTY);
            this.leftArmPose = BipedEntityModel.ArmPose.EMPTY;
        }
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);

        if (zombieArms && entity instanceof MobEntity mob && mob.isAttacking()) {
            float f = MathHelper.sin(this.handSwingProgress * (float) Math.PI);
            float g = MathHelper.sin((1.0f - (1.0f - this.handSwingProgress) * (1.0f - this.handSwingProgress)) * (float) Math.PI);
            this.rightArm.roll = 0.0f;
            this.leftArm.roll = 0.0f;
            this.rightArm.yaw = -(0.1f - f * 0.6f);
            this.leftArm.yaw = 0.1f - f * 0.6f;
            float reach = -(float) Math.PI / 2.25f;
            this.rightArm.pitch = reach + f * 1.2f - g * 0.4f;
            this.leftArm.pitch = reach + f * 1.2f - g * 0.4f;
            this.rightArm.roll += MathHelper.cos(animationProgress * 0.09f) * 0.05f + 0.05f;
            this.leftArm.roll -= MathHelper.cos(animationProgress * 0.09f) * 0.05f + 0.05f;
            this.rightArm.pitch += MathHelper.sin(animationProgress * 0.067f) * 0.05f;
            this.leftArm.pitch -= MathHelper.sin(animationProgress * 0.067f) * 0.05f;
            this.rightSleeve.copyTransform(this.rightArm);
            this.leftSleeve.copyTransform(this.leftArm);
        }

        if (zombieArms) {
            // gooey idle wobble
            this.head.roll += MathHelper.sin(animationProgress * 0.08f) * 0.05f;
            this.hat.copyTransform(this.head);
        }
    }
}
