package com.mandelahorror.horror;

import net.minecraft.server.level.ServerBossEvent;

import javax.annotation.Nullable;

/** Per-player haunting state (server side, not saved). */
public final class HauntState {
    public float influence;
    public long graceUntil;
    public long nextFakeJoin;
    public long nextAlternate;
    public long stabCooldownUntil;
    public boolean wasNight;
    @Nullable public ServerBossEvent bar;
    public int barTier = -1;
}
