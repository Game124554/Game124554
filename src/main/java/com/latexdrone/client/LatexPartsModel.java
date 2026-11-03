package com.latexdrone.client;

import com.latexdrone.LatexDroneMod;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;

/** Extra anthro parts layered onto the humanoid model: ears, snout, horns, tail, fin, wings. */
public final class LatexPartsModel {
    public static final EntityModelLayer LAYER = new EntityModelLayer(LatexDroneMod.id("latex_parts"), "main");

    private LatexPartsModel() {}

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        ModelPartData head = root.addChild("head_parts", ModelPartBuilder.create(), ModelTransform.NONE);
        head.addChild("ear_left", ModelPartBuilder.create().uv(20, 0).cuboid(-1.0f, -3.0f, -0.5f, 2, 3, 1),
                ModelTransform.of(2.6f, -7.6f, 0.0f, 0.0f, 0.0f, 0.3f));
        head.addChild("ear_right", ModelPartBuilder.create().uv(20, 0).mirrored().cuboid(-1.0f, -3.0f, -0.5f, 2, 3, 1),
                ModelTransform.of(-2.6f, -7.6f, 0.0f, 0.0f, 0.0f, -0.3f));
        head.addChild("ear_round_left", ModelPartBuilder.create().uv(20, 5).cuboid(-1.5f, -2.0f, -0.5f, 3, 2, 1),
                ModelTransform.of(3.0f, -7.6f, 0.0f, 0.0f, 0.0f, 0.25f));
        head.addChild("ear_round_right", ModelPartBuilder.create().uv(20, 5).mirrored().cuboid(-1.5f, -2.0f, -0.5f, 3, 2, 1),
                ModelTransform.of(-3.0f, -7.6f, 0.0f, 0.0f, 0.0f, -0.25f));
        head.addChild("long_ear_left", ModelPartBuilder.create().uv(26, 0).cuboid(-1.0f, -6.0f, -0.5f, 2, 6, 1),
                ModelTransform.of(2.0f, -7.6f, 0.5f, -0.2f, 0.0f, 0.15f));
        head.addChild("long_ear_right", ModelPartBuilder.create().uv(26, 0).mirrored().cuboid(-1.0f, -6.0f, -0.5f, 2, 6, 1),
                ModelTransform.of(-2.0f, -7.6f, 0.5f, -0.2f, 0.0f, -0.15f));
        head.addChild("snout", ModelPartBuilder.create().uv(0, 19).cuboid(-2.0f, -2.0f, -3.0f, 4, 3, 3),
                ModelTransform.pivot(0.0f, -1.0f, -4.0f));
        head.addChild("horn_left", ModelPartBuilder.create().uv(20, 10).cuboid(-0.5f, -4.0f, -0.5f, 1, 4, 1),
                ModelTransform.of(2.2f, -7.6f, -1.0f, -0.6f, 0.0f, 0.2f));
        head.addChild("horn_right", ModelPartBuilder.create().uv(20, 10).mirrored().cuboid(-0.5f, -4.0f, -0.5f, 1, 4, 1),
                ModelTransform.of(-2.2f, -7.6f, -1.0f, -0.6f, 0.0f, -0.2f));

        ModelPartData body = root.addChild("body_parts", ModelPartBuilder.create(), ModelTransform.NONE);
        ModelPartData tail = body.addChild("tail", ModelPartBuilder.create().uv(0, 0).cuboid(-1.5f, -1.5f, 0.0f, 3, 3, 6),
                ModelTransform.pivot(0.0f, 10.5f, 1.5f));
        tail.addChild("tail_tip", ModelPartBuilder.create().uv(0, 10).cuboid(-1.0f, -1.0f, 0.0f, 2, 2, 6),
                ModelTransform.pivot(0.0f, 0.0f, 5.5f));
        body.addChild("fin", ModelPartBuilder.create().uv(16, 19).cuboid(-0.5f, -5.0f, 0.0f, 1, 5, 3),
                ModelTransform.of(0.0f, 4.0f, 2.0f, -0.6f, 0.0f, 0.0f));
        body.addChild("wing_left", ModelPartBuilder.create().uv(0, 24).cuboid(0.0f, -4.0f, 0.0f, 8, 8, 0),
                ModelTransform.pivot(1.5f, 3.0f, 2.0f));
        body.addChild("wing_right", ModelPartBuilder.create().uv(0, 24).mirrored().cuboid(-8.0f, -4.0f, 0.0f, 8, 8, 0),
                ModelTransform.pivot(-1.5f, 3.0f, 2.0f));
        return TexturedModelData.of(data, 32, 32);
    }
}
