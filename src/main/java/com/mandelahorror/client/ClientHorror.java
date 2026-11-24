package com.mandelahorror.client;

import com.mandelahorror.entity.AlternateEntity;
import com.mandelahorror.network.FxPacket;
import com.mandelahorror.network.SpeechPacket;
import com.mandelahorror.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/** All client-side horror state: intro queue, subtitles, screen effects, skin glitch. */
public final class ClientHorror {

    public record SpeechLine(String text, int duration, int style, long startTick) {}

    public static long ticks;

    // intro
    @Nullable private static String pendingIntro;
    private static int introWait;

    // subtitles
    @Nullable public static SpeechLine speech;

    // effects
    public static int redFlash, redFlashMax;
    public static int shake, shakeMax;
    public static int staticBurst, staticBurstMax;
    public static int heartbeat;
    public static int skinGlitch;
    public static float proximityStatic;

    // ------------------------------------------------------------------ packets

    public static void openPaper(InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack stack = mc.player.getItemInHand(hand);
        mc.setScreen(new PaperScreen(hand, stack));
    }

    public static void queueIntro(String playerName) {
        pendingIntro = playerName;
        introWait = 0;
    }

    public static void showSpeech(String text, int duration, int style, boolean tts) {
        speech = new SpeechLine(text, duration, style, ticks);
        if (style == SpeechPacket.STYLE_ALTERNATE) {
            playUi(ModSounds.WHISPER.get(), 0.7F + (float) Math.random() * 0.2F, 0.6F);
        } else {
            playUi(ModSounds.BEEP.get(), 0.8F, 0.5F);
        }
        if (tts) Tts.say(text, true);
    }

    public static void handleFx(int type, int duration) {
        switch (type) {
            case FxPacket.RED_FLASH -> { redFlash = duration; redFlashMax = Math.max(1, duration); }
            case FxPacket.SHAKE -> { shake = duration; shakeMax = Math.max(1, duration); }
            case FxPacket.STATIC_BURST -> {
                staticBurst = duration;
                staticBurstMax = Math.max(1, duration);
                playUi(ModSounds.STATIC.get(), 1.0F, 0.5F);
            }
            case FxPacket.HEARTBEAT -> heartbeat = duration;
            case FxPacket.SKIN_GLITCH -> skinGlitch = duration;
            default -> { }
        }
    }

    // ------------------------------------------------------------------ tick

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        if (redFlash > 0) redFlash--;
        if (shake > 0) shake--;
        if (staticBurst > 0) staticBurst--;
        if (skinGlitch > 0) skinGlitch--;
        if (heartbeat > 0) {
            if (heartbeat % 16 == 0) playUi(ModSounds.HEARTBEAT.get(), 1.0F, 0.9F);
            heartbeat--;
        }
        if (speech != null && ticks - speech.startTick() > speech.duration()) speech = null;

        if (mc.player == null || mc.level == null) return;

        // open the intro once the world has loaded and no other screen is open
        if (pendingIntro != null && mc.screen == null) {
            if (++introWait > 20) {
                String name = pendingIntro;
                pendingIntro = null;
                mc.setScreen(new IntroScreen(name));
            }
        }

        // static grows as the Alternate gets closer
        if (ticks % 5 == 0) {
            List<AlternateEntity> near = mc.level.getEntitiesOfClass(AlternateEntity.class, mc.player.getBoundingBox().inflate(24));
            double best = Double.MAX_VALUE;
            for (AlternateEntity a : near) best = Math.min(best, a.distanceTo(mc.player));
            proximityStatic = best == Double.MAX_VALUE ? 0F : Mth.clamp(1F - (float) best / 24F, 0F, 1F);
        }
    }

    public static void reset() {
        pendingIntro = null;
        speech = null;
        redFlash = shake = staticBurst = heartbeat = skinGlitch = 0;
        proximityStatic = 0F;
        Tts.stop();
    }

    public static float shakeStrength() {
        return shake <= 0 ? 0F : (float) shake / shakeMax;
    }

    // ------------------------------------------------------------------ skin glitch

    public static boolean isSkinGlitching() {
        return skinGlitch > 0;
    }

    /** A different default skin every few ticks, per player. */
    public static UUID glitchSeed(AbstractClientPlayer player) {
        return new UUID(player.getId() * 0x9E3779B97F4A7C15L + ticks / 6, 0x5EEDL);
    }

    public static ResourceLocation glitchSkin(AbstractClientPlayer player) {
        return DefaultPlayerSkin.getDefaultSkin(glitchSeed(player));
    }

    // ------------------------------------------------------------------ util

    public static void playUi(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private ClientHorror() {}
}
