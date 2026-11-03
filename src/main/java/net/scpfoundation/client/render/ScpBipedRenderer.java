package net.scpfoundation.client.render;

import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.scpfoundation.client.model.ScpBipedModel;

public class ScpBipedRenderer<T extends MobEntity> extends BipedEntityRenderer<T, ScpBipedModel<T>> {
    private final Identifier texture;
    private final float scaleXZ;
    private final float scaleY;

    public ScpBipedRenderer(EntityRendererFactory.Context ctx, ScpBipedModel<T> model, Identifier texture, float shadow, float scaleXZ, float scaleY) {
        super(ctx, model, shadow);
        this.texture = texture;
        this.scaleXZ = scaleXZ;
        this.scaleY = scaleY;
    }

    @Override
    public Identifier getTexture(T entity) {
        return texture;
    }

    @Override
    public void scale(T entity, MatrixStack matrices, float amount) {
        matrices.scale(scaleXZ, scaleY, scaleXZ);
    }
}
