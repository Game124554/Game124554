package net.scpfoundation.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
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
import net.scpfoundation.registry.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A helmet lined with SCP-148 "Telekill" alloy. It blocks mental effects: SCP-012's compulsion, SCP-1471's
 * haunting and SCP-939's mimicry can't reach the wearer.
 */
public class Scp148Item extends Item implements Equipment {
    public Scp148Item(Settings settings) {
        super(settings);
    }

    public static boolean isShielded(PlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.SCP_148);
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
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_ARMOR_EQUIP_IRON, SoundCategory.PLAYERS, 1.0f, 0.8f);
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.scpfoundation.scp_148").formatted(Formatting.GRAY));
    }
}
