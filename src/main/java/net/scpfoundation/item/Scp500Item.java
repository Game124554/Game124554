package net.scpfoundation.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** SCP-500: cures every status effect (good and bad), restores full health and puts out fire. */
public class Scp500Item extends Item {
    public Scp500Item(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            user.clearStatusEffects();
            user.setHealth(user.getMaxHealth());
            user.extinguish();
            user.removeCommandTag(net.scpfoundation.entity.Scp1471Entity.INSTALLED_TAG);
            user.getHungerManager().add(4, 0.5f);
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.0f, 1.2f);
            ((ServerWorld) world).spawnParticles(ParticleTypes.HAPPY_VILLAGER, user.getX(), user.getBodyY(0.6), user.getZ(), 15, 0.4, 0.6, 0.4, 0.0);
            if (!user.getAbilities().creativeMode) stack.decrement(1);
            user.getItemCooldownManager().set(this, 40);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.scpfoundation.scp_500").formatted(Formatting.GRAY));
    }
}
