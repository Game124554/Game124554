package net.scpfoundation.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** SCP-427: opening the locket heals. Exposure accumulates; after too many uses the flesh starts to change. */
public class Scp427Item extends Item {
    private static final String KEY = "Exposure";

    public Scp427Item(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            NbtCompound nbt = stack.getOrCreateNbt();
            int exposure = nbt.getInt(KEY) + 1;
            nbt.putInt(KEY, exposure);
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 1200, 1));
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.8f, 0.6f);
            if (exposure > 12) {
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 300, 0));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 600, 2));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 600, 1));
                user.sendMessage(Text.translatable("message.scpfoundation.427_overuse").formatted(Formatting.DARK_RED), true);
            }
            user.getItemCooldownManager().set(this, 100);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.scpfoundation.scp_427").formatted(Formatting.GRAY));
    }
}
