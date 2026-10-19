package com.qwarwin.militarytech.util;

import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.entity.VehicleEntity;
import com.qwarwin.militarytech.registry.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Contaminated ground after a nuclear burst, kept with the world so it survives a restart. The dose falls off with
 * distance from ground zero and with time; a Geiger counter clicks faster the hotter it is, and the crew of a closed
 * vehicle get only a tenth of it.
 */
@Mod.EventBusSubscriber(modid = MilitaryTech.MODID)
public final class Radiation extends WorldSavedData {
    private static final String NAME = MilitaryTech.MODID + "_radiation";

    private static final class Zone {
        final double x, y, z, r;
        final long start, end;

        Zone(double x, double y, double z, double r, long start, long end) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.r = r;
            this.start = start;
            this.end = end;
        }
    }

    private final List<Zone> zones = new ArrayList<>();

    public Radiation() {
        super(NAME);
    }

    public static Radiation get(ServerWorld w) {
        return w.getDataStorage().computeIfAbsent(Radiation::new, NAME);
    }

    public void add(Vector3d at, double radius, long ticks, long now) {
        if (ticks <= 0) {
            return;
        }
        zones.add(new Zone(at.x, at.y, at.z, radius, now, now + ticks));
        setDirty();
    }

    /** Sieverts an hour, game-scaled: 0.1 is a nuisance, 1 makes you ill, 5+ kills in minutes. */
    public double dose(Vector3d p, long now) {
        double sum = 0;
        for (Zone z : zones) {
            if (now >= z.end) {
                continue;
            }
            double dx = p.x - z.x, dy = (p.y - z.y) * 0.5, dz = p.z - z.z;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d > z.r) {
                continue;
            }
            double k = 1 - d / z.r;
            double age = (double) (now - z.start) / (z.end - z.start);
            // short-lived isotopes go first: hot for a while, then a long tail
            double decay = Math.pow(1 - age, 2) * 0.8 + (1 - age) * 0.2;
            sum += 8.0 * k * k * decay;
        }
        return sum;
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.world instanceof ServerWorld)) {
            return;
        }
        ServerWorld w = (ServerWorld) event.world;
        if (w.getGameTime() % 10 != 0) {
            return;
        }
        Radiation r = w.getDataStorage().get(Radiation::new, NAME);
        if (r == null || r.zones.isEmpty()) {
            return;
        }
        long now = w.getGameTime();
        if (r.zones.removeIf(z -> now >= z.end)) {
            r.setDirty();
        }
        boolean second = now % 20 == 0;
        for (Zone z : r.zones) {
            AxisAlignedBB box = new AxisAlignedBB(z.x - z.r, z.y - z.r, z.z - z.r, z.x + z.r, z.y + z.r, z.z + z.r);
            for (LivingEntity e : w.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                double dose = r.dose(e.position(), now);
                if (e.getVehicle() instanceof VehicleEntity && ((VehicleEntity) e.getVehicle()).protectsCrew(e)) {
                    // a sealed, filtered, shielded cabin keeps out nearly all of it
                    dose *= ((VehicleEntity) e.getVehicle()).spec.nuclearHardened ? 0.02 : 0.1;
                }
                // the helmet's gas mask, and the whole flight kit, keep most of the fallout out
                dose *= com.qwarwin.militarytech.item.PilotArmor.doseFactor(e);
                if (dose < 0.02) {
                    continue;
                }
                if (e instanceof ServerPlayerEntity) {
                    geiger((ServerPlayerEntity) e, dose, second);
                }
                if (second) {
                    sicken(e, dose);
                }
            }
        }
        // fallout drifting down near ground zero
        for (ServerPlayerEntity p : w.players()) {
            for (Zone z : r.zones) {
                double d = p.position().distanceTo(new Vector3d(z.x, z.y, z.z));
                if (d < z.r * 1.2) {
                    w.sendParticles(p, ParticleTypes.WHITE_ASH, true, p.getX(), p.getY() + 6, p.getZ(), 40, 12, 4, 12, 0.01);
                    w.sendParticles(p, ParticleTypes.ASH, true, p.getX(), p.getY() + 6, p.getZ(), 20, 12, 4, 12, 0.01);
                }
            }
        }
    }

    private static void geiger(ServerPlayerEntity p, double dose, boolean second) {
        // a click or several per half second, more the hotter it is
        int clicks = (int) Math.min(6, 1 + dose * 3);
        for (int i = 0; i < clicks; i++) {
            if (p.getRandom().nextDouble() < Math.min(1, 0.35 + dose)) {
                p.playNotifySound(ModSounds.get("geiger"), SoundCategory.PLAYERS, 0.5F, 0.8F + p.getRandom().nextFloat() * 0.5F);
            }
        }
        if (second) {
            TextFormatting c = dose < 0.3 ? TextFormatting.YELLOW : dose < 2 ? TextFormatting.GOLD : TextFormatting.RED;
            p.displayClientMessage(new TranslationTextComponent("msg.militarytech.radiation_dose",
                    String.format(java.util.Locale.ROOT, "%.2f", dose)).withStyle(c), true);
        }
    }

    private static void sicken(LivingEntity e, double dose) {
        e.addEffect(new EffectInstance(Effects.HUNGER, 60, dose > 1 ? 2 : 0, false, false));
        if (dose > 0.25) {
            e.addEffect(new EffectInstance(Effects.WEAKNESS, 80, dose > 2 ? 1 : 0, false, false));
        }
        if (dose > 0.6) {
            e.addEffect(new EffectInstance(Effects.WITHER, 60, dose > 3 ? 1 : 0, false, false));
        }
        if (dose > 1.5) {
            e.addEffect(new EffectInstance(Effects.CONFUSION, 120, 0, false, false));
        }
        if (dose > 4) {
            e.hurt(DamageSource.MAGIC, (float) Math.min(6, dose - 3));
        }
    }

    @Override
    public void load(CompoundNBT tag) {
        zones.clear();
        ListNBT list = tag.getList("Zones", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundNBT t = list.getCompound(i);
            zones.add(new Zone(t.getDouble("X"), t.getDouble("Y"), t.getDouble("Z"), t.getDouble("R"),
                    t.getLong("Start"), t.getLong("End")));
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        ListNBT list = new ListNBT();
        for (Zone z : zones) {
            CompoundNBT t = new CompoundNBT();
            t.putDouble("X", z.x);
            t.putDouble("Y", z.y);
            t.putDouble("Z", z.z);
            t.putDouble("R", z.r);
            t.putLong("Start", z.start);
            t.putLong("End", z.end);
            list.add(t);
        }
        tag.put("Zones", list);
        return tag;
    }

    /** For the HUD and items: the dose at a spot right now (0 when the world has no fallout). */
    public static double doseAt(World w, Vector3d p) {
        if (!(w instanceof ServerWorld)) {
            return 0;
        }
        Radiation r = ((ServerWorld) w).getDataStorage().get(Radiation::new, NAME);
        return r == null ? 0 : r.dose(p, w.getGameTime());
    }
}
