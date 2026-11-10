package com.teardownvoxel.client.render;

import com.teardownvoxel.entity.VoxelDebrisEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renders a debris cube textured with a random fragment of the source block's texture, so a shattered
 * brick wall really rains little brick-textured voxels.
 */
public class VoxelDebrisRenderer extends EntityRenderer<VoxelDebrisEntity> {
    public VoxelDebrisRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.12f;
        this.shadowOpacity = 0.6f;
    }

    @Override
    public void render(VoxelDebrisEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        BlockState state = entity.getBlockState();
        float baseScale = entity.getScale();
        float scale = baseScale * entity.getFade(tickDelta);
        if (scale > 0.001f) {
            MinecraftClient mc = MinecraftClient.getInstance();
            Sprite sprite = mc.getBlockRenderManager().getModels().getModelParticleSprite(state);

            int color = mc.getBlockColors().getColor(state, entity.getWorld(), entity.getBlockPos(), 0);
            float r = 1f, g = 1f, b = 1f;
            if (color != -1) {
                r = (color >> 16 & 255) / 255f;
                g = (color >> 8 & 255) / 255f;
                b = (color & 255) / 255f;
            }

            boolean translucent = RenderLayers.getBlockLayer(state) == RenderLayer.getTranslucent();
            VertexConsumer vc = vertexConsumers.getBuffer(translucent
                    ? RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                    : RenderLayer.getEntityCutoutNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));

            // A px*px window of the 16x16 texture, chosen per entity.
            int px = MathHelper.clamp(Math.round(baseScale * 16f), 2, 16);
            int range = 17 - px;
            int id = entity.getId();
            float ou = Math.floorMod(id * 7 + 3, range);
            float ov = Math.floorMod(id * 13 + 5, range);
            float u0 = sprite.getFrameU(ou), u1 = sprite.getFrameU(ou + px);
            float v0 = sprite.getFrameV(ov), v1 = sprite.getFrameV(ov + px);

            matrices.push();
            matrices.translate(0.0, baseScale * 0.5, 0.0);
            float sx = MathHelper.lerp(tickDelta, entity.prevSpinX, entity.spinX);
            float sz = MathHelper.lerp(tickDelta, entity.prevSpinZ, entity.spinZ);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(id * 37 % 360));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sx));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sz));
            matrices.scale(scale, scale, scale);
            drawCube(matrices.peek(), vc, u0, u1, v0, v1, r, g, b, light);
            matrices.pop();
        }
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    static void drawCube(MatrixStack.Entry entry, VertexConsumer vc, float u0, float u1, float v0, float v1,
                         float r, float g, float b, int light) {
        Matrix4f pos = entry.getPositionMatrix();
        Matrix3f nrm = entry.getNormalMatrix();
        float h = 0.5f;
        // +Y
        quad(pos, nrm, vc, -h, h, -h, -h, h, h, h, h, h, h, h, -h, 0, 1, 0, u0, u1, v0, v1, r, g, b, light);
        // -Y
        quad(pos, nrm, vc, -h, -h, h, -h, -h, -h, h, -h, -h, h, -h, h, 0, -1, 0, u0, u1, v0, v1, r * 0.5f, g * 0.5f, b * 0.5f, light);
        // +Z
        quad(pos, nrm, vc, -h, h, h, -h, -h, h, h, -h, h, h, h, h, 0, 0, 1, u0, u1, v0, v1, r * 0.8f, g * 0.8f, b * 0.8f, light);
        // -Z
        quad(pos, nrm, vc, h, h, -h, h, -h, -h, -h, -h, -h, -h, h, -h, 0, 0, -1, u0, u1, v0, v1, r * 0.8f, g * 0.8f, b * 0.8f, light);
        // +X
        quad(pos, nrm, vc, h, h, h, h, -h, h, h, -h, -h, h, h, -h, 1, 0, 0, u0, u1, v0, v1, r * 0.6f, g * 0.6f, b * 0.6f, light);
        // -X
        quad(pos, nrm, vc, -h, h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -1, 0, 0, u0, u1, v0, v1, r * 0.6f, g * 0.6f, b * 0.6f, light);
    }

    private static void quad(Matrix4f pos, Matrix3f nrm, VertexConsumer vc,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float nx, float ny, float nz,
                             float u0, float u1, float v0, float v1,
                             float r, float g, float b, int light) {
        vertex(pos, nrm, vc, x1, y1, z1, u0, v0, nx, ny, nz, r, g, b, light);
        vertex(pos, nrm, vc, x2, y2, z2, u0, v1, nx, ny, nz, r, g, b, light);
        vertex(pos, nrm, vc, x3, y3, z3, u1, v1, nx, ny, nz, r, g, b, light);
        vertex(pos, nrm, vc, x4, y4, z4, u1, v0, nx, ny, nz, r, g, b, light);
    }

    private static void vertex(Matrix4f pos, Matrix3f nrm, VertexConsumer vc, float x, float y, float z, float u, float v,
                               float nx, float ny, float nz, float r, float g, float b, int light) {
        vc.vertex(pos, x, y, z)
                .color(r, g, b, 1.0f)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(nrm, nx, ny, nz)
                .next();
    }

    @Override
    public Identifier getTexture(VoxelDebrisEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
}
