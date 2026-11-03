package net.realisticoverhaul.client.smooth;

import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Smooth terrain mesher (Surface Nets on the block grid, the technique NoCubes uses).
 *
 * Every block centre is a sample: "solid" (smoothable or opaque full cube) or "empty".
 * Each 2x2x2 group of samples (a cell) gets one vertex, placed at the average of the
 * points where the solid/empty boundary crosses the cell's edges. For every face of a
 * smoothable block that touches an empty block, one quad is drawn between the 4 cell
 * vertices around that face. Flat ground stays exactly flat; steps and corners become
 * slopes. Vertex lighting is averaged from the empty samples of each cell, and cells
 * with more solid samples are darkened (ambient occlusion).
 */
public final class SmoothMesher {
	private static final Direction[] DIRS = Direction.values();
	/** The 12 edges of a cell, as pairs of corner indices (corner = x*4 + y*2 + z). */
	private static final int[][] EDGES = {
			{0, 4}, {1, 5}, {2, 6}, {3, 7},
			{0, 2}, {1, 3}, {4, 6}, {5, 7},
			{0, 1}, {2, 3}, {4, 5}, {6, 7}
	};

	/** Cell offsets (u, v) around a face, counter-clockwise seen from outside. */
	private static final int[][] POS_UV = {{-1, -1}, {0, -1}, {0, 0}, {-1, 0}};
	private static final int[][] NEG_UV = {{-1, 0}, {0, 0}, {0, -1}, {-1, -1}};

	private static final ThreadLocal<Work> WORK = ThreadLocal.withInitial(Work::new);

	private static final class Work {
		final boolean[] solid = new boolean[27];
		final int[] light = new int[27];
		final boolean[] lightDone = new boolean[27];
		final float[] vx = new float[8];
		final float[] vy = new float[8];
		final float[] vz = new float[8];
		final int[] vLight = new int[8];
		final float[] vAo = new float[8];
		final boolean[] vDone = new boolean[8];
		final int[] quadCells = new int[4];
		final int[] cell = new int[3];
		final BlockPos.Mutable m = new BlockPos.Mutable();
	}

	/** Lighting is done here (smooth light + corner shading), so the renderer's own AO and shading are off. */
	private static volatile RenderMaterial material;

	private static RenderMaterial material() {
		RenderMaterial m = material;
		if (m == null) {
			Renderer r = RendererAccess.INSTANCE.getRenderer();
			m = r.materialFinder().ambientOcclusion(TriState.FALSE).disableDiffuse(true).find();
			material = m;
		}
		return m;
	}

	private SmoothMesher() {}

	private static int sIdx(int x, int y, int z) {
		return (x + 1) * 9 + (y + 1) * 3 + (z + 1);
	}

	private static int cIdx(int cx, int cy, int cz) {
		return (cx + 1) * 4 + (cy + 1) * 2 + (cz + 1);
	}

	private static boolean isSolid(BlockRenderView world, BlockPos pos) {
		BlockState s = world.getBlockState(pos);
		return SmoothTerrain.isSmoothable(s) || s.isOpaqueFullCube(world, pos);
	}

	/** Emits the smooth surface of one smoothable block through the Fabric renderer. */
	public static void emit(BakedModel model, BlockState state, BlockPos pos, BlockRenderView world,
							Supplier<Random> randomSupplier, QuadEmitter emitter) {
		Work w = WORK.get();
		BlockPos.Mutable m = w.m;

		boolean exposed = false;
		for (Direction d : DIRS) {
			m.set(pos, d);
			if (!isSolid(world, m)) {
				exposed = true;
				break;
			}
		}
		if (!exposed) return;

		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					int i = sIdx(x, y, z);
					if (x == 0 && y == 0 && z == 0) {
						w.solid[i] = true;
					} else {
						m.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
						w.solid[i] = isSolid(world, m);
					}
					w.lightDone[i] = false;
				}
			}
		}
		Arrays.fill(w.vDone, false);

		boolean ao = MinecraftClient.getInstance().options.getAo().getValue();
		RenderMaterial mat = material();

		for (Direction d : DIRS) {
			if (w.solid[sIdx(d.getOffsetX(), d.getOffsetY(), d.getOffsetZ())]) continue;

			int axis = d.getAxis().ordinal();
			int ua = (axis + 1) % 3;
			int va = (axis + 2) % 3;
			boolean positive = d.getDirection() == Direction.AxisDirection.POSITIVE;
			int ca = positive ? 0 : -1;
			// Corners around the face, counter-clockwise seen from outside.
			int[][] uv = positive ? POS_UV : NEG_UV;
			for (int k = 0; k < 4; k++) {
				w.cell[axis] = ca;
				w.cell[ua] = uv[k][0];
				w.cell[va] = uv[k][1];
				int ci = cIdx(w.cell[0], w.cell[1], w.cell[2]);
				computeVertex(w, ci, w.cell[0], w.cell[1], w.cell[2], world, pos, ao);
				w.quadCells[k] = ci;
			}
			emitFace(w, d, state, world, model, randomSupplier, mat, emitter);
		}
	}

	private static int lightAt(Work w, int si, int x, int y, int z, BlockRenderView world, BlockPos pos) {
		if (!w.lightDone[si]) {
			w.m.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
			w.light[si] = WorldRenderer.getLightmapCoordinates(world, w.m);
			w.lightDone[si] = true;
		}
		return w.light[si];
	}

	private static void computeVertex(Work w, int ci, int cx, int cy, int cz, BlockRenderView world, BlockPos pos, boolean ao) {
		if (w.vDone[ci]) return;
		int solidCount = 0;
		int blockSum = 0;
		int skySum = 0;
		int lightCount = 0;
		for (int c = 0; c < 8; c++) {
			int x = cx + ((c >> 2) & 1);
			int y = cy + ((c >> 1) & 1);
			int z = cz + (c & 1);
			int si = sIdx(x, y, z);
			if (w.solid[si]) {
				solidCount++;
			} else {
				int l = lightAt(w, si, x, y, z, world, pos);
				blockSum += l & 0xFFFF;
				skySum += (l >>> 16) & 0xFFFF;
				lightCount++;
			}
		}

		float sx = 0f, sy = 0f, sz = 0f;
		int n = 0;
		for (int[] e : EDGES) {
			int a = e[0];
			int b = e[1];
			int ax = (a >> 2) & 1, ay = (a >> 1) & 1, az = a & 1;
			int bx = (b >> 2) & 1, by = (b >> 1) & 1, bz = b & 1;
			boolean sa = w.solid[sIdx(cx + ax, cy + ay, cz + az)];
			boolean sb = w.solid[sIdx(cx + bx, cy + by, cz + bz)];
			if (sa != sb) {
				sx += cx + (ax + bx) * 0.5f + 0.5f;
				sy += cy + (ay + by) * 0.5f + 0.5f;
				sz += cz + (az + bz) * 0.5f + 0.5f;
				n++;
			}
		}
		if (n > 0) {
			w.vx[ci] = sx / n;
			w.vy[ci] = sy / n;
			w.vz[ci] = sz / n;
		} else {
			w.vx[ci] = cx + 1f;
			w.vy[ci] = cy + 1f;
			w.vz[ci] = cz + 1f;
		}
		w.vLight[ci] = lightCount > 0 ? ((skySum / lightCount) << 16) | (blockSum / lightCount) : 0;
		w.vAo[ci] = ao ? MathHelper.clamp(1f - (solidCount - 4) * 0.13f, 0.5f, 1f) : 1f;
		w.vDone[ci] = true;
	}

	private static void emitFace(Work w, Direction d, BlockState state, BlockRenderView world, BakedModel model,
								 Supplier<Random> randomSupplier, RenderMaterial mat, QuadEmitter emitter) {
		int[] q = w.quadCells;
		float ax = w.vx[q[2]] - w.vx[q[0]], ay = w.vy[q[2]] - w.vy[q[0]], az = w.vz[q[2]] - w.vz[q[0]];
		float bx = w.vx[q[3]] - w.vx[q[1]], by = w.vy[q[3]] - w.vy[q[1]], bz = w.vz[q[3]] - w.vz[q[1]];
		float nx = ay * bz - az * by;
		float ny = az * bx - ax * bz;
		float nz = ax * by - ay * bx;
		float len = MathHelper.sqrt(nx * nx + ny * ny + nz * nz);
		if (len < 1.0e-5f) {
			nx = d.getOffsetX();
			ny = d.getOffsetY();
			nz = d.getOffsetZ();
		} else {
			nx /= len;
			ny /= len;
			nz /= len;
		}

		// Which texture of the block to use: top on gentle slopes, sides on steep ones.
		Direction tex;
		if (ny > 0.55f) tex = Direction.UP;
		else if (ny < -0.55f) tex = Direction.DOWN;
		else if (d.getAxis().isHorizontal()) tex = d;
		else if (Math.abs(nx) + Math.abs(nz) < 1.0e-4f) tex = Direction.NORTH;
		else tex = Direction.getFacing(nx, 0f, nz);

		List<BakedQuad> quads = model.getQuads(state, tex, randomSupplier.get());
		boolean filter = false;
		if (quads.isEmpty()) {
			quads = model.getQuads(state, null, randomSupplier.get());
			filter = true;
			if (quads.isEmpty()) return;
		}

		// Directional shading like vanilla, blended by the slope's normal.
		float shade = nx * nx * world.getBrightness(nx > 0 ? Direction.EAST : Direction.WEST, true)
				+ ny * ny * world.getBrightness(ny > 0 ? Direction.UP : Direction.DOWN, true)
				+ nz * nz * world.getBrightness(nz > 0 ? Direction.SOUTH : Direction.NORTH, true);

		boolean drewAny = false;
		for (int i = 0; i < quads.size(); i++) {
			BakedQuad quad = quads.get(i);
			if (filter && quad.getFace() != tex) continue;
			emitQuad(w, quad, d, tex, mat, emitter, shade, nx, ny, nz);
			drewAny = true;
		}
		if (!drewAny && filter) {
			emitQuad(w, quads.get(0), d, tex, mat, emitter, shade, nx, ny, nz);
		}
	}

	private static void emitQuad(Work w, BakedQuad quad, Direction face, Direction tex, RenderMaterial mat, QuadEmitter e,
								 float shade, float nx, float ny, float nz) {
		float s = quad.hasShade() ? shade : 1f;
		int[] q = w.quadCells;
		for (int k = 0; k < 4; k++) {
			int ci = q[k];
			float x = w.vx[ci], y = w.vy[ci], z = w.vz[ci];
			float u, v;
			switch (tex) {
				case UP -> { u = x; v = z; }
				case DOWN -> { u = x; v = 1f - z; }
				case NORTH -> { u = 1f - x; v = 1f - y; }
				case SOUTH -> { u = x; v = 1f - y; }
				case WEST -> { u = z; v = 1f - y; }
				default -> { u = 1f - z; v = 1f - y; }
			}
			int g = MathHelper.clamp((int) (s * w.vAo[ci] * 255f + 0.5f), 0, 255);
			e.pos(k, x, y, z);
			e.uv(k, MathHelper.clamp(u, 0f, 1f), MathHelper.clamp(v, 0f, 1f));
			e.color(k, 0xFF000000 | (g << 16) | (g << 8) | g);
			e.lightmap(k, w.vLight[ci]);
			e.normal(k, nx, ny, nz);
		}
		e.spriteBake(quad.getSprite(), MutableQuadView.BAKE_NORMALIZED);
		e.colorIndex(quad.getColorIndex());
		e.material(mat);
		e.cullFace(null);
		e.nominalFace(face);
		e.emit();
	}
}
