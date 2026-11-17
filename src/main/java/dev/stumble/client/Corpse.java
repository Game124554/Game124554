package dev.stumble.client;

import dev.stumble.physics.Ragdoll;
import net.minecraft.client.CameraType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * One body in the world: a dead one (outlives its entity, fades after a while) or a living one
 * (stands in for a knocked-down player or mob until it gets up).
 */
final class Corpse {
    static final int FADE_TICKS = 20;

    final Ragdoll body;
    final BodyType type;
    final ResourceLocation texture;
    final float scale;
    int lifetime;
    /** The entity this body replaces on screen; null once that entity is gone. */
    Entity owner;
    int age;

    /** Still alive: will get up again. */
    boolean alive;
    /** This is you: the body drives your player. */
    boolean local;
    boolean gettingUp;
    boolean fallSent;
    /** Camera to go back to when you are up again, if we changed it. */
    CameraType savedCamera;

    Corpse(Ragdoll body, BodyType type, ResourceLocation texture, float scale, int lifetime, Entity owner) {
        this.body = body;
        this.type = type;
        this.texture = texture;
        this.scale = scale;
        this.lifetime = lifetime;
        this.owner = owner;
    }

    /** 1 while solid, dropping to 0 over the last second of a dead body. */
    float alpha() {
        if (alive) {
            return 1f;
        }
        int left = lifetime - age;
        return left >= FADE_TICKS ? 1f : Math.max(0f, left / (float) FADE_TICKS);
    }
}
