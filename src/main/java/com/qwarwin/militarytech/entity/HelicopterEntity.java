package com.qwarwin.militarytech.entity;

import com.qwarwin.militarytech.Config;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;

/**
 * Coaxial attack helicopter (Ka-52). Flies like one, not like the jets: the rotors spool up first, then
 * Space lifts and Ctrl lets it down, W/S tilt the nose to fly forward and back, A/D slide it sideways, the mouse
 * turns it and aims the cannon and the missiles. Let go of everything and it hangs where it is. Nobody at the
 * controls and the rotors run down: it settles in autorotation, hard but not fatal over open ground.
 */
public class HelicopterEntity extends AircraftEntity {
    private static final float MAX_PITCH = 16F, MAX_ROLL = 18F;
    /** rotor turn, degrees per tick at full speed (client side, for the renderer) */
    private static final float ROTOR_SPEED = 38F;

    public float rotorAngle, rotorAngleO;
    private boolean wasGround = true;

    public HelicopterEntity(EntityType<?> type, World world, VehicleSpec spec) {
        super(type, world, spec);
    }

    @Override
    public boolean isFlying() {
        return !isOnGround();
    }

    /** How far the rotors have spun up, 0..1 (the throttle stands for it). */
    public float rotorRpm() {
        return getThrottle();
    }

    @Override
    public void tick() {
        rotorAngleO = rotorAngle;
        if (level.isClientSide) {
            rotorAngle += ROTOR_SPEED * rotorRpm();
            if (rotorAngle > 3600F) {
                rotorAngle -= 3600F;
                rotorAngleO -= 3600F;
            }
        }
        super.tick();
    }

    @Override
    public void simulate(@Nullable Controls in) {
        AirSpec a = air();
        double load = Math.min(1.5, storeMass() / Math.max(1, a.maxLoad));
        double maxV = a.maxSpeed * Config.AIR_SPEED.get() * (1 - 0.15 * load);
        boolean ground = isOnGround();
        float rpm = getThrottle();
        float fwd = 0F, side = 0F;
        double climb = 0;
        float tYaw = yRot;
        if (in != null) {
            // the turbines run up and the rotors with them: some five seconds from cold
            rpm = bladesGone() ? Math.max(0F, rpm - 0.02F) : Math.min(1F, rpm + 0.01F);
            fwd = in.forward;
            side = in.strafe;
            climb = in.jump ? 1 : in.sprint ? -1 : 0;
            tYaw = in.yaw;
        } else {
            rpm = Math.max(0F, rpm - 0.004F);
        }
        setThrottle(rpm);
        setFlag(FLAG_ENGINE, in != null && rpm > 0.01F);
        setFlag(FLAG_BRAKE, false);
        if (!isGearDown() && ground) {
            setFlag(FLAG_GEAR_UP, false);
        }
        // lift once the rotors are near their speed; a heavy load leaves less to climb with
        boolean lift = rpm > 0.85F && !bladesGone();
        double climbRate = a.climbRate * (1 - 0.35 * load);

        // the airframe leans into where it's going: nose down to fly forward, a wing down to slide
        // (xRot > 0 is nose down; a positive roll is the left side down, as on the jets)
        float wantPitch = ground ? 0F : fwd * MAX_PITCH;
        float wantRoll = ground ? 0F : side * MAX_ROLL;
        xRot += MathHelper.clamp(wantPitch - xRot, -1.6F, 1.6F);
        float roll = getRoll();
        roll += MathHelper.clamp(wantRoll - roll, -2.2F, 2.2F);
        setRoll(roll);
        // the tail turns after the mouse, slower with speed (the fins weathercock it)
        if (!ground || rpm > 0.5F) {
            double turn = a.yawRate * (1.0 - 0.4 * Math.min(1.0, speed / Math.max(0.1, maxV)));
            yRot += MathHelper.clamp(MathHelper.wrapDegrees(tYaw - yRot), (float) -turn, (float) turn);
        }

        // velocity in the world: the tilt pulls it along, the air slows it
        Vector3d vel = getDeltaMovement();
        double yawR = yRot * DEG;
        Vector3d ahead = new Vector3d(-MathHelper.sin((float) yawR), 0, MathHelper.cos((float) yawR));
        Vector3d right = new Vector3d(-ahead.z, 0, ahead.x);
        double h = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        Vector3d horiz = new Vector3d(vel.x, 0, vel.z);
        if (lift && !ground) {
            Vector3d want = ahead.scale(xRot / MAX_PITCH * maxV).add(right.scale(-roll / MAX_ROLL * maxV * 0.35));
            horiz = horiz.add(want.subtract(horiz).scale(0.025));
        } else {
            horiz = horiz.scale(ground ? 0.6 : 0.98);
        }
        double vy = vel.y;
        if (lift) {
            double wantVy = climb * climbRate;
            if (climb <= 0 && ground) {
                // keep the wheels pressed down, or it'd count as airborne
                wantVy = -0.08;
            }
            vy += MathHelper.clamp(wantVy - vy, -0.04, 0.04);
            // forward flight sinks a little unless held up
            if (climb == 0 && h > maxV * 0.6) {
                vy -= 0.004;
            }
        } else {
            // no lift: it falls, the free-wheeling rotors slow the fall to an autorotation
            vy = Math.max(vy - 0.06, !bladesGone() && (rpm > 0.2F || in == null && rotorSpinning()) ? -0.55 : -1.4);
        }
        vel = new Vector3d(horiz.x, vy, horiz.z);
        speed = Math.sqrt(horiz.x * horiz.x + horiz.z * horiz.z);

        double vyBefore = vel.y;
        Vector3d before = position();
        setDeltaMovement(vel);
        move(MoverType.SELF, vel);
        updateTrackAngle(position().subtract(before));

        boolean nowGround = isOnGround();
        boolean crashed = false;
        if (horizontalCollision && speed > 0.9) {
            crashed = true;
        }
        if (nowGround && !wasGround) {
            if (vyBefore < -0.7 || Math.abs(roll) > 30F || Math.abs(xRot) > 25F) {
                crashed = true;
            } else {
                xRot = 0F;
                setRoll(0F);
                setDeltaMovement(getDeltaMovement().multiply(0.3, 0, 0.3));
            }
        }
        if (isInWater() && speed > 0.3) {
            crashed = true;
        }
        wasGround = nowGround;
        if (crashed) {
            if (level.isClientSide) {
                crashPending = true;
            } else {
                crash();
            }
        }
    }

    public boolean bladesGone() {
        return hasFlag(FLAG_NO_BLADES);
    }

    /** K-37-800: charges cut the blades off at the roots and they fly clear, then the seats can fire up through the disc. */
    @Override
    protected void beforeEject() {
        if (!bladesGone() && level instanceof ServerWorld) {
            setFlag(FLAG_NO_BLADES, true);
            setThrottle(0F);
            ServerWorld sw = (ServerWorld) level;
            Vector3d hub = toWorld(new Vector3d(0, air().cockpit.y + 1.4, 0));
            sw.sendParticles(ParticleTypes.EXPLOSION, hub.x, hub.y, hub.z, 3, 0.6, 0.4, 0.6, 0);
            for (int k = 0; k < 12; k++) {
                // pieces of six blades thrown out over the disc
                double a = k * Math.PI / 6, r = 2 + level.random.nextDouble() * 5;
                sw.sendParticles(ParticleTypes.LARGE_SMOKE, hub.x + Math.cos(a) * r, hub.y + (k % 2) * 0.6, hub.z + Math.sin(a) * r,
                        3, 0.2, 0.2, 0.2, 0.02);
                sw.sendParticles(ParticleTypes.CRIT, hub.x + Math.cos(a) * r, hub.y, hub.z + Math.sin(a) * r, 6, 0.3, 0.1, 0.3, 0.6);
            }
            level.playSound(null, hub.x, hub.y, hub.z, SoundEvents.GENERIC_EXPLODE, SoundCategory.NEUTRAL, 2.5F, 1.3F);
            level.playSound(null, hub.x, hub.y, hub.z, SoundEvents.ANVIL_LAND, SoundCategory.NEUTRAL, 1.5F, 0.5F);
        }
        super.beforeEject();
    }

    private boolean rotorSpinning() {
        return getThrottle() > 0.05F;
    }

    /** Height over the ground under it, for the HUD and the gear. */
    public double heightAboveGround() {
        BlockPos p = blockPosition();
        return getY() - level.getHeight(Heightmap.Type.MOTION_BLOCKING, p.getX(), p.getZ());
    }

    /**
     * The 2A42 on its side mount follows the sight a little: up to 12 degrees either way and from 3 up to 30 down,
     * past that it fires along the nose.
     */
    @Override
    protected Vector3d fireDirection(WeaponSpec ws) {
        Vector3d nose = nose();
        if (aimPoint == null || ws.mount != WeaponSpec.Mount.HULL) {
            return nose;
        }
        Vector3d from = toWorld(ws.muzzles[0]);
        Vector3d to = aimPoint.subtract(from).normalize();
        float yaw = (float) (MathHelper.atan2(to.z, to.x) * (180.0 / Math.PI)) - 90F;
        float pitch = (float) -(MathHelper.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * (180.0 / Math.PI));
        float dy = MathHelper.wrapDegrees(yaw - yRot);
        float dp = pitch - xRot;
        if (Math.abs(dy) > 12F || dp < -3F || dp > 30F) {
            return nose;
        }
        return to;
    }

    /** Beam-riding missiles ride the operator's sight: from the cockpit to where he's looking. */
    @Override
    public Vector3d[] guidanceRay() {
        Vector3d eye = toWorld(air().cockpit);
        Vector3d dir = aimPoint != null ? aimPoint.subtract(eye).normalize() : nose();
        return new Vector3d[]{eye, dir};
    }

    @Override
    protected void effects() {
        if (!hasFlag(FLAG_ENGINE) && getThrottle() < 0.3F) {
            return;
        }
        // exhausts on both sides of the mast
        AirSpec a = air();
        for (int s = -1; s <= 1; s += 2) {
            Vector3d e = toWorld(new Vector3d(a.exhaust.x * s, a.exhaust.y, a.exhaust.z));
            if (random.nextFloat() < 0.4F) {
                level.addParticle(ParticleTypes.SMOKE, e.x, e.y, e.z, 0, 0.02, 0);
            }
        }
        // the downwash raises dust when it's low over the ground
        double hag = heightAboveGround();
        if (getThrottle() > 0.6F && hag < 14 && random.nextInt(2) == 0) {
            double r = 5 + random.nextDouble() * 4;
            double ang = random.nextDouble() * Math.PI * 2;
            double gx = getX() + Math.cos(ang) * r, gz = getZ() + Math.sin(ang) * r;
            double gy = getY() - Math.max(0, hag) + 0.2;
            level.addParticle(ParticleTypes.CLOUD, gx, gy, gz, Math.cos(ang) * 0.3, 0.02, Math.sin(ang) * 0.3);
        }
    }
}
