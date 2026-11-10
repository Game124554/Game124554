package com.teardownvoxel.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

/** Bright, gravity-affected, bouncing spark that cools from white-yellow to deep orange. */
public class SparkParticle extends SpriteBillboardParticle {
    private final SpriteProvider sprites;

    protected SparkParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
        super(world, x, y, z);
        this.sprites = sprites;
        this.velocityX = vx;
        this.velocityY = vy + 0.05;
        this.velocityZ = vz;
        this.gravityStrength = 0.9f;
        this.velocityMultiplier = 0.94f;
        this.collidesWithWorld = true;
        this.maxAge = 10 + this.random.nextInt(14);
        this.scale = 0.05f + this.random.nextFloat() * 0.05f;
        this.setColor(1.0f, 0.95f, 0.7f);
        this.setSpriteForAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.onGround) {
            this.velocityY = -this.velocityY * 0.4;
            this.velocityX *= 0.6;
            this.velocityZ *= 0.6;
        }
        float t = (float) this.age / this.maxAge;
        this.setColor(1.0f, 0.95f - t * 0.55f, 0.7f - t * 0.65f);
        this.setSpriteForAge(sprites);
    }

    @Override
    public float getSize(float tickDelta) {
        return this.scale * (1.0f - ((float) this.age + tickDelta) / this.maxAge * 0.5f);
    }

    @Override
    protected int getBrightness(float tint) {
        return 0xF000F0;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
            return new SparkParticle(world, x, y, z, vx, vy, vz, sprites);
        }
    }
}
