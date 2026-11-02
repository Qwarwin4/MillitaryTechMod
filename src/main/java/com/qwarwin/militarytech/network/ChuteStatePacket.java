package com.qwarwin.militarytech.network;

import com.qwarwin.militarytech.client.ChuteClient;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Server to client: this player has a pack on his back / a canopy open over him. */
public class ChuteStatePacket {
    private final int entity;
    private final boolean packed;
    private final int open;

    public ChuteStatePacket(int entity, boolean packed, int open) {
        this.entity = entity;
        this.packed = packed;
        this.open = open;
    }

    public static void encode(ChuteStatePacket m, PacketBuffer buf) {
        buf.writeVarInt(m.entity);
        buf.writeBoolean(m.packed);
        buf.writeByte(m.open);
    }

    public static ChuteStatePacket decode(PacketBuffer buf) {
        return new ChuteStatePacket(buf.readVarInt(), buf.readBoolean(), buf.readByte());
    }

    public static void handle(ChuteStatePacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ChuteClient.state(m.entity, m.packed, m.open)));
        ctx.get().setPacketHandled(true);
    }
}
