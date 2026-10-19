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
            } else if (VehicleCamera.isSight() && v.seatView(mc.player, pt) != null) {
                periscope(ms, mc, v, w, h);
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
        if (v.isEmpDead() && (mc.level.getGameTime() / 6) % 2 == 0) {
            String s = I18n.get("msg.militarytech.emp");
            text(ms, mc, s, (w - mc.font.width(s)) / 2, h / 2 - 40, 0xFFFF4030);
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
        turretIndicator(ms, v, w - 222, h - 30, pt);
    }

    /** Looking out through a prism block or a firing port: a dark frame with a wide slot. */
    private static void periscope(MatrixStack ms, Minecraft mc, VehicleEntity v, int w, int h) {
        int sw = w * 3 / 4, sh = h * 2 / 5;
        int x0 = (w - sw) / 2, y0 = (h - sh) / 2 - h / 12, x1 = x0 + sw, y1 = y0 + sh;
        AbstractGui.fill(ms, 0, 0, w, y0, 0xE8080808);
        AbstractGui.fill(ms, 0, y1, w, h, 0xE8080808);
        AbstractGui.fill(ms, 0, y0, x0, y1, 0xE8080808);
        AbstractGui.fill(ms, x1, y0, w, y1, 0xE8080808);
        // the glass block: a slight green tint and a bevel
        AbstractGui.fill(ms, x0, y0, x1, y1, 0x1830FF60);
        AbstractGui.fill(ms, x0, y0, x1, y0 + 2, 0xFF2A2A2A);
        AbstractGui.fill(ms, x0, y1 - 2, x1, y1, 0xFF2A2A2A);
        AbstractGui.fill(ms, x0, y0, x0 + 2, y1, 0xFF2A2A2A);
        AbstractGui.fill(ms, x1 - 2, y0, x1, y1, 0xFF2A2A2A);
        text(ms, mc, I18n.get(v.seatKey(v.seatOf(mc.player))), x0 + 4, y1 + 4, GREEN_DIM);
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
        float pt = mc.getFrameTime();
        int seat = v.seatOf(mc.player);
        // ---- left: the vehicle itself
        int x0 = 8, x1 = 186, y1 = h - 8, y0 = y1 - 82;
        HudKit.panel(ms, mc, x0, y0, x1, y1, HudKit.ACCENT, I18n.get(v.spec.key()), I18n.get(v.seatKey(seat)));
        float hp = v.getHealth() / v.getMaxHealth();
        text(ms, mc, I18n.get("hud.militarytech.hull"), x0 + 5, y0 + 17, HudKit.TEXT_DIM);
        HudKit.segments(ms, x0 + 42, y0 + 17, 100, 7, 12, hp);
        text(ms, mc, (int) (hp * 100) + "%", x0 + 147, y0 + 17, hp > 0.3F ? HudKit.TEXT : HudKit.RED);
        double kmh = Math.abs(v.getSpeed()) * 20 * 3.6;
        if (!v.isDriver(mc.player)) {
            kmh = v.getDeltaMovement().length() * 72;
        }
        double max = v.spec.maxSpeed * 72;
        HudKit.dial(ms, x0 + 22, y0 + 58, 15, (float) (kmh / Math.max(1, max)), HudKit.ACCENT);
        String sp = String.valueOf((int) Math.round(kmh));
        mc.font.drawShadow(ms, sp, x0 + 22 - mc.font.width(sp) / 2F, y0 + 64, HudKit.TEXT);
        text(ms, mc, I18n.get("hud.militarytech.kmh_unit"), x0 + 40, y0 + 64, HudKit.TEXT_DIM);
        // crew: one pip per seat inside, lit if taken; then riders on the armour
        int inside = Math.min(v.spec.insideSeats, v.spec.seats.length);
        text(ms, mc, I18n.get("hud.militarytech.crew"), x0 + 64, y0 + 41, HudKit.TEXT_DIM);
        for (int i = 0; i < v.spec.seats.length; i++) {
            int px = x0 + 64 + (i % 12) * 9, py = y0 + 52 + (i / 12) * 9;
            boolean taken = v.inSeat(i) != null;
            int c = i == seat ? HudKit.AMBER : taken ? (i < inside ? HudKit.ACCENT : 0xFFB0B0A0) : 0x50808080;
            AbstractGui.fill(ms, px, py, px + 6, py + 6, c);
            if (i >= inside) {
                AbstractGui.fill(ms, px + 1, py + 1, px + 5, py + 5, taken ? 0xFF404038 : 0x30000000);
            }
        }
        if (v.usesFuel()) {
            float f = v.getFuel();
            text(ms, mc, I18n.get("hud.militarytech.fuel"), x0 + 5, y0 + 28, HudKit.TEXT_DIM);
            HudKit.segments(ms, x0 + 42, y0 + 28, 100, 7, 12, f);
            text(ms, mc, (int) (f * 100) + "%", x0 + 147, y0 + 28, f > 0.15F ? HudKit.TEXT : HudKit.RED);
        }
        if (v.loadingTicks() > 0) {
            String ld = I18n.get("hud.militarytech.loading", (v.loadingTicks() + 19) / 20);
            text(ms, mc, ld, x1 - 4 - mc.font.width(ld), y0 + 41, HudKit.AMBER);
        }
        String status = v.outOfFuel() ? I18n.get("hud.militarytech.no_fuel") : v.isEmpDead() ? I18n.get("hud.militarytech.engine_dead")
                : v.spec.doors.isEmpty() ? I18n.get("hud.militarytech.seat_hint") : I18n.get("hud.militarytech.door_hint");
        text(ms, mc, status, x0 + 64, y0 + 70, v.isEmpDead() || v.outOfFuel() ? HudKit.RED : 0xFF707A70);

        // ---- top: heading tape for whoever looks out
        HudKit.compass(ms, mc, w / 2, 16, 220, mc.player.getViewYRot(pt), HudKit.ACCENT_DIM);

        if (!gunner || v.spec.weapons.isEmpty()) {
            return;
        }
        // ---- right: weapons, each ammunition with its own icon and count
        int rows = Math.min(2, v.spec.weapons.size());
        int bx1 = w - 8, bx0 = bx1 - 200, by1 = h - 8, by0 = by1 - (26 + rows * 22);
        HudKit.panel(ms, mc, bx0, by0, bx1, by1, HudKit.ACCENT, I18n.get("hud.militarytech.weapons"), null);
        int line = by0 + 16;
        for (int wi = 0; wi < rows; wi++) {
            WeaponSpec ws = v.spec.weapons.get(wi);
            int base = v.spec.ammoBase(wi);
            String key = I18n.get(ws.secondary ? "hud.militarytech.rmb" : "hud.militarytech.lmb");
            text(ms, mc, key, bx0 + 4, line + 5, HudKit.TEXT_DIM);
            int cx = bx0 + 34;
            for (int o = 0; o < ws.ammo.length; o++) {
                boolean sel = wi != 0 || v.getSelected() == o;
                int left = v.getAmmo(base + o), cap = Math.max(1, v.capacity(base + o));
                int chip = ws.ammo.length > 1 ? 52 : 150;
                if (sel && ws.ammo.length > 1) {
                    AbstractGui.fill(ms, cx - 2, line - 1, cx + chip - 2, line + 18, 0x307CFF6A);
                    AbstractGui.fill(ms, cx - 2, line + 17, cx + chip - 2, line + 18, HudKit.ACCENT);
                }
                HudKit.munitionIcon(mc, ws.ammo[o], cx, line, 1F);
                int c = left == 0 ? HudKit.RED : left < cap / 4 ? HudKit.AMBER : sel ? HudKit.TEXT : HudKit.TEXT_DIM;
                String n = ws.ammo.length > 1 ? String.valueOf(left) : I18n.get(ws.ammo[o].key()) + "  " + left + "/" + cap;
                text(ms, mc, n, cx + 18, line + 5, c);
                cx += chip;
            }
            line += 22;
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
            float k = (now - reloadStart + pt) / (float) reloadLength;
            text(ms, mc, I18n.get("hud.militarytech.reloading"), bx0 + 4, by1 - 11, HudKit.AMBER);
            HudKit.bar(ms, bx0 + 70, by1 - 8, bx1 - bx0 - 76, k, HudKit.AMBER);
        } else if (main.interval >= 20) {
            text(ms, mc, I18n.get(ammo > 0 ? "hud.militarytech.ready" : "hud.militarytech.empty_gun"), bx0 + 4, by1 - 11,
                    ammo > 0 ? HudKit.ACCENT : HudKit.RED);
            if (main.ammo.length > 1) {
                String hint = I18n.get("hud.militarytech.switch_hint");
                text(ms, mc, hint, bx1 - 4 - mc.font.width(hint), by1 - 11, 0xFF707A70);
            }
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
        // which warhead is picked, and how many of each are on board
        text(ms, mc, I18n.get(sel == 1 ? "hud.militarytech.warhead_nuke" : "hud.militarytech.warhead_conventional")
                + "  [" + o.getAmmo(0) + " / " + o.getAmmo(1) + "]", x, y + 44, sel == 1 ? 0xFFFFD040 : 0xFFB0B0B0);
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

        int ground = mc.level.getHeight(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(a.getX()), MathHelper.floor(a.getZ()));
        int alt = (int) Math.max(0, a.getY() - ground);
        double kmh = a.getSpeed() * 72 * 3;
        if (VehicleCamera.isSight()) {
            // in the cockpit: everything on the HUD glass in front of the pilot
            glassHud(ms, mc, a, w, h, pt, kmh, alt, sel);
        } else {
            flightPanels(ms, mc, a, w, h, pt, kmh, alt, sel);
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

    private static void flightPanels(MatrixStack ms, Minecraft mc, AircraftEntity a, int w, int h, float pt, double kmh, int alt,
                                     @Nullable Munition sel) {
        AirSpec air = a.air();
        // left: flight data
        int x0 = 8, x1 = 176, y1 = h - 8, y0 = y1 - 74;
        HudKit.panel(ms, mc, x0, y0, x1, y1, HudKit.ACCENT, I18n.get(a.spec.key()), I18n.get(a.seatKey(a.seatOf(mc.player))));
        float hp = a.getHealth() / a.getMaxHealth();
        HudKit.segments(ms, x0 + 5, y0 + 16, 120, 6, 12, hp);
        text(ms, mc, (int) (hp * 100) + "%", x0 + 130, y0 + 15, hp > 0.3F ? HudKit.TEXT : HudKit.RED);
        HudKit.dial(ms, x0 + 22, y0 + 46, 16, (float) (kmh / (air.maxSpeed * 72 * 3 * 1.1)),
                a.getSpeed() < air.stallSpeed * 1.08 && !a.isOnGround() ? HudKit.RED : HudKit.ACCENT);
        String sp = String.valueOf((int) kmh);
        mc.font.drawShadow(ms, sp, x0 + 22 - mc.font.width(sp) / 2F, y0 + 54, HudKit.TEXT);
        text(ms, mc, I18n.get("hud.militarytech.alt_short", alt), x0 + 46, y0 + 26, HudKit.TEXT);
        double vs = a.getDeltaMovement().y * 20;
        text(ms, mc, I18n.get("hud.militarytech.vs", String.format("%+.0f", vs)), x0 + 46, y0 + 37,
                vs < -15 && alt < 60 ? HudKit.RED : HudKit.TEXT_DIM);
        // throttle as a vertical bar on the right edge
        float thr = a.getThrottle();
        AbstractGui.fill(ms, x1 - 14, y0 + 26, x1 - 8, y1 - 6, 0x60202820);
        int top = (int) (y1 - 6 - (y1 - 6 - (y0 + 26)) * thr);
        AbstractGui.fill(ms, x1 - 14, top, x1 - 8, y1 - 6, thr > 0.95F ? HudKit.AMBER : HudKit.ACCENT);
        text(ms, mc, I18n.get("hud.militarytech.thr_short", (int) (thr * 100)), x0 + 46, y0 + 48, HudKit.TEXT_DIM);
        if (a.usesFuel()) {
            float f = a.getFuel();
            AbstractGui.fill(ms, x1 - 24, y0 + 26, x1 - 18, y1 - 6, 0x60202820);
            int ft = (int) (y1 - 6 - (y1 - 6 - (y0 + 26)) * f);
            AbstractGui.fill(ms, x1 - 24, ft, x1 - 18, y1 - 6, f > 0.2F ? 0xFFD0A040 : HudKit.RED);
        }
        if (a.loadingTicks() > 0) {
            text(ms, mc, I18n.get("hud.militarytech.loading", (a.loadingTicks() + 19) / 20), x0 + 2, y0 - 12, HudKit.AMBER);
        }
        boolean down = a.isGearDown();
        AbstractGui.fill(ms, x0 + 46, y0 + 60, x0 + 52, y0 + 66, down ? HudKit.ACCENT : 0x50808080);
        text(ms, mc, I18n.get(down ? "hud.militarytech.gear_down" : "hud.militarytech.gear_up"), x0 + 56, y0 + 59,
                down ? HudKit.TEXT : HudKit.TEXT_DIM);
        HudKit.compass(ms, mc, w / 2, 16, 240, a.yRot, HudKit.ACCENT_DIM);

        // right: stores, each kind with its icon
        java.util.List<Munition> groups = a.groups();
        int rows = groups.size() + (a.spec.weapons.isEmpty() ? 0 : 1);
        int bx1 = w - 8, bx0 = bx1 - 190, by1 = h - 8, by0 = by1 - (18 + rows * 19);
        HudKit.panel(ms, mc, bx0, by0, bx1, by1, HudKit.ACCENT, I18n.get(a.loadoutKey()), null);
        int line = by0 + 15;
        if (!a.spec.weapons.isEmpty()) {
            WeaponSpec gun = a.spec.weapons.get(0);
            HudKit.munitionIcon(mc, gun.ammo[0], bx0 + 6, line, 1F);
            text(ms, mc, I18n.get("hud.militarytech.lmb") + " " + I18n.get(gun.key()) + "  " + a.getAmmo(0), bx0 + 26, line + 5,
                    a.getAmmo(0) > 0 ? HudKit.TEXT : HudKit.RED);
            line += 19;
        }
        for (Munition m : groups) {
            boolean on = m == sel;
            if (on) {
                AbstractGui.fill(ms, bx0 + 2, line - 1, bx1 - 2, line + 17, 0x307CFF6A);
                AbstractGui.fill(ms, bx0 + 2, line - 1, bx0 + 4, line + 17, HudKit.ACCENT);
            }
            HudKit.munitionIcon(mc, m, bx0 + 6, line, 1F);
            int n = a.countOf(m);
            text(ms, mc, (on ? I18n.get("hud.militarytech.rmb") + " " : "") + I18n.get(m.key()) + "  " + n, bx0 + 26, line + 5,
                    n == 0 ? HudKit.RED : on ? HudKit.TEXT : HudKit.TEXT_DIM);
            line += 19;
        }
    }

    /**
     * The head-up display as the pilot sees it: green symbology on the combiner glass ahead of the cockpit, clipped to
     * the glass, moving with the aircraft: pitch ladder and horizon, flight path marker, heading, speed and altitude,
     * the selected weapon.
     */
    private static void glassHud(MatrixStack ms, Minecraft mc, AircraftEntity a, int w, int h, float pt, double kmh, int alt,
                                 @Nullable Munition sel) {
        AirSpec air = a.air();
        int g = 0xE860FF80, gd = 0x9060FF80;
        // the glass: a frame in the aircraft, projected to the screen
        Vector3d c = air.hudGlass != null ? air.hudGlass : air.cockpit.add(0, -0.08, 0.7);
        double hw = air.hudHalfWidth, hh = air.hudHalfHeight;
        Vector3d base = new Vector3d(MathHelper.lerp(pt, a.xo, a.getX()), MathHelper.lerp(pt, a.yo, a.getY()),
                MathHelper.lerp(pt, a.zo, a.getZ())).subtract(a.position());
        int[] tl = Projection.toScreen(a.toWorld(c.add(hw, hh, 0)).add(base), w, h);
        int[] br = Projection.toScreen(a.toWorld(c.add(-hw, -hh, 0)).add(base), w, h);
        int x0, y0, x1, y1;
        if (tl != null && br != null) {
            x0 = Math.min(tl[0], br[0]);
            x1 = Math.max(tl[0], br[0]);
            y0 = Math.min(tl[1], br[1]);
            y1 = Math.max(tl[1], br[1]);
        } else {
            x0 = w / 2 - h / 3;
            x1 = w / 2 + h / 3;
            y0 = h / 6;
            y1 = h * 5 / 6;
        }
        // keep it a sensible size whatever the zoom
        x0 = Math.max(4, x0);
        y0 = Math.max(4, y0);
        x1 = Math.min(w - 4, x1);
        y1 = Math.min(h - 4, y1);
        if (x1 - x0 < 60 || y1 - y0 < 50) {
            return;
        }
        AbstractGui.fill(ms, x0, y0, x1, y1, 0x1418FF60);
        HudKit.line(ms, x0, y0, x1, y0, 0x60303830);
        HudKit.line(ms, x0, y1, x1, y1, 0x60303830);
        HudKit.clip(mc, x0, y0, x1, y1);
        int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
        // pitch ladder: lines every 5 degrees, conformal (projected from the real horizon)
        Vector3d eye = mc.gameRenderer.getMainCamera().getPosition();
        float yaw = a.yRot;
        for (int p = -30; p <= 30; p += 5) {
            Vector3d dir = Vector3d.directionFromRotation(-p, yaw);
            Vector3d side = Vector3d.directionFromRotation(0, yaw + 90).scale(p == 0 ? 0.28 : 0.12);
            Vector3d mid = eye.add(dir.scale(100));
            int[] l = Projection.toScreen(mid.add(side.scale(100)), w, h);
            int[] r = Projection.toScreen(mid.subtract(side.scale(100)), w, h);
            int[] l2 = Projection.toScreen(mid.add(side.scale(25)), w, h);
            int[] r2 = Projection.toScreen(mid.subtract(side.scale(25)), w, h);
            if (l == null || r == null || l2 == null || r2 == null) {
                continue;
            }
            int col = p == 0 ? g : gd;
            if (p == 0) {
                HudKit.line(ms, l[0], l[1], r[0], r[1], col);
            } else {
                HudKit.line(ms, l[0], l[1], l2[0], l2[1], col);
                HudKit.line(ms, r2[0], r2[1], r[0], r[1], col);
                text(ms, mc, String.valueOf(Math.abs(p)), l[0] - 12, l[1] - 4, col);
            }
        }
        // flight path marker: where the aircraft is actually going
        Vector3d vel = a.getDeltaMovement();
        if (vel.lengthSqr() > 0.01) {
            int[] f = Projection.toScreen(eye.add(vel.normalize().scale(200)), w, h);
            if (f != null) {
                circle(ms, f[0], f[1], 4, g);
                AbstractGui.fill(ms, f[0] - 9, f[1], f[0] - 4, f[1] + 1, g);
                AbstractGui.fill(ms, f[0] + 5, f[1], f[0] + 10, f[1] + 1, g);
                AbstractGui.fill(ms, f[0], f[1] - 8, f[0] + 1, f[1] - 4, g);
            }
        }
        // boresight
        int[] n = Projection.toScreen(eye.add(a.nose().scale(300)), w, h);
        if (n != null) {
            HudKit.line(ms, n[0] - 6, n[1], n[0] - 2, n[1], g);
            HudKit.line(ms, n[0] + 2, n[1], n[0] + 6, n[1], g);
            HudKit.line(ms, n[0], n[1] - 5, n[0], n[1] - 2, g);
        }
        // speed box left, altitude box right, heading along the top
        int boxY = cy - 5;
        outline(ms, x0 + 6, boxY - 3, x0 + 44, boxY + 10, g);
        text(ms, mc, String.valueOf((int) kmh), x0 + 9, boxY, g);
        outline(ms, x1 - 44, boxY - 3, x1 - 6, boxY + 10, g);
        String as = String.valueOf(alt);
        text(ms, mc, as, x1 - 9 - mc.font.width(as), boxY, g);
        text(ms, mc, I18n.get("hud.militarytech.hud_m"), x1 - 30, boxY + 13, gd);
        float heading = MathHelper.wrapDegrees(a.yRot + 180F);
        if (heading < 0) {
            heading += 360;
        }
        for (int d = (int) heading - 25; d <= heading + 25; d++) {
            if (Math.floorMod(d, 5) != 0) {
                continue;
            }
            int px = cx + Math.round((d - heading) * 2.4F);
            int len = Math.floorMod(d, 10) == 0 ? 4 : 2;
            AbstractGui.fill(ms, px, y0 + 6, px + 1, y0 + 6 + len, g);
            if (Math.floorMod(d, 10) == 0) {
                String t = String.format("%02d", Math.floorMod(d, 360) / 10);
                mc.font.draw(ms, t, px - mc.font.width(t) / 2F, y0 + 12, gd);
            }
        }
        AbstractGui.fill(ms, cx, y0 + 2, cx + 1, y0 + 6, g);
        // weapon and mode at the bottom, G-load and throttle
        String wpn = sel == null ? (a.spec.weapons.isEmpty() ? "" : I18n.get(a.spec.weapons.get(0).key()) + " " + a.getAmmo(0))
                : I18n.get(sel.key()) + " " + a.countOf(sel);
        text(ms, mc, wpn, x0 + 8, y1 - 12, g);
        String thr = (int) (a.getThrottle() * 100) + "%";
        text(ms, mc, thr, x1 - 8 - mc.font.width(thr), y1 - 12, gd);
        if (a.isGearDown()) {
            text(ms, mc, I18n.get("hud.militarytech.hud_gear"), x0 + 8, y1 - 24, g);
        }
        HudKit.unclip();
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
