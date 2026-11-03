package net.realisticoverhaul.mixin.client;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.realisticoverhaul.client.MobAnimations;
import net.realisticoverhaul.client.Ragdoll;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Limb ragdolls on corpses, and corpses stop glowing red after the first second. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Shadow
	protected EntityModel<?> model;

	@Unique
	private static final String RENDER = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V";

	@Inject(method = RENDER, at = @At("HEAD"))
	private void realisticoverhaul$begin(LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices,
										 VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
		Ragdoll.beginRender();
	}

	@Inject(method = RENDER, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFF)V",
			shift = At.Shift.AFTER))
	private void realisticoverhaul$ragdoll(LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices,
										   VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
		Ragdoll.pose(entity, this.model, tickDelta);
		MobAnimations.pose(entity, this.model, tickDelta);
	}

	@Inject(method = "setupTransforms", at = @At("TAIL"))
	private void realisticoverhaul$creepyTransforms(LivingEntity entity, MatrixStack matrices, float animationProgress,
													float bodyYaw, float tickDelta, CallbackInfo ci) {
		MobAnimations.transforms(entity, matrices, tickDelta);
	}

	@Inject(method = RENDER, at = @At("RETURN"))
	private void realisticoverhaul$end(LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices,
									   VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
		Ragdoll.endRender();
	}

	@Inject(method = "getOverlay", at = @At("HEAD"), cancellable = true)
	private static void realisticoverhaul$noRedCorpse(LivingEntity entity, float whiteOverlayProgress, CallbackInfoReturnable<Integer> cir) {
		if (entity.deathTime > 20 && entity.hurtTime <= 0) {
			cir.setReturnValue(OverlayTexture.packUv(OverlayTexture.getU(whiteOverlayProgress), OverlayTexture.getV(false)));
		}
	}
}
