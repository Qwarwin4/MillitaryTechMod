package com.qwarwin.militarytech.entity;

import net.minecraft.util.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;

/** Flight numbers, pylons and loadouts of an aircraft. */
public final class AirSpec {
    /** blocks/tick */
    public double maxSpeed = 3.4, stallSpeed = 1.1, takeoffSpeed = 1.4;
    /** degrees/tick at full control authority */
    public float pitchRate = 3.2F, yawRate = 2.6F;
    public Vector3d cockpit = new Vector3d(0, 2, 3);
    /** where the gear touches, used for the ground clearance of the model */
    public double gearHeight = 1.2;
    public Vector3d[] pylons = {};
    /** downward offset of whatever hangs on a pylon */
    public final List<Loadout> loadouts = new ArrayList<>();
    public double camDistance = 20, camHeight = 4;
    public Vector3d exhaust = new Vector3d(0, 1.5, -6);
    public double exhaustSpread = 0.0;
    /** stores ride inside the fuselage (bomb bays): not drawn, the bay doors open to release */
    public boolean internalBay;
    /** the HUD combiner glass: centre in aircraft coordinates (null: just ahead of the cockpit eye) and half size */
    public Vector3d hudGlass;
    public double hudHalfWidth = 0.16, hudHalfHeight = 0.14;

    public static final class Store {
        public final int pylon;
        public final Munition munition;
        public final int count;
        /** model hung on the pylon: a pod for rockets, otherwise the munition itself */
        public final String model;

        Store(int pylon, Munition munition, int count, String model) {
            this.pylon = pylon;
            this.munition = munition;
            this.count = count;
            this.model = model;
        }
    }

    public static final class Loadout {
        public final String id;
        public final Store[] stores;

        Loadout(String id, Store[] stores) {
            this.id = id;
            this.stores = stores;
        }

        public String key() {
            return "loadout.militarytech." + id;
        }

        /** Store on a pylon, or null if it's empty in this loadout. */
        public Store at(int pylon) {
            for (Store s : stores) {
                if (s.pylon == pylon) {
                    return s;
                }
            }
            return null;
        }
    }

    /** Symmetric pairs are listed left first: a(0..n) for pylons on both wings. */
    static LoadoutBuilder loadout(String id) {
        return new LoadoutBuilder(id);
    }

    static final class LoadoutBuilder {
        private final String id;
        private final List<Store> stores = new ArrayList<>();

        LoadoutBuilder(String id) {
            this.id = id;
        }

        LoadoutBuilder put(Munition m, int count, int... pylons) {
            String model = m == Munition.S8KO ? "b8_pod" : m.model;
            for (int p : pylons) {
                stores.add(new Store(p, m, count, model));
            }
            return this;
        }

        Loadout build() {
            return new Loadout(id, stores.toArray(new Store[0]));
        }
    }
}
