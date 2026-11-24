package com.mandelahorror.network;

import net.minecraft.network.FriendlyByteBuf;

/** Server -> client: screen effects. */
public record FxPacket(int type, int duration) {
    public static final int RED_FLASH = 0;
    public static final int SHAKE = 1;
    public static final int STATIC_BURST = 2;
    public static final int HEARTBEAT = 3;
    public static final int SKIN_GLITCH = 4;

    public static void encode(FxPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.type);
        buf.writeVarInt(msg.duration);
    }

    public static FxPacket decode(FriendlyByteBuf buf) {
        return new FxPacket(buf.readVarInt(), buf.readVarInt());
    }
}
