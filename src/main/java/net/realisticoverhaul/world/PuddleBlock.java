package net.realisticoverhaul.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * A thin, shiny sheet of water that forms during rain and dries up afterwards.
 * Puddles next to each other join into one bigger irregular puddle; each puddle
 * has one of 4 shapes. Raindrops make ripples on it.
 */
public class PuddleBlock extends Block {
	public static final BooleanProperty NORTH = Properties.NORTH;
	public static final BooleanProperty EAST = Properties.EAST;
	public static final BooleanProperty SOUTH = Properties.SOUTH;
	public static final BooleanProperty WEST = Properties.WEST;
	public static final IntProperty SHAPE = IntProperty.of("shape", 0, 3);
	private static final VoxelShape OUTLINE = Block.createCuboidShape(0, 0, 0, 16, 0.5, 16);

	public PuddleBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState()
				.with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false).with(SHAPE, 0));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, SHAPE);
	}

	private static BooleanProperty prop(Direction d) {
		return switch (d) {
			case NORTH -> NORTH;
			case EAST -> EAST;
			case SOUTH -> SOUTH;
			default -> WEST;
		};
	}

	/** The right shape and joins for this position. */
	public BlockState shaped(BlockView world, BlockPos pos) {
		BlockState s = getDefaultState().with(SHAPE, (int) (MathHelper.hashCode(pos) >>> 7 & 3));
		for (Direction d : Direction.Type.HORIZONTAL) {
			s = s.with(prop(d), world.getBlockState(pos.offset(d)).isOf(this));
		}
		return s;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return shaped(ctx.getWorld(), ctx.getBlockPos());
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return OUTLINE;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.empty();
	}

	@Override
	public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockPos below = pos.down();
		return world.getBlockState(below).isSideSolidFullSquare(world, below, Direction.UP);
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		BlockState want = shaped(world, pos);
		if (!world.isClient && want != state) world.setBlockState(pos, want, Block.NOTIFY_ALL);
	}

	@Override
	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
		if (world.isClient) return;
		if (!canPlaceAt(state, world, pos)) {
			world.removeBlock(pos, false);
			return;
		}
		BlockState want = shaped(world, pos);
		if (want != state) world.setBlockState(pos, want, Block.NOTIFY_LISTENERS);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
												WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		if (direction == Direction.DOWN && !canPlaceAt(state, world, pos)) {
			return net.minecraft.block.Blocks.AIR.getDefaultState();
		}
		if (direction.getAxis().isHorizontal()) {
			return state.with(prop(direction), neighborState.isOf(this));
		}
		return state;
	}

	@Override
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		// Evaporate when it is not raining here.
		if (!world.hasRain(pos) && random.nextFloat() < 0.35f) {
			world.removeBlock(pos, false);
		}
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		// Raindrop ripples.
		if (world.hasRain(pos)) {
			for (int i = 0; i < 2; i++) {
				if (random.nextInt(2) != 0) continue;
				world.addParticle(ParticleTypes.RAIN, pos.getX() + 0.15 + random.nextDouble() * 0.7, pos.getY() + 0.05,
						pos.getZ() + 0.15 + random.nextDouble() * 0.7, 0, 0, 0);
			}
		}
	}

	@Override
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if (entity.isOnFire()) entity.extinguish();
		if (world.isClient && entity.getVelocity().horizontalLengthSquared() > 0.003 && world.random.nextInt(3) == 0) {
			world.addParticle(ParticleTypes.SPLASH, entity.getX(), pos.getY() + 0.05, entity.getZ(), 0, 0.1, 0);
		}
	}
}
