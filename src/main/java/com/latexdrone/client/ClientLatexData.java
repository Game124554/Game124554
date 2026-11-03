package com.latexdrone.client;

import com.latexdrone.infection.InfectionManager;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Client copy of every player's latex state (synced from the server). */
public final class ClientLatexData {
    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    public static final class State {
        public float infection;
        public boolean transformed;
        public int form;
        public int pendingForm;
        public int transformTicks;

        /** 0 -> 1 over the transformation. */
        public float progress(float tickDelta) {
            if (transformTicks <= 0) return transformed ? 1f : 0f;
            return MathHelper.clamp(1f - (transformTicks - tickDelta) / (float) InfectionManager.TRANSFORM_TIME, 0f, 1f);
        }
    }

    private ClientLatexData() {}

    @Nullable
    public static State get(UUID id) {
        return STATES.get(id);
    }

    public static void update(UUID id, float infection, boolean transformed, int form, int pendingForm, int ticks) {
        State s = STATES.computeIfAbsent(id, k -> new State());
        s.infection = infection;
        s.transformed = transformed;
        s.form = form;
        s.pendingForm = pendingForm;
        s.transformTicks = ticks;
    }

    /** Local prediction so the animation is smooth between server syncs. */
    public static void tick() {
        for (State s : STATES.values()) {
            if (s.transformTicks > 1) s.transformTicks--;
        }
    }

    public static void clear() {
        STATES.clear();
    }
}
