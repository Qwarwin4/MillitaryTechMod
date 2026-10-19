package com.qwarwin.militarytech.network;

import com.qwarwin.militarytech.entity.VehicleEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Right click on a part of a vehicle the client picked out: a seat, a door, a pylon, or just the hull. */
public class ClickVehiclePacket {
    private final int entityId, kind, index;

    public ClickVehiclePacket(int entityId, int kind, int index) {
        this.entityId = entityId;
        this.kind = kind;
        this.index = index;
    }

    public static void encode(ClickVehiclePacket m, PacketBuffer buf) {
        buf.writeVarInt(m.entityId);
        buf.writeByte(m.kind);
        buf.writeVarInt(m.index);
    }

    public static ClickVehiclePacket decode(PacketBuffer buf) {
        return new ClickVehiclePacket(buf.readVarInt(), buf.readByte(), buf.readVarInt());
    }

    public static void handle(ClickVehiclePacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity p = ctx.get().getSender();
            if (p == null) {
                return;
            }
            Entity e = p.level.getEntity(m.entityId);
            if (e instanceof VehicleEntity) {
                ((VehicleEntity) e).handleClick(p, m.kind, m.index);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
