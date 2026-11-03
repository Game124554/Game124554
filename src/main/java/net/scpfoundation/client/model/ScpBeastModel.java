package net.scpfoundation.client.model;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic animated model for the non-humanoid SCPs. Parts are found by name (generated in ScpModelData):
 * "head" turns, "leg_a*"/"leg_b*" walk in opposite phase, "arm_*" swing, "tail*" sway, "bob_*" bounce.
 */
public class ScpBeastModel<T extends Entity> extends SinglePartEntityModel<T> {
    private final ModelPart root;
    private ModelPart head;
    private final List<ModelPart> legsA = new ArrayList<>();
    private final List<ModelPart> legsB = new ArrayList<>();
    private final List<ModelPart> arms = new ArrayList<>();
    private final List<ModelPart> tails = new ArrayList<>();
    private final List<ModelPart> bobs = new ArrayList<>();
    private final List<float[]> bobBase = new ArrayList<>();
    private float headBasePitch;

    public ScpBeastModel(ModelPart root, String[] partPaths) {
        this.root = root;
        for (String path : partPaths) {
            ModelPart part = root;
            for (String seg : path.split("/")) part = part.getChild(seg);
            String name = path.substring(path.lastIndexOf('/') + 1);
            if (name.equals("head")) {
                head = part;
                headBasePitch = part.pitch;
            } else if (name.startsWith("leg_a")) legsA.add(part);
            else if (name.startsWith("leg_b")) legsB.add(part);
            else if (name.startsWith("arm_")) arms.add(part);
            else if (name.startsWith("tail")) tails.add(part);
            else if (name.startsWith("bob_")) {
                bobs.add(part);
                bobBase.add(new float[]{part.pivotY});
            }
        }
    }

    @Override
    public ModelPart getPart() {
        return root;
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        if (head != null) {
            head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
            head.pitch = headBasePitch + headPitch * MathHelper.RADIANS_PER_DEGREE;
        }
        float swing = MathHelper.cos(limbAngle * 0.6662f) * 1.2f * limbDistance;
        for (ModelPart leg : legsA) leg.pitch = swing;
        for (ModelPart leg : legsB) leg.pitch = -swing;
        for (int i = 0; i < arms.size(); i++) arms.get(i).pitch = (i % 2 == 0 ? swing : -swing) * 0.8f;
        for (int i = 0; i < tails.size(); i++) {
            tails.get(i).yaw = MathHelper.sin(animationProgress * 0.12f - i * 0.6f) * (0.25f + limbDistance * 0.3f);
        }
        for (int i = 0; i < bobs.size(); i++) {
            bobs.get(i).pivotY = bobBase.get(i)[0] - Math.abs(MathHelper.sin(animationProgress * 0.15f + limbAngle * 0.5f)) * (1.0f + limbDistance * 2.0f);
        }
    }
}
