package com.qwarwin.militarytech.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

/**
 * TOS-1A "Solntsepyok": 24 thermobaric rockets on a T-72 chassis. Drives like a tank, the pack lays itself on
 * coordinates (R) like the Grad's. LMB fires the ripple, RMB a single rocket.
 */
public class TosEntity extends TankEntity implements FireControl {

    public TosEntity(EntityType<?> type, World world) {
        super(type, world, Specs.TOS1A);
    }

    @Override
    public Munition rocket() {
        return Munition.TOS_220;
    }

    @Override
    public Vector3d rackOrigin() {
        return turretToWorld(spec.trunnion, getTurretYaw());
    }

    @Override
    public double maxElevation() {
        return -spec.minPitch;
    }
}
