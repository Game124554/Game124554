package net.scpfoundation.registry;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.scpfoundation.entity.Scp1471Entity;
import net.scpfoundation.item.Scp1471Item;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.scpfoundation.entity.Scp0492Entity;
import net.scpfoundation.entity.Scp049Entity;

public final class ModEvents {
    private ModEvents() {}

    public static void register() {
        // SCP-049's "cure": anything it kills rises again as SCP-049-2.
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed) -> {
            if (killer instanceof Scp049Entity && !(killed instanceof Scp0492Entity) && isCurable(killed)) {
                spawnInstance(world, killed);
            }
        });

        // SCP-2521: write about it in chat and it comes to take something from you.
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String text = message.getSignedContent();
            if (text.contains("2521") || text.contains("\u25CF\u25CF|\u25CF\u25CF\u25CF\u25CF\u25CF|\u25CF\u25CF|\u25CF")) {
                take2521(sender);
            }
        });

        // SCP-1471: players who installed MalO keep seeing the figure.
        ServerTickEvents.END_SERVER_TICK.register(ModEvents::haunt1471);
    }

    private static void take2521(ServerPlayerEntity player) {
        if (player.isCreative() || player.isSpectator()) return;
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < player.getInventory().main.size(); i++) {
            if (!player.getInventory().main.get(i).isEmpty()) slots.add(i);
        }
        if (slots.isEmpty()) return;
        int slot = slots.get(player.getRandom().nextInt(slots.size()));
        ItemStack taken = player.getInventory().main.get(slot);
        Text name = taken.getName();
        player.getInventory().main.set(slot, ItemStack.EMPTY);
        ServerWorld world = (ServerWorld) player.getWorld();
        world.spawnParticles(ParticleTypes.SQUID_INK, player.getX(), player.getBodyY(0.5), player.getZ(), 25, 0.5, 0.8, 0.5, 0.05);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PHANTOM_BITE, SoundCategory.HOSTILE, 1.0f, 0.5f);
        player.sendMessage(Text.translatable("message.scpfoundation.2521_taken", name).formatted(Formatting.DARK_RED), false);
    }

    private static void haunt1471(MinecraftServer server) {
        if (server.getTicks() % 20 != 0) return;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!Scp1471Item.isInstalled(player) || player.isSpectator()) continue;
            if (player.getRandom().nextInt(75) != 0) continue;
            if (!Scp1471Entity.hasActiveFigure(player)) Scp1471Entity.spawnFor(player);
        }
    }

    private static boolean isCurable(LivingEntity killed) {
        return killed instanceof net.minecraft.entity.player.PlayerEntity
                || killed instanceof net.minecraft.entity.passive.MerchantEntity
                || killed instanceof net.minecraft.entity.mob.ZombieEntity
                || killed instanceof net.minecraft.entity.mob.AbstractPiglinEntity
                || killed instanceof net.minecraft.entity.mob.IllagerEntity
                || killed instanceof net.minecraft.entity.mob.WitchEntity;
    }

    private static void spawnInstance(ServerWorld world, LivingEntity killed) {
        Scp0492Entity zombie = ModEntities.SCP_049_2.create(world);
        if (zombie == null) return;
        zombie.refreshPositionAndAngles(killed.getX(), killed.getY(), killed.getZ(), killed.getYaw(), 0.0f);
        if (killed.hasCustomName()) zombie.setCustomName(killed.getCustomName());
        if (killed instanceof MobEntity mob && mob.isPersistent()) zombie.setPersistent();
        world.spawnEntity(zombie);
        world.spawnParticles(ParticleTypes.SOUL, killed.getX(), killed.getBodyY(0.5), killed.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
        world.playSound(null, killed.getX(), killed.getY(), killed.getZ(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 1.0f, 0.6f);
    }
}
