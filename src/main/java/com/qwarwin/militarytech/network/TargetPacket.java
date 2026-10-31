package com.qwarwin.militarytech.network;

import com.qwarwin.militarytech.entity.VehicleEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** Fire control screen: coordinates typed in for the Grad / TOS / Oreshnik (null clears), or the Oreshnik's warhead. */
public class TargetPacket {
    private final int entityId;
    @Nullable
    private final BlockPos target;
    /** -1: set the target; otherwise pick this ammo option and leave the target alone */
    private final int select;

    public TargetPacket(int entityId, @Nullable BlockPos target) {
        this(entityId, target, -1);
    }

    private TargetPacket(int entityId, @Nullable BlockPos target, int select) {
        this.entityId = entityId;
        this.target = target;
        this.select = select;
    }

    public static TargetPacket select(int entityId, int option) {
        return new TargetPacket(entityId, null, option);
    }

    public static void encode(TargetPacket m, PacketBuffer buf) {
        buf.writeVarInt(m.entityId);
        buf.writeByte(m.select);
        buf.writeBoolean(m.target != null);
        if (m.target != null) {
            buf.writeBlockPos(m.target);
        }
    }

    public static TargetPacket decode(PacketBuffer buf) {
        int id = buf.readVarInt();
        int sel = buf.readByte();
        return new TargetPacket(id, buf.readBoolean() ? buf.readBlockPos() : null, sel);
    }

    public static void handle(TargetPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity p = ctx.get().getSender();
            if (p == null) {
                return;
            }
            Entity e = p.level.getEntity(m.entityId);
            if (e instanceof VehicleEntity && ((VehicleEntity) e).isGunner(p)) {
                if (m.select >= 0) {
                    ((VehicleEntity) e).selectAmmo(m.select);
                } else {
                    BlockPos t = m.target;
                    if (t != null) {
                        // the client only knows the ground height where its chunks are loaded (it put 64 in
                        // otherwise): the server looks it up, loading the chunk if it has to
                        t = new BlockPos(t.getX(), p.level.getHeight(Heightmap.Type.MOTION_BLOCKING, t.getX(), t.getZ()), t.getZ());
                    }
                    ((VehicleEntity) e).setTarget(t);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
