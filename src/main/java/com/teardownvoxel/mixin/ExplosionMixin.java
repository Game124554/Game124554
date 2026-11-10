package com.teardownvoxel.mixin;

import com.teardownvoxel.config.TVConfig;
import com.teardownvoxel.network.ModNetworking;
import com.teardownvoxel.registry.ModParticles;
import com.teardownvoxel.world.DebrisSpawner;
import com.teardownvoxel.world.StructuralIntegrity;
import com.teardownvoxel.world.VoxelDamage;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Hooks every vanilla explosion (TNT, creepers, beds, fireballs, crystals...): blocks it destroys are
 * turned into flying voxel debris, nearby players get screen shake, and the structure around the crater
 * gets a support check.
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow @Final private World world;
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;
    @Shadow @Final private float power;

    @Shadow
    public abstract List<BlockPos> getAffectedBlocks();

    @Unique
    private List<BlockPos> teardownVoxel$positions;
    @Unique
    private List<BlockState> teardownVoxel$states;

    @Inject(method = "affectWorld", at = @At("HEAD"))
    private void teardownVoxel$snapshot(boolean particles, CallbackInfo ci) {
        if (this.world.isClient) return;
        List<BlockPos> affected = this.getAffectedBlocks();
        teardownVoxel$positions = new ArrayList<>(affected.size());
        teardownVoxel$states = new ArrayList<>(affected.size());
        for (BlockPos pos : affected) {
            BlockState state = this.world.getBlockState(pos);
            if (!state.isAir()) {
                teardownVoxel$positions.add(pos.toImmutable());
                teardownVoxel$states.add(state);
            }
        }
    }

    @Inject(method = "affectWorld", at = @At("TAIL"))
    private void teardownVoxel$afterAffect(boolean particles, CallbackInfo ci) {
        if (!(this.world instanceof ServerWorld sw)) return;
        TVConfig cfg = TVConfig.get();
        Vec3d center = new Vec3d(this.x, this.y, this.z);

        ModNetworking.sendShake(sw, center, this.power * 1.4f, this.power * 12.0, 20);

        if (teardownVoxel$positions == null) return;
        List<BlockPos> destroyed = new ArrayList<>();
        int budget = cfg.vanillaExplosionDebris ? Math.min(cfg.maxDebrisPerExplosion, DebrisSpawner.remainingBudget(sw)) : 0;
        double reach = Math.max(1.0, this.power * 1.8);

        for (int i = 0; i < teardownVoxel$positions.size(); i++) {
            BlockPos pos = teardownVoxel$positions.get(i);
            if (!sw.getBlockState(pos).isAir()) continue; // survived (e.g. mobGriefing off)
            destroyed.add(pos);
            VoxelDamage.clearAt(sw, pos);
            if (budget <= 0) continue;

            BlockState state = teardownVoxel$states.get(i);
            Vec3d c = Vec3d.ofCenter(pos);
            Vec3d away = c.subtract(center);
            double d = away.length();
            away = d < 1.0E-4 ? new Vec3d(0, 1, 0) : away.multiply(1.0 / d);
            double falloff = MathHelper.clamp(1.0 - d / reach, 0.15, 1.0);
            double speed = (0.25 + 0.7 * falloff) * Math.min(2.0, this.power / 3.0);
            int n = Math.min(budget, 1 + sw.random.nextInt(2));
            budget -= DebrisSpawner.shatter(sw, pos, state, away.multiply(speed).add(0, 0.15 + 0.2 * falloff, 0), n, 0.12);
        }

        if (!destroyed.isEmpty()) {
            sw.spawnParticles(ModParticles.DUST_CLOUD, center.x, center.y, center.z, 8 + (int) (this.power * 3),
                    this.power * 0.5, this.power * 0.3, this.power * 0.5, 0.03);
            StructuralIntegrity.onBlocksDestroyed(sw, destroyed);
        }
        teardownVoxel$positions = null;
        teardownVoxel$states = null;
    }
}
