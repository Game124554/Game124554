package com.mandelahorror.client;

import com.mandelahorror.entity.AlternateEntity;
import com.mandelahorror.item.PaperData;
import com.mandelahorror.network.PaperSavePacket;
import com.mandelahorror.registry.ModSounds;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * The Strange Paper screen: draw with the mouse (pencil / red pen / eraser, 3 sizes) and write text anywhere.
 * Everything is saved into the item when the screen closes.
 */
public class PaperScreen extends Screen {
    private static final int W = PaperData.WIDTH;
    private static final int H = PaperData.HEIGHT;
    private static final float TEXT_SCALE = 0.75F;
    private static final int LINE_STEP = 8;

    private static final int TOOL_PENCIL = 0;
    private static final int TOOL_RED = 1;
    private static final int TOOL_ERASER = 2;
    private static final int TOOL_TEXT = 3;

    private static final int PAPER_COLOR = 0xFFEFEBDC;
    private static final int INK_BLACK = 0xFF262626;
    private static final int INK_RED = 0xFFB41A1A;

    private static final String[] GHOST_LINES = {
            "it's behind you", "don't turn around", "i drew this", "you didn't write this", "look up"
    };

    private record Snapshot(byte[] pixels, List<PaperData.TextEntry> texts) {}

    private final InteractionHand hand;
    private final byte[] pixels;
    private final List<PaperData.TextEntry> texts;
    private final Deque<Snapshot> undoStack = new ArrayDeque<>();
    private final RandomSource random = RandomSource.create();

    private int tool = TOOL_PENCIL;
    private byte ink = PaperData.BLACK;
    private int brush = 1;
    private boolean drawing;
    private byte drawValue;
    private int lastX, lastY;
    private int editing = -1;
    private int ticks;
    private boolean saved;

    // canvas texture
    @Nullable private NativeImage image;
    @Nullable private DynamicTexture texture;
    @Nullable private ResourceLocation textureId;
    private boolean dirty = true;

    // layout
    private int left, top, dispW, dispH;
    private float scale = 1F;
    private final List<Button> toolButtons = new ArrayList<>();

    // something else writes on your paper sometimes
    @Nullable private String ghostText;
    private int ghostX, ghostY, ghostAt;

    public PaperScreen(InteractionHand hand, ItemStack stack) {
        super(Component.translatable("gui.mandelahorror.paper.title"));
        this.hand = hand;
        this.pixels = PaperData.readPixels(stack);
        this.texts = new ArrayList<>(PaperData.readTexts(stack));
        pickGhost();
    }

    // ------------------------------------------------------------------ setup

    @Override
    protected void init() {
        float s = Math.min((this.height - 36) / (float) H, (this.width - 120) / (float) W);
        this.scale = Math.max(0.5F, s);
        this.dispW = Math.round(W * this.scale);
        this.dispH = Math.round(H * this.scale);
        this.left = 104 + Math.max(0, (this.width - 104 - this.dispW) / 2);
        this.top = Math.max(16, (this.height - this.dispH) / 2 + 6);

        if (this.texture == null) {
            this.image = new NativeImage(W, H, true);
            this.texture = new DynamicTexture(this.image);
            this.textureId = this.minecraft.getTextureManager().register("mandelahorror_paper", this.texture);
            redrawAll();
            ClientHorror.playUi(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
        }

        int bx = this.left - 94;
        int bw = 84;
        int bh = 18;
        int y = this.top;
        this.toolButtons.clear();
        this.toolButtons.add(addRenderableWidget(Button.builder(Component.translatable("gui.mandelahorror.paper.pencil"),
                b -> selectTool(TOOL_PENCIL)).bounds(bx, y, bw, bh).build()));
        y += 21;
        this.toolButtons.add(addRenderableWidget(Button.builder(Component.translatable("gui.mandelahorror.paper.red"),
                b -> selectTool(TOOL_RED)).bounds(bx, y, bw, bh).build()));
        y += 21;
        this.toolButtons.add(addRenderableWidget(Button.builder(Component.translatable("gui.mandelahorror.paper.eraser"),
                b -> selectTool(TOOL_ERASER)).bounds(bx, y, bw, bh).build()));
        y += 21;
        this.toolButtons.add(addRenderableWidget(Button.builder(Component.translatable("gui.mandelahorror.paper.text"),
                b -> selectTool(TOOL_TEXT)).bounds(bx, y, bw, bh).build()));
        y += 25;
        addRenderableWidget(Button.builder(sizeLabel(), b -> {
            this.brush = this.brush % 3 + 1;
            b.setMessage(sizeLabel());
            this.setFocused(null);
        }).bounds(bx, y, bw, bh).build());
        y += 25;
        addRenderableWidget(Button.builder(Component.translatable("gui.mandelahorror.paper.undo"), b -> {
            undo();
            this.setFocused(null);
        }).bounds(bx, y, bw, bh).build());
        y += 21;
        addRenderableWidget(Button.builder(Component.translatable("gui.mandelahorror.paper.clear"), b -> {
            clearAll();
            this.setFocused(null);
        }).bounds(bx, y, bw, bh).build());
        y += 25;
        addRenderableWidget(Button.builder(Component.translatable("gui.mandelahorror.paper.done"),
                b -> this.onClose()).bounds(bx, y, bw, bh).build());
    }

    private Component sizeLabel() {
        return Component.translatable("gui.mandelahorror.paper.size", this.brush);
    }

    private void selectTool(int t) {
        finishEditing();
        this.tool = t;
        if (t == TOOL_PENCIL) this.ink = PaperData.BLACK;
        if (t == TOOL_RED) this.ink = PaperData.RED;
        this.setFocused(null);
    }

    private void pickGhost() {
        Minecraft mc = Minecraft.getInstance();
        boolean near = mc.player != null && mc.level != null
                && !mc.level.getEntitiesOfClass(AlternateEntity.class, mc.player.getBoundingBox().inflate(24)).isEmpty();
        if (this.random.nextFloat() < (near ? 0.6F : 0.12F)) {
            this.ghostText = GHOST_LINES[this.random.nextInt(GHOST_LINES.length)];
            this.ghostX = 8 + this.random.nextInt(40);
            this.ghostY = 96 + this.random.nextInt(50);
            this.ghostAt = 100 + this.random.nextInt(200);
        }
    }

    // ------------------------------------------------------------------ canvas

    private static int abgr(byte v) {
        return switch (v) {
            case PaperData.BLACK -> 0xFF262626;
            case PaperData.RED -> 0xFF1A1AB4; // NativeImage is ABGR: this is red
            default -> 0x00000000;
        };
    }

    private void redrawAll() {
        if (this.image == null) return;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                this.image.setPixelRGBA(x, y, abgr(this.pixels[y * W + x]));
            }
        }
        this.dirty = true;
    }

    private void setPixel(int x, int y, byte v) {
        if (x < 0 || y < 0 || x >= W || y >= H) return;
        int i = y * W + x;
        if (this.pixels[i] == v) return;
        this.pixels[i] = v;
        if (this.image != null) this.image.setPixelRGBA(x, y, abgr(v));
        this.dirty = true;
    }

    private void stamp(int cx, int cy) {
        int r = this.brush - 1 + (this.drawValue == PaperData.BLANK ? 1 : 0);
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                if (dx * dx + dy * dy <= r * r + r) setPixel(cx + dx, cy + dy, this.drawValue);
            }
        }
    }

    /** Bresenham line so fast mouse moves still draw a solid stroke. */
    private void line(int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        int guard = 0;
        while (guard++ < 2000) {
            stamp(x0, y0);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    private void pushUndo() {
        List<PaperData.TextEntry> copy = new ArrayList<>();
        for (PaperData.TextEntry e : this.texts) copy.add(e.copy());
        this.undoStack.push(new Snapshot(this.pixels.clone(), copy));
        while (this.undoStack.size() > 30) this.undoStack.removeLast();
    }

    private void undo() {
        this.editing = -1;
        Snapshot s = this.undoStack.poll();
        if (s == null) return;
        System.arraycopy(s.pixels(), 0, this.pixels, 0, this.pixels.length);
        this.texts.clear();
        this.texts.addAll(s.texts());
        redrawAll();
    }

    private void clearAll() {
        pushUndo();
        this.editing = -1;
        Arrays.fill(this.pixels, PaperData.BLANK);
        this.texts.clear();
        redrawAll();
    }

    // ------------------------------------------------------------------ text

    private void finishEditing() {
        if (this.editing >= 0 && this.editing < this.texts.size() && this.texts.get(this.editing).text.isEmpty()) {
            this.texts.remove(this.editing);
        }
        this.editing = -1;
    }

    private int textAt(int cx, int cy) {
        for (int i = this.texts.size() - 1; i >= 0; i--) {
            PaperData.TextEntry e = this.texts.get(i);
            float w = this.font.width(e.text) * TEXT_SCALE;
            float h = 9 * TEXT_SCALE;
            if (cx >= e.x - 1 && cx <= e.x + w + 1 && cy >= e.y - 1 && cy <= e.y + h + 1) return i;
        }
        return -1;
    }

    private void clickText(int cx, int cy) {
        finishEditing();
        int hit = textAt(cx, cy);
        if (hit >= 0) {
            this.editing = hit;
            return;
        }
        if (this.texts.size() >= PaperData.MAX_TEXTS) return;
        pushUndo();
        this.texts.add(new PaperData.TextEntry(Mth.clamp(cx, 0, W - 8), Mth.clamp(cy - 3, 0, H - 7), "", this.ink));
        this.editing = this.texts.size() - 1;
    }

    /** Enter (or running out of space) starts a new line under the current one. */
    private boolean newLine() {
        if (this.editing < 0 || this.editing >= this.texts.size()) return false;
        PaperData.TextEntry cur = this.texts.get(this.editing);
        int ny = cur.y + LINE_STEP;
        if (ny > H - 7 || this.texts.size() >= PaperData.MAX_TEXTS) return false;
        this.texts.add(new PaperData.TextEntry(cur.x, ny, "", cur.color));
        if (cur.text.isEmpty()) this.texts.remove(this.editing);
        this.editing = this.texts.size() - 1;
        return true;
    }

    private void typeChar(char c) {
        if (this.editing < 0 || this.editing >= this.texts.size()) return;
        PaperData.TextEntry e = this.texts.get(this.editing);
        String next = e.text + c;
        if (next.length() > PaperData.MAX_TEXT_LENGTH || e.x + this.font.width(next) * TEXT_SCALE > W - 2) {
            if (!newLine() || c == ' ') return;
            e = this.texts.get(this.editing);
            next = e.text + c;
        }
        e.text = next;
    }

    // ------------------------------------------------------------------ input

    private boolean onPaper(double mx, double my) {
        return mx >= this.left && my >= this.top && mx < this.left + this.dispW && my < this.top + this.dispH;
    }

    private int canvasX(double mx) { return (int) Math.floor((mx - this.left) / this.scale); }
    private int canvasY(double my) { return (int) Math.floor((my - this.top) / this.scale); }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (onPaper(mx, my) && (button == 0 || button == 1)) {
            int cx = canvasX(mx);
            int cy = canvasY(my);
            if (this.tool == TOOL_TEXT) {
                if (button == 0) {
                    clickText(cx, cy);
                    return true;
                }
                int hit = textAt(cx, cy);
                if (hit >= 0) { // right-click a text to delete it
                    finishEditing();
                    hit = textAt(cx, cy);
                    if (hit >= 0) {
                        pushUndo();
                        this.texts.remove(hit);
                    }
                    return true;
                }
            }
            finishEditing();
            pushUndo();
            this.drawing = true;
            this.drawValue = (button == 1 || this.tool == TOOL_ERASER) ? PaperData.BLANK
                    : (this.tool == TOOL_RED ? PaperData.RED : PaperData.BLACK);
            this.lastX = cx;
            this.lastY = cy;
            stamp(cx, cy);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (this.drawing) {
            int cx = canvasX(mx);
            int cy = canvasY(my);
            line(this.lastX, this.lastY, cx, cy);
            this.lastX = cx;
            this.lastY = cy;
            return true;
        }
        return super.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (this.drawing) {
            this.drawing = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (this.editing >= 0 && SharedConstants.isAllowedChatCharacter(c)) {
            typeChar(c);
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (this.editing >= 0 && this.editing < this.texts.size()) {
            PaperData.TextEntry e = this.texts.get(this.editing);
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!e.text.isEmpty()) {
                    e.text = e.text.substring(0, e.text.length() - 1);
                } else {
                    this.texts.remove(this.editing);
                    this.editing = -1;
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                newLine();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                finishEditing();
                return true;
            }
            if (key == GLFW.GLFW_KEY_V && hasControlDown()) {
                String clip = this.minecraft.keyboardHandler.getClipboard();
                for (char c : clip.toCharArray()) {
                    if (SharedConstants.isAllowedChatCharacter(c)) typeChar(c);
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_Z && hasControlDown()) {
                undo();
                return true;
            }
            return true; // swallow other keys while typing (letters arrive through charTyped)
        }
        if (key == GLFW.GLFW_KEY_Z && hasControlDown()) {
            undo();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.ghostText != null && this.ticks == this.ghostAt) {
            ClientHorror.playUi(ModSounds.WHISPER.get(), 0.8F, 0.4F);
        }
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        // the paper
        g.fill(this.left + 4, this.top + 4, this.left + this.dispW + 4, this.top + this.dispH + 4, 0x88000000);
        g.fill(this.left, this.top, this.left + this.dispW, this.top + this.dispH, PAPER_COLOR);
        for (int ly = 24; ly < H; ly += 12) {
            int yy = this.top + Math.round(ly * this.scale);
            g.fill(this.left + 2, yy, this.left + this.dispW - 2, yy + 1, 0x223060B0);
        }
        int margin = this.left + Math.round(7 * this.scale);
        g.fill(margin, this.top, margin + 1, this.top + this.dispH, 0x40C03030);

        // the drawing
        if (this.texture != null && this.textureId != null) {
            if (this.dirty) {
                this.texture.upload();
                this.dirty = false;
            }
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.blit(this.textureId, this.left, this.top, this.dispW, this.dispH, 0.0F, 0.0F, W, H, W, H);
            RenderSystem.disableBlend();
        }

        // the text
        for (int i = 0; i < this.texts.size(); i++) {
            PaperData.TextEntry e = this.texts.get(i);
            boolean active = i == this.editing;
            String s = e.text + (active && (this.ticks / 6) % 2 == 0 ? "_" : "");
            drawPaperText(g, s, e.x, e.y, e.color == PaperData.RED ? INK_RED : INK_BLACK, 0, 0);
        }
        drawGhost(g, partialTick);

        // hint above the paper
        Component hint = Component.translatable(this.tool == TOOL_TEXT
                ? "gui.mandelahorror.paper.hint_text" : "gui.mandelahorror.paper.hint_draw");
        g.drawCenteredString(this.font, hint, this.left + this.dispW / 2, this.top - 12, 0xFFBBBBBB);

        super.render(g, mouseX, mouseY, partialTick);

        // selected tool + ink swatches
        Button sel = this.toolButtons.get(this.tool);
        g.renderOutline(sel.getX() - 1, sel.getY() - 1, sel.getWidth() + 2, sel.getHeight() + 2, 0xFFFFD000);
        swatch(g, this.toolButtons.get(TOOL_PENCIL), INK_BLACK);
        swatch(g, this.toolButtons.get(TOOL_RED), INK_RED);
        swatch(g, this.toolButtons.get(TOOL_TEXT), this.ink == PaperData.RED ? INK_RED : INK_BLACK);

        // brush preview
        if (this.tool != TOOL_TEXT && onPaper(mouseX, mouseY)) {
            int r = this.brush - 1 + (this.tool == TOOL_ERASER ? 1 : 0);
            int cx = canvasX(mouseX);
            int cy = canvasY(mouseY);
            int x0 = this.left + Math.round((cx - r) * this.scale);
            int y0 = this.top + Math.round((cy - r) * this.scale);
            int size = Math.max(2, Math.round((2 * r + 1) * this.scale));
            g.renderOutline(x0, y0, size, size, this.tool == TOOL_ERASER ? 0xAA888888 : 0xAA000000);
        }
    }

    private void drawPaperText(GuiGraphics g, String s, int x, int y, int color, int jx, int jy) {
        g.pose().pushPose();
        g.pose().translate(this.left + x * this.scale + jx, this.top + y * this.scale + jy, 0);
        float ts = this.scale * TEXT_SCALE;
        g.pose().scale(ts, ts, 1F);
        g.drawString(this.font, s, 0, 0, color, false);
        g.pose().popPose();
    }

    private void drawGhost(GuiGraphics g, float partialTick) {
        if (this.ghostText == null || this.ticks < this.ghostAt) return;
        float f = Math.min(1F, (this.ticks - this.ghostAt + partialTick) / 80F);
        int alpha = (int) (f * 150);
        if (alpha < 6) return;
        int jx = this.random.nextInt(12) == 0 ? this.random.nextInt(3) - 1 : 0;
        drawPaperText(g, this.ghostText, this.ghostX, this.ghostY, (alpha << 24) | 0x7A0E0E, jx, 0);
    }

    private static void swatch(GuiGraphics g, Button b, int color) {
        int x = b.getX() + b.getWidth() - 10;
        int y = b.getY() + 6;
        g.fill(x, y, x + 6, y + 6, color);
    }

    // ------------------------------------------------------------------ close

    @Override
    public boolean isPauseScreen() {
        return false; // the world keeps going while you draw...
    }

    @Override
    public void removed() {
        finishEditing();
        if (!this.saved && this.minecraft != null && this.minecraft.getConnection() != null) {
            this.saved = true;
            List<PaperData.TextEntry> copy = new ArrayList<>();
            for (PaperData.TextEntry e : this.texts) copy.add(e.copy());
            ClientNetwork.sendPaper(new PaperSavePacket(this.hand, PaperData.pack(this.pixels), copy));
        }
        if (this.textureId != null) {
            this.minecraft.getTextureManager().release(this.textureId);
            this.textureId = null;
            this.texture = null;
            this.image = null;
        }
        super.removed();
    }
}
