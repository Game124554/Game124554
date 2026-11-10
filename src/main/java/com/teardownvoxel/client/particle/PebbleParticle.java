package com.teardownvoxel.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

/** Small falling stone/gravel pebble that bounces once or twice and rests briefly. */
public class PebbleParticle extends SpriteBillboardParticle {
    private int bounces;

    protected PebbleParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
        super(world, x, y, z);
        this.velocityX = vx + (this.random.nextDouble() - 0.5) * 0.08;
        this.velocityY = vy + this.random.nextDouble() * 0.05;
        this.velocityZ = vz + (this.random.nextDouble() - 0.5) * 0.08;
        this.gravityStrength = 1.0f;
        this.velocityMultiplier = 0.98f;
        this.collidesWithWorld = true;
        this.maxAge = 30 + this.random.nextInt(30);
        this.scale = 0.06f + this.random.nextFloat() * 0.06f;
        float shade = 0.45f + this.random.nextFloat() * 0.35f;
        this.setColor(shade, shade * 0.97f, shade * 0.92f);
        this.setSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.onGround && bounces < 2 && Math.abs(this.velocityY) < 0.01) {
            bounces++;
            this.velocityY = 0.06 / bounces;
            this.velocityX *= 0.5;
            this.velocityZ *= 0.5;
        }
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
            return new PebbleParticle(world, x, y, z, vx, vy, vz, sprites);
        }
    }
}
