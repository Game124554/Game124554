package dev.stumble.client;

import static dev.stumble.physics.Ragdoll.CHEST;
import static dev.stumble.physics.Ragdoll.COUNT;
import static dev.stumble.physics.Ragdoll.ELB_L;
import static dev.stumble.physics.Ragdoll.ELB_R;
import static dev.stumble.physics.Ragdoll.FOOT_L;
import static dev.stumble.physics.Ragdoll.FOOT_R;
import static dev.stumble.physics.Ragdoll.HAND_L;
import static dev.stumble.physics.Ragdoll.HAND_R;
import static dev.stumble.physics.Ragdoll.HEAD;
import static dev.stumble.physics.Ragdoll.HIP_L;
import static dev.stumble.physics.Ragdoll.HIP_R;
import static dev.stumble.physics.Ragdoll.KNEE_L;
import static dev.stumble.physics.Ragdoll.KNEE_R;
import static dev.stumble.physics.Ragdoll.SH_L;
import static dev.stumble.physics.Ragdoll.SH_R;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.stumble.physics.Ragdoll;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws a body from its particles. Every model piece is hung on a joint and turned so that its
 * local +Y (down the limb, as in vanilla models) points along the bone.
 */
final class RagdollRenderer {
    private static final int[] SH = {SH_R, SH_L}, ELB = {ELB_R, ELB_L}, HAND = {HAND_R, HAND_L},
            HIP = {HIP_R, HIP_L}, KNEE = {KNEE_R, KNEE_L}, FOOT = {FOOT_R, FOOT_L};
    private static final int HURT_OVERLAY = OverlayTexture.pack(OverlayTexture.u(0f), OverlayTexture.v(true));

    // scratch state; rendering is single threaded
    private static final float[] P = new float[COUNT * 3];
    private static final Vector3f LEFT = new Vector3f(), DOWN = new Vector3f(), BACK = new Vector3f(),
            DIR = new Vector3f(), DIR2 = new Vector3f();
    private static final Matrix3f BASIS = new Matrix3f();
    private static final Quaternionf TORSO = new Quaternionf(), Q1 = new Quaternionf(), Q2 = new Quaternionf();

    private RagdollRenderer() {
    }

    static void render(Corpse c, PoseStack pose, MultiBufferSource buffers, Vec3 cam, float partial, ClientLevel level) {
        Ragdoll b = c.body;
        float alpha = c.alpha();
        if (alpha <= 0.01f) {
            return;
        }
        double wx = b.x[CHEST * 3], wy = b.x[CHEST * 3 + 1], wz = b.x[CHEST * 3 + 2];
        double ddx = wx - cam.x, ddy = wy - cam.y, ddz = wz - cam.z;
        if (ddx * ddx + ddy * ddy + ddz * ddz > 128.0 * 128.0) {
            return;
        }

        // interpolate between ticks, camera relative
        for (int i = 0; i < COUNT * 3; i += 3) {
            P[i] = (float) (b.snap[i] + (b.x[i] - b.snap[i]) * partial - cam.x);
            P[i + 1] = (float) (b.snap[i + 1] + (b.x[i + 1] - b.snap[i + 1]) * partial - cam.y);
            P[i + 2] = (float) (b.snap[i + 2] + (b.x[i + 2] - b.snap[i + 2]) * partial - cam.z);
        }

        // torso frame. Model space: +X is the body's left, +Y is down, -Z is the face.
        float smx = (px(SH_R) + px(SH_L)) * 0.5f, smy = (py(SH_R) + py(SH_L)) * 0.5f, smz = (pz(SH_R) + pz(SH_L)) * 0.5f;
        float hmx = (px(HIP_R) + px(HIP_L)) * 0.5f, hmy = (py(HIP_R) + py(HIP_L)) * 0.5f, hmz = (pz(HIP_R) + pz(HIP_L)) * 0.5f;
        LEFT.set(px(SH_L) - px(SH_R), py(SH_L) - py(SH_R), pz(SH_L) - pz(SH_R));
        DOWN.set(hmx - smx, hmy - smy, hmz - smz);
        if (LEFT.lengthSquared() < 1e-10f || DOWN.lengthSquared() < 1e-10f) {
            return;
        }
        LEFT.normalize();
        DOWN.normalize();
        LEFT.cross(DOWN, BACK);
        if (BACK.lengthSquared() < 1e-10f) {
            return;
        }
        BACK.normalize();
        DOWN.cross(BACK, LEFT).normalize();
        BASIS.set(LEFT.x, LEFT.y, LEFT.z, DOWN.x, DOWN.y, DOWN.z, BACK.x, BACK.y, BACK.z);
        TORSO.setFromNormalized(BASIS);

        int light = LevelRenderer.getLightColor(level, BlockPos.containing(wx, wy + 0.3, wz));
        int overlay = c.age < 8 ? HURT_OVERLAY : OverlayTexture.NO_OVERLAY;
        RenderType type = c.type.player || alpha < 1f
                ? RenderType.entityTranslucent(c.texture)
                : RenderType.entityCutoutNoCull(c.texture);
        VertexConsumer vc = buffers.getBuffer(type);
        RagdollModel model = RagdollModel.get(c.type);
        float s = c.scale;

        draw(pose, model.torso, (smx + hmx) * 0.5f, (smy + hmy) * 0.5f, (smz + hmz) * 0.5f, TORSO, s, vc, light, overlay, alpha);

        // head: tilts with the neck, keeps facing where the chest faces
        if (aim(DIR, smx - px(HEAD), smy - py(HEAD), smz - pz(HEAD))) {
            Q1.rotationTo(DOWN, DIR).mul(TORSO);
            draw(pose, model.head, px(HEAD), py(HEAD), pz(HEAD), Q1, s, vc, light, overlay, alpha);
        }

        for (int side = 0; side < 2; side++) {
            limb(pose, model.upperArm[side], model.lowerArm[side], SH[side], ELB[side], HAND[side], s, vc, light, overlay, alpha);
            limb(pose, model.thigh[side], model.shin[side], HIP[side], KNEE[side], FOOT[side], s, vc, light, overlay, alpha);
        }
    }

    /** Two-bone limb: each segment takes its parent's orientation, swung the shortest way onto its bone. */
    private static void limb(PoseStack pose, ModelPart upper, ModelPart lower, int a, int b, int c, float s,
                             VertexConsumer vc, int light, int overlay, float alpha) {
        if (!aim(DIR, px(b) - px(a), py(b) - py(a), pz(b) - pz(a))) {
            return;
        }
        Q1.rotationTo(DOWN, DIR).mul(TORSO);
        draw(pose, upper, px(a), py(a), pz(a), Q1, s, vc, light, overlay, alpha);
        if (!aim(DIR2, px(c) - px(b), py(c) - py(b), pz(c) - pz(b))) {
            return;
        }
        Q2.rotationTo(DIR, DIR2).mul(Q1);
        draw(pose, lower, px(b), py(b), pz(b), Q2, s, vc, light, overlay, alpha);
    }

    private static boolean aim(Vector3f out, float x, float y, float z) {
        out.set(x, y, z);
        if (out.lengthSquared() < 1e-10f) {
            return false;
        }
        out.normalize();
        return true;
    }

    private static void draw(PoseStack pose, ModelPart part, float x, float y, float z, Quaternionf rotation, float scale,
                             VertexConsumer vc, int light, int overlay, float alpha) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(rotation);
        pose.scale(scale, scale, scale);
        part.render(pose, vc, light, overlay, 1f, 1f, 1f, alpha);
        pose.popPose();
    }

    private static float px(int i) {
        return P[i * 3];
    }

    private static float py(int i) {
        return P[i * 3 + 1];
    }

    private static float pz(int i) {
        return P[i * 3 + 2];
    }
}
