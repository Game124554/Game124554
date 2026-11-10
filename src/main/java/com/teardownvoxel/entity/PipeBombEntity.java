package com.teardownvoxel.entity;

import com.teardownvoxel.config.TVConfig;
import com.teardownvoxel.explosion.ShockwaveExplosion;
import com.teardownvoxel.registry.ModEntities;
import com.teardownvoxel.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A thrown or placed pipe bomb. It bounces around with real physics while its fuse burns,
 * then detonates as a {@link ShockwaveExplosion}.
 */
public class PipeBombEntity extends Entity implements FlyingItemEntity {
    private static final TrackedData<Integer> FUSE = DataTracker.registerData(PipeBombEntity.class, TrackedDataHandlerRegistry.INTEGER);
    @Nullable
    private UUID ownerUuid;

    public PipeBombEntity(EntityType<? extends PipeBombEntity> type, World world) {
        super(type, world);
    }

    public static PipeBombEntity create(World world, @Nullable LivingEntity owner, double x, double y, double z, int fuse) {
        PipeBombEntity e = new PipeBombEntity(ModEntities.PIPE_BOMB, world);
        e.setPosition(x, y, z);
        e.setFuse(fuse);
        if (owner != null) e.ownerUuid = owner.getUuid();
        return e;
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(FUSE, 50);
    }

    public int getFuse() {
        return this.dataTracker.get(FUSE);
    }

    public void setFuse(int fuse) {
        this.dataTracker.set(FUSE, fuse);
    }

    @Nullable
    public Entity getOwner() {
        if (ownerUuid != null && this.getWorld() instanceof ServerWorld sw) {
            return sw.getEntity(ownerUuid);
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        double impact = SimplePhysics.step(this, 0.05, 0.98, 0.45, 0.6);

        if (!this.getWorld().isClient && impact > 0.15) {
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_CHAIN_HIT, SoundCategory.NEUTRAL,
                    (float) Math.min(1.0, impact * 2), 1.4f + this.random.nextFloat() * 0.3f);
        }

        int fuse = getFuse() - 1;
        setFuse(fuse);

        if (this.getWorld().isClient) {
            this.getWorld().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.25, this.getZ(), 0, 0.02, 0);
            if (this.age % 2 == 0) {
                this.getWorld().addParticle(ParticleTypes.SMALL_FLAME, this.getX(), this.getY() + 0.25, this.getZ(), 0, 0.01, 0);
            }
        } else {
            if (this.age % 10 == 0) {
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.HOSTILE, 0.4f, 1.8f);
            }
            if (fuse <= 0) {
                this.discard();
                ShockwaveExplosion.detonate((ServerWorld) this.getWorld(), this.getPos().add(0, 0.15, 0), TVConfig.get().pipeBombPower, getOwner(), this);
            }
        }
    }

    @Override
    public ItemStack getStack() {
        return new ItemStack(ModItems.PIPE_BOMB);
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        setFuse(nbt.getInt("Fuse"));
        if (nbt.containsUuid("Owner")) ownerUuid = nbt.getUuid("Owner");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Fuse", getFuse());
        if (ownerUuid != null) nbt.putUuid("Owner", ownerUuid);
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }
}
