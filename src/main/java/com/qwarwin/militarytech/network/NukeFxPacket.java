package com.qwarwin.militarytech.network;

import com.qwarwin.militarytech.client.NukeFx;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Server to client: a nuclear burst (or a FAB-9000) here, draw it. */
public class NukeFxPacket {
    private final double x, y, z;
    private final float crater, blast;
    private final boolean nuclear;

    public NukeFxPacket(Vector3d pos, float crater, float blast, boolean nuclear) {
        this.nuclear = nuclear;
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.crater = crater;
        this.blast = blast;
    }

    public static void encode(NukeFxPacket m, PacketBuffer buf) {
        buf.writeDouble(m.x);
        buf.writeDouble(m.y);
        buf.writeDouble(m.z);
        buf.writeFloat(m.crater);
        buf.writeFloat(m.blast);
        buf.writeBoolean(m.nuclear);
    }

    public static NukeFxPacket decode(PacketBuffer buf) {
        return new NukeFxPacket(new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readFloat(), buf.readFloat(),
                buf.readBoolean());
    }

    public static void handle(NukeFxPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> NukeFx.start(new Vector3d(m.x, m.y, m.z), m.crater, m.blast, m.nuclear)));
        ctx.get().setPacketHandled(true);
    }
}
