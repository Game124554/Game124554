package com.latexdrone.client;

import com.latexdrone.content.LatexForm;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.MinecraftClient;

/** Ears/tail and glow for players who are latex (or turning into it). */
public final class PlayerLatexSpecs {
    private PlayerLatexSpecs() {}

    private static float tickDelta() {
        return MinecraftClient.getInstance().getTickDelta();
    }

    public static PartsSpec parts(AbstractClientPlayerEntity player) {
        ClientLatexData.State s = ClientLatexData.get(player.getUuid());
        if (s == null) return null;
        if (s.transformed) {
            LatexForm f = LatexForm.get(s.form);
            return new PartsSpec(f.parts(), f.primary(), f.secondary(), 1f);
        }
        if (s.transformTicks > 0) {
            LatexForm f = LatexForm.get(s.pendingForm);
            float p = s.progress(tickDelta());
            return new PartsSpec(f.parts(), f.primary(), f.secondary(), Math.max(0f, (p - 0.3f) / 0.7f));
        }
        return null;
    }

    public static GlowSpec glow(AbstractClientPlayerEntity player) {
        ClientLatexData.State s = ClientLatexData.get(player.getUuid());
        if (s == null) return null;
        if (s.transformed) return new GlowSpec(FormTextures.glow(s.form), 1f);
        if (s.transformTicks > 0) {
            float p = s.progress(tickDelta());
            return new GlowSpec(FormTextures.glow(s.pendingForm), Math.max(0f, (p - 0.5f) * 2f));
        }
        return null;
    }
}
