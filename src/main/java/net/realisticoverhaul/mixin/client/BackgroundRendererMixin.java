package net.realisticoverhaul.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.realisticoverhaul.client.ClientEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Red, thick, smoky fog in the Nether and deep underground. */
@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
	@Shadow private static float red;
	@Shadow private static float green;
	@Shadow private static float blue;

	@Inject(method = "render", at = @At("TAIL"))
	private static void realisticoverhaul$hellColor(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfo ci) {
		float k = ClientEffects.hellIntensity;
		if (k <= 0.01f || camera.getSubmersionType() != CameraSubmersionType.NONE) return;
		float a = k * 0.75f;
		red = MathHelper.lerp(a, red, 0.42f);
		green = MathHelper.lerp(a, green, 0.06f);
		blue = MathHelper.lerp(a, blue, 0.03f);
		RenderSystem.clearColor(red, green, blue, 0.0f);
	}

	@Inject(method = "applyFog", at = @At("TAIL"))
	private static void realisticoverhaul$hellFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
		float k = ClientEffects.hellIntensity;
		if (k <= 0.01f || camera.getSubmersionType() != CameraSubmersionType.NONE) return;
		float start = RenderSystem.getShaderFogStart();
		float end = RenderSystem.getShaderFogEnd();
		float targetEnd = Math.max(24f, viewDistance * 0.4f);
		RenderSystem.setShaderFogStart(MathHelper.lerp(k * 0.8f, start, 0f));
		RenderSystem.setShaderFogEnd(MathHelper.lerp(k * 0.7f, end, Math.min(end, targetEnd)));
	}
}
