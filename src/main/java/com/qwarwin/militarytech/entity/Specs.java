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
        // driver, gunner, commander inside; four more ride on the engine deck
        s.seats = new Vector3d[]{v(0, 0.35, 1.9), v(0.45, 0.6, 0.0), v(-0.45, 0.6, 0.0),
                v(0.7, 1.35, -1.9), v(-0.7, 1.35, -1.9), v(0.7, 1.35, -2.7), v(-0.7, 1.35, -2.7)};
        s.insideSeats = 3;
        s.interior = true;
        s.wheelZ = T72_WHEELS;
        s.wheelModel = null;
        s.pivot = v(0, 1.45, 0.1);
        s.trunnion = v(0, 0.42, 0.85);
        s.sight = v(0.55, 1.2, 0.45);
        s.driverEye = v(0, 1.9, 2.2);
        // driver's prism blocks, commander's cupola beside the gunner's sight
        s.view(0, v(0, 1.9, 2.2), false).view(2, v(-0.55, 1.2, 0.2), true);
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

    public static final VehicleSpec TU160 = new VehicleSpec("tu160", VehicleSpec.Category.AIRCRAFT);

    public static final VehicleSpec MIG23 = new VehicleSpec("mig23", VehicleSpec.Category.AIRCRAFT);

    /** Replace the hit boxes (the real models came with their own proportions). */
    private static void boxes(VehicleSpec s, double[]... b) {
        s.hitBoxes.clear();
        for (double[] x : b) {
            s.box(x[0], x[1], x[2], x[3], x[4], x[5]);
        }
    }

    private static double[] b(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new double[]{x1, y1, z1, x2, y2, z2};
    }

    static {
        // ---- T-72B3, with the anti-drone screen over the turret
        T72B3.pivot = v(0, 1.36, -0.06);
        T72B3.trunnion = v(0, 0.27, 0.9);
        T72B3.weapons.get(0).muzzles = new Vector3d[]{v(0, 0, 4.9)};
        boxes(T72B3, b(-1.85, 0, -3.45, 1.85, 1.45, 3.2), b(-1.3, 1.4, -1.3, 1.3, 2.15, 1.1));

        // ---- T-80U: gas turbine, quick
        T80U.maxSpeed = 0.58;
        T80U.accel = 0.016;
        T80U.engineSound = "turbine";
        T80U.pivot = v(0, 1.36, -0.05);
        T80U.trunnion = v(0, 0.25, 1.0);
        T80U.weapons.get(0).muzzles = new Vector3d[]{v(0, 0, 5.2)};
        boxes(T80U, b(-1.85, 0, -3.55, 1.85, 1.4, 3.55), b(-1.45, 1.36, -1.5, 1.45, 2.2, 1.3));
        T80U.weapons.get(0).interval = 100;

        // ---- T-90M: Relikt, a bit more of everything
        T90M.health = 380F;
        T90M.maxSpeed = 0.47;
        T90M.pivot = v(0, 1.25, -0.33);
        T90M.trunnion = v(0, 0.25, 0.95);
        T90M.weapons.get(0).muzzles = new Vector3d[]{v(0, 0, 4.6)};
        T90M.weapons.get(0).interval = 95;
        boxes(T90M, b(-1.75, 0, -3.8, 1.75, 1.3, 3.0), b(-1.6, 1.25, -2.4, 1.6, 2.1, 1.3));
        for (VehicleSpec t : new VehicleSpec[]{T80U, T90M}) {
            t.seats = new Vector3d[]{v(0, 0.35, 1.9), v(0.45, 0.6, -0.3), v(-0.45, 0.6, -0.3),
                    v(0.7, 1.35, -2.1), v(-0.7, 1.35, -2.1), v(0.7, 1.35, -2.9), v(-0.7, 1.35, -2.9)};
        }

        // ---- BMPT "Terminator": two 30 mm and four Ataka
        VehicleSpec b = BMPT;
        b.health = 360F;
        b.armor = Armor.MEDIUM;
        b.width = 3.6F;
        b.height = 2.4F;
        b.box(-1.75, 0, -3.6, 1.75, 1.8, 3.55).box(-1.1, 1.8, -1.9, 1.1, 2.9, 1.4);
        // five crew, four on the deck
        b.seats = new Vector3d[]{v(0, 0.35, 1.9), v(0.45, 0.6, -0.2), v(-0.45, 0.6, -0.2), v(0.7, 0.35, 1.9), v(-0.7, 0.35, 1.9),
                v(0.7, 1.55, -2.2), v(-0.7, 1.55, -2.2), v(0.7, 1.55, -3.0), v(-0.7, 1.55, -3.0)};
        b.insideSeats = 5;
        b.interior = true;
        b.wheelZ = T72_WHEELS;
        b.wheelModel = null;
        b.pivot = v(0, 1.8, -0.4);
        b.trunnion = v(0, 0.62, 0.0);
        b.sight = v(0.48, 1.15, 0.45);
        b.driverEye = v(0, 1.9, 2.2);
        b.view(0, v(0, 1.9, 2.2), false).view(2, v(-0.48, 1.15, 0.45), true)
                .view(3, v(0.7, 1.95, 2.05), false).view(4, v(-0.7, 1.95, 2.05), false);
        b.minPitch = -45F;
        b.maxPitch = 5F;
        b.traverse = 4F;
        b.maxSpeed = 0.42;
        b.weapon("cannon_2a42")
                .ammo(SHELL_30, 850).auto(2).spread(0.5F)
                .mount(WeaponSpec.Mount.GUN, v(0.11, 0, 1.8), v(-0.11, 0, 1.8))
                .sound("cannon_30", 5F);
        b.weapon("ataka")
                .ammo(ATAKA, 4).secondary().interval(30).spread(0F)
                .mount(WeaponSpec.Mount.GUN, v(0.95, 0.17, 0.6), v(0.78, 0.21, 0.6), v(-0.95, 0.17, 0.6), v(-0.78, 0.21, 0.6))
                .sound("atgm_launch", 5F);

        // ---- BMP-1: 73 mm Grom, Malyutka, six in the back, floats
        b = BMP1;
        b.health = 170F;
        b.armor = Armor.LIGHT;
        b.width = 3.4F;
        b.height = 2.1F;
        b.box(-1.5, 0, -3.3, 1.5, 1.55, 3.0).box(-0.8, 1.5, -1.45, 0.8, 2.1, 0.2);
        // driver, gunner, commander, eight troops in the back; six more ride on the roof
        b.seats = new Vector3d[]{v(0.5, 0.3, 1.9), v(0, 0.55, -0.6), v(0.5, 0.3, 1.2),
                v(0.45, 0.3, -1.3), v(-0.45, 0.3, -1.3), v(0.45, 0.3, -1.9), v(-0.45, 0.3, -1.9),
                v(0.45, 0.3, -2.5), v(-0.45, 0.3, -2.5), v(0.45, 0.3, -3.0), v(-0.45, 0.3, -3.0),
                v(0.65, 1.55, -1.7), v(-0.65, 1.55, -1.7), v(0.65, 1.55, -2.4), v(-0.65, 1.55, -2.4),
                v(0.65, 1.55, -3.05), v(-0.65, 1.55, -3.05)};
        b.insideSeats = 11;
        b.interior = true;
        b.wheelZ = BMP_WHEELS;
        b.wheelModel = null;
        b.pivot = v(0, 1.53, -0.63);
        b.trunnion = v(0, 0.2, 0.7);
        b.sight = v(0.35, 0.75, 0.0);
        b.driverEye = v(0.6, 1.7, 1.8);
        // driver's and commander's periscopes; the troops look out of their firing ports, the back pair through the doors
        b.view(0, v(0.6, 1.7, 1.8), false).view(2, v(0.5, 1.75, 1.2), false);
        for (int i = 3; i <= 8; i++) {
            Vector3d st = b.seats[i];
            b.view(i, v(Math.signum(st.x) * 1.56, 1.05, st.z), false);
        }
        b.view(9, v(0.45, 1.0, -3.36), false).view(10, v(-0.45, 1.0, -3.36), false);
        // the two back doors, hinged on the outer edges; the troops get in through them
        b.doors.add(new VehicleSpec.Door("bmp1_door0", v(0.95, 0.95, -3.14), 'y', -105F,
                new net.minecraft.util.math.AxisAlignedBB(-0.25, 0.16, -3.6, 1.2, 1.73, -2.74), 3, 5, 7, 9));
        b.doors.add(new VehicleSpec.Door("bmp1_door1", v(-0.95, 0.95, -3.14), 'y', 105F,
                new net.minecraft.util.math.AxisAlignedBB(-1.2, 0.16, -3.6, 0.25, 1.73, -2.74), 4, 6, 8, 10));
        b.minPitch = -30F;
        b.maxPitch = 4F;
        b.traverse = 3.5F;
        b.maxSpeed = 0.5;
        b.amphibious = true;
        b.weapon("grom")
                .ammo(HEAT_73, 40).interval(45).spread(0.4F)
                .mount(WeaponSpec.Mount.GUN, v(0, 0, 1.7))
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
        b.box(-1.3, 0, -3.65, 1.3, 2.8, 3.75);
        b.seats = new Vector3d[]{v(0.5, 1.3, 2.5), v(-0.5, 1.3, 2.5), v(0, 1.3, 2.5)};
        b.interior = true;
        b.separateGunner = false;
        b.tracked = false;
        b.maxSpeed = 0.6;
        b.reverseSpeed = 0.2;
        b.accel = 0.014;
        b.turnRate = 3.0;
        b.wheelModel = null;
        b.wheelZ = new double[]{2.35, -1.0, -2.4};
        b.wheelX = 1.05;
        b.wheelY = 0.56;
        b.wheelRadius = 0.56;
        b.pivot = v(0, 1.6, -2.2);
        b.trunnion = v(0, 0.45, -0.3);
        b.minPitch = -55F;
        b.maxPitch = 0F;
        b.yawLimit = 110F;
        b.traverse = 2F;
        b.sight = v(0.9, 1.0, 0.6);
        b.driverEye = v(0.5, 2.15, 2.6);
        b.view(1, v(-0.5, 2.15, 2.6), false).view(2, v(0, 2.15, 2.6), false);
        b.camDistance = 11;
        b.engineSound = "truck_engine";
        Vector3d[] tubes = new Vector3d[40];
        for (int i = 0; i < 40; i++) {
            int col = i % 10, row = i / 10;
            // fire from alternating sides and rows, like the real ripple
            int c = (col % 2 == 0) ? col / 2 : 9 - col / 2;
            tubes[i] = v(-0.63 + c * 0.152, 0.275 + row * 0.128, 2.2);
        }
        b.weapon("grad").ammo(Munition.GRAD, 40).interval(3).spread(0.8F).ripple()
                .mount(WeaponSpec.Mount.GUN, tubes)
                .sound("grad_launch", 10F);

        // ---- Oreshnik on its MZKT chassis
        b = ORESHNIK;
        b.health = 160F;
        b.armor = Armor.SOFT;
        b.width = 5.36F;
        b.height = 6.03F;
        b.box(-2.094, 0, -11.223, 2.094, 4.523, 11.223);
        b.seats = new Vector3d[]{v(1.223, 1.34, 8.375), v(-1.223, 1.34, 8.375)};
        b.interior = true;
        b.separateGunner = false;
        b.tracked = false;
        b.hasTurret = false;
        b.maxSpeed = 0.42;
        b.accel = 0.008;
        b.turnRate = 1.8;
        b.wheelModel = null;
        b.turretModel = null;
        b.wheelZ = new double[]{4.4, 3.0, 1.0, -0.3, -1.6, -2.9, -4.2, -5.5};
        b.wheelX = 1.93;
        b.wheelY = 1.04;
        b.wheelRadius = 1.04;
        b.pivot = v(0, 2.06, -11.189);
        b.driverEye = v(1.223, 2.429, 8.877);
        b.view(1, v(-1.223, 2.429, 8.877), false);
        b.camDistance = 26.8;
        b.camHeight = 8.38;
        b.engineSound = "truck_engine";
        b.weapon("oreshnik").ammo(new Munition[]{Munition.ORESHNIK, Munition.ORESHNIK_NUKE}, new int[]{1, 1}).interval(100).spread(0F)
                .mount(WeaponSpec.Mount.HULL, v(0, 24.5, -11.2)).sound("oreshnik_launch", 30F);

        // ---- TOS-1A "Solntsepyok": T-72 chassis, 24 thermobaric rockets
        b = TOS1A;
        b.health = 300F;
        b.armor = Armor.HEAVY;
        b.width = 4.09F;
        b.height = 2.96F;
        b.box(-1.933, 0, -4.206, 1.933, 1.592, 3.467).box(-1.364, 1.592, -3.126, 1.251, 3.354, 1.819);
        b.seats = new Vector3d[]{v(0, 0.398, 2.16), v(0.512, 0.398, 1.364), v(-0.512, 0.398, 1.364)};
        b.interior = true;
        b.wheelZ = T72_WHEELS;
        b.wheelModel = null;
        b.pivot = v(0, 1.489, -1.205);
        b.trunnion = v(-0.068, 0.216, -1.523);
        b.sight = v(1.137, 1.137, 0.227);
        b.driverEye = v(0, 2.16, 2.501);
        b.view(2, v(-0.512, 2.05, 1.55), false);
        b.minPitch = -55F;
        b.maxPitch = 0F;
        b.yawLimit = 100F;
        b.traverse = 2F;
        b.maxSpeed = 0.42;
        b.camDistance = 12.51;
        // the TOS-1 pack: three rows of eight and six on top, thirty tubes
        Vector3d[] tos = new Vector3d[30];
        for (int i = 0; i < 30; i++) {
            int row = i < 24 ? i / 8 : 3;
            int col = i < 24 ? i % 8 : 1 + (i - 24);
            int n = row < 3 ? 8 : 6;
            int k = row < 3 ? col : col - 1;
            int c = (k % 2 == 0) ? k / 2 : n - 1 - k / 2;
            if (row == 3) {
                c += 1;
            }
            tos[i] = v(-1.08 + c * 0.299, 0.603 + row * 0.29, 4.547);
        }
        b.weapon("tos").ammo(Munition.TOS_220, 30).interval(8).spread(1.2F).ripple()
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
        b.width = 4.75F;
        b.height = 3.69F;
        b.box(-1.121, 0.791, -7.783, 1.121, 3.034, 6.991).box(-6.793, 1.319, -2.638, 6.793, 2.242, 0.264);
        b.seats = new Vector3d[]{v(0, 1.61, 2.5)};
        b.riderScale = 0.73F;
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
        a.cockpit = v(0, 2.79, 2.55);
        a.camDistance = 26.38;
        a.exhaust = v(1.121, 1.649, -3.957);
        a.exhaustSpread = 1.12;
        a.pylons = new Vector3d[]{v(2.242, 1.781, -1.121), v(3.166, 1.517, -1.187), v(4.089, 1.425, -1.253), v(5.013, 1.345, -1.319), v(5.804, 1.319, -1.385),
                v(-2.242, 1.781, -1.121), v(-3.166, 1.517, -1.187), v(-4.089, 1.425, -1.253), v(-5.013, 1.345, -1.319), v(-5.804, 1.319, -1.385)};
        a.loadouts.add(AirSpec.loadout("cas").put(R73, 1, 4, 9).put(S8KO, 20, 2, 7, 3, 8).put(FAB250, 1, 0, 5, 1, 6).build());
        a.loadouts.add(AirSpec.loadout("rockets").put(R73, 1, 4, 9).put(S8KO, 21, 0, 5, 1, 6, 2, 7, 3, 8).build());
        a.loadouts.add(AirSpec.loadout("bombs").put(R73, 1, 4, 9).put(FAB500, 1, 0, 5, 1, 6, 2, 7, 3, 8).build());
        a.loadouts.add(AirSpec.loadout("antitank").put(R73, 1, 4, 9).put(KH29, 1, 0, 5, 1, 6).put(S8KO, 20, 2, 7, 3, 8).build());
        b.air = a;
        b.weapon("gsh30").ammo(GSH_30, 250).auto(1).spread(0.5F)
                .mount(WeaponSpec.Mount.HULL, v(0.396, 1.253, 4.749)).sound("aircraft_gun", 6F);

        b = SU34;
        b.health = 170F;
        b.armor = Armor.AIR;
        b.width = 6.08F;
        b.height = 4.8F;
        b.box(-2.08, 0.96, -10.96, 2.08, 3.84, 11.12).box(-6.88, 1.92, -6.08, 6.88, 2.72, 0.8);
        b.seats = new Vector3d[]{v(0.38, 2.39, 4.5), v(-0.38, 2.39, 4.5)};
        b.riderScale = 0.68F;
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
        a.cockpit = v(0.38, 3.49, 4.5);
        a.camDistance = 36.8;
        a.exhaust = v(1.04, 1.92, -10.08);
        a.exhaustSpread = 1.04;
        a.pylons = new Vector3d[]{v(2.72, 2.24, -2.08), v(3.84, 2.32, -4.0), v(4.96, 2.32, -4.64), v(5.92, 2.32, -5.12),
                v(-2.72, 2.24, -2.08), v(-3.84, 2.32, -4.0), v(-4.96, 2.32, -4.64), v(-5.92, 2.32, -5.12),
                v(1.12, 2.08, 1.92), v(-1.12, 2.08, 1.92), v(1.12, 1.28, -3.84), v(-1.12, 1.28, -3.84)};
        a.loadouts.add(AirSpec.loadout("strike").put(R73, 1, 3, 7).put(KH59, 1, 0, 4).put(KH29, 1, 1, 5, 2, 6)
                .put(FAB500, 1, 8, 9).build());
        a.loadouts.add(AirSpec.loadout("heavy").put(R73, 1, 3, 7).put(FAB500, 1, 1, 5, 2, 6).put(FAB1500, 1, 0, 4)
                .put(FAB250, 1, 8, 9, 10, 11).build());
        a.loadouts.add(AirSpec.loadout("rockets168").put(S8KO, 21, 0, 4, 1, 5, 2, 6, 3, 7, 8, 9, 10, 11).build());
        b.air = a;
        b.weapon("gsh301").ammo(GSH_30, 180).auto(1).spread(0.4F)
                .mount(WeaponSpec.Mount.HULL, v(-1.6, 2.32, 2.4)).sound("aircraft_gun", 6F);

        // MiG-23UB: swing-wing, two seats in tandem, GSh-23 under the belly
        b = MIG23;
        b.gunModel = "mig23_gear";
        b.health = 130F;
        b.armor = Armor.AIR;
        b.width = 5.22F;
        b.height = 4.06F;
        b.box(-1.451, 0.435, -7.982, 1.451, 2.902, 7.837).box(-3.555, 1.306, -5.805, 3.555, 2.322, -0.726);
        b.seats = new Vector3d[]{v(0, 1.61, 2.8), v(0, 1.66, 1.5)};
        b.riderScale = 0.6F;
        b.separateGunner = false;
        b.hasTurret = false;
        b.tracked = false;
        b.enclosed = false;
        b.engineSound = "jet_engine";
        b.wheelModel = null;
        b.turretModel = null;
        a = new AirSpec();
        a.maxSpeed = 4.3;
        a.stallSpeed = 1.4;
        a.takeoffSpeed = 1.75;
        a.pitchRate = 2.8F;
        a.yawRate = 2.3F;
        a.cockpit = v(0, 2.58, 2.85);
        a.camDistance = 29.02;
        a.exhaust = v(0, 1.669, -7.982);
        a.exhaustSpread = 0.0;
        a.pylons = new Vector3d[]{v(1.959, 1.451, -1.741), v(1.088, 1.379, 0.435), v(-1.959, 1.451, -1.741), v(-1.088, 1.379, 0.435)};
        a.loadouts.add(AirSpec.loadout("intercept").put(R40, 1, 0, 2).put(R73, 1, 1, 3).build());
        a.loadouts.add(AirSpec.loadout("dogfight").put(R73, 1, 0, 1, 2, 3).build());
        a.loadouts.add(AirSpec.loadout("mig_bombs").put(FAB500, 1, 0, 1, 2, 3).build());
        a.loadouts.add(AirSpec.loadout("mig_rockets").put(S8KO, 20, 0, 1, 2, 3).build());
        b.air = a;
        b.weapon("gsh23").ammo(GSH_30, 200).auto(1).spread(0.5F)
                .mount(WeaponSpec.Mount.HULL, v(0, 1.016, 1.161)).sound("aircraft_gun", 6F);

        // Tu-160 "White Swan": 54 m long, four crew, everything in two internal bays on rotary launchers
        b = TU160;
        b.health = 420F;
        b.armor = Armor.AIR;
        b.width = 9F;
        b.height = 7F;
        b.box(-1.8, 3.2, -27, 1.8, 6.4, 27.1).box(-7, 3.2, -14, 7, 4.8, 6).box(-28.5, 3.6, -12, 28.5, 4.4, 0)
                .box(-6.6, 2.3, -11.3, 6.6, 3.8, 1).box(-0.4, 6.0, -27, 0.4, 13.0, -18).box(-8, 8.8, -27, 8, 9.4, -22);
        b.seats = new Vector3d[]{v(0.45, 4.4, 19.4), v(-0.45, 4.4, 19.4), v(0.45, 4.4, 18.2), v(-0.45, 4.4, 18.2)};
        b.riderScale = 0.9F;
        b.separateGunner = false;
        b.hasTurret = false;
        b.tracked = false;
        b.enclosed = true;
        b.insideSeats = 4;
        b.engineSound = "jet_engine";
        b.wheelModel = null;
        b.turretModel = null;
        b.gunModel = "tu160_gear";
        a = new AirSpec();
        a.maxSpeed = 4.6;
        a.stallSpeed = 1.5;
        a.takeoffSpeed = 1.9;
        a.pitchRate = 1.5F;
        a.yawRate = 1.1F;
        a.cockpit = v(0.45, 5.3, 19.4);
        a.camDistance = 78;
        a.camHeight = 10;
        a.exhaust = v(4.96, 3.05, -11.4);
        a.exhaustSpread = 0.9;
        // two bays, six stations each on the rotary launchers
        a.pylons = new Vector3d[]{v(0.5, 4.2, 10.5), v(-0.5, 4.2, 10.5), v(0.5, 4.2, 8.5), v(-0.5, 4.2, 8.5),
                v(0.5, 4.2, 6.5), v(-0.5, 4.2, 6.5), v(0.5, 4.2, -0.5), v(-0.5, 4.2, -0.5), v(0.5, 4.2, -2.5),
                v(-0.5, 4.2, -2.5), v(0.5, 4.2, -4.5), v(-0.5, 4.2, -4.5)};
        a.internalBay = true;
        a.loadouts.add(AirSpec.loadout("tu_cruise").put(KH101, 1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11).build());
        a.loadouts.add(AirSpec.loadout("tu_nuclear").put(KH102, 1, 0, 1, 2, 3, 4, 5).put(KH101, 1, 6, 7, 8, 9, 10, 11).build());
        a.loadouts.add(AirSpec.loadout("tu_fab9000").put(FAB9000, 1, 2, 8).put(FAB3000, 1, 0, 6).build());
        a.loadouts.add(AirSpec.loadout("tu_fab3000").put(FAB3000, 1, 0, 1, 2, 3, 6, 7, 8, 9).build());
        a.loadouts.add(AirSpec.loadout("tu_carpet").put(FAB500, 4, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11).build());
        b.air = a;
    }

    public static final VehicleSpec[] ALL = {T72B3, T80U, T90M, BMPT, BMP1, TOS1A, GRAD, ORESHNIK, SU25, SU34, MIG23, TU160, ZAK, ZRK};

    private Specs() {
    }
}
