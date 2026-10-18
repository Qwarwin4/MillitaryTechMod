package com.qwarwin.militarytech.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

/**
 * BM-21 "Grad": 40 x 122 mm on a Ural. Aims by hand like a turret, or lays itself on coordinates typed into the
 * fire control screen (R). LMB fires the whole ripple, RMB one rocket.
 */
public class GradEntity extends TruckEntity implements FireControl {

    public GradEntity(EntityType<?> type, World world) {
        super(type, world, Specs.GRAD);
    }

    @Override
    public Munition rocket() {
        return Munition.GRAD;
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
