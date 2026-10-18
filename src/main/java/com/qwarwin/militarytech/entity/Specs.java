package com.qwarwin.militarytech.entity;

import net.minecraft.util.math.vector.Vector3d;

import static com.qwarwin.militarytech.entity.Munition.*;

/**
 * The vehicles. Numbers are tuned for fun rather than realism: a 125 mm round takes a few seconds to load
 * instead of ten, an aircraft flies 70 blocks a second instead of 250 m.
 */
public final class Specs {
    private static final double[] T72_WHEELS = {2.05, 1.23, 0.41, -0.41, -1.23, -2.05};
    private static final double[] BMP_WHEELS = {1.95, 1.17, 0.39, -0.39, -1.17, -1.95};

    private static Vector3d v(double x, double y, double z) {
        return new Vector3d(x, y, z);
    }

    private static VehicleSpec mbt(String id) {
        VehicleSpec s = new VehicleSpec(id, VehicleSpec.Category.TANK);
        s.health = 320F;
        s.armor = Armor.HEAVY;
        s.width = 3.6F;
        s.height = 2.4F;
        s.box(-1.85, 0, -3.3, 1.85, 1.5, 3.05).box(-1.35, 1.45, -1.7, 1.35, 2.3, 1.5);
        s.seats = new Vector3d[]{v(0, 0.35, 1.9), v(0.45, 0.6, 0.0)};
        s.wheelZ = T72_WHEELS;
        s.pivot = v(0, 1.45, 0.1);
        s.trunnion = v(0, 0.42, 0.85);
        s.sight = v(0.55, 1.2, 0.45);
        s.driverEye = v(0, 1.9, 2.2);
        s.weapon("cannon_125")
                .ammo(new Munition[]{APFSDS, HE_125, REFLEKS}, new int[]{22, 18, 4})
                .interval(110).spread(0.1F)
                .mount(WeaponSpec.Mount.GUN, v(0, 0, 5.4))
                .sound("tank_gun", 12F);
        s.weapon("pkt")
                .ammo(PKT, 2000).secondary().auto(2).spread(0.6F)
                .mount(WeaponSpec.Mount.GUN, v(-0.3, 0.02, 0.6))
                .sound("mg", 3F);
        return s;
    }

    public static final VehicleSpec T72B3 = mbt("t72b3");

    public static final VehicleSpec T80U = mbt("t80u");

    public static final VehicleSpec T90M = mbt("t90m");

    public static final VehicleSpec BMPT = new VehicleSpec("bmpt", VehicleSpec.Category.TANK);

    public static final VehicleSpec BMP1 = new VehicleSpec("bmp1", VehicleSpec.Category.TANK);

    public static final VehicleSpec GRAD = new VehicleSpec("grad", VehicleSpec.Category.TRUCK);

    public static final VehicleSpec ORESHNIK = new VehicleSpec("oreshnik", VehicleSpec.Category.TRUCK);

    public static final VehicleSpec TOS1A = new VehicleSpec("tos1a", VehicleSpec.Category.TANK);

    public static final VehicleSpec ZAK = new VehicleSpec("zak", VehicleSpec.Category.TURRET);

    public static final VehicleSpec ZRK = new VehicleSpec("zrk", VehicleSpec.Category.TURRET);

    public static final VehicleSpec SU25 = new VehicleSpec("su25", VehicleSpec.Category.AIRCRAFT);

    public static final VehicleSpec SU34 = new VehicleSpec("su34", VehicleSpec.Category.AIRCRAFT);

    public static final VehicleSpec MIG25 = new VehicleSpec("mig25", VehicleSpec.Category.AIRCRAFT);

    static {
        // ---- T-80U: gas turbine, quick
        T80U.maxSpeed = 0.58;
        T80U.accel = 0.016;
        T80U.engineSound = "turbine";
        T80U.pivot = v(0, 1.45, 0.0);
        T80U.weapons.get(0).interval = 100;

        // ---- T-90M: Relikt, a bit more of everything
        T90M.health = 380F;
        T90M.maxSpeed = 0.47;
        T90M.trunnion = v(0, 0.45, 0.85);
        T90M.weapons.get(0).muzzles = new Vector3d[]{v(0, 0, 5.6)};
        T90M.weapons.get(0).interval = 95;
        T90M.box(-1.45, 1.45, -2.3, 1.45, 2.35, -1.0);

        // ---- BMPT "Terminator": two 30 mm and four Ataka
        VehicleSpec b = BMPT;
        b.health = 360F;
        b.armor = Armor.MEDIUM;
        b.width = 3.6F;
        b.height = 2.4F;
        b.box(-1.85, 0, -3.3, 1.85, 1.5, 3.05).box(-1.4, 1.45, -1.8, 1.4, 2.4, 1.4);
        b.seats = new Vector3d[]{v(0, 0.35, 1.9), v(0.45, 0.6, -0.2), v(-0.45, 0.6, -0.2)};
        b.wheelZ = T72_WHEELS;
        b.pivot = v(0, 1.45, -0.1);
        b.trunnion = v(0, 0.6, 0.55);
        b.gunModel = "bmpt_guns";
        b.sight = v(0.48, 1.15, 0.45);
        b.driverEye = v(0, 1.9, 2.2);
        b.minPitch = -45F;
        b.maxPitch = 5F;
        b.traverse = 4F;
        b.maxSpeed = 0.42;
        b.weapon("cannon_2a42")
                .ammo(SHELL_30, 850).auto(2).spread(0.5F)
                .mount(WeaponSpec.Mount.GUN, v(0.2, 0, 2.5), v(-0.2, 0, 2.5))
                .sound("cannon_30", 5F);
        b.weapon("ataka")
                .ammo(ATAKA, 4).secondary().interval(30).spread(0F)
                .mount(WeaponSpec.Mount.GUN, v(1.12, -0.15, 1.5), v(1.12, 0.19, 1.5), v(-1.12, -0.15, 1.5), v(-1.12, 0.19, 1.5))
                .sound("atgm_launch", 5F);

        // ---- BMP-1: 73 mm Grom, Malyutka, six in the back, floats
        b = BMP1;
        b.health = 170F;
        b.armor = Armor.LIGHT;
        b.width = 3.4F;
        b.height = 2.1F;
        b.box(-1.65, 0, -3.3, 1.65, 1.45, 3.2).box(-0.8, 1.4, -0.45, 0.8, 2.0, 1.15);
        b.seats = new Vector3d[]{v(0.6, 0.3, 1.8), v(0, 0.55, 0.35), v(0.5, 0.3, -0.9), v(-0.5, 0.3, -0.9),
                v(0.5, 0.3, -1.7), v(-0.5, 0.3, -1.7), v(0.5, 0.3, -2.5), v(-0.5, 0.3, -2.5)};
        b.wheelZ = BMP_WHEELS;
        b.wheelX = 1.34;
        b.wheelY = 0.49;
        b.wheelRadius = 0.37;
        b.pivot = v(0, 1.42, 0.35);
        b.trunnion = v(0, 0.28, 0.5);
        b.sight = v(0.35, 0.75, 0.0);
        b.driverEye = v(0.6, 1.7, 1.8);
        b.minPitch = -30F;
        b.maxPitch = 4F;
        b.traverse = 3.5F;
        b.maxSpeed = 0.5;
        b.amphibious = true;
        b.weapon("grom")
                .ammo(HEAT_73, 40).interval(45).spread(0.4F)
                .mount(WeaponSpec.Mount.GUN, v(0, 0, 2.2))
                .sound("grom", 8F);
        b.weapon("malyutka")
                .ammo(MALYUTKA, 4).secondary().interval(60).spread(0F)
                .mount(WeaponSpec.Mount.GUN, v(0, 0.38, 0.8))
                .sound("atgm_launch", 5F);

        // ---- BM-21 Grad on the Ural
        b = GRAD;
        b.health = 120F;
        b.armor = Armor.SOFT;
        b.width = 2.8F;
        b.height = 3.0F;
        b.box(-1.3, 0, -3.7, 1.3, 2.9, 3.7);
        b.seats = new Vector3d[]{v(0.5, 1.25, 2.45), v(-0.5, 1.25, 2.45)};
        b.separateGunner = false;
        b.tracked = false;
        b.maxSpeed = 0.6;
        b.reverseSpeed = 0.2;
        b.accel = 0.014;
        b.turnRate = 3.0;
        b.wheelModel = "ural_wheel";
        b.turretModel = "grad_mount";
        b.gunModel = "grad_rack";
        b.wheelZ = new double[]{2.35, -1.0, -2.4};
        b.wheelX = 1.05;
        b.wheelY = 0.56;
        b.wheelRadius = 0.56;
        b.pivot = v(0, 1.75, -1.6);
        b.trunnion = v(0, 0.55, -0.2);
        b.minPitch = -55F;
        b.maxPitch = 0F;
        b.yawLimit = 110F;
        b.traverse = 2F;
        b.sight = v(0.9, 1.0, 0.6);
        b.driverEye = v(0.5, 2.9, 2.45);
        b.camDistance = 11;
        b.engineSound = "truck_engine";
        Vector3d[] tubes = new Vector3d[40];
        for (int i = 0; i < 40; i++) {
            int col = i % 10, row = i / 10;
            // fire from alternating sides and rows, like the real ripple
            int c = (col % 2 == 0) ? col / 2 : 9 - col / 2;
            tubes[i] = v(-0.72 + c * 0.16, 0.1 + row * 0.16, 2.9);
        }
        b.weapon("grad").ammo(Munition.GRAD, 40).interval(3).spread(0.8F).ripple()
                .mount(WeaponSpec.Mount.GUN, tubes)
                .sound("grad_launch", 10F);

        // ---- Oreshnik on its MZKT chassis
        b = ORESHNIK;
        b.health = 160F;
        b.armor = Armor.SOFT;
        b.width = 3.2F;
        b.height = 3.6F;
        b.box(-1.5, 0, -6.6, 1.5, 3.4, 6.6);
        b.seats = new Vector3d[]{v(0.55, 1.5, 5.4), v(-0.55, 1.5, 5.4)};
        b.separateGunner = false;
        b.tracked = false;
        b.hasTurret = false;
        b.maxSpeed = 0.42;
        b.accel = 0.008;
        b.turnRate = 1.8;
        b.wheelModel = "mzkt_wheel";
        b.turretModel = null;
        b.gunModel = "oreshnik_canister";
        b.wheelZ = new double[]{4.4, 3.0, 1.0, -0.3, -1.6, -2.9, -4.2, -5.5};
        b.wheelX = 1.15;
        b.wheelY = 0.62;
        b.wheelRadius = 0.62;
        b.pivot = v(0, 2.55, -6.0);
        b.driverEye = v(0.55, 3.3, 5.4);
        b.camDistance = 16;
        b.camHeight = 5;
        b.engineSound = "truck_engine";
        b.weapon("oreshnik").ammo(Munition.ORESHNIK, 1).interval(100).spread(0F)
                .mount(WeaponSpec.Mount.HULL, v(0, 13.1, -6.9)).sound("oreshnik_launch", 30F);

        // ---- TOS-1A "Solntsepyok": T-72 chassis, 24 thermobaric rockets
        b = TOS1A;
        b.health = 300F;
        b.armor = Armor.HEAVY;
        b.width = 3.6F;
        b.height = 2.6F;
        b.box(-1.85, 0, -3.3, 1.85, 1.5, 3.05).box(-1.2, 1.45, -2.2, 1.2, 2.9, 2.0);
        b.seats = new Vector3d[]{v(0, 0.35, 1.9), v(0.45, 0.6, -1.5)};
        b.wheelZ = T72_WHEELS;
        b.turretModel = "tos1a_mount";
        b.gunModel = "tos1a_rack";
        b.pivot = v(0, 1.45, -0.6);
        b.trunnion = v(0, 0.55, -1.25);
        b.sight = v(1.0, 1.0, 0.2);
        b.driverEye = v(0, 1.9, 2.2);
        b.minPitch = -55F;
        b.maxPitch = 0F;
        b.yawLimit = 100F;
        b.traverse = 2F;
        b.maxSpeed = 0.42;
        b.camDistance = 11;
        Vector3d[] tos = new Vector3d[24];
        for (int i = 0; i < 24; i++) {
            int col = i % 6, row = i / 6;
            int c = (col % 2 == 0) ? col / 2 : 5 - col / 2;
            tos[i] = v(-0.75 + c * 0.3, 0.16 + row * 0.27, 3.75);
        }
        b.weapon("tos").ammo(Munition.TOS_220, 24).interval(8).spread(1.2F).ripple()
                .mount(WeaponSpec.Mount.GUN, tos)
                .sound("grad_launch", 12F);

        // ---- automatic air defence: twin 30 mm gun turret and a missile turret
        for (VehicleSpec t : new VehicleSpec[]{ZAK, ZRK}) {
            t.armor = Armor.LIGHT;
            t.width = 2.2F;
            t.height = 2.8F;
            t.box(-1.1, 0, -1.1, 1.1, 2.8, 1.1);
            t.seats = new Vector3d[0];
            t.separateGunner = false;
            t.tracked = false;
            t.maxSpeed = 0;
            t.stepHeight = 0F;
            t.wheelModel = null;
            t.hullModel = t.id + "_base";
            t.pivot = v(0, 1.2, 0);
        }
        ZAK.health = 140F;
        ZAK.trunnion = v(0, 0.55, 0.05);
        ZAK.minPitch = -85F;
        ZAK.maxPitch = 5F;
        ZAK.traverse = 9F;
        ZAK.gunModel = "zak_guns";
        ZAK.weapon("zak_30").ammo(Munition.AA_30, 800).auto(2).spread(0.7F)
                .mount(WeaponSpec.Mount.GUN, v(0.62, 0, 2.35), v(-0.62, 0, 2.35))
                .sound("cannon_30", 6F);
        ZRK.health = 120F;
        ZRK.trunnion = v(0, 0.65, 0);
        ZRK.minPitch = -80F;
        ZRK.maxPitch = 0F;
        ZRK.traverse = 6F;
        ZRK.gunModel = "zrk_launcher";
        ZRK.weapon("zrk").ammo(Munition.SAM, 8).interval(40).spread(0F)
                .mount(WeaponSpec.Mount.GUN, v(0.8, 0, 1.35), v(-0.8, 0, 1.35), v(0.8, 0.32, 1.35), v(-0.8, 0.32, 1.35),
                        v(1.12, 0, 1.35), v(-1.12, 0, 1.35), v(1.12, 0.32, 1.35), v(-1.12, 0.32, 1.35))
                .sound("missile_launch", 6F);

        // ---- aircraft
        b = SU25;
        b.health = 190F;
        b.armor = Armor.AIR;
        b.width = 3.6F;
        b.height = 2.8F;
        b.box(-0.8, 0.6, -5.8, 0.8, 2.7, 5.4).box(-5.2, 1.3, -0.6, 5.2, 1.9, 1.4);
        b.seats = new Vector3d[]{v(0, 1.35, 3.4)};
        b.separateGunner = false;
        b.hasTurret = false;
        b.tracked = false;
        b.enclosed = false;
        b.engineSound = "jet_engine";
        b.wheelModel = null;
        b.turretModel = null;
        b.gunModel = "su25_gear";
        AirSpec a = new AirSpec();
        a.maxSpeed = 3.4;
        a.stallSpeed = 1.05;
        a.takeoffSpeed = 1.35;
        a.pitchRate = 3.4F;
        a.yawRate = 2.8F;
        a.cockpit = v(0, 2.45, 3.4);
        a.camDistance = 20;
        a.exhaust = v(0.95, 1.4, -3.0);
        a.exhaustSpread = 0.95;
        a.pylons = new Vector3d[]{v(1.7, 1.78, 0.55), v(2.4, 1.78, 0.5), v(3.1, 1.78, 0.45), v(3.8, 1.78, 0.4), v(4.5, 1.78, 0.35),
                v(-1.7, 1.78, 0.55), v(-2.4, 1.78, 0.5), v(-3.1, 1.78, 0.45), v(-3.8, 1.78, 0.4), v(-4.5, 1.78, 0.35)};
        a.loadouts.add(AirSpec.loadout("cas").put(R73, 1, 4, 9).put(S8KO, 20, 2, 7, 3, 8).put(FAB250, 1, 0, 5, 1, 6).build());
        a.loadouts.add(AirSpec.loadout("rockets").put(R73, 1, 4, 9).put(S8KO, 21, 0, 5, 1, 6, 2, 7, 3, 8).build());
        a.loadouts.add(AirSpec.loadout("bombs").put(R73, 1, 4, 9).put(FAB500, 1, 0, 5, 1, 6, 2, 7, 3, 8).build());
        a.loadouts.add(AirSpec.loadout("antitank").put(R73, 1, 4, 9).put(KH29, 1, 0, 5, 1, 6).put(S8KO, 20, 2, 7, 3, 8).build());
        b.air = a;
        b.weapon("gsh30").ammo(GSH_30, 250).auto(1).spread(0.5F)
                .mount(WeaponSpec.Mount.HULL, v(0.32, 1.13, 4.9)).sound("aircraft_gun", 6F);

        b = SU34;
        b.health = 170F;
        b.armor = Armor.AIR;
        b.width = 3.8F;
        b.height = 3.0F;
        b.box(-1.0, 0.7, -7.2, 1.0, 2.9, 6.6).box(-4.4, 1.4, -3.0, 4.4, 2.0, 1.5);
        b.seats = new Vector3d[]{v(0.35, 1.55, 4.4), v(-0.35, 1.55, 4.4)};
        b.separateGunner = false;
        b.hasTurret = false;
        b.tracked = false;
        b.enclosed = false;
        b.engineSound = "jet_engine";
        b.wheelModel = null;
        b.turretModel = null;
        b.gunModel = "su34_gear";
        a = new AirSpec();
        a.maxSpeed = 3.9;
        a.stallSpeed = 1.25;
        a.takeoffSpeed = 1.55;
        a.pitchRate = 2.8F;
        a.yawRate = 2.4F;
        a.cockpit = v(0.35, 2.75, 4.4);
        a.camDistance = 23;
        a.exhaust = v(0.62, 1.75, -6.9);
        a.exhaustSpread = 0.62;
        a.pylons = new Vector3d[]{v(1.6, 1.85, -0.4), v(2.3, 1.85, -0.9), v(3.0, 1.85, -1.4), v(3.7, 1.85, -1.9),
                v(-1.6, 1.85, -0.4), v(-2.3, 1.85, -0.9), v(-3.0, 1.85, -1.4), v(-3.7, 1.85, -1.9),
                v(0.7, 1.1, 1.2), v(-0.7, 1.1, 1.2), v(0.7, 1.1, -2.4), v(-0.7, 1.1, -2.4)};
        a.loadouts.add(AirSpec.loadout("strike").put(R73, 1, 3, 7).put(KH59, 1, 0, 4).put(KH29, 1, 1, 5, 2, 6)
                .put(FAB500, 1, 8, 9).build());
        a.loadouts.add(AirSpec.loadout("heavy").put(R73, 1, 3, 7).put(FAB500, 1, 1, 5, 2, 6).put(FAB1500, 1, 0, 4)
                .put(FAB250, 1, 8, 9, 10, 11).build());
        a.loadouts.add(AirSpec.loadout("rockets168").put(S8KO, 21, 0, 4, 1, 5, 2, 6, 3, 7, 8, 9, 10, 11).build());
        b.air = a;
        b.weapon("gsh301").ammo(GSH_30, 180).auto(1).spread(0.4F)
                .mount(WeaponSpec.Mount.HULL, v(-1.05, 1.75, 2.2)).sound("aircraft_gun", 6F);

        b = MIG25;
        b.health = 120F;
        b.armor = Armor.AIR;
        b.width = 3.8F;
        b.height = 3.0F;
        b.box(-1.3, 0.7, -6.8, 1.3, 2.8, 6.0).box(-4.6, 1.7, -3.4, 4.6, 2.2, 0.6);
        b.seats = new Vector3d[]{v(0, 1.4, 3.8)};
        b.separateGunner = false;
        b.hasTurret = false;
        b.tracked = false;
        b.enclosed = false;
        b.engineSound = "jet_engine";
        b.wheelModel = null;
        b.turretModel = null;
        b.gunModel = "mig25_gear";
        a = new AirSpec();
        a.maxSpeed = 4.7;
        a.stallSpeed = 1.55;
        a.takeoffSpeed = 1.9;
        a.pitchRate = 2.2F;
        a.yawRate = 1.9F;
        a.cockpit = v(0, 2.5, 3.8);
        a.camDistance = 22;
        a.exhaust = v(0.55, 1.7, -7.0);
        a.exhaustSpread = 0.55;
        a.pylons = new Vector3d[]{v(2.2, 2.18, -1.4), v(3.4, 2.18, -1.9), v(-2.2, 2.18, -1.4), v(-3.4, 2.18, -1.9),
                v(0.7, 1.1, -0.6), v(-0.7, 1.1, -0.6), v(0.7, 1.1, -2.6), v(-0.7, 1.1, -2.6)};
        a.loadouts.add(AirSpec.loadout("intercept").put(R40, 1, 0, 2, 1, 3).build());
        a.loadouts.add(AirSpec.loadout("dogfight").put(R40, 1, 0, 2).put(R73, 1, 1, 3).build());
        a.loadouts.add(AirSpec.loadout("recon_bomber").put(R73, 1, 1, 3).put(FAB500, 1, 0, 2, 4, 5, 6, 7).build());
        b.air = a;
    }

    public static final VehicleSpec[] ALL = {T72B3, T80U, T90M, BMPT, BMP1, TOS1A, GRAD, ORESHNIK, SU25, SU34, MIG25, ZAK, ZRK};

    private Specs() {
    }
}
