package com.qwarwin.militarytech.client;

import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.client.render.NothingRenderer;
import com.qwarwin.militarytech.client.render.ProjectileRenderer;
import com.qwarwin.militarytech.client.render.VehicleRenderer;
import com.qwarwin.militarytech.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = MilitaryTech.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.T72B3.get(), VehicleRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.T80U.get(), VehicleRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.T90M.get(), VehicleRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.BMPT.get(), VehicleRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.BMP1.get(), VehicleRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.TOS1A.get(), VehicleRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.ZAK.get(), VehicleRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.ZRK.get(), VehicleRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.GRAD.get(), VehicleRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.PANTSIR.get(), VehicleRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.MSTA.get(), VehicleRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.ORESHNIK.get(), VehicleRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.SU25.get(), VehicleRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.SU34.get(), VehicleRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.MIG23.get(), VehicleRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.TU160.get(), VehicleRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.PROJECTILE.get(), ProjectileRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.CAMERA.get(), NothingRenderer::new);
        KeyBinds.register();
        event.enqueueWork(() -> {
            if (Minecraft.getInstance().getResourceManager() instanceof IReloadableResourceManager) {
                ((IReloadableResourceManager) Minecraft.getInstance().getResourceManager())
                        .registerReloadListener((IResourceManagerReloadListener) ClientSetup::onReload);
            }
        });
    }

    private static void onReload(IResourceManager rm) {
        BoxModel.clearCache();
        MeshWheels.clearCache();
        CabControls.clearCache();
    }

    private ClientSetup() {
    }
}
