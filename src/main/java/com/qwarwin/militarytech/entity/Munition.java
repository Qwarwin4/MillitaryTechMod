package com.qwarwin.militarytech.entity;

import net.minecraft.util.math.vector.Vector3d;

/**
 * Everything a vehicle can fire or drop. Flight constants are in blocks and ticks. The same {@link #step} runs on
 * the server, on the client's copy of the projectile and in the HUD's impact predictor, so all three agree.
 */
public enum Munition {
    // name             kind               speed grav   drag   life dmg   power  model
    PKT(Kind.BULLET, Guidance.NONE, 12.0, 0.012, 0.995, 40, 7F, 0F, null, Tracer.RED),
    KORD(Kind.BULLET, Guidance.NONE, 12.0, 0.010, 0.996, 50, 14F, 0F, null, Tracer.RED),
    SHELL_30(Kind.SHELL, Guidance.NONE, 10.0, 0.020, 0.995, 70, 18F, 1.2F, null, Tracer.ORANGE),
    GSH_30(Kind.SHELL, Guidance.NONE, 12.0, 0.018, 0.996, 60, 24F, 1.4F, null, Tracer.ORANGE),
    APFSDS(Kind.DART, Guidance.NONE, 18.0, 0.004, 0.999, 60, 210F, 0F, "dart", Tracer.WHITE),
    HE_125(Kind.HE, Guidance.NONE, 11.0, 0.025, 0.997, 90, 40.0F, 5.6F, "shell_125", Tracer.NONE),
    HEAT_73(Kind.HEAT, Guidance.NONE, 7.0, 0.030, 0.995, 80, 140F, 1.6F, "shell_73", Tracer.NONE),
    GRAD(Kind.HE, Guidance.NONE, 5.5, 0.035, 0.999, 600, 45.0F, 5.5F, "grad_rocket", Tracer.NONE),
    TOS_220(Kind.THERMOBARIC, Guidance.NONE, 4.6, 0.035, 0.999, 600, 55.0F, 8.0F, "tos_rocket", Tracer.NONE),
    AA_30(Kind.SHELL, Guidance.NONE, 11.0, 0.018, 0.996, 60, 20F, 1.2F, null, Tracer.ORANGE),
    SAM(Kind.AIR, Guidance.IR, 9.0, 0.0, 1.0, 240, 300.0F, 0.0F, "sam", Tracer.NONE),
    S8KO(Kind.HEAT, Guidance.NONE, 8.0, 0.020, 0.999, 160, 220.0F, 3.5F, "s8_rocket", Tracer.NONE),
    FAB250(Kind.BOMB, Guidance.NONE, 0.0, 0.050, 0.995, 600, 0.0F, 9.5F, "fab250", Tracer.NONE),
    FAB500(Kind.BOMB, Guidance.NONE, 0.0, 0.050, 0.995, 600, 0.0F, 14.0F, "fab500", Tracer.NONE),
    FAB1500(Kind.BOMB, Guidance.NONE, 0.0, 0.050, 0.996, 600, 0.0F, 24.0F, "fab1500", Tracer.NONE),
    REFLEKS(Kind.HEAT, Guidance.BEAM, 3.6, 0.0, 1.0, 260, 340.0F, 3.6F, "refleks", Tracer.NONE),
    MALYUTKA(Kind.HEAT, Guidance.BEAM, 2.0, 0.0, 1.0, 420, 290.0F, 3.2F, "malyutka", Tracer.NONE),
    ATAKA(Kind.HEAT, Guidance.BEAM, 3.2, 0.0, 1.0, 320, 390.0F, 4.4F, "ataka", Tracer.NONE),
    R73(Kind.AIR, Guidance.IR, 7.0, 0.0, 1.0, 200, 240.0F, 0.0F, "r73", Tracer.NONE),
    R40(Kind.AIR, Guidance.IR, 8.0, 0.0, 1.0, 280, 320.0F, 0.0F, "r40", Tracer.NONE),
    KH29(Kind.HEAT, Guidance.LASER, 5.0, 0.0, 1.0, 320, 480.0F, 10.0F, "kh29", Tracer.NONE),
    KH59(Kind.HEAT, Guidance.CRUISE, 4.5, 0.0, 1.0, 900, 480.0F, 12.0F, "kh59", Tracer.NONE),
    ORESHNIK(Kind.BALLISTIC, Guidance.NONE, 0.0, 0.0, 1.0, 400, 0F, 0F, "oreshnik_missile", Tracer.NONE),
    WARHEAD(Kind.WARHEAD, Guidance.NONE, 0.0, 0.0, 1.0, 400, 0F, 5.0F, "warhead", Tracer.NONE),
    ORESHNIK_NUKE(Kind.BALLISTIC, Guidance.NONE, 0.0, 0.0, 1.0, 400, 0F, 0F, "oreshnik_missile", Tracer.NONE),
    NUKE(Kind.WARHEAD, Guidance.NONE, 0.0, 0.0, 1.0, 400, 0F, 0F, "nuke", Tracer.NONE);

    public enum Kind {
        BULLET, SHELL, DART, HE, HEAT, THERMOBARIC, BOMB, AIR, BALLISTIC, WARHEAD
    }

    public enum Guidance {
        NONE,
        /** follows the launcher's line of sight (SACLOS): Refleks, Malyutka, Ataka */
        BEAM,
        /** infrared homing on an aircraft locked before launch: R-73, R-40 */
        IR,
        /** flies to the spot the pilot keeps the sight on: Kh-29L */
        LASER,
        /** fire and forget at a point or vehicle picked at launch, comes down on the roof: Kh-59 */
        CRUISE
    }

    public enum Tracer {
        NONE, RED, ORANGE, WHITE
    }

    public final Kind kind;
    public final Guidance guidance;
    public final double speed;
    public final double gravity;
    public final double drag;
    public final int life;
    public final float damage;
    public final float power;
    /** model under models/entity/munition/, null for tracers only */
    public final String model;
    public final Tracer tracer;

    Munition(Kind kind, Guidance guidance, double speed, double gravity, double drag, int life, float damage, float power,
             String model, Tracer tracer) {
        this.kind = kind;
        this.guidance = guidance;
        this.speed = speed;
        this.gravity = gravity;
        this.drag = drag;
        this.life = life;
        this.damage = damage;
        this.power = power;
        this.model = model;
        this.tracer = tracer;
    }

    public boolean isGuided() {
        return guidance != Guidance.NONE;
    }

    /** Only loaded by hand with a nuclear warhead item, never from an ammo crate. */
    public boolean isNuclear() {
        return this == ORESHNIK_NUKE || this == NUKE;
    }

    public boolean isRocket() {
        return this == GRAD || this == S8KO || this == TOS_220;
    }

    /**
     * Proximity fuse reach around a target, 0 for contact fuses. The AA shell's programmable fuse sees further
     * around a big aircraft than around a small drone.
     */
    public double fuseRadius(double targetWidth) {
        switch (this) {
            case R73:
                return 4.5;
            case R40:
            case SAM:
                return 6.0;
            case AA_30:
                return Math.min(4.0, 0.7 + 0.8 * targetWidth);
            default:
                return 0;
        }
    }

    public boolean hasProximityFuse() {
        return kind == Kind.AIR || this == AA_30;
    }

    /** Turn rate of guided ones, degrees per tick. */
    public float turnRate() {
        switch (this) {
            case R73:
                return 14F;
            case SAM:
                return 11F;
            case R40:
                return 7F;
            case KH29:
                return 4F;
            case KH59:
                return 3F;
            case MALYUTKA:
                return 3.5F;
            default:
                return 6F;
        }
    }

    /** Ballistic velocity update for unguided ones. */
    public Vector3d step(Vector3d vel) {
        return new Vector3d(vel.x * drag, vel.y * drag - gravity, vel.z * drag);
    }

    /** Where something launched from {@code pos} with {@code vel} comes down, or null if it doesn't within its life. */
    public Vector3d[] trajectory(Vector3d pos, Vector3d vel, int maxTicks) {
        int n = Math.min(maxTicks, life);
        Vector3d[] out = new Vector3d[n + 1];
        out[0] = pos;
        Vector3d p = pos;
        Vector3d v = vel;
        for (int i = 1; i <= n; i++) {
            p = p.add(v);
            v = step(v);
            out[i] = p;
        }
        return out;
    }

    public String key() {
        return "munition.militarytech." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static Munition byId(int id) {
        Munition[] v = values();
        return id >= 0 && id < v.length ? v[id] : PKT;
    }
}
