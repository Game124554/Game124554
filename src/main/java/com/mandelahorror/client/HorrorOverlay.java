package com.mandelahorror.client;

import com.mandelahorror.HorrorConfig;
import com.mandelahorror.network.SpeechPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;

import java.util.List;

/** HUD layer: VHS static, red flash, and the Alternate's "text speech" subtitles. */
public final class HorrorOverlay {
    private static final RandomSource RANDOM = RandomSource.create();

    /** Called from Fabric's HudRenderCallback. */
    public static void render(GuiGraphics g, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        VhsFilter.render(g, width, height, partialTick);

        float staticLevel = ClientHorror.proximityStatic * 0.35F;
        if (ClientHorror.staticBurst > 0) {
            staticLevel = Math.max(staticLevel, 0.25F + 0.6F * ClientHorror.staticBurst / (float) ClientHorror.staticBurstMax);
        }
        if (staticLevel > 0.01F && HorrorConfig.STATIC_OVERLAY.get()) {
            drawStatic(g, width, height, staticLevel, ClientHorror.ticks, partialTick);
        }

        if (ClientHorror.redFlash > 0) {
            float f = ClientHorror.redFlash / (float) ClientHorror.redFlashMax;
            int alpha = (int) (f * 170);
            g.fill(0, 0, width, height, (alpha << 24) | 0x8A0000);
        }

        drawSpeech(g, mc.font, width, height, partialTick);
    }

    /** VHS-style noise + scanlines + a rolling bright band. */
    public static void drawStatic(GuiGraphics g, int width, int height, float level, long ticks, float partialTick) {
        int count = (int) (500 * level);
        int alpha = (int) (Math.min(1F, level) * 220);
        for (int i = 0; i < count; i++) {
            int x = RANDOM.nextInt(Math.max(1, width));
            int y = RANDOM.nextInt(Math.max(1, height));
            int len = 1 + RANDOM.nextInt(8);
            int gray = 60 + RANDOM.nextInt(196);
            g.fill(x, y, x + len, y + 1, (alpha << 24) | (gray << 16) | (gray << 8) | gray);
        }
        int lineAlpha = (int) (Math.min(1F, level) * 70);
        for (int y = 0; y < height; y += 3) {
            g.fill(0, y, width, y + 1, lineAlpha << 24);
        }
        int band = (int) (((ticks + partialTick) * 3) % (height + 40)) - 20;
        g.fill(0, band, width, band + 10, ((int) (Math.min(1F, level) * 40) << 24) | 0xFFFFFF);
    }

    private static void drawSpeech(GuiGraphics g, Font font, int width, int height, float partialTick) {
        ClientHorror.SpeechLine s = ClientHorror.speech;
        if (s == null) return;
        float age = (ClientHorror.ticks - s.startTick()) + partialTick;
        if (age > s.duration()) return;

        float fade = Math.min(1F, (s.duration() - age) / 20F);
        int alpha = Math.max(8, (int) (fade * 255));
        int shown = Math.min(s.text().length(), (int) (age * 1.3F) + 1);
        String visible = s.text().substring(0, shown);

        if (s.style() == SpeechPacket.STYLE_BROADCAST) {
            int top = 6;
            g.fill(0, top, width, top + 34, (Math.min(alpha, 200) << 24));
            g.fill(0, top, width, top + 11, (alpha << 24) | 0xAA0000);
            g.drawString(font, "M.A.D. EMERGENCY BROADCAST", 6, top + 2, (alpha << 24) | 0xFFFFFF, false);
            g.drawString(font, visible, 6, top + 18, (alpha << 24) | 0xFFFFFF, false);
            return;
        }

        // the Alternate: big, shaky, chromatic text near the middle of the screen
        float scale = 1.6F;
        int maxWidth = (int) (width * 0.8F / scale);
        List<String> parts = IntroScreen.wrap(font, visible, maxWidth);
        int y = (int) (height * 0.62F);
        for (String part : parts) {
            drawJitter(g, font, part, width / 2, y, scale, alpha);
            y += (int) (font.lineHeight * scale) + 2;
        }
    }

    private static void drawJitter(GuiGraphics g, Font font, String text, int centerX, int y, float scale, int alpha) {
        g.pose().pushPose();
        g.pose().translate(centerX, y, 0);
        g.pose().scale(scale, scale, 1F);
        int x = -font.width(text) / 2;
        for (int i = 0; i < text.length(); i++) {
            String c = String.valueOf(text.charAt(i));
            int jx = RANDOM.nextInt(10) == 0 ? RANDOM.nextInt(3) - 1 : 0;
            int jy = RANDOM.nextInt(8) == 0 ? RANDOM.nextInt(3) - 1 : 0;
            g.drawString(font, c, x + jx + 1, jy, ((alpha / 2) << 24) | 0xFF0000, false);
            g.drawString(font, c, x + jx, jy, (alpha << 24) | 0xFFFFFF, false);
            x += font.width(c);
        }
        g.pose().popPose();
    }

    private HorrorOverlay() {}
}
