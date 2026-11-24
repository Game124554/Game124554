package com.mandelahorror.horror;

import com.mandelahorror.HorrorConfig;
import com.mandelahorror.entity.AlternateEntity;
import com.mandelahorror.network.FxPacket;
import com.mandelahorror.network.ModNetwork;
import com.mandelahorror.registry.ModDamageTypes;
import com.mandelahorror.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * When the M.A.D. bar fills up, the Alternate takes control:
 * the player pulls out a sword / pickaxe / axe from the hotbar, freezes, looks down... and stabs.
 * Counterplay: switch to a different hotbar slot before the stab lands.
 */
public final class SelfStab {
    private static final Set<UUID> IN_PROGRESS = ConcurrentHashMap.newKeySet();

    public static boolean isWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof SwordItem || item instanceof AxeItem || item instanceof PickaxeItem;
    }

    public static boolean isInProgress(UUID id) {
        return IN_PROGRESS.contains(id);
    }

    /** @return false if it could not start (disabled, creative, or no weapon in the hotbar). */
    public static boolean trigger(ServerPlayer player, @Nullable AlternateEntity source) {
        if (!HorrorConfig.SELF_STAB.get() || player.isCreative() || player.isSpectator()) return false;
        if (IN_PROGRESS.contains(player.getUUID())) return false;
        int slot = findWeaponSlot(player);
        if (slot < 0) return false;

        IN_PROGRESS.add(player.getUUID());
        MinecraftServer server = player.server;
        RandomSource r = player.getRandom();
        String name = player.getGameProfile().getName();
        UUID id = player.getUUID();

        // the hand moves on its own
        if (player.getInventory().selected != slot) {
            player.getInventory().selected = slot;
            player.connection.send(new ClientboundSetCarriedItemPacket(slot));
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 9, false, false));
        player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), 70.0F);
        ModNetwork.sendFx(player, FxPacket.SHAKE, 70);
        ModNetwork.sendFx(player, FxPacket.HEARTBEAT, 70);
        boolean voice = source != null && source.speakVoice();
        Speech.alternateSays(player, Lines.stabStart(name, r), !voice);

        Scheduler.later(server, 28, () -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null && p.isAlive()) Speech.fakeChat(p, name, Lines.playerForced(r));
        });
        Scheduler.later(server, 60, () -> stab(server, id));
        return true;
    }

    private static void stab(MinecraftServer server, UUID id) {
        IN_PROGRESS.remove(id);
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p == null || !p.isAlive()) return;
        String name = p.getGameProfile().getName();
        ItemStack stack = p.getMainHandItem();
        if (!isWeapon(stack)) {
            Speech.alternateSays(p, Lines.letGo(name)); // the player switched slots in time
            return;
        }

        ServerLevel level = (ServerLevel) p.level();
        float damage = (float) ((1.0 + attackDamage(stack)) * HorrorConfig.SELF_STAB_MULTIPLIER.get());
        if (!HorrorConfig.SELF_STAB_CAN_KILL.get()) {
            damage = Math.min(damage, Math.max(0.0F, p.getHealth() - 1.0F));
        }

        p.connection.teleport(p.getX(), p.getY(), p.getZ(), p.getYRot(), 85.0F);
        p.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.STAB.get(), SoundSource.PLAYERS, 1.0F,
                0.9F + p.getRandom().nextFloat() * 0.2F);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8F, 0.8F);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY(0.6), p.getZ(), 6, 0.2, 0.2, 0.2, 0.1);
        level.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY(0.6), p.getZ(), 12, 0.3, 0.3, 0.3, 0.2);

        stack.hurtAndBreak(8, p, pl -> pl.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        ModNetwork.sendFx(p, FxPacket.RED_FLASH, 35);
        ModNetwork.sendFx(p, FxPacket.SHAKE, 20);
        if (damage > 0.0F) {
            p.hurt(ModDamageTypes.selfStab(level), damage);
        }

        if (p.isAlive()) {
            RandomSource r = p.getRandom();
            Scheduler.later(server, 50, () -> {
                ServerPlayer again = server.getPlayerList().getPlayer(id);
                if (again != null && again.isAlive()) Speech.alternateSays(again, Lines.afterStab(name, r));
            });
        }
    }

    /** No weapon in the hotbar: a jump scare instead. */
    public static void fallbackScare(ServerPlayer player, @Nullable AlternateEntity alternate) {
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, false));
        ModNetwork.sendFx(player, FxPacket.STATIC_BURST, 30);
        ModNetwork.sendFx(player, FxPacket.SHAKE, 40);
        if (alternate != null) alternate.appearInFrontOf(player);
        Speech.alternateSays(player, Lines.emptyHands(player.getGameProfile().getName()));
    }

    private static int findWeaponSlot(ServerPlayer player) {
        int selected = player.getInventory().selected;
        if (isWeapon(player.getInventory().getItem(selected))) return selected;
        for (int i = 0; i < 9; i++) {
            if (isWeapon(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    private static double attackDamage(ItemStack stack) {
        double total = 0.0;
        for (AttributeModifier mod : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            if (mod.getOperation() == AttributeModifier.Operation.ADDITION) total += mod.getAmount();
        }
        return Math.max(total, 1.0);
    }

    public static void clear() {
        IN_PROGRESS.clear();
    }

    private SelfStab() {}
}
