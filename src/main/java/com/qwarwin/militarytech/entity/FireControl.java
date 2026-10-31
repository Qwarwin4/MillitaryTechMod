package com.qwarwin.militarytech.entity;

import com.qwarwin.militarytech.util.Ballistics;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

/** Rocket launchers that lay themselves on typed-in coordinates: Grad, TOS-1A. */
public interface FireControl {
    Munition rocket();

    /** Trunnion of the pack in world space, close enough to every tube for aiming. */
    Vector3d rackOrigin();

    double maxElevation();

    /** {yaw, pitch, 1 if out of reach, charge}. */
    default float[] solution(BlockPos target) {
        return Ballistics.solve(rocket(), rackOrigin(), Vector3d.atCenterOf(target), maxElevation(), highArc(),
                charges(), barrel());
    }

    /** Propellant charges as fractions of the full muzzle velocity, smallest first; rockets have one. */
    default double[] charges() {
        return new double[]{1.0};
    }

    /** From the trunnion to the muzzle along the bore, blocks. */
    default double barrel() {
        return 0;
    }

    /** Plunging fire (above 45 degrees) instead of the flat trajectory. */
    default boolean highArc() {
        return false;
    }
}
