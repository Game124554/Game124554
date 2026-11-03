package com.latexdrone.item;

import com.latexdrone.entity.PurifierShotEntity;
import com.latexdrone.registry.ModSounds;
import net.minecraft.client.item.TooltipContext;
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

/** Weapon 4: the resistance's answer. Bolts melt latex creatures and cleanse infected players. */
public class PurifierCannonItem extends Item {
    public PurifierCannonItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            PurifierShotEntity shot = new PurifierShotEntity(world, user);
            shot.setItem(new ItemStack(com.latexdrone.registry.ModItems.PURIFIER_CHARGE));
            shot.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 2.2f, 0.3f);
            world.spawnEntity(shot);
            stack.damage(1, user, p -> p.sendToolBreakStatus(hand));
        }
        world.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.PURIFIER_SHOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        user.getItemCooldownManager().set(this, 12);
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.latexdrone.purifier_cannon").formatted(Formatting.GRAY));
    }
}
