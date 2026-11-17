package com.thunderscience.changedtsc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> one player: "your own goo infection is at N percent". Feeds the paper doll. */
public class GooPercentPacket {
    private final float percent;
    private final boolean white;

    public GooPercentPacket(float percent, boolean white) {
        this.percent = percent;
        this.white = white;
    }

    public GooPercentPacket(FriendlyByteBuf buf) {
        this.percent = buf.readFloat();
        this.white = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(percent);
        buf.writeBoolean(white);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ClientGooData.setSelf(percent, white);
        ctx.get().setPacketHandled(true);
    }
}
