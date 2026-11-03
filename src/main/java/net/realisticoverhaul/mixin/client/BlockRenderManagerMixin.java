package net.realisticoverhaul.mixin.client;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.realisticoverhaul.RealisticOverhaul;
import net.realisticoverhaul.client.smooth.SmoothMesher;
import net.realisticoverhaul.client.smooth.SmoothTerrain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Smooth terrain hook. Chunk meshes are built by calling renderBlock for every block
 * (Fabric's Indigo renderer also calls it for vanilla models), so smoothable blocks are
 * drawn by the smooth mesher here instead of as cubes.
 */
@Mixin(BlockRenderManager.class)
public abstract class BlockRenderManagerMixin {
	@Unique
	private static volatile boolean realisticoverhaul$failed;

	@Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
	private void realisticoverhaul$smoothTerrain(BlockState state, BlockPos pos, BlockRenderView world, MatrixStack matrices,
												 VertexConsumer vertexConsumer, boolean cull, Random random, CallbackInfo ci) {
		if (realisticoverhaul$failed || !SmoothTerrain.enabled() || !(world instanceof ChunkRendererRegion)) return;
		if (!SmoothTerrain.isSmoothable(state)) return;
		try {
			SmoothMesher.render((BlockRenderManager) (Object) this, state, pos, world, matrices, vertexConsumer, random);
			ci.cancel();
		} catch (Throwable t) {
			// Never break chunk building: fall back to normal cubes.
			realisticoverhaul$failed = true;
			RealisticOverhaul.LOGGER.error("Smooth terrain failed, falling back to cubes", t);
		}
	}
}
