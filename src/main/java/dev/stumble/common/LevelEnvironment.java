package dev.stumble.common;

import dev.stumble.physics.BoxList;
import dev.stumble.physics.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Lets the physics see the blocks and fluids of a world, client or server. */
public final class LevelEnvironment implements Environment {
    public Level level;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    @Override
    public void collectBoxes(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, BoxList out) {
        AABB region = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        for (VoxelShape shape : level.getBlockCollisions(null, region)) {
            shape.forAllBoxes(out::add);
        }
    }

    @Override
    public double fluidSurface(double x, double y, double z, double[] flow) {
        pos.set(x, y, z);
        FluidState fluid = level.getFluidState(pos);
        if (fluid.isEmpty()) {
            // just above the water: the surface below still matters for a particle this size
            pos.move(Direction.DOWN);
            fluid = level.getFluidState(pos);
            if (fluid.isEmpty()) {
                return Double.NaN;
            }
        }
        Vec3 current = fluid.getFlow(level, pos);
        flow[0] = current.x;
        flow[1] = current.y;
        flow[2] = current.z;
        flow[3] = fluid.is(FluidTags.LAVA) ? 1 : 0;
        // climb to the top of the column
        for (int i = 0; i < 16; i++) {
            pos.move(Direction.UP);
            FluidState above = level.getFluidState(pos);
            if (!above.getType().isSame(fluid.getType())) {
                pos.move(Direction.DOWN);
                break;
            }
            fluid = above;
        }
        return pos.getY() + fluid.getHeight(level, pos);
    }
}
