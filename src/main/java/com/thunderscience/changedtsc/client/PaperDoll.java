package com.thunderscience.changedtsc.client;

import com.thunderscience.changedtsc.compat.ChangedCompat;
import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.network.ClientGooData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * The paper doll: a small front view of the player's body, in the style of the First Aid mod's
 * health figure, that shows where the goo is.
 *
 *   human body part   skin colour
 *   goo inside        skin with a goo stain
 *   furry             goo colour with fur; hands and feet are marked with a paw print
 *   transfurred       the whole doll is furry and gets ears and a tail
 *
 * The goo starts in the hands, moves up the arms, then the legs, the body and last the head.
 * It is drawn with plain rectangles, so it needs no texture and works at any size.
 */
public final class PaperDoll {
    /** Doll size in "units". One unit is drawn as {@code scale} pixels. */
    public static final int WIDTH = 16;
    public static final int HEIGHT = 32;
    /** Room above the head for the ears. */
    public static final int EARS = 3;

    public enum State { HUMAN, GOO_INSIDE, FURRY }

    /** Body parts as seen from the front, so the doll's right arm is on the viewer's left. */
    public enum Part {
        RIGHT_ARM("right_arm", 0, 8, 4, 9, 0, false),
        RIGHT_HAND("right_hand", 0, 17, 4, 3, 0, true),
        LEFT_ARM("left_arm", 12, 8, 4, 9, 1, false),
        LEFT_HAND("left_hand", 12, 17, 4, 3, 1, true),
        RIGHT_LEG("right_leg", 4, 20, 4, 9, 2, false),
        RIGHT_FOOT("right_foot", 4, 29, 4, 3, 2, true),
        LEFT_LEG("left_leg", 8, 20, 4, 9, 3, false),
        LEFT_FOOT("left_foot", 8, 29, 4, 3, 3, true),
        BODY("body", 4, 8, 8, 12, 4, false),
        HEAD("head", 4, 0, 8, 8, 5, false);

        public final String id;
        final int x, y, w, h;
        /** Order in which the goo reaches this part (0 = first). */
        final int order;
        /** Hands and feet: these become paws. */
        public final boolean paw;

        Part(String id, int x, int y, int w, int h, int order, boolean paw) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.order = order;
            this.paw = paw;
        }
    }

    private static final int GROUPS = 6;
    private static final int SKIN = 0xFFE3B38F;
    private static final int SKIN_LINE = 0xFF6B4A36;
    private static final int BLACK_GOO = 0xFF24242C;
    private static final int BLACK_GOO_LIGHT = 0xFF4A4A58;
    private static final int BLACK_LINE = 0xFF08080B;
    private static final int WHITE_GOO = 0xFFF2F2F6;
    private static final int WHITE_GOO_SHADE = 0xFFC9C9D4;
    private static final int WHITE_LINE = 0xFF777784;
    private static final int PAD_PINK = 0xFFE88AA4;
    private static final int PAD_GRAY = 0xFF85858F;

    private static int cachedTick = Integer.MIN_VALUE;
    private static boolean cachedTransfurred;
    private static float cachedChangedPercent;

    private PaperDoll() {}

    // ------------------------------------------------------------------ what to show

    private static void refresh(Player player) {
        if (player.tickCount == cachedTick) return;
        cachedTick = player.tickCount;
        cachedTransfurred = ChangedCompat.isTransfurred(player);
        cachedChangedPercent = TSCConfig.DOLL_CHANGED_PROGRESS.get() ? ChangedCompat.transfurPercent(player) : 0.0F;
    }

    /** Is the player in a transfurred form (from the Changed mod)? */
    public static boolean transfurred(Player player) {
        refresh(player);
        return cachedTransfurred;
    }

    /** Changed's own transfur progress, 0-100. */
    public static float changedPercent(Player player) {
        refresh(player);
        return cachedChangedPercent;
    }

    /** How far the body has turned, 0-100: the goo infection or Changed's own progress, whichever is higher. */
    public static float percent(Player player) {
        return Math.max(ClientGooData.selfPercent(), changedPercent(player));
    }

    public static State state(Part part, float percent, boolean transfurred) {
        if (transfurred) return State.FURRY;
        if (percent <= 0.0F) return State.HUMAN;
        float furryStart = TSCConfig.GOO_STAGE2_AT.get().floatValue();
        float furryAt = furryStart + part.order * (100.0F - furryStart) / GROUPS;
        if (percent >= furryAt) return State.FURRY;
        float insideAt = part.order * furryStart / GROUPS;
        return percent > insideAt ? State.GOO_INSIDE : State.HUMAN;
    }

    // ------------------------------------------------------------------ drawing

    /**
     * Draws the doll. (x, y) is the top-left corner of the head row; the ears are drawn above it,
     * so leave {@code EARS * scale} pixels free there.
     */
    public static void draw(GuiGraphics g, int x, int y, int scale, float percent, boolean white, boolean transfurred) {
        int goo = white ? WHITE_GOO : BLACK_GOO;
        int gooLine = white ? WHITE_LINE : BLACK_LINE;
        int furTick = white ? WHITE_GOO_SHADE : BLACK_GOO_LIGHT;
        int pad = white ? PAD_GRAY : PAD_PINK;
        boolean paws = TSCConfig.DOLL_PAWS.get();

        boolean headFurry = state(Part.HEAD, percent, transfurred) == State.FURRY;
        boolean bodyFurry = state(Part.BODY, percent, transfurred) == State.FURRY;

        // tail first, so the leg is drawn over its root
        if (bodyFurry) {
            box(g, x, y, scale, 11.0F, 20.5F, 3.5F, 2.0F, gooLine, goo);
            box(g, x, y, scale, 13.5F, 22.0F, 2.5F, 2.5F, gooLine, goo);
            box(g, x, y, scale, 14.5F, 24.0F, 2.0F, 3.5F, gooLine, goo);
        }
        if (headFurry) {
            box(g, x, y, scale, 4.0F, -EARS, 2.5F, EARS + 0.5F, gooLine, goo);
            box(g, x, y, scale, 9.5F, -EARS, 2.5F, EARS + 0.5F, gooLine, goo);
            rect(g, x, y, scale, 4.8F, -EARS + 1.0F, 0.9F, 1.6F, pad);
            rect(g, x, y, scale, 10.3F, -EARS + 1.0F, 0.9F, 1.6F, pad);
        }

        for (Part part : Part.values()) {
            State state = state(part, percent, transfurred);
            switch (state) {
                case HUMAN -> box(g, x, y, scale, part.x, part.y, part.w, part.h, SKIN_LINE, SKIN);
                case GOO_INSIDE -> {
                    box(g, x, y, scale, part.x, part.y, part.w, part.h, SKIN_LINE, blend(SKIN, goo, 0.25F));
                    // a goo stain moving under the skin
                    rect(g, x, y, scale, part.x + part.w / 2.0F - 0.8F, part.y + part.h / 2.0F - 0.8F, 1.6F, 1.6F,
                            blend(SKIN, goo, 0.8F));
                    rect(g, x, y, scale, part.x + part.w / 2.0F - 0.3F, part.y + part.h / 2.0F + 0.8F, 0.6F, 0.9F,
                            blend(SKIN, goo, 0.8F));
                }
                case FURRY -> {
                    box(g, x, y, scale, part.x, part.y, part.w, part.h, gooLine, goo);
                    if (!part.paw) {
                        // short fur strokes
                        for (float fy = part.y + 1.5F; fy < part.y + part.h - 1.0F; fy += 3.0F) {
                            rect(g, x, y, scale, part.x + 0.8F, fy, 0.7F, 0.9F, furTick);
                            rect(g, x, y, scale, part.x + part.w - 1.5F, fy + 1.2F, 0.7F, 0.9F, furTick);
                        }
                    } else if (paws) {
                        pawPrint(g, x, y, scale, part, pad);
                    }
                }
            }
        }

        // face
        int eye = headFurry ? (white ? BLACK_LINE : 0xFFF2F2F6) : 0xFF2A1C14;
        rect(g, x, y, scale, 5.8F, 3.2F, 1.2F, 1.2F, eye);
        rect(g, x, y, scale, 9.0F, 3.2F, 1.2F, 1.2F, eye);
        if (headFurry) rect(g, x, y, scale, 7.4F, 5.0F, 1.2F, 0.9F, pad); // nose
    }

    /** Three toe beans and one big pad. */
    private static void pawPrint(GuiGraphics g, int x, int y, int scale, Part part, int color) {
        rect(g, x, y, scale, part.x + 0.5F, part.y + 0.45F, 0.8F, 0.8F, color);
        rect(g, x, y, scale, part.x + 1.6F, part.y + 0.3F, 0.8F, 0.8F, color);
        rect(g, x, y, scale, part.x + 2.7F, part.y + 0.45F, 0.8F, 0.8F, color);
        rect(g, x, y, scale, part.x + 1.1F, part.y + 1.4F, 1.8F, 1.2F, color);
    }

    private static void box(GuiGraphics g, int x, int y, int scale, float ux, float uy, float uw, float uh, int line, int fill) {
        int x0 = x + Math.round(ux * scale);
        int y0 = y + Math.round(uy * scale);
        int x1 = x + Math.round((ux + uw) * scale);
        int y1 = y + Math.round((uy + uh) * scale);
        g.fill(x0, y0, x1, y1, line);
        if (x1 - x0 > 2 && y1 - y0 > 2) g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill);
    }

    private static void rect(GuiGraphics g, int x, int y, int scale, float ux, float uy, float uw, float uh, int color) {
        int x0 = x + Math.round(ux * scale);
        int y0 = y + Math.round(uy * scale);
        int x1 = Math.max(x0 + 1, x + Math.round((ux + uw) * scale));
        int y1 = Math.max(y0 + 1, y + Math.round((uy + uh) * scale));
        g.fill(x0, y0, x1, y1, color);
    }

    private static int blend(int a, int b, float amount) {
        int r = Math.round(((a >> 16) & 0xFF) * (1.0F - amount) + ((b >> 16) & 0xFF) * amount);
        int gr = Math.round(((a >> 8) & 0xFF) * (1.0F - amount) + ((b >> 8) & 0xFF) * amount);
        int bl = Math.round((a & 0xFF) * (1.0F - amount) + (b & 0xFF) * amount);
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }

    /** The body part under the mouse, or null. */
    @Nullable
    public static Part partAt(int x, int y, int scale, double mouseX, double mouseY) {
        for (Part part : Part.values()) {
            int x0 = x + part.x * scale;
            int y0 = y + part.y * scale;
            if (mouseX >= x0 && mouseX < x0 + part.w * scale && mouseY >= y0 && mouseY < y0 + part.h * scale) return part;
        }
        return null;
    }
}
