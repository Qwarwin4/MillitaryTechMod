package com.qwarwin.militarytech.client;

import com.qwarwin.militarytech.entity.Munition;
import com.qwarwin.militarytech.entity.ProjectileEntity;
import com.qwarwin.militarytech.entity.VehicleEntity;
import com.qwarwin.militarytech.entity.VehicleSpec;
import com.qwarwin.militarytech.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Entry points from common entity code, always called through DistExecutor. */
@OnlyIn(Dist.CLIENT)
public final class ClientHooks {

    public static void onVehicleSeen(VehicleEntity v) {
        if (v.spec.category == VehicleSpec.Category.TURRET) {
            return;
        }
        float vol = v.spec.isAircraft() ? 8F : 2.2F;
        Minecraft.getInstance().getSoundManager().play(new EntityLoopSound(v, ModSounds.get(v.spec.engineSound), vol));
    }

    public static void onProjectileSeen(ProjectileEntity p) {
        Munition m = p.getMunition();
        if (m.isGuided() || m.kind == Munition.Kind.BALLISTIC) {
            float vol = m.kind == Munition.Kind.BALLISTIC ? 30F : 3F;
            Minecraft.getInstance().getSoundManager().play(new EntityLoopSound(p, ModSounds.get("missile_motor"), vol));
        }
    }

    /** The local player drives this vehicle: one tick of its simulation with the current controls. */
    public static void driveLocal(VehicleEntity v) {
        // after an EMP the controls do nothing: it rolls to a stop, or glides down
        v.simulate(v.isEmpDead() || v.outOfFuel() ? null : VehicleControl.controlsFor(v));
    }

    private ClientHooks() {
    }
}
