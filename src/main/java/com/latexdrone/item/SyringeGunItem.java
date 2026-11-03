package com.latexdrone.item;

import com.latexdrone.entity.SyringeDartEntity;
import com.latexdrone.registry.ModSounds;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Weapon 2: fires syringe darts that inject latex serum + hypnotic. */
public class SyringeGunItem extends Item {
    public SyringeGunItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            SyringeDartEntity dart = new SyringeDartEntity(world, user);
            dart.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 3.0f, 0.5f);
            dart.setDamage(3.0);
            dart.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
            world.spawnEntity(dart);
            stack.damage(1, user, p -> p.sendToolBreakStatus(hand));
        }
        world.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.SYRINGE_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        user.getItemCooldownManager().set(this, 15);
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.latexdrone.syringe_gun").formatted(Formatting.GRAY));
    }
}
