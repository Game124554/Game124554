package com.mandelahorror.horror;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class HorrorUtil {

    /**
     * True if the player is looking toward the entity.
     * threshold ~0.97 = staring right at it, ~0.6 = it is somewhere on screen.
     */
    public static boolean isLookingAt(Player player, Entity target, double threshold) {
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 to = new Vec3(target.getX() - player.getX(), target.getY(0.8) - player.getEyeY(), target.getZ() - player.getZ());
        double len = to.length();
        if (len < 1.0E-4) return true;
        double dot = view.dot(to.scale(1.0 / len));
        return dot > threshold && player.hasLineOfSight(target);
    }

    /** Finds a spot where the given entity type can stand, searching up/down from the given position. */
    @Nullable
    public static BlockPos findStandable(ServerLevel level, BlockPos around, EntityType<?> type, int yRange) {
        for (int i = 0; i <= yRange * 2; i++) {
            int dy = (i % 2 == 0) ? i / 2 : -(i + 1) / 2; // 0, -1, 1, -2, 2 ...
            BlockPos pos = around.offset(0, dy, 0);
            if (!level.isLoaded(pos)) return null;
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.getCollisionShape(level, below).isEmpty()) continue;
            if (!belowState.getFluidState().isEmpty()) continue;
            AABB box = type.getAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (level.noCollision(box) && !level.containsAnyLiquid(box)) return pos;
        }
        return null;
    }

    /**
     * Random standable spot between minDist and maxDist blocks from the player.
     * If behind is true, prefers spots behind the player's back.
     */
    @Nullable
    public static BlockPos findSpotAround(ServerLevel level, Player player, EntityType<?> type,
                                          double minDist, double maxDist, boolean behind) {
        RandomSource r = player.getRandom();
        Vec3 look = player.getLookAngle();
        double baseAngle = Math.atan2(-look.z, -look.x); // direction behind the player
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = behind
                    ? baseAngle + (r.nextDouble() - 0.5) * Math.PI * 0.9
                    : r.nextDouble() * Math.PI * 2.0;
            double dist = Mth.lerp(r.nextDouble(), minDist, maxDist);
            BlockPos base = BlockPos.containing(player.getX() + Math.cos(angle) * dist, player.getY(),
                    player.getZ() + Math.sin(angle) * dist);
            BlockPos spot = findStandable(level, base, type, 8);
            if (spot != null) return spot;
        }
        return null;
    }

    /** A spot about `dist` blocks in front of the player's face. */
    @Nullable
    public static BlockPos findSpotInFront(ServerLevel level, Player player, EntityType<?> type, double dist) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();
        BlockPos base = BlockPos.containing(player.getX() + flat.x * dist, player.getY(), player.getZ() + flat.z * dist);
        return findStandable(level, base, type, 3);
    }

    private HorrorUtil() {}
}
