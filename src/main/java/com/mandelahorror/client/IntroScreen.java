package com.mandelahorror.client;

import com.mandelahorror.HorrorConfig;
import com.mandelahorror.horror.Lines;
import com.mandelahorror.registry.ModSounds;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * The world-start "open screen": color bars + emergency tone, then an emergency broadcast that
 * types itself out line by line (and is read aloud with text-to-speech), then it says hello to you.
 */
public class IntroScreen extends Screen {
    private static final int BARS_TICKS = 70;
    private static final int END_TICKS = 90;
    private static final int[] BAR_COLORS = {
            0xFFC0C0C0, 0xFFC0C000, 0xFF00C0C0, 0xFF00C000, 0xFFC000C0, 0xFFC00000, 0xFF0000C0
    };

    private final String playerName;
    private final List<String> lines;
    private final List<String> finished = new ArrayList<>();
    private final RandomSource random = RandomSource.create();

    private int phase;          // 0 = bars, 1 = broadcast, 2 = "hello"
    private int phaseTicks;
    private int lineIndex;
    private int charIndex;
    private int holdTicks;
    private boolean spokenThisLine;
    private int waitForPause = 10;
    private boolean started;

    public IntroScreen(String playerName) {
        super(Component.literal("Emergency Broadcast"));
        this.playerName = playerName;
        this.lines = Lines.intro(playerName);
    }

    @Override
    public void tick() {
        // wait until singleplayer is actually paused, so the sounds don't get paused with it
        if (!this.started) {
            if (this.minecraft != null && this.minecraft.hasSingleplayerServer() && !this.minecraft.isPaused() && this.waitForPause-- > 0) {
                return;
            }
            this.started = true;
            ClientHorror.playUi(ModSounds.EAS_TONE.get(), 1.0F, 0.7F);
        }

        this.phaseTicks++;
        switch (this.phase) {
            case 0 -> {
                if (this.phaseTicks >= BARS_TICKS) {
                    nextPhase(1);
                    ClientHorror.playUi(ModSounds.STATIC.get(), 1.0F, 0.4F);
                    Tts.say("This is an emergency broadcast.", true);
                }
            }
            case 1 -> tickBroadcast();
            case 2 -> {
                if (this.phaseTicks == 30) Tts.say("hello, " + this.playerName + ".", true);
                if (this.phaseTicks % 9 == 0) ClientHorror.playUi(ModSounds.STATIC.get(), 0.6F + this.random.nextFloat() * 0.6F, 0.35F);
                if (this.phaseTicks >= END_TICKS) this.onClose();
            }
            default -> { }
        }
    }

    private void tickBroadcast() {
        if (this.lineIndex >= this.lines.size()) {
            nextPhase(2);
            ClientHorror.playUi(ModSounds.STATIC.get(), 0.7F, 0.8F);
            return;
        }
        String line = this.lines.get(this.lineIndex);
        if (!this.spokenThisLine) {
            this.spokenThisLine = true;
            Tts.say(line, false);
        }
        if (this.charIndex < line.length()) {
            this.charIndex = Math.min(line.length(), this.charIndex + 2);
            if (this.phaseTicks % 2 == 0) ClientHorror.playUi(ModSounds.BEEP.get(), 1.4F + this.random.nextFloat() * 0.2F, 0.25F);
        } else if (++this.holdTicks > 25 + line.length()) {
            this.finished.add(line);
            this.lineIndex++;
            this.charIndex = 0;
            this.holdTicks = 0;
            this.spokenThisLine = false;
        }
    }

    private void nextPhase(int p) {
        this.phase = p;
        this.phaseTicks = 0;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xFF000000);
        switch (this.phase) {
            case 0 -> renderBars(g);
            case 1 -> renderBroadcast(g);
            default -> renderHello(g);
        }
        float noise = this.phase == 2 ? 0.55F : 0.12F;
        HorrorOverlay.drawStatic(g, this.width, this.height, noise, ClientHorror.ticks, partialTick);
        if (HorrorConfig.ALLOW_SKIP_INTRO.get() && this.phase != 2) {
            g.drawString(this.font, "ESC", this.width - 22, this.height - 12, 0x55FFFFFF, false);
        }
    }

    private void renderBars(GuiGraphics g) {
        int barW = this.width / BAR_COLORS.length + 1;
        int barH = (int) (this.height * 0.72);
        for (int i = 0; i < BAR_COLORS.length; i++) {
            g.fill(i * barW, 0, (i + 1) * barW, barH, BAR_COLORS[i]);
        }
        g.fill(0, barH, this.width, this.height, 0xFF101010);
        String text = "PLEASE STAND BY";
        int tw = this.font.width(text) * 2;
        int cx = this.width / 2;
        int cy = barH / 2;
        g.fill(cx - tw / 2 - 10, cy - 12, cx + tw / 2 + 10, cy + 22, 0xFF000000);
        drawScaled(g, text, cx, cy - 4, 2.0F, 0xFFFFFFFF, true);
    }

    private void renderBroadcast(GuiGraphics g) {
        // red header
        g.fill(0, 14, this.width, 46, 0xFFAA0000);
        drawScaled(g, "EMERGENCY ALERT SYSTEM", this.width / 2, 20, 2.0F, 0xFFFFFFFF, true);
        drawScaled(g, "M.A.D.  //  LOCAL BROADCAST", this.width / 2, 52, 1.0F, 0xFFAA0000, true);

        int maxWidth = (int) (this.width * 0.8F / 1.25F);
        int y = 80;
        for (String done : this.finished) {
            for (String part : wrap(this.font, done, maxWidth)) {
                drawScaled(g, part, this.width / 2, y, 1.25F, 0xFF8A8A8A, true);
                y += 14;
            }
            y += 4;
        }
        if (this.lineIndex < this.lines.size()) {
            String current = this.lines.get(this.lineIndex).substring(0, this.charIndex);
            boolean cursor = (ClientHorror.ticks / 6) % 2 == 0;
            List<String> parts = wrap(this.font, current + (cursor ? "_" : " "), maxWidth);
            for (String part : parts) {
                drawScaled(g, part, this.width / 2, y, 1.25F, 0xFFFFFFFF, true);
                y += 14;
            }
        }
        // keep the newest lines on screen: drop old ones if we run out of space
        if (y > this.height - 30 && !this.finished.isEmpty()) this.finished.remove(0);
    }

    private void renderHello(GuiGraphics g) {
        boolean flicker = this.random.nextFloat() < 0.18F;
        if (flicker) g.fill(0, 0, this.width, this.height, 0xFF220000);
        String hello = "hello, " + this.playerName + ".";
        int jx = this.random.nextInt(5) - 2;
        int jy = this.random.nextInt(5) - 2;
        if (this.phaseTicks > 12) {
            drawScaled(g, hello, this.width / 2 + jx + 2, this.height / 2 - 20 + jy, 3.0F, 0x88FF0000, false);
            drawScaled(g, hello, this.width / 2 + jx, this.height / 2 - 20 + jy, 3.0F, 0xFFFFFFFF, false);
        }
        if (this.phaseTicks > 45) {
            drawScaled(g, "i see you.", this.width / 2, this.height / 2 + 24, 1.5F, 0xFFAA0000, false);
        }
    }

    // ------------------------------------------------------------------ helpers

    static void drawScaled(GuiGraphics g, String text, int centerX, int y, float scale, int color, boolean shadow) {
        Font font = net.minecraft.client.Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(centerX, y, 0);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(font, text, -font.width(text) / 2, 0, color, shadow);
        g.pose().popPose();
    }

    static List<String> wrap(Font font, String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (font.width(test) > maxWidth && line.length() > 0) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return HorrorConfig.ALLOW_SKIP_INTRO.get();
    }

    @Override
    public void onClose() {
        if (this.phase != 2) Tts.stop();
        super.onClose();
    }
}
