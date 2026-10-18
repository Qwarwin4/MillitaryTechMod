package com.qwarwin.militarytech.util;

import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.entity.Munition;
import com.qwarwin.militarytech.entity.ProjectileEntity;
import com.qwarwin.militarytech.registry.ModSounds;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * The Oreshnik's flight between leaving the launcher's sight and arriving over the target isn't simulated as an
 * entity: a 3 km hop at those speeds would outrun chunk loading. Instead the arrival is scheduled here, the
 * target area is kept loaded meanwhile, and the warheads appear high above it at the right moment.
 */
@Mod.EventBusSubscriber(modid = MilitaryTech.MODID)
public final class StrikeScheduler {
    private static final List<Strike> PENDING = new ArrayList<>();
    private static int nextKey = 1;

    private static final class Strike {
        final ServerWorld world;
        final Vector3d target;
        @Nullable
        final UUID owner;
        final int key;
        int ticks;

        Strike(ServerWorld world, Vector3d target, @Nullable UUID owner, int ticks) {
            this.world = world;
            this.target = target;
            this.owner = owner;
            this.ticks = ticks;
            this.key = nextKey++;
        }
    }

    public static void oreshnik(ServerWorld world, Vector3d target, @Nullable UUID owner, Vector3d from) {
        double dist = Math.sqrt((target.x - from.x) * (target.x - from.x) + (target.z - from.z) * (target.z - from.z));
        // a few seconds, growing with range: still "hypersonic" next to everything else in the game
        int ticks = 60 + (int) (dist / 25.0);
        Strike s = new Strike(world, target, owner, ticks);
        PENDING.add(s);
        ChunkTickets.target(world, target, s.key);
        ServerPlayerEntity p = Explosions.player(world, owner);
        if (p != null) {
            p.displayClientMessage(new TranslationTextComponent("msg.militarytech.oreshnik_eta", ticks / 20), false);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) {
            return;
        }
        Iterator<Strike> it = PENDING.iterator();
        while (it.hasNext()) {
            Strike s = it.next();
            s.ticks--;
            if (s.ticks % 100 == 0) {
                ChunkTickets.target(s.world, s.target, s.key);
            }
            if (s.ticks == 40) {
                s.world.playSound(null, s.target.x, s.target.y + 20, s.target.z, ModSounds.get("incoming"),
                        SoundCategory.HOSTILE, 40F, 1F);
            }
            if (s.ticks <= 0) {
                it.remove();
                arrive(s);
            }
        }
    }

    private static void arrive(Strike s) {
        double top = Math.max(s.target.y + 170, 200);
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            Vector3d aim = s.target.add(Math.cos(a) * 15, 0, Math.sin(a) * 15);
            Vector3d start = new Vector3d(s.target.x + Math.cos(a) * 6, top + i * 3, s.target.z + Math.sin(a) * 6);
            ProjectileEntity w = ProjectileEntity.launch(s.world, Munition.WARHEAD, null, s.owner, start,
                    aim.subtract(start).normalize().scale(4.0));
            if (w != null) {
                w.setStage(1);
                w.setTargetPos(aim);
                s.world.addFreshEntity(w);
            }
        }
    }

    private StrikeScheduler() {
    }
}
