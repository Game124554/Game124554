package com.teardownvoxel.item;

import com.teardownvoxel.config.TVConfig;
import com.teardownvoxel.registry.ModParticles;
import com.teardownvoxel.registry.ModTags;
import com.teardownvoxel.world.VoxelDamage;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Hold right-click to cut. Metal blocks, iron bars and doors melt in about a second with a shower of
 * sparks; anything else only scorches slowly (and flammable blocks may catch fire).
 */
public class BlowtorchItem extends Item {
    private static final double RANGE = 3.5;

    public BlowtorchItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.5f, 1.6f);
        return TypedActionResult.consume(user.getStackInHand(hand));
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        HitResult result = user.raycast(RANGE, 0.0f, false);
        Vec3d eye = user.getEyePos();
        Vec3d look = user.getRotationVector();
        Vec3d nozzle = eye.add(look.multiply(0.6)).add(0, -0.15, 0);

        if (world.isClient) {
            // Flame jet (client-side for responsiveness).
            for (int i = 0; i < 2; i++) {
                Vec3d v = look.multiply(0.25 + world.random.nextDouble() * 0.1);
                world.addParticle(ParticleTypes.SMALL_FLAME, nozzle.x, nozzle.y, nozzle.z,
                        v.x + (world.random.nextDouble() - 0.5) * 0.02, v.y + (world.random.nextDouble() - 0.5) * 0.02, v.z + (world.random.nextDouble() - 0.5) * 0.02);
            }
            return;
        }

        ServerWorld sw = (ServerWorld) world;
        int used = getMaxUseTime(stack) - remainingUseTicks;
        if (used % 8 == 0) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.PLAYERS, 0.8f, 1.8f);
        }
        if (used % 4 == 0) {
            stack.damage(1, user, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        }

        // Burn entities in front of the nozzle.
        Vec3d tip = eye.add(look.multiply(1.6));
        for (Entity e : world.getOtherEntities(user, new Box(tip, tip).expand(0.6), en -> en instanceof LivingEntity && en.isAlive())) {
            e.setOnFireFor(3);
            e.damage(world.getDamageSources().inFire(), 1.0f);
        }

        if (!(result instanceof BlockHitResult bhr) || result.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = bhr.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (user instanceof PlayerEntity p && !world.canPlayerModifyAt(p, pos)) return;
        Vec3d hit = bhr.getPos();
        TVConfig cfg = TVConfig.get();

        boolean metal = state.isIn(ModTags.TORCHABLE);
        Vec3d away = look.multiply(-0.1);
        if (metal) {
            sw.spawnParticles(ModParticles.SPARK, hit.x, hit.y, hit.z, 6, 0.05, 0.05, 0.05, 0.35);
            if (used % 3 == 0) sw.spawnParticles(ParticleTypes.LAVA, hit.x, hit.y, hit.z, 1, 0.05, 0.05, 0.05, 0.0);
            if (used % 10 == 0) {
                world.playSound(null, hit.x, hit.y, hit.z, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.3f, 1.8f);
            }
            if (VoxelDamage.damage(sw, pos, cfg.blowtorchMetalDamagePerTick, away)) {
                world.playSound(null, hit.x, hit.y, hit.z, SoundEvents.BLOCK_ANVIL_BREAK, SoundCategory.BLOCKS, 0.6f, 1.5f);
            }
        } else {
            sw.spawnParticles(ParticleTypes.SMOKE, hit.x, hit.y, hit.z, 2, 0.05, 0.05, 0.05, 0.02);
            VoxelDamage.damage(sw, pos, cfg.blowtorchOtherDamagePerTick, away);
            if (used % 20 == 0 && world.random.nextInt(3) == 0) {
                BlockPos firePos = pos.offset(bhr.getSide());
                if (world.getBlockState(firePos).isAir()) {
                    BlockState fire = AbstractFireBlock.getState(world, firePos);
                    if (fire.canPlaceAt(world, firePos)) world.setBlockState(firePos, fire, Block.NOTIFY_ALL);
                }
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.teardown_voxel.blowtorch.tooltip").formatted(Formatting.GRAY));
    }
}
