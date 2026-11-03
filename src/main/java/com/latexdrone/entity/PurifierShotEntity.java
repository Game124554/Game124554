package com.latexdrone.entity;

import com.latexdrone.infection.InfectionManager;
import com.latexdrone.registry.ModEffects;
import com.latexdrone.registry.ModEntities;
import com.latexdrone.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** Purifier Cannon bolt: melts latex creatures, cleanses infection and hypnosis from players. */
public class PurifierShotEntity extends ThrownItemEntity {

    public PurifierShotEntity(EntityType<? extends PurifierShotEntity> type, World world) {
        super(type, world);
    }

    public PurifierShotEntity(World world, LivingEntity owner) {
        super(ModEntities.PURIFIER_SHOT, owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.PURIFIER_CHARGE;
    }

    @Override
    protected float getGravity() {
        return 0.004f;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            this.getWorld().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        } else if (this.age > 60) {
            this.discard();
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);
        Entity target = hit.getEntity();
        if (target instanceof ServerPlayerEntity player) {
            InfectionManager.reduceInfection(player, 20.0f);
            player.removeStatusEffect(ModEffects.HYPNOSIS);
            return;
        }
        float damage = target instanceof LatexMobEntity ? 12.0f : 3.0f;
        target.damage(this.getDamageSources().thrown(this, this.getOwner()), damage);
        if (target instanceof LivingEntity living) living.removeStatusEffect(ModEffects.HYPNOSIS);
    }

    @Override
    protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        if (this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 20, 0.3, 0.3, 0.3, 0.1);
            sw.spawnParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.05);
            this.discard();
        }
    }
}
