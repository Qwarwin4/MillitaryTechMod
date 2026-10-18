package com.qwarwin.militarytech.entity;

import com.qwarwin.militarytech.Config;
import com.qwarwin.militarytech.registry.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.item.Item;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** Tracked armour with a turret: T-72B3, T-80U, T-90M, BMPT, BMP-1. */
public class TankEntity extends VehicleEntity {

    public TankEntity(EntityType<?> type, World world, VehicleSpec spec) {
        super(type, world, spec);
    }

    @Override
    protected Item item() {
        return ModItems.forSpec(spec);
    }

    @Override
    public void simulate(@Nullable Controls in) {
        float fwd = in == null ? 0F : in.forward;
        float turn = in == null ? 0F : in.strafe;
        boolean floating = spec.amphibious && isInWater() && getFluidHeight(FluidTags.WATER) > 0.5;
        double max = spec.maxSpeed * Config.GROUND_SPEED.get();
        if (floating) {
            max *= 0.35;
        } else if (isInWater()) {
            max *= 0.6;
        }
        double accel = spec.accel * (in != null && in.sprint ? 1.4 : 1.0);
        if (fwd > 0) {
            speed = Math.min(max, speed + (speed < 0 ? accel * 2.5 : accel));
        } else if (fwd < 0) {
            speed = Math.max(-spec.reverseSpeed, speed - (speed > 0 ? accel * 2.5 : accel * 0.8));
        } else {
            speed *= 0.9;
            if (Math.abs(speed) < 0.003) {
                speed = 0;
            }
        }
        if (speed > max) {
            speed = Math.max(max, speed - accel * 3);
        }
        if (isOnGround() || floating) {
            // tracks turn on the spot, a bit slower at full speed
            float rate = (float) (spec.turnRate * (1.0 - 0.35 * Math.abs(speed) / Math.max(0.01, max)));
            if (speed < -0.01) {
                rate = -rate;
            }
            yRot -= turn * rate;
        }
        double vy = getDeltaMovement().y - 0.08;
        if (floating) {
            // hull floats with the deck just above the water
            double depth = getFluidHeight(FluidTags.WATER);
            vy = vy * 0.6 + (depth - 0.75) * 0.12;
        } else if (isInWater()) {
            vy *= 0.8;
        }
        Vector3d horiz = new Vector3d(-MathHelper.sin(yRot * DEG), 0, MathHelper.cos(yRot * DEG)).scale(speed);
        Vector3d before = position();
        setDeltaMovement(horiz.x, vy * 0.98, horiz.z);
        move(MoverType.SELF, getDeltaMovement());
        if (isOnGround()) {
            setDeltaMovement(getDeltaMovement().multiply(1, 0, 1));
        }
        if (horizontalCollision) {
            speed *= 0.5;
        }
        updateTrackAngle(position().subtract(before));
    }

    @Override
    protected void effects() {
        if (getDriver() == null) {
            return;
        }
        // exhaust; more of it when the engine works hard
        Vector3d ex = toWorld(new Vector3d(spec.width * 0.5, 1.2, -spec.hitBoxes.get(0).maxZ * 0.55));
        if (random.nextInt(Math.abs(speed) > 0.1 ? 2 : 6) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, ex.x, ex.y, ex.z, 0.05, 0.05, 0);
        }
        if (Math.abs(speed) > 0.15 && isOnGround() && random.nextInt(3) == 0) {
            Vector3d dust = toWorld(new Vector3d((random.nextBoolean() ? 1 : -1) * spec.wheelX, 0.1,
                    -spec.hitBoxes.get(0).maxZ));
            level.addParticle(ParticleTypes.POOF, dust.x, dust.y, dust.z, 0, 0.02, 0);
        }
    }
}
