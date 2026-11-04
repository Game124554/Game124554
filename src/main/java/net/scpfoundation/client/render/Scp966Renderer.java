package net.scpfoundation.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.scpfoundation.client.model.ScpBipedModel;
import net.scpfoundation.entity.Scp966Entity;

/** SCP-966 is only drawn for players who currently have night vision. */
public class Scp966Renderer extends ScpBipedRenderer<Scp966Entity> {
    public Scp966Renderer(EntityRendererFactory.Context ctx, ScpBipedModel<Scp966Entity> model, Identifier texture) {
        super(ctx, model, texture, 0.0f, 0.85f, 1.18f);
    }

    @Override
    public void render(Scp966Entity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        ClientPlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null || !viewer.hasStatusEffect(StatusEffects.NIGHT_VISION)) return;
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
