package com.qwarwin.militarytech.registry;

import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.entity.Specs;
import com.qwarwin.militarytech.entity.VehicleSpec;
import com.qwarwin.militarytech.item.AmmoCrateItem;
import com.qwarwin.militarytech.item.TooltipItem;
import com.qwarwin.militarytech.item.VehicleItem;
import net.minecraft.item.Item;
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
    public static final RegistryObject<Item> ORESHNIK = vehicle(Specs.ORESHNIK);
    public static final RegistryObject<Item> SU25 = vehicle(Specs.SU25);
    public static final RegistryObject<Item> SU34 = vehicle(Specs.SU34);
    public static final RegistryObject<Item> MIG25 = vehicle(Specs.MIG25);
    public static final RegistryObject<Item> ZAK = vehicle(Specs.ZAK);
    public static final RegistryObject<Item> ZRK = vehicle(Specs.ZRK);

    public static final RegistryObject<Item> AMMO_CRATE = ITEMS.register("ammo_crate",
            () -> new AmmoCrateItem(props().stacksTo(16)));
    public static final RegistryObject<Item> REPAIR_KIT = ITEMS.register("repair_kit",
            () -> new TooltipItem(props().stacksTo(16), "tooltip.militarytech.repair_kit"));

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
