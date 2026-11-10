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

/** Goo blob: a fat cube with a droplet on top and two stubby arms. The wobble is done in BlobRenderer#scale. */
public class BlobModel<T extends Entity> extends HierarchicalModel<T> {
    private final ModelPart root;
    private final ModelPart armRight;
    private final ModelPart armLeft;

    public BlobModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.armRight = body.getChild("arm_right");
        this.armLeft = body.getChild("arm_left");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.0F, -10.0F, -6.0F, 12.0F, 10.0F, 12.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
        body.addOrReplaceChild("nub", CubeListBuilder.create()
                .texOffs(0, 22).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 3.0F, 4.0F), PartPose.ZERO);
        body.addOrReplaceChild("arm_right", CubeListBuilder.create()
                .texOffs(16, 22).addBox(-2.0F, 0.0F, -2.0F, 2.0F, 4.0F, 4.0F), PartPose.offset(-6.0F, -6.0F, 0.0F));
        body.addOrReplaceChild("arm_left", CubeListBuilder.create()
                .texOffs(16, 22).mirror().addBox(0.0F, 0.0F, -2.0F, 2.0F, 4.0F, 4.0F), PartPose.offset(6.0F, -6.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float wave = Mth.sin(ageInTicks * 0.2F) * 0.15F + limbSwingAmount * 0.5F;
        this.armRight.zRot = 0.2F + wave;
        this.armLeft.zRot = -0.2F - wave;
    }
}
