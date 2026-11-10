package com.teardownvoxel.client;

import com.teardownvoxel.config.TVConfig;
import net.minecraft.util.math.MathHelper;

/**
 * Client-side camera trauma. Each shake adds trauma that decays over its duration; the camera offset
 * is smooth noise scaled by trauma^2 (feels punchy up close, subtle far away).
 */
public final class ScreenShake {
    private static final float MAX_DEGREES = 7.0f;
    private static float trauma;
    private static float decayPerTick = 0.05f;
    private static int ticks;

    private ScreenShake() {
    }

    public static void add(float intensity, int durationTicks) {
        float add = intensity * 0.25f * TVConfig.get().screenShakeMultiplier;
        trauma = MathHelper.clamp(trauma + add, 0f, 1.5f);
        decayPerTick = Math.max(decayPerTick, trauma / Math.max(4, durationTicks));
    }

    public static void tick() {
        ticks++;
        if (trauma > 0f) {
            trauma = Math.max(0f, trauma - decayPerTick);
            if (trauma == 0f) decayPerTick = 0.05f;
        }
    }

    /** @return {yawOffset, pitchOffset} in degrees. */
    public static float[] getOffset(float tickDelta) {
        if (trauma <= 0f) return new float[]{0f, 0f};
        float t = (ticks + tickDelta) * 1.7f;
        float shake = Math.min(1f, trauma) * Math.min(1f, trauma);
        float yaw = (MathHelper.sin(t * 1.3f) * 0.6f + MathHelper.sin(t * 3.1f + 1.7f) * 0.4f) * MAX_DEGREES * shake;
        float pitch = (MathHelper.sin(t * 1.9f + 0.5f) * 0.6f + MathHelper.sin(t * 2.7f + 2.3f) * 0.4f) * MAX_DEGREES * shake;
        return new float[]{yaw, pitch};
    }
}
