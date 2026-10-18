package com.qwarwin.militarytech.entity;

import com.qwarwin.militarytech.Config;
import com.qwarwin.militarytech.registry.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.item.Item;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** Wheeled chassis: steers like a car, can't turn standing still. */
public abstract class TruckEntity extends VehicleEntity {
    public float steer, steerO;

    protected TruckEntity(EntityType<?> type, World world, VehicleSpec spec) {
        super(type, world, spec);
    }

    @Override
    protected Item item() {
        return ModItems.forSpec(spec);
    }

    /** Parked for firing: no driving. */
    protected boolean immobile() {
        return false;
    }

    @Override
    public void simulate(@Nullable Controls in) {
        float fwd = in == null || immobile() ? 0F : in.forward;
        float turn = in == null ? 0F : in.strafe;
        double max = spec.maxSpeed * Config.GROUND_SPEED.get() * (isInWater() ? 0.4 : 1.0);
        if (fwd > 0) {
            speed = Math.min(max, speed + (speed < 0 ? spec.accel * 3 : spec.accel));
        } else if (fwd < 0) {
            speed = Math.max(-spec.reverseSpeed, speed - (speed > 0 ? spec.accel * 3 : spec.accel * 0.7));
        } else {
            speed *= 0.94;
            if (Math.abs(speed) < 0.003) {
                speed = 0;
            }
        }
        steerO = steer;
        steer += (turn - steer) * 0.3F;
        if (isOnGround()) {
            double k = MathHelper.clamp(Math.abs(speed) / 0.15, 0, 1) * Math.signum(speed);
            yRot -= steer * spec.turnRate * k;
        }
        double vy = getDeltaMovement().y - 0.08;
        Vector3d horiz = new Vector3d(-MathHelper.sin(yRot * DEG), 0, MathHelper.cos(yRot * DEG)).scale(speed);
        Vector3d before = position();
        setDeltaMovement(horiz.x, vy * 0.98, horiz.z);
        move(MoverType.SELF, getDeltaMovement());
        if (isOnGround()) {
            setDeltaMovement(getDeltaMovement().multiply(1, 0, 1));
        }
        if (horizontalCollision) {
            speed *= 0.4;
        }
        updateTrackAngle(position().subtract(before));
    }

    @Override
    protected void effects() {
        if (getDriver() != null && random.nextInt(Math.abs(speed) > 0.1 ? 2 : 5) == 0) {
            Vector3d ex = toWorld(new Vector3d(-spec.width * 0.45, 0.6, spec.hitBoxes.get(0).maxZ * 0.35));
            level.addParticle(ParticleTypes.SMOKE, ex.x, ex.y, ex.z, 0.02, 0.04, 0);
        }
    }
}
