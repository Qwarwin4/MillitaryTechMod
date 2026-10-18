package com.qwarwin.militarytech.entity;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything that differs between vehicles of the same kind: numbers, model names, crew seats, weapons.
 * Local coordinates: x to the vehicle's left, y up, z forward, in blocks, same as the model generator.
 */
public final class VehicleSpec {
    public enum Category {
        TANK, TRUCK, AIRCRAFT, TURRET
    }

    public final String id;
    public final Category category;

    public float health = 300F;
    public Armor armor = Armor.HEAVY;
    public float width = 3.5F, height = 2.3F;
    public final List<AxisAlignedBB> hitBoxes = new ArrayList<>();
    /** where each passenger's feet go; seat 0 drives */
    public Vector3d[] seats = {Vector3d.ZERO};
    /** second seat is a separate gunner; otherwise the driver does both and the rest are passengers */
    public boolean separateGunner = true;
    /** crew can't be shot or fragged while inside */
    public boolean enclosed = true;

    // driving (blocks/tick, degrees/tick)
    public double maxSpeed = 0.45, reverseSpeed = 0.2, accel = 0.012, turnRate = 2.4;
    public float stepHeight = 1.1F;
    public boolean tracked = true;
    public boolean amphibious;

    // turret / launcher
    public boolean hasTurret = true;
    public Vector3d pivot = new Vector3d(0, 1.45, 0);
    public Vector3d trunnion = new Vector3d(0, 0.4, 0.8);
    public float traverse = 2.5F;
    /** elevation limits in xRot terms, negative is up */
    public float minPitch = -18F, maxPitch = 6F;
    /** how far the turret may turn off the hull's nose, 180 for all round */
    public float yawLimit = 180F;
    /** gunner's eye in sight view, turret-local */
    public Vector3d sight = new Vector3d(0.5, 0.9, 0.5);
    /** driver's eye in sight view when there is no turret, hull-local */
    public Vector3d driverEye = new Vector3d(0, 2, 1);
    public double camDistance = 9.0, camHeight = 3.0;

    // models (under models/entity/), wheel stations
    public String hullModel, wheelModel, turretModel, gunModel;
    public double[] wheelZ = {};
    public double wheelX = 1.43, wheelY = 0.52, wheelRadius = 0.4;
    public String engineSound = "tank_engine";

    public final List<WeaponSpec> weapons = new ArrayList<>();
    public AirSpec air;

    VehicleSpec(String id, Category category) {
        this.id = id;
        this.category = category;
        this.hullModel = id + "_hull";
        this.wheelModel = id + "_wheel";
        this.turretModel = id + "_turret";
        this.gunModel = id + "_gun";
    }

    VehicleSpec box(double x1, double y1, double z1, double x2, double y2, double z2) {
        hitBoxes.add(new AxisAlignedBB(x1, y1, z1, x2, y2, z2));
        return this;
    }

    WeaponSpec weapon(String wid) {
        WeaponSpec w = new WeaponSpec(wid);
        weapons.add(w);
        return w;
    }

    public boolean isAircraft() {
        return category == Category.AIRCRAFT;
    }

    /** Number of ammo counters this vehicle uses (synced as separate values). */
    public int ammoSlots() {
        if (air != null) {
            return 1 + air.pylons.length;
        }
        int n = 0;
        for (WeaponSpec w : weapons) {
            n += w.ammo.length;
        }
        return n;
    }

    /** First ammo counter of a weapon. */
    public int ammoBase(int weapon) {
        int n = 0;
        for (int i = 0; i < weapon; i++) {
            n += weapons.get(i).ammo.length;
        }
        return n;
    }

    public String key() {
        return "entity.militarytech." + id;
    }
}
