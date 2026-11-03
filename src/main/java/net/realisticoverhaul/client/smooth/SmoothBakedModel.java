package net.realisticoverhaul.client.smooth;

import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
import net.realisticoverhaul.RealisticOverhaul;

import java.util.function.Supplier;

/**
 * Wraps the model of every full-cube block. When the block is in the smooth list and is
 * being built into a chunk mesh, it is drawn as smooth terrain; otherwise the normal
 * cube model is used. Works with Fabric's default renderer (Indigo) and with Indium on Sodium.
 */
public class SmoothBakedModel extends ForwardingBakedModel {
	private static volatile boolean failed;

	public SmoothBakedModel(BakedModel wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
		// Chunk meshes are built from a world snapshot; falling blocks, pistons and the
		// block-breaking overlay pass the real World and keep the cube shape.
		if (!failed && SmoothTerrain.enabled() && !(blockView instanceof World) && SmoothTerrain.isSmoothable(state)) {
			try {
				SmoothMesher.emit(wrapped, state, pos, blockView, randomSupplier, context.getEmitter());
				return;
			} catch (Throwable t) {
				failed = true;
				RealisticOverhaul.LOGGER.error("Smooth terrain failed, falling back to cubes", t);
			}
		}
		super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
	}
}
