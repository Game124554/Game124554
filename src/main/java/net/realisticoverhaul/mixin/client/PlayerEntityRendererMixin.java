package net.realisticoverhaul.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.realisticoverhaul.client.ClientPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cursed players are drawn with the Piglin model. */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
	@Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At("HEAD"), cancellable = true)
	private void realisticoverhaul$piglinModel(AbstractClientPlayerEntity player, float yaw, float tickDelta, MatrixStack matrices,
											   VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
		if (ClientPiglin.render(player, yaw, tickDelta, matrices, vertexConsumers, light)) ci.cancel();
	}
}
