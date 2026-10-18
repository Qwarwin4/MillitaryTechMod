package com.qwarwin.militarytech.client;

import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.client.screen.TargetScreen;
import com.qwarwin.militarytech.entity.AircraftEntity;
import com.qwarwin.militarytech.entity.FireControl;
import com.qwarwin.militarytech.entity.Munition;
import com.qwarwin.militarytech.entity.OreshnikEntity;
import com.qwarwin.militarytech.entity.ProjectileEntity;
import com.qwarwin.militarytech.entity.VehicleEntity;
import com.qwarwin.militarytech.network.Network;
import com.qwarwin.militarytech.network.VehicleInputPacket;
import com.qwarwin.militarytech.registry.ModSounds;
import com.qwarwin.militarytech.util.Targeting;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * Everything the local player does inside a vehicle: reads keys and mouse, aims, locks targets, and sends
 * the result to the server once per tick.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = MilitaryTech.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VehicleControl {
    private static final VehicleEntity.Controls CONTROLS = new VehicleEntity.Controls();

    /** where the crosshair points in the world (null: sky) */
    @Nullable
    public static Vector3d aimPoint;
    /** air target under the seeker, and whether it's locked */
    public static int seekerTarget = -1;
    public static int seekerTicks;
    public static final int LOCK_TICKS = 16;
    public static boolean missileWarning;

    private static float ctlYaw, ctlPitch;
    private static int cycles;
    private static boolean action, loadout;
    @Nullable
    private static VehicleEntity last;

    public static VehicleEntity.Controls controlsFor(VehicleEntity v) {
        return CONTROLS;
    }

    @Nullable
    public static VehicleEntity current() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        Entity v = mc.player.getVehicle();
        // riders on the armour are just passengers: their own camera, hands and weapons
        return v instanceof VehicleEntity && !((VehicleEntity) v).isOutside(mc.player) ? (VehicleEntity) v : null;
    }

    public static boolean lockedOn() {
        return seekerTarget >= 0 && seekerTicks >= LOCK_TICKS;
    }

    /** Yaw/pitch the vehicle is being steered or aimed with (frozen while free-looking). */
    public static float controlYaw() {
        return ctlYaw;
    }

    public static float controlPitch() {
        return ctlPitch;
    }

    // ------------------------------------------------------------------ input

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInputUpdate(InputUpdateEvent event) {
        if (current() == null) {
            return;
        }
        // the body sits; WASD and jump go to the vehicle. Sneak stays, it's how you get out.
        MovementInput in = event.getMovementInput();
        in.forwardImpulse = 0F;
        in.leftImpulse = 0F;
        in.up = in.down = in.left = in.right = false;
        in.jumping = false;
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        KeyBinds.onRawKey(event.getKey(), event.getScanCode(), event.getAction());
    }

    @SubscribeEvent
    public static void onClickInput(InputEvent.ClickInputEvent event) {
        if (current() != null) {
            // no punching or placing blocks from inside a tank; LMB/RMB are triggers
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (current() == null || mc.screen != null) {
            return;
        }
        VehicleCamera.scroll(event.getScrollDelta());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            reset(mc);
            return;
        }
        VehicleEntity v = current();
        if (v != last) {
            onVehicleChanged(mc, last, v);
            last = v;
        }
        if (v == null) {
            KeyBinds.clearRaw();
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            readKeys(mc, v);
        } else {
            aim(mc, v);
            send(mc, v);
            warnings(mc, v);
        }
    }

    private static void onVehicleChanged(Minecraft mc, @Nullable VehicleEntity from, @Nullable VehicleEntity to) {
        seekerTarget = -1;
        seekerTicks = 0;
        aimPoint = null;
        cycles = 0;
        action = loadout = false;
        if (to != null && mc.player != null) {
            ctlYaw = mc.player.yRot;
            ctlPitch = mc.player.xRot;
        }
        VehicleCamera.onVehicleChanged(to);
    }

    private static void readKeys(Minecraft mc, VehicleEntity v) {
        GameSettings o = mc.options;
        boolean noScreen = mc.screen == null;
        for (int n = KeyBinds.presses(KeyBinds.CAMERA); n > 0; n--) {
            VehicleCamera.toggle(v);
        }
        while (o.keyTogglePerspective.consumeClick()) {
            VehicleCamera.toggle(v);
        }
        for (int n = KeyBinds.presses(KeyBinds.CYCLE); n > 0; n--) {
            if ((v instanceof FireControl || v instanceof OreshnikEntity) && v.isGunner(mc.player)) {
                mc.setScreen(new TargetScreen(v));
            } else {
                cycles++;
            }
        }
        for (int n = KeyBinds.presses(KeyBinds.ACTION); n > 0; n--) {
            if (v instanceof AircraftEntity) {
                AircraftEntity a = (AircraftEntity) v;
                if (a.isDriver(mc.player) && !a.isOnGround()) {
                    a.setFlag(VehicleEntity.FLAG_GEAR_UP, a.isGearDown());
                    mc.getSoundManager().play(SimpleSound.forUI(ModSounds.get("hydraulics"), 1.4F, 0.6F));
                }
            } else {
                action = true;
            }
        }
        for (int n = KeyBinds.presses(KeyBinds.LOADOUT); n > 0; n--) {
            loadout = true;
        }
        KeyBinds.clearRaw();

        CONTROLS.forward = noScreen ? (o.keyUp.isDown() ? 1F : 0F) - (o.keyDown.isDown() ? 1F : 0F) : 0F;
        CONTROLS.strafe = noScreen ? (o.keyLeft.isDown() ? 1F : 0F) - (o.keyRight.isDown() ? 1F : 0F) : 0F;
        CONTROLS.jump = noScreen && o.keyJump.isDown();
        CONTROLS.sprint = noScreen && o.keySprint.isDown();
        // looking around inside doesn't swing the turret
        boolean freeLook = VehicleCamera.isInside() || KeyBinds.FREE_LOOK.isDown() && VehicleCamera.isExternal();
        if (!freeLook) {
            ctlYaw = mc.player.yRot;
            ctlPitch = mc.player.xRot;
        }
        CONTROLS.yaw = ctlYaw;
        CONTROLS.pitch = ctlPitch;
    }

    // ------------------------------------------------------------------ aiming

    /** Point the crosshair is on: from the camera along the control direction, blocks and entities. */
    private static void aim(Minecraft mc, VehicleEntity v) {
        Vector3d from = VehicleCamera.position(mc, v, 1F);
        Vector3d dir = Vector3d.directionFromRotation(ctlPitch, ctlYaw);
        double range = v.spec.isAircraft() ? 1500 : 800;
        Vector3d to = from.add(dir.scale(range));
        RayTraceResult r = mc.level.clip(new RayTraceContext(from, to, RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE, mc.player));
        Vector3d end = r.getType() == RayTraceResult.Type.MISS ? to : r.getLocation();
        Targeting.Hit hit = Targeting.firstEntity(mc.level, v, from, end, 0.2,
                e -> e != mc.player && !v.hasPassenger(e) && Targeting.isShootable(e) && !(e instanceof ProjectileEntity));
        if (hit != null) {
            end = hit.pos;
        }
        aimPoint = r.getType() == RayTraceResult.Type.MISS && hit == null ? null : end;
        Vector3d aimFar = aimPoint != null ? aimPoint : to;

        if (!v.isGunner(mc.player)) {
            return;
        }
        if (v.spec.hasTurret) {
            float[] want;
            BlockPos target = v.getTarget();
            if (v instanceof FireControl && target != null) {
                want = ((FireControl) v).solution(target);
            } else {
                want = v.aimAt(aimFar);
            }
            if (want != null) {
                v.slewLocal(want[0], want[1]);
            }
        }
        if (v instanceof AircraftEntity) {
            seeker(mc, (AircraftEntity) v, from, dir);
        }
    }

    /** IR seeker: nearest air target within a cone around the crosshair, locked after holding it a moment. */
    private static void seeker(Minecraft mc, AircraftEntity a, Vector3d from, Vector3d dir) {
        Munition sel = a.selectedGroup();
        if (sel == null || sel.guidance != Munition.Guidance.IR || a.countOf(sel) <= 0) {
            seekerTarget = -1;
            seekerTicks = 0;
            return;
        }
        double range = sel == Munition.R40 ? 900 : 500;
        double cone = sel == Munition.R40 ? 10 : 16;
        Entity best = null;
        double bestAngle = cone;
        for (Entity e : mc.level.getEntities(a, new AxisAlignedBB(from, from).inflate(range),
                e -> e != mc.player && Targeting.isAirTarget(e) && !a.hasPassenger(e))) {
            Vector3d c = e.getBoundingBox().getCenter();
            if (c.distanceTo(from) > range) {
                continue;
            }
            double ang = Targeting.angle(dir, c.subtract(from));
            if (ang < bestAngle) {
                bestAngle = ang;
                best = e;
            }
        }
        int id = best == null ? -1 : best.getId();
        if (id != seekerTarget) {
            seekerTarget = id;
            seekerTicks = 0;
        } else if (id >= 0) {
            seekerTicks++;
        }
        int t = mc.player.tickCount;
        if (lockedOn()) {
            if (t % 4 == 0) {
                mc.getSoundManager().play(SimpleSound.forUI(ModSounds.get("lock_tone"), 1F, 0.5F));
            }
        } else if (seekerTarget >= 0 && t % 6 == 0) {
            mc.getSoundManager().play(SimpleSound.forUI(ModSounds.get("seeker_tone"), 0.8F + seekerTicks / (float) LOCK_TICKS * 0.4F, 0.4F));
        }
    }

    // ------------------------------------------------------------------ to the server

    private static void send(Minecraft mc, VehicleEntity v) {
        VehicleInputPacket m = VehicleInputPacket.of(v);
        boolean screen = mc.screen != null;
        if (v.isDriver(mc.player)) {
            boolean crashed = false;
            if (v instanceof AircraftEntity) {
                crashed = ((AircraftEntity) v).crashPending;
                ((AircraftEntity) v).crashPending = false;
            }
            m.driverState(v, v.driverFlags(), crashed);
        }
        if (v.isGunner(mc.player)) {
            if (v.spec.hasTurret) {
                m.turret(v.getTurretYaw(), v.getGunPitch());
            }
            m.fire1 = !screen && mc.options.keyAttack.isDown();
            m.fire2 = !screen && mc.options.keyUse.isDown();
            m.aim = aimPoint;
            m.lockId = lockedOn() ? seekerTarget : -1;
            m.cycles = cycles;
            m.action = action;
            m.loadout = loadout;
        }
        cycles = 0;
        action = loadout = false;
        Network.CHANNEL.sendToServer(m);
    }

    /** Somebody's missile coming our way. */
    private static void warnings(Minecraft mc, VehicleEntity v) {
        missileWarning = false;
        if (!(v instanceof AircraftEntity)) {
            return;
        }
        Vector3d me = v.position();
        for (Entity e : mc.level.getEntities(v, v.getBoundingBox().inflate(350), e -> e instanceof ProjectileEntity)) {
            ProjectileEntity p = (ProjectileEntity) e;
            if (!p.getMunition().isGuided() || p.getShooterId() == v.getId()) {
                continue;
            }
            Vector3d vel = p.getDeltaMovement();
            if (vel.lengthSqr() > 0.01 && Targeting.angle(vel, me.subtract(p.position())) < 25) {
                missileWarning = true;
                break;
            }
        }
        if (missileWarning && mc.player.tickCount % 8 == 0) {
            mc.getSoundManager().play(SimpleSound.forUI(ModSounds.get("missile_warning"), 1F, 0.7F));
        }
    }

    private static void reset(Minecraft mc) {
        if (last != null) {
            last = null;
            VehicleCamera.onVehicleChanged(null);
        }
        Sensitivity.restore(mc);
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        reset(Minecraft.getInstance());
    }

    private VehicleControl() {
    }
}
