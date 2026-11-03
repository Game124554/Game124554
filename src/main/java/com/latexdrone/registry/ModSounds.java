package com.latexdrone.registry;

import com.latexdrone.LatexDroneMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    public static final SoundEvent LATEX_AMBIENT = reg("latex_ambient");
    public static final SoundEvent LATEX_HURT = reg("latex_hurt");
    public static final SoundEvent LATEX_DEATH = reg("latex_death");
    public static final SoundEvent DRONE_AMBIENT = reg("drone_ambient");
    public static final SoundEvent DRONE_HURT = reg("drone_hurt");
    public static final SoundEvent GOO_AMBIENT = reg("goo_ambient");
    public static final SoundEvent SCIENTIST_AMBIENT = reg("scientist_ambient");
    public static final SoundEvent SCIENTIST_HURT = reg("scientist_hurt");
    public static final SoundEvent SCIENTIST_DEATH = reg("scientist_death");
    public static final SoundEvent SYRINGE_INJECT = reg("syringe_inject");
    public static final SoundEvent SYRINGE_SHOOT = reg("syringe_shoot");
    public static final SoundEvent HYPNO_HUM = reg("hypno_hum");
    public static final SoundEvent TRANSFORM = reg("transform");
    public static final SoundEvent HEARTBEAT = reg("heartbeat");
    public static final SoundEvent PURIFIER_SHOT = reg("purifier_shot");
    public static final SoundEvent BLADE_SWING = reg("blade_swing");
    public static final SoundEvent POD_ACTIVATE = reg("pod_activate");
    public static final SoundEvent INFECT_HIT = reg("infect_hit");

    private static SoundEvent reg(String name) {
        Identifier id = LatexDroneMod.id(name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void init() {}
}
