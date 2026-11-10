package net.latexdrones.item;

import net.latexdrones.block.GooBlock;
import net.latexdrones.block.InfectiousBlock;
import net.latexdrones.block.LatexPlantBlock;
import net.latexdrones.block.PuddleBlock;
import net.latexdrones.entity.LatexDroneEntity;
import net.latexdrones.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * Purifier flamethrower: every right-click throws a short cone of fire. Sets what it hits ablaze,
 * does heavy damage to latex creatures and melts latex blocks, goo, puddles and growths in its path.
 */
public class FlamethrowerItem extends Item {
    private static final double RANGE = 7.0D;

    public FlamethrowerItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RANGE),
                    e -> e != player && e.isAlive())) {
                Vec3 to = target.getBoundingBox().getCenter().subtract(eye);
                double dist = to.length();
                if (dist > RANGE || dist < 1.0E-3D || to.scale(1.0D / dist).dot(look) < 0.82D || !player.hasLineOfSight(target)) {
                    continue;
                }
                target.setSecondsOnFire(6);
                target.hurt(server.damageSources().playerAttack(player), target instanceof LatexDroneEntity ? 9.0F : 4.0F);
            }
            for (int i = 1; i <= (int) RANGE; i++) {
                Vec3 p = eye.add(look.scale(i));
                double s = 0.08D * i;
                server.sendParticles(ParticleTypes.FLAME, p.x, p.y - 0.2D, p.z, 6, s, s, s, 0.01D);
                BlockPos pos = BlockPos.containing(p);
                Block block = server.getBlockState(pos).getBlock();
                if (block instanceof InfectiousBlock || block instanceof GooBlock || block instanceof PuddleBlock
                        || block instanceof LatexPlantBlock) {
                    server.destroyBlock(pos, false);
                }
            }
            server.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FLAMETHROWER, SoundSource.PLAYERS, 1.0F, 1.0F);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        player.getCooldowns().addCooldown(this, 8);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
