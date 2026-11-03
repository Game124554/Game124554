package net.realisticoverhaul.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.realisticoverhaul.config.ROConfig;
import net.realisticoverhaul.entity.Corpses;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla removes a dead mob after 1 second. Here it stays lying on the ground
 * as a corpse for the configured time and can be pushed by players.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "updatePostDeath", at = @At("HEAD"), cancellable = true)
	private void realisticoverhaul$corpse(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		ROConfig cfg = ROConfig.get();
		if (!cfg.corpses || !Corpses.appliesTo(self)) return;
		ci.cancel();

		if (self.deathTime < 32000) self.deathTime++;
		World world = self.getWorld();
		if (world.isClient) return;

		int maxTicks = Math.max(20, cfg.corpseSeconds * 20);
		if (self.deathTime >= maxTicks && !self.isRemoved()) {
			world.sendEntityStatus(self, (byte) 60); // poof particles
			self.remove(Entity.RemovalReason.KILLED);
			return;
		}

		// Players walking into a corpse push it.
		if (self.age % 2 == 0) {
			for (PlayerEntity p : world.getEntitiesByClass(PlayerEntity.class, self.getBoundingBox().expand(0.3, 0.0, 0.3), p -> !p.isSpectator())) {
				Vec3d d = self.getPos().subtract(p.getPos());
				Vec3d flat = new Vec3d(d.x, 0, d.z);
				if (flat.lengthSquared() < 1.0E-4) continue;
				flat = flat.normalize().multiply(0.12);
				self.addVelocity(flat.x, 0.0, flat.z);
				self.velocityModified = true;
			}
		}
	}
}
