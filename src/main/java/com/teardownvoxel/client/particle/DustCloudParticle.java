package com.teardownvoxel.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

/** Large, slow, billowing dust cloud that expands and fades. */
public class DustCloudParticle extends SpriteBillboardParticle {
    private final float startScale;

    protected DustCloudParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
        super(world, x, y, z);
        this.velocityX = vx + (this.random.nextDouble() - 0.5) * 0.03;
        this.velocityY = vy * 0.5 + 0.01;
        this.velocityZ = vz + (this.random.nextDouble() - 0.5) * 0.03;
        this.gravityStrength = -0.004f;
        this.velocityMultiplier = 0.92f;
        this.collidesWithWorld = true;
        this.maxAge = 60 + this.random.nextInt(60);
        this.startScale = 0.6f + this.random.nextFloat() * 0.6f;
        this.scale = startScale;
        float grey = 0.55f + this.random.nextFloat() * 0.2f;
        this.setColor(grey + 0.05f, grey, grey - 0.06f);
        this.setAlpha(0.65f);
        this.angle = this.random.nextFloat() * 6.283f;
        this.prevAngle = this.angle;
        this.setSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float t = (float) this.age / this.maxAge;
        this.scale = startScale * (1.0f + t * 1.8f);
        this.setAlpha(0.65f * (1.0f - t) * (1.0f - t));
        this.prevAngle = this.angle;
        this.angle += 0.01f;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
            return new DustCloudParticle(world, x, y, z, vx, vy, vz, sprites);
        }
    }
}
