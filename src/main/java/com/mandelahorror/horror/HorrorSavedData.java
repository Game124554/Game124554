package com.mandelahorror.horror;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Per-world save data: which players already saw the intro broadcast in this world. */
public class HorrorSavedData extends SavedData {
    private static final String NAME = "mandelahorror";
    private final Set<UUID> introSeen = new HashSet<>();

    public static HorrorSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(HorrorSavedData::load, HorrorSavedData::new, NAME);
    }

    private static HorrorSavedData load(CompoundTag tag) {
        HorrorSavedData data = new HorrorSavedData();
        ListTag list = tag.getList("IntroSeen", Tag.TAG_INT_ARRAY);
        for (Tag t : list) {
            try {
                data.introSeen.add(NbtUtils.loadUUID(t));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (UUID id : this.introSeen) list.add(NbtUtils.createUUID(id));
        tag.put("IntroSeen", list);
        return tag;
    }

    /** @return true the first time this player is marked. */
    public boolean markIntroSeen(UUID player) {
        boolean added = this.introSeen.add(player);
        if (added) this.setDirty();
        return added;
    }
}
