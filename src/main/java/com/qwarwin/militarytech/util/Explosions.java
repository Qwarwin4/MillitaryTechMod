package com.qwarwin.militarytech.util;

import com.qwarwin.militarytech.Config;
import com.qwarwin.militarytech.entity.VehicleEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.UUID;

/** Everything that goes bang. Server side only, every method returns straight away on the client. */
public final class Explosions {

    public static Explosion.Mode mode() {
        return Config.EXPLOSIONS_BREAK_BLOCKS.get() ? Explosion.Mode.BREAK : Explosion.Mode.NONE;
    }

    @Nullable
    public static ServerPlayerEntity player(World level, @Nullable UUID id) {
        if (id == null || level.getServer() == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(id);
    }

    public static void explode(World level, @Nullable Entity attacker, Vector3d pos, float power, boolean fire) {
        if (power <= 0F) {
            return;
        }
        LivingEntity a = attacker instanceof LivingEntity ? (LivingEntity) attacker : null;
        DamageSource src = DamageSource.explosion(a);
        level.explode((Entity) null, src, null, pos.x, pos.y, pos.z, power, fire, mode());
    }

    /** Shrapnel: line-of-sight damage falling off with distance, living things only. */
    public static void fragments(World level, @Nullable Entity attacker, Vector3d pos, double radius, float maxDamage) {
        float mul = Config.FRAGMENT_DAMAGE.get().floatValue();
        if (mul <= 0F) {
            return;
        }
        AxisAlignedBB box = new AxisAlignedBB(pos, pos).inflate(radius);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            Vector3d eye = e.getEyePosition(1F);
            double d = eye.distanceTo(pos);
            if (d > radius || e.getVehicle() instanceof VehicleEntity && ((VehicleEntity) e.getVehicle()).protectsCrew()) {
                continue;
            }
            RayTraceResult r = level.clip(new RayTraceContext(pos, eye, RayTraceContext.BlockMode.COLLIDER,
                    RayTraceContext.FluidMode.NONE, e));
            if (r.getType() != RayTraceResult.Type.MISS) {
                continue;
            }
            e.invulnerableTime = 0;
            e.hurt(ModDamageSources.shrapnel(attacker), ((float) (maxDamage * (1.0 - d / radius)) + 1F) * mul);
        }
    }

    /** Shaped-charge jet: damages everything shootable along a short line. */
    private static void jet(World level, @Nullable Entity attacker, Vector3d pos, Vector3d dir, double length, float damage) {
        Vector3d d = dir.normalize();
        Vector3d end = pos.add(d.scale(length));
        float mul = Config.FRAGMENT_DAMAGE.get().floatValue();
        for (Entity e : level.getEntities((Entity) null, new AxisAlignedBB(pos, end).inflate(6.0),
                e -> e.isAlive() && Targeting.isShootable(e))) {
            if (Targeting.clip(e, pos, end, 0.8) == null) {
                continue;
            }
            e.invulnerableTime = 0;
            e.hurt(ModDamageSources.cumulative(attacker), e instanceof LivingEntity ? damage * mul : damage);
        }
    }

    /**
     * Something hard going through blocks: clears a narrow tunnel up to {@code length} blocks, slowed by hard
     * blocks and stopped by obsidian-class ones. Returns where it ran out.
     */
    public static Vector3d bore(World level, Vector3d from, Vector3d dir, double length, float sideChance) {
        if (!Config.EXPLOSIONS_BREAK_BLOCKS.get()) {
            return from;
        }
        double left = length;
        Vector3d p = from;
        Vector3d step = dir.normalize().scale(0.4);
        BlockPos last = null;
        while (left > 0) {
            p = p.add(step);
            left -= 0.4;
            BlockPos bp = new BlockPos(p);
            if (bp.equals(last)) {
                continue;
            }
            last = bp;
            BlockState st = level.getBlockState(bp);
            if (st.isAir(level, bp)) {
                continue;
            }
            float res = st.getExplosionResistance(level, bp, null);
            if (res >= 1200F || st.getDestroySpeed(level, bp) < 0) {
                break;
            }
            left -= Math.min(3.0, res / 4.0);
            level.destroyBlock(bp, false);
            if (sideChance <= 0F) {
                continue;
            }
            for (Direction side : Direction.values()) {
                BlockPos n = bp.relative(side);
                BlockState ns = level.getBlockState(n);
                if (!ns.isAir(level, n) && ns.getExplosionResistance(level, n, null) < 10F
                        && ns.getDestroySpeed(level, n) >= 0 && level.random.nextFloat() < sideChance) {
                    level.destroyBlock(n, false);
                }
            }
        }
        return p;
    }

    /** HEAT warhead (ATGM, S-8KO, PG-15V): the hit, the jet through armour and walls, blasts at both ends. */
    public static void heat(World level, @Nullable Entity attacker, Vector3d pos, Vector3d dir, @Nullable Entity direct,
                            float power, float directDamage, double penetration) {
        if (level.isClientSide) {
            return;
        }
        if (direct != null && direct.isAlive()) {
            direct.invulnerableTime = 0;
            direct.hurt(ModDamageSources.cumulative(attacker), directDamage);
        }
        Vector3d d = dir.normalize();
        jet(level, attacker, pos.subtract(d.scale(0.5)), d, penetration + 2.0, directDamage * 0.4F);
        explode(level, attacker, pos, power, false);
        Vector3d end = bore(level, pos.subtract(d.scale(0.3)), d, penetration, 0.45F);
        if (end.distanceTo(pos) > 1.5) {
            explode(level, attacker, end, power * 0.55F, false);
        }
        fragments(level, attacker, pos, power * 1.6, 14F);
        ServerWorld sw = (ServerWorld) level;
        sw.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 30, d.x * 0.5, d.y * 0.5, d.z * 0.5, 0.25);
        sw.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 20, 0.6, 0.6, 0.6, 0.03);
    }

    /** APFSDS dart: no explosive, a lot of energy in a small spot, pokes a hole through walls. */
    public static void kinetic(World level, Entity dart, @Nullable Entity attacker, Vector3d pos, Vector3d dir,
                               @Nullable Entity direct, float damage) {
        if (level.isClientSide) {
            return;
        }
        if (direct != null && direct.isAlive()) {
            direct.invulnerableTime = 0;
            direct.hurt(ModDamageSources.kinetic(dart, attacker), damage);
        }
        Vector3d end = bore(level, pos.subtract(dir.normalize().scale(0.2)), dir, 6.0, 0.15F);
        ServerWorld sw = (ServerWorld) level;
        sw.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ANVIL_LAND, SoundCategory.BLOCKS, 2.5F, 0.6F);
        sw.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 25, 0.3, 0.3, 0.3, 0.6);
        sw.sendParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 10, 0.3, 0.3, 0.3, 0.02);
        fragments(level, attacker, end, 3.0, 10F);
    }

    /** High explosive: shells, rockets, bombs. */
    public static void he(World level, @Nullable Entity attacker, Vector3d pos, @Nullable Entity direct, float power,
                          float directDamage, double fragRadius, float fragDamage) {
        if (level.isClientSide) {
            return;
        }
        if (direct != null && direct.isAlive() && directDamage > 0) {
            direct.invulnerableTime = 0;
            direct.hurt(ModDamageSources.shell(null, attacker), directDamage);
        }
        explode(level, attacker, pos, power, false);
        fragments(level, attacker, pos, fragRadius, fragDamage);
        ServerWorld sw = (ServerWorld) level;
        if (power >= 6F) {
            sw.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, (int) (power / 3), power * 0.25,
                    power * 0.15, power * 0.25, 0);
            sw.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y + 1, pos.z, (int) (power * 4), power * 0.3,
                    power * 0.2, power * 0.3, 0.04);
        }
    }

    /** Aerial bomb: one big blast plus a ring of smaller ones, a crater without one enormous explosion. */
    public static void bomb(World level, @Nullable Entity attacker, Vector3d pos, float power) {
        if (level.isClientSide) {
            return;
        }
        power *= Config.BOMB_POWER.get().floatValue();
        he(level, attacker, pos, null, Math.min(power, 10F), 0F, power * 2.2, 30F);
        if (power > 10F) {
            int n = (int) ((power - 6F) / 1.5F);
            double r = power * 0.45;
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                explode(level, attacker, pos.add(Math.cos(a) * r, -1.0, Math.sin(a) * r), power * 0.45F, false);
            }
        }
        ((ServerWorld) level).sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.5, pos.z, (int) (power * 3),
                power * 0.2, 0.5, power * 0.2, 0.1);
    }

    /** Small HE-frag: 30 mm shells. With the proximity fuse it bursts next to aircraft and drones. */
    public static void shell(World level, Entity shell, @Nullable Entity attacker, Vector3d pos, @Nullable Entity direct,
                             float directDamage, float power) {
        if (level.isClientSide) {
            return;
        }
        if (direct != null && direct.isAlive()) {
            if (direct instanceof LivingEntity) {
                direct.invulnerableTime = 0;
            }
            direct.hurt(ModDamageSources.shell(shell, attacker), directDamage);
        }
        ServerWorld sw = (ServerWorld) level;
        double radius = 2.5;
        for (Entity e : level.getEntities(shell, new AxisAlignedBB(pos, pos).inflate(radius), Targeting::isShootable)) {
            if (e == direct) {
                continue;
            }
            double d = e.getBoundingBox().getCenter().distanceTo(pos);
            if (d > radius) {
                continue;
            }
            float dmg = (float) (6.0 * (1.0 - d / radius)) + 2F;
            if (e instanceof LivingEntity) {
                e.invulnerableTime = 0;
                dmg *= Config.FRAGMENT_DAMAGE.get().floatValue();
            }
            e.hurt(ModDamageSources.shrapnel(attacker), dmg);
        }
        if (Config.SHELLS_BREAK_BLOCKS.get() && Config.EXPLOSIONS_BREAK_BLOCKS.get()) {
            level.explode(shell, ModDamageSources.shell(shell, attacker), null, pos.x, pos.y, pos.z, power, false, Explosion.Mode.BREAK);
        } else {
            sw.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.4F, 1.7F);
            sw.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
        sw.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 8, 0.3, 0.3, 0.3, 0.02);
    }

    /** Air-to-air warhead: rod/fragment ring that shreds anything airborne close by. */
    public static void airburst(World level, Entity missile, @Nullable Entity attacker, Vector3d pos, float damage, double radius) {
        if (level.isClientSide) {
            return;
        }
        for (Entity e : level.getEntities(missile, new AxisAlignedBB(pos, pos).inflate(radius), Targeting::isShootable)) {
            double d = e.getBoundingBox().getCenter().distanceTo(pos);
            if (d > radius + e.getBbWidth()) {
                continue;
            }
            float dmg = (float) (damage * (1.0 - 0.6 * Math.min(1.0, d / radius)));
            e.invulnerableTime = 0;
            e.hurt(ModDamageSources.shell(missile, attacker), dmg);
        }
        ServerWorld sw = (ServerWorld) level;
        sw.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundCategory.BLOCKS, 6F, 1.1F);
        sw.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        sw.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 25, 1.2, 1.2, 1.2, 0.05);
    }

    /** Oreshnik submunition: a white-hot slug coming down at hypersonic speed. */
    public static void warhead(World level, @Nullable Entity attacker, Vector3d pos, Vector3d dir, float power) {
        if (level.isClientSide) {
            return;
        }
        Vector3d end = bore(level, pos, dir, power * 1.6, 0.6F);
        explode(level, attacker, end, power, true);
        if (end.distanceTo(pos) > 2) {
            explode(level, attacker, pos, power * 0.6F, true);
        }
        fragments(level, attacker, pos, power * 2.0, 25F);
        ServerWorld sw = (ServerWorld) level;
        sw.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 2, 1.0, 0.5, 1.0, 0);
        sw.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.5, pos.z, 20, 1.5, 0.5, 1.5, 0.1);
        sw.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.5, pos.z, 40, 1.0, 1.5, 1.0, 0.15);
    }

    private Explosions() {
    }
}
