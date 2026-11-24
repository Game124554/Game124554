package com.mandelahorror.client;

import com.mandelahorror.network.FxPacket;
import com.mandelahorror.network.IntroPacket;
import com.mandelahorror.network.ModNetwork;
import com.mandelahorror.network.PaperSavePacket;
import com.mandelahorror.network.SpeechPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

/** Client side of the networking: receive intro / speech / effects, send paper saves. */
public final class ClientNetwork {

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetwork.INTRO, (client, handler, buf, responseSender) -> {
            IntroPacket msg = IntroPacket.decode(buf);
            client.execute(() -> ClientHorror.queueIntro(msg.playerName()));
        });
        ClientPlayNetworking.registerGlobalReceiver(ModNetwork.SPEECH, (client, handler, buf, responseSender) -> {
            SpeechPacket msg = SpeechPacket.decode(buf);
            client.execute(() -> ClientHorror.showSpeech(msg.text(), msg.duration(), msg.style(), msg.tts()));
        });
        ClientPlayNetworking.registerGlobalReceiver(ModNetwork.FX, (client, handler, buf, responseSender) -> {
            FxPacket msg = FxPacket.decode(buf);
            client.execute(() -> ClientHorror.handleFx(msg.type(), msg.duration()));
        });
    }

    public static void sendPaper(PaperSavePacket msg) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        PaperSavePacket.encode(msg, buf);
        ClientPlayNetworking.send(ModNetwork.PAPER_SAVE, buf);
    }

    private ClientNetwork() {}
}
