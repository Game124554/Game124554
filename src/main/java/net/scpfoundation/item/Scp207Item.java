package net.scpfoundation.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** SCP-207: stacking speed boost that slowly poisons the drinker. */
public class Scp207Item extends Item {
    public Scp207Item(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 32;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient) {
            StatusEffectInstance speed = user.getStatusEffect(StatusEffects.SPEED);
            int amp = speed == null ? 1 : Math.min(speed.getAmplifier() + 1, 4);
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 180, amp));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 20 * 180, 0));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 20 * 20 * (amp + 1), 0));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 20 * 60, amp));
        }
        if (!(user instanceof PlayerEntity player) || !player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return stack;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.scpfoundation.scp_207").formatted(Formatting.GRAY));
    }
}
