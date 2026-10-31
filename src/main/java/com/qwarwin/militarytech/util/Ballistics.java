package com.qwarwin.militarytech.util;

import com.qwarwin.militarytech.entity.Munition;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

/** Firing solutions for unguided rockets, using the same flight model as the rockets themselves. */
public final class Ballistics {

    /** Horizontal distance covered at {@code elevation} degrees before dropping to {@code targetY}. */
    public static double range(Munition m, double fromY, double targetY, double elevation) {
        return range(m, m.speed, 0, fromY, targetY, elevation);
    }

    /**
     * The same for a shot at {@code speed} (the charge) leaving a barrel {@code barrel} blocks long: the muzzle is
     * that far from the trunnion along the bore, higher and further out the steeper the gun is laid.
     */
    public static double range(Munition m, double speed, double barrel, double fromY, double targetY, double elevation) {
        double e = Math.toRadians(elevation);
        double vx = Math.cos(e) * speed, vy = Math.sin(e) * speed;
        double x = Math.cos(e) * barrel, y = fromY + Math.sin(e) * barrel;
        for (int t = 0; t < m.life; t++) {
            double nx = x + vx, ny = y + vy;
            if (vy < 0 && ny <= targetY) {
                double k = (y - targetY) / Math.max(1e-6, y - ny);
                return x + (nx - x) * k;
            }
            x = nx;
            y = ny;
            vx *= m.drag;
            vy = vy * m.drag - m.gravity;
        }
        return x;
    }

    /**
     * Low-arc solution from {@code from} to {@code to}: {yaw, pitch (xRot), 1 if out of reach else 0, charge}.
     * Out of reach gives the maximum-range elevation along the bearing.
     */
    public static float[] solve(Munition m, Vector3d from, Vector3d to, double maxElevation) {
        return solve(m, from, to, maxElevation, false);
    }

    public static float[] solve(Munition m, Vector3d from, Vector3d to, double maxElevation, boolean high) {
        return solve(m, from, to, maxElevation, high, FULL, 0);
    }

    private static final double[] FULL = {1.0};
    /** the flat trajectory goes no higher than this when a smaller charge can do it at all */
    private static final double LOW_PREFERRED = 30;

    /**
     * {@code high}: the plunging solution above 45 degrees (over hills and houses, onto roofs), as howitzers fire;
     * otherwise the flat one at up to 45. {@code charges}: fractions of the full muzzle velocity, smallest first.
     * Plunging fire takes the smallest charge that reaches (the steepest fall); flat fire the smallest that does it
     * at up to 30 degrees, so short shots aren't laid a degree above the horizon where any error is a miss.
     */
    public static float[] solve(Munition m, Vector3d from, Vector3d to, double maxElevation, boolean high,
                                double[] charges, double barrel) {
        Vector3d d = to.subtract(from);
        double dist = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (MathHelper.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90F;
        if (high && maxElevation > 46) {
            for (int i = 0; i < charges.length; i++) {
                double v = m.speed * charges[i];
                if (range(m, v, barrel, from.y, to.y, 45) < dist) {
                    continue;
                }
                if (range(m, v, barrel, from.y, to.y, maxElevation) > dist) {
                    // too close even straight up the highest it goes: lob it as steep as it'll go, it'll land past
                    return new float[]{yaw, (float) -maxElevation, 0F, (float) charges[i]};
                }
                double lo = 45, hi = maxElevation;
                for (int k = 0; k < 24; k++) {
                    double mid = (lo + hi) / 2;
                    if (range(m, v, barrel, from.y, to.y, mid) > dist) {
                        lo = mid;
                    } else {
                        hi = mid;
                    }
                }
                return new float[]{yaw, (float) -((lo + hi) / 2), 0F, (float) charges[i]};
            }
            return new float[]{yaw, -45F, 1F, (float) charges[charges.length - 1]};
        }
        double top = Math.min(maxElevation, 45);
        float[] fallback = null;
        for (int i = 0; i < charges.length; i++) {
            double v = m.speed * charges[i];
            if (range(m, v, barrel, from.y, to.y, top) < dist) {
                continue;
            }
            double lo = 0.5, hi = top;
            for (int k = 0; k < 24; k++) {
                double mid = (lo + hi) / 2;
                if (range(m, v, barrel, from.y, to.y, mid) < dist) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            double el = (lo + hi) / 2;
            fallback = new float[]{yaw, (float) -el, 0F, (float) charges[i]};
            if (el <= LOW_PREFERRED) {
                return fallback;
            }
        }
        return fallback != null ? fallback : new float[]{yaw, (float) -top, 1F, (float) charges[charges.length - 1]};
    }

    private Ballistics() {
    }
}
