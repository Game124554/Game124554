package com.thunderscience.changedtsc.network;

import com.thunderscience.changedtsc.config.TSCConfig;
import com.thunderscience.changedtsc.infection.GooInfection;
import com.thunderscience.changedtsc.item.AntiGooSyringeItem;
import com.thunderscience.changedtsc.item.FirstAidKitItem;
import com.thunderscience.changedtsc.registry.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: the player pressed a treatment button on the body screen.
 * The server checks everything itself (item in the inventory, cooldown, settings) before treating.
 */
public class TreatPacket {
    public static final int BANDAGE = 0;
    public static final int KIT = 1;
    public static final int SYRINGE = 2;

    private final int kind;

    public TreatPacket(int kind) {
        this.kind = kind;
    }

    public TreatPacket(FriendlyByteBuf buf) {
        this.kind = buf.readByte();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(kind);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        ctx.get().setPacketHandled(true);
        if (player == null || !player.isAlive() || player.isSpectator()) return;
        if (kind < BANDAGE || kind > SYRINGE) return;

        Item item = kind == BANDAGE ? ModItems.BANDAGE.get() : kind == KIT ? ModItems.FIRST_AID_KIT.get()
                : ModItems.ANTI_GOO_SYRINGE.get();
        if (kind != SYRINGE && !TSCConfig.FIRST_AID_ENABLED.get()) {
            player.displayClientMessage(Component.translatable("message.changed_tsc.first_aid_disabled"), true);
            return;
        }
        if (player.getCooldowns().isOnCooldown(item)) return;
        if (kind == SYRINGE && GooInfection.get(player) <= 0.0F) {
            player.displayClientMessage(Component.translatable("message.changed_tsc.goo.clean"), true);
            return;
        }

        boolean creative = player.getAbilities().instabuild;
        ItemStack found = ItemStack.EMPTY;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                found = stack;
                break;
            }
        }
        if (found.isEmpty() && !creative) return;

        switch (kind) {
            case BANDAGE -> {
                FirstAidKitItem.applyTreatment(player, true);
                player.getCooldowns().addCooldown(item, 20);
            }
            case KIT -> {
                FirstAidKitItem.applyTreatment(player, false);
                player.getCooldowns().addCooldown(item, TSCConfig.FIRST_AID_COOLDOWN.get());
            }
            default -> {
                AntiGooSyringeItem.inject(player);
                player.getCooldowns().addCooldown(item, 40);
            }
        }
        if (!creative && !found.isEmpty()) found.shrink(1);
    }
}
