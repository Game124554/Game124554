package com.mandelahorror.registry;

import com.mandelahorror.MandelaHorror;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

public final class ModSounds {

    public static final Holder STATIC = reg("static");
    public static final Holder EAS_TONE = reg("eas_tone");
    public static final Holder DRONE = reg("drone");
    public static final Holder WHISPER = reg("whisper");
    public static final Holder BEEP = reg("beep");
    public static final Holder STAB = reg("stab");
    public static final Holder HEARTBEAT = reg("heartbeat");
    /** Your voice clip ("nothing is worth a risk"), spoken by the Alternate. */
    public static final Holder ALTERNATE_VOICE = reg("alternate_voice");

    /** Keeps the same .get() style as the NeoForge version. */
    public static final class Holder implements Supplier<SoundEvent> {
        private final SoundEvent event;

        private Holder(SoundEvent event) {
            this.event = event;
        }

        @Override
        public SoundEvent get() {
            return this.event;
        }
    }

    private static Holder reg(String name) {
        ResourceLocation id = MandelaHorror.id(name);
        return new Holder(Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id)));
    }

    /** Loading this class registers the sounds. */
    public static void register() {
    }

    private ModSounds() {}
}
