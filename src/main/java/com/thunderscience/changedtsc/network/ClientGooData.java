package com.thunderscience.changedtsc.network;

import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side copy of every visible entity's goo stage (0-3) and goo color, used by the body overlay. */
public final class ClientGooData {
    private static final int WHITE_BIT = 4;
    private static final Map<Integer, Integer> DATA = new ConcurrentHashMap<>();

    private static volatile float selfPercent;
    private static volatile boolean selfWhite;

    private ClientGooData() {}

    /** The local player's own infection, 0-100. */
    public static void setSelf(float percent, boolean white) {
        selfPercent = percent;
        selfWhite = white;
    }

    public static float selfPercent() {
        return selfPercent;
    }

    public static boolean selfWhite() {
        return selfWhite;
    }

    public static void set(int entityId, int stage, boolean white) {
        if (stage <= 0) DATA.remove(entityId);
        else DATA.put(entityId, (stage & 3) | (white ? WHITE_BIT : 0));
    }

    public static int stage(Entity entity) {
        Integer v = DATA.get(entity.getId());
        return v == null ? 0 : v & 3;
    }

    public static boolean isWhite(Entity entity) {
        Integer v = DATA.get(entity.getId());
        return v != null && (v & WHITE_BIT) != 0;
    }

    public static void remove(int entityId) {
        DATA.remove(entityId);
    }

    public static void clear() {
        selfPercent = 0.0F;
        selfWhite = false;
        DATA.clear();
    }
}
