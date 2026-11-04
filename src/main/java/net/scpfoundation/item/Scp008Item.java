package net.scpfoundation.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.scpfoundation.world.ZombiePlayers;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A sealed vial of SCP-008. Use it on a creature (or yourself, sneaking) to infect it. */
public class Scp008Item extends Item {
    public static final int INFECTION_TICKS = 20 * 150;

    public Scp008Item(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!user.getWorld().isClient) {
            ZombiePlayers.infect(entity, INFECTION_TICKS);
            user.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.8f, 1.2f);
            if (!user.getAbilities().creativeMode) stack.decrement(1);
        }
        return ActionResult.success(user.getWorld().isClient);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!user.isSneaking()) return TypedActionResult.pass(stack);
        if (!world.isClient) {
            ZombiePlayers.infect(user, INFECTION_TICKS);
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.8f, 1.2f);
            if (!user.getAbilities().creativeMode) stack.decrement(1);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.scpfoundation.scp_008").formatted(Formatting.DARK_GREEN));
    }
}
