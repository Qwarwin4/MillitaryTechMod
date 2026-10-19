package com.qwarwin.militarytech.util;

import com.qwarwin.militarytech.entity.Munition;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

/** Firing solutions for unguided rockets, using the same flight model as the rockets themselves. */
public final class Ballistics {

    /** Horizontal distance covered at {@code elevation} degrees before dropping to {@code targetY}. */
    public static double range(Munition m, double fromY, double targetY, double elevation) {
        double e = Math.toRadians(elevation);
        double vx = Math.cos(e) * m.speed, vy = Math.sin(e) * m.speed;
        double x = 0, y = fromY;
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
     * Low-arc solution from {@code from} to {@code to}: {yaw, pitch (xRot), 1 if out of reach else 0}. Out of
     * reach gives the maximum-range elevation along the bearing.
     */
    public static float[] solve(Munition m, Vector3d from, Vector3d to, double maxElevation) {
        return solve(m, from, to, maxElevation, false);
    }

    /**
     * {@code high}: the plunging solution above 45 degrees (over hills and houses, onto roofs), as howitzers fire;
     * otherwise the flat one at up to 45.
     */
    public static float[] solve(Munition m, Vector3d from, Vector3d to, double maxElevation, boolean high) {
        Vector3d d = to.subtract(from);
        double dist = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (MathHelper.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90F;
        if (high && maxElevation > 46) {
            // range falls as the barrel rises past 45: the steeper it goes, the shorter
            double lo = 45, hi = maxElevation;
            if (range(m, from.y, to.y, lo) < dist) {
                return new float[]{yaw, (float) -lo, 1F};
            }
            if (range(m, from.y, to.y, hi) > dist) {
                // too close even straight up the highest it goes: lob it as steep as it'll go, it'll land past
                return new float[]{yaw, (float) -hi, 0F};
            }
            for (int i = 0; i < 22; i++) {
                double mid = (lo + hi) / 2;
                if (range(m, from.y, to.y, mid) > dist) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            return new float[]{yaw, (float) -((lo + hi) / 2), 0F};
        }
        double lo = 0.5, hi = Math.min(maxElevation, 45);
        if (range(m, from.y, to.y, hi) < dist) {
            return new float[]{yaw, (float) -hi, 1F};
        }
        for (int i = 0; i < 22; i++) {
            double mid = (lo + hi) / 2;
            if (range(m, from.y, to.y, mid) < dist) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return new float[]{yaw, (float) -((lo + hi) / 2), 0F};
    }

    private Ballistics() {
    }
}
