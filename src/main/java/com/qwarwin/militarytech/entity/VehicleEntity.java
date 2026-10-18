package com.qwarwin.militarytech.entity;

import com.qwarwin.militarytech.Config;
import com.qwarwin.militarytech.client.ClientHooks;
import com.qwarwin.militarytech.item.AmmoCrateItem;
import com.qwarwin.militarytech.network.VehicleInputPacket;
import com.qwarwin.militarytech.registry.ModItems;
import com.qwarwin.militarytech.registry.ModSounds;
import com.qwarwin.militarytech.util.Explosions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Base of every vehicle. Movement is simulated by the client of whoever drives (like vanilla boats), which
 * sends the result to the server every tick together with the gunner's aim and triggers, see
 * {@link VehicleInputPacket}. The server fires the weapons, keeps score of health and ammo, and simulates
 * the vehicle itself when nobody is driving.
 *
 * Vanilla's own vehicle sync is switched off on purpose ({@link #getControllingPassenger()} returns null):
 * it would kick aircraft pilots for "flying" on servers with allow-flight off.
 */
public abstract class VehicleEntity extends Entity {
    private static final DataParameter<Float> HEALTH = EntityDataManager.defineId(VehicleEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> TURRET_YAW = EntityDataManager.defineId(VehicleEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> GUN_PITCH = EntityDataManager.defineId(VehicleEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> ROLL = EntityDataManager.defineId(VehicleEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> THROTTLE = EntityDataManager.defineId(VehicleEntity.class, DataSerializers.FLOAT);
    /** bits 0-3 selected ammo of the main weapon, 8-15 loadout, 16-23 selected pylon group */
    private static final DataParameter<Integer> SELECT = EntityDataManager.defineId(VehicleEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> FLAGS = EntityDataManager.defineId(VehicleEntity.class, DataSerializers.INT);
    private static final DataParameter<Optional<BlockPos>> TARGET = EntityDataManager.defineId(VehicleEntity.class, DataSerializers.OPTIONAL_BLOCK_POS);
    /** which seat each passenger (by entity id) sits in */
    private static final DataParameter<CompoundNBT> SEATS = EntityDataManager.defineId(VehicleEntity.class, DataSerializers.COMPOUND_TAG);
    /** one bit per door: open */
    private static final DataParameter<Integer> DOORS = EntityDataManager.defineId(VehicleEntity.class, DataSerializers.INT);
    public static final int MAX_AMMO_SLOTS = 13;
    @SuppressWarnings("unchecked")
    private static final DataParameter<Integer>[] AMMO = new DataParameter[MAX_AMMO_SLOTS];

    static {
        for (int i = 0; i < MAX_AMMO_SLOTS; i++) {
            AMMO[i] = EntityDataManager.defineId(VehicleEntity.class, DataSerializers.INT);
        }
    }

    public static final int FLAG_GEAR_UP = 1, FLAG_ERECT = 2, FLAG_BRAKE = 4, FLAG_ENGINE = 8, FLAG_ACTIVE = 16;
    protected static final float DEG = (float) (Math.PI / 180.0);

    public final VehicleSpec spec;
    @Nullable
    protected UUID owner;

    // ---- shared simulation state (client of the driver, or the server when nobody drives)
    protected double speed;
    public float trackAngle, trackAngleO;
    public float rollO;
    public float turretYawO, gunPitchO;

    // ---- client prediction of the turret for the local gunner, and smoothing for everyone else
    private float turretYawC, gunPitchC;
    private boolean turretInit;
    private boolean soundStarted;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYRot, lerpXRot;

    // ---- server side input from the crew
    protected boolean trigger1, trigger2;
    protected final int[] cooldown = new int[6];
    private final int[] muzzleIndex = new int[6];
    protected int rippleLeft;
    @Nullable
    protected Vector3d aimPoint;
    protected int lockId = -1;
    private int noInputTicks;

    protected VehicleEntity(EntityType<?> type, World world, VehicleSpec spec) {
        super(type, world);
        this.spec = spec;
        this.maxUpStep = spec.stepHeight;
        setHealth(spec.health);
    }

    // ------------------------------------------------------------------ synced state

    @Override
    protected void defineSynchedData() {
        entityData.define(HEALTH, 100F);
        entityData.define(TURRET_YAW, 0F);
        entityData.define(GUN_PITCH, 0F);
        entityData.define(ROLL, 0F);
        entityData.define(THROTTLE, 0F);
        entityData.define(SELECT, 0);
        entityData.define(FLAGS, 0);
        entityData.define(TARGET, Optional.empty());
        entityData.define(SEATS, new CompoundNBT());
        entityData.define(DOORS, 0);
        for (DataParameter<Integer> p : AMMO) {
            entityData.define(p, 0);
        }
    }

    public float getHealth() {
        return entityData.get(HEALTH);
    }

    public void setHealth(float h) {
        entityData.set(HEALTH, MathHelper.clamp(h, 0F, spec.health));
    }

    public float getMaxHealth() {
        return spec.health;
    }

    public int getAmmo(int slot) {
        return slot >= 0 && slot < MAX_AMMO_SLOTS ? entityData.get(AMMO[slot]) : 0;
    }

    public void setAmmo(int slot, int v) {
        if (slot >= 0 && slot < MAX_AMMO_SLOTS) {
            entityData.set(AMMO[slot], Math.max(0, Math.min(v, capacity(slot))));
        }
    }

    /** How many rounds a counter holds when full. */
    public int capacity(int slot) {
        int n = 0;
        for (WeaponSpec w : spec.weapons) {
            for (int c : w.capacity) {
                if (n++ == slot) {
                    return c;
                }
            }
        }
        return 0;
    }

    public int getSelected() {
        return entityData.get(SELECT) & 0xF;
    }

    /** Pick the main weapon's ammo type from a screen (the Oreshnik's warhead). */
    public void selectAmmo(int v) {
        if (!spec.weapons.isEmpty() && v >= 0 && v < spec.weapons.get(0).ammo.length) {
            setSelected(v);
        }
    }

    protected void setSelected(int v) {
        entityData.set(SELECT, (entityData.get(SELECT) & ~0xF) | (v & 0xF));
    }

    protected int getSelectBits(int shift) {
        return (entityData.get(SELECT) >> shift) & 0xFF;
    }

    protected void setSelectBits(int shift, int v) {
        int s = entityData.get(SELECT) & ~(0xFF << shift);
        entityData.set(SELECT, s | ((v & 0xFF) << shift));
    }

    public boolean hasFlag(int f) {
        if ((f & DRIVER_FLAGS) != 0 && drivenHere()) {
            return (localFlags & f) != 0;
        }
        return (entityData.get(FLAGS) & f) != 0;
    }

    public void setFlag(int f, boolean on) {
        if ((f & DRIVER_FLAGS) != 0) {
            localFlags = on ? localFlags | f : localFlags & ~f;
        }
        if (!level.isClientSide) {
            int v = entityData.get(FLAGS);
            entityData.set(FLAGS, on ? v | f : v & ~f);
        }
    }

    protected int getFlags() {
        return entityData.get(FLAGS);
    }

    /*
     * Roll, throttle and the driver's switches are owned by the driver's client. There they live in plain fields,
     * so the server echoing last tick's value back can't make them jitter; everyone else reads the synced copy.
     */
    private float localRoll, localThrottle;
    private int localFlags;
    private boolean localInit;
    private static final int DRIVER_FLAGS = FLAG_GEAR_UP | FLAG_BRAKE | FLAG_ENGINE;

    protected boolean drivenHere() {
        return level.isClientSide && isControlledByLocalInstance();
    }

    public float getRoll() {
        return drivenHere() ? localRoll : entityData.get(ROLL);
    }

    public void setRoll(float r) {
        localRoll = r;
        if (!level.isClientSide) {
            entityData.set(ROLL, r);
        }
    }

    public float getThrottle() {
        return drivenHere() ? localThrottle : entityData.get(THROTTLE);
    }

    public void setThrottle(float t) {
        localThrottle = MathHelper.clamp(t, 0F, 1F);
        if (!level.isClientSide) {
            entityData.set(THROTTLE, localThrottle);
        }
    }

    /** Driver switches (gear, brake, engine) as the driver sees them, for the input packet. */
    public int driverFlags() {
        return localFlags & DRIVER_FLAGS;
    }

    private void syncLocalState() {
        if (drivenHere()) {
            if (!localInit) {
                localInit = true;
                localRoll = entityData.get(ROLL);
                localThrottle = entityData.get(THROTTLE);
                localFlags = entityData.get(FLAGS) & DRIVER_FLAGS;
            }
        } else {
            localInit = false;
        }
    }

    @Nullable
    public BlockPos getTarget() {
        return entityData.get(TARGET).orElse(null);
    }

    public void setTarget(@Nullable BlockPos pos) {
        entityData.set(TARGET, Optional.ofNullable(pos));
    }

    public double getSpeed() {
        return speed;
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    // ------------------------------------------------------------------ crew

    /** The seat a passenger sits in (players pick theirs by where they click the vehicle). */
    public int seatOf(@Nullable Entity e) {
        if (e == null) {
            return -1;
        }
        CompoundNBT t = entityData.get(SEATS);
        String k = Integer.toString(e.getId());
        return t.contains(k) ? t.getInt(k) : getPassengers().indexOf(e);
    }

    @Nullable
    public Entity inSeat(int seat) {
        for (Entity p : getPassengers()) {
            if (seatOf(p) == seat) {
                return p;
            }
        }
        return null;
    }

    public boolean isSeatFree(int seat) {
        return seat >= 0 && seat < spec.seats.length && inSeat(seat) == null;
    }

    private void putSeat(Entity e, int seat) {
        CompoundNBT t = entityData.get(SEATS).copy();
        if (seat < 0) {
            t.remove(Integer.toString(e.getId()));
        } else {
            t.putInt(Integer.toString(e.getId()), seat);
        }
        entityData.set(SEATS, t);
    }

    @Nullable
    public Entity getDriver() {
        return inSeat(0);
    }

    @Nullable
    public Entity getGunner() {
        if (!spec.separateGunner) {
            return inSeat(0);
        }
        Entity g = inSeat(1);
        // alone in the vehicle, the driver works the gun too
        return g != null ? g : inSeat(0);
    }

    // ------------------------------------------------------------------ doors

    public boolean isDoorOpen(int d) {
        return (entityData.get(DOORS) & (1 << d)) != 0;
    }

    public void setDoor(int d, boolean open) {
        int v = entityData.get(DOORS);
        entityData.set(DOORS, open ? v | (1 << d) : v & ~(1 << d));
        level.playSound(null, getX(), getY(), getZ(), open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                SoundCategory.NEUTRAL, 1F, 0.8F);
    }

    /** 0 shut .. 1 open, smoothed on the client */
    public float doorOpen(int d, float partial) {
        if (doorAnim == null || d >= doorAnim.length) {
            return 0F;
        }
        return doorAnimO[d] + (doorAnim[d] - doorAnimO[d]) * partial;
    }

    private float[] doorAnim, doorAnimO;

    private void tickDoors() {
        int n = spec.doors.size();
        if (n == 0) {
            return;
        }
        if (doorAnim == null) {
            doorAnim = new float[n];
            doorAnimO = new float[n];
        }
        for (int d = 0; d < n; d++) {
            doorAnimO[d] = doorAnim[d];
            float want = isDoorOpen(d) ? 1F : 0F;
            doorAnim[d] += MathHelper.clamp(want - doorAnim[d], -0.08F, 0.08F);
        }
    }

    /** A seat a player can get into from here: free, and if it's behind a door, the door is open. */
    private boolean reachable(int seat) {
        if (!isSeatFree(seat)) {
            return false;
        }
        int d = spec.doorOf(seat);
        return d < 0 || isDoorOpen(d);
    }

    /** The free seat nearest to a point on the vehicle (vehicle coordinates), among the allowed ones. */
    private int nearestSeat(Vector3d local, @Nullable int[] among) {
        int best = -1;
        double bd = Double.MAX_VALUE;
        for (int s = 0; s < spec.seats.length; s++) {
            if (among != null) {
                boolean in = false;
                for (int a : among) {
                    in |= a == s;
                }
                if (!in) {
                    continue;
                }
            }
            if (!reachable(s)) {
                continue;
            }
            double d = spec.seats[s].distanceToSqr(local);
            if (d < bd) {
                bd = d;
                best = s;
            }
        }
        return best;
    }

    private int pendingSeat = -1;

    private void enter(PlayerEntity player, int seat) {
        if (owner == null) {
            owner = player.getUUID();
        }
        pendingSeat = seat;
        player.startRiding(this);
    }

    /** Right click: the spot clicked picks the seat; clicking a door opens it, or gets you in through it. */
    @Override
    public ActionResultType interactAt(PlayerEntity player, Vector3d vec, Hand hand) {
        if (hand != Hand.MAIN_HAND || player.getVehicle() == this) {
            return ActionResultType.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() || player.isShiftKeyDown() && getPassengers().isEmpty() && spec.doors.isEmpty()) {
            // ammo, repair kits, picking the vehicle up: interact() does those
            return ActionResultType.PASS;
        }
        // the entity's box is a square around the middle; where the player really looks on the hull picks the spot
        Vector3d eye = player.getEyePosition(1F);
        Vector3d hit = com.qwarwin.militarytech.util.Targeting.clip(this, eye, eye.add(player.getViewVector(1F).scale(6)), 0.15);
        Vector3d local = toLocal(hit != null ? hit : position().add(vec));
        for (int d = 0; d < spec.doors.size(); d++) {
            VehicleSpec.Door door = spec.doors.get(d);
            if (!door.area.contains(local)) {
                continue;
            }
            if (level.isClientSide) {
                return ActionResultType.SUCCESS;
            }
            if (!isDoorOpen(d) || player.isShiftKeyDown()) {
                setDoor(d, !isDoorOpen(d));
                return ActionResultType.CONSUME;
            }
            int s = nearestSeat(local, door.seats);
            if (s >= 0 && !player.isPassenger()) {
                enter(player, s);
            } else {
                setDoor(d, false);
            }
            return ActionResultType.CONSUME;
        }
        if (player.isShiftKeyDown() && getPassengers().isEmpty()) {
            return ActionResultType.PASS;
        }
        int s = nearestSeat(local, null);
        if (s < 0 || player.isPassenger()) {
            return ActionResultType.PASS;
        }
        if (level.isClientSide) {
            return ActionResultType.SUCCESS;
        }
        enter(player, s);
        return ActionResultType.CONSUME;
    }

    /** V: move to the next free seat (inside the vehicle you can climb anywhere). */
    public void nextSeat(ServerPlayerEntity player) {
        int cur = seatOf(player);
        for (int k = 1; k < spec.seats.length; k++) {
            int s = (cur + k) % spec.seats.length;
            if (isSeatFree(s)) {
                putSeat(player, s);
                player.displayClientMessage(new TranslationTextComponent(seatKey(s)), true);
                return;
            }
        }
    }

    /** "driver", "gunner", "commander", "troops", "on the armour" */
    public String seatKey(int s) {
        if (s == 0) {
            return "seat.militarytech.driver";
        }
        if (s == 1 && spec.separateGunner) {
            return "seat.militarytech.gunner";
        }
        if (s >= spec.insideSeats) {
            return "seat.militarytech.outside";
        }
        return s == 2 && spec.separateGunner && spec.category == VehicleSpec.Category.TANK ? "seat.militarytech.commander"
                : "seat.militarytech.passenger";
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (!level.isClientSide) {
            int s = pendingSeat >= 0 && isSeatFree(pendingSeat) ? pendingSeat : -1;
            if (s < 0) {
                for (int i = 0; i < spec.seats.length && s < 0; i++) {
                    if (reachable(i)) {
                        s = i;
                    }
                }
            }
            if (s < 0) {
                for (int i = 0; i < spec.seats.length && s < 0; i++) {
                    if (isSeatFree(i)) {
                        s = i;
                    }
                }
            }
            pendingSeat = -1;
            putSeat(passenger, Math.max(0, s));
            if (passenger instanceof PlayerEntity) {
                ((PlayerEntity) passenger).displayClientMessage(new TranslationTextComponent(seatKey(Math.max(0, s))), true);
            }
        }
    }

    public boolean isDriver(@Nullable Entity e) {
        return e != null && e == getDriver();
    }

    public boolean isGunner(@Nullable Entity e) {
        return e != null && e == getGunner();
    }

    /** Crew inside can't be hurt by blast and shrapnel until the vehicle itself goes. */
    public boolean protectsCrew() {
        return spec.enclosed;
    }

    /** Same, for one passenger: riders on the armour are out in the open. */
    public boolean protectsCrew(Entity e) {
        return protectsCrew() && !isOutside(e);
    }

    /** A passenger sitting outside on the hull (troops riding on the armour). */
    public boolean isOutside(@Nullable Entity e) {
        return e != null && seatOf(e) >= spec.insideSeats;
    }

    @Nullable
    @Override
    public Entity getControllingPassenger() {
        return null;
    }

    @Override
    public boolean isControlledByLocalInstance() {
        Entity d = getDriver();
        if (level.isClientSide) {
            return d instanceof PlayerEntity && ((PlayerEntity) d).isLocalPlayer();
        }
        return !(d instanceof PlayerEntity);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        for (int s = 0; s < spec.seats.length; s++) {
            if (isSeatFree(s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void positionRider(Entity passenger) {
        if (!hasPassenger(passenger)) {
            return;
        }
        int i = seatOf(passenger);
        Vector3d seat = spec.seats[MathHelper.clamp(i, 0, spec.seats.length - 1)];
        Vector3d p = toWorld(seat);
        passenger.setPos(p.x, p.y, p.z);
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0;
    }

    @Override
    public Vector3d getDismountLocationForPassenger(LivingEntity passenger) {
        // step out to the side, clear of the hull
        Vector3d side = toWorld(new Vector3d(spec.width * 0.5 + 1.0, 0, 0));
        return new Vector3d(side.x, getY() + 0.2, side.z);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level.isClientSide) {
            putSeat(passenger, -1);
        }
        if (getGunner() == null) {
            trigger1 = trigger2 = false;
            rippleLeft = 0;
        }
    }

    // ------------------------------------------------------------------ frames

    /** Hull-local to world. Aircraft override this to include pitch and roll. */
    public Vector3d toWorld(Vector3d local) {
        return position().add(local.yRot(-yRot * DEG));
    }

    /** World to hull-local, yaw only (hit boxes). */
    public Vector3d toLocal(Vector3d world) {
        return world.subtract(position()).yRot(yRot * DEG);
    }

    public List<AxisAlignedBB> hitBoxes() {
        return spec.hitBoxes;
    }

    public Vector3d pivotWorld() {
        return toWorld(spec.pivot);
    }

    /** Turret-local (relative to the pivot, turns with the turret) to world. */
    public Vector3d turretToWorld(Vector3d local, float turretYaw) {
        return pivotWorld().add(local.yRot(-turretYaw * DEG));
    }

    /** Gun-local (relative to the trunnion, turns and elevates with the gun) to world. */
    public Vector3d gunToWorld(Vector3d local, float turretYaw, float gunPitch) {
        Vector3d v = local.xRot(-gunPitch * DEG).add(spec.trunnion);
        return turretToWorld(v, turretYaw);
    }

    public Vector3d aimVector() {
        return Vector3d.directionFromRotation(getGunPitch(), getTurretYaw());
    }

    /** This crew member's own sight view (periscope, cupola, firing port), or null to use the defaults. */
    @Nullable
    public Vector3d seatView(Entity who, float partial) {
        int s = seatOf(who);
        if (spec.seatViews == null || s < 0 || s >= spec.seatViews.length || spec.seatViews[s] == null
                || spec.hasTurret && isGunner(who)) {
            return null;
        }
        return spec.seatViewOnTurret[s] ? turretToWorld(spec.seatViews[s], getTurretYaw(partial)) : toWorld(spec.seatViews[s]);
    }

    /** The view of this crew member turns with the turret (gunner, commander). */
    public boolean viewsFromTurret(Entity who) {
        if (!spec.hasTurret) {
            return false;
        }
        if (isGunner(who)) {
            return true;
        }
        int s = seatOf(who);
        return spec.seatViews != null && s >= 0 && s < spec.seatViews.length && spec.seatViews[s] != null
                && spec.seatViewOnTurret[s];
    }

    /** Where the gunner's eye sits in sight view. */
    public Vector3d sightWorld(float partial) {
        if (!spec.hasTurret) {
            return toWorld(spec.driverEye);
        }
        return turretToWorld(spec.sight, getTurretYaw(partial));
    }

    // ------------------------------------------------------------------ turret

    public float getTurretYaw() {
        if (level.isClientSide && turretInit) {
            return turretYawC;
        }
        return entityData.get(TURRET_YAW);
    }

    public float getGunPitch() {
        if (level.isClientSide && turretInit) {
            return gunPitchC;
        }
        return entityData.get(GUN_PITCH);
    }

    public float getTurretYaw(float partial) {
        return turretYawO + MathHelper.wrapDegrees(getTurretYaw() - turretYawO) * partial;
    }

    public float getGunPitch(float partial) {
        return MathHelper.lerp(partial, gunPitchO, getGunPitch());
    }

    protected void setAimRaw(float yaw, float pitch) {
        entityData.set(TURRET_YAW, MathHelper.wrapDegrees(yaw));
        entityData.set(GUN_PITCH, MathHelper.clamp(pitch, spec.minPitch, spec.maxPitch));
    }

    /** Clamp a world yaw to the turret's arc around the hull's nose. */
    protected float limitYaw(float yaw) {
        if (spec.yawLimit >= 180F) {
            return yaw;
        }
        float rel = MathHelper.wrapDegrees(yaw - yRot);
        return yRot + MathHelper.clamp(rel, -spec.yawLimit, spec.yawLimit);
    }

    /**
     * Local gunner: turn the predicted turret towards a yaw/pitch at the traverse speed. The result goes to the
     * server in the input packet.
     */
    public void slewLocal(float yaw, float pitch) {
        ensureTurretInit();
        yaw = limitYaw(yaw);
        float sp = spec.traverse;
        float dy = MathHelper.wrapDegrees(yaw - turretYawC);
        float ny = limitYaw(turretYawC + MathHelper.clamp(dy, -sp, sp));
        float tp = MathHelper.clamp(pitch, spec.minPitch, spec.maxPitch);
        float np = gunPitchC + MathHelper.clamp(tp - gunPitchC, -sp * 0.7F, sp * 0.7F);
        turretYawC = MathHelper.wrapDegrees(ny);
        gunPitchC = np;
    }

    /** Server side slewing for mounts nobody sits in. Returns the error left in degrees. */
    protected float slewServer(float yaw, float pitch) {
        float cy = entityData.get(TURRET_YAW), cp = entityData.get(GUN_PITCH);
        yaw = limitYaw(yaw);
        float sp = spec.traverse;
        float tp = MathHelper.clamp(pitch, spec.minPitch, spec.maxPitch);
        float ny = cy + MathHelper.clamp(MathHelper.wrapDegrees(yaw - cy), -sp, sp);
        float np = cp + MathHelper.clamp(tp - cp, -sp * 0.8F, sp * 0.8F);
        setAimRaw(ny, np);
        return Math.abs(MathHelper.wrapDegrees(yaw - ny)) + Math.abs(tp - np);
    }

    /** Yaw/pitch that points the gun from the trunnion at a world point. */
    public float[] aimAt(Vector3d point) {
        Vector3d from = turretToWorld(spec.trunnion, getTurretYaw());
        Vector3d d = point.subtract(from);
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (MathHelper.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90F;
        float pitch = (float) (-MathHelper.atan2(d.y, h) * (180.0 / Math.PI));
        return new float[]{yaw, pitch};
    }

    private void ensureTurretInit() {
        if (!turretInit) {
            turretInit = true;
            turretYawC = entityData.get(TURRET_YAW);
            gunPitchC = entityData.get(GUN_PITCH);
            turretYawO = turretYawC;
            gunPitchO = gunPitchC;
        }
    }

    public boolean isLocalGunner() {
        Entity g = getGunner();
        return g instanceof PlayerEntity && ((PlayerEntity) g).isLocalPlayer();
    }

    private void clientTurretTick() {
        ensureTurretInit();
        turretYawO = turretYawC;
        gunPitchO = gunPitchC;
        rollO = getRoll();
        if (!isLocalGunner()) {
            // follow the synced values, smoothed over a couple of ticks
            float ty = entityData.get(TURRET_YAW);
            float tp = entityData.get(GUN_PITCH);
            turretYawC = MathHelper.wrapDegrees(turretYawC + MathHelper.wrapDegrees(ty - turretYawC) * 0.5F);
            gunPitchC += (tp - gunPitchC) * 0.5F;
            if (Math.abs(MathHelper.wrapDegrees(ty - turretYawC)) > 60F) {
                turretYawC = ty;
                turretYawO = ty;
            }
        }
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        tickDoors();
        trackAngleO = trackAngle;
        if (level.isClientSide) {
            if (!soundStarted) {
                soundStarted = true;
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.onVehicleSeen(this));
            }
            syncLocalState();
            clientTurretTick();
            if (isControlledByLocalInstance()) {
                lerpSteps = 0;
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.driveLocal(this));
            } else {
                tickLerp();
            }
            effects();
            return;
        }
        if (isControlledByLocalInstance()) {
            // nobody (or no player) at the wheel: the server keeps it rolling to a stop / falling
            simulate(null);
        } else if (++noInputTicks > 100) {
            // driver's client went quiet; keep physics honest until it's back
            simulate(null);
        }
        if (getGunner() == null) {
            trigger1 = trigger2 = false;
        }
        serverWeapons();
        if (getY() < -64) {
            destroy(DamageSource.OUT_OF_WORLD);
        }
    }

    /**
     * One tick of movement. {@code in} is the driver's controls, null when nobody drives.
     * Runs on the driver's client, or on the server for an empty vehicle.
     */
    public abstract void simulate(@Nullable Controls in);

    /** Client-side particles and the like, every tick. */
    protected void effects() {
    }

    /** Driver's controls as read on the client. */
    public static final class Controls {
        public float forward, strafe;
        public boolean jump, sprint;
        public float yaw, pitch;
        /** where the crosshair is in the world, for mouse-aim flying */
        public Vector3d look = Vector3d.ZERO;
    }

    protected void updateTrackAngle(Vector3d moved) {
        double fwd = moved.x * -MathHelper.sin(yRot * DEG) + moved.z * MathHelper.cos(yRot * DEG);
        trackAngle += (float) (fwd / Math.max(0.1, spec.wheelRadius)) * (180F / (float) Math.PI);
        if (Math.abs(trackAngle) > 3600F) {
            float wrap = Math.signum(trackAngle) * 3600F;
            trackAngle -= wrap;
            trackAngleO -= wrap;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpXRot = xRot;
        lerpSteps = 4;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void lerpMotion(double x, double y, double z) {
        if (!isControlledByLocalInstance()) {
            setDeltaMovement(x, y, z);
        }
    }

    private void tickLerp() {
        Vector3d before = position();
        if (lerpSteps > 0) {
            double nx = getX() + (lerpX - getX()) / lerpSteps;
            double ny = getY() + (lerpY - getY()) / lerpSteps;
            double nz = getZ() + (lerpZ - getZ()) / lerpSteps;
            yRot += MathHelper.wrapDegrees(lerpYRot - yRot) / lerpSteps;
            xRot += (lerpXRot - xRot) / lerpSteps;
            --lerpSteps;
            setPos(nx, ny, nz);
            setRot(yRot, xRot);
        }
        updateTrackAngle(position().subtract(before));
    }

    // ------------------------------------------------------------------ input from the crew (server)

    public void applyInput(ServerPlayerEntity player, VehicleInputPacket m) {
        if (isDriver(player) && m.hasDriverState()) {
            noInputTicks = 0;
            Vector3d before = position();
            setPos(m.x, m.y, m.z);
            yRot = m.yRot;
            xRot = m.xRot;
            setRoll(m.roll);
            setDeltaMovement(m.vx, m.vy, m.vz);
            speed = m.speed;
            setThrottle(m.throttle);
            int keep = getFlags() & ~DRIVER_FLAGS;
            entityData.set(FLAGS, keep | (m.flags & DRIVER_FLAGS));
            localFlags = m.flags & DRIVER_FLAGS;
            fallDistance = 0F;
            setOnGround(m.onGround);
            updateTrackAngle(position().subtract(before));
            for (Entity p : getPassengers()) {
                positionRider(p);
            }
            ((ServerWorld) level).getChunkSource().move(player);
            if (m.crashed) {
                crash();
                return;
            }
        }
        if (isGunner(player)) {
            if (spec.hasTurret && m.hasTurret()) {
                setAimRaw(limitYaw(m.turretYaw), m.gunPitch);
            }
            trigger1 = m.fire1;
            trigger2 = m.fire2;
            aimPoint = m.aim;
            lockId = m.lockId;
            for (int i = 0; i < m.cycles; i++) {
                cycle(player);
            }
            if (m.action) {
                action(player);
            }
            if (m.loadout) {
                nextLoadout(player);
            }
        }
    }

    /** R: next ammo type / weapon group. */
    protected void cycle(ServerPlayerEntity player) {
        if (spec.weapons.isEmpty()) {
            return;
        }
        WeaponSpec w = spec.weapons.get(0);
        if (w.ammo.length < 2) {
            return;
        }
        int next = (getSelected() + 1) % w.ammo.length;
        setSelected(next);
        // switching rounds means unloading the one in the breech
        cooldown[0] = Math.max(cooldown[0], w.interval / 2);
        player.displayClientMessage(new TranslationTextComponent("msg.militarytech.ammo_selected",
                new TranslationTextComponent(w.ammo[next].key())), true);
        level.playSound(null, getX(), getY(), getZ(), ModSounds.get("reload"), SoundCategory.NEUTRAL, 1F, 1F);
    }

    /** G: vehicle specific (gear, erecting the launcher). */
    protected void action(ServerPlayerEntity player) {
    }

    /** K: next loadout (aircraft on the ground). */
    protected void nextLoadout(ServerPlayerEntity player) {
    }

    // ------------------------------------------------------------------ weapons (server)

    protected void serverWeapons() {
        for (int i = 0; i < cooldown.length; i++) {
            if (cooldown[i] > 0) {
                cooldown[i]--;
            }
        }
        Entity gunner = getGunner();
        if (!(gunner instanceof ServerPlayerEntity)) {
            rippleLeft = 0;
            return;
        }
        ServerPlayerEntity p = (ServerPlayerEntity) gunner;
        for (int w = 0; w < spec.weapons.size(); w++) {
            WeaponSpec ws = spec.weapons.get(w);
            boolean pulled = ws.secondary ? trigger2 : trigger1;
            if (ws.ripple && trigger2 && rippleLeft <= 0 && cooldown[w] <= 0) {
                // RMB on a launcher: a single rocket
                fire(w, p);
                trigger2 = false;
                continue;
            }
            if (ws.ripple && rippleLeft > 0 && !ws.secondary) {
                if (cooldown[w] <= 0) {
                    if (fire(w, p)) {
                        rippleLeft--;
                    } else {
                        rippleLeft = 0;
                    }
                }
                continue;
            }
            if (pulled && cooldown[w] <= 0) {
                if (ws.ripple) {
                    rippleLeft = getAmmo(spec.ammoBase(w) + getSelectedFor(w));
                    if (fire(w, p)) {
                        rippleLeft--;
                    }
                } else {
                    fire(w, p);
                }
                if (!ws.auto) {
                    // semi-auto: one shot per click
                    if (ws.secondary) {
                        trigger2 = false;
                    } else {
                        trigger1 = false;
                    }
                }
            }
        }
    }

    protected int getSelectedFor(int weapon) {
        return weapon == 0 ? Math.min(getSelected(), spec.weapons.get(0).ammo.length - 1) : 0;
    }

    /** Fire one round of weapon {@code w}. Returns false when empty. */
    /** Fire one round of weapon {@code w}; shooter is null for automatic mounts. Returns false when empty. */
    protected boolean fire(int w, @Nullable ServerPlayerEntity shooter) {
        WeaponSpec ws = spec.weapons.get(w);
        int opt = getSelectedFor(w);
        int slot = spec.ammoBase(w) + opt;
        int left = getAmmo(slot);
        if (left <= 0) {
            if (cooldown[w] <= 0) {
                if (shooter != null) {
                    shooter.displayClientMessage(new TranslationTextComponent("msg.militarytech.no_ammo",
                            new TranslationTextComponent(ws.key())), true);
                }
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.DISPENSER_FAIL, SoundCategory.NEUTRAL, 1F, 1.2F);
                cooldown[w] = 10;
            }
            return false;
        }
        Munition m = ws.ammo[opt];
        int muzzle;
        if (ws.ripple || m.isGuided() && ws.muzzles.length > 1) {
            // tubes empty in order
            muzzle = (ws.capacity[opt] - left) % ws.muzzles.length;
        } else {
            muzzle = muzzleIndex[w]++ % ws.muzzles.length;
        }
        Vector3d pos = muzzleWorld(ws, ws.muzzles[muzzle]);
        Vector3d dir = fireDirection(ws);
        float spread = ws.spread;
        if (spread > 0) {
            dir = Vector3d.directionFromRotation(
                    pitchOf(dir) + (random.nextFloat() - 0.5F) * spread * 2F,
                    yawOf(dir) + (random.nextFloat() - 0.5F) * spread * 2F);
        }
        ProjectileEntity pe = ProjectileEntity.launch(level, m, this, shooter != null ? shooter.getUUID() : owner, pos,
                dir.scale(m.speed).add(launchVelocity()));
        if (pe != null) {
            prepareProjectile(pe, m);
            level.addFreshEntity(pe);
        }
        setAmmo(slot, left - 1);
        cooldown[w] = ws.interval;
        SoundEvent s = ModSounds.get(ws.sound);
        level.playSound(null, pos.x, pos.y, pos.z, s, SoundCategory.HOSTILE, ws.volume, 0.95F + random.nextFloat() * 0.1F);
        muzzleFlash(ws, m, pos, dir);
        return true;
    }

    /** Guidance hookup for a projectile that's about to be spawned. */
    protected void prepareProjectile(ProjectileEntity pe, Munition m) {
    }

    protected void muzzleFlash(WeaponSpec ws, Munition m, Vector3d pos, Vector3d dir) {
        ServerWorld sw = (ServerWorld) level;
        if (m.kind == Munition.Kind.BULLET) {
            sw.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.01);
            return;
        }
        if (m == Munition.GRAD || m.isGuided()) {
            sw.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 10, 0.3, 0.3, 0.3, 0.06);
            sw.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 6, 0.1, 0.1, 0.1, 0.05);
            return;
        }
        int n = m.kind == Munition.Kind.SHELL ? 2 : 14;
        sw.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, n, dir.x * 0.2, dir.y * 0.2, dir.z * 0.2, 0.08);
        sw.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, n, 0.4, 0.4, 0.4, 0.04);
        if (m.kind == Munition.Kind.DART || m.kind == Munition.Kind.HE) {
            // big gun: blast kicks up dust around the muzzle
            sw.sendParticles(ParticleTypes.POOF, pos.x, pos.y, pos.z, 25, 1.2, 0.6, 1.2, 0.08);
            for (Entity e : level.getEntities(this, new AxisAlignedBB(pos, pos).inflate(3.0), x -> x instanceof LivingEntity && !hasPassenger(x))) {
                e.hurt(DamageSource.explosion((LivingEntity) null), 4F);
            }
        }
    }

    public Vector3d muzzleWorld(WeaponSpec ws, Vector3d local) {
        switch (ws.mount) {
            case TURRET:
                return turretToWorld(local, getTurretYaw());
            case GUN:
                return gunToWorld(local, getTurretYaw(), getGunPitch());
            default:
                return toWorld(local);
        }
    }

    protected Vector3d fireDirection(WeaponSpec ws) {
        if (ws.mount == WeaponSpec.Mount.HULL) {
            return Vector3d.directionFromRotation(xRot, yRot);
        }
        return aimVector();
    }

    /** Added to every projectile: an aircraft's own speed. */
    protected Vector3d launchVelocity() {
        return Vector3d.ZERO;
    }

    /** Line the beam-riding missiles follow: from the sight along the gun. */
    public Vector3d[] guidanceRay() {
        Vector3d from = turretToWorld(spec.trunnion, getTurretYaw());
        return new Vector3d[]{from, aimVector()};
    }

    @Nullable
    public Vector3d getAimPoint() {
        return aimPoint;
    }

    private static float yawOf(Vector3d d) {
        return (float) (MathHelper.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90F;
    }

    private static float pitchOf(Vector3d d) {
        return (float) (-MathHelper.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * (180.0 / Math.PI));
    }

    // ------------------------------------------------------------------ ammo & interaction

    /** Everything, nuclear warheads included (creative). */
    public void fillAmmo() {
        fillAmmo(true);
    }

    /** Ammo crates don't carry nuclear warheads: those are loaded one by one, by hand. */
    public void fillAmmo(boolean nuclear) {
        for (int i = 0; i < spec.ammoSlots(); i++) {
            Munition m = spec.munitionAt(i);
            if (!nuclear && m != null && m.isNuclear()) {
                continue;
            }
            setAmmo(i, capacity(i));
        }
    }

    public boolean isFullyArmed() {
        for (int i = 0; i < spec.ammoSlots(); i++) {
            Munition m = spec.munitionAt(i);
            if (m != null && m.isNuclear()) {
                continue;
            }
            if (getAmmo(i) < capacity(i)) {
                return false;
            }
        }
        return true;
    }

    protected abstract Item item();

    @Override
    public ActionResultType interact(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != Hand.MAIN_HAND) {
            return ActionResultType.PASS;
        }
        if (level.isClientSide) {
            return ActionResultType.SUCCESS;
        }
        if (stack.getItem() instanceof AmmoCrateItem) {
            if (isFullyArmed()) {
                player.displayClientMessage(new TranslationTextComponent("msg.militarytech.ammo_full"), true);
            } else {
                fillAmmo(false);
                if (!player.abilities.instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, getX(), getY(), getZ(), ModSounds.get("reload"), SoundCategory.NEUTRAL, 1.5F, 0.9F);
                player.displayClientMessage(new TranslationTextComponent("msg.militarytech.rearmed"), true);
            }
            return ActionResultType.CONSUME;
        }
        if (stack.getItem() == ModItems.REPAIR_KIT.get()) {
            if (getHealth() >= getMaxHealth()) {
                player.displayClientMessage(new TranslationTextComponent("msg.militarytech.no_repair"), true);
            } else {
                setHealth(getHealth() + getMaxHealth() * 0.35F);
                if (!player.abilities.instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_USE, SoundCategory.NEUTRAL, 1F, 1F);
            }
            return ActionResultType.CONSUME;
        }
        if (stack.isEmpty() && player.isShiftKeyDown() && getPassengers().isEmpty()) {
            pickUp(player);
            return ActionResultType.CONSUME;
        }
        int seat = nearestSeat(toLocal(player.position()), null);
        if (seat >= 0 && !player.isPassenger()) {
            enter(player, seat);
            return ActionResultType.CONSUME;
        }
        if (!spec.doors.isEmpty() && canAddPassenger(player)) {
            player.displayClientMessage(new TranslationTextComponent("msg.militarytech.door_shut"), true);
        }
        return ActionResultType.PASS;
    }

    private void pickUp(PlayerEntity player) {
        ItemStack item = new ItemStack(item());
        CompoundNBT tag = item.getOrCreateTagElement("Vehicle");
        tag.putFloat("Health", getHealth());
        int[] ammo = new int[spec.ammoSlots()];
        for (int i = 0; i < ammo.length; i++) {
            ammo[i] = getAmmo(i);
        }
        tag.putIntArray("Ammo", ammo);
        tag.putInt("Select", entityData.get(SELECT));
        if (!player.inventory.add(item)) {
            player.drop(item, false);
        }
        remove();
    }

    /** Restore what {@link #pickUp} saved. */
    public void readItemData(@Nullable CompoundNBT tag) {
        if (tag == null) {
            return;
        }
        if (tag.contains("Health")) {
            setHealth(tag.getFloat("Health"));
        }
        if (tag.contains("Select")) {
            entityData.set(SELECT, tag.getInt("Select"));
        }
        int[] ammo = tag.getIntArray("Ammo");
        for (int i = 0; i < ammo.length; i++) {
            setAmmo(i, ammo[i]);
        }
    }

    // ------------------------------------------------------------------ damage

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level.isClientSide) {
            return true;
        }
        if (isInvulnerableTo(source) || !isAlive()) {
            return false;
        }
        Entity direct = source.getDirectEntity();
        if (direct != null && hasPassenger(direct)) {
            return false;
        }
        if (source.getEntity() instanceof PlayerEntity && ((PlayerEntity) source.getEntity()).abilities.instabuild
                && direct == source.getEntity()) {
            destroy(null);
            return true;
        }
        float dealt = amount * spec.armor.factor(source) * Config.DAMAGE_TO_VEHICLES.get().floatValue();
        if (dealt <= 0.05F) {
            if (direct != null && !(direct instanceof PlayerEntity)) {
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundCategory.NEUTRAL, 0.3F, 1.8F);
            }
            return false;
        }
        float h = getHealth() - dealt;
        setHealth(h);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundCategory.NEUTRAL, 0.6F, 1.2F);
        if (h <= 0F) {
            destroy(source);
        }
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source == DamageSource.IN_WALL || source == DamageSource.CACTUS || source == DamageSource.SWEET_BERRY_BUSH
                || source == DamageSource.FALL || super.isInvulnerableTo(source);
    }

    /** Health hit zero (null cause: a creative player took it apart). */
    protected void destroy(@Nullable DamageSource cause) {
        if (!isAlive()) {
            return;
        }
        Vector3d c = position().add(0, spec.height * 0.6, 0);
        List<Entity> crew = getPassengers();
        ejectPassengers();
        remove();
        if (cause == null) {
            return;
        }
        int rounds = 0;
        for (int i = 0; i < spec.ammoSlots(); i++) {
            rounds += getAmmo(i) > 0 ? 1 : 0;
        }
        Entity killer = cause.getEntity();
        for (Entity e : crew) {
            e.hurt(DamageSource.explosion(killer instanceof LivingEntity ? (LivingEntity) killer : null), 12F);
        }
        // fuel and ammunition going up
        Explosions.he(level, killer, c, null, 3.5F + rounds * 0.8F, 0F, 6.0, 12F);
        ServerWorld sw = (ServerWorld) level;
        sw.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 30, 1.5, 1.0, 1.5, 0.2);
        sw.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y + 1, c.z, 40, 1.5, 1.0, 1.5, 0.05);
    }

    /** Driver's client reported a crash (aircraft into the ground, mostly). */
    protected void crash() {
        destroy(new DamageSource("militarytech.crash"));
    }

    // ------------------------------------------------------------------ misc entity stuff

    @Override
    public boolean isPickable() {
        return isAlive();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(8.0, 6.0, 8.0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSqr) {
        return distSqr < 320.0 * 320.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT tag) {
        if (tag.contains("Health")) {
            setHealth(tag.getFloat("Health"));
        }
        int[] ammo = tag.getIntArray("Ammo");
        for (int i = 0; i < ammo.length; i++) {
            setAmmo(i, ammo[i]);
        }
        entityData.set(SELECT, tag.getInt("Select"));
        entityData.set(FLAGS, tag.getInt("Flags") & ~FLAG_ENGINE);
        setAimRaw(tag.getFloat("TurretYaw"), tag.getFloat("GunPitch"));
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        if (tag.contains("TargetX")) {
            setTarget(new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ")));
        }
        speed = 0;
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT tag) {
        tag.putFloat("Health", getHealth());
        int[] ammo = new int[spec.ammoSlots()];
        for (int i = 0; i < ammo.length; i++) {
            ammo[i] = getAmmo(i);
        }
        tag.putIntArray("Ammo", ammo);
        tag.putInt("Select", entityData.get(SELECT));
        tag.putInt("Flags", getFlags());
        tag.putFloat("TurretYaw", entityData.get(TURRET_YAW));
        tag.putFloat("GunPitch", entityData.get(GUN_PITCH));
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        BlockPos t = getTarget();
        if (t != null) {
            tag.putInt("TargetX", t.getX());
            tag.putInt("TargetY", t.getY());
            tag.putInt("TargetZ", t.getZ());
        }
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
