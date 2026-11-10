package net.latexdrones.infection;

import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.latexdrones.LatexDrones;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Server -&gt; client sync of a player's infection state (the receiver is registered in LatexDronesClient). */
public final class ModNetworking {
    private ModNetworking() {
    }

    public static final ResourceLocation SYNC = LatexDrones.id("sync");

    /** Tells {@code to} about the infection state of player {@code about}. */
    public static void send(ServerPlayer to, UUID about, InfectionData.Entry e) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(about);
        buf.writeVarInt(e.infection);
        buf.writeBoolean(e.transformed);
        buf.writeUtf(e.form);
        buf.writeVarInt(e.transformTicks);
        buf.writeVarInt(e.unit);
        ServerPlayNetworking.send(to, SYNC, buf);
    }
}
