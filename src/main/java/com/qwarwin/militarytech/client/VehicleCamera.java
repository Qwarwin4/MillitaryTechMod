package com.qwarwin.militarytech.client;

import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.entity.AircraftEntity;
import com.qwarwin.militarytech.entity.CameraEntity;
import com.qwarwin.militarytech.entity.VehicleEntity;
import com.qwarwin.militarytech.entity.VehicleSpec;
import com.qwarwin.militarytech.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * Camera while in a vehicle: outside, orbiting with the mouse (War Thunder style); at the sight / in the cockpit
 * with zoom; or inside the crew compartment, looking around at the interior. The view is carried by a client-only
 * {@link CameraEntity} placed every frame.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = MilitaryTech.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VehicleCamera {
    private static final int CAMERA_ID = -77_000_123;
    private static final int OUTSIDE = 0, SIGHT = 1, INSIDE = 2;
    private static int mode = OUTSIDE;
    private static float zoom = 1F;
    private static float distance = 1F;
    @Nullable
    private static CameraEntity cam;
    @Nullable
    private static PointOfView savedView;

    public static boolean isExternal() {
        return mode == OUTSIDE;
    }

    public static boolean isSight() {
        return mode == SIGHT;
    }

    public static boolean isInside() {
        return mode == INSIDE;
    }

    public static float zoom() {
        return mode == SIGHT ? zoom : 1F;
    }

    /** C / F5: outside -> sight -> inside (when the vehicle has an interior) -> outside. */
    static void toggle(VehicleEntity v) {
        boolean inside = v.spec.interior && !(v instanceof AircraftEntity);
        mode = mode == OUTSIDE ? SIGHT : mode == SIGHT && inside ? INSIDE : OUTSIDE;
        zoom = 1F;
    }

    static void scroll(double delta) {
        if (mode == SIGHT) {
            zoom = MathHelper.clamp(zoom * (delta > 0 ? 1.3F : 1F / 1.3F), 1F, 10F);
        } else {
            distance = MathHelper.clamp(distance * (delta > 0 ? 0.88F : 1F / 0.88F), 0.45F, 2.5F);
        }
    }

    static void onVehicleChanged(@Nullable VehicleEntity v) {
        Minecraft mc = Minecraft.getInstance();
        if (v == null) {
            mode = OUTSIDE;
            zoom = 1F;
            if (mc.player != null && mc.getCameraEntity() != mc.player) {
                mc.setCameraEntity(mc.player);
            }
            if (cam != null && cam.level instanceof ClientWorld) {
                ((ClientWorld) cam.level).removeEntity(CAMERA_ID);
            }
            cam = null;
            if (savedView != null) {
                mc.options.setCameraType(savedView);
                savedView = null;
            }
            Sensitivity.restore(mc);
        } else if (savedView == null) {
            savedView = mc.options.getCameraType();
            // aircraft start outside, ground vehicles too: it's the fun view
            mode = OUTSIDE;
        }
    }

    /** Where the camera is for this vehicle at {@code partial}. */
    public static Vector3d position(Minecraft mc, VehicleEntity v, float partial) {
        Vector3d base = new Vector3d(MathHelper.lerp(partial, v.xo, v.getX()), MathHelper.lerp(partial, v.yo, v.getY()),
                MathHelper.lerp(partial, v.zo, v.getZ()));
        if (mode == INSIDE) {
            return v.toWorld(insideEye(v, mc.player)).subtract(v.position()).add(base);
        }
        if (mode == SIGHT) {
            if (v instanceof AircraftEntity) {
                return v.toWorld(((AircraftEntity) v).air().cockpit).subtract(v.position()).add(base);
            }
            if (v.spec.hasTurret && v.isGunner(mc.player)) {
                return v.sightWorld(partial).subtract(v.position()).add(base);
            }
            return v.toWorld(v.spec.driverEye).subtract(v.position()).add(base);
        }
        double dist, height;
        if (v instanceof AircraftEntity) {
            dist = ((AircraftEntity) v).air().camDistance;
            height = ((AircraftEntity) v).air().camHeight;
        } else {
            dist = v.spec.camDistance;
            height = v.spec.camHeight;
        }
        dist *= distance;
        Vector3d center = base.add(0, v.spec.height * 0.5 + height * 0.5, 0);
        float yaw = mc.player.getViewYRot(partial);
        float pitch = mc.player.getViewXRot(partial);
        Vector3d back = Vector3d.directionFromRotation(pitch, yaw).scale(-dist).add(0, height * 0.5, 0);
        Vector3d want = center.add(back);
        RayTraceResult r = mc.level.clip(new RayTraceContext(center, want, RayTraceContext.BlockMode.VISUAL,
                RayTraceContext.FluidMode.NONE, mc.player));
        if (r.getType() != RayTraceResult.Type.MISS) {
            Vector3d hit = r.getLocation();
            want = hit.add(center.subtract(hit).normalize().scale(0.4));
        }
        return want;
    }

    /** Eye of a crew member sitting in their seat; truck crews look out of the cab beside the driver. */
    private static Vector3d insideEye(VehicleEntity v, Entity who) {
        int i = Math.max(0, v.getPassengers().indexOf(who));
        Vector3d seat = v.spec.seats[Math.min(i, v.spec.seats.length - 1)];
        if (v.spec.category == VehicleSpec.Category.TRUCK) {
            return new Vector3d(seat.x, v.spec.driverEye.y, v.spec.driverEye.z);
        }
        return seat.add(0, 0.72, 0);
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        VehicleEntity v = VehicleControl.current();
        if (v == null || mc.level == null || mc.player == null) {
            return;
        }
        if (cam == null || cam.level != mc.level) {
            cam = new CameraEntity(ModEntities.CAMERA.get(), mc.level);
            cam.setId(CAMERA_ID);
            mc.level.putNonPlayerEntity(CAMERA_ID, cam);
        }
        float pt = event.renderTickTime;
        Vector3d p = position(mc, v, pt);
        cam.setPos(p.x, p.y, p.z);
        cam.xo = cam.xOld = p.x;
        cam.yo = cam.yOld = p.y;
        cam.zo = cam.zOld = p.z;
        cam.yRot = cam.yRotO = mc.player.getViewYRot(pt);
        cam.xRot = cam.xRotO = mc.player.getViewXRot(pt);
        if (mc.getCameraEntity() != cam) {
            mc.setCameraEntity(cam);
        }
        if (mc.options.getCameraType() != PointOfView.FIRST_PERSON) {
            mc.options.setCameraType(PointOfView.FIRST_PERSON);
        }
        Sensitivity.update(mc, zoom());
    }

    @SubscribeEvent
    public static void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        VehicleEntity v = VehicleControl.current();
        if (mode == SIGHT && v instanceof AircraftEntity) {
            float pt = (float) event.getRenderPartialTicks();
            event.setRoll(MathHelper.lerp(pt, v.rollO, v.getRoll()));
        }
    }

    @SubscribeEvent
    public static void onFov(EntityViewRenderEvent.FOVModifier event) {
        if (VehicleControl.current() != null) {
            event.setFOV(event.getFOV() / zoom());
        }
        Projection.fov = event.getFOV();
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Entity veh = event.getPlayer().getVehicle();
        if (veh instanceof VehicleEntity && ((VehicleEntity) veh).spec.enclosed && !((VehicleEntity) veh).isOutside(event.getPlayer())) {
            // the crew sits inside
            event.setCanceled(true);
        } else if (veh instanceof AircraftEntity && mode == SIGHT && event.getPlayer() == Minecraft.getInstance().player) {
            event.setCanceled(true);
        } else if (veh instanceof VehicleEntity && ((VehicleEntity) veh).spec.riderScale != 1F) {
            // pilots sit in the cockpit, scaled to fit under the canopy
            float k = ((VehicleEntity) veh).spec.riderScale;
            event.getMatrixStack().pushPose();
            event.getMatrixStack().scale(k, k, k);
            scaledRider = event.getPlayer();
        }
    }

    @Nullable
    private static Entity scaledRider;

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (scaledRider == event.getPlayer()) {
            event.getMatrixStack().popPose();
            scaledRider = null;
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (VehicleControl.current() != null) {
            event.setCanceled(true);
        }
    }

    /** Hide the bits of our own vehicle the camera sits inside. */
    public static boolean hideTurret(VehicleEntity v) {
        Minecraft mc = Minecraft.getInstance();
        return mode == SIGHT && v.hasPassenger(mc.player) && v.spec.hasTurret && v.isGunner(mc.player);
    }

    public static boolean hideWhole(VehicleEntity v) {
        Minecraft mc = Minecraft.getInstance();
        return mode == SIGHT && v instanceof AircraftEntity && v.hasPassenger(mc.player);
    }

    /** We're inside this vehicle looking at its interior. */
    public static boolean showInterior(VehicleEntity v) {
        Minecraft mc = Minecraft.getInstance();
        return mode == INSIDE && v.spec.interior && v.hasPassenger(mc.player);
    }

    private VehicleCamera() {
    }
}
