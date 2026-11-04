package net.scpfoundation.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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

/** SCP-178 "3-D Specs": wear them and you see everything that lives around you - even through walls. */
public class Scp178Item extends Item implements Equipment {
    public Scp178Item(Settings settings) {
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
        if (!worn.isEmpty() && worn.isOf(this)) return TypedActionResult.fail(held);
        user.equipStack(EquipmentSlot.HEAD, held.copy());
        user.setStackInHand(hand, worn.copy());
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, SoundCategory.PLAYERS, 1.0f, 1.2f);
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient || !(entity instanceof PlayerEntity player) || world.getTime() % 20 != 0) return;
        if (player.getEquippedStack(EquipmentSlot.HEAD) != stack) return;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, true, false, true));
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(32.0), e -> e != player)) {
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, true, false, false));
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.scpfoundation.scp_178").formatted(Formatting.GRAY));
    }
}
