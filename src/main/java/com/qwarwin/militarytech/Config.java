package com.qwarwin.militarytech;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue EXPLOSIONS_BREAK_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue SHELLS_BREAK_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue FRAGMENT_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_TO_VEHICLES;
    public static final ForgeConfigSpec.DoubleValue BOMB_POWER;
    public static final ForgeConfigSpec.DoubleValue GROUND_SPEED;
    public static final ForgeConfigSpec.DoubleValue AIR_SPEED;
    public static final ForgeConfigSpec.IntValue ORESHNIK_RANGE;
    public static final ForgeConfigSpec.DoubleValue ORESHNIK_POWER;
    public static final ForgeConfigSpec.IntValue NUKE_CRATER;
    public static final ForgeConfigSpec.IntValue NUKE_BLAST;
    public static final ForgeConfigSpec.IntValue NUKE_FALLOUT;
    public static final ForgeConfigSpec.BooleanValue NEED_AMMO_CRATES;
    public static final ForgeConfigSpec.BooleanValue REALISTIC_AMMO;
    public static final ForgeConfigSpec.BooleanValue NEED_FUEL;
    public static final ForgeConfigSpec.BooleanValue TIMED_LOADING;
    public static final ForgeConfigSpec.BooleanValue LOAD_CHUNKS;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> BANNED_STORES;
    public static final ForgeConfigSpec.BooleanValue RELEASE_ON_GROUND;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("damage");
        EXPLOSIONS_BREAK_BLOCKS = b.comment("Shells, rockets, missiles and bombs break blocks")
                .define("explosionsBreakBlocks", true);
        SHELLS_BREAK_BLOCKS = b.comment("Small 30 mm shells also break blocks")
                .define("smallShellsBreakBlocks", false);
        FRAGMENT_DAMAGE = b.comment("Multiplier for shrapnel and blast damage to living things")
                .defineInRange("fragmentDamage", 1.0, 0.0, 10.0);
        DAMAGE_TO_VEHICLES = b.comment("Multiplier for all damage taken by vehicles")
                .defineInRange("damageToVehicles", 1.0, 0.0, 10.0);
        BOMB_POWER = b.comment("Multiplier for the blast of FAB bombs")
                .defineInRange("bombPower", 1.0, 0.1, 3.0);
        b.pop();
        b.push("vehicles");
        GROUND_SPEED = b.comment("Speed multiplier for tanks and trucks").defineInRange("groundSpeed", 1.0, 0.2, 3.0);
        AIR_SPEED = b.comment("Speed multiplier for aircraft").defineInRange("airSpeed", 1.0, 0.2, 2.0);
        NEED_AMMO_CRATES = b.comment("Placed vehicles start empty and are rearmed with ammo crates (creative ones always come loaded)")
                .define("needAmmoCrates", true);
        REALISTIC_AMMO = b.comment("Shells, rockets, missiles and bombs are items loaded into vehicles one by one; the ammo crate",
                        "only tops up machine-gun belts and 30 mm. Off: the crate fills everything but nuclear weapons.")
                .define("realisticAmmo", true);
        NEED_FUEL = b.comment("Vehicles burn fuel and are refuelled with fuel barrels").define("needFuel", true);
        TIMED_LOADING = b.comment("Loading a shell, a rocket or a bomb takes time, and only with the vehicle stopped")
                .define("timedLoading", true);
        LOAD_CHUNKS = b.comment("Rockets and missiles keep the chunks they fly through loaded")
                .define("loadChunks", true);
        BANNED_STORES = b.comment("Aircraft stores nobody may hang or release on this server: munition ids (fab9000, rn28, kh102,",
                        "kh29, r73, s8, ...), \"nuclear\" for every nuclear weapon, \"all\" for every store")
                .defineList("bannedStores", java.util.Collections.emptyList(), o -> o instanceof String);
        RELEASE_ON_GROUND = b.comment("Stores can be released with the weight on the wheels. Off (as in real aircraft): the",
                        "weapons interlock holds everything until the aircraft is airborne")
                .define("releaseOnGround", false);
        b.pop();
        b.push("oreshnik");
        ORESHNIK_RANGE = b.comment("Maximum range in blocks").defineInRange("range", 3000, 100, 30000);
        ORESHNIK_POWER = b.comment("Blast of each of the 42 submunitions").defineInRange("warheadPower", 8.0, 0.0, 20.0);
        b.pop();
        b.push("nuke");
        NUKE_CRATER = b.comment("Crater radius of the nuclear Oreshnik, blocks").defineInRange("crater", 42, 5, 80);
        NUKE_BLAST = b.comment("Radius of the blast that kills, burns and throws things, blocks").defineInRange("blast", 200, 20, 500);
        NUKE_FALLOUT = b.comment("Seconds the ground around a nuclear crater stays radioactive (0: no fallout)")
                .defineInRange("radiationSeconds", 1800, 0, 72000);
        b.pop();
        SPEC = b.build();
    }

    private Config() {
    }

    /** Whether the server forbids this store (config bannedStores). */
    public static boolean banned(com.qwarwin.militarytech.entity.Munition m) {
        java.util.List<? extends String> list = BANNED_STORES.get();
        if (list.isEmpty()) {
            return false;
        }
        String id = m.name().toLowerCase(java.util.Locale.ROOT);
        for (String s : list) {
            String t = s.trim().toLowerCase(java.util.Locale.ROOT);
            if (t.equals(id) || t.equals("all") || t.equals("*") || (t.equals("nuclear") && m.isNuclear())) {
                return true;
            }
        }
        return false;
    }
}
