package net.latexdrones.infection;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.player.Player;

/**
 * Per-player infection state. The server keeps the authoritative copy ({@link #SERVER}, persisted by
 * {@link InfectionSavedData}); clients keep a mirror ({@link #CLIENT}) that is filled by the sync packet.
 * This class is free of client-only references so it is safe on a dedicated server.
 */
public final class InfectionData {
    private InfectionData() {
    }

    /** Infection meter maximum (1000 = 100%). */
    public static final int MAX = 1000;
    /** Length of the transformation sequence in ticks (7 seconds). */
    public static final int TRANSFORM_TICKS = 140;

    public static final class Entry {
        /** 0..MAX */
        public int infection;
        /** True once the player has become a latex creature. */
        public boolean transformed;
        /** Name of the mob form the player turns / has turned into. */
        public String form = "";
        /** &gt; 0 while the transformation animation is running. */
        public int transformTicks;
        /** The drone's serial number shown on the HUD. */
        public int unit;

        // ---- life as a drone (see DroneTasks)
        /** Current order from a scientist (DroneTasks.Task ordinal, 0 = none). */
        public int task;
        public int taskProgress;
        public int taskGoal;
        /** Number of orders completed - decides the drone's rank. */
        public int tasksDone;
        /** Asleep in standby mode on a charging pad. */
        public boolean standby;
        /** Server only: ticks spent in standby, pad position, guard spot, order cooldown. */
        public int standbyTicks;
        public long standbyPos;
        public int guardX;
        public int guardZ;
        public int orderCooldown;
    }

    public static final Map<UUID, Entry> SERVER = new ConcurrentHashMap<>();
    public static final Map<UUID, Entry> CLIENT = new ConcurrentHashMap<>();

    /** Server-side entry, created on demand. */
    public static Entry server(UUID id) {
        return SERVER.computeIfAbsent(id, k -> new Entry());
    }

    /** Entry for this player on whichever side we are on, or null. */
    public static Entry of(Player player) {
        return (player.level().isClientSide ? CLIENT : SERVER).get(player.getUUID());
    }

    public static boolean isTransformed(Player player) {
        Entry e = of(player);
        return e != null && e.transformed;
    }

    public static boolean isTransforming(Player player) {
        Entry e = of(player);
        return e != null && e.transformTicks > 0;
    }
}
