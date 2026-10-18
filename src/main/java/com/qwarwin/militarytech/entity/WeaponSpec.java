package com.qwarwin.militarytech.entity;

import net.minecraft.util.math.vector.Vector3d;

/** One weapon on a vehicle. Built with chained setters in {@link Specs}. */
public final class WeaponSpec {
    public enum Mount {
        /** hull-local */
        HULL,
        /** relative to the turret pivot, turns with the turret */
        TURRET,
        /** relative to the trunnion, turns and elevates with the gun */
        GUN
    }

    public final String id;
    public Munition[] ammo;
    public int[] capacity;
    public boolean secondary;
    public boolean auto;
    public int interval = 10;
    public float spread = 0.3F;
    public Mount mount = Mount.GUN;
    public Vector3d[] muzzles = {Vector3d.ZERO};
    public String sound = "tank_gun";
    public float volume = 6F;
    /** RMB on a launcher: one at a time; LMB: the whole ripple */
    public boolean ripple;

    WeaponSpec(String id) {
        this.id = id;
    }

    public WeaponSpec ammo(Munition m, int cap) {
        return ammo(new Munition[]{m}, new int[]{cap});
    }

    public WeaponSpec ammo(Munition[] m, int[] cap) {
        this.ammo = m;
        this.capacity = cap;
        return this;
    }

    public WeaponSpec secondary() {
        this.secondary = true;
        return this;
    }

    public WeaponSpec auto(int interval) {
        this.auto = true;
        this.interval = interval;
        return this;
    }

    public WeaponSpec interval(int t) {
        this.interval = t;
        return this;
    }

    public WeaponSpec spread(float s) {
        this.spread = s;
        return this;
    }

    public WeaponSpec mount(Mount m, Vector3d... muzzles) {
        this.mount = m;
        this.muzzles = muzzles;
        return this;
    }

    public WeaponSpec sound(String s, float volume) {
        this.sound = s;
        this.volume = volume;
        return this;
    }

    public WeaponSpec ripple() {
        this.ripple = true;
        return this;
    }

    public String key() {
        return "weapon.militarytech." + id;
    }
}
