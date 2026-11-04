package net.scpfoundation.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.scpfoundation.registry.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** SCP-963 "Immortality": carry it and, once every five minutes, your mind refuses to die. */
public class Scp963Item extends Item {
    private static final int COOLDOWN = 20 * 300;

    public Scp963Item(Settings settings) {
        super(settings);
    }

    public static boolean tryRevive(ServerPlayerEntity player) {
        if (!player.getInventory().contains(new ItemStack(ModItems.SCP_963))) return false;
        if (player.getItemCooldownManager().isCoolingDown(ModItems.SCP_963)) return false;
        player.setHealth(8.0f);
        player.clearStatusEffects();
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 400, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 400, 0));
        player.getItemCooldownManager().set(ModItems.SCP_963, COOLDOWN);
        ServerWorld world = (ServerWorld) player.getWorld();
        world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getBodyY(0.5), player.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 0.6f);
        player.sendMessage(Text.translatable("message.scpfoundation.963_saved").formatted(Formatting.RED, Formatting.ITALIC), false);
        return true;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.scpfoundation.scp_963").formatted(Formatting.GRAY));
    }
}
