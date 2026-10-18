package com.qwarwin.militarytech.network;

import com.qwarwin.militarytech.entity.VehicleEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** V: move to the next free seat of the vehicle you're in. */
public class SeatPacket {
    public static void encode(SeatPacket m, PacketBuffer buf) {
    }

    public static SeatPacket decode(PacketBuffer buf) {
        return new SeatPacket();
    }

    public static void handle(SeatPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity p = ctx.get().getSender();
            if (p != null && p.getVehicle() instanceof VehicleEntity) {
                ((VehicleEntity) p.getVehicle()).nextSeat(p);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
