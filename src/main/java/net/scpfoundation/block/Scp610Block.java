package net.scpfoundation.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.scpfoundation.registry.ModBlocks;

/**
 * SCP-610 "The Flesh that Hates": a spreading growth. It slowly converts natural blocks around it (only while
 * mobGriefing is on, and never more than 30 blocks in any one patch) and sickens anything that walks on it.
 */
public class Scp610Block extends Block {
    private static final int RADIUS = 4;
    private static final int PATCH_LIMIT = 30;

    public Scp610Block(Settings settings) {
        super(settings);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING) || random.nextInt(3) != 0) return;
        BlockPos target = pos.offset(Direction.random(random));
        BlockState t = world.getBlockState(target);
        if (!isConvertible(t)) return;
        int count = 0;
        for (BlockPos p : BlockPos.iterate(pos.add(-RADIUS, -RADIUS, -RADIUS), pos.add(RADIUS, RADIUS, RADIUS))) {
            if (world.getBlockState(p).isOf(this) && ++count >= PATCH_LIMIT) return;
        }
        world.setBlockState(target, ModBlocks.SCP_610.getDefaultState());
    }

    private static boolean isConvertible(BlockState s) {
        return s.isIn(BlockTags.DIRT) || s.isIn(BlockTags.SAND) || s.isIn(BlockTags.LOGS) || s.isIn(BlockTags.LEAVES)
                || s.isIn(BlockTags.WOOL) || s.isIn(BlockTags.BASE_STONE_OVERWORLD);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity living && living.getGroup() != EntityGroup.UNDEAD && world.getTime() % 20 == 0) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60, 0));
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 200, 1));
        }
        super.onSteppedOn(world, pos, state, entity);
    }
}
