package com.teardownvoxel.item;

import com.teardownvoxel.config.TVConfig;
import com.teardownvoxel.entity.PipeBombEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Right-click to light and throw. Sneak + right-click a block to plant it there with a longer fuse.
 */
public class PipeBombItem extends Item {
    public PipeBombItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 0.6f, 1.6f);
        if (!world.isClient) {
            Vec3d eye = user.getEyePos();
            Vec3d look = user.getRotationVector();
            PipeBombEntity bomb = PipeBombEntity.create(world, user, eye.x + look.x * 0.4, eye.y - 0.15 + look.y * 0.4, eye.z + look.z * 0.4,
                    TVConfig.get().pipeBombFuseTicks);
            bomb.setVelocity(look.multiply(1.05).add(user.getVelocity().x, user.isOnGround() ? 0.0 : user.getVelocity().y, user.getVelocity().z).add(0, 0.12, 0));
            world.spawnEntity(bomb);
        }
        user.getItemCooldownManager().set(this, 10);
        if (!user.getAbilities().creativeMode) stack.decrement(1);
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || !player.isSneaking()) return ActionResult.PASS; // falls through to use() => throw
        World world = context.getWorld();
        if (!world.isClient) {
            Vec3d hit = context.getHitPos();
            Vec3d n = Vec3d.of(context.getSide().getVector()).multiply(0.16);
            PipeBombEntity bomb = PipeBombEntity.create(world, player, hit.x + n.x, hit.y + n.y - 0.15, hit.z + n.z,
                    TVConfig.get().pipeBombFuseTicks + 40);
            world.spawnEntity(bomb);
            world.playSound(null, hit.x, hit.y, hit.z, SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 0.8f, 1.4f);
        }
        if (!player.getAbilities().creativeMode) context.getStack().decrement(1);
        return ActionResult.success(world.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.teardown_voxel.pipe_bomb.tooltip").formatted(Formatting.GRAY));
    }
}
