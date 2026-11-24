package com.mandelahorror.client;

import com.mandelahorror.HorrorConfig;
import com.mandelahorror.MandelaHorror;
import com.mandelahorror.mixin.GameRendererInvoker;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.lwjgl.glfw.GLFW;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Old camcorder look:
 *  - black & white (or faded color) post-processing shader with a slight horizontal smear
 *  - scanlines, noise, rolling tracking band, occasional tracking glitch, dark edges
 *  - "PLAY ▶" / "SP" labels and a date + year + clock stamp
 * Toggle with V (or vhs.enabled in config/mandelahorror.properties).
 */
public final class VhsFilter {
    private static final ResourceLocation BW = MandelaHorror.id("shaders/post/vhs.json");
    private static final ResourceLocation COLOR = MandelaHorror.id("shaders/post/vhs_color.json");
    private static final RandomSource RANDOM = RandomSource.create();

    private static KeyMapping toggleKey;
    private static boolean enabled = true;
    private static boolean failed;
    private static int glitchTicks;

    public static void init() {
        enabled = HorrorConfig.VHS_FILTER.get();
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.mandelahorror.vhs",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.mandelahorror"));
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        while (toggleKey != null && toggleKey.consumeClick()) {
            enabled = !enabled;
        }
        if (glitchTicks > 0) glitchTicks--;
        else if (enabled && RANDOM.nextInt(400) == 0) glitchTicks = 4 + RANDOM.nextInt(10);
        if (ClientHorror.staticBurst > 0 && glitchTicks < 3) glitchTicks = 3;

        if (mc.level == null || mc.gameRenderer == null) return;
        ResourceLocation wanted = HorrorConfig.VHS_BLACK_AND_WHITE.get() ? BW : COLOR;
        PostChain current = mc.gameRenderer.currentEffect();
        boolean ours = current != null && (current.getName().equals(BW.toString()) || current.getName().equals(COLOR.toString()));

        if (enabled && !failed) {
            if (current == null || (ours && !current.getName().equals(wanted.toString()))) {
                try {
                    ((GameRendererInvoker) mc.gameRenderer).mandelahorror$loadEffect(wanted);
                } catch (Throwable t) {
                    failed = true;
                    MandelaHorror.LOGGER.warn("Mandela Horror: could not load the VHS shader", t);
                }
                if (mc.gameRenderer.currentEffect() == null) failed = true; // shader failed to compile: don't retry every tick
            }
        } else if (ours) {
            mc.gameRenderer.shutdownEffect();
        }
    }

    public static void reset() {
        failed = false;
    }

    /** Drawn under the rest of the horror HUD. */
    public static void render(GuiGraphics g, int width, int height, float partialTick) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        long ticks = ClientHorror.ticks;

        // scanlines
        for (int y = 0; y < height; y += 2) g.fill(0, y, width, y + 1, 0x1A000000);
        // dark edges (cheap vignette)
        for (int i = 0; i < 12; i++) {
            int a = (12 - i) * 5;
            int c = a << 24;
            g.fill(0, i, width, i + 1, c);
            g.fill(0, height - i - 1, width, height - i, c);
            g.fill(i, 0, i + 1, height, c);
            g.fill(width - i - 1, 0, width - i, height, c);
        }
        // fine noise
        for (int i = 0; i < 90; i++) {
            int x = RANDOM.nextInt(Math.max(1, width));
            int y = RANDOM.nextInt(Math.max(1, height));
            int gray = 120 + RANDOM.nextInt(136);
            g.fill(x, y, x + 1 + RANDOM.nextInt(3), y + 1, 0x30000000 | (gray << 16) | (gray << 8) | gray);
        }
        // slow rolling tracking band
        int band = (int) (((ticks + partialTick) * 0.7F) % (height + 60)) - 30;
        g.fill(0, band, width, band + 6, 0x0FFFFFFF);
        g.fill(0, band + 6, width, band + 7, 0x18FFFFFF);
        // bottom-of-tape distortion strip
        g.fill(0, height - 5, width, height - 2, 0x22FFFFFF);
        // tracking glitch
        if (glitchTicks > 0) {
            for (int i = 0; i < 4; i++) {
                int y = RANDOM.nextInt(Math.max(1, height));
                int h = 1 + RANDOM.nextInt(4);
                int shift = RANDOM.nextInt(30) - 15;
                g.fill(Math.max(0, shift), y, width + Math.min(0, shift), y + h, 0x55FFFFFF);
            }
        }

        // labels
        Font font = mc.font;
        boolean blink = (ticks / 15) % 2 == 0;
        drawVhsText(g, font, "PLAY ▶", 14, 12, 1.5F);
        drawVhsText(g, font, "SP", width - 14 - (int) (font.width("SP") * 1.5F), 12, 1.5F);
        if (blink) drawVhsText(g, font, "●", 14, 30, 1.2F);

        drawVhsText(g, font, clock(mc), 14, height - 42, 1.5F);
        drawVhsText(g, font, date(), 14, height - 26, 1.5F);
    }

    private static void drawVhsText(GuiGraphics g, Font font, String text, int x, int y, float scale) {
        int jx = glitchTicks > 0 && RANDOM.nextBoolean() ? RANDOM.nextInt(3) - 1 : 0;
        g.pose().pushPose();
        g.pose().translate(x + jx, y, 0);
        g.pose().scale(scale, scale, 1F);
        g.drawString(font, text, 1, 1, 0xAA000000, false);
        g.drawString(font, text, 0, 0, 0xFFEFEFEF, false);
        g.pose().popPose();
    }

    private static String date() {
        LocalDate today = LocalDate.now();
        int year = HorrorConfig.VHS_YEAR.get() == 0 ? today.getYear() : HorrorConfig.VHS_YEAR.get();
        String month = today.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ROOT);
        return month + ". " + today.getDayOfMonth() + " " + year;
    }

    private static String clock(Minecraft mc) {
        int hour, minute;
        if (HorrorConfig.VHS_REAL_TIME.get() || mc.level == null) {
            LocalTime now = LocalTime.now();
            hour = now.getHour();
            minute = now.getMinute();
        } else {
            long t = (mc.level.getDayTime() + 6000L) % 24000L; // tick 0 = 6:00 AM
            hour = (int) (t / 1000L);
            minute = (int) ((t % 1000L) * 60L / 1000L);
        }
        int h12 = hour % 12 == 0 ? 12 : hour % 12;
        return (hour < 12 ? "AM " : "PM ") + h12 + ":" + String.format(Locale.ROOT, "%02d", minute);
    }

    private VhsFilter() {}
}
