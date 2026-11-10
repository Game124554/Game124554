package net.latexdrones.infection;

import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.latexdrones.LatexDrones;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Server -&gt; client sync of a player's infection state (the receiver is registered in LatexDronesClient). */
public final class ModNetworking {
    private ModNetworking() {
    }

    public static final ResourceLocation SYNC = LatexDrones.id("sync");
    /** Server -&gt; client: open the drone terminal screen. */
    public static final ResourceLocation OPEN_TERMINAL = LatexDrones.id("open_terminal");
    /** Client -&gt; server: a terminal button was pressed (one var-int, see DroneLife.ACTION_*). */
    public static final ResourceLocation TERMINAL_ACTION = LatexDrones.id("terminal_action");

    /** Tells {@code to} about the infection state of player {@code about}. */
    public static void send(ServerPlayer to, UUID about, InfectionData.Entry e) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(about);
        buf.writeVarInt(e.infection);
        buf.writeBoolean(e.transformed);
        buf.writeUtf(e.form);
        buf.writeVarInt(e.transformTicks);
        buf.writeVarInt(e.unit);
        buf.writeVarInt(e.task);
        buf.writeVarInt(e.taskProgress);
        buf.writeVarInt(e.taskGoal);
        buf.writeVarInt(e.tasksDone);
        buf.writeBoolean(e.standby);
        buf.writeVarInt(e.taskTicks);
        buf.writeUtf(e.taskGiver);
        buf.writeVarInt(e.taskX);
        buf.writeVarInt(e.taskY);
        buf.writeVarInt(e.taskZ);
        buf.writeVarInt(e.energy);
        buf.writeVarInt(e.obedience);
        ServerPlayNetworking.send(to, SYNC, buf);
    }

    public static void openTerminal(ServerPlayer player) {
        ServerPlayNetworking.send(player, OPEN_TERMINAL, PacketByteBufs.empty());
    }
}
