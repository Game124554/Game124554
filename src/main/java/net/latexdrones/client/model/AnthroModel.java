package net.latexdrones.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.latexdrones.content.MobDef;
import net.latexdrones.entity.LatexDroneEntity;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Biped model for drones, latex animals and scientists: the vanilla humanoid plus optional extras
 * (ears, horns, snout, beak, visor, antenna, fin, tails, wings) switched on by {@link MobDef#flags}.
 *
 * <p>The extras are separate root parts that are drawn by hand in the head's / body's coordinate system,
 * so the same model can also draw just the extras on top of a transformed player (LatexFormLayer).
 * Texture layout: classic 64x32 biped UVs on a 64x64 sheet, extras in the lower half
 * (see paint_extras in tools/gen_textures.py - the UV offsets below must match it).
 */
public class AnthroModel<T extends LivingEntity> extends HumanoidModel<T> {
    private final ModelPart earsPointy;
    private final ModelPart earsLong;
    private final ModelPart earsRound;
    private final ModelPart horns;
    private final ModelPart snout;
    private final ModelPart beak;
    private final ModelPart visor;
    private final ModelPart antenna;
    private final ModelPart fin;
    private final ModelPart tailBushy;
    private final ModelPart tailThin;
    private final ModelPart tailShort;
    private final ModelPart tailThick;
    private final ModelPart wingRight;
    private final ModelPart wingLeft;

    /** Which extras to draw - see the constants in {@link MobDef}. */
    public int flags;

    public AnthroModel(ModelPart root) {
        super(root);
        this.earsPointy = root.getChild("ears_pointy");
        this.earsLong = root.getChild("ears_long");
        this.earsRound = root.getChild("ears_round");
        this.horns = root.getChild("horns");
        this.snout = root.getChild("snout");
        this.beak = root.getChild("beak");
        this.visor = root.getChild("visor");
        this.antenna = root.getChild("antenna");
        this.fin = root.getChild("fin");
        this.tailBushy = root.getChild("tail_bushy");
        this.tailThin = root.getChild("tail_thin");
        this.tailShort = root.getChild("tail_short");
        this.tailThick = root.getChild("tail_thick");
        this.wingRight = root.getChild("wing_right");
        this.wingLeft = root.getChild("wing_left");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        // ---- attached to the head (coordinates relative to the head pivot)
        root.addOrReplaceChild("ears_pointy", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-4.0F, -11.0F, -1.0F, 3.0F, 3.0F, 1.0F)
                .texOffs(8, 32).addBox(1.0F, -11.0F, -1.0F, 3.0F, 3.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("ears_long", CubeListBuilder.create()
                .texOffs(16, 32).addBox(-3.5F, -15.0F, -0.5F, 2.0F, 7.0F, 1.0F)
                .texOffs(22, 32).addBox(1.5F, -15.0F, -0.5F, 2.0F, 7.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("ears_round", CubeListBuilder.create()
                .texOffs(0, 36).addBox(-5.0F, -10.0F, -1.0F, 3.0F, 3.0F, 1.0F)
                .texOffs(8, 36).addBox(2.0F, -10.0F, -1.0F, 3.0F, 3.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("horns", CubeListBuilder.create()
                .texOffs(28, 32).addBox(-3.5F, -11.0F, -2.5F, 1.0F, 3.0F, 1.0F)
                .texOffs(32, 32).addBox(2.5F, -11.0F, -2.5F, 1.0F, 3.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("snout", CubeListBuilder.create()
                .texOffs(36, 32).addBox(-2.0F, -4.0F, -6.0F, 4.0F, 3.0F, 2.0F), PartPose.ZERO);
        root.addOrReplaceChild("beak", CubeListBuilder.create()
                .texOffs(48, 32).addBox(-1.0F, -4.0F, -7.0F, 2.0F, 2.0F, 3.0F), PartPose.ZERO);
        root.addOrReplaceChild("visor", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-4.5F, -6.0F, -5.0F, 9.0F, 3.0F, 2.0F), PartPose.ZERO);
        root.addOrReplaceChild("antenna", CubeListBuilder.create()
                .texOffs(58, 32).addBox(-0.5F, -12.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("fin", CubeListBuilder.create()
                .texOffs(22, 40).addBox(-0.5F, -11.0F, -2.0F, 1.0F, 3.0F, 5.0F), PartPose.ZERO);
        // ---- attached to the body (coordinates relative to the body pivot, tails hinge at the lower back)
        root.addOrReplaceChild("tail_bushy", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 8.0F), PartPose.offset(0.0F, 10.0F, 2.0F));
        root.addOrReplaceChild("tail_thin", CubeListBuilder.create()
                .texOffs(44, 37).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 9.0F), PartPose.offset(0.0F, 10.5F, 2.0F));
        root.addOrReplaceChild("tail_short", CubeListBuilder.create()
                .texOffs(34, 40).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 2.0F), PartPose.offset(0.0F, 10.0F, 2.0F));
        root.addOrReplaceChild("tail_thick", CubeListBuilder.create()
                .texOffs(22, 48).addBox(-2.0F, -1.5F, 0.0F, 4.0F, 3.0F, 9.0F), PartPose.offset(0.0F, 10.0F, 2.0F));
        root.addOrReplaceChild("wing_right", CubeListBuilder.create()
                .texOffs(48, 48).addBox(-6.0F, 0.0F, 0.0F, 6.0F, 8.0F, 1.0F), PartPose.offset(-1.0F, 1.0F, 2.0F));
        root.addOrReplaceChild("wing_left", CubeListBuilder.create()
                .texOffs(48, 48).mirror().addBox(0.0F, 0.0F, 0.0F, 6.0F, 8.0F, 1.0F), PartPose.offset(1.0F, 1.0F, 2.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity instanceof LatexDroneEntity drone) {
            MobDef def = drone.def();
            this.flags = def.flags;
            if (drone.isAggressive()) {
                if (drone.getMainHandItem().isEmpty()) {
                    // grasping arms, like a zombie
                    AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, true, this.attackTime, ageInTicks);
                } else if (def.ranged) {
                    // level the weapon at the target
                    this.rightArm.xRot = -1.4F + this.head.xRot;
                    this.rightArm.yRot = this.head.yRot * 0.6F - 0.1F;
                }
            }
        }
        this.animateExtras(ageInTicks, limbSwingAmount);
    }

    /** Idle motion of tails and wings. */
    public void animateExtras(float ageInTicks, float limbSwingAmount) {
        float wag = Mth.cos(ageInTicks * 0.15F) * 0.25F;
        float lift = limbSwingAmount * 0.35F;
        this.tailBushy.xRot = -0.55F + lift;
        this.tailBushy.yRot = wag;
        this.tailThin.xRot = -0.75F + lift;
        this.tailThin.yRot = wag * 1.6F;
        this.tailShort.xRot = 0.2F;
        this.tailShort.yRot = 0.0F;
        this.tailThick.xRot = -0.45F + lift * 0.5F;
        this.tailThick.yRot = wag * 0.6F;
        float flap = Mth.sin(ageInTicks * 0.2F) * 0.18F;
        this.wingRight.yRot = 0.6F + flap;
        this.wingLeft.yRot = -0.6F - flap;
        this.wingRight.xRot = 0.1F;
        this.wingLeft.xRot = 0.1F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.renderExtras(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    /** Draws only the extras selected by {@link #flags}, following the current head and body pose. */
    public void renderExtras(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                             float red, float green, float blue, float alpha) {
        int f = this.flags;
        if (f == 0) {
            return;
        }
        poseStack.pushPose();
        this.head.translateAndRotate(poseStack);
        draw(this.earsPointy, f, MobDef.EARS_POINTY, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        draw(this.earsLong, f, MobDef.EARS_LONG, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        draw(this.earsRound, f, MobDef.EARS_ROUND, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        draw(this.horns, f, MobDef.HORNS, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        draw(this.snout, f, MobDef.SNOUT, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        draw(this.beak, f, MobDef.BEAK, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        draw(this.visor, f, MobDef.VISOR, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        draw(this.antenna, f, MobDef.ANTENNA, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        draw(this.fin, f, MobDef.FIN, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();

        poseStack.pushPose();
        this.body.translateAndRotate(poseStack);
        draw(this.tailBushy, f, MobDef.TAIL_BUSHY, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        draw(this.tailThin, f, MobDef.TAIL_THIN, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        draw(this.tailShort, f, MobDef.TAIL_SHORT, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        draw(this.tailThick, f, MobDef.TAIL_THICK, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        draw(this.wingRight, f, MobDef.WINGS, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        draw(this.wingLeft, f, MobDef.WINGS, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }

    private static void draw(ModelPart part, int flags, int flag, PoseStack poseStack, VertexConsumer buffer, int packedLight,
                             int packedOverlay, float red, float green, float blue, float alpha) {
        if ((flags & flag) != 0) {
            part.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }
}
