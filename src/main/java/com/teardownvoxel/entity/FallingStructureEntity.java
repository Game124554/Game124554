package com.teardownvoxel.entity;

import com.teardownvoxel.config.TVConfig;
import com.teardownvoxel.network.ModNetworking;
import com.teardownvoxel.registry.ModEntities;
import com.teardownvoxel.registry.ModParticles;
import com.teardownvoxel.world.DebrisSpawner;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
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
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * One block of a structure that lost its support. It hangs for a short "creak" delay, falls as part of
 * the collapsing cluster, and shatters into {@link VoxelDebrisEntity} cubes on impact.
 */
public class FallingStructureEntity extends Entity {
    private static final TrackedData<BlockState> STATE = DataTracker.registerData(FallingStructureEntity.class, TrackedDataHandlerRegistry.BLOCK_STATE);
    private static final TrackedData<Integer> DELAY = DataTracker.registerData(FallingStructureEntity.class, TrackedDataHandlerRegistry.INTEGER);

    // Visual tumble.
    public float tiltX, tiltZ, prevTiltX, prevTiltZ;
    private final float tiltSpeedX, tiltSpeedZ;
    private double startY;
    private double lastVy;

    public FallingStructureEntity(EntityType<? extends FallingStructureEntity> type, World world) {
        super(type, world);
        this.tiltSpeedX = (this.random.nextFloat() - 0.5f) * 2.5f;
        this.tiltSpeedZ = (this.random.nextFloat() - 0.5f) * 2.5f;
    }

    public static FallingStructureEntity create(World world, BlockPos pos, BlockState state, int delay) {
        FallingStructureEntity e = new FallingStructureEntity(ModEntities.FALLING_STRUCTURE, world);
        e.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        e.dataTracker.set(STATE, state);
        e.dataTracker.set(DELAY, delay);
        e.startY = pos.getY();
        e.prevX = e.getX();
        e.prevY = e.getY();
        e.prevZ = e.getZ();
        return e;
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(STATE, Blocks.STONE.getDefaultState());
        this.dataTracker.startTracking(DELAY, 0);
    }

    public BlockState getBlockState() {
        return this.dataTracker.get(STATE);
    }

    public int getDelay() {
        return this.dataTracker.get(DELAY);
    }

    @Override
    public void tick() {
        super.tick();
        prevTiltX = tiltX;
        prevTiltZ = tiltZ;

        if (this.age < getDelay()) {
            // Creaking: slight shudder + dust before letting go.
            if (this.getWorld().isClient && this.random.nextInt(4) == 0) {
                this.getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, getBlockState()),
                        this.getX() + (this.random.nextDouble() - 0.5), this.getY(), this.getZ() + (this.random.nextDouble() - 0.5), 0, 0, 0);
            }
            return;
        }

        tiltX += tiltSpeedX;
        tiltZ += tiltSpeedZ;

        lastVy = this.getVelocity().y;
        double impact = SimplePhysics.step(this, 0.04, 0.98, 0.0, 0.5);

        if (this.getWorld().isClient) {
            if (this.random.nextInt(6) == 0) {
                this.getWorld().addParticle(ModParticles.PEBBLE, this.getX() + (this.random.nextDouble() - 0.5), this.getY() + this.random.nextDouble(),
                        this.getZ() + (this.random.nextDouble() - 0.5), 0, -0.05, 0);
            }
            return;
        }

        // Crush things underneath while falling.
        if (lastVy < -0.25) {
            for (Entity hit : this.getWorld().getOtherEntities(this, this.getBoundingBox().expand(0.05),
                    e -> e instanceof LivingEntity && e.isAlive())) {
                float dmg = (float) Math.min(20.0, (startY - this.getY()) * 1.5 + 2.0);
                hit.damage(this.getWorld().getDamageSources().fallingBlock(this), dmg);
            }
        }

        boolean landed = impact > 0.0 || (this.isOnGround() && this.age > getDelay() + 2);
        if (landed || this.age > getDelay() + 600 || this.getY() < this.getWorld().getBottomY() - 16) {
            shatter(Math.max(impact, -lastVy));
        }
    }

    private void shatter(double impactSpeed) {
        if (!(this.getWorld() instanceof ServerWorld world)) return;
        TVConfig cfg = TVConfig.get();
        BlockState state = getBlockState();
        BlockPos pos = this.getBlockPos();

        world.playSound(null, this.getX(), this.getY(), this.getZ(), state.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS,
                1.0f, 0.7f + this.random.nextFloat() * 0.3f);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), this.getX(), this.getY() + 0.3, this.getZ(),
                18, 0.35, 0.2, 0.35, 0.15);
        world.spawnParticles(ModParticles.DUST_CLOUD, this.getX(), this.getY() + 0.2, this.getZ(), 2, 0.4, 0.1, 0.4, 0.02);

        boolean placedAsRubble = false;
        if (impactSpeed < 0.9 && this.random.nextFloat() < cfg.rubbleChance) {
            BlockState here = world.getBlockState(pos);
            if (here.isReplaceable() && state.canPlaceAt(world, pos)) {
                world.setBlockState(pos, state, Block.NOTIFY_ALL);
                placedAsRubble = true;
            }
        }

        if (!placedAsRubble) {
            int count = 2 + (int) Math.min(5, impactSpeed * 4);
            Vec3d up = new Vec3d(0, Math.min(0.45, impactSpeed * 0.3), 0);
            DebrisSpawner.shatter(world, pos, state, up, count, 0.22);
            if (this.random.nextFloat() < cfg.shatterDropChance) {
                Block.dropStacks(state, world, pos);
            }
        }

        if (impactSpeed > 0.6 && this.random.nextInt(4) == 0) {
            ModNetworking.sendShake(world, this.getPos(), 0.8f, 24.0, 10);
        }
        this.discard();
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("BlockState")) {
            this.dataTracker.set(STATE, NbtHelper.toBlockState(this.getWorld().createCommandRegistryWrapper(RegistryKeys.BLOCK), nbt.getCompound("BlockState")));
        }
        startY = nbt.getDouble("StartY");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.put("BlockState", NbtHelper.fromBlockState(getBlockState()));
        nbt.putDouble("StartY", startY);
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }
}
