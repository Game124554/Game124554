package com.mandelahorror.network;

import com.mandelahorror.item.PaperData;
import com.mandelahorror.registry.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Client -> server: save the drawing + text of the Strange Paper held in this hand. */
public record PaperSavePacket(InteractionHand hand, byte[] packedPixels, List<PaperData.TextEntry> texts) {

    public static void encode(PaperSavePacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.hand);
        buf.writeByteArray(msg.packedPixels);
        int count = Math.min(msg.texts.size(), PaperData.MAX_TEXTS);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            PaperData.TextEntry e = msg.texts.get(i);
            buf.writeShort(e.x);
            buf.writeShort(e.y);
            String t = e.text.length() > PaperData.MAX_TEXT_LENGTH ? e.text.substring(0, PaperData.MAX_TEXT_LENGTH) : e.text;
            buf.writeUtf(t, PaperData.MAX_TEXT_LENGTH);
            buf.writeByte(e.color);
        }
    }

    public static PaperSavePacket decode(FriendlyByteBuf buf) {
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        byte[] packed = buf.readByteArray(PaperData.PACKED_SIZE);
        int count = Math.min(buf.readVarInt(), PaperData.MAX_TEXTS);
        List<PaperData.TextEntry> texts = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int x = buf.readShort();
            int y = buf.readShort();
            String t = buf.readUtf(PaperData.MAX_TEXT_LENGTH);
            byte c = buf.readByte();
            texts.add(new PaperData.TextEntry(x, y, t, c));
        }
        return new PaperSavePacket(hand, packed, texts);
    }

    /** Runs on the server thread. Only writes if the player really holds a Strange Paper in that hand. */
    public static void handle(PaperSavePacket msg, ServerPlayer player) {
        ItemStack stack = player.getItemInHand(msg.hand);
        if (stack.is(ModItems.STRANGE_PAPER)) {
            PaperData.write(stack, msg.packedPixels, msg.texts);
        }
    }
}
