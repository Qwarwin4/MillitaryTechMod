package com.qwarwin.militarytech.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qwarwin.militarytech.client.BoxModel;
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

/** Draws any vehicle from its spec: hull, road wheels, turret and gun, or the aircraft with its stores. */
@OnlyIn(Dist.CLIENT)
public class VehicleRenderer extends EntityRenderer<VehicleEntity> {

    public VehicleRenderer(EntityRendererManager mgr) {
        super(mgr);
        shadowRadius = 1.6F;
    }

    @Override
    public void render(VehicleEntity e, float entityYaw, float partial, MatrixStack ms, IRenderTypeBuffer buf, int light) {
        if (VehicleCamera.hideWhole(e)) {
            return;
        }
        VehicleSpec s = e.spec;
        float hullYaw = MathHelper.rotLerp(partial, e.yRotO, e.yRot);
        // flash red for a moment after taking a hit is vanilla's job for mobs; vehicles just show smoke
        int overlay = OverlayTexture.NO_OVERLAY;
        ms.pushPose();
        ms.mulPose(Vector3f.YP.rotationDegrees(-hullYaw));
        if (e instanceof AircraftEntity) {
            ms.mulPose(Vector3f.XP.rotationDegrees(MathHelper.lerp(partial, e.xRotO, e.xRot)));
            ms.mulPose(Vector3f.ZP.rotationDegrees(MathHelper.lerp(partial, e.rollO, e.getRoll())));
        }
        BoxModel.get(s.hullModel).render(ms, buf, light, overlay, 1F, 1F, 1F, 1F);

        float wheel = MathHelper.lerp(partial, e.trackAngleO, e.trackAngle);
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
            ms.translate(s.pivot.x, s.pivot.y, s.pivot.z);
            ms.mulPose(Vector3f.XP.rotationDegrees(-90F * o.erectProgress(partial)));
            BoxModel.get(s.gunModel).render(ms, buf, light);
            if (o.getAmmo(0) > 0) {
                BoxModel.get("oreshnik_cap").render(ms, buf, light);
            }
            ms.popPose();
        } else if (s.hasTurret && s.turretModel != null && !VehicleCamera.hideTurret(e)) {
            ms.translate(s.pivot.x, s.pivot.y, s.pivot.z);
            ms.mulPose(Vector3f.YP.rotationDegrees(-MathHelper.wrapDegrees(e.getTurretYaw(partial) - hullYaw)));
            BoxModel.get(s.turretModel).render(ms, buf, light);
            ms.translate(s.trunnion.x, s.trunnion.y, s.trunnion.z);
            ms.mulPose(Vector3f.XP.rotationDegrees(e.getGunPitch(partial)));
            BoxModel.get(s.gunModel).render(ms, buf, light);
            loadedTubes(e, ms, buf, light);
        }
        ms.popPose();
        super.render(e, entityYaw, partial, ms, buf, light);
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

    private static void aircraftParts(AircraftEntity a, MatrixStack ms, IRenderTypeBuffer buf, int light) {
        VehicleSpec s = a.spec;
        if (a.isGearDown() && s.gunModel != null) {
            BoxModel.get(s.gunModel).render(ms, buf, light);
        }
        AirSpec air = a.air();
        AirSpec.Loadout l = a.getLoadout();
        for (int i = 0; i < air.pylons.length; i++) {
            AirSpec.Store st = l.at(i);
            if (st == null) {
                continue;
            }
            boolean pod = st.munition == Munition.S8KO;
            if (!pod && a.getAmmo(1 + i) <= 0) {
                continue;
            }
            Vector3d p = air.pylons[i];
            ms.pushPose();
            ms.translate(p.x, p.y, p.z);
            BoxModel.get("pylon").render(ms, buf, light);
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
