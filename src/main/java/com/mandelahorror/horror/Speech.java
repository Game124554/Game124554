package com.mandelahorror.horror;

import com.mandelahorror.network.ModNetwork;
import com.mandelahorror.network.SpeechPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Sends "text speech" to a player: on-screen subtitles + optional text-to-speech voice. */
public final class Speech {

    /** The Alternate talks: big distorted subtitle + TTS voice. */
    public static void alternateSays(ServerPlayer player, String text) {
        alternateSays(player, text, true);
    }

    /** tts = false when the Alternate's voice clip is already playing, so the voices don't overlap. */
    public static void alternateSays(ServerPlayer player, String text, boolean tts) {
        ModNetwork.send(player, new SpeechPacket(text, 70 + text.length() * 2, SpeechPacket.STYLE_ALTERNATE, tts));
    }

    /** M.A.D. emergency broadcast banner + TTS voice. */
    public static void broadcast(ServerPlayer player, String text) {
        ModNetwork.send(player, new SpeechPacket(text, 100 + text.length() * 2, SpeechPacket.STYLE_BROADCAST, true));
    }

    /** A chat line that looks exactly like a real player talking: "<Name> message". */
    public static void fakeChat(ServerPlayer player, String name, String message) {
        player.sendSystemMessage(Component.translatable("chat.type.text", name, message));
    }

    private Speech() {}
}
