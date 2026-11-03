package net.realisticoverhaul.client;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.realisticoverhaul.RealisticOverhaul;
import net.realisticoverhaul.config.ROConfig;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Eyes that glow in the dark: pale yellow for zombies, red for skeletons. */
public class GlowEyesFeature<T extends LivingEntity, M extends EntityModel<T>> extends FeatureRenderer<T, M> {
	private static final Identifier GLOW = new Identifier(RealisticOverhaul.MOD_ID, "textures/entity/glow.png");

	public GlowEyesFeature(FeatureRendererContext<T, M> context) {
		super(context);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity,
					   float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		if (!ROConfig.get().glowingEyes || entity.isDead() || entity.isInvisible()) return;
		if (!(getContextModel() instanceof BipedEntityModel<?> biped) || !biped.head.visible) return;
		// Brighter the darker it is around the mob.
		int level = entity.getWorld().getLightLevel(entity.getBlockPos().up());
		float glow = MathHelper.clamp((11f - level) / 7f, 0f, 1f);
		if (glow <= 0.02f) return;

		boolean skeleton = entity instanceof AbstractSkeletonEntity;
		float r = 1f, g = skeleton ? 0.18f : 0.86f, b = skeleton ? 0.1f : 0.45f;
		float flicker = 0.85f + 0.15f * MathHelper.sin((entity.age + tickDelta) * 0.25f + entity.getId());

		matrices.push();
		biped.head.rotate(matrices);
		VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEyes(GLOW));
		Matrix4f pos = matrices.peek().getPositionMatrix();
		Matrix3f normal = matrices.peek().getNormalMatrix();
		float a = glow * flicker;
		// Head cube is 8x8x8 pixels, front face at z = -4. Eyes sit 4-5 pixels below the top.
		float y = skeleton ? -4.0f : -3.5f;
		float size = skeleton ? 1.35f : 1.1f;
		eye(vc, pos, normal, -2f, y, size, r * a, g * a, b * a);
		eye(vc, pos, normal, 2f, y, size, r * a, g * a, b * a);
		// Soft halo around each eye.
		eye(vc, pos, normal, -2f, y, size * 2.6f, r * a * 0.35f, g * a * 0.35f, b * a * 0.35f);
		eye(vc, pos, normal, 2f, y, size * 2.6f, r * a * 0.35f, g * a * 0.35f, b * a * 0.35f);
		matrices.pop();
	}

	private static void eye(VertexConsumer vc, Matrix4f pos, Matrix3f normal, float cx, float cy, float half,
							float r, float g, float b) {
		float z = -4.05f / 16f;
		float x0 = (cx - half) / 16f, x1 = (cx + half) / 16f;
		float y0 = (cy - half) / 16f, y1 = (cy + half) / 16f;
		vertex(vc, pos, normal, x0, y1, z, 0, 1, r, g, b);
		vertex(vc, pos, normal, x1, y1, z, 1, 1, r, g, b);
		vertex(vc, pos, normal, x1, y0, z, 1, 0, r, g, b);
		vertex(vc, pos, normal, x0, y0, z, 0, 0, r, g, b);
		// Back side too, so it shows whichever way the quad faces.
		vertex(vc, pos, normal, x0, y0, z, 0, 0, r, g, b);
		vertex(vc, pos, normal, x1, y0, z, 1, 0, r, g, b);
		vertex(vc, pos, normal, x1, y1, z, 1, 1, r, g, b);
		vertex(vc, pos, normal, x0, y1, z, 0, 1, r, g, b);
	}

	private static void vertex(VertexConsumer vc, Matrix4f pos, Matrix3f normal, float x, float y, float z,
							   float u, float v, float r, float g, float b) {
		vc.vertex(pos, x, y, z).color(r, g, b, 1f).texture(u, v).overlay(OverlayTexture.DEFAULT_UV)
				.light(0xF000F0).normal(normal, 0f, 0f, -1f).next();
	}
}
