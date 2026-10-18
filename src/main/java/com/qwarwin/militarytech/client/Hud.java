package com.qwarwin.militarytech.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.entity.AirSpec;
import com.qwarwin.militarytech.entity.AircraftEntity;
import com.qwarwin.militarytech.entity.FireControl;
import com.qwarwin.militarytech.entity.Munition;
import com.qwarwin.militarytech.entity.OreshnikEntity;
import com.qwarwin.militarytech.entity.VehicleEntity;
import com.qwarwin.militarytech.entity.WeaponSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/** Sights, aiming marks and the status panel for whatever the player is sitting in. */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = MilitaryTech.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Hud {
    private static final int GREEN = 0xFF7CFF6A;
    private static final int GREEN_DIM = 0x997CFF6A;
    private static final int AMBER = 0xFFFFC040;
    private static final int RED = 0xFFFF4040;
    private static final int WHITE = 0xFFE8E8E8;
    private static final int PANEL = 0x70000000;

    // impact prediction, recomputed once a tick
    private static int predictedTick = -1;
    @Nullable
    private static Vector3d predicted;
    // reload timer of the main weapon, tracked from the ammo counter going down
    private static int lastMainAmmo = -1, reloadStart, reloadLength;
    private static int lastSelected = -1;

    @SubscribeEvent
    public static void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (VehicleControl.current() == null) {
            return;
        }
        switch (event.getType()) {
            case CROSSHAIRS:
            case EXPERIENCE:
            case JUMPBAR:
            case HEALTHMOUNT:
                event.setCanceled(true);
                break;
            default:
                break;
        }
    }

    @SubscribeEvent
    public static void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        VehicleEntity v = VehicleControl.current();
        if (v == null || mc.options.hideGui) {
            lastMainAmmo = -1;
            return;
        }
        MatrixStack ms = event.getMatrixStack();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        float pt = event.getPartialTicks();
        boolean gunner = v.isGunner(mc.player);
        if (v instanceof AircraftEntity) {
            aircraft(ms, mc, (AircraftEntity) v, w, h, pt);
        } else {
            if (VehicleCamera.isSight() && gunner && v.spec.hasTurret) {
                sightOverlay(ms, mc, v, w, h);
            }
            if (gunner) {
                groundAim(ms, mc, v, w, h, pt);
            } else {
                cross(ms, w / 2, h / 2, 3, WHITE);
            }
            if (v instanceof FireControl) {
                fireControl(ms, mc, v, (FireControl) v, w, h);
            } else if (v instanceof OreshnikEntity) {
                oreshnik(ms, mc, (OreshnikEntity) v, w, h);
            }
            groundPanel(ms, mc, v, w, h, gunner);
        }
    }

    // ------------------------------------------------------------------ ground vehicles

    private static void groundAim(MatrixStack ms, Minecraft mc, VehicleEntity v, int w, int h, float pt) {
        int cx = w / 2, cy = h / 2;
        cross(ms, cx, cy, 4, WHITE);
        if (!v.spec.hasTurret) {
            return;
        }
        // where the gun actually points, at the distance of what's under the crosshair
        Vector3d from = v.turretToWorld(v.spec.trunnion, v.getTurretYaw(pt));
        Vector3d aim = VehicleControl.aimPoint;
        double dist = aim == null ? 400 : Math.max(10, aim.distanceTo(from));
        Vector3d gunDir = Vector3d.directionFromRotation(v.getGunPitch(pt), v.getTurretYaw(pt));
        int[] g = Projection.toScreen(from.add(gunDir.scale(dist)), w, h);
        if (g != null && !(v instanceof FireControl && v.getTarget() != null)) {
            boolean on = Math.abs(g[0] - cx) < 3 && Math.abs(g[1] - cy) < 3;
            circle(ms, g[0], g[1], 5, on ? GREEN : AMBER);
        }
        if (aim != null && !(v instanceof FireControl)) {
            String r = I18n.get("hud.militarytech.meters", (int) aim.distanceTo(from));
            text(ms, mc, r, cx + 8, cy + 6, GREEN_DIM);
        }
        turretIndicator(ms, v, w - 34, h - 60, pt);
    }

    private static void sightOverlay(MatrixStack ms, Minecraft mc, VehicleEntity v, int w, int h) {
        int cx = w / 2, cy = h / 2;
        int r = Math.min(w, h) / 2 - 8;
        // dark surround of the eyepiece
        AbstractGui.fill(ms, 0, 0, w, cy - r, 0xD0000000);
        AbstractGui.fill(ms, 0, cy + r, w, h, 0xD0000000);
        AbstractGui.fill(ms, 0, cy - r, cx - r, cy + r, 0xD0000000);
        AbstractGui.fill(ms, cx + r, cy - r, w, cy + r, 0xD0000000);
        circle(ms, cx, cy, r, 0xFF101010);
        // reticle: chevron, broken horizontal line, stadia marks below
        int c = 0xFFFF5030;
        line(ms, cx - 6, cy + 6, cx, cy, c);
        line(ms, cx, cy, cx + 6, cy + 6, c);
        AbstractGui.fill(ms, cx - r + 10, cy, cx - 14, cy + 1, c);
        AbstractGui.fill(ms, cx + 14, cy, cx + r - 10, cy + 1, c);
        for (int i = 1; i <= 6; i++) {
            int y = cy + i * 10;
            int len = i % 2 == 0 ? 8 : 4;
            AbstractGui.fill(ms, cx - len, y, cx + len, y + 1, c);
        }
        AbstractGui.fill(ms, cx, cy + 8, cx + 1, cy + 64, c);
        text(ms, mc, String.format("%.1fx", VehicleCamera.zoom()), cx - r + 14, cy + r - 16, GREEN);
    }

    /** Little hull outline with the turret's heading on it, bottom right. */
    private static void turretIndicator(MatrixStack ms, VehicleEntity v, int x, int y, float pt) {
        AbstractGui.fill(ms, x - 6, y - 10, x + 6, y + 10, 0x80303030);
        outline(ms, x - 6, y - 10, x + 6, y + 10, 0xC0A0A0A0);
        float hull = MathHelper.rotLerp(pt, v.yRotO, v.yRot);
        float rel = MathHelper.wrapDegrees(v.getTurretYaw(pt) - hull);
        double a = Math.toRadians(rel);
        int ex = x - (int) Math.round(Math.sin(a) * 16);
        int ey = y - (int) Math.round(Math.cos(a) * 16);
        line(ms, x, y, ex, ey, GREEN);
        AbstractGui.fill(ms, x - 2, y - 2, x + 3, y + 3, GREEN);
    }

    private static void groundPanel(MatrixStack ms, Minecraft mc, VehicleEntity v, int w, int h, boolean gunner) {
        int x = 6, y = h - 92;
        AbstractGui.fill(ms, x - 3, y - 3, x + 150, y + 62, PANEL);
        text(ms, mc, I18n.get(v.spec.key()), x, y, WHITE);
        bar(ms, x, y + 11, 100, v.getHealth() / v.getMaxHealth());
        double kmh = Math.abs(v.getSpeed()) * 20 * 3.6;
        if (!v.isDriver(mc.player)) {
            kmh = v.getDeltaMovement().length() * 72;
        }
        text(ms, mc, I18n.get("hud.militarytech.kmh", (int) Math.round(kmh)), x + 106, y + 9, WHITE);
        if (!gunner || v.spec.weapons.isEmpty()) {
            return;
        }
        int line = y + 22;
        for (int wi = 0; wi < v.spec.weapons.size() && wi < 2; wi++) {
            WeaponSpec ws = v.spec.weapons.get(wi);
            int base = v.spec.ammoBase(wi);
            StringBuilder sb = new StringBuilder(I18n.get(ws.secondary ? "hud.militarytech.rmb" : "hud.militarytech.lmb")).append(' ');
            for (int o = 0; o < ws.ammo.length; o++) {
                boolean sel = wi != 0 || v.getSelected() == o;
                if (ws.ammo.length > 1) {
                    sb.append(sel ? "[" : " ");
                }
                sb.append(I18n.get(ws.ammo[o].key())).append(' ').append(v.getAmmo(base + o));
                if (ws.ammo.length > 1) {
                    sb.append(sel ? "]" : " ");
                }
            }
            text(ms, mc, sb.toString(), x, line, wi == 0 ? GREEN : GREEN_DIM);
            line += 10;
        }
        // reload of the main weapon
        WeaponSpec main = v.spec.weapons.get(0);
        int sel = Math.min(v.getSelected(), main.ammo.length - 1);
        int ammo = v.getAmmo(sel);
        int now = mc.player.tickCount;
        if (sel != lastSelected && lastSelected >= 0) {
            reloadStart = now;
            reloadLength = main.interval / 2;
        } else if (lastMainAmmo >= 0 && ammo < lastMainAmmo && main.interval >= 20) {
            reloadStart = now;
            reloadLength = main.interval;
        }
        lastSelected = sel;
        lastMainAmmo = ammo;
        if (main.interval >= 20 && now - reloadStart < reloadLength) {
            float k = (now - reloadStart) / (float) reloadLength;
            text(ms, mc, I18n.get("hud.militarytech.reloading"), x, line, AMBER);
            bar(ms, x + 70, line + 1, 60, k);
        } else if (main.interval >= 20) {
            text(ms, mc, I18n.get("hud.militarytech.ready"), x, line, GREEN);
        }
        if (main.ammo.length > 1) {
            text(ms, mc, I18n.get("hud.militarytech.switch_hint"), x, line + 10, 0xFF909090);
        }
    }

    private static void fireControl(MatrixStack ms, Minecraft mc, VehicleEntity g, FireControl fc, int w, int h) {
        int now = mc.player.tickCount;
        if (predictedTick != now) {
            predictedTick = now;
            Vector3d from = fc.rackOrigin();
            Vector3d dir = Vector3d.directionFromRotation(g.getGunPitch(), g.getTurretYaw());
            predicted = predict(mc, fc.rocket(), from, dir.scale(fc.rocket().speed));
        }
        int x = w - 160, y = 8;
        AbstractGui.fill(ms, x - 4, y - 4, w - 4, y + 54, PANEL);
        text(ms, mc, I18n.get(g.spec.key()) + " — " + I18n.get("hud.militarytech.fire_control"), x, y, WHITE);
        if (predicted != null) {
            int d = (int) Math.sqrt(predicted.distanceToSqr(g.getX(), predicted.y, g.getZ()));
            text(ms, mc, I18n.get("hud.militarytech.impact", d), x, y + 11, GREEN);
            int[] p = Projection.toScreen(predicted, w, h);
            if (p != null) {
                diamond(ms, p[0], p[1], 6, AMBER);
            }
        } else {
            text(ms, mc, I18n.get("hud.militarytech.impact_far"), x, y + 11, AMBER);
        }
        BlockPos t = g.getTarget();
        if (t != null) {
            int d = (int) Math.sqrt(t.distSqr(g.getX(), t.getY(), g.getZ(), true));
            text(ms, mc, I18n.get("hud.militarytech.target", t.getX(), t.getZ(), d), x, y + 22, WHITE);
            if (fc.solution(t)[2] > 0) {
                text(ms, mc, I18n.get("hud.militarytech.out_of_reach"), x, y + 33, RED);
            } else {
                text(ms, mc, I18n.get("hud.militarytech.laid"), x, y + 33, GREEN);
            }
        } else {
            text(ms, mc, I18n.get("hud.militarytech.manual"), x, y + 22, 0xFF909090);
        }
        text(ms, mc, I18n.get("hud.militarytech.grad_hint"), x, y + 44, 0xFF909090);
    }

    private static void oreshnik(MatrixStack ms, Minecraft mc, OreshnikEntity o, int w, int h) {
        int x = w - 170, y = 8;
        AbstractGui.fill(ms, x - 4, y - 4, w - 4, y + 65, PANEL);
        text(ms, mc, I18n.get("entity.militarytech.oreshnik"), x, y, WHITE);
        float e = o.erectProgress(1F);
        String st = o.isErected() ? I18n.get("hud.militarytech.erected")
                : e > 0 ? I18n.get("hud.militarytech.erecting", (int) (e * 100)) : I18n.get("hud.militarytech.stowed");
        text(ms, mc, st, x, y + 11, o.isErected() ? GREEN : AMBER);
        BlockPos t = o.getTarget();
        if (t != null) {
            int d = (int) Math.sqrt(t.distSqr(o.getX(), t.getY(), o.getZ(), true));
            text(ms, mc, I18n.get("hud.militarytech.target", t.getX(), t.getZ(), d), x, y + 22, WHITE);
        } else {
            text(ms, mc, I18n.get("hud.militarytech.no_target"), x, y + 22, AMBER);
        }
        int sel = Math.min(o.getSelected(), 1);
        boolean ready = o.isErected() && t != null && o.getAmmo(sel) > 0;
        text(ms, mc, o.getAmmo(sel) > 0 ? (ready ? I18n.get("hud.militarytech.ready_launch") : I18n.get("hud.militarytech.not_ready"))
                : I18n.get("hud.militarytech.empty"), x, y + 33, ready ? RED : 0xFF909090);
        text(ms, mc, I18n.get(sel == 1 ? "hud.militarytech.warhead_nuke" : "hud.militarytech.warhead_conventional"), x, y + 44,
                sel == 1 ? 0xFFFFD040 : 0xFFB0B0B0);
        text(ms, mc, I18n.get("hud.militarytech.oreshnik_hint"), x, y + 55, 0xFF909090);
    }

    // ------------------------------------------------------------------ aircraft

    private static void aircraft(MatrixStack ms, Minecraft mc, AircraftEntity a, int w, int h, float pt) {
        int cx = w / 2, cy = h / 2;
        AirSpec air = a.air();
        // where the mouse wants to go
        Vector3d camPos = VehicleCamera.position(mc, a, pt);
        int[] want = Projection.toScreen(camPos.add(Vector3d.directionFromRotation(VehicleControl.controlPitch(),
                VehicleControl.controlYaw()).scale(800)), w, h);
        if (want != null) {
            circle(ms, want[0], want[1], 9, 0xB0FFFFFF);
        }
        // where the nose (and the gun) points
        Vector3d pos = new Vector3d(MathHelper.lerp(pt, a.xo, a.getX()), MathHelper.lerp(pt, a.yo, a.getY()) + 2,
                MathHelper.lerp(pt, a.zo, a.getZ()));
        Vector3d nose = a.nose();
        int[] n = Projection.toScreen(pos.add(nose.scale(400)), w, h);
        if (n != null) {
            cross(ms, n[0], n[1], 6, GREEN);
            circle(ms, n[0], n[1], 2, GREEN);
        }
        Munition sel = a.selectedGroup();
        int now = mc.player.tickCount;
        if (sel != null && (sel.kind == Munition.Kind.BOMB || sel.isRocket())) {
            if (predictedTick != now) {
                predictedTick = now;
                Vector3d v = a.getDeltaMovement();
                Vector3d v0 = sel.kind == Munition.Kind.BOMB ? v.add(0, -0.15, 0) : v.add(nose.scale(sel.speed));
                predicted = predict(mc, sel, a.position().add(0, 1.5, 0), v0);
            }
            if (predicted != null) {
                int[] p = Projection.toScreen(predicted, w, h);
                if (p != null) {
                    circle(ms, p[0], p[1], 7, AMBER);
                    AbstractGui.fill(ms, p[0], p[1], p[0] + 1, p[1] + 1, AMBER);
                    if (n != null) {
                        line(ms, n[0], n[1], p[0], p[1], 0x80FFC040);
                    }
                }
            }
        }
        if (sel != null && (sel.guidance == Munition.Guidance.LASER || sel.guidance == Munition.Guidance.CRUISE)
                && VehicleControl.aimPoint != null) {
            int[] p = Projection.toScreen(VehicleControl.aimPoint, w, h);
            if (p != null) {
                diamond(ms, p[0], p[1], 7, RED);
            }
        }
        if (VehicleControl.seekerTarget >= 0) {
            Entity t = mc.level.getEntity(VehicleControl.seekerTarget);
            if (t != null) {
                int[] p = Projection.toScreen(t.getBoundingBox().getCenter(), w, h);
                if (p != null) {
                    boolean locked = VehicleControl.lockedOn();
                    int c = locked ? RED : AMBER;
                    int s = locked ? 10 : 14;
                    outline(ms, p[0] - s, p[1] - s, p[0] + s, p[1] + s, c);
                    if (locked) {
                        text(ms, mc, "LOCK", p[0] - 10, p[1] + s + 3, RED);
                    }
                }
            }
        }

        // left: flight data
        int x = 6, y = h - 100;
        AbstractGui.fill(ms, x - 3, y - 3, x + 128, y + 66, PANEL);
        text(ms, mc, I18n.get(a.spec.key()), x, y, WHITE);
        bar(ms, x, y + 11, 80, a.getHealth() / a.getMaxHealth());
        double kmh = a.getSpeed() * 72 * 3;
        int ground = mc.level.getHeight(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(a.getX()), MathHelper.floor(a.getZ()));
        int alt = (int) Math.max(0, a.getY() - ground);
        text(ms, mc, I18n.get("hud.militarytech.ias", (int) kmh), x, y + 22, GREEN);
        text(ms, mc, I18n.get("hud.militarytech.alt", alt, (int) a.getY()), x, y + 32, GREEN);
        text(ms, mc, I18n.get("hud.militarytech.throttle", (int) (a.getThrottle() * 100)), x, y + 42, GREEN);
        text(ms, mc, I18n.get(a.isGearDown() ? "hud.militarytech.gear_down" : "hud.militarytech.gear_up"), x, y + 52,
                a.isGearDown() ? AMBER : GREEN_DIM);

        // right: weapons
        int rx = w - 150, ry = h - 100;
        AbstractGui.fill(ms, rx - 3, ry - 3, w - 4, ry + 66, PANEL);
        text(ms, mc, I18n.get(a.getLoadout().key()), rx, ry, WHITE);
        if (!a.spec.weapons.isEmpty()) {
            text(ms, mc, I18n.get(a.spec.weapons.get(0).key()) + " " + a.getAmmo(0), rx, ry + 11, GREEN_DIM);
        }
        int line = ry + 22;
        for (Munition m : a.groups()) {
            boolean s = m == sel;
            text(ms, mc, (s ? "> " : "  ") + I18n.get(m.key()) + " " + a.countOf(m), rx, line, s ? GREEN : GREEN_DIM);
            line += 10;
        }

        // warnings
        boolean airborne = !a.isOnGround();
        if (airborne && a.getSpeed() < air.stallSpeed * 1.08) {
            centered(ms, mc, I18n.get("hud.militarytech.stall"), cx, cy - 46, RED);
        } else if (airborne && a.getDeltaMovement().y < -0.6 && alt < 40) {
            centered(ms, mc, I18n.get("hud.militarytech.pull_up"), cx, cy - 46, RED);
        }
        if (VehicleControl.missileWarning) {
            centered(ms, mc, I18n.get("hud.militarytech.missile"), cx, cy - 58, (now / 4) % 2 == 0 ? RED : AMBER);
        }
        if (a.isOnGround() && a.getSpeed() < 0.1) {
            centered(ms, mc, I18n.get("hud.militarytech.takeoff_hint"), cx, h - 70, 0xFFB0B0B0);
        }
    }

    // ------------------------------------------------------------------ prediction

    /** First block an unguided munition launched like this hits, or null. */
    @Nullable
    private static Vector3d predict(Minecraft mc, Munition m, Vector3d pos, Vector3d vel) {
        Vector3d p = pos;
        Vector3d v = vel;
        int max = Math.min(m.life, 500);
        for (int i = 0; i < max; i++) {
            Vector3d next = p.add(v);
            RayTraceResult r = mc.level.clip(new RayTraceContext(p, next, RayTraceContext.BlockMode.COLLIDER,
                    RayTraceContext.FluidMode.ANY, mc.player));
            if (r.getType() != RayTraceResult.Type.MISS) {
                return r.getLocation();
            }
            p = next;
            v = m.step(v);
            if (p.y < -64) {
                return null;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ drawing helpers

    private static void text(MatrixStack ms, Minecraft mc, String s, int x, int y, int color) {
        mc.font.drawShadow(ms, s, x, y, color);
    }

    private static void centered(MatrixStack ms, Minecraft mc, String s, int x, int y, int color) {
        FontRenderer f = mc.font;
        f.drawShadow(ms, s, x - f.width(s) / 2F, y, color);
    }

    private static void bar(MatrixStack ms, int x, int y, int width, float k) {
        k = MathHelper.clamp(k, 0F, 1F);
        AbstractGui.fill(ms, x, y, x + width, y + 5, 0xA0202020);
        int c = k > 0.6F ? 0xFF50D040 : k > 0.3F ? 0xFFE0B030 : 0xFFE04030;
        AbstractGui.fill(ms, x, y, x + (int) (width * k), y + 5, c);
    }

    private static void cross(MatrixStack ms, int x, int y, int r, int c) {
        AbstractGui.fill(ms, x - r, y, x - 1, y + 1, c);
        AbstractGui.fill(ms, x + 2, y, x + r + 1, y + 1, c);
        AbstractGui.fill(ms, x, y - r, x + 1, y - 1, c);
        AbstractGui.fill(ms, x, y + 2, x + 1, y + r + 1, c);
    }

    private static void outline(MatrixStack ms, int x1, int y1, int x2, int y2, int c) {
        AbstractGui.fill(ms, x1, y1, x2, y1 + 1, c);
        AbstractGui.fill(ms, x1, y2 - 1, x2, y2, c);
        AbstractGui.fill(ms, x1, y1, x1 + 1, y2, c);
        AbstractGui.fill(ms, x2 - 1, y1, x2, y2, c);
    }

    private static void circle(MatrixStack ms, int x, int y, int r, int c) {
        int steps = Math.max(12, r * 3);
        for (int i = 0; i < steps; i++) {
            double a = i * Math.PI * 2 / steps;
            int px = x + (int) Math.round(Math.cos(a) * r);
            int py = y + (int) Math.round(Math.sin(a) * r);
            AbstractGui.fill(ms, px, py, px + 1, py + 1, c);
        }
    }

    private static void diamond(MatrixStack ms, int x, int y, int r, int c) {
        line(ms, x - r, y, x, y - r, c);
        line(ms, x, y - r, x + r, y, c);
        line(ms, x + r, y, x, y + r, c);
        line(ms, x, y + r, x - r, y, c);
    }

    private static void line(MatrixStack ms, int x1, int y1, int x2, int y2, int c) {
        int n = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (n > 2000) {
            return;
        }
        for (int i = 0; i <= n; i++) {
            int px = x1 + (n == 0 ? 0 : (x2 - x1) * i / n);
            int py = y1 + (n == 0 ? 0 : (y2 - y1) * i / n);
            AbstractGui.fill(ms, px, py, px + 1, py + 1, c);
        }
    }

    private Hud() {
    }
}
