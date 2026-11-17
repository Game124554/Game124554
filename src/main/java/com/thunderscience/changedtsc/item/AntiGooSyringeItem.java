package com.thunderscience.changedtsc.item;

import com.thunderscience.changedtsc.infection.GooInfection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Removes ALL goo from the body (any stage) at the cost of a few seconds of nausea. */
public class AntiGooSyringeItem extends Item {
    public AntiGooSyringeItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 24;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (GooInfection.get(player) <= 0.0F) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.changed_tsc.goo.clean"), true);
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide) {
            inject(user);
            if (!(user instanceof Player p && p.getAbilities().instabuild)) stack.shrink(1);
            if (user instanceof Player p) p.getCooldowns().addCooldown(this, 40);
        }
        return stack;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (GooInfection.get(target) <= 0.0F) return InteractionResult.PASS;
        if (!player.level().isClientSide) {
            inject(target);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    /** Shared by the item and by the body screen's "clear goo" button. */
    public static void inject(LivingEntity target) {
        float removed = GooInfection.cure(target, 100.0F);
        GooInfection.regressChanged(target, Math.max(removed, 100.0F));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BOTTLE_EMPTY,
                SoundSource.PLAYERS, 1.0F, 1.3F);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.changed_tsc.syringe").withStyle(ChatFormatting.AQUA));
    }
}
