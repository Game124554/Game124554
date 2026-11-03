package net.realisticoverhaul.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.realisticoverhaul.config.ROConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes bedrock breakable (as hard as obsidian). */
@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockStateMixin {
	@Shadow
	public abstract Block getBlock();

	@Inject(method = "getHardness", at = @At("HEAD"), cancellable = true)
	private void realisticoverhaul$bedrockHardness(BlockView world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
		if (ROConfig.get().bedrockBreakable && getBlock() == Blocks.BEDROCK) {
			cir.setReturnValue(50.0f);
		}
	}
}
