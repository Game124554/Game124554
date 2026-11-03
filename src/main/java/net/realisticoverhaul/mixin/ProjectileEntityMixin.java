package net.realisticoverhaul.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.realisticoverhaul.config.ROConfig;
import net.realisticoverhaul.physics.BlockPhysics;
import net.realisticoverhaul.physics.GlassPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fast arrows, tridents, snowballs etc. shatter glass. */
@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {
	@Inject(method = "onBlockHit", at = @At("HEAD"))
	private void realisticoverhaul$shatterGlass(BlockHitResult hit, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		World world = self.getWorld();
		ROConfig cfg = ROConfig.get();
		if (world.isClient || !cfg.glassBreaking) return;
		if (self.getVelocity().length() < cfg.glassBreakSpeed) return;
		BlockPos pos = hit.getBlockPos();
		if (GlassPhysics.isGlass(world.getBlockState(pos))) {
			world.breakBlock(pos, false, self);
			if (world instanceof ServerWorld sw) BlockPhysics.queue(sw, pos);
		}
	}
}
