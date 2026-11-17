package com.thunderscience.changedtsc.client;

import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.network.ClientGooData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Draws the paper doll on the HUD while there is goo in the body or the player is transfurred. */
public final class PaperDollHud {
    private static final int PADDING = 3;

    private PaperDollHud() {}

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || mc.options.renderDebug || player.isSpectator()) return;
        if (!TSCConfig.DOLL_HUD.get() || mc.screen instanceof GooBodyScreen) return;

        float percent = PaperDoll.percent(player);
        boolean transfurred = PaperDoll.transfurred(player);
        if (percent <= 0.0F && !transfurred && !TSCConfig.DOLL_ALWAYS.get()) return;

        int scale = TSCConfig.DOLL_SCALE.get();
        Font font = mc.font;
        Component text = transfurred ? Component.translatable("hud.changed_tsc.transfurred")
                : Component.literal(Math.round(percent) + "%");
        int dollWidth = PaperDoll.WIDTH * scale;
        int dollHeight = (PaperDoll.HEIGHT + PaperDoll.EARS) * scale;
        int boxWidth = Math.max(dollWidth, font.width(text)) + PADDING * 2;
        int boxHeight = dollHeight + font.lineHeight + PADDING * 3;

        String position = TSCConfig.DOLL_POSITION.get();
        int x = position.endsWith("right") ? screenWidth - boxWidth - 4 : 4;
        int y = position.startsWith("top") ? 4
                : position.startsWith("bottom") ? screenHeight - boxHeight - 44
                : (screenHeight - boxHeight) / 2;

        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0x70000000);
        PaperDoll.draw(graphics, x + (boxWidth - dollWidth) / 2, y + PADDING + PaperDoll.EARS * scale, scale,
                percent, ClientGooData.selfWhite(), transfurred);
        graphics.drawCenteredString(font, text, x + boxWidth / 2, y + boxHeight - font.lineHeight - PADDING + 1,
                stageColor(percent, transfurred));
    }

    /** Grey = goo inside, gold = furry body, red = transfurring, purple = transfurred. */
    public static int stageColor(float percent, boolean transfurred) {
        if (transfurred) return 0xC77DFF;
        if (percent <= 0.0F) return 0x55FF55;
        if (percent >= TSCConfig.GOO_STAGE3_AT.get()) return 0xFF5555;
        if (percent >= TSCConfig.GOO_STAGE2_AT.get()) return 0xFFAA00;
        return 0xBBBBBB;
    }
}
