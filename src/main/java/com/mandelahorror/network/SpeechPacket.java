package com.mandelahorror.network;

import net.minecraft.network.FriendlyByteBuf;

/** Server -> client: show a subtitle ("text speech") and optionally read it with text-to-speech. */
public record SpeechPacket(String text, int duration, int style, boolean tts) {
    public static final int STYLE_ALTERNATE = 0;
    public static final int STYLE_BROADCAST = 1;

    public static void encode(SpeechPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.text, 512);
        buf.writeVarInt(msg.duration);
        buf.writeVarInt(msg.style);
        buf.writeBoolean(msg.tts);
    }

    public static SpeechPacket decode(FriendlyByteBuf buf) {
        return new SpeechPacket(buf.readUtf(512), buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }
}
