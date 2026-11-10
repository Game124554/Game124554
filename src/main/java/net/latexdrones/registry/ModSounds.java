package net.latexdrones.registry;

import net.latexdrones.LatexDrones;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/** Sound events - the .ogg files are synthesised by tools/gen_sounds.py. */
public final class ModSounds {
    private ModSounds() {
    }

    public static final SoundEvent DRONE_AMBIENT = register("drone_ambient");
    public static final SoundEvent DRONE_HURT = register("drone_hurt");
    public static final SoundEvent DRONE_DEATH = register("drone_death");
    public static final SoundEvent DRONE_STEP = register("drone_step");
    public static final SoundEvent BEAST_GROWL = register("beast_growl");
    public static final SoundEvent BLOB_SQUISH = register("blob_squish");
    public static final SoundEvent HOVER_BUZZ = register("hover_buzz");
    public static final SoundEvent SCIENTIST_LAUGH = register("scientist_laugh");
    public static final SoundEvent SCIENTIST_HURT = register("scientist_hurt");
    public static final SoundEvent INJECT = register("inject");
    public static final SoundEvent DART_SHOOT = register("dart_shoot");
    public static final SoundEvent GOO_SPLAT = register("goo_splat");
    public static final SoundEvent FLAMETHROWER = register("flamethrower");
    public static final SoundEvent ZAP = register("zap");
    public static final SoundEvent TRANSFORM_START = register("transform_start");
    public static final SoundEvent TRANSFORM_COMPLETE = register("transform_complete");
    public static final SoundEvent HYPNOSIS = register("hypnosis");
    public static final SoundEvent ALARM = register("alarm");
    public static final SoundEvent HEARTBEAT = register("heartbeat");
    public static final SoundEvent CURE = register("cure");
    public static final SoundEvent TERMINAL_BEEP = register("terminal_beep");
    public static final SoundEvent TASK_COMPLETE = register("task_complete");
    public static final SoundEvent STANDBY = register("standby");
    public static final SoundEvent CORRECTION = register("correction");

    private static SoundEvent register(String name) {
        ResourceLocation id = LatexDrones.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    /** Forces the static initialiser to run. */
    public static void init() {
    }
}
