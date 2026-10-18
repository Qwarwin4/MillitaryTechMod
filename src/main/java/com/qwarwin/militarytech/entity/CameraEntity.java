package com.qwarwin.militarytech.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.EntitySize;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.world.World;

/** Invisible entity the client's camera is attached to while looking at its vehicle from outside. */
public class CameraEntity extends Entity {
    public CameraEntity(EntityType<?> type, World world) {
        super(type, world);
        noPhysics = true;
    }

    @Override
    protected float getEyeHeight(Pose pose, EntitySize size) {
        return 0F;
    }

    @Override
    public void tick() {
        // moved by the camera code, every frame
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT tag) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        throw new IllegalStateException("camera entities are client side only");
    }
}
