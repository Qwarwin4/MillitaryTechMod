package com.qwarwin.militarytech.network;

import com.qwarwin.militarytech.util.Parachute;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: P pressed (open the parachute, or take it off on the ground). */
public class ChutePacket {
    public static void encode(ChutePacket m, PacketBuffer buf) {
    }

    public static ChutePacket decode(PacketBuffer buf) {
        return new ChutePacket();
    }

    public static void handle(ChutePacket m, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayerEntity p = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (p != null) {
                Parachute.key(p);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
