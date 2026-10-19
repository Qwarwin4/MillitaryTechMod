package com.qwarwin.militarytech.client;

import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.entity.VehicleEntity;
import com.qwarwin.militarytech.network.ClickVehiclePacket;
import com.qwarwin.militarytech.network.Network;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Right clicks on vehicles go by what's actually under the crosshair: a seat (anywhere on it, seen through glass or
 * a hatch), a door, a pylon, or the hull, measured against the vehicle's own shape instead of the one square box
 * Minecraft gives entities (which misses the nose and tail of anything long).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = MilitaryTech.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VehicleClicks {
    private static final double REACH = 6.0;

    @SubscribeEvent
    public static void onClick(InputEvent.ClickInputEvent event) {
        if (!event.isUseItem()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.getVehicle() instanceof VehicleEntity) {
            return;
        }
        Vector3d eye = mc.player.getEyePosition(1F);
        Vector3d dir = mc.player.getViewVector(1F);
        // a block in the way closer than the vehicle wins
        double limit = REACH;
        RayTraceResult hit = mc.hitResult;
        if (hit instanceof BlockRayTraceResult && hit.getType() == RayTraceResult.Type.BLOCK) {
            limit = Math.min(limit, hit.getLocation().distanceTo(eye));
        }
        VehicleEntity best = null;
        double[] bestPick = null;
        for (VehicleEntity v : mc.level.getEntitiesOfClass(VehicleEntity.class, new AxisAlignedBB(eye, eye).inflate(40))) {
            double[] p = v.pick(eye, dir, REACH);
            if (p != null && p[2] <= limit + 0.3 && (bestPick == null || p[2] < bestPick[2])) {
                best = v;
                bestPick = p;
            }
        }
        if (best == null) {
            return;
        }
        Network.CHANNEL.sendToServer(new ClickVehiclePacket(best.getId(), (int) bestPick[0], (int) bestPick[1]));
        event.setSwingHand(true);
        event.setCanceled(true);
    }

    private VehicleClicks() {
    }
}
