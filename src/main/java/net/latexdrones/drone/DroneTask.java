package net.latexdrones.drone;

/** The jobs the hive (or a scientist) can give a drone player. */
public enum DroneTask {
    NONE("none", 0, 0, 0, false),
    /** Break / mop up latex spills. Spills are spawned near the drone when the task is given. */
    CLEAN("clean", 6, 9, 360, true),
    /** Hand in Raw Latex at a terminal or to a scientist. */
    COLLECT("collect", 4, 8, 480, false),
    /** Hand in Iron Ingots. */
    DELIVER("deliver", 3, 6, 480, false),
    /** Walk to a marked point. */
    PATROL("patrol", 1, 1, 300, true),
    /** Stay near the spot where the task was given (goal in seconds). */
    GUARD("guard", 60, 90, 300, true),
    /** Kill hostile, non-latex mobs. */
    HUNT("hunt", 3, 5, 600, true),
    /** Turn villagers into drones (latex syringe / dart / blade). */
    CONVERT("convert", 1, 1, 600, true);

    public final String key;
    public final int minGoal;
    public final int maxGoal;
    /** Time limit in seconds. */
    public final int seconds;
    /** True: finishes by itself when the goal is reached. False: must be reported (items handed in). */
    public final boolean autoComplete;

    DroneTask(String key, int minGoal, int maxGoal, int seconds, boolean autoComplete) {
        this.key = key;
        this.minGoal = minGoal;
        this.maxGoal = maxGoal;
        this.seconds = seconds;
        this.autoComplete = autoComplete;
    }

    public static DroneTask byId(int id) {
        DroneTask[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}
