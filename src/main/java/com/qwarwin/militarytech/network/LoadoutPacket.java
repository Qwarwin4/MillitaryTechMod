package com.qwarwin.militarytech.network;

import com.qwarwin.militarytech.entity.AircraftEntity;
import com.qwarwin.militarytech.entity.Munition;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Loadout screen: hang a store on a pylon (-1: empty), or pick a preset (pylon -1, value = preset). */
public class LoadoutPacket {
    private final int entityId, pylon, value;
    private final boolean both;

    public LoadoutPacket(int entityId, int pylon, int value, boolean both) {
        this.entityId = entityId;
        this.pylon = pylon;
        this.value = value;
        this.both = both;
    }

    public static void encode(LoadoutPacket m, PacketBuffer buf) {
        buf.writeVarInt(m.entityId);
        buf.writeVarInt(m.pylon + 1);
        buf.writeVarInt(m.value + 1);
        buf.writeBoolean(m.both);
    }

    public static LoadoutPacket decode(PacketBuffer buf) {
        return new LoadoutPacket(buf.readVarInt(), buf.readVarInt() - 1, buf.readVarInt() - 1, buf.readBoolean());
    }

    public static void handle(LoadoutPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity p = ctx.get().getSender();
            if (p == null) {
                return;
            }
            Entity e = p.level.getEntity(m.entityId);
            if (!(e instanceof AircraftEntity)) {
                return;
            }
            AircraftEntity a = (AircraftEntity) e;
            // the pilot, or someone standing by the aircraft
            if (!a.isDriver(p) && (p.isPassenger() || p.distanceTo(a) > 14)) {
                return;
            }
            if (m.pylon < 0) {
                a.applyPreset(p, m.value);
            } else {
                a.configurePylon(p, m.pylon, m.value < 0 ? null : Munition.byId(m.value), m.both);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
