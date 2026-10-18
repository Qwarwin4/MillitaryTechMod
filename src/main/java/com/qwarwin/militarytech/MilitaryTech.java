package com.qwarwin.militarytech;

import com.qwarwin.militarytech.network.Network;
import com.qwarwin.militarytech.registry.ModEntities;
import com.qwarwin.militarytech.registry.ModItems;
import com.qwarwin.militarytech.registry.ModSounds;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MilitaryTech.MODID)
public class MilitaryTech {
    public static final String MODID = "militarytech";

    public static final ItemGroup TAB = new ItemGroup(MODID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.T72B3.get());
        }
    };

    public MilitaryTech() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModSounds.SOUNDS.register(bus);
        bus.addListener(this::setup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    private void setup(FMLCommonSetupEvent event) {
        Network.init();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
