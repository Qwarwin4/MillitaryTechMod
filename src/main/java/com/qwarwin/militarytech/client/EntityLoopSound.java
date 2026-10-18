package com.qwarwin.militarytech.client;

import com.qwarwin.militarytech.entity.AircraftEntity;
import com.qwarwin.militarytech.entity.ProjectileEntity;
import com.qwarwin.militarytech.entity.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Engine / motor loop that follows an entity and changes with how hard it works. */
@OnlyIn(Dist.CLIENT)
public class EntityLoopSound extends TickableSound {
    private final Entity entity;
    private final float baseVolume;
    private double lastX, lastY, lastZ;

    public EntityLoopSound(Entity entity, SoundEvent event, float baseVolume) {
        super(event, SoundCategory.NEUTRAL);
        this.entity = entity;
        this.baseVolume = baseVolume;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F;
        this.x = lastX = entity.getX();
        this.y = lastY = entity.getY();
        this.z = lastZ = entity.getZ();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (!entity.isAlive()) {
            stop();
            return;
        }
        x = entity.getX();
        y = entity.getY();
        z = entity.getZ();
        double moved = Math.sqrt((x - lastX) * (x - lastX) + (y - lastY) * (y - lastY) + (z - lastZ) * (z - lastZ));
        lastX = x;
        lastY = y;
        lastZ = z;
        if (entity instanceof AircraftEntity) {
            AircraftEntity a = (AircraftEntity) entity;
            boolean on = a.isVehicle() || a.hasFlag(VehicleEntity.FLAG_ENGINE);
            float thr = a.getThrottle();
            float target = on ? baseVolume * (0.35F + thr * 0.65F) : 0F;
            // quieter in the cockpit of your own plane
            if (a.hasPassenger(Minecraft.getInstance().player)) {
                target *= 0.45F;
            }
            volume += (target - volume) * 0.1F;
            pitch = MathHelper.clamp(0.7F + thr * 0.5F + (float) moved * 0.05F, 0.6F, 1.6F);
        } else if (entity instanceof VehicleEntity) {
            VehicleEntity v = (VehicleEntity) entity;
            float work = (float) Math.min(1.0, moved * 2.5 + (v.isVehicle() ? 0.35 : 0.0));
            float target = v.isVehicle() || moved > 0.02 ? baseVolume * work : 0F;
            if (v.hasPassenger(Minecraft.getInstance().player)) {
                target *= 0.6F;
            }
            volume += (target - volume) * 0.1F;
            pitch = 0.6F + work * 0.5F;
        } else if (entity instanceof ProjectileEntity) {
            volume = baseVolume;
            pitch = 1F;
        } else {
            volume = baseVolume;
        }
    }
}
