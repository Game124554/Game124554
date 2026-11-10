package com.teardownvoxel.entity;

import com.teardownvoxel.config.TVConfig;
import com.teardownvoxel.registry.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

import java.util.List;

/**
 * A small physical cube of a destroyed block. It renders a fragment of the source block's texture,
 * bounces, slides, can be kicked around by players and hurts things when it hits them fast.
 * After {@link TVConfig#debrisLifetimeTicks} it shrinks away.
 */
public class VoxelDebrisEntity extends Entity {
    private static final TrackedData<BlockState> STATE = DataTracker.registerData(VoxelDebrisEntity.class, TrackedDataHandlerRegistry.BLOCK_STATE);
    private static final TrackedData<Float> SCALE = DataTracker.registerData(VoxelDebrisEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(VoxelDebrisEntity.class, TrackedDataHandlerRegistry.INTEGER);

    // Client-side spin, purely visual.
    public float spinX, spinZ, prevSpinX, prevSpinZ;
    private float spinSpeedX, spinSpeedZ;
    private int settledTicks;
    private boolean hurtSomething;

    public VoxelDebrisEntity(EntityType<? extends VoxelDebrisEntity> type, World world) {
        super(type, world);
        this.spinSpeedX = (this.random.nextFloat() - 0.5f) * 40f;
        this.spinSpeedZ = (this.random.nextFloat() - 0.5f) * 40f;
        this.spinX = this.random.nextFloat() * 360f;
        this.spinZ = this.random.nextFloat() * 360f;
    }

    public static VoxelDebrisEntity create(World world, BlockState state, double x, double y, double z, float scale) {
        VoxelDebrisEntity e = new VoxelDebrisEntity(ModEntities.VOXEL_DEBRIS, world);
        e.setBlockState(state);
        e.setScale(scale);
        e.setPosition(x, y, z);
        e.dataTracker.set(LIFETIME, TVConfig.get().debrisLifetimeTicks + world.random.nextInt(40));
        return e;
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(STATE, Blocks.STONE.getDefaultState());
        this.dataTracker.startTracking(SCALE, 0.35f);
        this.dataTracker.startTracking(LIFETIME, 300);
    }

    public BlockState getBlockState() {
        return this.dataTracker.get(STATE);
    }

    public void setBlockState(BlockState state) {
        this.dataTracker.set(STATE, state);
    }

    public float getScale() {
        return this.dataTracker.get(SCALE);
    }

    public void setScale(float scale) {
        this.dataTracker.set(SCALE, scale);
        this.calculateDimensions();
    }

    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }

    /** 1.0 while alive, shrinking to 0 during the last second. */
    public float getFade(float tickDelta) {
        float remaining = getLifetime() - (this.age + tickDelta);
        return Math.max(0f, Math.min(1f, remaining / 20f));
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (SCALE.equals(data)) {
            this.calculateDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        float s = Math.max(0.1f, getScale());
        return EntityDimensions.changing(s, s);
    }

    @Override
    public void tick() {
        super.tick();
        TVConfig cfg = TVConfig.get();

        double impact = SimplePhysics.step(this, 0.045, 0.985, cfg.debrisBounciness, cfg.debrisGroundFriction);

        double speedSq = this.getVelocity().lengthSquared();
        // Visual spin slows down when resting.
        prevSpinX = spinX;
        prevSpinZ = spinZ;
        if (this.isOnGround()) {
            spinSpeedX *= 0.6f;
            spinSpeedZ *= 0.6f;
            if (impact > 0.2) {
                spinSpeedX += (this.random.nextFloat() - 0.5f) * 30f * (float) impact;
                spinSpeedZ += (this.random.nextFloat() - 0.5f) * 30f * (float) impact;
            }
        }
        spinX += spinSpeedX;
        spinZ += spinSpeedZ;
        if (this.isOnGround() && Math.abs(spinSpeedX) < 3f && Math.abs(spinSpeedZ) < 3f) {
            // Topple flat onto a face when resting.
            spinX += (Math.round(spinX / 90f) * 90f - spinX) * 0.35f;
            spinZ += (Math.round(spinZ / 90f) * 90f - spinZ) * 0.35f;
        }

        if (!this.getWorld().isClient) {
            // Hurt / shove living things when moving fast.
            if (!hurtSomething && speedSq > 0.16) {
                List<Entity> hits = this.getWorld().getOtherEntities(this, this.getBoundingBox(),
                        e -> e instanceof LivingEntity && e.isAlive());
                for (Entity hit : hits) {
                    float dmg = (float) Math.min(6.0, Math.sqrt(speedSq) * getScale() * 6.0);
                    if (dmg >= 0.5f) {
                        hit.damage(this.getWorld().getDamageSources().fallingBlock(this), dmg);
                    }
                    hit.addVelocity(this.getVelocity().x * 0.15, 0.05, this.getVelocity().z * 0.15);
                    this.setVelocity(this.getVelocity().multiply(-0.3));
                    hurtSomething = true;
                    break;
                }
            }

            if (this.isOnGround() && speedSq < 1.0E-4) {
                settledTicks++;
            } else {
                settledTicks = 0;
            }

            if (this.age >= getLifetime() || this.getY() < this.getWorld().getBottomY() - 32) {
                this.discard();
            }
        }
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    public boolean isSettled() {
        return settledTicks > 10;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("BlockState")) {
            setBlockState(NbtHelper.toBlockState(this.getWorld().createCommandRegistryWrapper(RegistryKeys.BLOCK), nbt.getCompound("BlockState")));
        }
        if (nbt.contains("Scale")) setScale(nbt.getFloat("Scale"));
        if (nbt.contains("Lifetime")) this.dataTracker.set(LIFETIME, nbt.getInt("Lifetime"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.put("BlockState", NbtHelper.fromBlockState(getBlockState()));
        nbt.putFloat("Scale", getScale());
        nbt.putInt("Lifetime", getLifetime());
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }
}
