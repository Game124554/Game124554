package net.latexdrones.infection;

import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Saves {@link InfectionData#SERVER} with the world (data/latexdrones_infection.dat of the overworld). */
public class InfectionSavedData extends SavedData {
    private static final String NAME = "latexdrones_infection";

    public static InfectionSavedData load(CompoundTag tag) {
        InfectionSavedData data = new InfectionSavedData();
        InfectionData.SERVER.clear();
        ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            if (!t.hasUUID("uuid")) {
                continue;
            }
            InfectionData.Entry e = new InfectionData.Entry();
            e.infection = t.getInt("infection");
            e.transformed = t.getBoolean("transformed");
            e.form = t.getString("form");
            e.unit = t.getInt("unit");
            e.task = t.getInt("task");
            e.taskProgress = t.getInt("taskProgress");
            e.taskGoal = t.getInt("taskGoal");
            e.tasksDone = t.getInt("tasksDone");
            e.guardX = t.getInt("guardX");
            e.guardZ = t.getInt("guardZ");
            InfectionData.SERVER.put(t.getUUID("uuid"), e);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, InfectionData.Entry> en : InfectionData.SERVER.entrySet()) {
            InfectionData.Entry e = en.getValue();
            if (e.infection <= 0 && !e.transformed) {
                continue;
            }
            CompoundTag t = new CompoundTag();
            t.putUUID("uuid", en.getKey());
            t.putInt("infection", e.infection);
            t.putBoolean("transformed", e.transformed);
            t.putString("form", e.form);
            t.putInt("unit", e.unit);
            t.putInt("task", e.task);
            t.putInt("taskProgress", e.taskProgress);
            t.putInt("taskGoal", e.taskGoal);
            t.putInt("tasksDone", e.tasksDone);
            t.putInt("guardX", e.guardX);
            t.putInt("guardZ", e.guardZ);
            list.add(t);
        }
        tag.put("players", list);
        return tag;
    }

    public static InfectionSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(InfectionSavedData::load, InfectionSavedData::new, NAME);
    }

    public static void markDirty(MinecraftServer server) {
        if (server != null) {
            get(server).setDirty();
        }
    }
}
