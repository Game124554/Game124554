package net.scpfoundation.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * SCP-035 "Possessive Mask". It whispers to whoever carries it. Put it on and it takes hold: great strength
 * and resilience, hostile mobs keep their distance, but it binds itself to you and slowly rots its host.
 * Only SCP-500 can pry it off.
 */
public class Scp035Item extends Item implements Equipment {
    private static final int WHISPERS = 6;

    public Scp035Item(Settings settings) {
        super(settings);
    }

    @Override
    public EquipmentSlot getSlotType() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack held = user.getStackInHand(hand);
        ItemStack worn = user.getEquippedStack(EquipmentSlot.HEAD);
        if (EnchantmentHelper.hasBindingCurse(worn)) return TypedActionResult.fail(held);
        ItemStack mask = held.copy();
        mask.setCount(1);
        if (EnchantmentHelper.getLevel(Enchantments.BINDING_CURSE, mask) == 0) mask.addEnchantment(Enchantments.BINDING_CURSE, 1);
        user.equipStack(EquipmentSlot.HEAD, mask);
        held.decrement(1);
        if (!worn.isEmpty()) user.getInventory().offerOrDrop(worn.copy());
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_WOLOLO, SoundCategory.PLAYERS, 1.0f, 0.5f);
        if (!world.isClient) {
            user.sendMessage(Text.translatable("message.scpfoundation.035_worn").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false);
        }
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient || !(entity instanceof PlayerEntity player)) return;
        long t = world.getTime();
        boolean worn = player.getEquippedStack(EquipmentSlot.HEAD) == stack;
        if (worn) {
            if (t % 20 == 0) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, 1, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 0, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 40, 0, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, true, false, true));
                for (HostileEntity mob : world.getEntitiesByClass(HostileEntity.class, player.getBoundingBox().expand(10.0), m -> m.getTarget() == player)) {
                    if (world.getRandom().nextInt(3) == 0) mob.setTarget(null);
                }
            }
            if (t % 300 == 0) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 80, 0));
                whisper(player);
            }
        } else if (t % 600 == 0) {
            player.sendMessage(Text.translatable("message.scpfoundation.035_call").formatted(Formatting.DARK_PURPLE, Formatting.ITALIC), true);
        }
    }

    private static void whisper(PlayerEntity player) {
        int i = player.getRandom().nextInt(WHISPERS);
        player.sendMessage(Text.literal("SCP-035: ").formatted(Formatting.DARK_PURPLE)
                .append(Text.translatable("message.scpfoundation.035." + i).formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC)), false);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.scpfoundation.scp_035").formatted(Formatting.DARK_PURPLE));
    }
}
