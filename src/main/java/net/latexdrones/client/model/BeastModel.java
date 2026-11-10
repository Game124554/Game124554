package net.latexdrones.client.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** Four-legged feral latex beast (hounds, cats, boar ...). Texture: beast_texture in tools/gen_textures.py. */
public class BeastModel<T extends Entity> extends HierarchicalModel<T> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart legFrontRight;
    private final ModelPart legFrontLeft;
    private final ModelPart legBackRight;
    private final ModelPart legBackLeft;
    private final ModelPart tail;

    public BeastModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.legFrontRight = root.getChild("leg_front_right");
        this.legFrontLeft = root.getChild("leg_front_left");
        this.legBackRight = root.getChild("leg_back_right");
        this.legBackLeft = root.getChild("leg_back_left");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -3.0F, -8.0F, 8.0F, 6.0F, 16.0F), PartPose.offset(0.0F, 13.0F, 0.0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 22).addBox(-3.0F, -3.0F, -6.0F, 6.0F, 6.0F, 6.0F), PartPose.offset(0.0F, 11.0F, -8.0F));
        head.addOrReplaceChild("snout", CubeListBuilder.create()
                .texOffs(24, 22).addBox(-1.5F, 0.0F, -9.0F, 3.0F, 3.0F, 3.0F), PartPose.ZERO);
        head.addOrReplaceChild("ear_right", CubeListBuilder.create()
                .texOffs(36, 22).addBox(-3.0F, -5.0F, -3.0F, 2.0F, 2.0F, 1.0F), PartPose.ZERO);
        head.addOrReplaceChild("ear_left", CubeListBuilder.create()
                .texOffs(42, 22).addBox(1.0F, -5.0F, -3.0F, 2.0F, 2.0F, 1.0F), PartPose.ZERO);
        CubeListBuilder leg = CubeListBuilder.create().texOffs(48, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F);
        root.addOrReplaceChild("leg_front_right", leg, PartPose.offset(-2.5F, 16.0F, -6.0F));
        root.addOrReplaceChild("leg_front_left", leg, PartPose.offset(2.5F, 16.0F, -6.0F));
        root.addOrReplaceChild("leg_back_right", leg, PartPose.offset(-2.5F, 16.0F, 6.0F));
        root.addOrReplaceChild("leg_back_left", leg, PartPose.offset(2.5F, 16.0F, 6.0F));
        root.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(24, 28).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 8.0F), PartPose.offset(0.0F, 11.0F, 8.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);
        float swing = Mth.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount;
        this.legFrontRight.xRot = swing;
        this.legFrontLeft.xRot = -swing;
        this.legBackRight.xRot = -swing;
        this.legBackLeft.xRot = swing;
        this.tail.xRot = 0.3F + limbSwingAmount * 0.4F;
        this.tail.yRot = Mth.cos(ageInTicks * 0.2F) * 0.3F;
    }
}
