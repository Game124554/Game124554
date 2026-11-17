package com.thunderscience.changedtsc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: "entity X is at goo stage N (black or white goo)". */
public class GooSyncPacket {
    private final int entityId;
    private final int stage;
    private final boolean white;

    public GooSyncPacket(int entityId, int stage, boolean white) {
        this.entityId = entityId;
        this.stage = stage;
        this.white = white;
    }

    public GooSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.stage = buf.readByte();
        this.white = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeByte(stage);
        buf.writeBoolean(white);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ClientGooData.set(entityId, stage, white);
        ctx.get().setPacketHandled(true);
    }
}
