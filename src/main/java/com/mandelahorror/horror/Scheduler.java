package com.mandelahorror.horror;

import com.mandelahorror.MandelaHorror;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Tiny server-side "run this N ticks later" queue. */
public final class Scheduler {
    private record Task(long at, Runnable run) {}

    private static final List<Task> TASKS = new ArrayList<>();

    public static void later(MinecraftServer server, int ticks, Runnable run) {
        synchronized (TASKS) {
            TASKS.add(new Task((long) server.getTickCount() + Math.max(1, ticks), run));
        }
    }

    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        List<Task> due = new ArrayList<>();
        synchronized (TASKS) {
            Iterator<Task> it = TASKS.iterator();
            while (it.hasNext()) {
                Task t = it.next();
                if (t.at() <= now) {
                    due.add(t);
                    it.remove();
                }
            }
        }
        for (Task t : due) {
            try {
                t.run().run();
            } catch (Exception e) {
                MandelaHorror.LOGGER.error("Mandela Horror scheduled task failed", e);
            }
        }
    }

    public static void clear() {
        synchronized (TASKS) {
            TASKS.clear();
        }
    }

    private Scheduler() {}
}
