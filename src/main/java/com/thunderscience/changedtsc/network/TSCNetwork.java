package com.thunderscience.changedtsc.network;

import com.thunderscience.changedtsc.ChangedTSC;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class TSCNetwork {
    private static final String VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ChangedTSC.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private TSCNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(GooSyncPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GooSyncPacket::encode)
                .decoder(GooSyncPacket::new)
                .consumerMainThread(GooSyncPacket::handle)
                .add();
        CHANNEL.messageBuilder(GooPercentPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GooPercentPacket::encode)
                .decoder(GooPercentPacket::new)
                .consumerMainThread(GooPercentPacket::handle)
                .add();
        CHANNEL.messageBuilder(TreatPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TreatPacket::encode)
                .decoder(TreatPacket::new)
                .consumerMainThread(TreatPacket::handle)
                .add();
    }

    /** Tell everyone who can see this entity (and the entity itself if it is a player). */
    public static void sendGooStage(Entity entity, int stage, boolean white) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new GooSyncPacket(entity.getId(), stage, white));
    }

    /** Tell one player their own infection percent (for the paper doll). */
    public static void sendGooPercent(ServerPlayer player, float percent, boolean white) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new GooPercentPacket(percent, white));
    }

    public static void sendGooStageTo(ServerPlayer player, Entity entity, int stage, boolean white) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new GooSyncPacket(entity.getId(), stage, white));
    }
}
