package com.qwarwin.militarytech.util;

import com.qwarwin.militarytech.Config;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;

/**
 * Short lived chunk tickets for things flying with nobody near them. Callers refresh them every few ticks,
 * once the flight is over they run out on their own.
 *
 * A region ticket of radius r keeps chunks entity-ticking up to r - 2 chunks out, so radius 3 keeps the 3x3
 * around the projectile ticking and the ring around it loaded.
 */
public final class ChunkTickets {
    public static final TicketType<Integer> FLIGHT = TicketType.create("militarytech_flight", Integer::compareTo, 60);
    public static final TicketType<Integer> STRIKE = TicketType.create("militarytech_strike", Integer::compareTo, 400);
    private static final int RADIUS = 3;

    public static void keep(ServerWorld world, int key, Vector3d at, int radius) {
        if (!Config.LOAD_CHUNKS.get()) {
            return;
        }
        ChunkPos pos = new ChunkPos(((int) Math.floor(at.x)) >> 4, ((int) Math.floor(at.z)) >> 4);
        world.getChunkSource().addRegionTicket(FLIGHT, pos, radius, key);
    }

    /** Where it is now plus where it will be over the next {@code aheadTicks}, one ticket per second of flight. */
    public static void follow(ServerWorld world, Entity entity, Vector3d velocity, int aheadTicks) {
        keep(world, entity.getId(), entity.position(), RADIUS);
        double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (speed < 0.05) {
            return;
        }
        int step = Math.max(4, (int) (14.0 / speed));
        for (int t = step; t <= aheadTicks; t += step) {
            keep(world, entity.getId(), entity.position().add(velocity.scale(t)), RADIUS);
        }
    }

    /** Long ticket on a target area so a strike arriving in a while lands in a loaded world. */
    public static void target(ServerWorld world, Vector3d at, int key) {
        if (!Config.LOAD_CHUNKS.get()) {
            return;
        }
        ChunkPos pos = new ChunkPos(((int) Math.floor(at.x)) >> 4, ((int) Math.floor(at.z)) >> 4);
        world.getChunkSource().addRegionTicket(STRIKE, pos, 4, key);
    }

    private ChunkTickets() {
    }
}
