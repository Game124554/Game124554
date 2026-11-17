package com.thunderscience.changedtsc.entity;

import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.registry.ModEntities;
import com.thunderscience.changedtsc.registry.ModItems;
import com.thunderscience.changedtsc.util.LatexHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Fast, gravity-free bullet fired by TSC guns and guards. */
public class BulletEntity extends ThrowableItemProjectile {
    private float damage = 5.0F;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
    }

    public BulletEntity(Level level, LivingEntity shooter) {
        super(ModEntities.BULLET.get(), shooter, level);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BULLET.get();
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > 60) this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) return;
        Entity target = result.getEntity();
        Entity owner = this.getOwner();

        // NPC bullets never hurt friends (other humans, friendly Puro, players they are not fighting)
        if (owner instanceof AbstractTSCHuman human) {
            if (target instanceof AbstractTSCHuman || LatexHelper.isFriendlyLatex(target)) return;
            if (target instanceof Player && human.getTarget() != target) return;
        }

        float dmg = this.damage;
        if (LatexHelper.isLatex(target)) dmg *= TSCConfig.BULLET_LATEX_MULTIPLIER.get().floatValue();
        if (target.hurt(this.damageSources().thrown(this, owner), dmg) && target instanceof LivingEntity living) {
            living.invulnerableTime = 0; // allow shotgun pellets / rapid fire to stack
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 4; i++) {
                this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(),
                        (this.random.nextDouble() - 0.5) * 0.2, 0.1, (this.random.nextDouble() - 0.5) * 0.2);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("BulletDamage", this.damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BulletDamage")) this.damage = tag.getFloat("BulletDamage");
    }
}
