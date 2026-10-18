package com.qwarwin.militarytech.network;

import com.qwarwin.militarytech.entity.VehicleEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** Coordinates typed into the Grad / Oreshnik fire control screen (null clears). */
public class TargetPacket {
    private final int entityId;
    @Nullable
    private final BlockPos target;

    public TargetPacket(int entityId, @Nullable BlockPos target) {
        this.entityId = entityId;
        this.target = target;
    }

    public static void encode(TargetPacket m, PacketBuffer buf) {
        buf.writeVarInt(m.entityId);
        buf.writeBoolean(m.target != null);
        if (m.target != null) {
            buf.writeBlockPos(m.target);
        }
    }

    public static TargetPacket decode(PacketBuffer buf) {
        int id = buf.readVarInt();
        return new TargetPacket(id, buf.readBoolean() ? buf.readBlockPos() : null);
    }

    public static void handle(TargetPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity p = ctx.get().getSender();
            if (p == null) {
                return;
            }
            Entity e = p.level.getEntity(m.entityId);
            if (e instanceof VehicleEntity && ((VehicleEntity) e).isGunner(p)) {
                ((VehicleEntity) e).setTarget(m.target);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
