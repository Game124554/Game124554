package com.mandelahorror.network;

import net.minecraft.network.FriendlyByteBuf;

/** Server -> client: open the emergency broadcast intro screen. */
public record IntroPacket(String playerName) {

    public static void encode(IntroPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.playerName, 64);
    }

    public static IntroPacket decode(FriendlyByteBuf buf) {
        return new IntroPacket(buf.readUtf(64));
    }
}
