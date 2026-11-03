package net.scpfoundation.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.scpfoundation.entity.Scp018Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp018Item extends Item {
    public Scp018Item(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.6f, 0.5f);
        if (!world.isClient) {
            Scp018Entity ball = new Scp018Entity(world, user);
            ItemStack thrown = stack.copy();
            thrown.setCount(1);
            ball.setItem(thrown);
            ball.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 1.1f, 0.5f);
            world.spawnEntity(ball);
        }
        if (!user.getAbilities().creativeMode) stack.decrement(1);
        user.getItemCooldownManager().set(this, 20);
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.scpfoundation.scp_018").formatted(Formatting.GRAY));
    }
}
