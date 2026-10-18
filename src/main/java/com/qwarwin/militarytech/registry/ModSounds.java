package com.qwarwin.militarytech.registry;

import com.qwarwin.militarytech.MilitaryTech;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MilitaryTech.MODID);

    /** Keep in step with tools/gen_sounds.py. */
    public static final String[] NAMES = {"tank_gun", "grom", "mg", "cannon_30", "aircraft_gun", "atgm_launch",
            "missile_launch", "rocket_launch", "grad_launch", "bomb_release", "oreshnik_launch", "incoming", "reload",
            "hydraulics", "tank_engine", "turbine", "truck_engine", "jet_engine", "lock_tone", "seeker_tone",
            "missile_warning", "missile_motor"};

    private static final Map<String, RegistryObject<SoundEvent>> BY_NAME = new HashMap<>();

    static {
        for (String n : NAMES) {
            BY_NAME.put(n, SOUNDS.register(n, () -> new SoundEvent(MilitaryTech.id(n))));
        }
    }

    public static SoundEvent get(String name) {
        RegistryObject<SoundEvent> r = BY_NAME.get(name);
        return r == null ? SoundEvents.GENERIC_EXPLODE : r.get();
    }

    private ModSounds() {
    }
}
