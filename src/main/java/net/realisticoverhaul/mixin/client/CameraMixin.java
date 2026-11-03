package net.realisticoverhaul.mixin.client;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.realisticoverhaul.client.CameraShake;
import net.realisticoverhaul.client.ClientEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Shadow
	public abstract float getYaw();

	@Shadow
	public abstract float getPitch();

	@Shadow
	protected abstract void setPos(double x, double y, double z);

	@Shadow
	public abstract Vec3d getPos();

	@Inject(method = "update", at = @At("TAIL"))
	private void realisticoverhaul$shake(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
		float[] o = CameraShake.offsets(tickDelta);
		if (o != null) setRotation(getYaw() + o[0], getPitch() + o[1]);
		// Glide up steps instead of snapping up a whole block.
		float step = ClientEffects.stepOffset(tickDelta);
		if (step != 0f) {
			Vec3d p = getPos();
			setPos(p.x, p.y + step, p.z);
		}
	}
}
