package dev.stumble.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * The two-handed shove: hands come up to the chest, then both arms drive straight out, hold a
 * moment, and drop back into whatever the player was doing. Blended over the vanilla pose, so
 * walking legs and the head keep moving underneath it.
 */
public final class PushAnimation {
    /** Ticks: wind-up ends, arms fully out, hold ends, back to normal. */
    private static final float WIND = 2.5f, OUT = 4f, HOLD = 6f, END = 10f;
    /** Player entity id -> its tickCount when it pushed. */
    private static final Map<Integer, Integer> START = new HashMap<>();

    private PushAnimation() {
    }

    public static void start(Entity e) {
        START.put(e.getId(), e.tickCount);
    }

    /** Forget finished pushes (run once a tick, never while drawing). */
    public static void tick() {
        if (START.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            START.clear();
            return;
        }
        START.entrySet().removeIf(entry -> {
            Entity e = mc.level.getEntity(entry.getKey());
            return e == null || e.tickCount - entry.getValue() > END + 2 || e.tickCount < entry.getValue();
        });
    }

    /** Called at the end of HumanoidModel.setupAnim. {@code age} is tickCount plus the partial tick. */
    public static void apply(LivingEntity e, float age, ModelPart right, ModelPart left) {
        if (START.isEmpty()) {
            return;
        }
        Integer start = START.get(e.getId());
        if (start == null) {
            return;
        }
        float t = age - start;
        if (t < 0 || t >= END) {
            return;
        }
        // how much of the push pose shows: quick in, held, eased out
        float w = t < 1.5f ? smooth(t / 1.5f) : t < HOLD ? 1f : 1f - smooth((t - HOLD) / (END - HOLD));

        // arm pitch (negative = forward) and how far the arms turn in toward the middle
        float pitch, in;
        if (t < WIND) {
            float k = smooth(t / WIND);
            pitch = lerp(-0.5f, -1.05f, k);         // hands come up to chest height
            in = lerp(0.15f, 0.35f, k);             // and close in until the hands nearly meet
        } else if (t < OUT) {
            float k = smooth((t - WIND) / (OUT - WIND));
            pitch = lerp(-1.05f, -1.62f, k);        // drive out, arms straight ahead
            in = lerp(0.35f, 0.12f, k);
        } else {
            pitch = -1.62f;
            in = 0.12f;
        }
        right.xRot = lerp(right.xRot, pitch, w);
        right.yRot = lerp(right.yRot, -in, w);
        right.zRot = lerp(right.zRot, 0f, w);
        left.xRot = lerp(left.xRot, pitch, w);
        left.yRot = lerp(left.yRot, in, w);
        left.zRot = lerp(left.zRot, 0f, w);
    }

    private static float smooth(float k) {
        k = Math.max(0f, Math.min(1f, k));
        return k * k * (3 - 2 * k);
    }

    private static float lerp(float a, float b, float k) {
        return a + (b - a) * k;
    }
}
