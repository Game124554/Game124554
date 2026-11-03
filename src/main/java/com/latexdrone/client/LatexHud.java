package com.latexdrone.client;

import com.latexdrone.content.LatexForm;
import com.latexdrone.registry.ModEffects;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Hypnosis spiral, creeping latex goo vignette + drips, infection meter, current-form label. */
public final class LatexHud {
    private LatexHud() {}

    public static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        float time = mc.player.age + tickDelta;

        if (mc.player.hasStatusEffect(ModEffects.HYPNOSIS)) drawSpiral(ctx, w, h, time);

        ClientLatexData.State s = ClientLatexData.get(mc.player.getUuid());
        if (s == null) return;

        float goo = 0f;
        int formIdx = s.transformTicks > 0 ? s.pendingForm : s.pendingForm;
        if (s.transformTicks > 0) goo = s.progress(tickDelta);
        else if (!s.transformed && s.infection > 50f) goo = (s.infection - 50f) / 50f * 0.45f;
        if (goo > 0f) drawGoo(ctx, w, h, goo, LatexForm.get(formIdx), time);

        if (s.transformTicks > 0) {
            Text t = Text.translatable("hud.latexdrone.transforming");
            int alpha = (int) (160 + 95 * MathHelper.sin(time * 0.4f));
            ctx.drawCenteredTextWithShadow(mc.textRenderer, t, w / 2, h / 2 - 30, (alpha << 24) | 0xE070FF);
        } else if (s.transformed) {
            LatexForm f = LatexForm.get(s.form);
            ctx.drawTextWithShadow(mc.textRenderer, Text.translatable("hud.latexdrone.form", Text.translatable("entity.latexdrone." + f.id())),
                    6, 6, 0xFF000000 | f.glow());
        } else if (s.infection > 0.5f) {
            drawMeter(ctx, s.infection);
        }
    }

    private static void drawMeter(DrawContext ctx, float infection) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int x = 6, y = 6, bw = 100;
        ctx.drawTextWithShadow(mc.textRenderer, Text.translatable("hud.latexdrone.infection", (int) infection), x, y, 0xFFD8B4FF);
        ctx.fill(x, y + 11, x + bw + 2, y + 18, 0xAA000000);
        int fill = (int) (bw * infection / 100f);
        int color = infection >= 60f ? 0xFFB01E5A : (infection >= 30f ? 0xFF8A3AD6 : 0xFF4A4A5A);
        ctx.fill(x + 1, y + 12, x + 1 + fill, y + 17, color);
        ctx.fill(x + 1, y + 12, x + 1 + fill, y + 13, 0x55FFFFFF); // gloss
    }

    private static void drawSpiral(DrawContext ctx, int w, int h, float time) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ctx.fill(0, 0, w, h, 0x28A020FF);
        float alpha = 0.28f + 0.12f * MathHelper.sin(time * 0.15f);
        ctx.setShaderColor(1f, 1f, 1f, alpha);
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(w / 2f, h / 2f, 0f);
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(time * 6f));
        float zoom = 1.0f + 0.08f * MathHelper.sin(time * 0.2f);
        m.scale(zoom, zoom, 1f);
        int size = (int) (Math.max(w, h) * 1.5f);
        ctx.drawTexture(FormTextures.SPIRAL, -size / 2, -size / 2, 0f, 0f, size, size, size, size);
        m.pop();
        ctx.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private static void drawGoo(DrawContext ctx, int w, int h, float goo, LatexForm form, float time) {
        int rgb = form.primary() & 0xFFFFFF;
        int depth = (int) (goo * Math.min(w, h) * 0.45f);
        int steps = 12;
        for (int k = 0; k < steps; k++) {
            int d0 = depth * k / steps, d1 = depth * (k + 1) / steps;
            int a = (int) (goo * 230f * (1f - k / (float) steps));
            if (a <= 0 || d1 <= d0) continue;
            int c = (Math.min(255, a) << 24) | rgb;
            ctx.fill(0, d0, w, d1, c);
            ctx.fill(0, h - d1, w, h - d0, c);
            ctx.fill(d0, 0, d1, h, c);
            ctx.fill(w - d1, 0, w - d0, h, c);
        }
        // drips running down from the top
        int dripColor = (Math.min(255, (int) (goo * 255f)) << 24) | rgb;
        for (int x = 0; x < w; x += 7) {
            int hash = (x * 73856093) ^ 0x5bd1e995;
            float rnd = ((hash >>> 8) & 1023) / 1023f;
            float wobble = 0.9f + 0.1f * MathHelper.sin(time * 0.05f + x);
            int len = (int) (goo * h * 0.65f * rnd * wobble);
            int wd = 2 + (hash & 3);
            if (len < 3) continue;
            ctx.fill(x, 0, x + wd, len, dripColor);
            ctx.fill(x - 1, len - 3, x + wd + 1, len + 1, dripColor);
            ctx.fill(x + 1, 1, x + 2, Math.max(2, len - 4), 0x30FFFFFF); // shine
        }
    }
}
