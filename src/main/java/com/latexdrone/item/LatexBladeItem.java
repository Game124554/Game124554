package com.latexdrone.item;

import com.latexdrone.infection.InfectionManager;
import com.latexdrone.registry.ModSounds;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Weapon 1: a blade of hardened dark latex. Coats targets in goo (slowness) and infects players. */
public class LatexBladeItem extends SwordItem {
    public LatexBladeItem(Settings settings) {
        super(LatexToolMaterial.INSTANCE, 4, -2.2f, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
            if (target instanceof ServerPlayerEntity player) InfectionManager.addInfection(player, 6.0f, 0);
            attacker.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.BLADE_SWING, SoundCategory.PLAYERS, 0.8f, 1.0f);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.latexdrone.latex_blade").formatted(Formatting.GRAY));
    }
}
