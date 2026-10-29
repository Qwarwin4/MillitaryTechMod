package com.qwarwin.militarytech.entity;

import com.qwarwin.militarytech.network.VehicleInputPacket;
import com.qwarwin.militarytech.registry.ModSounds;
import com.qwarwin.militarytech.util.Targeting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pantsir-S1: a KamAZ that drives like the other trucks, with a combat module of 12 x 57E6 missiles and twin 2A38M.
 * By hand the operator aims it like a turret (LMB guns, RMB a missile at whatever is nearest the crosshair). G
 * switches the automatic mode: the search radar finds aircraft, drones and incoming missiles out to 1800 blocks,
 * the module turns on its own, missiles go out at anything beyond gun range, one per target until it's down, and
 * the guns take what comes close. It never fires at its owner, the owner's team or their missiles.
 */
public class PantsirEntity extends TruckEntity {
    public static final double MISSILE_RANGE = 1800.0;
    private static final double MISSILE_MIN = 40.0;
    private static final double GUN_RANGE = 260.0;
    private static final int W_GUNS = 0, W_SAM = 1;

    private int gunTargetId = -1, samTargetId = -1;
    private int trackTicks;
    private float errYaw, errPitch;
    private int scan;
    /** target id -> tick a missile last went out at it */
    private final Map<Integer, Long> launched = new HashMap<>();
    private final List<Entity> contacts = new ArrayList<>();

    public PantsirEntity(EntityType<?> type, World world) {
        super(type, world, Specs.PANTSIR);
    }

    public boolean isAuto() {
        return hasFlag(FLAG_ACTIVE);
    }

    /** In automatic mode the module is the radar's, not the operator's. */
    @Nullable
    @Override
    public Entity getGunner() {
        return isAuto() ? null : super.getGunner();
    }

    @Override
    public void applyInput(ServerPlayerEntity player, VehicleInputPacket m) {
        boolean crew = seatOf(player) == 0 || seatOf(player) == 1;
        boolean wasAuto = isAuto();
        boolean gunner = isGunner(player);
        super.applyInput(player, m);
        // G from the operator's or the driver's seat switches automatic mode (the gunner's own G already went
        // through action())
        if (m.action && crew && (wasAuto || !gunner)) {
            toggleAuto(player);
        }
    }

    @Override
    protected void action(ServerPlayerEntity player) {
        toggleAuto(player);
    }

    private void toggleAuto(PlayerEntity player) {
        boolean on = !isAuto();
        setFlag(FLAG_ACTIVE, on);
        if (owner == null) {
            owner = player.getUUID();
        }
        gunTargetId = samTargetId = -1;
        launched.clear();
        trigger1 = trigger2 = false;
        for (Entity p : getPassengers()) {
            if (p instanceof PlayerEntity) {
                ((PlayerEntity) p).displayClientMessage(new TranslationTextComponent(on ? "msg.militarytech.pantsir_auto_on"
                        : "msg.militarytech.pantsir_auto_off").withStyle(on ? TextFormatting.RED : TextFormatting.GRAY), true);
            }
        }
        level.playSound(null, getX(), getY(), getZ(), ModSounds.get("lock_tone"), SoundCategory.NEUTRAL, 2F, on ? 1.2F : 0.6F);
    }

    // ------------------------------------------------------------------ manual: a missile goes for what's nearest the sight

    @Override
    protected void prepareProjectile(ProjectileEntity pe, Munition m) {
        if (m.guidance != Munition.Guidance.IR) {
            return;
        }
        Entity t = null;
        if (isAuto()) {
            t = samTargetId < 0 ? null : level.getEntity(samTargetId);
        } else {
            if (lockId >= 0) {
                t = level.getEntity(lockId);
            }
            if (t == null || !canEngage(t)) {
                t = inSight();
            }
        }
        if (t != null && random.nextDouble() < Targeting.lockChance(t)) {
            pe.setTargetEntity(t);
        }
    }

    /** The air target closest to the gun line, within a few degrees of it. */
    @Nullable
    private Entity inSight() {
        Vector3d from = turretToWorld(spec.trunnion, getTurretYaw());
        Vector3d dir = Vector3d.directionFromRotation(getGunPitch(), getTurretYaw());
        Entity best = null;
        double bestA = 6.0;
        for (Entity e : candidates(from)) {
            Vector3d to = e.getBoundingBox().getCenter().subtract(from);
            if (to.length() > MISSILE_RANGE) {
                continue;
            }
            double a = Targeting.angle(dir, to);
            if (a < bestA && hostile(e)) {
                bestA = a;
                best = e;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ automatic mode

    @Override
    protected void serverWeapons() {
        if (!isAuto()) {
            super.serverWeapons();
            return;
        }
        for (int i = 0; i < cooldown.length; i++) {
            if (cooldown[i] > 0) {
                cooldown[i]--;
            }
        }
        if (hasFlag(FLAG_EMP)) {
            // the pulse took the radars down
            return;
        }
        Vector3d eye = pivotWorld().add(0, 2.0, 0);
        long now = level.getGameTime();
        if (--scan <= 0 || contacts.isEmpty() && tickCount % 5 == 0) {
            scan = 10;
            contacts.clear();
            for (Entity e : candidates(eye)) {
                if (valid(e, eye) && random.nextDouble() < Targeting.scanChance(e)) {
                    contacts.add(e);
                }
            }
            // missiles first, then aircraft, then drones; nearer first
            contacts.sort((a, b) -> Double.compare(score(a, eye), score(b, eye)));
            launched.entrySet().removeIf(en -> now - en.getValue() > 400);
        }
        contacts.removeIf(e -> !e.isAlive());

        Entity gunT = null, samT = null;
        for (Entity e : contacts) {
            double d = e.position().distanceTo(eye);
            if (gunT == null && d < GUN_RANGE && getAmmo(spec.ammoBase(W_GUNS)) > 0) {
                gunT = e;
            }
            if (samT == null && d > MISSILE_MIN && getAmmo(spec.ammoBase(W_SAM)) > 0 && !missileOnTheWay(e, d, now)) {
                samT = e;
            }
        }
        int gid = gunT == null ? -1 : gunT.getId();
        if (gid != gunTargetId) {
            trackTicks = 0;
        }
        gunTargetId = gid;
        samTargetId = samT == null ? -1 : samT.getId();

        // the module turns to the gun target if there is one, else to the next missile target
        Entity lay = gunT != null ? gunT : samT;
        if (lay == null) {
            return;
        }
        trackTicks++;
        Vector3d muzzle = turretToWorld(spec.trunnion, getTurretYaw());
        Vector3d aim;
        if (lay == gunT) {
            aim = Targeting.lead(muzzle, gunT, Munition.AA_30.speed).subtract(muzzle);
            double size = Math.max(0.3, gunT.getBbWidth());
            float wander = (float) (1.4 / size) * (trackTicks < 30 ? 1F : 0.6F);
            errYaw = MathHelper.clamp(errYaw * 0.9F + (random.nextFloat() - 0.5F) * wander, -2.5F * wander, 2.5F * wander);
            errPitch = MathHelper.clamp(errPitch * 0.9F + (random.nextFloat() - 0.5F) * wander, -2.5F * wander, 2.5F * wander);
        } else {
            // missiles are launched up and out towards the target, they find it themselves
            aim = lay.getBoundingBox().getCenter().subtract(muzzle);
            errYaw = errPitch = 0F;
        }
        float yaw = Targeting.yawTo(aim) + errYaw, pitch = Targeting.pitchTo(aim) + errPitch;
        if (lay == samT) {
            pitch = Math.min(pitch, -12F);
        }
        float err = slewServer(yaw, pitch);
        if (gunT != null && cooldown[W_GUNS] <= 0 && trackTicks > 8 && err < 3F) {
            fire(W_GUNS, null);
        }
        if (samT != null && cooldown[W_SAM] <= 0) {
            // the tubes don't need to be dead on, but roughly pointing that way
            Vector3d dir = Vector3d.directionFromRotation(getGunPitch(), getTurretYaw());
            Vector3d to = samT.getBoundingBox().getCenter().subtract(muzzle);
            if (Targeting.angle(dir, to) < 40 && (lay == samT ? err < 10F : true)) {
                if (fire(W_SAM, null)) {
                    launched.put(samT.getId(), now);
                    level.playSound(null, getX(), getY(), getZ(), ModSounds.get("lock_tone"), SoundCategory.HOSTILE, 3F, 0.8F);
                }
            }
        }
    }

    /** A missile already flying at it, and not long enough ago that it must have missed. */
    private boolean missileOnTheWay(Entity e, double dist, long now) {
        Long t = launched.get(e.getId());
        if (t == null) {
            return false;
        }
        long flight = (long) (dist / Munition.SAM.speed) + 40;
        return now - t < flight;
    }

    private double score(Entity e, Vector3d eye) {
        double prio = e instanceof ProjectileEntity ? 0.25 : e instanceof AircraftEntity ? 0.6 : 1.0;
        return e.distanceToSqr(eye) * prio;
    }

    /**
     * Everything that could be a target: what's flying round each player (aircraft are flown by players, missiles
     * fly at them) and close round the Pantsir itself. Looking through the world's own players instead of one box
     * 3.6 km across keeps the scan cheap.
     */
    private Set<Entity> candidates(Vector3d eye) {
        Set<Entity> out = new LinkedHashSet<>();
        double r2 = (MISSILE_RANGE + 100) * (MISSILE_RANGE + 100);
        for (PlayerEntity p : level.players()) {
            if (p.distanceToSqr(eye) > r2) {
                continue;
            }
            out.addAll(level.getEntities(this, p.getBoundingBox().inflate(160), this::canEngage));
        }
        out.addAll(level.getEntities(this, new AxisAlignedBB(eye, eye).inflate(320), this::canEngage));
        return out;
    }

    private boolean canEngage(Entity e) {
        if (e == this || !e.isAlive() || e instanceof AirDefenseTurretEntity || e instanceof PantsirEntity) {
            return false;
        }
        if (e instanceof ProjectileEntity) {
            ProjectileEntity p = (ProjectileEntity) e;
            // incoming missiles and rockets, not our own
            return p.getShooterId() != getId() && (p.isGuided() || p.getMunition().isRocket());
        }
        if (e instanceof AircraftEntity) {
            return ((AircraftEntity) e).isFlying();
        }
        return (Targeting.isAirTarget(e) || e.getType().is(AirDefenseTurretEntity.SAM_TARGETS)) && !e.isOnGround();
    }

    private boolean hostile(Entity e) {
        java.util.UUID other = e instanceof ProjectileEntity ? ((ProjectileEntity) e).getOwnerId() : Targeting.ownerOf(e);
        if (owner == null) {
            return true;
        }
        return !Targeting.isFriendly(level, owner, other);
    }

    private boolean valid(Entity e, Vector3d eye) {
        double r = Targeting.detectRange(level, e, MISSILE_RANGE);
        return canEngage(e) && e.distanceToSqr(eye) < r * r && hostile(e) && clearNear(eye, e);
    }

    /**
     * Line of sight, checked only over the first 200 blocks: hills and buildings round the Pantsir mask it, and
     * tracing kilometres through the world would load every chunk on the way.
     */
    private boolean clearNear(Vector3d eye, Entity target) {
        Vector3d to = target.getBoundingBox().getCenter();
        Vector3d d = to.subtract(eye);
        double len = d.length();
        Vector3d end = len > 200 ? eye.add(d.scale(200 / len)) : to;
        return level.clip(new RayTraceContext(eye, end, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE,
                this)).getType() == RayTraceResult.Type.MISS;
    }

    // ------------------------------------------------------------------ for the HUD

    /** Contacts the radar holds right now (server side only). */
    public int contactCount() {
        return contacts.size();
    }
}
