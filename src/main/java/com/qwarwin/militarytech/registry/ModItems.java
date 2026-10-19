package com.qwarwin.militarytech.registry;

import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.entity.Munition;
import com.qwarwin.militarytech.entity.Specs;
import com.qwarwin.militarytech.entity.VehicleSpec;
import com.qwarwin.militarytech.item.AmmoCrateItem;
import com.qwarwin.militarytech.item.MunitionBoxItem;
import com.qwarwin.militarytech.item.MunitionItem;
import com.qwarwin.militarytech.item.PilotArmor;
import com.qwarwin.militarytech.item.TooltipItem;
import com.qwarwin.militarytech.item.VehicleItem;
import net.minecraft.item.Item;
import net.minecraft.item.Rarity;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MilitaryTech.MODID);

    private static final Map<String, RegistryObject<Item>> BY_SPEC = new HashMap<>();

    public static final RegistryObject<Item> T72B3 = vehicle(Specs.T72B3);
    public static final RegistryObject<Item> T80U = vehicle(Specs.T80U);
    public static final RegistryObject<Item> T90M = vehicle(Specs.T90M);
    public static final RegistryObject<Item> BMPT = vehicle(Specs.BMPT);
    public static final RegistryObject<Item> BMP1 = vehicle(Specs.BMP1);
    public static final RegistryObject<Item> TOS1A = vehicle(Specs.TOS1A);
    public static final RegistryObject<Item> GRAD = vehicle(Specs.GRAD);
    public static final RegistryObject<Item> PANTSIR = vehicle(Specs.PANTSIR);
    public static final RegistryObject<Item> MSTA = vehicle(Specs.MSTA);
    public static final RegistryObject<Item> ORESHNIK = vehicle(Specs.ORESHNIK);
    public static final RegistryObject<Item> SU25 = vehicle(Specs.SU25);
    public static final RegistryObject<Item> SU34 = vehicle(Specs.SU34);
    public static final RegistryObject<Item> MIG23 = vehicle(Specs.MIG23);
    public static final RegistryObject<Item> TU160 = vehicle(Specs.TU160);
    public static final RegistryObject<Item> ZAK = vehicle(Specs.ZAK);
    public static final RegistryObject<Item> ZRK = vehicle(Specs.ZRK);

    public static final RegistryObject<Item> AMMO_CRATE = ITEMS.register("ammo_crate",
            () -> new AmmoCrateItem(props().stacksTo(16)));
    // the pilot's kit
    public static final RegistryObject<Item> PILOT_HELMET = ITEMS.register("pilot_helmet",
            () -> new net.minecraft.item.ArmorItem(PilotArmor.MATERIAL, net.minecraft.inventory.EquipmentSlotType.HEAD, props()));
    public static final RegistryObject<Item> PILOT_SUIT = ITEMS.register("pilot_suit",
            () -> new net.minecraft.item.ArmorItem(PilotArmor.MATERIAL, net.minecraft.inventory.EquipmentSlotType.CHEST, props()));
    public static final RegistryObject<Item> PILOT_TROUSERS = ITEMS.register("pilot_trousers",
            () -> new net.minecraft.item.ArmorItem(PilotArmor.MATERIAL, net.minecraft.inventory.EquipmentSlotType.LEGS, props()));
    public static final RegistryObject<Item> PILOT_BOOTS = ITEMS.register("pilot_boots",
            () -> new net.minecraft.item.ArmorItem(PilotArmor.MATERIAL, net.minecraft.inventory.EquipmentSlotType.FEET, props()));
    public static final RegistryObject<Item> MUNITION_BOX = ITEMS.register("munition_box",
            () -> new MunitionBoxItem(props().stacksTo(1)));
    public static final RegistryObject<Item> FUEL_BARREL = ITEMS.register("fuel_barrel",
            () -> new TooltipItem(props().stacksTo(4), "tooltip.militarytech.fuel_barrel"));
    /** The only way to take a vehicle back into the inventory (or, in creative, to break it with a hit). */
    public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench",
            () -> new TooltipItem(props().stacksTo(1), "tooltip.militarytech.wrench"));
    public static final RegistryObject<Item> REPAIR_KIT = ITEMS.register("repair_kit",
            () -> new TooltipItem(props().stacksTo(16), "tooltip.militarytech.repair_kit"));
    public static final RegistryObject<Item> NUCLEAR_WARHEAD = ITEMS.register("nuclear_warhead",
            () -> new TooltipItem(props().stacksTo(1).rarity(Rarity.EPIC), "tooltip.militarytech.nuclear_warhead"));

    // ammunition: loaded into vehicles by hand (the crate only tops up belts and 30 mm)
    public static final RegistryObject<Item> BELT_762 = ammo("belt_762", 16, 250, Munition.PKT);
    public static final RegistryObject<Item> BELT_127 = ammo("belt_127", 16, 150, Munition.KORD);
    public static final RegistryObject<Item> BELT_30 = ammo("belt_30", 16, 100, Munition.SHELL_30, Munition.GSH_30, Munition.AA_30);
    public static final RegistryObject<Item> SHELL_125_APFSDS = ammo("shell_125_apfsds", 16, 1, Munition.APFSDS);
    public static final RegistryObject<Item> SHELL_125_HE = ammo("shell_125_he", 16, 1, Munition.HE_125);
    public static final RegistryObject<Item> SHELL_152_HE = ammo("shell_152_he", 8, 1, Munition.HE_152);
    public static final RegistryObject<Item> ATGM_REFLEKS = ammo("atgm_refleks", 8, 1, Munition.REFLEKS);
    public static final RegistryObject<Item> SHELL_73 = ammo("shell_73", 16, 2, Munition.HEAT_73);
    public static final RegistryObject<Item> ATGM_MALYUTKA = ammo("atgm_malyutka", 8, 1, Munition.MALYUTKA);
    public static final RegistryObject<Item> ATGM_ATAKA = ammo("atgm_ataka", 8, 1, Munition.ATAKA);
    public static final RegistryObject<Item> ROCKET_GRAD = ammo("rocket_grad", 16, 4, Munition.GRAD);
    public static final RegistryObject<Item> ROCKET_TOS = ammo("rocket_tos", 16, 2, Munition.TOS_220);
    public static final RegistryObject<Item> ROCKETS_S8 = ammo("rockets_s8", 8, 20, Munition.S8KO);
    public static final RegistryObject<Item> MISSILE_SAM = ammo("missile_sam", 8, 2, Munition.SAM);
    public static final RegistryObject<Item> MISSILE_R73 = ammo("missile_r73", 4, 1, Munition.R73);
    public static final RegistryObject<Item> MISSILE_R40 = ammo("missile_r40", 4, 1, Munition.R40);
    public static final RegistryObject<Item> MISSILE_KH29 = ammo("missile_kh29", 4, 1, Munition.KH29);
    public static final RegistryObject<Item> MISSILE_KH59 = ammo("missile_kh59", 2, 1, Munition.KH59);
    public static final RegistryObject<Item> MISSILE_KH101 = ammo("missile_kh101", 4, 1, Munition.KH101);
    public static final RegistryObject<Item> MISSILE_KH102 = ITEMS.register("missile_kh102",
            () -> new MunitionItem(props().stacksTo(1).rarity(Rarity.EPIC), 1, Munition.KH102));
    public static final RegistryObject<Item> BOMB_RN28 = ITEMS.register("bomb_rn28",
            () -> new MunitionItem(props().stacksTo(1).rarity(Rarity.EPIC), 1, Munition.RN28));
    public static final RegistryObject<Item> BOMB_FAB250 = ammo("bomb_fab250", 8, 1, Munition.FAB250);
    public static final RegistryObject<Item> BOMB_FAB500 = ammo("bomb_fab500", 4, 1, Munition.FAB500);
    public static final RegistryObject<Item> BOMB_FAB1500 = ammo("bomb_fab1500", 2, 1, Munition.FAB1500);
    public static final RegistryObject<Item> BOMB_FAB3000 = ammo("bomb_fab3000", 1, 1, Munition.FAB3000);
    public static final RegistryObject<Item> BOMB_FAB9000 = ammo("bomb_fab9000", 1, 1, Munition.FAB9000);
    public static final RegistryObject<Item> MISSILE_ORESHNIK = ammo("missile_oreshnik", 1, 1, Munition.ORESHNIK);

    // crafting parts
    public static final RegistryObject<Item> ARMOR_PLATE = part("armor_plate");
    public static final RegistryObject<Item> ERA_BLOCK = part("era_block");
    public static final RegistryObject<Item> TRACK_LINKS = part("track_links");
    public static final RegistryObject<Item> TRUCK_WHEEL = part("truck_wheel");
    public static final RegistryObject<Item> DIESEL_ENGINE = part("diesel_engine");
    public static final RegistryObject<Item> GAS_TURBINE = part("gas_turbine");
    public static final RegistryObject<Item> JET_ENGINE = part("jet_engine");
    public static final RegistryObject<Item> GUN_BARREL = part("gun_barrel");
    public static final RegistryObject<Item> AUTOCANNON = part("autocannon");
    public static final RegistryObject<Item> FIRE_CONTROL = part("fire_control");
    public static final RegistryObject<Item> ROCKET_MOTOR = part("rocket_motor");
    public static final RegistryObject<Item> WARHEAD = part("warhead");
    public static final RegistryObject<Item> AIRFRAME = part("airframe");

    private static Item.Properties props() {
        return new Item.Properties().tab(MilitaryTech.TAB);
    }

    private static RegistryObject<Item> ammo(String id, int stack, int rounds, Munition... feeds) {
        return ITEMS.register(id, () -> new MunitionItem(props().stacksTo(stack), rounds, feeds));
    }

    private static RegistryObject<Item> part(String id) {
        return ITEMS.register(id, () -> new Item(props()));
    }

    private static RegistryObject<Item> vehicle(VehicleSpec s) {
        RegistryObject<Item> r = ITEMS.register(s.id, () -> new VehicleItem(props().stacksTo(1), s));
        BY_SPEC.put(s.id, r);
        return r;
    }

    public static Item forSpec(VehicleSpec s) {
        return BY_SPEC.get(s.id).get();
    }

    private ModItems() {
    }
}
