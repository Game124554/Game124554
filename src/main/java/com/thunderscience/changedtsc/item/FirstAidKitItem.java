package com.thunderscience.changedtsc.item;

import com.thunderscience.changedtsc.compat.FirstAidCompat;
import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.infection.GooInfection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectCategory;
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
import java.util.ArrayList;
import java.util.List;

/**
 * First Aid Kit (big heal, cures harmful effects, regeneration) and Bandage (small, fast heal).
 * Hold right-click to treat yourself, or right-click another mob/player to treat them.
 */
public class FirstAidKitItem extends Item {
    private final boolean bandage;

    public FirstAidKitItem(boolean bandage, Properties properties) {
        super(properties);
        this.bandage = bandage;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return bandage ? 16 : TSCConfig.FIRST_AID_USE_TICKS.get();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!TSCConfig.FIRST_AID_ENABLED.get()) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.changed_tsc.first_aid_disabled"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (player.getHealth() >= player.getMaxHealth() && !hasHarmful(player) && GooInfection.get(player) <= 0.0F) {
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide) {
            applyTreatment(user, bandage);
            if (user instanceof Player player) {
                player.getCooldowns().addCooldown(this, bandage ? 20 : TSCConfig.FIRST_AID_COOLDOWN.get());
                if (!player.getAbilities().instabuild) stack.shrink(1);
            } else {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!TSCConfig.FIRST_AID_ENABLED.get()) return InteractionResult.PASS;
        if (!player.level().isClientSide) {
            applyTreatment(target, bandage);
            player.getCooldowns().addCooldown(this, bandage ? 20 : TSCConfig.FIRST_AID_COOLDOWN.get());
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    private static boolean hasHarmful(LivingEntity e) {
        for (MobEffectInstance inst : e.getActiveEffects()) {
            if (inst.getEffect().getCategory() == MobEffectCategory.HARMFUL) return true;
        }
        return false;
    }

    /** Shared by the item, by scientists and by anything else that wants to heal someone. */
    public static void applyTreatment(LivingEntity target, boolean bandage) {
        // With the First Aid mod, heal() is spread over the hurt body parts by that mod.
        // "healthOnlyFromFirstAidMod" leaves health to that mod's own bandages and plasters.
        if (!FirstAidCompat.healthOnlyFromMod()) {
            target.heal((bandage ? TSCConfig.BANDAGE_HEAL.get() : TSCConfig.FIRST_AID_HEAL.get()).floatValue());
        }
        // push the goo back out of the body
        float cure = (bandage ? TSCConfig.GOO_BANDAGE_CURE.get() : TSCConfig.GOO_KIT_CURE.get()).floatValue();
        GooInfection.cure(target, cure);
        GooInfection.regressChanged(target, cure);
        if (!bandage) {
            if (TSCConfig.FIRST_AID_CURES_EFFECTS.get()) {
                for (MobEffectInstance inst : new ArrayList<>(target.getActiveEffects())) {
                    if (inst.getEffect().getCategory() == MobEffectCategory.HARMFUL) target.removeEffect(inst.getEffect());
                }
            }
            int regen = FirstAidCompat.healthOnlyFromMod() ? 0 : TSCConfig.FIRST_AID_REGEN_TICKS.get();
            if (regen > 0) target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regen, 0));
        }
        Level level = target.level();
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER,
                SoundSource.PLAYERS, 1.0F, 1.2F);
        if (level instanceof ServerLevel server) {
            // Hearts for everyone watching, but not for a treated player: at their own head the hearts
            // would sit right in front of the camera and cover the screen in first person.
            double y = target.getY() + target.getBbHeight() * 0.8;
            int count = bandage ? 2 : 5;
            for (ServerPlayer viewer : server.players()) {
                if (viewer == target) continue;
                server.sendParticles(viewer, ParticleTypes.HEART, false, target.getX(), y, target.getZ(), count, 0.4, 0.3, 0.4, 0.0);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        double heal = bandage ? TSCConfig.BANDAGE_HEAL.get() : TSCConfig.FIRST_AID_HEAL.get();
        tooltip.add(Component.translatable("tooltip.changed_tsc.heals", String.format("%.0f", heal / 2.0))
                .withStyle(ChatFormatting.GREEN));
        double cure = bandage ? TSCConfig.GOO_BANDAGE_CURE.get() : TSCConfig.GOO_KIT_CURE.get();
        tooltip.add(Component.translatable("tooltip.changed_tsc.goo_cure", String.format("%.0f", cure)).withStyle(ChatFormatting.AQUA));
        if (!bandage) tooltip.add(Component.translatable("tooltip.changed_tsc.first_aid").withStyle(ChatFormatting.GRAY));
    }
}
