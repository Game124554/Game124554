package net.scpfoundation.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public final class ScpUtil {
    private ScpUtil() {}

    /** Line of sight that ignores glass (uses the camera/visual shape, so containment windows don't block view). */
    public static boolean canSeeThroughGlass(Entity from, Vec3d start, Vec3d end) {
        HitResult hit = from.getWorld().raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, from));
        return hit.getType() == HitResult.Type.MISS;
    }

    /** True if the player is looking roughly toward the given point (dot product threshold) and has a line of sight. */
    public static boolean isLookingAt(PlayerEntity player, Vec3d point, double minDot) {
        Vec3d eye = player.getEyePos();
        Vec3d to = point.subtract(eye);
        double len = to.length();
        if (len < 1.0E-4) return true;
        Vec3d look = player.getRotationVec(1.0f).normalize();
        if (look.dotProduct(to.multiply(1.0 / len)) < minDot) return false;
        return canSeeThroughGlass(player, eye, point);
    }

    public static boolean isTargetablePlayer(PlayerEntity p) {
        return p.isAlive() && !p.isSpectator() && !p.isCreative();
    }

    /** Smashes breakable blocks in front of a mob (respects the mobGriefing game rule). */
    public static void smashAhead(MobEntity mob, int halfWidth, int height) {
        World world = mob.getWorld();
        if (!world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) return;
        Direction facing = mob.getHorizontalFacing();
        Direction side = facing.rotateYClockwise();
        BlockPos base = mob.getBlockPos().offset(facing, (int) Math.ceil(mob.getWidth() / 2.0));
        for (int w = -halfWidth; w <= halfWidth; w++) {
            for (int dy = 0; dy < height; dy++) {
                BlockPos p = base.offset(side, w).up(dy);
                BlockState s = world.getBlockState(p);
                if (!s.isAir() && s.getHardness(world, p) >= 0.0f && !s.isIn(BlockTags.WITHER_IMMUNE)) {
                    world.breakBlock(p, true, mob);
                }
            }
        }
    }
}
