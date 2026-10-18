package com.qwarwin.militarytech.network;

import com.qwarwin.militarytech.entity.VehicleEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Sent every tick by each crew member's client: the driver's simulated position and controls, the gunner's
 * turret, triggers and one-shot key presses.
 */
public class VehicleInputPacket {
    private static final int DRIVER = 1, TURRET = 2, FIRE1 = 4, FIRE2 = 8, ACTION = 16, LOADOUT = 32, CRASH = 64,
            GROUND = 128, AIM = 256;

    public int entityId;
    private int bits;
    public double x, y, z;
    public float yRot, xRot, roll;
    public float vx, vy, vz;
    public float speed, throttle;
    public int flags;
    public float turretYaw, gunPitch;
    public boolean fire1, fire2, action, loadout, crashed, onGround;
    @Nullable
    public Vector3d aim;
    public int lockId = -1;
    public int cycles;

    public VehicleInputPacket() {
    }

    public static VehicleInputPacket of(VehicleEntity v) {
        VehicleInputPacket m = new VehicleInputPacket();
        m.entityId = v.getId();
        return m;
    }

    public void driverState(VehicleEntity v, int flags, boolean crashed) {
        bits |= DRIVER;
        x = v.getX();
        y = v.getY();
        z = v.getZ();
        yRot = v.yRot;
        xRot = v.xRot;
        roll = v.getRoll();
        Vector3d d = v.getDeltaMovement();
        vx = (float) d.x;
        vy = (float) d.y;
        vz = (float) d.z;
        speed = (float) v.getSpeed();
        throttle = v.getThrottle();
        this.flags = flags;
        this.crashed = crashed;
        this.onGround = v.isOnGround();
    }

    public void turret(float yaw, float pitch) {
        bits |= TURRET;
        turretYaw = yaw;
        gunPitch = pitch;
    }

    public boolean hasDriverState() {
        return (bits & DRIVER) != 0;
    }

    public boolean hasTurret() {
        return (bits & TURRET) != 0;
    }

    public static void encode(VehicleInputPacket m, PacketBuffer buf) {
        int b = m.bits;
        b |= m.fire1 ? FIRE1 : 0;
        b |= m.fire2 ? FIRE2 : 0;
        b |= m.action ? ACTION : 0;
        b |= m.loadout ? LOADOUT : 0;
        b |= m.crashed ? CRASH : 0;
        b |= m.onGround ? GROUND : 0;
        b |= m.aim != null ? AIM : 0;
        buf.writeVarInt(m.entityId);
        buf.writeShort(b);
        if ((b & DRIVER) != 0) {
            buf.writeDouble(m.x);
            buf.writeDouble(m.y);
            buf.writeDouble(m.z);
            buf.writeFloat(m.yRot);
            buf.writeFloat(m.xRot);
            buf.writeFloat(m.roll);
            buf.writeFloat(m.vx);
            buf.writeFloat(m.vy);
            buf.writeFloat(m.vz);
            buf.writeFloat(m.speed);
            buf.writeFloat(m.throttle);
            buf.writeByte(m.flags);
        }
        if ((b & TURRET) != 0) {
            buf.writeFloat(m.turretYaw);
            buf.writeFloat(m.gunPitch);
        }
        if (m.aim != null) {
            buf.writeDouble(m.aim.x);
            buf.writeDouble(m.aim.y);
            buf.writeDouble(m.aim.z);
        }
        buf.writeVarInt(m.lockId + 1);
        buf.writeByte(m.cycles);
    }

    public static VehicleInputPacket decode(PacketBuffer buf) {
        VehicleInputPacket m = new VehicleInputPacket();
        m.entityId = buf.readVarInt();
        int b = buf.readShort();
        m.bits = b & (DRIVER | TURRET);
        m.fire1 = (b & FIRE1) != 0;
        m.fire2 = (b & FIRE2) != 0;
        m.action = (b & ACTION) != 0;
        m.loadout = (b & LOADOUT) != 0;
        m.crashed = (b & CRASH) != 0;
        m.onGround = (b & GROUND) != 0;
        if ((b & DRIVER) != 0) {
            m.x = buf.readDouble();
            m.y = buf.readDouble();
            m.z = buf.readDouble();
            m.yRot = buf.readFloat();
            m.xRot = buf.readFloat();
            m.roll = buf.readFloat();
            m.vx = buf.readFloat();
            m.vy = buf.readFloat();
            m.vz = buf.readFloat();
            m.speed = buf.readFloat();
            m.throttle = buf.readFloat();
            m.flags = buf.readByte();
        }
        if ((b & TURRET) != 0) {
            m.turretYaw = buf.readFloat();
            m.gunPitch = buf.readFloat();
        }
        if ((b & AIM) != 0) {
            m.aim = new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }
        m.lockId = buf.readVarInt() - 1;
        m.cycles = Math.min(4, buf.readByte());
        return m;
    }

    public static void handle(VehicleInputPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity p = ctx.get().getSender();
            if (p == null) {
                return;
            }
            Entity e = p.level.getEntity(m.entityId);
            if (e instanceof VehicleEntity && e.hasPassenger(p) && isSane(m)) {
                ((VehicleEntity) e).applyInput(p, m);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static boolean isSane(VehicleInputPacket m) {
        return !(Double.isNaN(m.x) || Double.isNaN(m.y) || Double.isNaN(m.z) || Math.abs(m.x) > 3.0E7
                || Math.abs(m.z) > 3.0E7 || Float.isNaN(m.yRot) || Float.isNaN(m.turretYaw));
    }
}
