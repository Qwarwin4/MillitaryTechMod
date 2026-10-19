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
    /** what each pylon may carry, kg */
    public double[] pylonMax = {};
    /** wingtip rails: air-to-air missiles only */
    public boolean[] pylonAam = {};
    /** everything this aircraft can hang, in the order the loadout screen offers it */
    public Munition[] carries = {};
    /** take-off load the flight model is tuned around, kg: more makes it slower and less agile */
    public double maxLoad = 4000;
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

    // ------------------------------------------------------------------ choosing stores pylon by pylon

    /** How many of a munition one pylon takes: a pod of 20 rockets, racks of bombs in a bay, else one. */
    public int countFor(int pylon, Munition m) {
        if (m == Munition.S8KO) {
            return 20;
        }
        if (internalBay && m.kind == Munition.Kind.BOMB) {
            return (int) Math.max(1, Math.min(6, Math.floor(pylonMax[pylon] / m.massKg())));
        }
        return 1;
    }

    /** Weight of a store: the rocket pod is 160 kg empty. */
    public static double storeMass(Munition m, int count) {
        return m == Munition.S8KO ? 160 + 11.3 * count : m.massKg() * count;
    }

    /** Every store there is: any of them goes on any pylon, the weight only costs speed and agility. */
    public static final Munition[] ALL_STORES = {Munition.R73, Munition.R40, Munition.KH29, Munition.KH59, Munition.KH101,
            Munition.KH102, Munition.S8KO, Munition.FAB250, Munition.FAB500, Munition.FAB1500, Munition.FAB3000,
            Munition.FAB9000, Munition.RN28};

    public boolean allows(int pylon, Munition m) {
        if (pylon < 0 || pylon >= pylons.length) {
            return false;
        }
        for (Munition c : ALL_STORES) {
            if (c == m) {
                return true;
            }
        }
        return false;
    }

    public List<Munition> allowed(int pylon) {
        List<Munition> out = new ArrayList<>();
        for (Munition m : ALL_STORES) {
            if (allows(pylon, m)) {
                out.add(m);
            }
        }
        return out;
    }

    private final java.util.Map<Long, Store> stores = new java.util.HashMap<>();

    /** The store a pylon carries with this munition (cached, so renderers can call it every frame). */
    public Store store(int pylon, Munition m) {
        long key = (long) pylon << 32 | m.ordinal();
        return stores.computeIfAbsent(key, k -> new Store(pylon, m, countFor(pylon, m),
                m == Munition.S8KO ? "b8_pod" : m.model));
    }

    /** The pylon on the other wing, or -1. */
    public int mirror(int pylon) {
        Vector3d p = pylons[pylon];
        for (int i = 0; i < pylons.length; i++) {
            Vector3d q = pylons[i];
            if (i != pylon && Math.abs(q.x + p.x) < 0.05 && Math.abs(q.y - p.y) < 0.05 && Math.abs(q.z - p.z) < 0.05) {
                return i;
            }
        }
        return -1;
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
