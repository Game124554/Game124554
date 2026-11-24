package com.mandelahorror.horror;

import com.mandelahorror.HorrorConfig;
import com.mandelahorror.entity.AlternateEntity;
import com.mandelahorror.network.FxPacket;
import com.mandelahorror.network.IntroPacket;
import com.mandelahorror.network.ModNetwork;
import com.mandelahorror.registry.ModEntities;
import com.mandelahorror.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The "director": decides when the intro plays, when fake players join, when the Alternate shows up,
 * runs the M.A.D. influence bar and triggers the self-stab.
 */
public final class HorrorDirector {
    private static final Map<UUID, HauntState> STATES = new HashMap<>();

    // ------------------------------------------------------------------ events

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onLogin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onLogout(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> resetBar(newPlayer.getUUID()));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> resetBar(player.getUUID()));
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> FakeJoins.onChat(sender, message.signedContent()));
        ServerTickEvents.END_SERVER_TICK.register(HorrorDirector::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> onServerStopped());
    }

    private static void onLogin(ServerPlayer player) {
        boolean firstTime = HorrorSavedData.get(player.server).markIntroSeen(player.getUUID());
        if (firstTime || HorrorConfig.INTRO_EVERY_JOIN.get()) {
            ModNetwork.send(player, new IntroPacket(player.getGameProfile().getName()));
        }

        long now = player.server.getTickCount();
        HauntState s = new HauntState();
        RandomSource r = player.getRandom();
        s.graceUntil = now + 20L * HorrorConfig.GRACE_SECONDS.get();
        s.nextFakeJoin = s.graceUntil + randomTicks(r, HorrorConfig.FAKE_JOIN_MIN_SECONDS.get(), HorrorConfig.FAKE_JOIN_MAX_SECONDS.get()) / 2;
        s.nextAlternate = s.graceUntil + randomTicks(r, HorrorConfig.ALTERNATE_MIN_SECONDS.get(), HorrorConfig.ALTERNATE_MAX_SECONDS.get()) / 2;
        s.wasNight = player.level().isNight();
        STATES.put(player.getUUID(), s);
    }

    private static void onLogout(ServerPlayer player) {
        HauntState s = STATES.remove(player.getUUID());
        if (s != null && s.bar != null) s.bar.removeAllPlayers();
        FakeJoins.endAllFor(player.server, player.getUUID());
    }

    /** The player object changes on respawn / dimension change: rebuild the M.A.D. bar for the new one. */
    private static void resetBar(UUID id) {
        HauntState s = STATES.get(id);
        if (s != null && s.bar != null) {
            s.bar.removeAllPlayers();
            s.bar = null;
            s.barTier = -1;
        }
    }

    private static void onServerStopped() {
        for (HauntState s : STATES.values()) if (s.bar != null) s.bar.removeAllPlayers();
        STATES.clear();
        Scheduler.clear();
        FakeJoins.clear();
        SelfStab.clear();
    }

    private static void onServerTick(MinecraftServer server) {
        Scheduler.tick(server);
        long now = server.getTickCount();
        if (now % 20 == 0) FakeJoins.tick(server);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            HauntState s = STATES.get(player.getUUID());
            if (s == null) continue;
            if (now % 5 == 0) updateInfluence(player, s, now);
            if (now % 20 == 0) everySecond(player, s, now);
        }
    }

    // ------------------------------------------------------------------ logic

    private static void everySecond(ServerPlayer player, HauntState s, long now) {
        if (player.isSpectator() || now < s.graceUntil) return;
        ServerLevel level = (ServerLevel) player.level();
        RandomSource r = player.getRandom();

        boolean night = level.isNight();
        if (night && !s.wasNight && HorrorConfig.NIGHT_BROADCASTS.get()) {
            Speech.broadcast(player, Lines.night(r));
        }
        s.wasNight = night;

        if (HorrorConfig.FAKE_JOINS.get() && now >= s.nextFakeJoin) {
            s.nextFakeJoin = now + randomTicks(r, HorrorConfig.FAKE_JOIN_MIN_SECONDS.get(), HorrorConfig.FAKE_JOIN_MAX_SECONDS.get());
            FakeJoins.start(player, false);
        }

        if (HorrorConfig.ALTERNATE_SPAWNS.get() && now >= s.nextAlternate) {
            s.nextAlternate = now + randomTicks(r, HorrorConfig.ALTERNATE_MIN_SECONDS.get(), HorrorConfig.ALTERNATE_MAX_SECONDS.get());
            boolean dark = night || level.dimensionType().hasFixedTime()
                    || (player.getY() < 50 && !level.canSeeSky(player.blockPosition()));
            if ((dark || !HorrorConfig.ALTERNATE_NIGHT_ONLY.get()) && !player.isCreative()
                    && level.getDifficulty() != Difficulty.PEACEFUL && nearestAlternate(player, 96) == null) {
                spawnAlternateFor(player);
            }
        }
    }

    private static void updateInfluence(ServerPlayer player, HauntState s, long now) {
        AlternateEntity nearest = null;
        if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
            s.influence = Math.max(0F, s.influence - 5F);
        } else {
            nearest = nearestAlternate(player, 32);
            if (nearest != null) {
                float gain = 0.4F;
                if (HorrorUtil.isLookingAt(player, nearest, 0.97)) gain = 3.0F;      // staring at it
                else if (HorrorUtil.isLookingAt(player, nearest, 0.6)) gain = 1.2F;  // it's on screen
                if (player.distanceTo(nearest) < 8F) gain += 0.8F;
                s.influence = Math.min(100F, s.influence + (float) (gain * HorrorConfig.INFLUENCE_SPEED.get()));
            } else {
                s.influence = Math.max(0F, s.influence - 2F);
            }

            if (s.influence >= 100F && now >= s.stabCooldownUntil) {
                s.influence = 35F;
                s.stabCooldownUntil = now + 20L * 90;
                if (!SelfStab.trigger(player, nearest)) SelfStab.fallbackScare(player, nearest);
            }
        }
        updateBar(player, s, nearest);
    }

    private static void updateBar(ServerPlayer player, HauntState s, @Nullable AlternateEntity nearest) {
        if (!HorrorConfig.MAD_BOSS_BAR.get()) {
            if (s.bar != null) { s.bar.removeAllPlayers(); s.bar = null; }
            return;
        }
        boolean show = s.influence > 0.5F || nearest != null;
        if (!show) {
            if (s.bar != null) { s.bar.removeAllPlayers(); s.bar = null; s.barTier = -1; }
            return;
        }
        if (s.bar == null) {
            s.bar = new ServerBossEvent(barName(0), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
            s.bar.addPlayer(player);
            s.barTier = 0;
        }
        int tier = nearest == null ? 3 : s.influence < 30F ? 0 : s.influence < 70F ? 1 : 2;
        if (tier != s.barTier) {
            s.barTier = tier;
            s.bar.setName(barName(tier));
            s.bar.setDarkenScreen(tier == 2);
        }
        s.bar.setProgress(Mth.clamp(s.influence / 100F, 0F, 1F));
    }

    private static Component barName(int tier) {
        String text = switch (tier) {
            case 1 -> "M.A.D. // ALTERNATE NEARBY - DO NOT LOOK AT IT";
            case 2 -> "M.A.D. // IT IS TALKING TO YOU - LOOK AWAY";
            case 3 -> "M.A.D. // SIGNAL FADING";
            default -> "M.A.D. // SIGNAL DETECTED";
        };
        return Component.literal(text).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
    }

    /** Spawns an Alternate somewhere behind the player. Returns true on success. */
    public static boolean spawnAlternateFor(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        AlternateEntity alt = ModEntities.ALTERNATE.create(level);
        if (alt == null) return false;
        BlockPos pos = HorrorUtil.findSpotAround(level, player, alt.getType(), 20, 32, true);
        if (pos == null) pos = HorrorUtil.findSpotAround(level, player, alt.getType(), 8, 20, false);
        if (pos == null) return false;
        alt.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getRandom().nextFloat() * 360F, 0F);
        alt.setNatural(true);
        alt.setTarget(player);
        level.addFreshEntity(alt);
        level.playSound(null, pos, ModSounds.DRONE.get(), SoundSource.HOSTILE, 2.0F, 0.8F);
        ModNetwork.sendFx(player, FxPacket.STATIC_BURST, 12);
        return true;
    }

    @Nullable
    public static AlternateEntity nearestAlternate(ServerPlayer player, double radius) {
        List<AlternateEntity> list = player.level().getEntitiesOfClass(AlternateEntity.class,
                player.getBoundingBox().inflate(radius), e -> e.isAlive());
        return list.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
    }

    /** For the /mandela command. */
    public static void setInfluence(ServerPlayer player, float value) {
        HauntState s = STATES.get(player.getUUID());
        if (s != null) {
            s.influence = value;
            s.stabCooldownUntil = 0;
        }
    }

    private static long randomTicks(RandomSource r, int minSeconds, int maxSeconds) {
        int lo = Math.min(minSeconds, maxSeconds);
        int hi = Math.max(minSeconds, maxSeconds);
        return 20L * (lo + r.nextInt(hi - lo + 1));
    }

    private HorrorDirector() {}
}
