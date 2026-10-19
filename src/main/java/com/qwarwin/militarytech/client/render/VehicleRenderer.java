package com.qwarwin.militarytech.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qwarwin.militarytech.client.BoxModel;
import com.qwarwin.militarytech.client.CabControls;
import com.qwarwin.militarytech.client.MeshWheels;
import com.qwarwin.militarytech.client.VehicleCamera;
import com.qwarwin.militarytech.entity.AirSpec;
import com.qwarwin.militarytech.entity.AircraftEntity;
import com.qwarwin.militarytech.entity.Munition;
import com.qwarwin.militarytech.entity.OreshnikEntity;
import com.qwarwin.militarytech.entity.TruckEntity;
import com.qwarwin.militarytech.entity.VehicleEntity;
import com.qwarwin.militarytech.entity.VehicleSpec;
import com.qwarwin.militarytech.entity.WeaponSpec;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/** Draws any vehicle from its spec: hull, road wheels, turret and gun, or the aircraft with its stores. */
@OnlyIn(Dist.CLIENT)
public class VehicleRenderer extends EntityRenderer<VehicleEntity> {

    public VehicleRenderer(EntityRendererManager mgr) {
        super(mgr);
        shadowRadius = 1.6F;
    }

    @Override
    public void render(VehicleEntity e, float entityYaw, float partial, MatrixStack ms, IRenderTypeBuffer buf, int light) {
        VehicleSpec s = e.spec;
        float hullYaw = MathHelper.rotLerp(partial, e.yRotO, e.yRot);
        boolean inside = VehicleCamera.showInterior(e);
        // flash red for a moment after taking a hit is vanilla's job for mobs; vehicles just show smoke
        int overlay = OverlayTexture.NO_OVERLAY;
        ms.pushPose();
        ms.mulPose(Vector3f.YP.rotationDegrees(-hullYaw));
        if (e instanceof AircraftEntity) {
            ms.mulPose(Vector3f.XP.rotationDegrees(MathHelper.lerp(partial, e.xRotO, e.xRot)));
            ms.mulPose(Vector3f.ZP.rotationDegrees(MathHelper.lerp(partial, e.rollO, e.getRoll())));
        }
        // inside an armoured vehicle the armour itself isn't drawn: you look out through the vision slits
        boolean shell = !(inside && s.category == VehicleSpec.Category.TANK);
        if (shell) {
            BoxModel.get(s.hullModel).render(ms, buf, light, overlay, 1F, 1F, 1F, 1F);
        }
        boolean doorOpen = false;
        for (int d = 0; d < s.doors.size(); d++) {
            doorOpen |= e.doorOpen(d, partial) > 0.01F;
        }
        if (inside || doorOpen || s.category == VehicleSpec.Category.TRUCK) {
            // truck cabs have windows, so their insides are always there; an open door shows the troop compartment
            BoxModel.get(s.id + "_interior").render(ms, buf, lit(light));
        }
        if (e instanceof AircraftEntity) {
            BoxModel.get(s.id + "_cockpit").render(ms, buf, lit(light));
        }
        if (inside || doorOpen || s.category == VehicleSpec.Category.TRUCK || e instanceof AircraftEntity) {
            controls(e, ms, buf, lit(light), partial, hullYaw);
        }
        // generated additions to a real model (the Oreshnik's rear housing)
        BoxModel.get(s.id + "_extra").render(ms, buf, light);
        if (shell || inside) {
            // doors and hatches swing about their hinges, their inner skins with them
            for (int d = 0; d < s.doors.size(); d++) {
                VehicleSpec.Door door = s.doors.get(d);
                float open = e.doorOpen(d, partial);
                // ease in and out, like a heavy door
                open = open * open * (3F - 2F * open);
                ms.pushPose();
                ms.translate(door.hinge.x, door.hinge.y, door.hinge.z);
                Vector3f axis = door.axis == 'x' ? Vector3f.XP : door.axis == 'z' ? Vector3f.ZP : Vector3f.YP;
                ms.mulPose(axis.rotationDegrees(door.angle * open));
                if (shell) {
                    BoxModel.get(door.model).render(ms, buf, light);
                }
                if (inside || doorOpen) {
                    BoxModel.get(door.model + "_inner").render(ms, buf, lit(light));
                }
                ms.popPose();
            }
        }

        float wheel = MathHelper.lerp(partial, e.trackAngleO, e.trackAngle);
        if (s.wheelModel == null && !(e instanceof AircraftEntity) && shell) {
            // real-model wheels: each spins about its own axle, smaller ones faster; truck front wheels steer
            float steer = 0F;
            if (e instanceof TruckEntity) {
                TruckEntity t = (TruckEntity) e;
                steer = MathHelper.lerp(partial, t.steerO, t.steer) * 25F;
            }
            List<MeshWheels.Wheel> ws = MeshWheels.of(s.id);
            float front = -Float.MAX_VALUE;
            for (MeshWheels.Wheel w : ws) {
                front = Math.max(front, w.z);
            }
            for (MeshWheels.Wheel w : ws) {
                ms.pushPose();
                ms.translate(w.x, w.y, w.z);
                if (steer != 0F && w.z > front - 0.3F) {
                    ms.mulPose(Vector3f.YP.rotationDegrees(steer));
                }
                ms.mulPose(Vector3f.XP.rotationDegrees(wheel * (float) s.wheelRadius / Math.max(0.05F, w.r)));
                BoxModel.get(w.model).render(ms, buf, light);
                ms.popPose();
            }
        }
        if (s.wheelModel != null) {
            float steer = 0F;
            if (e instanceof TruckEntity) {
                TruckEntity t = (TruckEntity) e;
                steer = MathHelper.lerp(partial, t.steerO, t.steer) * 25F;
            }
            for (int i = 0; i < s.wheelZ.length; i++) {
                for (int side = -1; side <= 1; side += 2) {
                    ms.pushPose();
                    ms.translate(side * s.wheelX, s.wheelY, s.wheelZ[i]);
                    if (i == 0 && steer != 0F) {
                        ms.mulPose(Vector3f.YP.rotationDegrees(steer));
                    }
                    if (side < 0) {
                        ms.mulPose(Vector3f.YP.rotationDegrees(180F));
                        ms.mulPose(Vector3f.XP.rotationDegrees(-wheel));
                    } else {
                        ms.mulPose(Vector3f.XP.rotationDegrees(wheel));
                    }
                    BoxModel.get(s.wheelModel).render(ms, buf, light);
                    ms.popPose();
                }
            }
        }

        if (e instanceof AircraftEntity) {
            aircraftParts((AircraftEntity) e, ms, buf, light);
        } else if (e instanceof OreshnikEntity) {
            OreshnikEntity o = (OreshnikEntity) e;
            ms.pushPose();
            // the canister swings up about its hinge in the rear housing
            float up = o.erectProgress(partial);
            ms.translate(s.pivot.x, s.pivot.y, s.pivot.z);
            // once nearly upright the canister is lowered onto its base on the ground, not left hanging on the hinge
            float drop = MathHelper.clamp((up - 0.6F) / 0.4F, 0F, 1F);
            ms.translate(0, -(s.pivot.y - 0.08) * drop, 0);
            ms.mulPose(Vector3f.XP.rotationDegrees(-90F * up));
            BoxModel.get(s.gunModel).render(ms, buf, light);
            if (o.getAmmo(0) > 0) {
                BoxModel.get("oreshnik_cap").render(ms, buf, light);
            }
            ms.popPose();
        } else if (s.hasTurret && s.turretModel != null && !VehicleCamera.hideTurret(e)) {
            ms.translate(s.pivot.x, s.pivot.y, s.pivot.z);
            ms.mulPose(Vector3f.YP.rotationDegrees(-MathHelper.wrapDegrees(e.getTurretYaw(partial) - hullYaw)));
            if (shell) {
                BoxModel.get(s.turretModel).render(ms, buf, light);
            }
            if (inside) {
                BoxModel.get(s.id + "_interior_turret").render(ms, buf, lit(light));
            }
            ms.translate(s.trunnion.x, s.trunnion.y, s.trunnion.z);
            ms.mulPose(Vector3f.XP.rotationDegrees(e.getGunPitch(partial)));
            if (shell) {
                BoxModel.get(s.gunModel).render(ms, buf, light);
            }
            if (inside) {
                BoxModel.get(s.id + "_interior_gun").render(ms, buf, lit(light));
            }
            loadedTubes(e, ms, buf, light);
        }
        ms.popPose();
        super.render(e, entityYaw, partial, ms, buf, light);
    }

    /** Interiors have their own lamps: never darker than block light 11. */
    private static int lit(int light) {
        return Math.max(light & 0xFFFF, 11 << 4) | (light & 0xFFFF0000);
    }

    /** Caps on the ATGM tubes that still hold a missile; rockets peeking out of the Grad's tubes. */
    private static void loadedTubes(VehicleEntity e, MatrixStack ms, IRenderTypeBuffer buf, int light) {
        for (int w = 0; w < e.spec.weapons.size(); w++) {
            WeaponSpec ws = e.spec.weapons.get(w);
            Munition m = ws.ammo[0];
            String cap = m == Munition.ATAKA ? "bmpt_cap" : m == Munition.GRAD ? "grad_rocket_tip"
                    : m == Munition.TOS_220 ? "tos1a_rocket_tip" : m == Munition.SAM ? "zrk_cap"
                    : m == Munition.MALYUTKA ? "malyutka_rail" : null;
            if (cap == null) {
                continue;
            }
            int left = e.getAmmo(e.spec.ammoBase(w));
            int fired = ws.capacity[0] - left;
            if (m == Munition.MALYUTKA) {
                if (left > 0) {
                    ms.pushPose();
                    ms.translate(ws.muzzles[0].x, ws.muzzles[0].y - 0.1, ws.muzzles[0].z - 0.6);
                    BoxModel.get("munition/malyutka").render(ms, buf, light);
                    ms.popPose();
                }
                continue;
            }
            for (int i = fired; i < ws.capacity[0]; i++) {
                Vector3d t = ws.muzzles[i % ws.muzzles.length];
                ms.pushPose();
                ms.translate(t.x, t.y, t.z - (m == Munition.GRAD || m == Munition.TOS_220 ? 0.05 : 0.3));
                BoxModel.get(cap).render(ms, buf, light);
                ms.popPose();
            }
        }
    }

    /** Wheels, levers and sticks inside, moved by what the driver or pilot is doing. */
    private static void controls(VehicleEntity e, MatrixStack ms, IRenderTypeBuffer buf, int light, float partial, float hullYaw) {
        List<CabControls.Control> list = CabControls.of(e.spec.id);
        if (list.isEmpty()) {
            return;
        }
        float steer = 0, turn = 0, pitchIn = 0, rollIn = 0, thr = 0;
        if (e instanceof TruckEntity) {
            TruckEntity t = (TruckEntity) e;
            steer = MathHelper.lerp(partial, t.steerO, t.steer);
        }
        if (e instanceof AircraftEntity) {
            AircraftEntity a = (AircraftEntity) e;
            pitchIn = MathHelper.clamp((e.xRot - e.xRotO) / Math.max(0.5F, a.air().pitchRate), -1, 1);
            rollIn = MathHelper.clamp((e.getRoll() - e.rollO) / Math.max(0.5F, a.air().yawRate * 1.5F), -1, 1);
            thr = a.getThrottle();
        } else {
            turn = MathHelper.clamp(MathHelper.wrapDegrees(e.yRot - e.yRotO) / Math.max(0.5F, (float) e.spec.turnRate), -1, 1);
        }
        for (CabControls.Control c : list) {
            ms.pushPose();
            ms.translate(c.x, c.y, c.z);
            switch (c.kind) {
                case STEERING:
                    ms.mulPose(Vector3f.XP.rotationDegrees(c.tilt));
                    ms.mulPose(Vector3f.ZP.rotationDegrees(-steer * 270F));
                    break;
                case LEVER_L:
                    // pull the left lever to turn left
                    ms.mulPose(Vector3f.XP.rotationDegrees(-Math.max(0, -turn) * 28F));
                    break;
                case LEVER_R:
                    ms.mulPose(Vector3f.XP.rotationDegrees(-Math.max(0, turn) * 28F));
                    break;
                case STICK:
                    ms.mulPose(Vector3f.XP.rotationDegrees(-pitchIn * 14F));
                    ms.mulPose(Vector3f.ZP.rotationDegrees(-rollIn * 14F));
                    break;
                case THROTTLE:
                    ms.mulPose(Vector3f.XP.rotationDegrees(25F - thr * 50F));
                    break;
                default:
                    break;
            }
            BoxModel.get(c.model).render(ms, buf, light);
            ms.popPose();
        }
    }

    private static void aircraftParts(AircraftEntity a, MatrixStack ms, IRenderTypeBuffer buf, int light) {
        VehicleSpec s = a.spec;
        if (a.isGearDown() && s.gunModel != null) {
            BoxModel.get(s.gunModel).render(ms, buf, light);
        } else if (!a.isGearDown()) {
            // gear up: the bay doors shut over the wells
            BoxModel.get(s.id + "_gearup").render(ms, buf, light);
        }
        AirSpec air = a.air();
        if (air.internalBay) {
            // in the bomb bays, out of sight
            return;
        }
        for (int i = 0; i < air.pylons.length; i++) {
            Vector3d p = air.pylons[i];
            AirSpec.Store st = a.storeAt(i);
            ms.pushPose();
            ms.translate(p.x, p.y, p.z);
            // the pylon stays on the wing whatever hangs from it
            BoxModel.get("pylon").render(ms, buf, light);
            boolean pod = st != null && st.munition == Munition.S8KO;
            if (st == null || !pod && a.getAmmo(1 + i) <= 0) {
                ms.popPose();
                continue;
            }
            ms.translate(0, -0.3, 0);
            BoxModel.get(pod ? "munition/b8_pod" : "munition/" + st.munition.model).render(ms, buf, light);
            ms.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(VehicleEntity e) {
        return new ResourceLocation("militarytech", "textures/entity/" + e.spec.id + ".png");
    }
}
