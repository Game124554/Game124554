package net.scpfoundation.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Water from SCP-006: heals and removes the effects of age, poison and fatigue. Leaves an empty bottle. */
public class Scp006Item extends Item {
    public Scp006Item(Settings settings) {
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
            user.removeStatusEffect(StatusEffects.POISON);
            user.removeStatusEffect(StatusEffects.WITHER);
            user.removeStatusEffect(StatusEffects.WEAKNESS);
            user.removeStatusEffect(StatusEffects.SLOWNESS);
            user.removeStatusEffect(StatusEffects.MINING_FATIGUE);
            user.removeStatusEffect(StatusEffects.HUNGER);
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 600, 1));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 10, 2));
        }
        if (user instanceof PlayerEntity player && player.getAbilities().creativeMode) return stack;
        stack.decrement(1);
        if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
        if (user instanceof PlayerEntity player) player.getInventory().offerOrDrop(new ItemStack(Items.GLASS_BOTTLE));
        return stack;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.scpfoundation.scp_006").formatted(Formatting.GRAY));
    }
}
