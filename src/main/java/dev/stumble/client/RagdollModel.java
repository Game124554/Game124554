package dev.stumble.client;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The vanilla humanoid cut into ten pieces: head, torso, and every limb split at the elbow or knee.
 * Each piece keeps the vanilla texture coordinates, so any skin or mob texture fits unchanged.
 * Index 0 of the limb arrays is the right side, 1 the left.
 */
final class RagdollModel {
    private static final Map<BodyType, RagdollModel> CACHE = new EnumMap<>(BodyType.class);

    final ModelPart head, torso;
    final ModelPart[] upperArm = new ModelPart[2], lowerArm = new ModelPart[2];
    final ModelPart[] thigh = new ModelPart[2], shin = new ModelPart[2];

    static RagdollModel get(BodyType type) {
        return CACHE.computeIfAbsent(type, RagdollModel::new);
    }

    private RagdollModel(BodyType t) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation outer = new CubeDeformation(0.25f);

        // head and hat layer, centred on the head particle
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -4, -4, 8, 8, 8)
                .texOffs(32, 0).addBox(-4, -4, -4, 8, 8, 8, new CubeDeformation(0.5f)), PartPose.ZERO);

        // torso, centred on the middle of the shoulder and hip joints (one pixel below its own centre)
        CubeListBuilder body = CubeListBuilder.create().texOffs(16, 16).addBox(-4, -7, -2, 8, 12, 4);
        if (t.player) {
            body.texOffs(16, 32).addBox(-4, -7, -2, 8, 12, 4, outer);
        }
        root.addOrReplaceChild("torso", body, PartPose.ZERO);

        float limb = t.thinLimbs ? 2 : 4;
        float armW = t.thinLimbs ? 2 : (t.slimArms ? 3 : 4);
        float z = -limb / 2;
        // arms hang from the shoulder joint, which sits 2 px below the top of the arm
        float rightArmX = t.thinLimbs ? -1 : (t.slimArms ? -2 : -3);
        float leftArmX = -1;
        float legX = -limb / 2;

        split(root, "arm_r", 40, 16, t.player ? 40 : -1, 32, false, rightArmX, -2, z, armW, limb, outer);
        split(root, "leg_r", 0, 16, t.player ? 0 : -1, 32, false, legX, 0, z, limb, limb, outer);
        if (t.player) {
            split(root, "arm_l", 32, 48, 48, 48, false, leftArmX, -2, z, armW, limb, outer);
            split(root, "leg_l", 16, 48, 0, 48, false, legX, 0, z, limb, limb, outer);
        } else {
            split(root, "arm_l", 40, 16, -1, 0, true, leftArmX, -2, z, armW, limb, outer);
            split(root, "leg_l", 0, 16, -1, 0, true, legX, 0, z, limb, limb, outer);
        }

        ModelPart baked = LayerDefinition.create(mesh, t.texWidth, t.texHeight).bakeRoot();
        head = baked.getChild("head");
        torso = baked.getChild("torso");
        upperArm[0] = baked.getChild("arm_r_upper");
        lowerArm[0] = baked.getChild("arm_r_lower");
        upperArm[1] = baked.getChild("arm_l_upper");
        lowerArm[1] = baked.getChild("arm_l_lower");
        thigh[0] = baked.getChild("leg_r_upper");
        shin[0] = baked.getChild("leg_r_lower");
        thigh[1] = baked.getChild("leg_l_upper");
        shin[1] = baked.getChild("leg_l_lower");
    }

    /**
     * Cut a 12 px vanilla limb into two 6 px halves. The upper half keeps the limb's texture origin;
     * the lower half starts 6 texels further down, which lands its sides on the lower half of the skin.
     *
     * @param ou,ov texture origin of the outer skin layer, or ou = -1 for none
     * @param top   where the limb's top sits relative to its joint (-2 for arms, 0 for legs)
     */
    private static void split(PartDefinition root, String name, int u, int v, int ou, int ov, boolean mirror,
                              float x, float top, float z, float w, float d, CubeDeformation outer) {
        CubeListBuilder upper = CubeListBuilder.create().mirror(mirror).texOffs(u, v).addBox(x, top, z, w, 6, d);
        CubeListBuilder lower = CubeListBuilder.create().mirror(mirror).texOffs(u, v + 6).addBox(x, 0, z, w, 6, d);
        if (ou >= 0) {
            upper.texOffs(ou, ov).addBox(x, top, z, w, 6, d, outer);
            lower.texOffs(ou, ov + 6).addBox(x, 0, z, w, 6, d, outer);
        }
        root.addOrReplaceChild(name + "_upper", upper, PartPose.ZERO);
        root.addOrReplaceChild(name + "_lower", lower, PartPose.ZERO);
    }
}
