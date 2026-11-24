package com.mandelahorror.client;

import com.mandelahorror.HorrorConfig;
import com.mandelahorror.MandelaHorror;
import com.mojang.text2speech.Narrator;

/**
 * Real text-to-speech using Minecraft's own narrator library (works even if the in-game Narrator option is OFF).
 * Windows: built-in voice. macOS: built-in voice. Linux: needs "flite" installed, otherwise silent.
 */
public final class Tts {
    private static Narrator narrator;
    private static boolean failed;

    public static void say(String text, boolean interrupt) {
        if (failed || text == null || text.isBlank()) return;
        try {
            if (!HorrorConfig.TEXT_TO_SPEECH.get()) return;
            if (narrator == null) narrator = Narrator.getNarrator();
            if (narrator.active()) narrator.say(text, interrupt);
        } catch (Throwable t) {
            failed = true;
            MandelaHorror.LOGGER.warn("Mandela Horror: text-to-speech is not available on this system", t);
        }
    }

    public static void stop() {
        try {
            if (narrator != null) narrator.clear();
        } catch (Throwable ignored) {
        }
    }

    private Tts() {}
}
