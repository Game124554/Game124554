package net.scpfoundation.client.model;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.scpfoundation.entity.Scp096Entity;

/** Humanoid model shared by SCP-173, 096, 049, 049-2 and 106, with a pose per creature. */
public class ScpBipedModel<T extends MobEntity> extends BipedEntityModel<T> {
    public enum Pose { STATUE, SHY, REACH, SHAMBLE, NORMAL }

    private final Pose pose;

    public ScpBipedModel(ModelPart root, Pose pose) {
        super(root);
        this.pose = pose;
    }

    public static TexturedModelData plain() {
        return TexturedModelData.of(BipedEntityModel.getModelData(Dilation.NONE, 0.0f), 64, 64);
    }

    /** SCP-049: adds the plague-doctor beak to the head. */
    public static TexturedModelData withBeak() {
        ModelData data = BipedEntityModel.getModelData(Dilation.NONE, 0.0f);
        data.getRoot().getChild("head").addChild("beak",
                ModelPartBuilder.create().uv(0, 32).cuboid(-1.0f, -4.0f, -8.0f, 2.0f, 2.0f, 4.0f), ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 64);
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        switch (pose) {
            case STATUE -> {
                head.pitch = 0.0f;
                head.yaw = 0.0f;
                body.pitch = 0.0f;
                rightArm.pitch = 0.0f;
                rightArm.yaw = 0.0f;
                rightArm.roll = 0.05f;
                leftArm.pitch = 0.0f;
                leftArm.yaw = 0.0f;
                leftArm.roll = -0.05f;
                rightLeg.pitch = 0.0f;
                leftLeg.pitch = 0.0f;
            }
            case SHY -> {
                int state = entity instanceof Scp096Entity s ? s.getState() : Scp096Entity.CALM;
                if (state == Scp096Entity.TRIGGERED) {
                    // hands clawing at its face
                    rightArm.pitch = -2.6f;
                    rightArm.yaw = -0.35f;
                    leftArm.pitch = -2.6f;
                    leftArm.yaw = 0.35f;
                    head.pitch = 0.4f + MathHelper.sin(animationProgress * 0.9f) * 0.08f;
                } else if (state == Scp096Entity.ENRAGED) {
                    float swing = MathHelper.sin(animationProgress * 0.6f) * 0.25f;
                    rightArm.pitch = -1.45f + swing;
                    leftArm.pitch = -1.45f - swing;
                    head.pitch = -0.2f;
                } else {
                    // hunched and long-armed
                    head.pitch = 0.35f;
                    rightArm.pitch *= 0.4f;
                    leftArm.pitch *= 0.4f;
                    rightArm.roll = 0.12f;
                    leftArm.roll = -0.12f;
                }
            }
            case REACH -> {
                if (entity.isAttacking()) {
                    rightArm.pitch = -1.4f + MathHelper.sin(animationProgress * 0.3f) * 0.1f;
                }
            }
            case SHAMBLE -> {
                float sway = MathHelper.sin(animationProgress * 0.067f) * 0.05f;
                rightArm.pitch = -1.5f + sway;
                leftArm.pitch = -1.5f - sway;
                rightArm.yaw = -0.1f;
                leftArm.yaw = 0.1f;
            }
            default -> {
            }
        }
        hat.copyTransform(head);
    }
}
