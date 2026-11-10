package com.teardownvoxel.client.render;

import com.teardownvoxel.entity.FallingStructureEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** Renders a collapsing block with its real (tinted, lit) model, shuddering before it falls and tumbling slightly. */
public class FallingStructureRenderer extends EntityRenderer<FallingStructureEntity> {
    private final BlockRenderManager blockRenderManager;

    public FallingStructureRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.5f;
        this.blockRenderManager = ctx.getBlockRenderManager();
    }

    @Override
    public void render(FallingStructureEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        BlockState state = entity.getBlockState();
        if (state.getRenderType() != BlockRenderType.MODEL) return;
        World world = entity.getWorld();

        matrices.push();
        float age = entity.age + tickDelta;
        if (age < entity.getDelay()) {
            float j = 0.02f;
            matrices.translate(MathHelper.sin(age * 4.1f) * j, 0, MathHelper.cos(age * 3.7f) * j);
        }
        matrices.translate(0.0, 0.5, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.lerp(tickDelta, entity.prevTiltX, entity.tiltX)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(tickDelta, entity.prevTiltZ, entity.tiltZ)));
        matrices.translate(-0.5, -0.5, -0.5);

        BlockPos pos = BlockPos.ofFloored(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
        this.blockRenderManager.getModelRenderer().render(world, this.blockRenderManager.getModel(state), state, pos, matrices,
                vertexConsumers.getBuffer(RenderLayers.getMovingBlockLayer(state)), false, Random.create(), state.getRenderingSeed(pos),
                OverlayTexture.DEFAULT_UV);
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(FallingStructureEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
}
