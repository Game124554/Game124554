package com.teardownvoxel.explosion;

import com.teardownvoxel.block.NitroChargeBlock;
import com.teardownvoxel.config.TVConfig;
import com.teardownvoxel.network.ModNetworking;
import com.teardownvoxel.registry.ModParticles;
import com.teardownvoxel.registry.ModTags;
import com.teardownvoxel.world.DebrisSpawner;
import com.teardownvoxel.world.ServerScheduler;
import com.teardownvoxel.world.StructuralIntegrity;
import com.teardownvoxel.world.VoxelDamage;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TntBlock;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Raycasted shockwave used by pipe bombs and nitro charges.
 * <p>
 * Rays are marched outward from the centre; each ray loses energy with distance and with the blast
 * resistance of every block it passes. Blocks a ray passes with energy left are shredded into flying
 * voxel debris; the block where a ray finally stops forms the crater wall and gets charred.
 */
public final class ShockwaveExplosion {
    private ShockwaveExplosion() {
    }

    public static void detonate(ServerWorld world, Vec3d center, float power, @Nullable Entity owner, @Nullable Entity source) {
        TVConfig cfg = TVConfig.get();
        Random r = world.random;

        LongOpenHashSet destroyed = new LongOpenHashSet();
        LongOpenHashSet crater = new LongOpenHashSet();
        BlockPos.Mutable m = new BlockPos.Mutable();

        // --- 1. Raycast shockwave (16^3 shell of directions, like vanilla but with crater tracking) ---
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                for (int k = 0; k < 16; k++) {
                    if (i != 0 && i != 15 && j != 0 && j != 15 && k != 0 && k != 15) continue;
                    double dx = i / 15.0 * 2.0 - 1.0;
                    double dy = j / 15.0 * 2.0 - 1.0;
                    double dz = k / 15.0 * 2.0 - 1.0;
                    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    dx /= len;
                    dy /= len;
                    dz /= len;
                    float energy = power * (0.7f + r.nextFloat() * 0.6f);
                    double x = center.x, y = center.y, z = center.z;
                    while (energy > 0.0f) {
                        m.set(x, y, z);
                        if (!world.isInBuildLimit(m)) break;
                        BlockState state = world.getBlockState(m);
                        FluidState fluid = world.getFluidState(m);
                        if (!state.isAir() || !fluid.isEmpty()) {
                            float res = Math.max(state.getBlock().getBlastResistance(), fluid.getBlastResistance());
                            if (state.isIn(ModTags.INDESTRUCTIBLE) || state.getHardness(world, m) < 0) res = 3_600_000f;
                            energy -= (res + 0.3f) * 0.3f;
                            if (energy > 0.0f && !state.isAir() && fluid.isEmpty()) {
                                destroyed.add(m.asLong());
                            } else if (!state.isAir()) {
                                crater.add(m.asLong());
                                break;
                            }
                        }
                        x += dx * 0.3;
                        y += dy * 0.3;
                        z += dz * 0.3;
                        energy -= 0.22500001f;
                    }
                }
            }
        }
        crater.removeAll(destroyed);

        // --- 2. Entities: damage + knockback ---
        DamageSource damageSource = world.getDamageSources().explosion(source, owner);
        double radius = power * 2.0;
        Box box = new Box(center.x - radius - 1, center.y - radius - 1, center.z - radius - 1,
                center.x + radius + 1, center.y + radius + 1, center.z + radius + 1);
        for (Entity e : world.getOtherEntities(source, box)) {
            if (e.isImmuneToExplosion()) continue;
            double dist = Math.sqrt(e.squaredDistanceTo(center)) / radius;
            if (dist > 1.0) continue;
            Vec3d dir = e.getEyePos().subtract(center);
            if (dir.lengthSquared() < 1.0E-6) dir = new Vec3d(0, 1, 0);
            dir = dir.normalize();
            double exposure = Explosion.getExposure(center, e);
            double impact = (1.0 - dist) * exposure;
            e.damage(damageSource, (float) ((impact * impact + impact) / 2.0 * 7.0 * radius + 1.0));
            double kb = impact;
            if (e instanceof LivingEntity living) {
                kb = ProtectionEnchantment.transformExplosionKnockback(living, impact);
            }
            if (e instanceof PlayerEntity p && (p.isSpectator() || (p.isCreative() && p.getAbilities().flying))) continue;
            e.addVelocity(dir.x * kb, dir.y * kb * 0.8 + 0.1 * kb, dir.z * kb);
            e.velocityModified = true;
        }

        // --- 3. Shred blocks into flying debris ---
        List<BlockPos> list = new ArrayList<>(destroyed.size());
        for (long l : destroyed) list.add(BlockPos.fromLong(l));
        list.sort(Comparator.comparingDouble(p -> p.getSquaredDistance(center)));

        int debrisBudget = Math.min(cfg.maxDebrisPerExplosion, DebrisSpawner.remainingBudget(world));
        double reach = Math.max(1.0, power * 1.8);
        List<BlockPos> actuallyDestroyed = new ArrayList<>();
        for (BlockPos pos : list) {
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) continue;
            Vec3d c = Vec3d.ofCenter(pos);

            if (state.getBlock() instanceof NitroChargeBlock) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                int delay = 2 + r.nextInt(4);
                ServerScheduler.schedule(world, delay, w -> detonate(w, c, cfg.nitroPower, owner, null));
                continue;
            }
            if (state.getBlock() instanceof TntBlock) {
                TntBlock.primeTnt(world, pos);
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                continue;
            }

            if (state.hasBlockEntity()) {
                world.breakBlock(pos, true);
            } else {
                if (r.nextFloat() < 0.2f) Block.dropStacks(state, world, pos);
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }
            VoxelDamage.clearAt(world, pos);
            actuallyDestroyed.add(pos);

            if (debrisBudget > 0) {
                Vec3d away = c.subtract(center);
                double d = away.length();
                away = d < 1.0E-4 ? new Vec3d(0, 1, 0) : away.multiply(1.0 / d);
                double falloff = MathHelper.clamp(1.0 - d / reach, 0.15, 1.0);
                double speed = (0.25 + 0.75 * falloff) * Math.min(2.0, power / 3.0);
                Vec3d impulse = away.multiply(speed).add(0, 0.15 + 0.25 * falloff, 0);
                int n = Math.min(debrisBudget, 1 + r.nextInt(3));
                debrisBudget -= DebrisSpawner.shatter(world, pos, state, impulse, n, 0.12);
            }
        }

        // --- 4. Char the crater walls ---
        if (cfg.charring) {
            for (long l : crater) {
                if (r.nextFloat() > 0.65f) continue;
                BlockPos pos = BlockPos.fromLong(l);
                BlockState state = world.getBlockState(pos);
                if (state.isAir() || state.hasBlockEntity()) continue;
                BlockState charred = Charring.charred(state, r);
                if (charred != null) {
                    world.setBlockState(pos, charred, Block.NOTIFY_ALL);
                }
                BlockPos up = pos.up();
                if (r.nextFloat() < 0.08f && world.getBlockState(up).isAir()) {
                    BlockState fire = AbstractFireBlock.getState(world, up);
                    if (fire.canPlaceAt(world, up)) world.setBlockState(up, fire, Block.NOTIFY_ALL);
                }
            }
        }

        // --- 5. Audio-visual feedback ---
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS,
                4.0f, (1.0f + (r.nextFloat() - r.nextFloat()) * 0.2f) * 0.75f);
        world.spawnParticles(power >= 4.0f ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION,
                center.x, center.y, center.z, power >= 4.0f ? 1 : 4, 0.6, 0.6, 0.6, 0.0);
        world.spawnParticles(ModParticles.SPARK, center.x, center.y, center.z, 50 + (int) (power * 10), 0.3, 0.3, 0.3, 0.6);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z, 30, power * 0.4, power * 0.3, power * 0.4, 0.05);
        world.spawnParticles(ModParticles.DUST_CLOUD, center.x, center.y, center.z, 10 + (int) (power * 3), power * 0.5, power * 0.3, power * 0.5, 0.04);
        ServerScheduler.addSmoke(world, center, Math.max(1.0, power * 0.6), cfg.smokeDurationTicks);
        ModNetworking.sendShake(world, center, power * 1.6f, power * 14.0, 22);
        world.emitGameEvent(source, GameEvent.EXPLODE, center);

        // --- 6. Structural integrity ---
        StructuralIntegrity.onBlocksDestroyed(world, actuallyDestroyed);
    }
}
