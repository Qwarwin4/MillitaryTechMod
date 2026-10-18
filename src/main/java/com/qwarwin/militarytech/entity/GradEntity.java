package com.qwarwin.militarytech.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * BM-21 "Grad": 40 x 122 mm on a Ural. Aims by hand like a turret, or lays itself on coordinates typed into the
 * fire control screen (R). LMB fires the whole ripple, RMB one rocket.
 */
public class GradEntity extends TruckEntity {

    public GradEntity(EntityType<?> type, World world) {
        super(type, world, Specs.GRAD);
    }

    /** Trunnion of the rack in world space, close enough to every tube for aiming. */
    public Vector3d rackOrigin() {
        return turretToWorld(spec.trunnion, getTurretYaw());
    }

    /**
     * Yaw and pitch that put a rocket on {@code target}, low arc. Null when it's out of reach; then the second
     * best (maximum range along the bearing) is in {@link #lastSolutionShort}.
     */
    @Nullable
    public float[] solution(BlockPos target) {
        Vector3d from = rackOrigin();
        Vector3d to = Vector3d.atCenterOf(target);
        Vector3d d = to.subtract(from);
        double dist = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (MathHelper.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90F;
        double lo = 0.5, hi = 50;
        if (range(from.y, to.y, hi) < dist) {
            lastSolutionShort = true;
            return new float[]{yaw, (float) -hi};
        }
        lastSolutionShort = false;
        for (int i = 0; i < 22; i++) {
            double mid = (lo + hi) / 2;
            if (range(from.y, to.y, mid) < dist) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return new float[]{yaw, (float) -((lo + hi) / 2)};
    }

    public boolean lastSolutionShort;

    /** Horizontal distance a rocket fired at {@code elevation} degrees covers before dropping to {@code targetY}. */
    public static double range(double fromY, double targetY, double elevation) {
        Munition m = Munition.GRAD;
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

    @Override
    protected boolean immobile() {
        return false;
    }
}
