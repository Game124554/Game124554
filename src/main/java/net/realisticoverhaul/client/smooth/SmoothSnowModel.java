package net.realisticoverhaul.client.smooth;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
import net.realisticoverhaul.RealisticOverhaul;
import net.realisticoverhaul.config.ROConfig;

import java.util.List;
import java.util.function.Supplier;

/**
 * Snow layers drawn as one continuous, wavy snow surface. The height at each corner and
 * edge of a block is the average of the snow (or ground) heights around it, so layers of
 * different depth melt into smooth dunes and drifts ramp up against walls.
 */
public class SmoothSnowModel extends ForwardingBakedModel {
	private static final int SNOW = 0, SOLID = 1, OPEN = 2;
	private static volatile boolean failed;

	public SmoothSnowModel(BakedModel wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockRenderView view, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
		if (!failed && ROConfig.get().smoothSnow && !(view instanceof World) && state.isOf(Blocks.SNOW)) {
			try {
				emitSnow(view, state, pos, randomSupplier, context.getEmitter());
				return;
			} catch (Throwable t) {
				failed = true;
				RealisticOverhaul.LOGGER.error("Smooth snow failed, using normal snow layers", t);
			}
		}
		super.emitBlockQuads(view, state, pos, randomSupplier, context);
	}

	private static boolean solid(BlockRenderView view, BlockPos p) {
		BlockState s = view.getBlockState(p);
		return !s.isOf(Blocks.SNOW) && (s.isOpaqueFullCube(view, p) || SmoothTerrain.isSmoothable(s));
	}

	private void emitSnow(BlockRenderView view, BlockState state, BlockPos pos, Supplier<Random> rs, QuadEmitter e) {
		float[][] col = new float[3][3];
		int[][] type = new int[3][3];
		BlockPos.Mutable m = new BlockPos.Mutable();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				m.set(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
				BlockState s = view.getBlockState(m);
				float h;
				int t;
				if (s.isOf(Blocks.SNOW)) {
					h = s.get(SnowBlock.LAYERS) / 8f;
					t = SNOW;
				} else if (solid(view, m)) {
					h = 1f;
					t = SOLID;
				} else {
					t = OPEN;
					m.move(Direction.DOWN);
					BlockState below = view.getBlockState(m);
					if (below.isOf(Blocks.SNOW)) h = below.get(SnowBlock.LAYERS) / 8f - 1f;
					else if (solid(view, m)) h = 0f;
					else h = -0.3f;
				}
				col[dx + 1][dz + 1] = h;
				type[dx + 1][dz + 1] = t;
			}
		}

		// Heights of a 3x3 grid of vertices (corners, edge middles, centre).
		float[][] h = new float[3][3];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				float sum = 0;
				int n = 0;
				for (int cx = (i == 0 ? -1 : 0); cx <= (i == 2 ? 1 : 0); cx++) {
					for (int cz = (j == 0 ? -1 : 0); cz <= (j == 2 ? 1 : 0); cz++) {
						sum += col[cx + 1][cz + 1];
						n++;
					}
				}
				h[i][j] = MathHelper.clamp(sum / n, 0.01f, 1f);
			}
		}

		BakedModel model = this.wrapped;
		List<BakedQuad> quads = model.getQuads(state, Direction.UP, rs.get());
		Sprite sprite = quads.isEmpty() ? model.getParticleSprite() : quads.get(0).getSprite();
		RenderMaterial mat = SmoothMesher.material();
		int lightTop = WorldRenderer.getLightmapCoordinates(view, pos);

		// Top surface: 4 quads.
		for (int qi = 0; qi < 2; qi++) {
			for (int qj = 0; qj < 2; qj++) {
				float[] xs = {qi * 0.5f, qi * 0.5f, qi * 0.5f + 0.5f, qi * 0.5f + 0.5f};
				float[] zs = {qj * 0.5f, qj * 0.5f + 0.5f, qj * 0.5f + 0.5f, qj * 0.5f};
				float[] ys = {h[qi][qj], h[qi][qj + 1], h[qi + 1][qj + 1], h[qi + 1][qj]};
				emit(e, mat, sprite, view, xs, ys, zs, lightTop, Direction.UP);
			}
		}

		// Sides facing open air.
		m.set(pos).move(Direction.DOWN);
		float bottom = SmoothTerrain.enabled() && SmoothTerrain.isSmoothable(view.getBlockState(m)) ? -0.5f : 0f;
		for (Direction d : Direction.Type.HORIZONTAL) {
			if (type[d.getOffsetX() + 1][d.getOffsetZ() + 1] != OPEN) continue;
			int light = WorldRenderer.getLightmapCoordinates(view, m.set(pos, d));
			for (int seg = 0; seg < 2; seg++) {
				float a0 = seg * 0.5f, a1 = a0 + 0.5f;
				float[] xs, ys, zs;
				switch (d) {
					case EAST -> {
						float t0 = h[2][seg], t1 = h[2][seg + 1];
						xs = new float[]{1, 1, 1, 1};
						ys = new float[]{bottom, t0, t1, bottom};
						zs = new float[]{a0, a0, a1, a1};
					}
					case WEST -> {
						float t0 = h[0][seg], t1 = h[0][seg + 1];
						xs = new float[]{0, 0, 0, 0};
						ys = new float[]{bottom, t1, t0, bottom};
						zs = new float[]{a1, a1, a0, a0};
					}
					case SOUTH -> {
						float t0 = h[seg][2], t1 = h[seg + 1][2];
						xs = new float[]{a0, a1, a1, a0};
						ys = new float[]{bottom, bottom, t1, t0};
						zs = new float[]{1, 1, 1, 1};
					}
					default -> { // NORTH
						float t0 = h[seg][0], t1 = h[seg + 1][0];
						xs = new float[]{a0, a1, a1, a0};
						ys = new float[]{t0, t1, bottom, bottom};
						zs = new float[]{0, 0, 0, 0};
					}
				}
				if (Math.max(Math.max(ys[0], ys[1]), Math.max(ys[2], ys[3])) <= bottom + 0.005f) continue;
				emit(e, mat, sprite, view, xs, ys, zs, light, d);
			}
		}
	}

	private static void emit(QuadEmitter e, RenderMaterial mat, Sprite sprite, BlockRenderView view,
							 float[] xs, float[] ys, float[] zs, int light, Direction face) {
		float ax = xs[2] - xs[0], ay = ys[2] - ys[0], az = zs[2] - zs[0];
		float bx = xs[3] - xs[1], by = ys[3] - ys[1], bz = zs[3] - zs[1];
		float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
		float len = MathHelper.sqrt(nx * nx + ny * ny + nz * nz);
		if (len < 1.0e-6f) {
			nx = face.getOffsetX();
			ny = face.getOffsetY();
			nz = face.getOffsetZ();
		} else {
			nx /= len;
			ny /= len;
			nz /= len;
		}
		float shade = nx * nx * view.getBrightness(nx > 0 ? Direction.EAST : Direction.WEST, true)
				+ ny * ny * view.getBrightness(ny > 0 ? Direction.UP : Direction.DOWN, true)
				+ nz * nz * view.getBrightness(nz > 0 ? Direction.SOUTH : Direction.NORTH, true);
		int g = MathHelper.clamp((int) (shade * 255f + 0.5f), 0, 255);
		int color = 0xFF000000 | (g << 16) | (g << 8) | g;
		boolean top = face == Direction.UP;
		for (int k = 0; k < 4; k++) {
			float u, v;
			if (top) {
				u = xs[k];
				v = zs[k];
			} else {
				u = face.getAxis() == Direction.Axis.X ? zs[k] : xs[k];
				v = 1f - ys[k];
			}
			e.pos(k, xs[k], ys[k], zs[k]);
			e.uv(k, MathHelper.clamp(u, 0f, 1f), MathHelper.clamp(v, 0f, 1f));
			e.color(k, color);
			e.lightmap(k, light);
			e.normal(k, nx, ny, nz);
		}
		e.spriteBake(sprite, MutableQuadView.BAKE_NORMALIZED);
		e.colorIndex(-1);
		e.material(mat);
		e.cullFace(null);
		e.nominalFace(face);
		e.emit();
	}
}
