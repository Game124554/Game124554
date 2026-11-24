package com.mandelahorror.network;

import com.mandelahorror.MandelaHorror;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Packet ids + server side of the networking. The client side is in client/ClientNetwork. */
public final class ModNetwork {
    public static final ResourceLocation INTRO = MandelaHorror.id("intro");
    public static final ResourceLocation SPEECH = MandelaHorror.id("speech");
    public static final ResourceLocation FX = MandelaHorror.id("fx");
    public static final ResourceLocation PAPER_SAVE = MandelaHorror.id("paper_save");

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(PAPER_SAVE, (server, player, handler, buf, responseSender) -> {
            PaperSavePacket msg = PaperSavePacket.decode(buf);
            server.execute(() -> PaperSavePacket.handle(msg, player));
        });
    }

    /** Server -> one player. */
    public static void send(ServerPlayer player, Object message) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        ResourceLocation id;
        if (message instanceof IntroPacket m) {
            IntroPacket.encode(m, buf);
            id = INTRO;
        } else if (message instanceof SpeechPacket m) {
            SpeechPacket.encode(m, buf);
            id = SPEECH;
        } else if (message instanceof FxPacket m) {
            FxPacket.encode(m, buf);
            id = FX;
        } else {
            throw new IllegalArgumentException("Unknown packet " + message);
        }
        ServerPlayNetworking.send(player, id, buf);
    }

    public static void sendFx(ServerPlayer player, int type, int duration) {
        send(player, new FxPacket(type, duration));
    }

    private ModNetwork() {}
}
