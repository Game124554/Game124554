package net.realisticoverhaul.client;

import net.minecraft.util.math.MathHelper;
import net.realisticoverhaul.config.ROConfig;

/** "Trauma" based camera shake: hits add trauma, trauma fades over time. */
public final class CameraShake {
	private static float trauma;
	private static int time;

	private CameraShake() {}

	public static void add(float amount) {
		if (!ROConfig.get().cameraShake) return;
		trauma = MathHelper.clamp(trauma + amount, 0f, 1f);
	}

	public static void tick() {
		time++;
		trauma = Math.max(0f, trauma - 0.03f);
	}

	public static void reset() {
		trauma = 0f;
	}

	/** Returns {yawOffset, pitchOffset} in degrees, or null when not shaking. */
	public static float[] offsets(float tickDelta) {
		if (trauma <= 0f || !ROConfig.get().cameraShake) return null;
		float strength = trauma * trauma * (ROConfig.get().cameraShakeStrength / 100f);
		float t = (time + tickDelta) * 0.9f;
		float yaw = strength * 5f * (MathHelper.sin(t * 1.7f) * 0.6f + MathHelper.sin(t * 3.3f + 1.3f) * 0.4f);
		float pitch = strength * 4f * (MathHelper.sin(t * 2.1f + 0.7f) * 0.6f + MathHelper.sin(t * 3.9f + 2.1f) * 0.4f);
		return new float[]{yaw, pitch};
	}
}
