package com.qwarwin.militarytech.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/** World point to GUI coordinates for HUD markers. */
@OnlyIn(Dist.CLIENT)
public final class Projection {
    // vertical FOV actually used this frame, set from the FOV event
    public static double fov = 70.0;

    @Nullable
    /** camera roll this frame, degrees (aircraft in the cockpit view) */
    public static float roll;

    public static int[] toScreen(Vector3d world, int w, int h) {
        ActiveRenderInfo cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3d d = world.subtract(cam.getPosition());
        Vector3d fwd = Vector3d.directionFromRotation(cam.getXRot(), cam.getYRot());
        Vector3d left = Vector3d.directionFromRotation(0F, cam.getYRot() - 90F);
        Vector3d up = fwd.cross(left);
        double z = d.dot(fwd);
        if (z < 0.05) {
            return null;
        }
        double tanV = Math.tan(Math.toRadians(fov / 2.0));
        double tanH = tanV * w / (double) h;
        double sx = -d.dot(left) / z / tanH * (w / 2.0);
        double sy = d.dot(up) / z / tanV * (h / 2.0);
        if (roll != 0) {
            // the camera is rolled with the aircraft: so is everything on screen
            double r = Math.toRadians(roll);
            double rx = sx * Math.cos(r) - sy * Math.sin(r);
            double ry = sx * Math.sin(r) + sy * Math.cos(r);
            sx = rx;
            sy = ry;
        }
        return new int[]{(int) Math.round(w / 2.0 + sx), (int) Math.round(h / 2.0 - sy)};
    }

    private Projection() {
    }
}
