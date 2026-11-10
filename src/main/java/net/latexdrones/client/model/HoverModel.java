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

/** Mechanical quad-rotor drone with a glowing lens and an injector needle. It always floats above the ground. */
public class HoverModel<T extends Entity> extends HierarchicalModel<T> {
    private final ModelPart root;
    private final ModelPart core;
    private final ModelPart rotorNorth;
    private final ModelPart rotorSouth;
    private final ModelPart rotorEast;
    private final ModelPart rotorWest;

    public HoverModel(ModelPart root) {
        this.root = root;
        this.core = root.getChild("core");
        this.rotorNorth = this.core.getChild("rotor_north");
        this.rotorSouth = this.core.getChild("rotor_south");
        this.rotorEast = this.core.getChild("rotor_east");
        this.rotorWest = this.core.getChild("rotor_west");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition core = root.addOrReplaceChild("core", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        core.addOrReplaceChild("lens", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-2.0F, -2.0F, -5.0F, 4.0F, 4.0F, 1.0F), PartPose.ZERO);
        core.addOrReplaceChild("bar_x", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-9.0F, -5.0F, -1.0F, 18.0F, 1.0F, 2.0F), PartPose.ZERO);
        core.addOrReplaceChild("bar_z", CubeListBuilder.create()
                .texOffs(0, 19).addBox(-1.0F, -5.0F, -9.0F, 2.0F, 1.0F, 18.0F), PartPose.ZERO);
        core.addOrReplaceChild("needle", CubeListBuilder.create()
                .texOffs(42, 0).addBox(-0.5F, 4.0F, -0.5F, 1.0F, 5.0F, 1.0F), PartPose.ZERO);
        CubeListBuilder rotor = CubeListBuilder.create().texOffs(0, 38).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 1.0F, 6.0F);
        core.addOrReplaceChild("rotor_north", rotor, PartPose.offset(0.0F, -6.0F, -8.0F));
        core.addOrReplaceChild("rotor_south", rotor, PartPose.offset(0.0F, -6.0F, 8.0F));
        core.addOrReplaceChild("rotor_east", rotor, PartPose.offset(8.0F, -6.0F, 0.0F));
        core.addOrReplaceChild("rotor_west", rotor, PartPose.offset(-8.0F, -6.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.core.y = 12.0F + Mth.sin(ageInTicks * 0.12F) * 1.5F;
        this.core.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.core.xRot = headPitch * ((float) Math.PI / 180F) * 0.5F;
        float spin = ageInTicks * 1.6F;
        this.rotorNorth.yRot = spin;
        this.rotorSouth.yRot = -spin;
        this.rotorEast.yRot = spin;
        this.rotorWest.yRot = -spin;
    }
}
