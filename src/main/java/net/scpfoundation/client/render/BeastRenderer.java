package net.scpfoundation.client.render;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.scpfoundation.client.model.ScpBeastModel;

import java.util.function.Function;

public class BeastRenderer<T extends MobEntity> extends MobEntityRenderer<T, ScpBeastModel<T>> {
    private final Function<T, Identifier> texture;
    private final float scale;

    public BeastRenderer(EntityRendererFactory.Context ctx, ScpBeastModel<T> model, float shadow, float scale, Function<T, Identifier> texture) {
        super(ctx, model, shadow * scale);
        this.texture = texture;
        this.scale = scale;
    }

    @Override
    public Identifier getTexture(T entity) {
        return texture.apply(entity);
    }

    @Override
    public void scale(T entity, MatrixStack matrices, float amount) {
        matrices.scale(scale, scale, scale);
    }
}
