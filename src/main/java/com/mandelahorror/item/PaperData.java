package com.mandelahorror.item;

import com.mandelahorror.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * What is drawn and written on a Strange Paper, stored in the item's NBT.
 *  - "Pixels": 128 x 160 canvas, 2 bits per pixel (0 = blank, 1 = pencil, 2 = red)
 *  - "Texts" : list of {X, Y, T (text), C (color)}
 */
public final class PaperData {
    public static final int WIDTH = 128;
    public static final int HEIGHT = 160;
    public static final int PIXELS = WIDTH * HEIGHT;
    public static final int PACKED_SIZE = PIXELS / 4;
    public static final int MAX_TEXTS = 48;
    public static final int MAX_TEXT_LENGTH = 64;

    public static final byte BLANK = 0;
    public static final byte BLACK = 1;
    public static final byte RED = 2;

    /** One piece of text on the paper (position in canvas pixels). */
    public static final class TextEntry {
        public int x;
        public int y;
        public String text;
        public byte color;

        public TextEntry(int x, int y, String text, byte color) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
        }

        public TextEntry copy() {
            return new TextEntry(this.x, this.y, this.text, this.color);
        }
    }

    // ------------------------------------------------------------------ read

    /** Unpacked pixels, one byte per pixel. */
    public static byte[] readPixels(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Pixels", Tag.TAG_BYTE_ARRAY)) {
            byte[] packed = tag.getByteArray("Pixels");
            if (packed.length == PACKED_SIZE) return unpack(packed);
        }
        return new byte[PIXELS];
    }

    public static List<TextEntry> readTexts(ItemStack stack) {
        List<TextEntry> out = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("Texts", Tag.TAG_LIST)) return out;
        ListTag list = tag.getList("Texts", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && out.size() < MAX_TEXTS; i++) {
            CompoundTag t = list.getCompound(i);
            String text = t.getString("T");
            if (text.isEmpty()) continue;
            out.add(new TextEntry(t.getInt("X"), t.getInt("Y"), cut(text), t.getByte("C")));
        }
        return out;
    }

    public static boolean hasDrawing(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("Pixels", Tag.TAG_BYTE_ARRAY);
    }

    // ------------------------------------------------------------------ write

    public static void write(ItemStack stack, byte[] packed, List<TextEntry> texts) {
        CompoundTag tag = stack.getOrCreateTag();
        boolean empty = true;
        if (packed.length == PACKED_SIZE) {
            for (byte b : packed) {
                if (b != 0) { empty = false; break; }
            }
        }
        if (empty) tag.remove("Pixels");
        else tag.putByteArray("Pixels", packed);

        ListTag list = new ListTag();
        for (TextEntry e : texts) {
            if (list.size() >= MAX_TEXTS) break;
            if (e.text == null || e.text.isBlank()) continue;
            CompoundTag t = new CompoundTag();
            t.putInt("X", Mth.clamp(e.x, 0, WIDTH - 1));
            t.putInt("Y", Mth.clamp(e.y, 0, HEIGHT - 1));
            t.putString("T", cut(e.text));
            t.putByte("C", e.color == RED ? RED : BLACK);
            list.add(t);
        }
        if (list.isEmpty()) tag.remove("Texts");
        else tag.put("Texts", list);
        if (tag.isEmpty()) stack.setTag(null);
    }

    // ------------------------------------------------------------------ packing

    public static byte[] pack(byte[] pixels) {
        byte[] packed = new byte[PACKED_SIZE];
        for (int i = 0; i < PIXELS && i < pixels.length; i++) {
            int v = pixels[i] & 3;
            packed[i >> 2] |= (byte) (v << ((i & 3) * 2));
        }
        return packed;
    }

    public static byte[] unpack(byte[] packed) {
        byte[] pixels = new byte[PIXELS];
        for (int i = 0; i < PIXELS; i++) {
            int v = (packed[i >> 2] >> ((i & 3) * 2)) & 3;
            pixels[i] = (byte) (v == 3 ? 1 : v);
        }
        return pixels;
    }

    private static String cut(String s) {
        return s.length() > MAX_TEXT_LENGTH ? s.substring(0, MAX_TEXT_LENGTH) : s;
    }

    // ------------------------------------------------------------------ the Alternate's note

    /** Paper dropped by a dead Alternate: two staring eyes, a grin, and a message. */
    public static ItemStack alternateNote(String killerName) {
        ItemStack stack = new ItemStack(ModItems.STRANGE_PAPER);
        byte[] px = new byte[PIXELS];
        eye(px, 42, 52);
        eye(px, 86, 52);
        // grin
        for (int x = 30; x <= 98; x++) {
            double t = (x - 64) / 34.0;
            int y = (int) Math.round(84 + 10 * (1 - t * t));
            set(px, x, y, RED);
            set(px, x, y + 1, RED);
        }
        for (int x = 36; x <= 92; x += 4) {
            double t = (x - 64) / 34.0;
            int y = (int) Math.round(84 + 10 * (1 - t * t));
            for (int dy = -3; dy <= -1; dy++) set(px, x, y + dy, BLACK);
        }
        List<TextEntry> texts = new ArrayList<>();
        texts.add(new TextEntry(10, 112, "THERE ARE MORE OF US", RED));
        texts.add(new TextEntry(10, 124, "IT KNOWS YOUR NAME", BLACK));
        texts.add(new TextEntry(10, 136, killerName + ".", BLACK));
        write(stack, pack(px), texts);
        return stack;
    }

    private static void eye(byte[] px, int cx, int cy) {
        for (int y = -10; y <= 10; y++) {
            for (int x = -16; x <= 16; x++) {
                double d = (x * x) / 256.0 + (y * y) / 100.0;
                if (d <= 1.0 && d >= 0.62) set(px, cx + x, cy + y, BLACK); // outline
            }
        }
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) set(px, cx + x, cy + y, BLACK); // tiny pupil
        }
    }

    private static void set(byte[] px, int x, int y, byte v) {
        if (x >= 0 && y >= 0 && x < WIDTH && y < HEIGHT) px[y * WIDTH + x] = v;
    }

    private PaperData() {}
}
