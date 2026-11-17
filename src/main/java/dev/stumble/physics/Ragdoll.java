package dev.stumble.physics;

import java.util.Random;

/**
 * An "active" ragdoll: a position-based particle skeleton with joint limits and block collision,
 * driven for its first moments by a small brain that tries to stay on its feet (stagger steps,
 * flailing arms), then braces for the fall, then goes limp.
 *
 * A living body (one that was pushed, or fell from a height) may instead catch its balance, and
 * once it has come to rest it can be told to get back up, which pulls it through a crouch to a
 * standing pose so the game can swap the normal model back in.
 *
 * Units are blocks and game ticks. This class knows nothing about Minecraft.
 */
public final class Ragdoll {
    public static final int HEAD = 0, SH_R = 1, SH_L = 2, HIP_R = 3, HIP_L = 4, CHEST = 5,
            ELB_R = 6, ELB_L = 7, HAND_R = 8, HAND_L = 9,
            KNEE_R = 10, KNEE_L = 11, FOOT_R = 12, FOOT_L = 13, COUNT = 14;

    public enum State { STAGGER, FALLING, LIMP, RECOVERED, GETUP }

    /** How a body starts out. */
    public enum Mode {
        /** Hit or pushed: tries to keep its feet. */
        STAGGER,
        /** Knocked clean off its feet, or falling from a height: braces for the landing. */
        FALL,
        /** Thrown by an explosion: launched whole, flails in the air. */
        BLAST,
        /** No reaction at all. */
        LIMP;

        public static Mode of(int ordinal) {
            Mode[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : LIMP;
        }
    }

    /** {@link #getUpKind()}: not ready, back on its feet already, lying down. */
    public static final int NOT_READY = 0, STANDING = 1, LYING = 2;

    private static final int SUBSTEPS = 4;
    private static final int ITERATIONS = 8;
    private static final double H = 1.0 / SUBSTEPS;
    private static final double GRAVITY = 0.058;
    private static final double MAX_SPEED = 1.6;
    private static final double LOOKAHEAD = 4.0;
    private static final int STEP_TICKS = 3;
    /** Strength of the living balance muscle: higher means harder to knock over. */
    private static final double BALANCE = 0.005;

    private static final int[] SH = {SH_R, SH_L}, ELB = {ELB_R, ELB_L}, HAND = {HAND_R, HAND_L},
            HIP = {HIP_R, HIP_L}, KNEE = {KNEE_R, KNEE_L}, FOOT = {FOOT_R, FOOT_L};
    /** Bone pairs whose midpoint also collides, so bodies drape over edges instead of sinking in. */
    private static final int[] MID = {HIP_R, KNEE_R, HIP_L, KNEE_L, SH_R, HIP_R, SH_L, HIP_L, SH_R, SH_L};

    /** Rest pose in model pixels as (left, up, front), feet at the origin. */
    private static final double[] REST = {
            0, 28, 0,
            -5, 22, 0, 5, 22, 0,
            -2, 12, 0, 2, 12, 0,
            0, 17, 0,
            -5, 18, 0, 5, 18, 0,
            -5, 14, 0, 5, 14, 0,
            -2, 6, 0, 2, 6, 0,
            -2, 2, 0, 2, 2, 0};
    /**
     * Crouch the body passes through when getting up: weight over the feet, hands on the knees.
     * Same layout as REST.
     */
    private static final double[] CROUCH = {
            0, 20.5, 2.5,
            -5, 15, 1, 5, 15, 1,
            -2, 6, -3, 2, 6, -3,
            0, 10.5, -1,
            -5.5, 11, 3, 5.5, 11, 3,
            -4, 7.5, 5, 4, 7.5, 5,
            -2.5, 5.5, 2.5, 2.5, 5.5, 2.5,
            -2, 2, 0, 2, 2, 0};
    /** How strongly each particle floats, against its weight. Torso floats, limbs hang. */
    private static final double[] BUOYANCY = {1.15, 1.3, 1.3, 1.25, 1.25, 1.4, 0.9, 0.9, 0.85, 0.85, 0.95, 0.95, 0.85, 0.85};
    private static final double[] RADIUS_PX = {4, 2, 2, 2, 2, 2, 1.6, 1.8, 1.6, 1.8, 2, 2, 2, 2};
    private static final double[] MASS = {1.2, 1.6, 1.6, 1.8, 1.8, 2.5, 0.6, 0.6, 0.5, 0.5, 1.1, 1.1, 0.9, 0.9};
    /** How much of a hit each particle takes: the upper body gets shoved, the feet stay put. */
    private static final double[] HIT_SHARE = {1, 1, 1, 0.55, 0.55, 0.9, 0.8, 0.8, 0.6, 0.6, 0.3, 0.3, 0.05, 0.05};

    /** Current particle positions (x, y, z triples). */
    public final double[] x = new double[COUNT * 3];
    /** Positions at the start of the current tick, for render interpolation. */
    public final double[] snap = new double[COUNT * 3];
    public final double scale;

    private final double[] xp = new double[COUNT * 3];
    private final double[] v = new double[COUNT * 3];
    private final double[] vOld = new double[COUNT * 3];
    private final double[] cn = new double[COUNT * 3];
    private final double[] hold = new double[COUNT * 3];
    private final double[] w = new double[COUNT];
    private final double[] rad = new double[COUNT];
    private final boolean[] hit = new boolean[COUNT];
    private final boolean[] hitTick = new boolean[COUNT];
    private final boolean[] groundTick = new boolean[COUNT];
    private final boolean[] touch = new boolean[COUNT];
    private final boolean[] ground = new boolean[COUNT];
    /** Fluid surface height over each particle (NaN: none), its current, and whether it is lava. */
    private final double[] surface = new double[COUNT];
    private final double[] flow = new double[COUNT * 3];
    private final boolean[] lava = new boolean[COUNT];
    private final double[] probe = new double[4];
    private boolean wetAny, currentAny, torsoWet;

    private int nc;
    private final int[] ca = new int[64], cb = new int[64];
    private final double[] cmin = new double[64], cmax = new double[64];

    private final BoxList boxes = new BoxList();
    private final Random rnd;
    private final double unit, legRest, armLen, thighLen, torsoLen, headChestRest;

    // torso frame: left, down, front
    private double lx, ly, lz, dx, dy, dz, fx, fy, fz;
    private double comX, comY, comZ, comVx, comVy, comVz;

    // brain
    private State state = State.LIMP;
    private int age, staggerLeft, stepsLeft, stepCooldown, airTicks, fallTicks, limpTicks;
    private int stepFoot = -1, clutchSide = -1;
    private double stepT, brace, legK;
    private final double[] stepFrom = new double[3], stepTo = new double[3];
    private final double[][] plant = new double[2][3];
    // muscle outputs, refreshed every tick
    private double neckK, uprightK, balanceK;
    private final double[] strutK = new double[2], pinK = new double[2], armK = new double[2];
    private final boolean[] pinFull = new boolean[2];
    private final double[][] pin = new double[2][3], armOff = new double[2][3];

    private boolean asleep, forcedSleep;
    private boolean alive;
    private int recoveredTicks;
    // getting up
    private final double[] stand = new double[COUNT * 3], crouch = new double[COUNT * 3], start = new double[COUNT * 3],
            target = new double[COUNT * 3];
    private int getUpT, getUpTicks;
    private boolean getUpFromGround, getUpDone;
    /** Joint-limit strength, and how much of a limit correction is kept out of the velocity. */
    private double limitK = 0.5, limitCarry = 0.6;
    private int calm, sleepClock;
    private double impact;

    // scratch output of sweep()
    private double tx, ty, tz, tnx, tny, tnz;

    public Ragdoll(double scale, boolean thinLimbs, long seed) {
        this.scale = scale;
        this.unit = scale / 16.0;
        this.rnd = new Random(seed);
        for (int i = 0; i < COUNT; i++) {
            x[i * 3] = REST[i * 3] * unit;
            x[i * 3 + 1] = REST[i * 3 + 1] * unit;
            x[i * 3 + 2] = REST[i * 3 + 2] * unit;
            w[i] = 1.0 / MASS[i];
            rad[i] = RADIUS_PX[i] * unit;
        }
        if (thinLimbs) {
            for (int i : new int[]{ELB_R, ELB_L, HAND_R, HAND_L, KNEE_R, KNEE_L}) {
                rad[i] = 1.2 * unit;
            }
        }

        // rigid torso
        link(SH_R, SH_L); link(HIP_R, HIP_L);
        link(SH_R, HIP_R); link(SH_L, HIP_L); link(SH_R, HIP_L); link(SH_L, HIP_R);
        link(CHEST, SH_R); link(CHEST, SH_L); link(CHEST, HIP_R); link(CHEST, HIP_L);
        // neck: the head swings fore and aft about the shoulder line
        link(HEAD, SH_R); link(HEAD, SH_L);
        range(HEAD, CHEST, 0.87 * 11, 11);
        for (int s = 0; s < 2; s++) {
            int o = 1 - s;
            link(SH[s], ELB[s]); link(ELB[s], HAND[s]);
            link(HIP[s], KNEE[s]); link(KNEE[s], FOOT[s]);
            min(SH[s], HAND[s], 3.5);      // elbow cannot fold flat
            min(HIP[s], FOOT[s], 5);       // knee cannot fold flat
            min(HAND[s], CHEST, 4.5); min(ELB[s], CHEST, 4.5);
            min(HAND[s], HEAD, 5.5);
            min(ELB[s], HIP[s], 3);
            min(FOOT[s], CHEST, 7); min(KNEE[s], CHEST, 6);
            min(KNEE[s], SH[s], 8.5);      // hip flexion limit
            min(FOOT[s], HEAD, 8);
            min(KNEE[s], HIP[o], 4.5);
            min(FOOT[s], KNEE[o], 3.2);
        }
        min(HAND_R, HAND_L, 3);
        min(KNEE_R, KNEE_L, 3.6);
        min(FOOT_R, FOOT_L, 3.6);

        legRest = dist(HIP_R, FOOT_R);
        armLen = dist(SH_R, ELB_R) + dist(ELB_R, HAND_R);
        thighLen = dist(HIP_R, KNEE_R);
        torsoLen = 10 * unit;
        headChestRest = 11 * unit;
    }

    // ------------------------------------------------------------------ setup

    /**
     * Put the body in the world, feet at (ox, oy, oz), facing {@code yawDegrees} (Minecraft yaw).
     *
     * @param prone       lie face down instead of standing (swimming, gliding, sleeping)
     * @param armsForward start with the arms held out in front, zombie style
     */
    public void place(double ox, double oy, double oz, double yawDegrees, boolean prone, boolean armsForward) {
        pose(REST, ox, oy, oz, yawDegrees, prone, armsForward, x);
        System.arraycopy(x, 0, snap, 0, x.length);
        System.arraycopy(x, 0, xp, 0, x.length);
        java.util.Arrays.fill(v, 0);
        for (int s = 0; s < 2; s++) {
            plant[s][0] = x[FOOT[s] * 3];
            plant[s][1] = x[FOOT[s] * 3 + 1];
            plant[s][2] = x[FOOT[s] * 3 + 2];
        }
    }

    /** Write a model-pixel pose into world positions. */
    private void pose(double[] model, double ox, double oy, double oz, double yawDegrees, boolean prone,
                      boolean armsForward, double[] out) {
        double yaw = Math.toRadians(yawDegrees);
        double frx = -Math.sin(yaw), frz = Math.cos(yaw);   // front
        double lfx = Math.cos(yaw), lfz = Math.sin(yaw);    // left
        for (int i = 0; i < COUNT; i++) {
            double l = model[i * 3] * unit, u = model[i * 3 + 1] * unit, f = model[i * 3 + 2] * unit;
            if (armsForward && !prone && (i == ELB_R || i == ELB_L || i == HAND_R || i == HAND_L)) {
                boolean hand = i == HAND_R || i == HAND_L;
                u = 22 * unit;
                f = (hand ? 8 : 4) * unit;
            }
            double px, py, pz;
            if (prone) {
                // up becomes front, front becomes down
                px = ox + lfx * l + frx * (u - 16 * unit);
                py = oy + 3 * unit - f;
                pz = oz + lfz * l + frz * (u - 16 * unit);
            } else {
                px = ox + lfx * l + frx * f;
                py = oy + u;
                pz = oz + lfz * l + frz * f;
            }
            out[i * 3] = px;
            out[i * 3 + 1] = py;
            out[i * 3 + 2] = pz;
        }
    }

    /**
     * Start the simulation.
     *
     * @param vx,vy,vz velocity the body already had
     * @param jx,jy,jz the blow that set it off
     * @param mode     how it reacts; a stagger hit too hard, or a body already dropping fast, falls instead
     * @param alive    the body may catch its balance and can get back up; dead bodies never do
     */
    public void launch(double vx, double vy, double vz, double jx, double jy, double jz, Mode mode, boolean alive) {
        this.alive = alive;
        boolean blast = mode == Mode.BLAST, limp = mode == Mode.LIMP;
        double j = Math.sqrt(jx * jx + jy * jy + jz * jz);
        for (int i = 0; i < COUNT; i++) {
            double share = blast ? 0.8 + rnd.nextDouble() * 0.4 : HIT_SHARE[i];
            // a little noise so no two falls are alike and nothing balances on a knife edge
            v[i * 3] = vx + jx * share + (rnd.nextDouble() - 0.5) * 0.03;
            v[i * 3 + 1] = vy + jy * share;
            v[i * 3 + 2] = vz + jz * share + (rnd.nextDouble() - 0.5) * 0.03;
        }
        if (limp) {
            // dead on its feet: let the knees give way
            frame();
            for (int s = 0; s < 2; s++) {
                v[KNEE[s] * 3] += fx * 0.1;
                v[KNEE[s] * 3 + 2] += fz * 0.1;
            }
        }
        age = 0;
        asleep = false;
        calm = 0;
        stepFoot = -1;
        getUpDone = false;
        if (limp) {
            state = State.LIMP;
            limpTicks = 0;
        } else if (mode != Mode.STAGGER || j > 0.55 || vy < -0.35) {
            state = State.FALLING;
            fallTicks = 0;
            brace = 1;
            legK = blast ? 0 : 0.3;
        } else {
            startStagger(j);
        }
    }

    private void startStagger(double j) {
        state = State.STAGGER;
        double keep = Math.max(0.25, 1 - j / 0.8);
        staggerLeft = alive ? 45 : (int) ((14 + rnd.nextInt(20)) * keep);
        stepsLeft = alive ? 4 + rnd.nextInt(3) : 2 + rnd.nextInt(4);
        stepCooldown = 0;
        clutchSide = !alive && rnd.nextFloat() < 0.4f ? rnd.nextInt(2) : -1;
        recoveredTicks = 0;
    }

    // ------------------------------------------------------------------ queries

    public State state() {
        return state;
    }

    public boolean isAsleep() {
        return asleep;
    }

    public boolean isAlive() {
        return alive;
    }

    /** True once a get-up has finished; the body is standing where it was told to. */
    public boolean isUp() {
        return getUpDone;
    }

    /** Any part of the body is in water or lava. */
    public boolean inFluid() {
        return wetAny;
    }

    /** Point under the chest, at the lowest part of the body: where the game should keep the entity. */
    public double rootX() {
        return x[CHEST * 3];
    }

    public double rootZ() {
        return x[CHEST * 3 + 2];
    }

    public double rootY() {
        double low = Double.MAX_VALUE;
        for (int i = 0; i < COUNT; i++) {
            low = Math.min(low, x[i * 3 + 1] - rad[i]);
        }
        return low;
    }

    /** The chest is down near the ground, rather than up over the feet. */
    public boolean isLying() {
        return x[CHEST * 3 + 1] - rootY() < 0.45 * scale;
    }

    /** Which way the body would face standing up from here (Minecraft yaw): the way its head points. */
    public float headingYaw() {
        double hx = 0.5 * (x[SH_R * 3] + x[SH_L * 3]) - 0.5 * (x[HIP_R * 3] + x[HIP_L * 3]);
        double hz = 0.5 * (x[SH_R * 3 + 2] + x[SH_L * 3 + 2]) - 0.5 * (x[HIP_R * 3 + 2] + x[HIP_L * 3 + 2]);
        if (!isLying() || hx * hx + hz * hz < 1e-4) {
            frame();
            hx = fx;
            hz = fz;
        }
        return (float) Math.toDegrees(Math.atan2(-hx, hz));
    }

    /**
     * Whether a living body is ready to get up: {@link #NOT_READY}, {@link #STANDING} (it caught its
     * balance) or {@link #LYING} (it went down and has stopped moving, or is floating).
     */
    public int getUpKind() {
        if (!alive) {
            return NOT_READY;
        }
        if (state == State.RECOVERED && recoveredTicks > 6) {
            return STANDING;
        }
        if (state == State.LIMP) {
            if (wetAny && limpTicks > 25) {
                return LYING;
            }
            if (asleep || (limpTicks > 30 && calm > 8)) {
                return LYING;
            }
            if (limpTicks > 100) {
                return LYING;
            }
        }
        return NOT_READY;
    }

    /**
     * Get up and stand with feet at (ox, oy, oz) facing {@code yawDegrees}. A lying body goes through
     * a crouch first. {@link #isUp()} turns true after {@code ticks} ticks.
     */
    public void beginGetUp(double ox, double oy, double oz, double yawDegrees, int ticks) {
        pose(REST, ox, oy, oz, yawDegrees, false, false, stand);
        pose(CROUCH, ox, oy, oz, yawDegrees, false, false, crouch);
        getUpFromGround = isLying();
        System.arraycopy(x, 0, start, 0, x.length);
        getUpTicks = Math.max(1, ticks);
        getUpT = 0;
        getUpDone = false;
        state = State.GETUP;
        asleep = false;
        calm = 0;
        stepFoot = -1;
    }

    /** The body was killed: no more balance, no more getting up. */
    public void kill() {
        alive = false;
        if (state == State.STAGGER || state == State.RECOVERED) {
            beginFall();
        } else if (state == State.GETUP) {
            state = State.LIMP;
            limpTicks = 0;
        }
        asleep = false;
    }

    /** Give up balancing now (the authority on this body decided it fell). */
    public void forceFall() {
        if (state == State.STAGGER || state == State.RECOVERED) {
            beginFall();
        }
    }

    /** A blow to a body that is already a ragdoll: shove it, and knock a standing one off balance. */
    public void impulse(double jx, double jy, double jz) {
        for (int i = 0; i < COUNT; i++) {
            v[i * 3] += jx * HIT_SHARE[i];
            v[i * 3 + 1] += jy * HIT_SHARE[i];
            v[i * 3 + 2] += jz * HIT_SHARE[i];
        }
        asleep = false;
        calm = 0;
        if (state == State.LIMP) {
            limpTicks = 0;
        } else if (state == State.RECOVERED) {
            double j = Math.sqrt(jx * jx + jy * jy + jz * jz);
            if (j > 0.55) {
                beginFall();
            } else {
                startStagger(j);
            }
        }
    }

    /** Move the whole body without changing its velocity: used to keep it on the entity it stands for. */
    public void shift(double mx, double my, double mz) {
        for (int i = 0; i < COUNT; i++) {
            x[i * 3] += mx; x[i * 3 + 1] += my; x[i * 3 + 2] += mz;
            hold[i * 3] += mx; hold[i * 3 + 1] += my; hold[i * 3 + 2] += mz;
        }
        for (int s = 0; s < 2; s++) {
            plant[s][0] += mx; plant[s][1] += my; plant[s][2] += mz;
        }
    }

    /** Hardest landing of the body this tick (blocks per tick), then resets. */
    public double consumeImpact() {
        double i = impact;
        impact = 0;
        return i;
    }

    public double radius(int particle) {
        return rad[particle];
    }

    /** Shove every particle inside the box with the given horizontal velocity (something walked into it). */
    public void nudge(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double pvx, double pvz) {
        boolean any = false;
        for (int i = 0; i < COUNT; i++) {
            double px = x[i * 3], py = x[i * 3 + 1], pz = x[i * 3 + 2];
            if (px < minX || px > maxX || py < minY || py > maxY || pz < minZ || pz > maxZ) {
                continue;
            }
            v[i * 3] += (pvx - v[i * 3]) * 0.5;
            v[i * 3 + 2] += (pvz - v[i * 3 + 2]) * 0.5;
            v[i * 3 + 1] += 0.03;
            any = true;
        }
        if (any) {
            asleep = false;
            calm = 0;
            limpTicks = 0;
        }
    }

    /** Throw the body away from an explosion at (cx, cy, cz). Works on bodies already lying still. */
    public void blast(double cx, double cy, double cz, double radius, double strength) {
        boolean any = false;
        for (int i = 0; i < COUNT; i++) {
            double ex = x[i * 3] - cx, ey = x[i * 3 + 1] - cy, ez = x[i * 3 + 2] - cz;
            double d = Math.sqrt(ex * ex + ey * ey + ez * ez);
            if (d >= radius) {
                continue;
            }
            if (d < 1e-3) {
                ex = 0; ey = 1; ez = 0; d = 1;
            }
            double f = (1 - d / radius) * strength;
            v[i * 3] += ex / d * f;
            v[i * 3 + 1] += ey / d * f + f * 0.5;
            v[i * 3 + 2] += ez / d * f;
            any = true;
        }
        if (any) {
            asleep = false;
            calm = 0;
            limpTicks = 0;
        }
    }

    // ------------------------------------------------------------------ simulation

    public void tick(Environment env) {
        System.arraycopy(x, 0, snap, 0, x.length);
        if (state == State.GETUP && getUpDone) {
            return;
        }
        if (asleep) {
            if (++sleepClock % 20 != 0) {
                return;
            }
            System.arraycopy(x, 0, hold, 0, x.length);     // probe tick: undone unless the body starts to fall
        }

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (int i = 0; i < COUNT; i++) {
            double px = x[i * 3], py = x[i * 3 + 1], pz = x[i * 3 + 2];
            minX = Math.min(minX, px); maxX = Math.max(maxX, px);
            minY = Math.min(minY, py); maxY = Math.max(maxY, py);
            minZ = Math.min(minZ, pz); maxZ = Math.max(maxZ, pz);
        }
        wetAny = currentAny = torsoWet = false;
        for (int i = 0; i < COUNT; i++) {
            probe[0] = probe[1] = probe[2] = probe[3] = 0;
            double top = env.fluidSurface(x[i * 3], x[i * 3 + 1], x[i * 3 + 2], probe);
            surface[i] = top;
            flow[i * 3] = probe[0]; flow[i * 3 + 1] = probe[1]; flow[i * 3 + 2] = probe[2];
            lava[i] = probe[3] > 0.5;
            if (!Double.isNaN(top) && x[i * 3 + 1] - rad[i] < top) {
                wetAny = true;
                currentAny |= probe[0] * probe[0] + probe[2] * probe[2] > 1e-4;
                torsoWet |= i == CHEST;
            }
        }
        double pad = MAX_SPEED + 0.4;
        boxes.clear();
        env.collectBoxes(minX - pad, minY - pad, minZ - pad, maxX + pad, maxY + pad, maxZ + pad, boxes);

        think();

        for (int i = 0; i < COUNT; i++) {
            hitTick[i] = false;
            groundTick[i] = false;
        }
        for (int s = 0; s < SUBSTEPS; s++) {
            substep();
        }
        double fastest = 0, moved = 0;
        for (int i = 0; i < COUNT; i++) {
            int k = i * 3;
            touch[i] = hitTick[i];
            ground[i] = groundTick[i];
            fastest = Math.max(fastest, v[k] * v[k] + v[k + 1] * v[k + 1] + v[k + 2] * v[k + 2]);
            double mx = x[k] - snap[k], my = x[k + 1] - snap[k + 1], mz = x[k + 2] - snap[k + 2];
            moved = Math.max(moved, mx * mx + my * my + mz * mz);
        }

        if (asleep) {
            // A sleeping body only wakes when it starts to drop (the block under it was removed).
            // Judged on the body as a whole, so one limb that was frozen mid-sag does not count.
            double sink = 0, mass = 0;
            for (int i = 0; i < COUNT; i++) {
                sink += v[i * 3 + 1] * MASS[i];
                mass += MASS[i];
            }
            if (sink / mass < (forcedSleep ? -0.045 : -0.02)) {
                asleep = false;
                calm = 0;
                limpTicks = 0;
            } else {
                System.arraycopy(hold, 0, x, 0, x.length);
                System.arraycopy(hold, 0, snap, 0, x.length);
                java.util.Arrays.fill(v, 0);
            }
        } else if (state == State.LIMP) {
            limpTicks++;
            if (currentAny) {
                calm = 0;        // drifting with the current: never asleep
                return;
            }
            calm = moved < 0.012 * 0.012 ? calm + 1 : 0;      // judged by what the eye would see
            // give up on bodies that keep twitching: lying limp for five and a half seconds and slow,
            // or lying limp for eight seconds at all, and it is put to rest
            boolean resting = false;
            for (int i = 0; i < COUNT; i++) {
                resting |= touch[i];
            }
            if (calm > 25 || (resting && limpTicks > 110 && fastest < 0.15 * 0.15) || (resting && limpTicks > 160)) {
                asleep = true;
                forcedSleep = calm <= 25;      // frozen mid-fidget: needs a real drop to wake again
                sleepClock = 0;
                java.util.Arrays.fill(v, 0);
            }
        }
    }

    private void substep() {
        boolean lying = false;
        for (int i = 0; i < COUNT; i++) {
            lying |= touch[i];
        }
        boolean settle = state == State.LIMP && limpTicks > 60 && lying;
        // a body that has been lying limp for a while stops arguing with its own joints
        limitK = settle ? 0.25 : 0.5;
        limitCarry = settle ? 1.0 : 0.6;
        double gravity = GRAVITY;
        if (state == State.GETUP) {
            gravity *= Math.max(0, 1 - 2.0 * getUpT / getUpTicks);     // the muscles carry the weight
        }
        for (int i = 0; i < COUNT; i++) {
            int k = i * 3;
            double drag = settle ? (limpTicks > 110 ? 0.8 : 0.95) : 0.997;
            double vx = v[k] * drag, vy = (v[k + 1] - gravity * H) * drag, vz = v[k + 2] * drag;
            double top = surface[i];
            if (!Double.isNaN(top)) {
                // how much of this particle is under the surface
                double sub = (top - (x[k + 1] - rad[i])) / (2 * rad[i]);
                if (sub > 0) {
                    sub = Math.min(1, sub);
                    vy += GRAVITY * H * BUOYANCY[i] * sub * (lava[i] ? 0.9 : 1.0);
                    double cur = lava[i] ? 0.015 : 0.06, slow = (lava[i] ? 0.3 : 0.1) * sub;
                    double cx = flow[k] * cur, cy = flow[k + 1] * cur, cz = flow[k + 2] * cur;
                    vx = cx + (vx - cx) * (1 - slow);
                    vy = cy + (vy - cy) * (1 - slow);
                    vz = cz + (vz - cz) * (1 - slow);
                }
            }
            double sp = vx * vx + vy * vy + vz * vz;
            if (sp > MAX_SPEED * MAX_SPEED) {
                double c = MAX_SPEED / Math.sqrt(sp);
                vx *= c; vy *= c; vz *= c;
            }
            vOld[k] = vx; vOld[k + 1] = vy; vOld[k + 2] = vz;
            xp[k] = x[k]; xp[k + 1] = x[k + 1]; xp[k + 2] = x[k + 2];
            x[k] += vx * H; x[k + 1] += vy * H; x[k + 2] += vz * H;
            hit[i] = false;
            cn[k] = cn[k + 1] = cn[k + 2] = 0;
        }

        for (int it = 0; it < ITERATIONS; it++) {
            // weakest first: muscles, then joint limits, then bones, then the world. Whatever is solved
            // last wins, so a pinned foot cannot stretch a leg and nothing ends up inside a block.
            solveMuscles();
            solveJointLimits();
            solveDistances();
            if ((it & 1) == 1) {
                collide();
            }
        }

        for (int i = 0; i < COUNT; i++) {
            int k = i * 3;
            double vx = (x[k] - xp[k]) / H, vy = (x[k + 1] - xp[k + 1]) / H, vz = (x[k + 2] - xp[k + 2]) / H;
            // constraints may redirect a particle but not fling it: cap how much speed one substep can add
            double was = Math.sqrt(vOld[k] * vOld[k] + vOld[k + 1] * vOld[k + 1] + vOld[k + 2] * vOld[k + 2]);
            double now = Math.sqrt(vx * vx + vy * vy + vz * vz);
            double cap = was + 0.2;
            if (now > cap) {
                double c = cap / now;
                vx *= c; vy *= c; vz *= c;
            }
            if (hit[i]) {
                double nx = cn[k], ny = cn[k + 1], nz = cn[k + 2];
                double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (nl > 1e-9) {
                    nx /= nl; ny /= nl; nz /= nl;
                    double into = vOld[k] * nx + vOld[k + 1] * ny + vOld[k + 2] * nz;
                    if (!touch[i] && !hitTick[i] && i <= CHEST && -into > impact) {
                        impact = -into;
                    }
                    double vn = vx * nx + vy * ny + vz * nz;
                    double txv = vx - nx * vn, tyv = vy - ny * vn, tzv = vz - nz * vn;
                    double ts = Math.sqrt(txv * txv + tyv * tyv + tzv * tzv);
                    // Friction acts on the position as well as the velocity. Below the sticking speed a
                    // touching particle is put back where it was along the surface, which stops joint
                    // limits from slowly walking a resting body across the floor.
                    double keep = ts < 0.05 ? 0 : 0.78;
                    double back = (1 - keep) * H;
                    x[k] -= txv * back; x[k + 1] -= tyv * back; x[k + 2] -= tzv * back;
                    vn = Math.max(0, Math.min(vn, 0.15));     // no bounce, no pop-out
                    vx = txv * keep + nx * vn;
                    vy = tyv * keep + ny * vn;
                    vz = tzv * keep + nz * vn;
                    if (ny > 0.6) {
                        groundTick[i] = true;
                    }
                }
                hitTick[i] = true;
            }
            v[k] = vx; v[k + 1] = vy; v[k + 2] = vz;
        }
    }

    // ------------------------------------------------------------------ constraints

    private void link(int a, int b) {
        double d = dist(a, b);
        add(a, b, d, d);
    }

    private void range(int a, int b, double minPx, double maxPx) {
        add(a, b, minPx * unit, maxPx * unit);
    }

    private void min(int a, int b, double minPx) {
        add(a, b, minPx * unit, Double.MAX_VALUE);
    }

    private void add(int a, int b, double lo, double hi) {
        ca[nc] = a; cb[nc] = b; cmin[nc] = lo; cmax[nc] = hi;
        nc++;
    }

    private double dist(int a, int b) {
        double ex = x[b * 3] - x[a * 3], ey = x[b * 3 + 1] - x[a * 3 + 1], ez = x[b * 3 + 2] - x[a * 3 + 2];
        return Math.sqrt(ex * ex + ey * ey + ez * ez);
    }

    private void solveDistances() {
        for (int c = 0; c < nc; c++) {
            int a = ca[c] * 3, b = cb[c] * 3;
            double ex = x[b] - x[a], ey = x[b + 1] - x[a + 1], ez = x[b + 2] - x[a + 2];
            double d = Math.sqrt(ex * ex + ey * ey + ez * ez);
            if (d < 1e-9) {
                continue;
            }
            double target = d < cmin[c] ? cmin[c] : (d > cmax[c] ? cmax[c] : d);
            if (target == d) {
                continue;
            }
            double wa = w[ca[c]], wb = w[cb[c]];
            if (cmin[c] == cmax[c]) {
                double g = (d - target) / d / (wa + wb);
                x[a] += ex * g * wa; x[a + 1] += ey * g * wa; x[a + 2] += ez * g * wa;
                x[b] -= ex * g * wb; x[b + 1] -= ey * g * wb; x[b + 2] -= ez * g * wb;
            } else {
                double g = (d - target) / d / (wa + wb) * limitK;
                limit(a, ex * g * wa, ey * g * wa, ez * g * wa);
                limit(b, -ex * g * wb, -ey * g * wb, -ez * g * wb);
            }
        }
    }

    /** Pull two particles toward a target separation with stiffness k (0..1). */
    private void soft(int pa, int pb, double target, double k) {
        int a = pa * 3, b = pb * 3;
        double ex = x[b] - x[a], ey = x[b + 1] - x[a + 1], ez = x[b + 2] - x[a + 2];
        double d = Math.sqrt(ex * ex + ey * ey + ez * ez);
        if (d < 1e-9) {
            return;
        }
        double wa = w[pa], wb = w[pb];
        double g = (d - target) / d / (wa + wb) * k;
        x[a] += ex * g * wa; x[a + 1] += ey * g * wa; x[a + 2] += ez * g * wa;
        x[b] -= ex * g * wb; x[b + 1] -= ey * g * wb; x[b + 2] -= ez * g * wb;
    }

    /** Keep (child - parent) . n at or above lim. */
    private void planeMin(int child, int parent, double nx, double ny, double nz, double lim) {
        int c = child * 3, p = parent * 3;
        double d = (x[c] - x[p]) * nx + (x[c + 1] - x[p + 1]) * ny + (x[c + 2] - x[p + 2]) * nz;
        if (d >= lim) {
            return;
        }
        double wc = w[child], wp = w[parent];
        double g = (lim - d) / (wc + wp) * limitK;
        limit(c, nx * g * wc, ny * g * wc, nz * g * wc);
        limit(p, -nx * g * wp, -ny * g * wp, -nz * g * wp);
    }

    /**
     * Apply a joint-limit correction to the particle at offset k. Limits are soft, and most of the
     * move is also applied to the previous position, so it shifts the particle without handing it
     * speed. Limits that disagree with each other (a body folded into a heap) then settle instead
     * of pumping energy into the body.
     */
    private void limit(int k, double mx, double my, double mz) {
        x[k] += mx; x[k + 1] += my; x[k + 2] += mz;
        xp[k] += mx * limitCarry; xp[k + 1] += my * limitCarry; xp[k + 2] += mz * limitCarry;
    }

    private void frame() {
        lx = x[SH_L * 3] - x[SH_R * 3]; ly = x[SH_L * 3 + 1] - x[SH_R * 3 + 1]; lz = x[SH_L * 3 + 2] - x[SH_R * 3 + 2];
        double n = Math.sqrt(lx * lx + ly * ly + lz * lz) + 1e-12;
        lx /= n; ly /= n; lz /= n;
        dx = 0.5 * (x[HIP_R * 3] + x[HIP_L * 3] - x[SH_R * 3] - x[SH_L * 3]);
        dy = 0.5 * (x[HIP_R * 3 + 1] + x[HIP_L * 3 + 1] - x[SH_R * 3 + 1] - x[SH_L * 3 + 1]);
        dz = 0.5 * (x[HIP_R * 3 + 2] + x[HIP_L * 3 + 2] - x[SH_R * 3 + 2] - x[SH_L * 3 + 2]);
        n = Math.sqrt(dx * dx + dy * dy + dz * dz) + 1e-12;
        dx /= n; dy /= n; dz /= n;
        // back = left x down, front is its opposite
        double bx = ly * dz - lz * dy, by = lz * dx - lx * dz, bz = lx * dy - ly * dx;
        n = Math.sqrt(bx * bx + by * by + bz * bz) + 1e-12;
        fx = -bx / n; fy = -by / n; fz = -bz / n;
    }

    private void solveJointLimits() {
        frame();
        for (int s = 0; s < 2; s++) {
            double sg = s == 0 ? -1 : 1;     // outward = left * sg
            int hip = HIP[s], knee = KNEE[s], foot = FOOT[s];
            // hip: little backward swing, little crossing over, no splits, no thigh through the belly
            planeMin(knee, hip, fx, fy, fz, -0.35 * thighLen);
            planeMin(knee, hip, lx * sg, ly * sg, lz * sg, -0.25 * thighLen);
            planeMin(knee, hip, -lx * sg, -ly * sg, -lz * sg, -0.8 * thighLen);
            planeMin(knee, hip, dx, dy, dz, -0.55 * thighLen);

            // knee: a hinge that only folds one way, so the knee has to stay in front of the hip-foot line
            int h = hip * 3, k = knee * 3, f = foot * 3;
            double ax = x[f] - x[h], ay = x[f + 1] - x[h + 1], az = x[f + 2] - x[h + 2];
            double al = Math.sqrt(ax * ax + ay * ay + az * az);
            if (al < 1e-6) {
                continue;
            }
            ax /= al; ay /= al; az /= al;
            double dot = fx * ax + fy * ay + fz * az;
            double rx = fx - ax * dot, ry = fy - ay * dot, rz = fz - az * dot;
            double rl = Math.sqrt(rx * rx + ry * ry + rz * rz);
            if (rl < 0.25) {
                // leg points straight ahead: "in front of the knee" is then toward the head
                dot = -(dx * ax + dy * ay + dz * az);
                rx = -dx - ax * dot; ry = -dy - ay * dot; rz = -dz - az * dot;
                rl = Math.sqrt(rx * rx + ry * ry + rz * rz);
                if (rl < 1e-6) {
                    continue;
                }
            }
            rx /= rl; ry /= rl; rz /= rl;
            double c = (x[k] - 0.5 * (x[h] + x[f])) * rx + (x[k + 1] - 0.5 * (x[h + 1] + x[f + 1])) * ry
                    + (x[k + 2] - 0.5 * (x[h + 2] + x[f + 2])) * rz;
            if (c < 0) {
                double wk = w[knee], wh = w[hip], wf = w[foot];
                double g = -c / (wk + 0.25 * wh + 0.25 * wf) * limitK;
                limit(k, rx * g * wk, ry * g * wk, rz * g * wk);
                limit(h, -rx * g * wh * 0.5, -ry * g * wh * 0.5, -rz * g * wh * 0.5);
                limit(f, -rx * g * wf * 0.5, -ry * g * wf * 0.5, -rz * g * wf * 0.5);
            }
        }
    }

    private void solveMuscles() {
        if (state == State.LIMP) {
            return;
        }
        if (state == State.GETUP) {
            for (int i = 0; i < COUNT * 3; i++) {
                x[i] += (target[i] - x[i]) * 0.12;
            }
            return;
        }
        if (neckK > 0) {
            soft(HEAD, CHEST, headChestRest, neckK);
        }
        if (uprightK > 0) {
            // hold the shoulders above the hips
            double ex = 0.5 * (x[HIP_R * 3] + x[HIP_L * 3] - x[SH_R * 3] - x[SH_L * 3]);
            double ey = 0.5 * (x[HIP_R * 3 + 1] + x[HIP_L * 3 + 1] - x[SH_R * 3 + 1] - x[SH_L * 3 + 1]) + torsoLen;
            double ez = 0.5 * (x[HIP_R * 3 + 2] + x[HIP_L * 3 + 2] - x[SH_R * 3 + 2] - x[SH_L * 3 + 2]);
            double g = uprightK * 0.5;
            // a living body straightens up from the waist; a dying one is just pulled about
            double hg = alive ? g * 0.2 : g;
            for (int s = 0; s < 2; s++) {
                x[SH[s] * 3] += ex * g; x[SH[s] * 3 + 1] += ey * g; x[SH[s] * 3 + 2] += ez * g;
                x[HIP[s] * 3] -= ex * hg; x[HIP[s] * 3 + 1] -= ey * hg; x[HIP[s] * 3 + 2] -= ez * hg;
            }
        }
        if (balanceK > 0) {
            balance();
        }
        for (int s = 0; s < 2; s++) {
            if (strutK[s] > 0) {
                soft(HIP[s], FOOT[s], legRest * 0.98, strutK[s]);
            }
            if (pinK[s] > 0) {
                int f = FOOT[s] * 3;
                x[f] += (pin[s][0] - x[f]) * pinK[s];
                x[f + 2] += (pin[s][2] - x[f + 2]) * pinK[s];
                if (pinFull[s]) {
                    x[f + 1] += (pin[s][1] - x[f + 1]) * pinK[s];
                }
            }
            if (armK[s] > 0) {
                int hd = HAND[s] * 3, sh = SH[s] * 3;
                double ex = x[sh] + armOff[s][0] - x[hd], ey = x[sh + 1] + armOff[s][1] - x[hd + 1],
                        ez = x[sh + 2] + armOff[s][2] - x[hd + 2];
                double wa = w[HAND[s]], wb = w[SH[s]];
                double g = armK[s] / (wa + wb);
                x[hd] += ex * g * wa; x[hd + 1] += ey * g * wa; x[hd + 2] += ez * g * wa;
                x[sh] -= ex * g * wb; x[sh + 1] -= ey * g * wb; x[sh + 2] -= ez * g * wb;
            }
        }
    }

    /**
     * Living bodies only: keep the hips over the planted feet and at standing height. The push on
     * the hips is paid for by the feet, which the ground holds, so a small shove is soaked up by
     * the ankles and hips, and only a bigger one makes the body step.
     */
    private void balance() {
        int fr = FOOT_R * 3, fl = FOOT_L * 3, hr = HIP_R * 3, hl = HIP_L * 3;
        boolean gr = ground[FOOT_R] || stepFoot == 1, gl = ground[FOOT_L] || stepFoot == 0;
        if (stepFoot == 0) {
            gr = false;
        } else if (stepFoot == 1) {
            gl = false;
        }
        if (!gr && !gl) {
            return;
        }
        double sx, sz, sy;
        if (gr && gl) {
            sx = 0.5 * (x[fr] + x[fl]); sz = 0.5 * (x[fr + 2] + x[fl + 2]); sy = Math.min(x[fr + 1], x[fl + 1]);
        } else {
            int f = gr ? fr : fl;
            sx = x[f]; sz = x[f + 2]; sy = x[f + 1];
        }
        double hx = 0.5 * (x[hr] + x[hl]), hy = 0.5 * (x[hr + 1] + x[hl + 1]), hz = 0.5 * (x[hr + 2] + x[hl + 2]);
        double ex = sx - hx, ez = sz - hz;
        double el = Math.hypot(ex, ez);
        if (el > 0.6 * legRest) {
            return;                 // too far gone for the ankles; only a step can save it now
        }
        double ey = Math.max(0, sy + legRest * 0.92 - hy);
        double k = balanceK;
        for (int s = 0; s < 2; s++) {
            int h = HIP[s] * 3, sh = SH[s] * 3;
            x[h] += ex * k; x[h + 1] += ey * k; x[h + 2] += ez * k;
            x[sh] += ex * k * 0.8; x[sh + 1] += ey * k; x[sh + 2] += ez * k * 0.8;
        }
        x[CHEST * 3] += ex * k * 0.9; x[CHEST * 3 + 1] += ey * k; x[CHEST * 3 + 2] += ez * k * 0.9;
        double share = gr && gl ? 0.5 : 1.0;
        if (gr) {
            x[fr] -= ex * k * share; x[fr + 2] -= ez * k * share;
        }
        if (gl) {
            x[fl] -= ex * k * share; x[fl + 2] -= ez * k * share;
        }
    }

    // ------------------------------------------------------------------ collision

    private void collide() {
        if (boxes.size == 0) {
            return;
        }
        for (int i = 0; i < COUNT; i++) {
            int k = i * 3;
            if (sweep(x[k], x[k + 1], x[k + 2], rad[i], xp[k], xp[k + 1], xp[k + 2])) {
                x[k] = tx; x[k + 1] = ty; x[k + 2] = tz;
                cn[k] += tnx; cn[k + 1] += tny; cn[k + 2] += tnz;
                hit[i] = true;
            }
        }
        for (int m = 0; m < MID.length; m += 2) {
            int a = MID[m] * 3, b = MID[m + 1] * 3;
            double mx = 0.5 * (x[a] + x[b]), my = 0.5 * (x[a + 1] + x[b + 1]), mz = 0.5 * (x[a + 2] + x[b + 2]);
            double r = 0.5 * (rad[MID[m]] + rad[MID[m + 1]]);
            if (sweep(mx, my, mz, r, 0.5 * (xp[a] + xp[b]), 0.5 * (xp[a + 1] + xp[b + 1]), 0.5 * (xp[a + 2] + xp[b + 2]))) {
                double px = tx - mx, py = ty - my, pz = tz - mz;
                x[a] += px; x[a + 1] += py; x[a + 2] += pz;
                x[b] += px; x[b + 1] += py; x[b + 2] += pz;
                cn[a] += tnx; cn[a + 1] += tny; cn[a + 2] += tnz;
                cn[b] += tnx; cn[b + 1] += tny; cn[b + 2] += tnz;
                hit[MID[m]] = true;
                hit[MID[m + 1]] = true;
            }
        }
    }

    /**
     * Push a sphere out of every box it overlaps. (qx,qy,qz) is where it was at the start of the
     * substep, used to choose the way out when the centre is already inside a box.
     */
    private boolean sweep(double px, double py, double pz, double r, double qx, double qy, double qz) {
        boolean any = false;
        double nx = 0, ny = 0, nz = 0;
        final double[] b = boxes.data;
        final int n = boxes.size * 6;
        for (int k = 0; k < n; k += 6) {
            double x0 = b[k], y0 = b[k + 1], z0 = b[k + 2], x1 = b[k + 3], y1 = b[k + 4], z1 = b[k + 5];
            if (px < x0 - r || px > x1 + r || py < y0 - r || py > y1 + r || pz < z0 - r || pz > z1 + r) {
                continue;
            }
            double cx = px < x0 ? x0 : (px > x1 ? x1 : px);
            double cy = py < y0 ? y0 : (py > y1 ? y1 : py);
            double cz = pz < z0 ? z0 : (pz > z1 ? z1 : pz);
            double ex = px - cx, ey = py - cy, ez = pz - cz;
            double d2 = ex * ex + ey * ey + ez * ez;
            if (d2 >= r * r) {
                continue;
            }
            if (d2 > 1e-10) {
                double d = Math.sqrt(d2);
                double push = (r - d) / d;
                px += ex * push; py += ey * push; pz += ez * push;
                nx += ex / d; ny += ey / d; nz += ez / d;
            } else {
                // The centre is inside the box. Leave by the face we came in through if we know it,
                // otherwise by the nearest one, but never by a face that opens into another block:
                // on a floor of cubes that would shuffle the particle sideways underground forever.
                int face = 3;
                int rejected = 0;
                for (int tries = 0; tries < 6; tries++) {
                    double best = Double.MAX_VALUE;
                    int pick = -1;
                    boolean bestPref = false;
                    for (int f = 0; f < 6; f++) {
                        if ((rejected & (1 << f)) != 0) {
                            continue;
                        }
                        double depth;
                        boolean pref;
                        switch (f) {
                            case 0: depth = px - x0; pref = qx <= x0; break;
                            case 1: depth = x1 - px; pref = qx >= x1; break;
                            case 2: depth = py - y0; pref = qy <= y0; break;
                            case 3: depth = (y1 - py) * 0.5; pref = qy >= y1; break;   // when in doubt, up
                            case 4: depth = pz - z0; pref = qz <= z0; break;
                            default: depth = z1 - pz; pref = qz >= z1; break;
                        }
                        if (pick < 0 || (pref && !bestPref) || (pref == bestPref && depth < best)) {
                            best = depth;
                            pick = f;
                            bestPref = pref;
                        }
                    }
                    if (pick < 0) {
                        break;
                    }
                    double ox = px, oy = py, oz = pz;
                    switch (pick) {
                        case 0: ox = x0 - 1e-4; break;
                        case 1: ox = x1 + 1e-4; break;
                        case 2: oy = y0 - 1e-4; break;
                        case 3: oy = y1 + 1e-4; break;
                        case 4: oz = z0 - 1e-4; break;
                        default: oz = z1 + 1e-4; break;
                    }
                    if (!solidAt(ox, oy, oz)) {
                        face = pick;
                        break;
                    }
                    rejected |= 1 << pick;
                }
                switch (face) {
                    case 0: px = x0 - r; nx -= 1; break;
                    case 1: px = x1 + r; nx += 1; break;
                    case 2: py = y0 - r; ny -= 1; break;
                    case 3: py = y1 + r; ny += 1; break;
                    case 4: pz = z0 - r; nz -= 1; break;
                    default: pz = z1 + r; nz += 1; break;
                }
            }
            any = true;
        }
        tx = px; ty = py; tz = pz;
        tnx = nx; tny = ny; tnz = nz;
        return any;
    }

    private boolean solidAt(double px, double py, double pz) {
        final double[] b = boxes.data;
        final int n = boxes.size * 6;
        for (int k = 0; k < n; k += 6) {
            if (px > b[k] && px < b[k + 3] && py > b[k + 1] && py < b[k + 4] && pz > b[k + 2] && pz < b[k + 5]) {
                return true;
            }
        }
        return false;
    }

    /** Highest box top under (px, pz) between bottom and top, or NaN. Also NaN if something stands on that spot. */
    private double groundY(double px, double pz, double top, double bottom) {
        double best = Double.NaN;
        final double[] b = boxes.data;
        final int n = boxes.size * 6;
        for (int k = 0; k < n; k += 6) {
            if (px < b[k] || px > b[k + 3] || pz < b[k + 2] || pz > b[k + 5]) {
                continue;
            }
            double t = b[k + 4];
            if (t > top || t < bottom) {
                continue;
            }
            if (Double.isNaN(best) || t > best) {
                best = t;
            }
        }
        if (Double.isNaN(best)) {
            return best;
        }
        for (int k = 0; k < n; k += 6) {
            if (px < b[k] || px > b[k + 3] || pz < b[k + 2] || pz > b[k + 5]) {
                continue;
            }
            if (b[k + 1] < best + 0.45 && b[k + 4] > best + 0.01) {
                return Double.NaN;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ brain

    private void think() {
        age++;
        frame();
        double m = 0;
        comX = comY = comZ = comVx = comVy = comVz = 0;
        for (int i = 0; i < COUNT; i++) {
            double mi = MASS[i];
            m += mi;
            comX += x[i * 3] * mi; comY += x[i * 3 + 1] * mi; comZ += x[i * 3 + 2] * mi;
            comVx += v[i * 3] * mi; comVy += v[i * 3 + 1] * mi; comVz += v[i * 3 + 2] * mi;
        }
        comX /= m; comY /= m; comZ /= m; comVx /= m; comVy /= m; comVz /= m;

        neckK = uprightK = balanceK = 0;
        for (int s = 0; s < 2; s++) {
            strutK[s] = pinK[s] = armK[s] = 0;
        }
        if (state == State.LIMP) {
            return;
        }
        if (state == State.GETUP) {
            // the pose to be in by the end of this tick: from where it lay, through a crouch, to standing
            double p = Math.min(1.0, (getUpT + 1.0) / getUpTicks);
            for (int i = 0; i < COUNT * 3; i++) {
                double a, b, t;
                if (getUpFromGround && p < 0.5) {
                    a = start[i]; b = crouch[i]; t = p / 0.5;
                } else if (getUpFromGround) {
                    a = crouch[i]; b = stand[i]; t = (p - 0.5) / 0.5;
                } else {
                    a = start[i]; b = stand[i]; t = p;
                }
                t = t * t * (3 - 2 * t);
                target[i] = a + (b - a) * t;
            }
            if (++getUpT >= getUpTicks) {
                // done: settle exactly on the standing pose so the swap to the normal model is clean
                System.arraycopy(stand, 0, x, 0, x.length);
                java.util.Arrays.fill(v, 0);
                getUpDone = true;
            }
            return;
        }
        boolean torsoTouch = touch[CHEST] || touch[SH_R] || touch[SH_L] || touch[HIP_R] || touch[HIP_L] || touch[HEAD];

        if (state == State.STAGGER || state == State.RECOVERED) {
            staggerLeft--;
            airTicks = ground[FOOT_R] || ground[FOOT_L] ? 0 : airTicks + 1;
            double hipX = 0.5 * (x[HIP_R * 3] + x[HIP_L * 3]), hipY = 0.5 * (x[HIP_R * 3 + 1] + x[HIP_L * 3 + 1]),
                    hipZ = 0.5 * (x[HIP_R * 3 + 2] + x[HIP_L * 3 + 2]);
            double footX = 0.5 * (x[FOOT_R * 3] + x[FOOT_L * 3]), footZ = 0.5 * (x[FOOT_R * 3 + 2] + x[FOOT_L * 3 + 2]);
            double footY = Math.min(x[FOOT_R * 3 + 1], x[FOOT_L * 3 + 1]);
            boolean sunk = hipY - footY < 0.5 * legRest;
            boolean leaning = Math.hypot(hipX - footX, hipZ - footZ) > 0.9 * legRest;
            boolean lost = -dy < 0.45 || airTicks > 3 || sunk || leaning || torsoTouch || torsoWet;
            if (alive && !lost) {
                // a living body that has its feet under it again has caught itself
                boolean steady = -dy > 0.8 && airTicks == 0 && stepFoot < 0 && Math.hypot(comVx, comVz) < 0.025
                        && Math.hypot(hipX - footX, hipZ - footZ) < 0.35 * legRest;
                if (state == State.STAGGER && steady && age > 8) {
                    state = State.RECOVERED;
                    recoveredTicks = 0;
                } else if (state == State.STAGGER && staggerLeft <= 0) {
                    lost = true;            // still windmilling after two seconds: over it goes
                }
                if (state == State.RECOVERED) {
                    recoveredTicks++;
                }
            } else if (staggerLeft <= 0) {
                lost = true;
            }
            if (lost) {
                beginFall();
            }
        }
        if (state == State.STAGGER || state == State.RECOVERED) {
            staggerThink();
        } else {
            fallThink(torsoTouch);
        }
    }

    private void beginFall() {
        if (state == State.FALLING || state == State.LIMP) {
            return;
        }
        state = State.FALLING;
        fallTicks = 0;
        brace = 1;
        legK = 0.5;
        stepFoot = -1;
        if (Math.hypot(comVx, comVz) < 0.03) {
            // passing out on the spot: tip over in some direction instead of folding straight down
            double a = rnd.nextDouble() * Math.PI * 2;
            double px = Math.cos(a) * 0.06, pz = Math.sin(a) * 0.06;
            for (int i = 0; i <= CHEST; i++) {
                v[i * 3] += px * HIT_SHARE[i];
                v[i * 3 + 2] += pz * HIT_SHARE[i];
            }
        }
    }

    private void staggerThink() {
        double m = alive ? 1.0 : Math.min(1.0, 0.4 + staggerLeft / 12.0);
        neckK = 0.3 * m;
        uprightK = 0.22 * m;
        balanceK = alive ? BALANCE : 0;

        // where the body is about to be, against where the feet are
        double cpx = comX + comVx * LOOKAHEAD, cpz = comZ + comVz * LOOKAHEAD;
        double sx = 0.5 * (x[FOOT_R * 3] + x[FOOT_L * 3]), sz = 0.5 * (x[FOOT_R * 3 + 2] + x[FOOT_L * 3 + 2]);
        double ex = cpx - sx, ez = cpz - sz;
        double el = Math.hypot(ex, ez);

        if (stepFoot < 0) {
            if (stepCooldown > 0) {
                stepCooldown--;
            } else if (el > 0.13 * scale && stepsLeft > 0) {
                tryStep(cpx, cpz, sx, sz, ex / el, ez / el);
            }
        }

        for (int s = 0; s < 2; s++) {
            int f = FOOT[s] * 3;
            if (stepFoot == s) {
                stepT += 1.0 / STEP_TICKS;
                double t = Math.min(1, stepT);
                pin[s][0] = stepFrom[0] + (stepTo[0] - stepFrom[0]) * t;
                pin[s][1] = stepFrom[1] + (stepTo[1] - stepFrom[1]) * t + Math.sin(Math.PI * t) * 0.16 * scale;
                pin[s][2] = stepFrom[2] + (stepTo[2] - stepFrom[2]) * t;
                pinK[s] = 0.6 * m;
                pinFull[s] = true;
                if (stepT >= 1) {
                    stepFoot = -1;
                    stepCooldown = 1;
                    plant[s][0] = stepTo[0]; plant[s][1] = stepTo[1]; plant[s][2] = stepTo[2];
                }
            } else {
                if (Math.hypot(x[f] - plant[s][0], x[f + 2] - plant[s][2]) > 0.22 * scale) {
                    plant[s][0] = x[f]; plant[s][2] = x[f + 2];    // the foot slipped
                }
                pin[s][0] = plant[s][0]; pin[s][2] = plant[s][2];
                pinK[s] = ground[FOOT[s]] ? 0.55 * m : 0;
                pinFull[s] = false;
                strutK[s] = 0.55 * m;
            }
        }

        // arms: one may clutch the wound, the rest windmill against the lean
        double lean = Math.min(1, el / (0.35 * scale));
        double nx = el > 1e-6 ? ex / el : 0, nz = el > 1e-6 ? ez / el : 0;
        for (int s = 0; s < 2; s++) {
            double sg = s == 0 ? -1 : 1;
            if (s == clutchSide) {
                int sh = SH[s] * 3;
                armOff[s][0] = x[CHEST * 3] + fx * 3 * unit - x[sh];
                armOff[s][1] = x[CHEST * 3 + 1] + fy * 3 * unit + unit - x[sh + 1];
                armOff[s][2] = x[CHEST * 3 + 2] + fz * 3 * unit - x[sh + 2];
                armK[s] = 0.25 * m;
            } else if (state == State.RECOVERED) {
                // steadied: arms come down to the sides
                armOff[s][0] = lx * sg * 0.15 * armLen - dx * 0.95 * armLen;
                armOff[s][1] = ly * sg * 0.15 * armLen - dy * 0.95 * armLen;
                armOff[s][2] = lz * sg * 0.15 * armLen - dz * 0.95 * armLen;
                armK[s] = 0.1;
            } else {
                double wave = Math.sin(age * 0.9 + s * 2.1);
                double ox = lx * sg * 0.75 - nx * 0.5 * lean + fx * 0.35 * wave;
                double oy = 0.15 + 0.35 * lean + 0.2 * Math.sin(age * 1.3 + s);
                double oz = lz * sg * 0.75 - nz * 0.5 * lean + fz * 0.35 * wave;
                double ol = Math.sqrt(ox * ox + oy * oy + oz * oz) + 1e-9;
                double reach = armLen * 0.95 / ol;
                armOff[s][0] = ox * reach; armOff[s][1] = oy * reach; armOff[s][2] = oz * reach;
                armK[s] = 0.14 * m;
            }
        }
    }

    private void tryStep(double cpx, double cpz, double sx, double sz, double nx, double nz) {
        double pr = (x[FOOT_R * 3] - sx) * nx + (x[FOOT_R * 3 + 2] - sz) * nz;
        double pl = (x[FOOT_L * 3] - sx) * nx + (x[FOOT_L * 3 + 2] - sz) * nz;
        int s = pr <= pl ? 0 : 1;                     // move the trailing foot
        if (!ground[FOOT[1 - s]]) {
            return;                                    // nothing to stand on while stepping
        }
        double sg = s == 0 ? -1 : 1;
        double hlx = lx, hlz = lz;
        double hn = Math.hypot(hlx, hlz);
        if (hn > 1e-3) {
            hlx /= hn; hlz /= hn;
        }
        double tgx = cpx + nx * 0.1 * scale + hlx * sg * 0.1 * scale;
        double tgz = cpz + nz * 0.1 * scale + hlz * sg * 0.1 * scale;
        double hx = 0.5 * (x[HIP_R * 3] + x[HIP_L * 3]), hz = 0.5 * (x[HIP_R * 3 + 2] + x[HIP_L * 3 + 2]);
        double rx = tgx - hx, rz = tgz - hz;
        double rl = Math.hypot(rx, rz);
        // a dying body lunges; a living one takes steps its legs can actually reach
        double reach = (alive ? 0.42 : 0.68) * scale;
        if (rl > reach) {
            tgx = hx + rx / rl * reach;
            tgz = hz + rz / rl * reach;
        }
        int f = FOOT[s] * 3;
        double sole = x[f + 1] - rad[FOOT[s]];
        double gy = groundY(tgx, tgz, sole + 0.6, sole - 0.9);
        if (Double.isNaN(gy)) {
            stepsLeft = 0;                             // a ledge or a wall: nowhere to put the foot
            return;
        }
        stepFrom[0] = x[f]; stepFrom[1] = x[f + 1]; stepFrom[2] = x[f + 2];
        stepTo[0] = tgx; stepTo[1] = gy + rad[FOOT[s]]; stepTo[2] = tgz;
        stepT = 0;
        stepFoot = s;
        stepsLeft--;
    }

    private void fallThink(boolean torsoTouch) {
        fallTicks++;
        legK *= 0.55;
        if (torsoTouch || torsoWet) {
            brace *= 0.78;
        }
        if (brace < 0.06 || fallTicks > 60) {
            state = State.LIMP;
            limpTicks = 0;
            return;
        }
        neckK = 0.35 * brace;
        strutK[0] = strutK[1] = legK;

        // which way are we going down?
        double hx = comVx, hz = comVz;
        double hl = Math.hypot(hx, hz);
        if (hl < 0.03) {
            hx = -dx; hz = -dz;
            hl = Math.hypot(hx, hz);
        }
        if (hl < 1e-4) {
            hx = fx; hz = fz;
            hl = Math.hypot(hx, hz) + 1e-9;
        }
        hx /= hl; hz /= hl;

        boolean anyTouch = false;
        for (int i = 0; i < COUNT; i++) {
            anyTouch |= touch[i];
        }
        boolean rising = comVy > 0.05 && !anyTouch;
        for (int s = 0; s < 2; s++) {
            double sg = s == 0 ? -1 : 1;
            double ox, oy, oz;
            if (rising) {
                // thrown into the air: flail
                double wave = Math.sin(age * 1.4 + s * 3.0);
                ox = lx * sg * 0.7 + fx * 0.6 * wave;
                oy = 0.5 + 0.3 * Math.cos(age * 1.1 + s);
                oz = lz * sg * 0.7 + fz * 0.6 * wave;
            } else {
                // going down: get the hands between the body and the ground
                ox = hx * 0.75 + lx * sg * 0.25;
                oy = -0.6;
                oz = hz * 0.75 + lz * sg * 0.25;
            }
            double ol = Math.sqrt(ox * ox + oy * oy + oz * oz) + 1e-9;
            double reach = armLen * 0.95 / ol;
            armOff[s][0] = ox * reach; armOff[s][1] = oy * reach; armOff[s][2] = oz * reach;
            armK[s] = 0.2 * brace;
        }
    }
}
