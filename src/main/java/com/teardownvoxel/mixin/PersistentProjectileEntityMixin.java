package com.teardownvoxel.mixin;

import com.teardownvoxel.block.NitroChargeBlock;
import com.teardownvoxel.config.TVConfig;
import com.teardownvoxel.world.VoxelDamage;
import net.minecraft.block.BlockState;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Arrows and tridents chip blocks progressively; fragile blocks (glass, ice...) shatter and the
 * projectile keeps flying through the hole.
 */
@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityMixin {
    @Inject(method = "onBlockHit", at = @At("HEAD"), cancellable = true)
    private void teardownVoxel$damageBlock(BlockHitResult hit, CallbackInfo ci) {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        if (!(self.getWorld() instanceof ServerWorld sw)) return;
        TVConfig cfg = TVConfig.get();
        if (!cfg.projectileBlockDamage) return;

        BlockPos pos = hit.getBlockPos();
        BlockState state = sw.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof NitroChargeBlock) return;

        Vec3d v = self.getVelocity();
        float dmg = (float) (v.length() * cfg.projectileDamageMultiplier * (self.isCritical() ? 1.5 : 1.0));
        if (dmg < 0.2f) return;

        if (VoxelDamage.damage(sw, pos, dmg, v.multiply(0.15))) {
            // Punched through: keep flying, slowed down.
            self.setVelocity(v.multiply(0.6));
            ci.cancel();
        }
    }
}
