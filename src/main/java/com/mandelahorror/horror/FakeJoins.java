package com.mandelahorror.horror;

import com.mandelahorror.HorrorConfig;
import com.mandelahorror.MandelaHorror;
import com.mandelahorror.entity.MimicEntity;
import com.mandelahorror.network.FxPacket;
import com.mandelahorror.network.ModNetwork;
import com.mandelahorror.registry.ModEntities;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Fake players: "Name joined the game" in yellow, a TAB-list entry, chat messages,
 * and a mimic walking around wearing that player's skin (random, or yours).
 */
public final class FakeJoins {

    public record Session(UUID fakeId, UUID haunted, String name, boolean ownName, long endTick, boolean tabListed) {}

    /** key = fake player id (also used as the mimic's skin seed) */
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    public static boolean isActive(UUID fakeId) {
        return SESSIONS.containsKey(fakeId);
    }

    @Nullable
    public static Session activeFor(UUID playerId) {
        for (Session s : SESSIONS.values()) {
            if (s.haunted().equals(playerId)) return s;
        }
        return null;
    }

    /** Start a fake join for this player. */
    public static void start(ServerPlayer player, boolean force) {
        MinecraftServer server = player.server;
        Session existing = activeFor(player.getUUID());
        if (existing != null) {
            if (!force) return;
            endSession(server, existing.fakeId());
        }

        RandomSource r = player.getRandom();
        boolean own = r.nextDouble() < HorrorConfig.OWN_NAME_CHANCE.get();
        String playerName = player.getGameProfile().getName();
        String name = own ? playerName : Lines.randomName(r, playerName);
        UUID fakeId = UUID.randomUUID();
        long now = server.getTickCount();
        Session s = new Session(fakeId, player.getUUID(), name, own,
                now + 20L * (120 + r.nextInt(120)), HorrorConfig.FAKE_TAB_LIST.get());
        SESSIONS.put(fakeId, s);

        // 1) the "normal" join message, identical to vanilla
        player.sendSystemMessage(Component.translatable("multiplayer.player.joined", name).withStyle(ChatFormatting.YELLOW));
        // 2) TAB list entry
        if (s.tabListed()) addToTabList(player, s);

        // 3) it talks
        later(server, s, 60 + r.nextInt(80), p -> Speech.fakeChat(p, name, Lines.fakeFirst(own, p.getGameProfile().getName(), r)));
        later(server, s, 260 + r.nextInt(200), p -> Speech.fakeChat(p, name, Lines.fakeSecond(r)));

        // 4) it shows up in the world
        later(server, s, 120 + r.nextInt(100), p -> {
            spawnMimic(p, s);
            if (own) ModNetwork.sendFx(p, FxPacket.SKIN_GLITCH, 100);
        });
    }

    /** Fake player leaves: "Name left the game" + removed from TAB list. */
    public static void endSession(MinecraftServer server, UUID fakeId) {
        Session s = SESSIONS.remove(fakeId);
        if (s == null) return;
        ServerPlayer p = server.getPlayerList().getPlayer(s.haunted());
        if (p == null) return;
        p.sendSystemMessage(Component.translatable("multiplayer.player.left", s.name()).withStyle(ChatFormatting.YELLOW));
        if (s.tabListed()) {
            p.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(s.fakeId())));
        }
    }

    public static void endAllFor(MinecraftServer server, UUID playerId) {
        for (Session s : List.copyOf(SESSIONS.values())) {
            if (s.haunted().equals(playerId)) SESSIONS.remove(s.fakeId());
        }
    }

    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        for (Session s : List.copyOf(SESSIONS.values())) {
            if (now >= s.endTick() || server.getPlayerList().getPlayer(s.haunted()) == null) {
                endSession(server, s.fakeId());
            }
        }
    }

    /** The real player typed in chat while a fake player is online: it answers... or repeats you. */
    public static void onChat(ServerPlayer player, String message) {
        Session s = activeFor(player.getUUID());
        if (s == null) return;
        RandomSource r = player.getRandom();
        String reply = r.nextBoolean() ? message.toLowerCase(Locale.ROOT) : Lines.fakeReply(r);
        later(player.server, s, 40 + r.nextInt(60), p -> Speech.fakeChat(p, s.name(), reply));
    }

    public static void clear() {
        SESSIONS.clear();
    }

    // ------------------------------------------------------------------

    private static void later(MinecraftServer server, Session s, int ticks, Consumer<ServerPlayer> action) {
        Scheduler.later(server, ticks, () -> {
            if (!isActive(s.fakeId())) return;
            ServerPlayer p = server.getPlayerList().getPlayer(s.haunted());
            if (p != null && p.isAlive()) action.accept(p);
        });
    }

    private static void spawnMimic(ServerPlayer player, Session s) {
        ServerLevel level = (ServerLevel) player.level();
        MimicEntity mimic = ModEntities.MIMIC.create(level);
        if (mimic == null) return;
        BlockPos pos = HorrorUtil.findSpotAround(level, player, mimic.getType(), 16, 28, true);
        if (pos == null) return;
        mimic.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getRandom().nextFloat() * 360F, 0F);
        mimic.setup(s.name(), s.fakeId(), s.ownName() ? player.getUUID() : null, player.getUUID(), true);
        if (s.ownName()) {
            // it even holds what you hold
            mimic.setItemSlot(EquipmentSlot.MAINHAND, player.getMainHandItem().copy());
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                mimic.setItemSlot(slot, player.getItemBySlot(slot).copy());
            }
        }
        level.addFreshEntity(mimic);
    }

    private static void addToTabList(ServerPlayer player, Session s) {
        try {
            GameProfile profile = new GameProfile(s.fakeId(), s.name());
            if (s.ownName()) {
                profile.getProperties().putAll(player.getGameProfile().getProperties());
            }
            ServerPlayer fake = new ServerPlayer(player.server, (ServerLevel) player.level(), profile);
            player.connection.send(new ClientboundPlayerInfoUpdatePacket(
                    EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
                            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY),
                    List.of(fake)));
        } catch (Exception e) {
            MandelaHorror.LOGGER.warn("Could not add fake player to tab list", e);
        }
    }

    private FakeJoins() {}
}
