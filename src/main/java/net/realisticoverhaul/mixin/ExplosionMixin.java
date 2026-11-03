package net.realisticoverhaul.mixin;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.realisticoverhaul.config.ROConfig;
import net.realisticoverhaul.net.ModNet;
import net.realisticoverhaul.physics.BlockPhysics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Explosions shake the camera and make the blocks around the crater collapse. */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {
	@Shadow @Final private World world;
	@Shadow @Final private double x;
	@Shadow @Final private double y;
	@Shadow @Final private double z;
	@Shadow @Final private float power;

	@Shadow
	public abstract List<BlockPos> getAffectedBlocks();

	@Inject(method = "affectWorld", at = @At("TAIL"))
	private void realisticoverhaul$afterExplosion(boolean particles, CallbackInfo ci) {
		if (!(world instanceof ServerWorld sw)) return;
		ROConfig cfg = ROConfig.get();
		if (cfg.cameraShake) {
			ModNet.shakeNear(sw, new Vec3d(x, y, z), Math.max(8.0, power * 8.0), Math.min(1.0f, power / 4.0f));
		}
		if (cfg.blockPhysics) {
			for (BlockPos pos : getAffectedBlocks()) BlockPhysics.queue(sw, pos);
		}
	}
}
