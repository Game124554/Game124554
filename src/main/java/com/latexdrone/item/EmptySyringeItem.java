package com.latexdrone.item;

import com.latexdrone.entity.LatexMobEntity;
import com.latexdrone.registry.ModItems;
import com.latexdrone.registry.ModSounds;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Use on a latex creature to draw a sample -> Latex Injector of that form. */
public class EmptySyringeItem extends Item {
    public EmptySyringeItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof LatexMobEntity latex)) return ActionResult.PASS;
        if (!user.getWorld().isClient) {
            ItemStack filled = LatexInjectorItem.filledWith(ModItems.LATEX_INJECTOR, latex.getForm());
            if (!user.getAbilities().creativeMode) stack.decrement(1);
            if (!user.getInventory().insertStack(filled)) user.dropItem(filled, false);
            user.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.SYRINGE_INJECT, SoundCategory.PLAYERS, 1.0f, 0.7f);
        }
        return ActionResult.success(user.getWorld().isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.latexdrone.empty_syringe").formatted(Formatting.GRAY));
    }
}
